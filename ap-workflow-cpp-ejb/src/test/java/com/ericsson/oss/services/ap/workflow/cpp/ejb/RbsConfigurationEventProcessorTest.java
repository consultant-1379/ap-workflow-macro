/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.ejb;

import static com.ericsson.oss.services.ap.common.model.Namespace.AP;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_MISMATCH_ERROR;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_POSITION_UNAVAILABLE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_SUCCESSFULLY_MATCHED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_WANTED_POSITION_NOT_SET;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.ap.api.cluster.APServiceClusterMember;
import com.ericsson.oss.services.ap.common.cm.DpsQueries;
import com.ericsson.oss.services.ap.common.cm.DpsQueries.DpsQueryExecutor;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

/**
 * Unit tests for {@link RbsConfigurationEventConsumer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RbsConfigurationEventProcessorTest {

    private static final String BUSINESS_KEY = "AP_Node=" + NODE_NAME;

    private static final String ME_CONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String RBS_CONFIGURATION_FDN = ME_CONTEXT_FDN + ",ManagedElement=1,ENodeBFunction=1,RbsConfiguration=1";

    private static final String RBS_CONFIG_LEVEL = "rbsConfigLevel";

    private static final String SITE_CONFIG_COMPLETE = "SITE_CONFIG_COMPLETE";
    private static final String S1_COMPLETE = "S1_COMPLETE";
    private static final String S1_NOT_NEEDED = "S1_NOT_NEEDED";
    private static final String GPS_CHECK_POSITION_COMPLETE = "GPS_POSITION_CHECK_COMPLETE";
    private static final String WORKFLOW_VARIABLES_KEY_GPS_CHECK_RESULT = "gps_check_result";

    @Mock
    private Logger logger; // NOPMD

    @Mock
    private SystemRecorder recorder; // NOPMD

    @Mock
    private DpsQueries dpsQueries;

    @Mock
    private DpsQueryExecutor<ManagedObject> dpsQueryExecutor;

    @Mock
    private APServiceClusterMember apServiceClusterMembership;

    @Mock
    private WorkflowInstanceServiceLocal wfsInstanceService;

    @InjectMocks
    private RbsConfigurationEventProcessor rbsConfigurationEventProcessor;

    @Before
    public void setUp() {
        when(apServiceClusterMembership.isMasterNode()).thenReturn(true);
        when(dpsQueries.findMoByName(NODE_NAME, MoType.NODE.toString(), AP.toString())).thenReturn(dpsQueryExecutor);
        when(dpsQueryExecutor.executeCount()).thenReturn(1L);
    }

    @Test
    public void whenRbsConfigLevelChangesToSITECONFIGCOMPLETEthenCorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", SITE_CONFIG_COMPLETE);
        rbsConfigurationEventProcessor.processNotification(payload);
        verify(wfsInstanceService).correlateMessage(SITE_CONFIG_COMPLETE, BUSINESS_KEY, Collections.<String, Object> emptyMap());
    }

    @Test
    public void whenRbsConfigLevelChangesToS1COMPLETEthenCorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", S1_COMPLETE);
        rbsConfigurationEventProcessor.processNotification(payload);
        verify(wfsInstanceService).correlateMessage(S1_COMPLETE, BUSINESS_KEY, Collections.<String, Object> emptyMap());
    }

    @Test
    public void whenRbsConfigLevelChangesToS1NOTNEEDEDThenCorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", S1_NOT_NEEDED);
        rbsConfigurationEventProcessor.processNotification(payload);
        verify(wfsInstanceService).correlateMessage(S1_NOT_NEEDED, BUSINESS_KEY, Collections.<String, Object> emptyMap());
    }

    @Test
    public void whenRbsConfigLevelChangesToGPSSUCCESSFULLYMATCHEDthenCorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", GPS_SUCCESSFULLY_MATCHED.toString());
        final Map<String, Object> workflowVariables = new HashMap<>();
        workflowVariables.put(WORKFLOW_VARIABLES_KEY_GPS_CHECK_RESULT, GPS_SUCCESSFULLY_MATCHED.toString());

        rbsConfigurationEventProcessor.processNotification(payload);

        verify(wfsInstanceService).correlateMessage(GPS_CHECK_POSITION_COMPLETE, BUSINESS_KEY, workflowVariables);
    }

    @Test
    public void whenRbsConfigLevelChangesToGPSMISMATCHERRORthenorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", GPS_MISMATCH_ERROR.toString());
        final Map<String, Object> workflowVariables = new HashMap<>();
        workflowVariables.put(WORKFLOW_VARIABLES_KEY_GPS_CHECK_RESULT, GPS_MISMATCH_ERROR.toString());

        rbsConfigurationEventProcessor.processNotification(payload);

        verify(wfsInstanceService).correlateMessage(GPS_CHECK_POSITION_COMPLETE, BUSINESS_KEY, workflowVariables);
    }

    @Test
    public void whenRbsConfigLevelChangesToGPSPOSITIONUNAVAILABLEthenCorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", GPS_POSITION_UNAVAILABLE.toString());
        final Map<String, Object> workflowVariables = new HashMap<>();
        workflowVariables.put(WORKFLOW_VARIABLES_KEY_GPS_CHECK_RESULT, GPS_POSITION_UNAVAILABLE.toString());

        rbsConfigurationEventProcessor.processNotification(payload);

        verify(wfsInstanceService).correlateMessage(GPS_CHECK_POSITION_COMPLETE, BUSINESS_KEY, workflowVariables);
    }

    @Test
    public void whenRbsConfigLevelChangesToGPSWANTEDPOSITIONNOTSETthenCorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", GPS_WANTED_POSITION_NOT_SET.toString());
        final Map<String, Object> workflowVariables = new HashMap<>();
        workflowVariables.put(WORKFLOW_VARIABLES_KEY_GPS_CHECK_RESULT, GPS_WANTED_POSITION_NOT_SET.toString());

        rbsConfigurationEventProcessor.processNotification(payload);

        verify(wfsInstanceService).correlateMessage(GPS_CHECK_POSITION_COMPLETE, BUSINESS_KEY, workflowVariables);
    }

    @Test
    public void whenRbsConfigLevelChangesToANYOTHERVALUEthenCorrelateMessageNotSentAndNoDPScheckOccurs()
            throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", "AnyOtherValue");
        rbsConfigurationEventProcessor.processNotification(payload);
        verify(wfsInstanceService, never()).correlateMessage(anyString(), anyString(), anyMapOf(String.class, Object.class));
        verify(dpsQueries, never()).findMoByName(anyString(), anyString(), anyString());
    }

    @Test
    public void whenAnyOtherRbsConfigurationAttributeChangesThenCorrelateMessageNotSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload("AnyOtherAttribute", "ANY", S1_COMPLETE);
        rbsConfigurationEventProcessor.processNotification(payload);
        verify(wfsInstanceService, never()).correlateMessage(anyString(), anyString(), anyMapOf(String.class, Object.class));
    }

    @Test
    public void whenNotifcationFdnContainsOssPrefixThenCorrelateMessageSent() throws WorkflowMessageCorrelationException {
        final DpsAttributeChangedEvent payload = createRbsConfigurationPayload(RBS_CONFIG_LEVEL, "ANY", SITE_CONFIG_COMPLETE);
        payload.setFdn("SubNetwork=SubNetwork-1," + RBS_CONFIGURATION_FDN);
        rbsConfigurationEventProcessor.processNotification(payload);
        verify(wfsInstanceService).correlateMessage(SITE_CONFIG_COMPLETE, BUSINESS_KEY, Collections.<String, Object> emptyMap());
    }

    private DpsAttributeChangedEvent createRbsConfigurationPayload(final String name, final String oldValue, final String newValue) {
        final AttributeChangeData attributeChangeData = new AttributeChangeData(name, oldValue, newValue, null, null);
        final Set<AttributeChangeData> dataSet = new HashSet<>();
        dataSet.add(attributeChangeData);

        final DpsAttributeChangedEvent payload = new DpsAttributeChangedEvent();
        payload.setType("RbsConfiguration");
        payload.setChangedAttributes(dataSet);
        payload.setFdn(RBS_CONFIGURATION_FDN);

        return payload;
    }
}