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

import static com.ericsson.oss.services.ap.arquillian.util.data.validation.DpsAssert.assertThat;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.CatchException.verifyException;
import java.io.File;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.exception.ArtifactNotFoundException;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Files;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.CommonArtifactDefinitions;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

public class RbsSummaryArtifactTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private Dps dpsHelper;

    @Inject
    private Files files;

    private static final String GENERATED_RBS_SUMMARY_ARTIFACT_NAME = "AutoIntegrationRbsSummaryFile.xml";
    private static final String DELETE_RBS_SUMMARY_TASK_NAME = "Delete Rbs Summary File";

    @Test
    public void when_create_RbsSummary_file_executed_successfully_then_the_file_is_generated_on_smrs() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_erbs_artifacts()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);

        createOtherNodeArtifactFiles(nodeMo);

        // Execute the test
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.RBSSUMMARY.toString(), nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.GENERATE_PROVISIONING_ARTIFACTS.toString());

        Assertions.assertThat(taskStatus)
                .isNotNull()
                .as("Task Status should not be null");
        Assertions.assertThat(taskStatus)
                .contains("Started")
                .as("Expected task progress to be Started");

        // Verify the file generation
        final File smrsArtifactsDirectory = files.getNodeArtifactFolder(nodeMo.getName(), CommonArtifactDefinitions.SMRS_ERBS_DIRECTORY_TYPE);
        final String rbsSummaryArtifactId = getNodeArtifactId(nodeMo);

        assertThat(dpsHelper).withManagedObject(CommonArtifactDefinitions.NODE_ARTIFACT_FDN, nodeMo.getFdn(), rbsSummaryArtifactId)
                .as("The generatedLocation attribute in the Node Artifact MO was not updated as expected")
                .withNotEmptyAttributeValue("generatedLocation")
                .withAttributeValue("generatedLocation", smrsArtifactsDirectory.getAbsolutePath() + "/" + GENERATED_RBS_SUMMARY_ARTIFACT_NAME);

        Assertions.assertThat(smrsArtifactsDirectory)
                .as("Generated Rbs Summary file was not generated as expected at smrs location: " + smrsArtifactsDirectory.getAbsolutePath())
                .exists();

    }

    @Test
    public void when_create_RbsSummary_file_failed_then_artifact_not_found_exception_is_thrown() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().without_site_basic_artifact()
                .build(); // RbsSummary needs the SiteBasic file, so leave it out to force an error

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        verifyException(workflowTaskEjbTestSteps).create_generated_artifact(ArtifactType.RBSSUMMARY.toString(), nodeMo.getFdn());

        Assertions.assertThat(caughtException().getCause()
                .getClass())
                .isEqualTo(ArtifactNotFoundException.class)
                .as("ArtifactNotFoundException was not thrown as expected");
    }

    @Test
    public void when_delete_RbsSummary_file_executed_successfully_then_the_artifact_is_deleted_from_smrs() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_erbs_artifacts()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createOtherNodeArtifactFiles(nodeMo);

        // Create the rbs summary so we can test deletion
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.RBSSUMMARY.toString(), nodeMo.getFdn());

        // Execute the test method
        workflowTaskEjbTestSteps.delete_generated_artifact(ArtifactType.RBSSUMMARY.toString(), nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), DELETE_RBS_SUMMARY_TASK_NAME);

        Assertions.assertThat(taskStatus)
                .isNotNull()
                .as("Task Status should not be null");
        Assertions.assertThat(taskStatus)
                .contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        final File smrsArtifactsDirectory = files.getNodeArtifactFolder(nodeMo.getName(), CommonArtifactDefinitions.SMRS_ERBS_DIRECTORY_TYPE);

        Assertions.assertThat(smrsArtifactsDirectory.listFiles())
                .extracting("name")
                .doesNotContain(GENERATED_RBS_SUMMARY_ARTIFACT_NAME)
                .as("Rbs Summary file was not deleted as expected from smrs location: " + smrsArtifactsDirectory.getAbsolutePath());

    }

    private String getNodeArtifactId(final ManagedObject nodeMo) {
        final ManagedObject siteEquipmentArtifactMo = dataSteps.getNodeArtifactMo(nodeMo.getFdn(), ArtifactType.RBSSUMMARY.toString());
        return siteEquipmentArtifactMo.getAttribute(CommonArtifactDefinitions.ATTRIBUTE_NODE_ARTIFACT_ID);

    }

    private void createOtherNodeArtifactFiles(final ManagedObject nodeMo) {
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEBASIC.toString(), nodeMo.getFdn());
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());
    }

}