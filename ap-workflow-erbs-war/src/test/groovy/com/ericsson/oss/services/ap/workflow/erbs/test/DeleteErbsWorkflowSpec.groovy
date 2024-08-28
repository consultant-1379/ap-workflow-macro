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

import javax.inject.Inject
import mockit.Mock
import mockit.MockUp
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.RequiredHistoryLevel
import org.junit.Test

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderSPI
import com.ericsson.oss.services.ap.api.status.StateTransitionEvent
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal
import com.ericsson.oss.services.ap.common.workflow.task.order.OrderFailedTask
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables
import com.ericsson.oss.services.ap.workflow.erbs.task.order.SetupConfigurationTask
import com.ericsson.oss.services.ap.workflow.test.WorkflowUnitTestSpec
import com.ericsson.oss.services.wfs.task.api.TaskExecution

/**
 * Unit tests for delete erbs workflow.
 */
class DeleteErbsWorkflowSpec extends WorkflowUnitTestSpec {

    private static final String DELETE_WORKFLOW_NAME = "delete_erbs_d.1.44_4"

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

    @Deployment(resources = "erbs/d.1.44/delete_erbs_d.1.44_4.bpmn")
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when cancel security or remove node fail delete workflow fails and state is delete failed"() {

        given: "workflow variables are set and delete workflow tasks will fail"
            mockSetupConfigurationTask(true)
            final Map<String, Object> workflowVariables = ["deleteIgnoresNetworkElement" : false]
            workflowTaskFacade.cancelSecurity(AP_NODE_FDN, CppNodeType.ERBS) >> cancelSecurityResponse
            workflowTaskFacade.removeNode(AP_NODE_FDN) >> removeNodeResponse

        when: "delete workflow executes"
            final ProcessInstance processInstance = startWorkflow(DELETE_WORKFLOW_NAME, workflowVariables)

        then: "all tasks complete and state is set to delete failed"
            assertThat(processInstance).hasPassedInOrder("SetupConfiguration",
                "CancelSecurity", "RemoveNode", "DHCP_RemoveClientConfiguration")
            1 * stateTransitionManagerLocal.validateAndSetNextState(AP_NODE_FDN, StateTransitionEvent.DELETE_FAILED)

        where:
            cancelSecurityResponse                  | removeNodeResponse
            new Exception("Cancel Security Failed") | void
            void                                    | new Exception("Remove Node Failed")
    }

    @Deployment(resources = "erbs/d.1.44/delete_erbs_d.1.44_4.bpmn")
    @RequiredHistoryLevel(value = ProcessEngineConfiguration.HISTORY_ACTIVITY)
    def "when delete executes without error, workflow succeeds and tasks completed based on workflow variables"() {
        given: "workflow variables are set"
            mockSetupConfigurationTask(isSecurityEnabled)
            final Map<String, Object> workflowVariables = ["deleteIgnoresNetworkElement" : deleteIgnoresNetworkElement]

        when: "delete workflow executes"
            final ProcessInstance processInstance = startWorkflow(DELETE_WORKFLOW_NAME, workflowVariables)

        then: "all tasks succeed"
            assertThat(processInstance).hasPassedInOrder(passedTasks as String[])
            assertThat(processInstance).hasNotPassed(notPassedTasks as String[])

        where:
            isSecurityEnabled | deleteIgnoresNetworkElement || passedTasks                                                                             | notPassedTasks
            false             | false                       || ["SetupConfiguration", "RemoveNode", "DHCP_RemoveClientConfiguration"]                  | ["CancelSecurity"]
            true              | true                        || ["SetupConfiguration", "DHCP_RemoveClientConfiguration"]                                | ["CancelSecurity", "RemoveNode"]
            true              | false                       || ["SetupConfiguration", "CancelSecurity", "RemoveNode", "DHCP_RemoveClientConfiguration"]| ["all tasks succeed"]
    }

    def mockSetupConfigurationTask(boolean isSecurityEnabled) {
        new MockUp<SetupConfigurationTask>() {
            @Mock
            ErbsWorkflowVariables createErbsWorkflowVariables(final TaskExecution execution) {
                return ErbsWorkflowVariables.newInstance([apNodeFdn: AP_NODE_FDN, nodeType: "ERBS", securityEnabled: isSecurityEnabled])
            }
        }
    }
}
