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

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderSPI
import com.ericsson.oss.services.ap.api.status.StateTransitionEvent
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal
import com.ericsson.oss.services.ap.common.cm.TransactionalExecutor
import com.ericsson.oss.services.ap.common.workflow.task.order.OrderFailedTask
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables
import com.ericsson.oss.services.ap.workflow.erbs.task.order.SetupConfigurationTask
import com.ericsson.oss.services.ap.workflow.test.WorkflowUnitTestSpec
import com.ericsson.oss.services.wfs.task.api.TaskExecution
import java.util.concurrent.Callable
import mockit.Mock
import mockit.MockUp
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.RequiredHistoryLevel

import javax.inject.Inject

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat

/**
 * Unit tests for order erbs workflow.
 */
class OrderErbsWorkflowSpec extends WorkflowUnitTestSpec {

    private static final String ORDER_WORKFLOW_NAME = "order_erbs_d.1.44_4"
    private static final String HARDWARE_SERIAL_NUMBER = "ABC1234567"

    static def orderSuccessWithoutOptionalTasks = ["Setup_Configuration",
        "Validate_Configuration", "AddNode", "AssignTargetGroup",
        "CreateFileArtifact__type_SiteBasic", "CreateFileArtifact__type_SiteEquipment",
        "CreateFileArtifact__type_RbsSummary", "CreateFileArtifact__type_SiteInstallation",
        "BindNode", "DHCPConfiguration", "OrderIntegrationSuccess"]

    static def orderSuccessWithOptionalTasks = ["Setup_Configuration", "Import_License_Key_File",
        "Validate_Configuration", "AddNode", "AssignTargetGroup", "GenerateSecurity",
        "CreateFileArtifact__type_SiteBasic", "CreateFileArtifact__type_SiteEquipment",
        "CreateFileArtifact__type_RbsSummary", "CreateFileArtifact__type_SiteInstallation",
        "CreateNodeCredentials", "BindNode", "DHCPConfiguration", "OrderIntegrationSuccess"]

