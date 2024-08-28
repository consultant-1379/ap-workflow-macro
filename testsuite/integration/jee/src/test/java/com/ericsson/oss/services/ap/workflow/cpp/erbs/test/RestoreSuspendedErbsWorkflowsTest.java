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
import static com.ericsson.oss.services.ap.api.status.State.INTEGRATION_FAILED;
import static com.ericsson.oss.services.ap.api.status.State.ORDER_COMPLETED;
import static com.ericsson.oss.services.ap.api.status.State.ORDER_STARTED;
import static com.ericsson.oss.services.ap.api.status.State.UNKNOWN;
import static com.ericsson.oss.services.ap.arquillian.util.data.workflow.WorkflowFunctionsFactory.newCreateCiRefAssociationFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newCreateErbsNodeMosFunctionWithSync;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newCreateNetworkElementChildMosFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newNodeUpFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newSiteConfigCompleteFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.validation.DpsAssert.assertThat;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.validation.WorkflowInstanceAssert.assertThat;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Jndi;
import com.ericsson.oss.services.ap.arquillian.util.data.managedobject.OSSMosGenerator;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.RestoreTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowServiceTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Arquillian tests class to test Restore functionality.
 * <p>
 * Tests have been disabled due to inconsistent failures only seen on Jenkins. If developing on related content, tests should be re-enabled and run
 * locally.
 * <p>
 * Test cases will be covered by TORF-149326, at which point this class can be removed.
 */
public class RestoreSuspendedErbsWorkflowsTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private Dps dps;

    @Inject
    private Jndi jndi;

    @Inject
    private WorkflowServiceTestSteps wfsSteps;

    @Inject
    private RestoreTestSteps restoreTestSteps;

    @Inject
    private WorkflowStubbedServicesSteps stubsStep;

    @Inject
    private WorkflowDataSteps workflowDataTestSteps;

    @Inject
    private OSSMosGenerator ossMosGenerator;

    @Test(enabled = false)
    @Features("AP Restore")
    @Stories({
            "WHEN workflow node is NOT SYNCHED and last restore attempt and waiting for node up THEN state is ORDER_COMPLETED and workflow RESUMED" })
    public void when_workflow_for_unsynched_node_and_last_attempt_and_waiting_for_node_up_then_state_remains_in_order_completed_and_workflow_resumed() {
        stubsStep.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_erbs_auto_integration_options()
                .with_default_supervision_options()
                .build();

        restoreTestSteps.prepare_resource_for_restore();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_STARTED);

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener()
                .onEnd(StatusEntryNames.ADD_NODE_TASK.toString(), newCreateNetworkElementChildMosFunction(dataSteps))
                .onEnd(StatusEntryNames.ADD_NODE_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), StatusEntryNames.NODE_UP.toString());

        final WorkflowInstance workflowInstance = wfsSteps.execute_auto_integrate_erbs_workflow(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        statusEntriesListener.waitForResults();

        restoreTestSteps.suspend_workflow(workflowInstance.getId());

        restoreTestSteps.start_restore();

        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", ORDER_COMPLETED.toString());

        assertThat(workflowInstance)
                .isInActiveState();
    }

    @Test(enabled = false)
    @Features("AP Restore")
    @Stories({
            "WHEN workflow node is NOT SYNCHED and last restore attempt and NOT waiting for node up then state is UNKNOWN and workflow CANCELLED" })
    public void when_workflow_for_unsynched_node_and_last_attempt_not_waiting_for_node_up_then_restore_state_is_unknown_and_workflow_cancelled() {
        stubsStep.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_erbs_auto_integration_options()
                .with_default_supervision_options()
                .build();

        restoreTestSteps.prepare_resource_for_restore();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_STARTED);

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener()
                .onEnd(StatusEntryNames.ADD_NODE_TASK.toString(), newCreateNetworkElementChildMosFunction(dataSteps))

                .onEnd(StatusEntryNames.ADD_NODE_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator))
                .onStart(StatusEntryNames.NODE_UP.toString(), newNodeUpFunction(jndi));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), "INTEGRATION_STARTED");

        final WorkflowInstance workflowInstance = wfsSteps.execute_auto_integrate_erbs_workflow(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        statusEntriesListener.waitForResults();

        restoreTestSteps.suspend_workflow(workflowInstance.getId());

        restoreTestSteps.start_restore();
        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", UNKNOWN.toString());

        assertThat(workflowInstance)
                .isCancelled();
    }

    @Test(enabled = false)
    @Features("AP Restore")
    @Stories({ "WHEN workflow node is SYNCHED and rbs is OSS_CONFIGURATION_FAILED then state is INTEGRATION_FAILED and workflow CANCELLED" })
    public void when_node_synchronized_and_rbs_oss_config_failed_then_workflow_cancelled_and_state_is_integration_failed() {
        stubsStep.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_erbs_auto_integration_options()
                .with_default_supervision_options()
                .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_STARTED);

        restoreTestSteps.prepare_resource_for_restore();

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener()
                .onEnd(StatusEntryNames.GENERATE_SECURITY_TASK.toString(), newCreateErbsNodeMosFunctionWithSync(dataSteps))
                .onEnd(StatusEntryNames.GENERATE_SECURITY_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator))
                .onStart(StatusEntryNames.NODE_UP.toString(), newNodeUpFunction(jndi));
        statusEntriesListener.listenUntilEntryCompletes(node.getFdn(), "S1 Complete or S1 Not Needed Notification");

        final WorkflowInstance workflowInstance = wfsSteps.execute_auto_integrate_erbs_workflow(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        statusEntriesListener.waitForResults();

        workflowDataTestSteps.update_rbs_config_level(node.getName(), "OSS_CONFIGURATION_FAILED");

        restoreTestSteps.suspend_workflow(workflowInstance.getId());
        restoreTestSteps.start_restore();

        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", INTEGRATION_FAILED.toString());

        assertThat(workflowInstance)
                .isCancelled();
    }

    @Test(enabled = false)
    @Features("AP Restore")
    @Stories({ "WHEN workflow node is SYNCHED and rbs is INTEGRATION_COMPLETE then state is INTEGRATION_COMPLETED and workflow CANCELLED" })
    public void when_node_synchronized_and_rbs_integration_complete_then_workflow_cancelled_and_state_integration_complete() {
        stubsStep.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_erbs_auto_integration_options()
                .with_default_supervision_options()
                .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_STARTED);

        restoreTestSteps.prepare_resource_for_restore();

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener()
                .onEnd(StatusEntryNames.GENERATE_SECURITY_TASK.toString(), newCreateErbsNodeMosFunctionWithSync(dataSteps))
                .onEnd(StatusEntryNames.GENERATE_SECURITY_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator))
                .onStart(StatusEntryNames.NODE_UP.toString(), newNodeUpFunction(jndi))
                .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi));
        statusEntriesListener.listenUntilEntryStarts(node.getFdn(), "S1 Complete or S1 Not Needed Notification");

        final WorkflowInstance workflowInstance = wfsSteps.execute_auto_integrate_erbs_workflow(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        statusEntriesListener.waitForResults();

        workflowDataTestSteps.update_rbs_config_level(node.getName(), "INTEGRATION_COMPLETE");

        restoreTestSteps.suspend_workflow(workflowInstance.getId());

        restoreTestSteps.start_restore();

        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", INTEGRATION_COMPLETED.toString());

        assertThat(workflowInstance)
                .isCancelled();
    }

    @Test(enabled = false)
    @Features("AP Restore")
    @Stories({ "WHEN workflow node is SYNCHED and rbs is UNRESOLVABLE then state is UNKNOWN and workflow CANCELLED" })
    public void when_node_synchronized_and_rbs_is_not_mapped_then_workflow_cancelled_and_state_unknown() {
        stubsStep.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_erbs_auto_integration_options()
                .with_default_supervision_options()
                .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), ORDER_STARTED);

        restoreTestSteps.prepare_resource_for_restore();

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener()
                .onEnd(StatusEntryNames.GENERATE_SECURITY_TASK.toString(), newCreateErbsNodeMosFunctionWithSync(dataSteps))
                .onEnd(StatusEntryNames.GENERATE_SECURITY_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator))
                .onStart(StatusEntryNames.NODE_UP.toString(), newNodeUpFunction(jndi));
        statusEntriesListener.listenUntilEntryStarts(node.getFdn(), "S1 Complete or S1 Not Needed Notification");

        final WorkflowInstance workflowInstance = wfsSteps.execute_auto_integrate_erbs_workflow(node);
        dataSteps.update_workflow_instance_id_on_node_mo(node.getFdn(), workflowInstance.getId());

        statusEntriesListener.waitForResults();

        workflowDataTestSteps.update_rbs_config_level(node.getName(), "SITE_CONFIG_COMPLETE");

        restoreTestSteps.suspend_workflow(workflowInstance.getId());
        restoreTestSteps.start_restore();

        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", UNKNOWN.toString());

        assertThat(workflowInstance)
                .isCancelled();
    }
}
