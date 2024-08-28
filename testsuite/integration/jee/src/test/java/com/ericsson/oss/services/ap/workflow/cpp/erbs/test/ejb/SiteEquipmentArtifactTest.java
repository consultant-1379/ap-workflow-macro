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

public class SiteEquipmentArtifactTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private Dps dpsHelper;

    @Inject
    private Files files;

    private static final String GENERATED_SITE_EQUIPMENT_ARTIFACT_NAME = "RbsEquipment.xml";
    private static final String DELETE_SITE_EQUIPMENT_TASK_NAME = "Delete Site Equipment File";

    @Test(enabled = true)
    public void when_create_SiteEquipment_file_executed_successfully_then_the_artifact_is_generated_on_smrs() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_erbs_artifacts()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String siteEquipmentArtifactId = getNodeArtifactId(nodeMo);

        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.GENERATE_PROVISIONING_ARTIFACTS.toString());

        Assertions.assertThat(taskStatus)
                .isNotNull()
                .as("Task Status should not be null");
        Assertions.assertThat(taskStatus)
                .contains("Started")
                .as("Expected task progress to be Started");

        final File smrsArtifactsDirectory = files.getNodeArtifactFolder(nodeMo.getName(), CommonArtifactDefinitions.SMRS_ERBS_DIRECTORY_TYPE);

        assertThat(dpsHelper).withManagedObject(CommonArtifactDefinitions.NODE_ARTIFACT_FDN, nodeMo.getFdn(), siteEquipmentArtifactId)
                .withNotEmptyAttributeValue("generatedLocation")
                .withAttributeValue("generatedLocation", smrsArtifactsDirectory.getAbsolutePath() + "/" + GENERATED_SITE_EQUIPMENT_ARTIFACT_NAME)
                .as("The generatedLocation attribute in the Node Artifact MO was not updated as expected");

        Assertions.assertThat(smrsArtifactsDirectory.listFiles())
                .extracting("name")
                .as("Generated Site Equipment file was not generated as expected at smrs location: " + smrsArtifactsDirectory.getAbsolutePath())
                .contains(GENERATED_SITE_EQUIPMENT_ARTIFACT_NAME);

    }

    @Test
    public void when_create_SiteEquipment_file_failed_then_artifact_not_found_exception_is_thrown() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().without_site_equipment_artifact()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        verifyException(workflowTaskEjbTestSteps).create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());

        Assertions.assertThat(caughtException().getCause()
                .getClass())
                .as("ArtifactNotFoundException was not thrown as expected")
                .isEqualTo(ArtifactNotFoundException.class);
    }

    @Test
    public void when_delete_SiteEquipment_file_executed_successfully_then_the_artifact_is_deleted_from_smrs() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_erbs_artifacts()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String siteEquipmentArtifactId = getNodeArtifactId(nodeMo);

        createDependantNodeArtifactFiles(nodeMo);
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());

        // Execute the test method
        workflowTaskEjbTestSteps.delete_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), DELETE_SITE_EQUIPMENT_TASK_NAME);

        Assertions.assertThat(taskStatus)
                .isNotNull()
                .as("Task Status should not be null");
        Assertions.assertThat(taskStatus)
                .contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        final File smrsArtifactsDirectory = files.getNodeArtifactFolder(nodeMo.getName(), CommonArtifactDefinitions.SMRS_ERBS_DIRECTORY_TYPE);

        assertThat(dpsHelper).withManagedObject(CommonArtifactDefinitions.NODE_ARTIFACT_FDN, nodeMo.getFdn(), siteEquipmentArtifactId)
                .withBlankAttributeValue("generatedLocation")
                .as("The generatedLocation attribute in the Node Artifact MO was not reset as expected");

        Assertions.assertThat(smrsArtifactsDirectory.listFiles())
                .extracting("name")
                .doesNotContain(GENERATED_SITE_EQUIPMENT_ARTIFACT_NAME)
                .as("Site Equipment file was not deleted as expected from smrs location: " + smrsArtifactsDirectory.getAbsolutePath());

    }

    @Test
    public void when_delete_SiteEquipment_file_executed_successfully_then_the_artifact_and_node_directory_are_deleted_from_smrs() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_erbs_artifacts()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String siteEquipmentArtifactId = getNodeArtifactId(nodeMo);

        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());

        // Execute the test method
        workflowTaskEjbTestSteps.delete_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), DELETE_SITE_EQUIPMENT_TASK_NAME);

        Assertions.assertThat(taskStatus)
                .isNotNull()
                .as("Task Status should not be null");
        Assertions.assertThat(taskStatus)
                .contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        final File smrsArtifactsDirectory = files.getNodeArtifactFolder(nodeMo.getName(), CommonArtifactDefinitions.SMRS_ERBS_DIRECTORY_TYPE);

        assertThat(dpsHelper).withManagedObject(CommonArtifactDefinitions.NODE_ARTIFACT_FDN, nodeMo.getFdn(), siteEquipmentArtifactId)
                .withBlankAttributeValue("generatedLocation")
                .as("The generatedLocation attribute in the Node Artifact MO was not reset as expected");

        Assertions.assertThat(smrsArtifactsDirectory.listFiles())
                .as("Node directory on smrs was not deleted as expected during site equipment file deletion: "
                        + smrsArtifactsDirectory.getAbsolutePath())
                .isNull();

    }

    private void createDependantNodeArtifactFiles(final ManagedObject nodeMo) {
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEBASIC.toString(), nodeMo.getFdn());
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());
    }

    private String getNodeArtifactId(final ManagedObject nodeMo) {
        final ManagedObject siteEquipmentArtifactMo = dataSteps.getNodeArtifactMo(nodeMo.getFdn(), ArtifactType.SITEEQUIPMENT.toString());
        return siteEquipmentArtifactMo.getAttribute(CommonArtifactDefinitions.ATTRIBUTE_NODE_ARTIFACT_ID);

    }
}