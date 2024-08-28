/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.security;

import static com.ericsson.nms.security.nscs.api.enums.SecurityLevel.getSecurityLevel;
import static com.ericsson.oss.services.ap.common.model.MoType.NETWORK_ELEMENT;
import static com.ericsson.oss.services.ap.common.model.NodeAttribute.NODE_IDENTIFIER;
import static com.ericsson.oss.services.ap.common.model.NodeAttribute.NODE_TYPE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.ENROLLMENT_MODE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.IPSEC_LEVEL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.MIN_SEC_LEVEL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.OPT_SEC_LEVEL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.SUBJECT_ALT_NAME;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.SUBJECT_ALT_NAME_TYPE;
import static java.lang.String.format;

import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.ericsson.nms.security.nscs.api.IscfService;
import com.ericsson.nms.security.nscs.api.enums.EnrollmentMode;
import com.ericsson.nms.security.nscs.api.enums.SecurityLevel;
import com.ericsson.nms.security.nscs.api.exception.IscfServiceException;
import com.ericsson.nms.security.nscs.api.iscf.IpsecArea;
import com.ericsson.nms.security.nscs.api.iscf.IscfResponse;
import com.ericsson.nms.security.nscs.api.iscf.SubjectAltNameFormat;
import com.ericsson.nms.security.nscs.api.iscf.SubjectAltNameParam;
import com.ericsson.nms.security.nscs.api.iscf.SubjectAltNameStringType;
import com.ericsson.nms.security.nscs.api.model.NodeModelInformation;
import com.ericsson.nms.security.nscs.api.model.NodeModelInformation.ModelIdentifierType;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.api.exception.NodeNotFoundException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.util.cdi.Transactional;
import com.ericsson.oss.services.ap.common.util.cdi.Transactional.TxType;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.workflow.cpp.model.IpSecLevel;

/**
 * Generate ISCF node security related data.
 */
class IscfDataGenerator {

    private static final String SECURITY_SERVICE_ERROR = "Security Service Error";

    @Inject
    private DpsOperations dpsOperations;

    private IscfService iscfService;

    @PostConstruct
    public void init() {
        iscfService = new ServiceFinderBean().find(IscfService.class);
    }

    /**
     * Generate the security data for the node using the {@link IscfService}.
     *
     * @param apSecurityFdn
     *            the FDN of the AP Security MO
     * @return the response containing data related to the generate security information from the {@link IscfService}
     */
    public IscfResponse generateIscfSecurityForNode(final String apSecurityFdn) {
        final ManagedObject securityMo = getMo(apSecurityFdn);
        return generateIscfSecurityData(securityMo);
    }

    /**
     * Cancel/cleanup any ISCF security related resources for the given AP node in a new transaction.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     */
    @Transactional(txType = TxType.REQUIRES_NEW)
    public void cancelIscfSecurity(final String apNodeFdn) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        final String networkElementFdn = getNetworkElementFdn(nodeName);

