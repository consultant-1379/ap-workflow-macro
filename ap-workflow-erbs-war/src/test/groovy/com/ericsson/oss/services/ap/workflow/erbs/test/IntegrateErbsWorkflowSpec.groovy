/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.test

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat

import java.util.concurrent.Callable

import javax.inject.Inject

import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.RequiredHistoryLevel

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderSPI
import com.ericsson.oss.services.ap.api.status.StateTransitionEvent
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal
import com.ericsson.oss.services.ap.common.cm.TransactionalExecutor
import com.ericsson.oss.services.ap.common.model.SupervisionMoType
import com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables
import com.ericsson.oss.services.ap.workflow.erbs.task.integrate.IntegrationSuccessTask
import com.ericsson.oss.services.wfs.task.api.TaskExecution

import mockit.Mock
import mockit.MockUp

/**
 * Unit tests for integrate workflow.
 */
class IntegrateErbsWorkflowSpec extends ErbsWorkflowUnitTestSpec {

    private static final String INTEGRATE_WORKFLOW_NAME = "integrate_erbs_d.1.44_4"

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

        new MockUp<TransactionalExecutor>() {
            @Mock
            <T> T execute(Callable<T> callable) throws Exception { }
        }
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when all options enabled integration completes successfully"() throws Exception {

        given: "integrate with all options enabled"
            final Map<String, Object> workflowVariables = new HashMap<>()
            final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
            erbsWorkflowVariables.setUnlockCells(true)
            erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
            erbsWorkflowVariables.setNodeType("ERBS")
            erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
            erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
            erbsWorkflowVariables.setActivateOptionalFeatures(true)
            workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

        when: "workflow executes"
            ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
            waitForTask("site_config_complete", processInstance)

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
            assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
                "enable_supervision", "create_cv_after_import", "activate_optional_features", "gps_position_check_completion", "unlock_cells",
                "set_integrate_complete", "create_cv", "create_cv_after_integration", "upload_cv_after_integration",
                "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL)
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when all options disabled integration completes successfully"() throws Exception {

