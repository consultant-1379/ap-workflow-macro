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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test;

import static com.ericsson.oss.services.ap.api.status.State.INTEGRATION_COMPLETED;
import static com.ericsson.oss.services.ap.api.status.State.INTEGRATION_COMPLETED_WITH_WARNING;
import static com.ericsson.oss.services.ap.api.status.State.ORDER_COMPLETED;
import static com.ericsson.oss.services.ap.arquillian.util.data.validation.DpsAssert.assertThat;
import static com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener.getIntegrationCompleteStates;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newCreateErbsNodeMosFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newGpsPositionSuccessfullyMatchedFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newGpsPositionUnavailableFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newGpsWantedPositionNotSetFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newNodeStateIntegrationStartedFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newS1CompleteFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newSiteConfigCompleteFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Jndi;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesResult;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowServiceTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * This class tests the Call Activity workflow definition, 'integrate_erbs.bpmn', which contains the BPMN logic for integrating an ERBS node. It
 * executes this workflow instance directly, (i.e. not as a Call Activity from the parent workflow).
 */
public class IntegrateErbsWorkflowTest extends Arquillian {

    private static final String S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME = "S1 Complete or S1 Not Needed Notification";

    @Inject
    private Dps dps;

    @Inject
    private Jndi jndi;

