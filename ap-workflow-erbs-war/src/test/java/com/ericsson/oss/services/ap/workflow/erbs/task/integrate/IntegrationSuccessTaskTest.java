/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.task.integrate;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.ap.api.status.State;
import com.ericsson.oss.services.ap.api.status.StateTransitionEvent;
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.cm.NodeOperations;
import com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables;
import com.ericsson.oss.services.ap.common.workflow.recording.CommandRecorder;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * Unit tests for {@link IntegrationSuccessTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IntegrationSuccessTaskTest {

    private final ErbsWorkflowVariables workflowVariables = new ErbsWorkflowVariables();

    @Mock
    private CommandRecorder commandRecorder; // NOPMD

    @Mock
    private DpsOperations dpsOperations; // NOPMD

    @Mock
    private ServiceFinderBean serviceFinder;

    @Mock
    private StateTransitionManagerLocal stateTransitionManager;

    @Mock
    private StatusEntryManagerLocal statusEntryManager;

    @Mock
    private TaskExecution taskExecution;

    @Mock
    private NodeOperations nodeOperations; // NOPMD

    @Mock
    private WorkflowTaskFacade workflowTaskFacade;

    @InjectMocks
    private IntegrationSuccessTask integrationSuccessTask;

    @Mock
    private SystemRecorder systemRecorder; // NOPWD

    @Before
    public void setUp() {
        workflowVariables.setApNodeFdn(NODE_FDN);
        when(taskExecution.getVariable(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY)).thenReturn(workflowVariables);
        when(serviceFinder.find(WorkflowTaskFacade.class)).thenReturn(workflowTaskFacade);
        when(serviceFinder.find(StateTransitionManagerLocal.class)).thenReturn(stateTransitionManager);
        when(serviceFinder.find(StatusEntryManagerLocal.class)).thenReturn(statusEntryManager);
    }

    @Test
    public void whenIntegrationSuccessful_andNoWarnings_thenApNodeStateChangesToIntegrationCompleted_andStateIsPrinted() {
        workflowVariables.setIntegrationTaskWarning(false);
        integrationSuccessTask.executeTask(taskExecution);
        verify(statusEntryManager, times(1)).printNodeState(NODE_FDN, State.INTEGRATION_COMPLETED);
        verify(stateTransitionManager, times(1)).validateAndSetNextState(NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL);
    }

    @Test
    public void whenIntegrationSuccessful_andHasWarnings_thenApNodeStateChangesToIntegrationCompletedWithWarnings_andStateIsPrinted() {
        workflowVariables.setIntegrationTaskWarning(true);
        integrationSuccessTask.executeTask(taskExecution);
        verify(statusEntryManager, times(1)).printNodeState(NODE_FDN, State.INTEGRATION_COMPLETED_WITH_WARNING);
        verify(stateTransitionManager, times(1)).validateAndSetNextState(NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL_WITH_WARNING);
    }

    @Test
    public void whenIntegrationSuccessful_andCleanupFails_thenNoExceptionIsPropagated() {
        doThrow(Exception.class).when(workflowTaskFacade).cleanUpOnCompletion(anyString(), anyBoolean(), eq(CppNodeType.ERBS));
        integrationSuccessTask.executeTask(taskExecution);
    }
}