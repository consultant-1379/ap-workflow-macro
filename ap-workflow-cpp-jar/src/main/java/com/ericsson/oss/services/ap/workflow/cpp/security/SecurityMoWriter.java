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

import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.ISCF_FILE_LOCATION;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.RBS_INTEGRITY_CODE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.SECURITY_CONFIG_CHECKSUM;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.nms.security.nscs.api.iscf.IscfResponse;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.ModelConstraintViolationException;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.NotDefinedInModelException;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;

/**
 * Manage updates to Auto Provisioning <code>Security</code> MO.
 */
class SecurityMoWriter {

    @Inject
    private DpsOperations dpsOperations;

    /**
     * Update the AP <code>Security</code> MO attributes (RIC, ISCF location, checksum) based on the {@link IscfResponse} object settings.
     *
     * @param securityMoFdn
     *            the FDN of the AP security MO
     * @param iscfResponse
     *            the ISCF response object
     * @param iscfFilePath
     *            the path of the ISCF file on the SMRS
     */
    public void updateSecurityAttributes(final String securityMoFdn, final IscfResponse iscfResponse, final String iscfFilePath) {
        try {
            setSecurityAttributes(securityMoFdn, iscfResponse.getRbsIntegrityCode(), iscfResponse.getSecurityConfigChecksum(), iscfFilePath);
        } catch (final Exception e) {
            throw new ApApplicationException(String.format("Error occured while updating attributes for security MO %s : %s", securityMoFdn,
                    e.getMessage()), e);
        }
    }

    /**
     * Reset the security attribute to null to indicate that security is not configured for the AP node.
     *
     * @param securityFdn
     *            the FDN of the AP <code>Security</code> MO
     */
    public void resetSecurityAttributes(final String securityFdn) {
        setSecurityAttributes(securityFdn, null, null, null);
    }

    private void setSecurityAttributes(final String securityMoFdn, final String rbsIntegrityCode, final String securityConfigChecksum,
            final String iscfFilePath) {
        try {
            final Map<String, Object> securityAttributes = new HashMap<>();
            securityAttributes.put(ISCF_FILE_LOCATION.toString(), iscfFilePath);
            securityAttributes.put(RBS_INTEGRITY_CODE.toString(), rbsIntegrityCode);
            securityAttributes.put(SECURITY_CONFIG_CHECKSUM.toString(), securityConfigChecksum);
            dpsOperations.getDataPersistenceService().getLiveBucket().findMoByFdn(securityMoFdn).setAttributes(securityAttributes);
        } catch (final NotDefinedInModelException | ModelConstraintViolationException e) {
            throw new ApApplicationException(String.format("Error occured while updating attributes for security MO %s : %s", securityMoFdn,
                    e.getMessage()), e);
        }
    }
}
