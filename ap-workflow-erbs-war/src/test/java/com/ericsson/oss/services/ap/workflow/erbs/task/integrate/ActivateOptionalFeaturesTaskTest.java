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
 * Unit tests for {@link ActivateOptionalFeaturesTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ActivateOptionalFeaturesTaskTest {

    private static final String MECONTEXT_FDN = "MeContext=" + NODE_NAME;

    private final ErbsWorkflowVariables workflowVariables = new ErbsWorkflowVariables();

    @Mock
    private WorkflowTaskFacade workflowTaskFacade;

    @Mock
    private ServiceFinderBean serviceFinder;

    @Mock
    private TaskExecution taskExecution;

    @Mock
    private DdpTimer ddpTimer; //NOPMD

    @InjectMocks
    private ActivateOptionalFeaturesTask activateOptionalFeaturesTask;

    @Before
    public void setUp() {
        workflowVariables.setApNodeFdn(NODE_FDN);
        when(taskExecution.getVariable(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY)).thenReturn(workflowVariables);
        when(serviceFinder.find(WorkflowTaskFacade.class)).thenReturn(workflowTaskFacade);
    }

    @Test
    public void when_task_fails_then_creation_of_second_cv_is_disabled() {
        doThrow(ApServiceException.class).when(workflowTaskFacade).activateOptionalFeatures(NODE_FDN, MECONTEXT_FDN);
        activateOptionalFeaturesTask.executeTask(taskExecution);
        assertFalse(workflowVariables.shouldCreateSecondCV());
    }

    @Test
    public void when_task_fails_then_isIntegrationTaskWarning_returns_true() {
        doThrow(ApServiceException.class).when(workflowTaskFacade).activateOptionalFeatures(NODE_FDN, MECONTEXT_FDN);
        activateOptionalFeaturesTask.executeTask(taskExecution);
        assertTrue(workflowVariables.isIntegrationTaskWarning());
    }
}