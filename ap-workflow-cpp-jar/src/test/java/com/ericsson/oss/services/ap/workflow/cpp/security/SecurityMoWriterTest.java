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

import static com.ericsson.oss.services.ap.common.model.MoType.SECURITY;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.RBS_INTEGRITY_CODE_VALUE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NodeDescriptorBuilder.createDefaultNode;
import static com.ericsson.oss.services.ap.model.NodeType.ERBS;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.ISCF_FILE_LOCATION;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.RBS_INTEGRITY_CODE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.SECURITY_CONFIG_CHECKSUM;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.nms.security.nscs.api.iscf.IscfResponse;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.exception.model.ModelConstraintViolationException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor;
import com.ericsson.oss.services.ap.common.test.stubs.dps.StubbedDpsGenerator;

/**
 * Unit tests for {@link SecurityMoWriter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityMoWriterTest {

    private static final String NODE_SECURITY_FDN = NODE_FDN + ",Security=1";
    private static final String SECURITY_FILE_PATH = "/ericsson/tor/smrs/lran/ai/erbs/" + NODE_NAME + "/Security/Iscf.xml";
    private static final String SECURITY_CHECKSUM = "check sum";

    private final StubbedDpsGenerator dpsGenerator = new StubbedDpsGenerator();

    @Mock
    private DataBucket liveBucket;

    @Mock
    private IscfResponse iscfResponse;

    @InjectMocks
    private SecurityMoWriter securityMoWriter;

    @InjectMocks
    private DpsOperations dpsOperations;

    @Before
    public void setUp() {
        final DataPersistenceService dps = dpsGenerator.getStubbedDps();
        Whitebox.setInternalState(dpsOperations, "dps", dps);
        Whitebox.setInternalState(securityMoWriter, "dpsOperations", dpsOperations);

        when(iscfResponse.getRbsIntegrityCode()).thenReturn(RBS_INTEGRITY_CODE_VALUE);
        when(iscfResponse.getSecurityConfigChecksum()).thenReturn(SECURITY_CHECKSUM);
    }

    @Test
    public void when_update_security_mo_then_update_successfuly() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(RBS_INTEGRITY_CODE.toString(), RBS_INTEGRITY_CODE_VALUE)
                .build();
        final ManagedObject nodeMo = dpsGenerator.generate(nodeDescriptor);
        final ManagedObject securityMo = nodeMo.getChild(SECURITY.toString() + "=1");
        securityMoWriter.updateSecurityAttributes(NODE_SECURITY_FDN, iscfResponse, SECURITY_FILE_PATH);

        assertEquals(securityMo.getAttribute(ISCF_FILE_LOCATION.toString()), SECURITY_FILE_PATH);
        assertEquals(securityMo.getAttribute(RBS_INTEGRITY_CODE.toString()), RBS_INTEGRITY_CODE_VALUE);
        assertEquals(securityMo.getAttribute(SECURITY_CONFIG_CHECKSUM.toString()), SECURITY_CHECKSUM);
    }

    @Test(expected = ApApplicationException.class)
    public void when_update_security_mo_dps_throw_ModelConstraintViolationException_then_exception_been_catched() {
        final DataPersistenceService dps = Mockito.mock(DataPersistenceService.class);
        final ManagedObject securityMo = Mockito.mock(ManagedObject.class);

        Whitebox.setInternalState(securityMoWriter, "dpsOperations", dpsOperations);
        when(dps.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(NODE_SECURITY_FDN)).thenReturn(securityMo);

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(ISCF_FILE_LOCATION.toString(), SECURITY_FILE_PATH);
        attributes.put(RBS_INTEGRITY_CODE.toString(), RBS_INTEGRITY_CODE_VALUE);
        attributes.put(SECURITY_CONFIG_CHECKSUM.toString(), SECURITY_CHECKSUM);

        doThrow(ModelConstraintViolationException.class).when(securityMo).setAttributes(attributes);
        securityMoWriter.updateSecurityAttributes(NODE_SECURITY_FDN, iscfResponse, SECURITY_FILE_PATH);
    }
}
