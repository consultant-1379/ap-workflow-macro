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

import static com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.context.classic.ContextConstants;
import com.ericsson.oss.itpf.sdk.context.classic.ContextServiceBean;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.task.api.AbstractServiceTask;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * Service Task to create Configuration Version (CV).
 * <p>
 * It constructs the CV type, creates the CV itself, then sets the CV to <b>Startable</b> and <b>First in Rollback List</b>.
 */
public class CreateCvTask extends AbstractServiceTask {

    private static final String CREATED_BY_AP_AFTER = "Created by AP after ";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private DdpTimer ddpTimer = new DdpTimer(); // NOPMD
    private ServiceFinderBean serviceFinder = new ServiceFinderBean(); // NOPMD
    private ContextServiceBean contextService = new ContextServiceBean(); // NOPMD

    @Override
    public void executeTask(final TaskExecution execution) {
        final ErbsWorkflowVariables workflowVariables = (ErbsWorkflowVariables) execution.getVariable(WORKFLOW_VARIABLES_KEY);
        setUserIdContext(workflowVariables);
        ddpTimer.start(CommandLogName.CREATE_CV.toString());
        
        final String cvComment = CREATED_BY_AP_AFTER + execution.getTaskId().split("_")[3];
        final String apNodeFdn = workflowVariables.getApNodeFdn();

        logger.info("Executing {} for node {}", this.getClass().getSimpleName(), workflowVariables.getApNodeFdn());

        try {
            final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
            final String createdCvName = workflowTaskFacade.createCV(apNodeFdn, workflowVariables.getMeContextFdn(), cvComment);

            workflowVariables.setLastBackupName(createdCvName);
            workflowVariables.setCreateCvSuccessful(true);
            ddpTimer.end(apNodeFdn);
        } catch (final Exception e) {
            logger.warn("Error executing {} for node {}: {}", getClass().getSimpleName(), workflowVariables.getApNodeFdn(), e.getMessage(), e);
            workflowVariables.setCreateCvSuccessful(false);
            workflowVariables.setIntegrationTaskWarning(true);
            ddpTimer.endWithError(apNodeFdn);
        }
    }

    private void setUserIdContext(final AbstractWorkflowVariables workflowVariables) {
        final String userId = workflowVariables.getUserId();
        contextService.setContextValue(ContextConstants.HTTP_HEADER_USERNAME_KEY, userId);
    }
}