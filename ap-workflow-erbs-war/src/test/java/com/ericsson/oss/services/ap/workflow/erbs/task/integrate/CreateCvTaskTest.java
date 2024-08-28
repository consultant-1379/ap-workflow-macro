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
import static org.junit.Assert.assertEquals;
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

import com.ericsson.oss.itpf.sdk.context.classic.ContextServiceBean;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * Unit tests for {@link CreateCvTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateCvTaskTest {

    private static final String CV_COMMENT = "Created by AP after integration";
    private static final String CV_NAME = "AP";
    private static final String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String TASK_ID = "create_cv_after_integration__prg";

    private final ErbsWorkflowVariables workflowVariables = new ErbsWorkflowVariables();

    @Mock
    private ContextServiceBean contextService; //NOPMD

    @Mock
    private WorkflowTaskFacade workflowTaskFacade;

    @Mock
    private ServiceFinderBean serviceFinder;

    @Mock
    private TaskExecution taskExecution;

    @Mock
    private DdpTimer ddpTimer; //NOPMD

    @InjectMocks
    private CreateCvTask createCvTask;

    @Before
    public void setUp() {
        workflowVariables.setApNodeFdn(NODE_FDN);
        workflowVariables.setUserId("userId");
        when(taskExecution.getVariable(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY)).thenReturn(workflowVariables);
        when(taskExecution.getTaskId()).thenReturn(TASK_ID);
        when(serviceFinder.find(WorkflowTaskFacade.class)).thenReturn(workflowTaskFacade);
    }

    @Test
    public void when_createCV_succeeds_then_cvName_is_added_to_workflow_variables() {
        when(workflowTaskFacade.createCV(NODE_FDN, MECONTEXT_FDN, CV_COMMENT)).thenReturn(CV_NAME);
        createCvTask.executeTask(taskExecution);
        assertEquals(CV_NAME, workflowVariables.getLastBackupName());
    }

    @Test
    public void when_createCV_succceeds_then_isCreateCvSuccessful_flag_equals_True() {
        createCvTask.executeTask(taskExecution);
        assertTrue(workflowVariables.isCreateCvSuccessful());
    }

    @Test
    public void when_createCV_fails_then_isCreateCvSuccessful_flag_equals_False() {
        doThrow(ApServiceException.class).when(workflowTaskFacade).createCV(NODE_FDN, MECONTEXT_FDN, CV_COMMENT);
        createCvTask.executeTask(taskExecution);
        assertFalse(workflowVariables.isCreateCvSuccessful());
    }

    @Test
    public void when_createCV_succceeds_then_isIntegrationTaskWarning_flag_equals_False() {
        createCvTask.executeTask(taskExecution);
        assertFalse(workflowVariables.isIntegrationTaskWarning());
    }

    @Test
    public void when_createCV_fails_then_isIntegrationTaskWarning_flag_equals_True() {
        doThrow(ApServiceException.class).when(workflowTaskFacade).createCV(NODE_FDN, MECONTEXT_FDN, CV_COMMENT);
        createCvTask.executeTask(taskExecution);
        assertTrue(workflowVariables.isIntegrationTaskWarning());
    }
}