        given: "integrate with all options disabled"
            final Map<String, Object> workflowVariables = new HashMap<>()
            final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
            erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
            erbsWorkflowVariables.setNodeType("ERBS")
            erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, false)
            erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(false)
            erbsWorkflowVariables.setActivateOptionalFeatures(false)
            erbsWorkflowVariables.setUnlockCells(false)
            workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

        when: "workflow executes"
            ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
            waitForTask("site_config_complete", processInstance)

        and: "siteConfigComplete message correlated"
            correlateMessage("SITE_CONFIG_COMPLETE")

        and: "Import configurations completes"
            waitForTask("wait_for_import_config_complete", processInstance)
            executeImportConfigurations(true, processInstance)

        and: "s1Complete message correlated"
            waitForTask("wait_for_s1_complete_or_not_needed", processInstance)
            correlateMessage("S1_COMPLETE")

        and: "gps position check message correlated"
            waitForTask("gps_position_check_completion", processInstance)
            correlateMessage("GPS_POSITION_CHECK_COMPLETE")

        then: "integration succeeds"
            assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
                "create_cv_after_import", "gps_position_check_completion", "set_integrate_complete", "create_cv", "Configure_Management_State_Normal",
                "dhcp_remove_client_configuration", "IntegrationSuccess")
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when all options enabled and S1 not needed integration completes successfully"() throws Exception {

        given: "integrate with all options enabled"
            final Map<String, Object> workflowVariables = new HashMap<>()
            final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
            erbsWorkflowVariables.setUnlockCells(true)
            erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
            erbsWorkflowVariables.setNodeType("ERBS")
            erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
            erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
            erbsWorkflowVariables.setActivateOptionalFeatures(true)
            workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

        when: "workflow executes"
            ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
            waitForTask("site_config_complete", processInstance)

        and: "siteConfigComplete message correlated"
            correlateMessage("SITE_CONFIG_COMPLETE")

        and: "Import configurations completes"
            waitForTask("wait_for_import_config_complete", processInstance)
            executeImportConfigurations(true, processInstance)

        and: "s1Complete message correlated"
            waitForTask("wait_for_s1_complete_or_not_needed", processInstance)
            correlateMessage("S1_NOT_NEEDED")

        and: "activate optional features message correlated"
            waitForTask("activate_optional_features_completion", processInstance)
            correlateMessage("ACTIVATE_OPTIONAL_FEATURES_COMPLETION")

        then: "integration succeeds"
            assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_not_needed",
                "enable_supervision", "create_cv_after_import", "activate_optional_features","set_integrate_complete", "create_cv", "create_cv_after_integration",
                "upload_cv_after_integration", "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "WHEN the integration workflow is executed WITH all integration options disabled AND S1 not needed THEN only one cv is created"() throws Exception {

        given: "integrate with all options disabled"
            final Map<String, Object> workflowVariables = new HashMap<>()
            final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
            erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
            erbsWorkflowVariables.setNodeType("ERBS")
            erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, false)
            erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(false)
            erbsWorkflowVariables.setActivateOptionalFeatures(false)
            erbsWorkflowVariables.setUnlockCells(false)
            workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

        when: "workflow executes"
            ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
            waitForTask("site_config_complete", processInstance)

        and: "siteConfigComplete message correlated"
            correlateMessage("SITE_CONFIG_COMPLETE")

        and: "Import configurations completes"
            waitForTask("wait_for_import_config_complete", processInstance)
            executeImportConfigurations(true, processInstance)

        and: "s1Complete message correlated"
            waitForTask("wait_for_s1_complete_or_not_needed", processInstance)
            correlateMessage("S1_NOT_NEEDED")

        then: "integration succeeds"
            assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_not_needed",
                "create_cv_after_import", "set_integrate_complete", "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when unlock cells fails then create cv after integration does not execute and integration completes with warning"() {
        given: "integrate with all options enabled"
        final Map<String, Object> workflowVariables = new HashMap<>()
        final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
        erbsWorkflowVariables.setUnlockCells(true)
        erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
        erbsWorkflowVariables.setNodeType("ERBS")
        erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
        erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
        erbsWorkflowVariables.setActivateOptionalFeatures(true)
        workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

    when: "workflow executes with unlock cell failure"
        workflowTaskFacade.unlockCells(AP_NODE_FDN, "MeContext=Node1") >> new Exception("Unlock cells failed")
        ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
        waitForTask("site_config_complete", processInstance)

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
        assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
            "enable_supervision", "create_cv_after_import", "activate_optional_features", "gps_position_check_completion", "unlock_cells",
            "set_integrate_complete", "create_cv",
            "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
        1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL_WITH_WARNING)
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when GPS unavailable then unlock cells and create cv after integration do not complete and integration completes with warning"() {
        given: "integrate with all options enabled"
        final Map<String, Object> workflowVariables = new HashMap<>()
        final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
        erbsWorkflowVariables.setUnlockCells(true)
        erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
        erbsWorkflowVariables.setNodeType("ERBS")
        erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
        erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
        erbsWorkflowVariables.setActivateOptionalFeatures(true)
        workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

    when: "workflow executes"
        ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
        waitForTask("site_config_complete", processInstance)

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
        final Map<String, String> gpsMap = new HashMap<>();
        gpsMap.put("gps_check_result", "GPS_POSITION_UNAVAILABLE")
        correlateMessage("GPS_POSITION_CHECK_COMPLETE", gpsMap)

    then: "integration succeeds"
        assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
            "enable_supervision", "create_cv_after_import", "activate_optional_features", "gps_position_check_completion",
            "set_integrate_complete", "create_cv",
            "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
        1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL_WITH_WARNING)
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when GPS_WANTED_POSITION_NOT_SET then integration succeeds"() {
        given: "integrate with all options enabled"
        final Map<String, Object> workflowVariables = new HashMap<>()
        final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
        erbsWorkflowVariables.setUnlockCells(true)
        erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
        erbsWorkflowVariables.setNodeType("ERBS")
        erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
        erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
        erbsWorkflowVariables.setActivateOptionalFeatures(true)
        workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

    when: "workflow executes"
        ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
        waitForTask("site_config_complete", processInstance)

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
        final Map<String, String> gpsMap = new HashMap<>();
        gpsMap.put("gps_check_result", "GPS_WANTED_POSITION_NOT_SET")
        correlateMessage("GPS_POSITION_CHECK_COMPLETE", gpsMap)

    then: "integration succeeds"
        assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
                "enable_supervision", "create_cv_after_import", "activate_optional_features", "gps_position_check_completion", "unlock_cells",
                "set_integrate_complete", "create_cv", "create_cv_after_integration", "upload_cv_after_integration",
                "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL)
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when create cv fails after integration then upload cv does not execute and integration is successful with warning"() throws Exception {

        given: "integrate with all options enabled"
            final Map<String, Object> workflowVariables = new HashMap<>()
            final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
            erbsWorkflowVariables.setUnlockCells(true)
            erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
            erbsWorkflowVariables.setNodeType("ERBS")
            erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
            erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
            erbsWorkflowVariables.setActivateOptionalFeatures(true)
            workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

        when: "workflow executes"
            workflowTaskFacade.createCV(AP_NODE_FDN, "MeContext=Node1", "Created by AP after integration") >> { throw new Exception("Create cv after integration failed")}
            ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
            waitForTask("site_config_complete", processInstance)

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
            assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
                "enable_supervision", "create_cv_after_import", "activate_optional_features", "gps_position_check_completion", "unlock_cells",
                "set_integrate_complete", "create_cv", "create_cv_after_integration",
                "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL_WITH_WARNING)
    }

    @Deployment(resources = ["erbs/d.1.44/integrate_erbs_d.1.44_4.bpmn"])
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when create cv fails after import then upload cv does execute and integration is successful with warning"() throws Exception {

        given: "integrate with all options enabled"
            final Map<String, Object> workflowVariables = new HashMap<>()
            final ErbsWorkflowVariables erbsWorkflowVariables = new ErbsWorkflowVariables()
            erbsWorkflowVariables.setUnlockCells(true)
            erbsWorkflowVariables.setApNodeFdn(AP_NODE_FDN)
            erbsWorkflowVariables.setNodeType("ERBS")
            erbsWorkflowVariables.setEnableSupervision(SupervisionMoType.FM, true)
            erbsWorkflowVariables.setUploadCvAfterIntegrationEnabled(true)
            erbsWorkflowVariables.setActivateOptionalFeatures(true)
            workflowVariables.put(AbstractWorkflowVariables.WORKFLOW_VARIABLES_KEY, erbsWorkflowVariables)

        when: "workflow executes"
            workflowTaskFacade.createCV(AP_NODE_FDN, "MeContext=Node1", "Created by AP after import") >> { throw new Exception("Create cv after import failed")}
            ProcessInstance processInstance = startWorkflow(INTEGRATE_WORKFLOW_NAME, workflowVariables)
            waitForTask("site_config_complete", processInstance)

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
            assertThat(processInstance).hasPassedInOrder("initiate_node_synchronization", "site_config_complete", "wait_for_import_config_complete", "s1_complete",
                "enable_supervision", "create_cv_after_import", "activate_optional_features", "gps_position_check_completion", "unlock_cells",
                "set_integrate_complete", "create_cv", "create_cv_after_integration", "upload_cv_after_integration",
                "Configure_Management_State_Normal", "dhcp_remove_client_configuration", "IntegrationSuccess")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.INTEGRATION_SUCCESSFUL_WITH_WARNING)
    }

}
