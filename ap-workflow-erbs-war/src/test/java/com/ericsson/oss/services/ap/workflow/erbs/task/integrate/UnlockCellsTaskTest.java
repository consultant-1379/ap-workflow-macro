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
package com.ericsson.oss.services.ap.workflow.erbs.task.integrate;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * Unit tests for {@link UnlockCellsTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UnlockCellsTaskTest {

    private static final String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String TASK_ID = "taskId";

    private final ErbsWorkflowVariables workflowVariables = new ErbsWorkflowVariables();

    @Mock
    private ServiceFinderBean serviceFinder;

    @Mock
    private WorkflowTaskFacade workflowTaskFacade;

    @Mock
    private TaskExecution taskExecution;

    @Mock
    private DdpTimer ddpTimer; //NOPMD

    @InjectMocks
    private UnlockCellsTask unlockCellsTask;

    @Before
    public void setUp() {
        workflowVariables.setApNodeFdn(NODE_FDN);
        workflowVariables.setUserId("test");

        when(taskExecution.getVariable(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY)).thenReturn(workflowVariables);
        when(taskExecution.getTaskId()).thenReturn(TASK_ID);
        when(serviceFinder.find(WorkflowTaskFacade.class)).thenReturn(workflowTaskFacade);
    }

    @Test
    public void when_task_fails_then_creation_of_second_cv_is_disabled() {
        doThrow(ApServiceException.class).when(workflowTaskFacade).unlockCells(NODE_FDN, MECONTEXT_FDN);
        unlockCellsTask.executeTask(taskExecution);
        assertFalse(workflowVariables.shouldCreateSecondCV());
    }

    @Test
    public void when_task_fails_then_isIntegrationTaskWarning_equals_true() {
        doThrow(ApServiceException.class).when(workflowTaskFacade).unlockCells(NODE_FDN, MECONTEXT_FDN);
        unlockCellsTask.executeTask(taskExecution);
        assertTrue(workflowVariables.isIntegrationTaskWarning());
    }
}