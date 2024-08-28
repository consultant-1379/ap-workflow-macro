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
package com.ericsson.oss.services.ap.workflow.erbs.task.integrate;

import static com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.INTEGRATION_COMPLETE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.common.workflow.recording.ErrorRecorder;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.task.api.AbstractServiceTask;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * This service task is used to update the <code>rbsConfigLevel</code> of the node to <b>INTEGRATION_COMPLETE</b>.
 */
public class RbsIntegrationCompleteTask extends AbstractServiceTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServiceFinderBean serviceFinder = new ServiceFinderBean();
    private final ErrorRecorder errorRecorder = new ErrorRecorder();

    @Override
    public void executeTask(final TaskExecution execution) {
        final ErbsWorkflowVariables workflowVariables = (ErbsWorkflowVariables) execution.getVariable(WORKFLOW_VARIABLES_KEY);
        final String apNodeFdn = workflowVariables.getApNodeFdn();

        logger.info("Executing {} for node {}", this.getClass().getSimpleName(), workflowVariables.getApNodeFdn());
        try {
            final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
            workflowTaskFacade.updateRbsConfigLevel(workflowVariables.getMeContextFdn(), INTEGRATION_COMPLETE);
        } catch (final Exception e) {
            errorRecorder.updateRbsConfigLevelFailed(apNodeFdn, RbsConfigLevel.INTEGRATION_COMPLETE.toString(), e);
        }
    }
}
