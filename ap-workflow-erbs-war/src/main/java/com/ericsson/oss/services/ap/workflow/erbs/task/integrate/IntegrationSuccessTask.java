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

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.status.State;
import com.ericsson.oss.services.ap.api.status.StateTransitionEvent;
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.model.NodeAttribute;
import com.ericsson.oss.services.ap.common.model.NodeStatusAttribute;
import com.ericsson.oss.services.ap.common.workflow.recording.CommandRecorder;
import com.ericsson.oss.services.ap.common.workflow.recording.ZeroTouchRecorder;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.task.api.AbstractServiceTask;
import com.ericsson.oss.services.wfs.task.api.TaskExecution;

/**
 * This service task sets the AP node state to <b>INTEGRATION_COMPLETED</b> or <b>INTEGRATION_COMPLETED_WITH_WARNING</b>.
 * <p>
 * Also cleans up the system of leftover AP files.
 */
public class IntegrationSuccessTask extends AbstractServiceTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CommandRecorder commandRecorder = new CommandRecorder(); // NOPMD
    private DpsOperations dpsOperations = new DpsOperations(); // NOPMD
    private ServiceFinderBean serviceFinder = new ServiceFinderBean(); // NOPMD
    private ZeroTouchRecorder zeroTouchRecorder = new ZeroTouchRecorder();

    @Override
    public void executeTask(final TaskExecution execution) {
        final ErbsWorkflowVariables workflowVariables = (ErbsWorkflowVariables) execution.getVariable(WORKFLOW_VARIABLES_KEY);
        final String apNodeFdn = workflowVariables.getApNodeFdn();
        logger.info("Executing {} for node {}", getClass().getSimpleName(), apNodeFdn);

        cleanUp(workflowVariables);
        setNodeStatusSuccessful(apNodeFdn, workflowVariables);
        commandRecorder.integrationSuccessful(workflowVariables);
        dpsOperations.updateMo(apNodeFdn, NodeAttribute.ACTIVE_WORKFLOW_INSTANCE_ID.toString(), null);
        dpsOperations.updateMo(buildNodeStatusFdn(apNodeFdn), NodeStatusAttribute.END_STATE_DATE.toString(),
            LocalDate.now().toString());
    }

    private void cleanUp(final ErbsWorkflowVariables workflowVariables) {
        try {
            final WorkflowTaskFacade workflowTask = serviceFinder.find(WorkflowTaskFacade.class);
            workflowTask.cleanUpOnCompletion(workflowVariables.getApNodeFdn(), workflowVariables.isSecurityEnabled(), CppNodeType.ERBS);
        } catch (final Exception e) {
            logger.warn("Exception occurred during cleanup after integration", e);
        }
    }

    private void setNodeStatusSuccessful(final String apNodeFdn, final ErbsWorkflowVariables workflowVariables) {
        final StateTransitionManagerLocal stateTransitionManager = serviceFinder.find(StateTransitionManagerLocal.class);
        final StatusEntryManagerLocal statusEntryManager = serviceFinder.find(StatusEntryManagerLocal.class);

        if (workflowVariables.isIntegrationTaskWarning()) {
            statusEntryManager.printNodeState(apNodeFdn, State.INTEGRATION_COMPLETED_WITH_WARNING);
            stateTransitionManager.validateAndSetNextState(apNodeFdn, StateTransitionEvent.INTEGRATION_SUCCESSFUL_WITH_WARNING);
        } else {
            statusEntryManager.printNodeState(apNodeFdn, State.INTEGRATION_COMPLETED);
            stateTransitionManager.validateAndSetNextState(apNodeFdn, StateTransitionEvent.INTEGRATION_SUCCESSFUL);
            zeroTouchRecorder.processResponseTime(apNodeFdn, statusEntryManager);
        }
    }

    private static String buildNodeStatusFdn(final String apNodeFdn) {
        return String.format("%s%s", apNodeFdn, ",NodeStatus=1");
    }
}
