/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test;

import static com.ericsson.oss.services.ap.api.status.State.DELETE_FAILED;
import static com.ericsson.oss.services.ap.api.status.State.DELETE_STARTED;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.validation.DpsAssert.assertThat;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesResult;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowServiceTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Arquillian test class to test the ERBS delete workflow.
 */
public class DeleteErbsWorkflowTest extends Arquillian {

    private static final String NETWORK_ELEMENT_MO = "NetworkElement=%s";

    @Inject
    private Dps dps;

    @Inject
    private WorkflowServiceTestSteps wfsSteps;

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    /**
     * Covered by DeleteErbsWorkflowSpec
     */
    @Test
    @Features("AP Delete")
    @Stories({ "WHEN node_deleted THEN networkElement deleted and security cancelled" })
    public void when_delete_in_state_order_completed_then_networkElement_deleted_and_security_cancelled() {
        stubsSteps.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_erbs_auto_integration_options()
                .with_default_supervision_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.update_node_state_mo(nodeMo.getFdn(), DELETE_STARTED);

        dataSteps.create_network_element_mo(nodeMo.getName());

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps);
        statusEntriesListener.listenUntilEntryCompletes(nodeMo.getFdn(), StatusEntryNames.REMOVE_NODE_TASK.toString());
        wfsSteps.execute_delete_workflow(nodeMo, false, CppNodeType.ERBS);

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(dps).hasNotManagedObject(NETWORK_ELEMENT_MO, nodeMo.getName());

        assertThat(statusEntries.getFailedEntries()).as("No task should fail").isEmpty();

        assertThat(statusEntries.getSuccessfulEntries()).as("All tasks should succeed").containsExactlyInAnyOrder(
                StatusEntryNames.CANCEL_SECURITY_TASK.toString(),
                StatusEntryNames.REMOVE_NODE_TASK.toString());

        // On successful delete, do not update Status as the Status MO will be deleted, so state remains in Delete Started.
        final String nodeStateFdn = format("%s,NodeStatus=1", nodeMo.getFdn());
        assertThat(dps)
                .withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", DELETE_STARTED.toString());
    }

    /**
     * Covered by DeleteErbsWorkflowSpec
     */
    @Test
    @Features("AP Delete")
    @Stories({ "WHEN cancel security fails THEN delete fails" })
    public void when_cancel_security_fails_then_delete_fails() {
        stubsSteps.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_erbs_auto_integration_options()
                .with_default_supervision_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.update_node_state_mo(nodeMo.getFdn(), DELETE_STARTED);

        dataSteps.create_network_element_mo(nodeMo.getName());

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps);
        statusEntriesListener.listenUntilEntryCompletes(nodeMo.getFdn(), StatusEntryNames.CANCEL_SECURITY_TASK.toString());

        stubsSteps.create_flawed_iscf_service_stub();
        wfsSteps.execute_delete_workflow(nodeMo, false, CppNodeType.ERBS);

        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults();

        assertThat(statusEntries.getFailedEntries()).as("Cancel Security should fail").containsOnly(
            StatusEntryNames.CANCEL_SECURITY_TASK.toString());

        final String nodeStateFdn = format("%s,NodeStatus=1", nodeMo.getFdn());
        assertThat(dps)
                .withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", DELETE_FAILED.toString());
    }
}
