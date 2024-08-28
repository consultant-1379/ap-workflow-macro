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
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_CHECK_POSITION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.task.api.AbstractServiceTask;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * This service task is responsible to initiate GPS position check for the node
 */
public class InitiateGPSPositionCheckTask extends AbstractServiceTask {

    private final DdpTimer ddpTimer = new DdpTimer();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ServiceFinderBean serviceFinder = new ServiceFinderBean();

    @Override
    public void executeTask(final TaskExecution execution) {
        ddpTimer.start(CommandLogName.GPS_POSITION_CHECK.toString());
        final ErbsWorkflowVariables workflowVariables = (ErbsWorkflowVariables) execution.getVariable(WORKFLOW_VARIABLES_KEY);
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        final String apNodeFdn = workflowVariables.getApNodeFdn();

        logger.info("Executing {} for node {}", this.getClass().getSimpleName(), apNodeFdn);

        try {
            workflowTaskFacade.initiateGpsPositionCheck(apNodeFdn, workflowVariables.getMeContextFdn());
        } catch (final Exception e) {
            logger.warn("Error executing {} for node {}: Failed to update rbsConfigLevel to {}: {}", getClass().getSimpleName(),
                    workflowVariables.getApNodeFdn(), GPS_CHECK_POSITION, e.getMessage(), e);
        }
        ddpTimer.end(apNodeFdn);
    }
}
