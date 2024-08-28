/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.security;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_IDENTIFIER_VALUE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.VALID_NODE_TYPE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NodeDescriptorBuilder.createDefaultNode;
import static com.ericsson.oss.services.ap.model.NodeType.ERBS;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.ENROLLMENT_MODE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.IPSEC_LEVEL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.MIN_SEC_LEVEL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.OPT_SEC_LEVEL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.SUBJECT_ALT_NAME;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.SUBJECT_ALT_NAME_TYPE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

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
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor;
import com.ericsson.oss.services.ap.common.test.stubs.dps.StubbedDpsGenerator;
import com.ericsson.oss.services.ap.workflow.cpp.model.IpSecLevel;

/**
 * Unit tests for {@link IscfDataGenerator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IscfDataGeneratorTest {

    private static final String ME_CONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String NODE_SECURITY_FDN = NODE_FDN + ",Security=1";
    private static final String SUBJECT_ALT_NAME_VALUE = "1.2.3.4";
    private static final String SUBJECT_ALT_TYPE_VALUE_1 = "IPV6";
    private static final String IPSEC_LEVEL_VALUE_0 = "CUS";

    private static final byte[] ISCF_CONTENT = new byte[2];
    private static final SecurityLevel MINIMUM_SEC_LEVEL = SecurityLevel.LEVEL_1;
    private static final SecurityLevel WANTED_SEC_lEVEL = SecurityLevel.LEVEL_2;

    private static final NodeModelInformation MODEL_INFO = new NodeModelInformation(NODE_IDENTIFIER_VALUE, ModelIdentifierType.OSS_IDENTIFIER,
            VALID_NODE_TYPE);
    private static final SubjectAltNameParam SUBJECT_ALT_NAME_PARAM = new SubjectAltNameParam(SubjectAltNameFormat.NONE,
            new SubjectAltNameStringType(null));

    @Mock
    private IscfService iscfService;

    @Mock
    private IscfResponse iscfResponse;

    @InjectMocks
    private IscfDataGenerator iscfDataGenerator;

    @InjectMocks
    private DpsOperations dpsOperations;

    private final StubbedDpsGenerator dpsGenerator = new StubbedDpsGenerator();

    @Before
    public void setUp() {
        final DataPersistenceService dps = dpsGenerator.getStubbedDps();
        Whitebox.setInternalState(dpsOperations, "dps", dps);
        Whitebox.setInternalState(iscfDataGenerator, "dpsOperations", dpsOperations);
        when(iscfResponse.getIscfContent()).thenReturn(ISCF_CONTENT);
    }

    @Test
    public void when_generate_iscf_security_without_ipsec_then_expected_method_been_called() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(MIN_SEC_LEVEL.toString(), "1")
                .withSecurityOption(OPT_SEC_LEVEL.toString(), "2")
                .withSecurityOption(ENROLLMENT_MODE.toString(), "SCEP")
                .build();
        dpsGenerator.generate(nodeDescriptor);

        when(iscfService.generate(NODE_NAME, ME_CONTEXT_FDN, WANTED_SEC_lEVEL, MINIMUM_SEC_LEVEL, EnrollmentMode.SCEP, MODEL_INFO))
                .thenReturn(iscfResponse);

        iscfDataGenerator.generateIscfSecurityForNode(NODE_SECURITY_FDN);

        verify(iscfService).generate(anyString(),
                anyString(),
                any(SecurityLevel.class),
                any(SecurityLevel.class),
                any(EnrollmentMode.class),
                any(NodeModelInformation.class));
    }

    @Test
    public void when_generate_iscf_security_with_ipsec_then_expected_method_been_called() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(MIN_SEC_LEVEL.toString(), "1")
                .withSecurityOption(OPT_SEC_LEVEL.toString(), "2")
                .withSecurityOption(ENROLLMENT_MODE.toString(), "SCEP")
                .withSecurityOption(SUBJECT_ALT_NAME.toString(), SUBJECT_ALT_NAME_VALUE)
                .withSecurityOption(SUBJECT_ALT_NAME_TYPE.toString(), SUBJECT_ALT_TYPE_VALUE_1)
                .withSecurityOption(IPSEC_LEVEL.toString(), IPSEC_LEVEL_VALUE_0)
                .build();
        dpsGenerator.generate(nodeDescriptor);

        when(iscfService.generate(NODE_NAME, ME_CONTEXT_FDN, WANTED_SEC_lEVEL, MINIMUM_SEC_LEVEL, "", SUBJECT_ALT_NAME_PARAM,
                getWantedIpSecAreas(IPSEC_LEVEL_VALUE_0), EnrollmentMode.SCEP, MODEL_INFO)).thenReturn(
                        iscfResponse);

        iscfDataGenerator.generateIscfSecurityForNode(NODE_SECURITY_FDN);

        verify(iscfService).generate(anyString(), anyString(), any(SecurityLevel.class), any(SecurityLevel.class), anyString(),
                any(SubjectAltNameParam.class), anySetOf(IpsecArea.class),
                any(EnrollmentMode.class), any(NodeModelInformation.class));
    }

    @Test(expected = ApApplicationException.class)
    public void when_generate_iscf_security_with_invalid_security_attribute_then_application_exception_been_thrown() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(MIN_SEC_LEVEL.toString(), "1")
                .withSecurityOption(OPT_SEC_LEVEL.toString(), "2")
                .withSecurityOption(ENROLLMENT_MODE.toString(), "SCEP")
                .withSecurityOption(SUBJECT_ALT_NAME.toString(), SUBJECT_ALT_NAME_VALUE)
                .withSecurityOption(SUBJECT_ALT_NAME_TYPE.toString(), "illegal Argument")
                .withSecurityOption(IPSEC_LEVEL.toString(), IPSEC_LEVEL_VALUE_0)
                .build();
        dpsGenerator.generate(nodeDescriptor);

        iscfDataGenerator.generateIscfSecurityForNode(NODE_SECURITY_FDN);
    }

    @Test(expected = ApApplicationException.class)
    public void when_generate_iscf_security_throws_Iscf_service_exception_then_application_exception_been_thrown() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(MIN_SEC_LEVEL.toString(), "1")
                .withSecurityOption(OPT_SEC_LEVEL.toString(), "2")
                .withSecurityOption(ENROLLMENT_MODE.toString(), "SCEP")
                .build();
        dpsGenerator.generate(nodeDescriptor);

        doThrow(IscfServiceException.class).when(iscfService).generate(anyString(), anyString(), any(SecurityLevel.class), any(SecurityLevel.class),
                any(EnrollmentMode.class),
                any(NodeModelInformation.class));

        iscfDataGenerator.generateIscfSecurityForNode(NODE_SECURITY_FDN);
    }

    @Test(expected = ApApplicationException.class)
    public void whenGenerateIscfSecurityThrowsExceptionThenApplicationExceptionBeenThrown() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(MIN_SEC_LEVEL.toString(), "1")
                .withSecurityOption(OPT_SEC_LEVEL.toString(), "2")
                .withSecurityOption(ENROLLMENT_MODE.toString(), "SCEP")
                .build();
        dpsGenerator.generate(nodeDescriptor);

        doThrow(Exception.class).when(iscfService).generate(anyString(), anyString(), any(SecurityLevel.class), any(SecurityLevel.class),
                any(EnrollmentMode.class),
                any(NodeModelInformation.class));

        iscfDataGenerator.generateIscfSecurityForNode(NODE_SECURITY_FDN);
    }

    @Test
    public void when_cancel_iscf_security_succeed_then_excepted_method_been_called() {
        iscfDataGenerator.cancelIscfSecurity(NODE_FDN);
        verify(iscfService, times(1)).cancel(eq("NetworkElement=" + NODE_NAME));
    }

    @Test(expected = ApApplicationException.class)
    public void when_cancel_iscf_security_fails_for_iscfServiceException_then_application_been_thrown() {
        doThrow(IscfServiceException.class).when(iscfService).cancel(anyString());
        iscfDataGenerator.cancelIscfSecurity(NODE_FDN);
    }

    private Set<IpsecArea> getWantedIpSecAreas(final String ipSecLevelName) {
        final IpSecLevel ipSecLevel = IpSecLevel.getIpSecLevel(ipSecLevelName);
        return ipSecLevel.getWantedIpSecAreas();
    }
}