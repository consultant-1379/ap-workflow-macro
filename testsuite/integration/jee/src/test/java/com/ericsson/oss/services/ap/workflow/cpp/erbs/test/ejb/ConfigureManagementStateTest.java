/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.ejb;

import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.validation.DpsAssert.assertThat;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;

import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Arquillian test class to test the ConfigureManagementStateTask method.
 */
public class ConfigureManagementStateTest extends Arquillian {
    private static final String MANAGEMENT_STATE = "Set Management State";
    private static final String NETWORK_ELEMENT_MO = "NetworkElement=%s";

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private Dps dpsHelper;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Test
    @Stories("WHEN value of managementState in Supervision Options is not present AND a call to configure Network Elements managementState value to NORMAL is done "
                 + "THEN task is visible AND Network Elements managementState value is set to NORMAL")
    public void when_managementState__not_present_and_configuration_of_ne_managementState_to_NORMAL_done_then_task_visible_and_ne_managementState_set_to_normal() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_supervision_options().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        final String nodeFdn = nodeMo.getFdn();
        workflowTaskEjbTestSteps.add_cpp_node(nodeFdn);
        workflowTaskEjbTestSteps.configureManagementState(nodeFdn, "NORMAL");

        final String taskStatus = dataSteps.get_ap_task_status(nodeFdn, MANAGEMENT_STATE);
        Assertions.assertThat(taskStatus)
            .as("Expected task progress to be Completed")
            .contains("Completed");

        final String supervisionFdn = nodeFdn + ",SupervisionOptions=1";
        assertThat(dpsHelper).withManagedObject(supervisionFdn)
                .withFdnAttributeValue(supervisionFdn, "managementState", "AUTOMATIC");

        final String networkElementMo = String.format(NETWORK_ELEMENT_MO, nodeMo.getName());
        assertThat(dpsHelper).withManagedObject(networkElementMo)
                .withFdnAttributeValue(networkElementMo, "managementState", "NORMAL");
    }

    @Test
    @Stories("WHEN value of managementState in Supervision Options is AUTOMATIC AND a call to configure Network Elements managementState value to NORMAL is done "
                 + "THEN task is visible AND Network Elements managementState value is set to NORMAL")
    public void when_managementState_is_automatic_and_configuration_of_ne_managementState_to_NORMAL_done_then_task_visible_and_ne_managementState_set_to_normal() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().withSupervisionOptionsAutomatic().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.add_cpp_node(nodeMo.getFdn());
        workflowTaskEjbTestSteps.configureManagementState(nodeMo.getFdn(), "NORMAL");

        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), MANAGEMENT_STATE);
        Assertions.assertThat(taskStatus)
            .as("Expected task progress to be Completed")
            .contains("Completed");

        final String supervisionFdn = nodeMo.getFdn() + ",SupervisionOptions=1";
        assertThat(dpsHelper).withManagedObject(supervisionFdn)
                .withFdnAttributeValue(supervisionFdn, "managementState", "AUTOMATIC");

        final String networkElementMo = String.format(NETWORK_ELEMENT_MO, nodeMo.getName());
        assertThat(dpsHelper).withManagedObject(networkElementMo)
                .withFdnAttributeValue(networkElementMo, "managementState", "NORMAL");
    }

    @Test
    @Stories("WHEN value of managementState in Supervision Options is MANUAL AND a call to configure Network Elements managementState value to NORMAL is done "
                 + "THEN task is not visible AND Network Elements managementState value remains in MAINTENANCE")
    public void when_managementState_is_manual_and_configuration_of_ne_managementState_to_normal_done_then_task_not_visible_and_ne_managementState_remain_maintenance() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().withSupervisionOptionsManual().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.add_cpp_node(nodeMo.getFdn());
        workflowTaskEjbTestSteps.configureManagementState(nodeMo.getFdn(), "NORMAL");

        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), MANAGEMENT_STATE);
        Assertions.assertThat(taskStatus).isNull();

        final String supervisionFdn = nodeMo.getFdn() + ",SupervisionOptions=1";
        assertThat(dpsHelper).withManagedObject(supervisionFdn)
                .withFdnAttributeValue(supervisionFdn, "managementState", "MANUAL");

        final String networkElementMo = String.format(NETWORK_ELEMENT_MO, nodeMo.getName());
        assertThat(dpsHelper).withManagedObject(networkElementMo)
                .withFdnAttributeValue(networkElementMo, "managementState", "MAINTENANCE");
    }
}
