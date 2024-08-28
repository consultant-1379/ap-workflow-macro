/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
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
import static com.googlecode.catchexception.CatchException.verifyException;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.CommonArtifactDefinitions;

public class UserCredentialsTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    private static final String NODE_CREDENTIAL_SECURE_PASSWORD = "password";

    @Test
    public void when_create_user_credentials_successful_then() {
        stubsSteps.create_credential_service_stub();

        final Map<String, Object> nodeCredentialData = dataSteps.get_encrypted_node_credential_data(NODE_CREDENTIAL_SECURE_PASSWORD);
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_user_node_credentials(nodeCredentialData)
                .build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        // Execute the test
        workflowTaskEjbTestSteps.create_node_user_credentials(nodeMo.getFdn());

        // Verify the task status updates to Completed
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.CREATE_NODE_USER_CREDENTIALS.toString());

        assertThat(taskStatus)
                .as("Task Status should not be null")
                .isNotNull();
        assertThat(taskStatus)
                .contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");
    }

    @Test
    public void when_create_user_credentials_unsuccessful_then_exception_handled_and_task_fails() {
        stubsSteps.create_flawed_credential_service_stub();

        final Map<String, Object> nodeCredentialData = dataSteps.get_encrypted_node_credential_data(NODE_CREDENTIAL_SECURE_PASSWORD);
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_user_node_credentials(nodeCredentialData)
                .build();

        // Execute the test
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        // Verify the result
        verifyException(workflowTaskEjbTestSteps).create_node_user_credentials(nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.CREATE_NODE_USER_CREDENTIALS.toString());
        assertThat(taskStatus).as("Task Status should not be null")
                .isNotNull();
        assertThat(taskStatus).contains("Failed")
                .as("Expected task progress to be Failed");

    }
}
