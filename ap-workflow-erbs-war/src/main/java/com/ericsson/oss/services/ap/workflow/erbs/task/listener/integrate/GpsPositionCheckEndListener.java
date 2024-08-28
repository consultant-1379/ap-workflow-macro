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
package com.ericsson.oss.services.ap.workflow.erbs.task.listener.integrate;

import static com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY;

import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.ap.workflow.erbs.task.integrate.GpsPositionCheckResultHandler;
import com.ericsson.oss.services.wfs.task.api.AbstractServiceTask;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * Listener attached to the <b>end</b> event for the <code>GPS Position Check</code> BPMN wait point.
 * <p>
 * Calls {@link GpsPositionCheckResultHandler} to handle the received notification message.
 */
public class GpsPositionCheckEndListener extends AbstractServiceTask {

    private final GpsPositionCheckResultHandler gpsResultHandler = new GpsPositionCheckResultHandler();

    @Override
    public void executeTask(final TaskExecution execution) {
        final ErbsWorkflowVariables workflowVariables = (ErbsWorkflowVariables) execution.getVariable(WORKFLOW_VARIABLES_KEY);
        final String messageReceived = (String) execution.getVariable("gps_check_result");
        gpsResultHandler.handleResult(messageReceived, workflowVariables);
    }
}
