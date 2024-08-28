/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs;

import java.net.StandardProtocolFamily;
import java.util.Set;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import com.ericsson.nms.security.nscs.api.IscfService;
import com.ericsson.nms.security.nscs.api.cpp.level.CPPSecurityLevel;
import com.ericsson.nms.security.nscs.api.enums.EnrollmentMode;
import com.ericsson.nms.security.nscs.api.enums.SecurityLevel;
import com.ericsson.nms.security.nscs.api.exception.IscfServiceException;
import com.ericsson.nms.security.nscs.api.iscf.IpsecArea;
import com.ericsson.nms.security.nscs.api.iscf.IscfResponse;
import com.ericsson.nms.security.nscs.api.iscf.NodeIdentifier;
import com.ericsson.nms.security.nscs.api.iscf.SecurityDataResponse;
import com.ericsson.nms.security.nscs.api.iscf.SubjectAltNameFormat;
import com.ericsson.nms.security.nscs.api.iscf.SubjectAltNameParam;
import com.ericsson.nms.security.nscs.api.model.NodeModelInformation;
import com.ericsson.oss.itpf.sdk.core.annotation.EService;

@Stateless
@EService
@Remote
public class DummyISCFService implements IscfService {

    public static final String ISCF_CONTENT = "Dummy ISCF File Content";
    public static final String RBS_INTEGRATION_CODE = "RIC";
    public static final String SECURITY_CHECKSUM = "DummyChecksum";

    @Override
    public void cancel(final String arg0) throws IscfServiceException {
    }

    @Override
    public IscfResponse generate(final String arg0, final String arg1, final CPPSecurityLevel arg2, final CPPSecurityLevel arg3)
        throws IscfServiceException {

        final IscfResponse iscfResponse = new IscfResponse();
        iscfResponse.setIscfContent(ISCF_CONTENT.getBytes());
        iscfResponse.setRbsIntegrityCode(RBS_INTEGRATION_CODE);
        iscfResponse.setSecurityConfigChecksum(SECURITY_CHECKSUM);
        return iscfResponse;
    }

    @Override
    public IscfResponse generate(final String arg0, final String arg1, final String arg2, final String arg3, final SubjectAltNameFormat arg4,
        final Set<IpsecArea> arg5) throws IscfServiceException {
        final IscfResponse iscfResponse = new IscfResponse();
        iscfResponse.setIscfContent(ISCF_CONTENT.getBytes());
        iscfResponse.setRbsIntegrityCode(RBS_INTEGRATION_CODE);
        iscfResponse.setSecurityConfigChecksum(SECURITY_CHECKSUM);
        return iscfResponse;
    }

    @Override
    public IscfResponse generate(final String arg0, final String arg1, final SecurityLevel arg2, final SecurityLevel arg3, final EnrollmentMode arg4,
        final NodeModelInformation arg5) throws IscfServiceException {
        final IscfResponse iscfResponse = new IscfResponse();
        iscfResponse.setIscfContent(ISCF_CONTENT.getBytes());
        iscfResponse.setRbsIntegrityCode(RBS_INTEGRATION_CODE);
        iscfResponse.setSecurityConfigChecksum(SECURITY_CHECKSUM);
        return iscfResponse;
    }

    @Override
    public IscfResponse generate(final String arg0, final String arg1, final String arg2, final SubjectAltNameParam arg3, final Set<IpsecArea> arg4,
        final EnrollmentMode arg5, final NodeModelInformation arg6) throws IscfServiceException {
        final IscfResponse iscfResponse = new IscfResponse();
        iscfResponse.setIscfContent(ISCF_CONTENT.getBytes());
        iscfResponse.setRbsIntegrityCode(RBS_INTEGRATION_CODE);
        iscfResponse.setSecurityConfigChecksum(SECURITY_CHECKSUM);
        return iscfResponse;
    }

    @Override
    public IscfResponse generate(final String arg0, final String arg1, final CPPSecurityLevel arg2, final CPPSecurityLevel arg3, final String arg4,
        final String arg5, final SubjectAltNameFormat arg6, final Set<IpsecArea> arg7) throws IscfServiceException {
        final IscfResponse iscfResponse = new IscfResponse();
        iscfResponse.setIscfContent(ISCF_CONTENT.getBytes());
        iscfResponse.setRbsIntegrityCode(RBS_INTEGRATION_CODE);
        iscfResponse.setSecurityConfigChecksum(SECURITY_CHECKSUM);
        return iscfResponse;
    }

    @Override
    public IscfResponse generate(final String arg0, final String arg1, final SecurityLevel arg2, final SecurityLevel arg3, final String arg4,
        final SubjectAltNameParam arg5, final Set<IpsecArea> arg6, final EnrollmentMode arg7, final NodeModelInformation arg8)
        throws IscfServiceException {
        final IscfResponse iscfResponse = new IscfResponse();
        iscfResponse.setIscfContent(ISCF_CONTENT.getBytes());
        iscfResponse.setRbsIntegrityCode(RBS_INTEGRATION_CODE);
        iscfResponse.setSecurityConfigChecksum(SECURITY_CHECKSUM);
        return iscfResponse;
    }

    @Override
    public SecurityDataResponse generateSecurityDataCombo(final String arg0, final SubjectAltNameParam arg1, final EnrollmentMode arg2,
        final NodeModelInformation arg3) throws IscfServiceException {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataIpsec(final String arg0, final SubjectAltNameParam arg1, final EnrollmentMode arg2,
        final NodeModelInformation arg3) throws IscfServiceException {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataOam(final String arg0, final EnrollmentMode arg1, final NodeModelInformation arg2)
        throws IscfServiceException {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataOam(final NodeIdentifier nodeId, final EnrollmentMode wantedEnrollmentMode,
        final NodeModelInformation modelInfo) throws IscfServiceException {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataIpsec(final NodeIdentifier nodeId, final SubjectAltNameParam ipsecSubjectAltName,
        final EnrollmentMode wantedEnrollmentMode, final NodeModelInformation modelInfo) throws IscfServiceException {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataCombo(final NodeIdentifier nodeId, final SubjectAltNameParam ipsecSubjectAltName,
        final EnrollmentMode wantedEnrollmentMode, final NodeModelInformation modelInfo) throws IscfServiceException {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataOam(final NodeIdentifier nodeId, final SubjectAltNameParam ipsecSubjectAltName,
        final EnrollmentMode wantedEnrollmentMode,
        final NodeModelInformation modelInfo)
        throws IscfServiceException {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataOam(NodeIdentifier nodeId, EnrollmentMode wantedEnrollmentMode,
            NodeModelInformation modelInfo, StandardProtocolFamily ipVersion) {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataOam(NodeIdentifier nodeId, SubjectAltNameParam subjectAltName,
            EnrollmentMode wantedEnrollmentMode, NodeModelInformation modelInfo, StandardProtocolFamily ipVersion) {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataIpsec(NodeIdentifier nodeId,
            SubjectAltNameParam ipsecSubjectAltName, EnrollmentMode wantedEnrollmentMode,
            NodeModelInformation modelInfo, StandardProtocolFamily ipVersion) {
        return null;
    }

    @Override
    public SecurityDataResponse generateSecurityDataCombo(NodeIdentifier nodeId,
            SubjectAltNameParam ipsecSubjectAltName, EnrollmentMode wantedEnrollmentMode,
            NodeModelInformation modelInfo, StandardProtocolFamily ipVersion) {
        return null;
    }
}
