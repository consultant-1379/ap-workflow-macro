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

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.StatusEntryProgress;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs.DummyTargetGroupManagementInternalService;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;
import ru.yandex.qatools.allure.annotations.Stories;

import javax.inject.Inject;

import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.googlecode.catchexception.CatchException.verifyException;

/**
 * Arquillian test class to test the AddTargetToTargetGroupTask method.
 */
public class AddTargetToTargetGroupTest extends Arquillian {

    private static final String ASSIGN_TARGET_GROUP = "Assign Target Groups";

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Test
    @Stories("WHEN TargetGroup are added to Nodeinfo a call to add the node to TargetGroup isdone "
            + "THEN task is visible AND in completed state")
    public void when_TargetGroup_are_added_response_is_success_then_task_visible_and_completed() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options_targetGroup().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.add_cpp_node(nodeMo.getFdn());
        workflowTaskEjbTestSteps.addTargetToTargetGroup(nodeMo.getFdn());
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), ASSIGN_TARGET_GROUP);

        Assertions.assertThat(taskStatus)
                .as("Expected task progress to be Completed")
                .contains("Completed");
    }

    @Test
    @Stories("WHEN TargetGroup are added to Nodeinfo a call to add the node to TargetGroup is done but exception is thrown and retry limit exceeded "
            + "THEN task is visible AND in failed state")
    public void when_TargetGroup_are_added_response_is_failure_5_times_then_task_visible_and_failed() {
        DummyTargetGroupManagementInternalService.throwExceptionOnAddTargetsToTargetGroup = true;
        DummyTargetGroupManagementInternalService.throwExceptionOnAddTargetsToTargetGroupRetryCount = 5;
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options_targetGroup().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.add_cpp_node(nodeMo.getFdn());
        verifyException(workflowTaskEjbTestSteps).addTargetToTargetGroup(nodeMo.getFdn());
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), ASSIGN_TARGET_GROUP);

        Assertions.assertThat(taskStatus)
                .as("Task Status should not be null")
                .isNotNull();
        Assertions.assertThat(taskStatus)
                .as("Expected task progress to be Failed")
                .contains(StatusEntryProgress.FAILED.toString());
        Assertions.assertThat(taskStatus)
                .as("Additional Info contains failure reason only")
                .contains("additionalInfo\":\"Cannot add Target Group");
    }

    @Test
    @Stories("WHEN TargetGroup are added to Nodeinfo a call to add the node to TargetGroup and exception not marked as retirable is thrown "
            + "THEN task is visible AND in failed state")
    public void when_TargetGroup_are_added_andnot_retriable_exception_thrown_then_task_visible_and_failed() {
        DummyTargetGroupManagementInternalService.throwExceptionOnAddTargetsToTargetGroup = true;
        DummyTargetGroupManagementInternalService.throwExceptionOnAddTargetsToTargetGroupRetryCount = 0;
        DummyTargetGroupManagementInternalService.throwNonRetriableException = true;
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options_targetGroup().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.add_cpp_node(nodeMo.getFdn());
        verifyException(workflowTaskEjbTestSteps).addTargetToTargetGroup(nodeMo.getFdn());
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), ASSIGN_TARGET_GROUP);

        Assertions.assertThat(taskStatus)
                .as("Task Status should not be null")
                .isNotNull();
        Assertions.assertThat(taskStatus)
                .as("Expected task progress to be Failed")
                .contains(StatusEntryProgress.FAILED.toString());
        Assertions.assertThat(taskStatus)
                .as("Additional Info contains failure reason only")
                .contains("additionalInfo\":\"Cannot add Target Group");
    }

    @Test
    @Stories("WHEN TargetGroup are added to Nodeinfo a call to add the node to TargetGroup is done and exception but retry limit not exceeded "
            + "THEN task is visible AND in completed state")
    public void when_TargetGroup_are_added_response_is_failure_4_times_then_task_visible_and_failed() {
        DummyTargetGroupManagementInternalService.throwExceptionOnAddTargetsToTargetGroup = true;
        DummyTargetGroupManagementInternalService.throwNonRetriableException = false;
        DummyTargetGroupManagementInternalService.throwExceptionOnAddTargetsToTargetGroupRetryCount = 4;
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options_targetGroup().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.add_cpp_node(nodeMo.getFdn());
        workflowTaskEjbTestSteps.addTargetToTargetGroup(nodeMo.getFdn());
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), ASSIGN_TARGET_GROUP);

        Assertions.assertThat(taskStatus)
                .as("Task Status should not be null")
                .isNotNull();
        Assertions.assertThat(taskStatus)
                .as("Expected task progress to be Completed")
                .contains("Completed");
    }

    @Test
    @Stories("WHEN TargetGroup are not added to Nodeinfo a call to add the node to TargetGroup is not made"
            + "THEN task is not visible")
    public void when_TargetGroup_are_not_added_then_task_is_not_visible() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.add_cpp_node(nodeMo.getFdn());
        workflowTaskEjbTestSteps.addTargetToTargetGroup(nodeMo.getFdn());
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), ASSIGN_TARGET_GROUP);

        Assertions.assertThat(taskStatus).isNull();
    }

}