    @Inject
    private WorkflowServiceTestSteps wfsSteps;

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    /**
     * Covered by IntegrateErbsWorkflowSpec
     */
    @Test
    @Features("AP Integrate")
    @Stories({ "WHEN integrate ERbs workflow is executed WITH nbiot Cell having invalid EutranCellRef, then Integration completes with warnings" })
    public void when_integrate_erbs_workflow_is_executed_with_nbiot_Cell_having_invalid_EutranCellRef_then_Integration_completes_with_warnings() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_valid_configurations()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_COMPLETED);
        dataSteps.create_me_context_and_all_erbs_node_mos_with_flawed_nbiot_cell(node.getName());

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newNodeStateIntegrationStartedFunction(dps))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newCreateErbsNodeMosFunction(dataSteps))
            .onStart(S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME, newS1CompleteFunction(dps))
            .onStart(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), newGpsPositionSuccessfullyMatchedFunction(dps));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), getIntegrationCompleteStates());

        final WorkflowInstance workflowInstance = wfsSteps.execute_integrate_erbs_workflow_with_license_options_disabled(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(statusEntries.getSuccessfulEntries()).containsOnly(
            StatusEntryNames.SYNC_NODE.toString(),
            StatusEntryNames.SITE_CONFIG_COMPLETE.toString(),
            StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString(),
            S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME,
            StatusEntryNames.CREATE_CV_TASK.toString(),
            StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(),
            StatusEntryNames.SET_MANAGEMENT_STATE.toString());

        assertThat(statusEntries.getFailedEntries()).contains(StatusEntryNames.UNLOCK_CELLS.toString());
        assertThat(dps).withManagedObject(nodeStateFdn).withAttributeValue("state", INTEGRATION_COMPLETED_WITH_WARNING.toString());
        final String rbsConfigLevelFdn = "MeContext=" + node.getName() + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1";
        assertThat(dps).withManagedObject(rbsConfigLevelFdn).withAttributeValue("rbsConfigLevel", "INTEGRATION_COMPLETE");
    }

    /**
     * Covered by IntegrateErbsWorkflowSpec
     */
    @Test
    @Features("AP Integrate")
    @Stories({
        "WHEN integrate ERbs workflow AND GPS position unavailable THEN Integration Succeeds with Warning AND cells remain locked AS Unlock Cells is not_executed" })
    public void when_integrate_erbs_workflow_and_gps_position_unavailable_then_integration_succeeds_with_warning_and_cells_remain_locked_as_unlock_cells_is_not_executed() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_valid_configurations()
            .with_default_erbs_auto_integration_options()
            .with_default_supervision_options()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_COMPLETED);

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newNodeStateIntegrationStartedFunction(dps))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newCreateErbsNodeMosFunction(dataSteps))
            .onStart(S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME, newS1CompleteFunction(dps))
            .onStart(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), newGpsPositionUnavailableFunction(dps));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), getIntegrationCompleteStates());

        final WorkflowInstance workflowInstance = wfsSteps.execute_integrate_erbs_workflow_with_license_unlockCells_options_disabled(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(statusEntries.getSuccessfulEntries()).contains(
            StatusEntryNames.SYNC_NODE.toString(),
            StatusEntryNames.SITE_CONFIG_COMPLETE.toString(),
            StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString(),
            S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME,
            StatusEntryNames.CREATE_CV_TASK.toString());

        assertThat(statusEntries.getFailedEntries()).containsOnly(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString());
        assertThat(dps).withManagedObject(nodeStateFdn).withAttributeValue("state", INTEGRATION_COMPLETED_WITH_WARNING.toString());
    }

    /**
     * Covered by IntegrateErbsWorkflowSpec
     */
    @Test
    @Features("AP Integrate")
    @Stories({ "WHEN integrate ERbs workflow AND GPS wanted position not set THEN integration succeeds" })
    public void when_integrate_erbs_workflow_and_gps_wanted_position_not_set_then_integration_succeeds() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_auto_integration_options()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_COMPLETED);

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newNodeStateIntegrationStartedFunction(dps))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newCreateErbsNodeMosFunction(dataSteps))
            .onStart(S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME, newS1CompleteFunction(dps))
            .onStart(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), newGpsWantedPositionNotSetFunction(dps));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), getIntegrationCompleteStates());

        final WorkflowInstance workflowInstance = wfsSteps.execute_integrate_erbs_workflow_with_all_options_disabled(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(statusEntries.getSuccessfulEntries()).containsOnly(
            StatusEntryNames.SYNC_NODE.toString(),
            StatusEntryNames.SITE_CONFIG_COMPLETE.toString(),
            StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString(),
            S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME,
            StatusEntryNames.CREATE_CV_TASK.toString(),
            StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(),
            StatusEntryNames.SET_MANAGEMENT_STATE.toString());

        assertThat(statusEntries.getFailedEntries()).isEmpty();
        assertThat(dps).withManagedObject(nodeStateFdn).withAttributeValue("state", INTEGRATION_COMPLETED.toString());
    }

    /**
     * Covered by IntegrateErbsWorkflowSpec
     */
    @Test
    @Features("AP Integrate")
    @Stories({
        "WHEN the integration workflow is executed WITH Create CV Failure THEN integration succeeds with warning and upload CV is not executed" })
    public void when_create_cv_failure_then_integration_succeeds_with_warning_and_upload_cv_is_not_executed() {
        stubsSteps.create_flawed_cv_management_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_auto_integration_options()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.create_me_context_and_all_erbs_node_mos(node.getName());
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_COMPLETED);

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newNodeStateIntegrationStartedFunction(dps))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newCreateErbsNodeMosFunction(dataSteps))
            .onStart(S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME, newS1CompleteFunction(dps))
            .onStart(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), newGpsPositionSuccessfullyMatchedFunction(dps));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), getIntegrationCompleteStates());

        final ErbsWorkflowVariables workflowVariables = wfsSteps.create_successful_with_warning_workflow_variables(node.getFdn());
        final WorkflowInstance workflowInstance = wfsSteps.execute_integrate_erbs_workflow(node, workflowVariables);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(statusEntries.getSuccessfulEntries()).contains(
            StatusEntryNames.SYNC_NODE.toString(),
            StatusEntryNames.SITE_CONFIG_COMPLETE.toString(),
            StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString(),
            S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME,
            StatusEntryNames.GPS_POSITION_CHECK_TASK.toString());

        assertThat(statusEntries.getFailedEntries()).contains(StatusEntryNames.CREATE_CV_TASK.toString(), StatusEntryNames.CREATE_CV_TASK.toString());
        assertThat(dps).withManagedObject(nodeStateFdn).withAttributeValue("state", INTEGRATION_COMPLETED_WITH_WARNING.toString());
    }

    /**
     * Covered by IntegrateErbsWorkflowSpec
     */
    @Test
    @Features("AP Integrate")
    @Stories({
        "WHEN the integration workflow is executed WITH Create CV After Integration Failure THEN Integration Succeeds with Warning AND upload CV is not executed" })
    public void when_create_cv_after_import_succeeds_and_after_integration_fails_then_integration_succeeds_with_warning_and_upload_cv_is_not_executed() {
        stubsSteps.create_flawed_after_integration_cv_management_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_auto_integration_options()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_COMPLETED);

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newNodeStateIntegrationStartedFunction(dps))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newCreateErbsNodeMosFunction(dataSteps))
            .onStart(S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME, newS1CompleteFunction(dps))
            .onStart(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), newGpsPositionSuccessfullyMatchedFunction(dps));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), getIntegrationCompleteStates());

        final ErbsWorkflowVariables workflowVariables = wfsSteps
            .create_successful_with_warning_workflow_variables_with_unlock_cells_enabled(node.getFdn());
        final WorkflowInstance workflowInstance = wfsSteps.execute_integrate_erbs_workflow(node, workflowVariables);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(statusEntries.getSuccessfulEntries()).containsOnly(
            StatusEntryNames.SYNC_NODE.toString(),
            StatusEntryNames.SITE_CONFIG_COMPLETE.toString(),
            StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString(),
            S1_COMPLETE_OR_NOT_NEEDED_STATUS_ENTRY_NAME,
            StatusEntryNames.UNLOCK_CELLS.toString(),
            StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(),
            StatusEntryNames.CREATE_CV_TASK.toString(),
            StatusEntryNames.SET_MANAGEMENT_STATE.toString());

        assertThat(statusEntries.getFailedEntries()).containsOnly(StatusEntryNames.CREATE_CV_TASK.toString());
        assertThat(dps).withManagedObject(nodeStateFdn).withAttributeValue("state", INTEGRATION_COMPLETED_WITH_WARNING.toString());
    }

    /**
     * Covered by EndToEndErbsWorkflowSpec
     */
    @Test
    @Features("AP Integrate")
    @Stories({ "WHEN the integration workflow is executed WITH Invalid RN file THEN integration fails" })
    public void when_integrate_erbs_workflow_is_executed_with_Invalid_RN_file_then_integration_fails() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_invalid_configurations_rn_file()
            .with_default_erbs_auto_integration_options()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.update_node_state_mo(node.getFdn(), ORDER_COMPLETED);

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newCreateErbsNodeMosFunction(dataSteps));
        statusEntriesListener.listenUntilEntryCompletes(node.getFdn(), StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString());

        final ErbsWorkflowVariables workflowVariables = wfsSteps.create_successful_erbs_workflow_variables(node.getFdn());
        final WorkflowInstance workflowInstance = wfsSteps.execute_integrate_erbs_workflow(node, workflowVariables);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(statusEntries.getSuccessfulEntries()).containsExactlyInAnyOrder(
            StatusEntryNames.SYNC_NODE.toString(),
            StatusEntryNames.SITE_CONFIG_COMPLETE.toString());

        assertThat(statusEntries.getFailedEntries()).containsExactly(StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString());

        final String rbsConfigLevelFdn = "MeContext=" + node.getName() + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1";
        assertThat(dps).withManagedObject(rbsConfigLevelFdn).withAttributeValue("rbsConfigLevel", "OSS_CONFIGURATION_FAILED");
    }
}