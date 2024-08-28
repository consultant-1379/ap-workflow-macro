/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.test

import java.util.concurrent.Callable

import javax.inject.Inject
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.RequiredHistoryLevel
import org.junit.Test
import mockit.Mock
import mockit.MockUp
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderSPI
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal
import com.ericsson.oss.services.ap.common.cm.TransactionalExecutor
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables
import com.ericsson.oss.services.ap.workflow.erbs.task.order.SetupConfigurationTask
import com.ericsson.oss.services.wfs.task.api.TaskExecution

class EndToEndErbsWorkflowSpec {

    private static final String WORKFLOW_NAME = "auto_integrate_erbs_d.1.44_4"
    private static final String HARDWARE_SERIAL_NUMBER = "ABC1234567"

    @MockedImplementation
    ServiceFinderSPI serviceFinderSPI

    @Inject
    private StatusEntryManagerLocal statusEntryManager

    @Inject
    private StateTransitionManagerLocal stateTransitionManagerLocal

    @MockedImplementation
    private WorkflowTaskFacade workflowTaskFacade

    def setup() {
        ServiceFinderBean.serviceFinder = this.serviceFinderSPI
        serviceFinderSPI.find(StatusEntryManagerLocal.class, null) >> statusEntryManager
        serviceFinderSPI.find(StateTransitionManagerLocal.class, null) >> stateTransitionManagerLocal
        serviceFinderSPI.find(WorkflowTaskFacade.class, null) >> workflowTaskFacade
        
        new MockUp<SetupConfigurationTask>() {
            @Mock
            ErbsWorkflowVariables createErbsWorkflowVariables(TaskExecution execution) {
                final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
                erbsWorkflowVariables.setCreateUserCredentials(true)
                erbsWorkflowVariables.setSecurityEnabled(true)
                erbsWorkflowVariables.setImportLicenseKeyFile(true)
                erbsWorkflowVariables.setValidationRequired(true)
                erbsWorkflowVariables.setUnlockCells(true)
                erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
                erbsWorkflowVariables.setNodeType("ERBS")
                erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
                erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
                erbsWorkflowVariables.setActivateOptionalFeatures(true)
                return erbsWorkflowVariables
            }
        }
        new MockUp<TransactionalExecutor>() {
            @Mock
            <T> T execute(Callable<T> callable) throws Exception { }
        }
    }
    
    @Deployment(resources = ["erbs/d.1.44/auto_integrate_erbs_d.1.44_4.bpmn", "erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn", "erbs/d.1.44/order_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when integrating node then auto-integration succeeds"() {
        
        given: "User orders a node with or with optional tasks"
        
        when: "order workflow executes"
            final ProcessInstance processInstance = startWorkflow(END_TO_END_WORKFLOW_NAME)
            executeJob()

        then: "node state order completed"
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.ORDER_SUCCESSFUL)

        and: "node up notification is received"
            waitForTask("wait_for_nodeup", processInstance)
            correlateMessage("NODE_UP", workflowVariables)
            waitForTask("Integrate", processInstance)
            executeJob()
        
        and: "siteConfigComplete message correlated"
            correlateMessage("SITE_CONFIG_COMPLETE")

        and: "Import configurations completes"
            waitForTask("wait_for_import_config_complete", processInstance)
            executeImportConfigurations(true, processInstance)

        and: "s1Complete message correlated"
            waitForTask("wait_for_s1_complete_or_not_needed", processInstance)
            correlateMessage("S1_COMPLETE")

        and: "activate optional features message correlated"
            waitForTask("activate_optional_features_completion", processInstance)
            correlateMessage("ACTIVATE_OPTIONAL_FEATURES_COMPLETION")


        and: "gps position check message correlated"
            waitForTask("gps_position_check_completion", processInstance)
            correlateMessage("GPS_POSITION_CHECK_COMPLETE")

        then: "integration succeeds"
            assertThat(processInstance).hasPassedInOrder(
                "Setup_Configuration", "Import_License_Key_File",
                "Validate_Configuration", "AddNode", "AssignTargetGroup", "GenerateSecurity",
                "CreateFileArtifact__type_SiteBasic", "CreateFileArtifact__type_SiteEquipment",
                "CreateFileArtifact__type_RbsSummary", "CreateFileArtifact__type_SiteInstallation",
                "CreateNodeCredentials", "BindNode", "DHCPConfiguration", "OrderIntegrationSuccess","initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
                "enable_supervision", "create_cv_after_import", "activate_optional_features", "gps_position_check_completion", "unlock_cells",
                "set_integrate_complete", "create_cv", "create_cv_after_integration", "upload_cv_after_integration",
                "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL)
        
    }

    @Deployment(resources = ["erbs/d.1.44/auto_integrate_erbs_d.1.44_4.bpmn", "erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn", "erbs/d.1.44/order_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when import configurations fails then integration fails"() throws Exception {

        given: "integrate with all options enabled"

        when: "order workflow executes"
            final ProcessInstance processInstance = startWorkflow(END_TO_END_WORKFLOW_NAME)
            executeJob()

        then: "node state order completed"
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.ORDER_SUCCESSFUL)

        and: "node up notification is received"
            waitForTask("wait_for_nodeup", processInstance)
            correlateMessage("NODE_UP", workflowVariables)
            waitForTask("Integrate", processInstance)
            executeJob()

        and: "siteConfigComplete message correlated"
            correlateMessage("SITE_CONFIG_COMPLETE")

        and: "Import configurations fails"
           waitForTask("wait_for_import_config_complete", processInstance)
            executeImportConfigurations(false, processInstance)

        then: "integration fails"
            assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete",
                "Import Configurations Failed", "auto_integration_failed")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_FAILED)
    }
}