    def failingWorkflowTasks = [
        "importLKF" : { workflowTaskFacade.importLicenseKeyFile(AP_NODE_FDN) >> new Exception("Import LKF Failed") },
        "validation" : { workflowTaskFacade.validateBulkConfiguration(AP_NODE_FDN) >> new Exception("Validation Failed") },
        "addNode" : { workflowTaskFacade.addNode(AP_NODE_FDN) >> new Exception("Add Node Failed") },
        "assignTargetGroups" : { workflowTaskFacade.assignTargetGroups(AP_NODE_FDN) >> new Exception("Assign target groups Failed") },
        "generateSecurity" : { workflowTaskFacade.enableSecurity(AP_NODE_FDN, CppNodeType.ERBS) >> new Exception("Generate Security Failed") },
        "generateArtifact" : { workflowTaskFacade.createGeneratedArtifact(*_, AP_NODE_FDN, CppNodeType.ERBS) >> new Exception("Generate Artifact Failed") },
        "bind" : { workflowTaskFacade.bindNodeDuringOrder(AP_NODE_FDN, HARDWARE_SERIAL_NUMBER, CppNodeType.ERBS) >> new Exception("Generate Artifact Failed") }
    ]

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
    }

    @Deployment(resources = "erbs/d.1.44/order_erbs_d.1.44_4.bpmn")
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when execute order workflow optional task execution is dependent on workflow variables"() {

        given: "User orders a node with or without optional tasks"
            mockWorkflowTasks(isCreateNodeUserCredentials, isSecurityEnabled, isImportLkf)

        when: "order workflow executes"
            final ProcessInstance processInstance = startWorkflow(ORDER_WORKFLOW_NAME, Collections.EMPTY_MAP)

        then: "all expected tasks succeed"
            assertThat(processInstance).hasPassedInOrder(hasPassedInOrderTasks as String[])
            assertThat(processInstance).hasNotPassed(hasNotPassedTasks as String[])
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.ORDER_SUCCESSFUL)

        where:
            isCreateNodeUserCredentials | isSecurityEnabled | isImportLkf || hasPassedInOrderTasks            | hasNotPassedTasks
            false                       | false             | false       || orderSuccessWithoutOptionalTasks | ["Import_License_Key_File", "GenerateSecurity", "CreateNodeCredentials"]
            true                        | true              | true        || orderSuccessWithOptionalTasks    | [""]
    }

    @Deployment(resources = "erbs/d.1.44/order_erbs_d.1.44_4.bpmn")
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when task in order workflow fails workflow is rolled back"() {
        given: "a workflow task will fail"
            failingWorkflowTasks[failingTask].call()

        when: "order workflow executes"
            final ProcessInstance processInstance = startWorkflow(ORDER_WORKFLOW_NAME, Collections.EMPTY_MAP)

        then: "order fails, and rollback tasks are invoked"
            assertThat(processInstance).hasPassedInOrder(hasPassedInOrderTasks as String[])
            assertThat(processInstance).hasNotPassed(hasNotPassedTasks as String[])
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.ORDER_FAILED)

        where:
            failingTask          || hasNotPassedTasks                      | hasPassedInOrderTasks
            "importLKF"          || ["Validate_Configuration"]             | ["Setup_Configuration", "Import_License_Key_File", "OrderIntegrationFailed"]
            "validation"         || ["AddNode"]                            | ["Setup_Configuration", "Import_License_Key_File", "Validate_Configuration", "DeleteLicenseKeyFile", "OrderIntegrationFailed"]
            "addNode"            || ["AssignTargetGroup"]                  | ["Setup_Configuration", "Import_License_Key_File", "Validate_Configuration", "AddNode", "DeleteLicenseKeyFile", "OrderIntegrationFailed"]
            "assignTargetGroups" || ["GenerateSecurity"]                   | ["Setup_Configuration", "Import_License_Key_File", "Validate_Configuration", "AddNode", "AssignTargetGroup", "RemoveNode", "DeleteLicenseKeyFile", "OrderIntegrationFailed"]
            "generateSecurity"   || ["CreateFileArtifact__type_SiteBasic"] | ["Setup_Configuration", "Import_License_Key_File", "Validate_Configuration", "AddNode", "GenerateSecurity","RemoveNode", "DeleteLicenseKeyFile", "OrderIntegrationFailed"]
            "generateArtifact"   || ["BindNode"]                           | ["Setup_Configuration", "Import_License_Key_File", "Validate_Configuration", "AddNode", "GenerateSecurity", "CreateFileArtifact__type_SiteBasic", "RemoveNode", "DeleteLicenseKeyFile", "OrderIntegrationFailed"]
            "bind"               || ["DHCPConfiguration"]                  | ["Setup_Configuration", "Import_License_Key_File", "Validate_Configuration", "AddNode", "GenerateSecurity", "CreateFileArtifact__type_SiteBasic", "BindNode", "DeleteFileArtifact__type_SiteEquipment", "RemoveNode", "DeleteLicenseKeyFile", "OrderIntegrationFailed"]
    }

    @Deployment(resources = "erbs/d.1.44/order_erbs_d.1.44_4.bpmn")
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when order rollback fails the state is order rollback failed"() {
        given: "create artifact task will fail"
            workflowTaskFacade.createGeneratedArtifact(*_, AP_NODE_FDN, CppNodeType.ERBS) >> new Exception("Generate Artifact Failed")

        and: "remove node task during rollback will fail"
            workflowTaskFacade.removeNode(AP_NODE_FDN) >> new Exception("Add Node Failed")

        when: "order workflow executes"
            final ProcessInstance processInstance = startWorkflow(ORDER_WORKFLOW_NAME, Collections.EMPTY_MAP)

        then: "order fails, and rollback tasks are invoked, and order rollback fails"
            assertThat(processInstance).hasPassedInOrder("Setup_Configuration", "Import_License_Key_File", "Validate_Configuration", "AddNode", "GenerateSecurity", "CreateFileArtifact__type_SiteBasic", "RemoveNode", "DeleteLicenseKeyFile", "OrderIntegrationFailed")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.ORDER_ROLLBACK_FAILED)
    }

    def mockWorkflowTasks(createNodeUserCredentials, generateSecurity, importLKF) {
        new MockUp<SetupConfigurationTask>() {
            @Mock
            ErbsWorkflowVariables createErbsWorkflowVariables(TaskExecution execution) {
                ErbsWorkflowVariables.newInstance([createUserCredentials: createNodeUserCredentials, securityEnabled: generateSecurity,
                                                   importLicenseKeyFile: importLKF, apNodeFdn: AP_NODE_FDN, nodeType: "ERBS",
                                                   validationRequired: true, hardwareSerialNumber: HARDWARE_SERIAL_NUMBER])
            }
        }
        new MockUp<TransactionalExecutor>() {
            @Mock
            <T> T execute(Callable<T> callable) throws Exception { }
        }
    }
}