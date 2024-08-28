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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps;

import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.ErbsWorkflowVariableBuilder.newErbsWorkflowVariables;
import static java.lang.String.format;

import java.util.Map;

import javax.ejb.EJB;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.Jndi;
import com.ericsson.oss.services.ap.common.model.SupervisionMoType;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;
import com.google.common.collect.ImmutableMap;

import ru.yandex.qatools.allure.annotations.Step;
/**
 * Workflow Service related test steps for all the Workflow arquillian tests.
 */
public class WorkflowServiceTestSteps {

    @Inject
    private Jndi jndi;

    @EJB
    private WorkflowTaskFacade workflowTaskFacade;

    @Step("Executes the 'integrate_erbs' workflow with default successful variables")
    public WorkflowInstance execute_integrate_erbs_workflow(final ManagedObject node) {
        final ErbsWorkflowVariables workflowVariables = create_successful_erbs_workflow_variables(node.getFdn());
        return execute_integrate_erbs_workflow(node, workflowVariables);
    }

    @Step("Executes the 'integrate_erbs' workflow with all options disabled")
    public WorkflowInstance execute_integrate_erbs_workflow_with_all_options_disabled(final ManagedObject node) {
        final ErbsWorkflowVariables workflowVariables = newErbsWorkflowVariables()
                .userId("testUser")
                .apNodeFdn(node.getFdn())
                .installLicenseEnabled(false)
                .activateLicenseEnabled(false)
                .unlockCellsEnabled(false)
                .uploadCvAfterIntegrationEnabled(false)
                .orderSuccessful()
                .build();
        return execute_integrate_erbs_workflow(node, workflowVariables);
    }

    @Step("Executes the 'integrate_erbs' workflow with license and unlock cells options disabled")
    public WorkflowInstance execute_integrate_erbs_workflow_with_license_unlockCells_options_disabled(final ManagedObject node) {
        final ErbsWorkflowVariables workflowVariables = newErbsWorkflowVariables()
                .userId("testUser")
                .apNodeFdn(node.getFdn())
                .installLicenseEnabled(false)
                .activateLicenseEnabled(false)
                .unlockCellsEnabled(false)
                .uploadCvAfterIntegrationEnabled(true)
                .orderSuccessful()
                .build();
        return execute_integrate_erbs_workflow(node, workflowVariables);
    }

    @Step("Executes the 'integrate_erbs' workflow with license options disabled")
    public WorkflowInstance execute_integrate_erbs_workflow_with_license_options_disabled(final ManagedObject node) {
        final ErbsWorkflowVariables workflowVariables = newErbsWorkflowVariables()
                .userId("testUser")
                .apNodeFdn(node.getFdn())
                .installLicenseEnabled(false)
                .activateLicenseEnabled(false)
                .unlockCellsEnabled(true)
                .uploadCvAfterIntegrationEnabled(true)
                .orderSuccessful()
                .build();
        return execute_integrate_erbs_workflow(node, workflowVariables);
    }

    @Step("Executes the 'integrate_erbs' workflow")
    public WorkflowInstance execute_integrate_erbs_workflow(final ManagedObject node, final ErbsWorkflowVariables workflowVariables) {
        final Map<String, Object> variables = ImmutableMap.of("WorkflowVariables", (Object) workflowVariables);
        final String businessKey = format("AP_Node=%s", node.getName());
        return get_workflow_service().startWorkflowInstanceByDefinitionId("integrate_erbs_d.1.44_4", businessKey, variables);
    }

    @Step("Executes the 'auto_integrate_erbs' workflow")
    public WorkflowInstance execute_auto_integrate_erbs_workflow(final ManagedObject node) {
        final Map<String, Object> variables = ImmutableMap.of("fdn", (Object) node.getFdn(), "useCaseStartTime", 0L);

        final String businessKey = format("AP_Node=%s", node.getName());

        return get_workflow_service().startWorkflowInstanceByDefinitionId("auto_integrate_erbs_d.1.44_4", businessKey, variables);
    }

    @Step("Executes the delete workflow")
    public WorkflowInstance execute_delete_workflow(final ManagedObject node, final boolean ignoreNetworkElement, final CppNodeType nodeType) {
        final Map<String, Object> variables = ImmutableMap.of("fdn", (Object) node.getFdn(), "deleteIgnoresNetworkElement", ignoreNetworkElement,
                "useCaseStartTime", 0L);

        final String businessKey = format("AP_Node=%s", node.getName());

        final String workflowName = nodeType.toString().equalsIgnoreCase("erbs") ? "delete_erbs_d.1.44_4" : "delete_rbs";
        return get_workflow_service().startWorkflowInstanceByDefinitionId(workflowName, businessKey, variables);
    }

    @Step("Create Workflow Variables for successful integration")
    public ErbsWorkflowVariables create_successful_erbs_workflow_variables(final String nodeFdn) {
        return newErbsWorkflowVariables()
                .userId("testUser")
                .apNodeFdn(nodeFdn)
                .installLicenseEnabled(true)
                .activateLicenseEnabled(true)
                .unlockCellsEnabled(true)
                .uploadCvAfterIntegrationEnabled(true)
                .orderSuccessful()
                .setDhcpConfiguration()
                .serialHardwareNumber("SCB8765484")
                .oldHardwareSerialNumber("SCB8765484")
                .build();
    }

    @Step("Create Workflow Variables for successful integration with integrationTaskWarning")
    public ErbsWorkflowVariables create_successful_with_warning_workflow_variables(final String nodeFdn) {
        return newErbsWorkflowVariables()
                .userId("testUser")
                .apNodeFdn(nodeFdn)
                .installLicenseEnabled(false)
                .activateLicenseEnabled(false)
                .unlockCellsEnabled(false)
                .uploadCvAfterIntegrationEnabled(true)
                .orderSuccessful()
                .integrationTaskWarning(true)
                .setDhcpConfiguration()
                .serialHardwareNumber("SCB8765484")
                .oldHardwareSerialNumber("SCB8765484")
                .build();
    }

    @Step("Create Workflow Variables for successful integration with integrationTaskWarning with unlock cells enabled")
    public ErbsWorkflowVariables create_successful_with_warning_workflow_variables_with_unlock_cells_enabled(final String nodeFdn) {
        return newErbsWorkflowVariables()
                .userId("testUser")
                .apNodeFdn(nodeFdn)
                .installLicenseEnabled(false)
                .activateLicenseEnabled(false)
                .unlockCellsEnabled(true)
                .uploadCvAfterIntegrationEnabled(true)
                .orderSuccessful()
                .integrationTaskWarning(true)
                .build();
    }

    private WorkflowInstanceServiceLocal get_workflow_service() {
        return jndi.lookupByProperty("wfs.instance.lookup.name");
    }

    @Step("Enable supervision options")
    public void enable_supervision_options(final String apNodeFdn, final SupervisionMoType supervisionMoType) {
        workflowTaskFacade.enableSupervision(apNodeFdn, supervisionMoType);
    }
}