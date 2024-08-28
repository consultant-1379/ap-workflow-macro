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
 * This service task is responsible for unlocking the EUtranCellFDD and EUtranCellTDD MOs on a node.
 */
public class UnlockCellsTask extends AbstractServiceTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private DdpTimer ddpTimer = new DdpTimer(); // NOPMD
    private ServiceFinderBean serviceFinder = new ServiceFinderBean(); // NOPMD

    @Override
    public void executeTask(final TaskExecution execution) {
        ddpTimer.start(CommandLogName.UNLOCK_CELLS.toString());
        final ErbsWorkflowVariables workflowVariables = (ErbsWorkflowVariables) execution.getVariable(WORKFLOW_VARIABLES_KEY);
        final String apNodeFdn = workflowVariables.getApNodeFdn();

        logger.info("Executing {} for node {}", this.getClass().getSimpleName(), apNodeFdn);

        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);

        try {
            workflowTaskFacade.unlockCells(apNodeFdn, workflowVariables.getMeContextFdn());
            workflowVariables.setOptionalFeaturesOrUnlockCellsExecuted(true);
            ddpTimer.end(apNodeFdn);
        } catch (final Exception e) {
            logger.warn("Error executing {} for node {}: {}", getClass().getSimpleName(), workflowVariables.getApNodeFdn(), e.getMessage(), e);
            workflowVariables.flagFailureOnOptionalFeaturesOrUnlockCells();
            ddpTimer.endWithError(apNodeFdn);
        }
    }
}