        try {
            iscfService.cancel(networkElementFdn);
        } catch (final IscfServiceException e) {
            throw new ApApplicationException(format("Error cancelling ISCF generation for node %s : %s", networkElementFdn, e.getMessage()), e);
        } catch (final Exception e) {
            throw new ApApplicationException(SECURITY_SERVICE_ERROR, e);
        }

    }

    private IscfResponse generateIscfSecurityData(final ManagedObject securityMo) {
        final Map<String, Object> securityAttributes = securityMo.getAllAttributes();
        final String nodeName = securityMo.getParent().getName();

        try {
            if (isIpSecSecurityRequired(securityAttributes)) {
                return generateSecurityWithIpSec(securityMo);
            } else {
                return generateSecurityWithoutIpSec(securityMo);
            }
        } catch (final IscfServiceException e) {
            throw new ApApplicationException(String.format("Error generating ISCF content for node %s : %s", nodeName, e.getMessage()), e);
        } catch (final Exception e) {
            throw new ApApplicationException(SECURITY_SERVICE_ERROR, e);
        }
    }

    private IscfResponse generateSecurityWithIpSec(final ManagedObject securityMo) {
        final ManagedObject nodeMo = securityMo.getParent();
        final String nodeName = nodeMo.getName();
        final String networkElementFdn = getNetworkElementFdn(nodeName);
        final Map<String, Object> securityAttributes = securityMo.getAllAttributes();

        final String ipsecUserLabel = ""; // ipsecUserLabel set by operator, not needed for AP so set to blank
        final SecurityLevel minimumSecLevel = getMinimumSecurityLevel(securityAttributes);
        final SecurityLevel wantedSecLevel = getWantedSecurityLevel(securityAttributes);
        final String ipSecLevel = (String) securityAttributes.get(IPSEC_LEVEL.toString());
        final SubjectAltNameParam subjectAltNameParam = getSubjectAltNameParam(securityAttributes);
        final Set<IpsecArea> wantedIpSecAreas = getWantedIpSecAreas(ipSecLevel);
        final EnrollmentMode wantedEnrollmentMode = getEnrollmentMode(securityAttributes);
        final NodeModelInformation modelInfo = getNodeModelInformation(nodeMo);

        return iscfService.generate(nodeName, networkElementFdn, wantedSecLevel, minimumSecLevel, ipsecUserLabel, subjectAltNameParam,
                wantedIpSecAreas, wantedEnrollmentMode, modelInfo);
    }

    private IscfResponse generateSecurityWithoutIpSec(final ManagedObject securityMo) {
        final ManagedObject nodeMo = securityMo.getParent();
        final String nodeName = nodeMo.getName();
        final String networkElementFdn = getNetworkElementFdn(nodeName);
        final Map<String, Object> securityAttributes = securityMo.getAllAttributes();

        final SecurityLevel minimumSecLevel = getMinimumSecurityLevel(securityAttributes);
        final SecurityLevel wantedSecLevel = getWantedSecurityLevel(securityAttributes);
        final EnrollmentMode wantedEnrollmentMode = getEnrollmentMode(securityAttributes);
        final NodeModelInformation modelInfo = getNodeModelInformation(nodeMo);

        return iscfService.generate(nodeName, networkElementFdn, wantedSecLevel, minimumSecLevel, wantedEnrollmentMode, modelInfo);
    }

    private static SubjectAltNameParam getSubjectAltNameParam(final Map<String, Object> securityAttributes) {
        final String subjectAltNameType = (String) securityAttributes.get(SUBJECT_ALT_NAME_TYPE.toString());

        if (subjectAltNameType == null) {
            return new SubjectAltNameParam(SubjectAltNameFormat.NONE, new SubjectAltNameStringType(null));
        } else {
            final String ipsecSubjectAltName = (String) securityAttributes.get(SUBJECT_ALT_NAME.toString());
            return buildSubjectAltNameParam(ipsecSubjectAltName, subjectAltNameType);
        }
    }

    private static SubjectAltNameParam buildSubjectAltNameParam(final String ipsecSubjectAltName, final String subjectAltNameType) {
        final SubjectAltNameFormat subjectAltNameFormat = getSubjectAltNameFormatFromString(subjectAltNameType);
        final SubjectAltNameStringType subjectAltNameStringType = new SubjectAltNameStringType(ipsecSubjectAltName);
        return new SubjectAltNameParam(subjectAltNameFormat, subjectAltNameStringType);
    }

    private static EnrollmentMode getEnrollmentMode(final Map<String, Object> securityAttributes) {
        final String enrollModeString = (String) securityAttributes.get(ENROLLMENT_MODE.toString());
        return EnrollmentMode.valueOf(enrollModeString);
    }

    public static NodeModelInformation getNodeModelInformation(final ManagedObject nodeMo) {
        final String nodeIdentifier = nodeMo.getAttribute(NODE_IDENTIFIER.toString());
        final String nodeType = nodeMo.getAttribute(NODE_TYPE.toString());
        return new NodeModelInformation(nodeIdentifier, ModelIdentifierType.OSS_IDENTIFIER, nodeType);
    }

    private static SecurityLevel getWantedSecurityLevel(final Map<String, Object> securityAttributes) {
        final String wantedSecurityLevelString = (String) securityAttributes.get(OPT_SEC_LEVEL.toString());
        return getSecurityLevel(wantedSecurityLevelString);
    }

    private static SecurityLevel getMinimumSecurityLevel(final Map<String, Object> securityAttributes) {
        final String minimumSecurityLevelString = (String) securityAttributes.get(MIN_SEC_LEVEL.toString());
        return getSecurityLevel(minimumSecurityLevelString);
    }

    private static Set<IpsecArea> getWantedIpSecAreas(final String ipSecLevelName) {
        final IpSecLevel ipSecLevel = IpSecLevel.getIpSecLevel(ipSecLevelName);
        return ipSecLevel.getWantedIpSecAreas();
    }

    private static SubjectAltNameFormat getSubjectAltNameFormatFromString(final String subjectAltNameType) {
        for (final SubjectAltNameFormat subjectAltNameFormat : SubjectAltNameFormat.values()) {
            if (subjectAltNameFormat.name().equals(subjectAltNameType)) {
                return subjectAltNameFormat;
            }
        }
        throw new ApApplicationException(
                String.format("SubjectAltNameType %s from Security MO does not match any valid SubjectAltNameFormat types", subjectAltNameType));
    }

    private static boolean isIpSecSecurityRequired(final Map<String, Object> securityMoAttributes) {
        return securityMoAttributes.containsKey(IPSEC_LEVEL.toString()) && securityMoAttributes.get(IPSEC_LEVEL.toString()) != null;
    }

    private static String getNetworkElementFdn(final String nodeName) {
        return NETWORK_ELEMENT.toString() + "=" + nodeName;
    }

    private ManagedObject getMo(final String moFdn) {
        final ManagedObject nodeMo = dpsOperations.getDataPersistenceService().getLiveBucket().findMoByFdn(moFdn);
        if (nodeMo == null) {
            throw new NodeNotFoundException(String.format("No Managed Object could be found for MO %s", moFdn));
        }
        return nodeMo;
    }
}
