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
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.CommonArtifactDefinitions;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

public class SiteInstallationArtifactTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private Dps dpsHelper;

    private static final String GENERATED_SITE_INSTALL_ARTIFACT_NAME = "SiteInstall.xml";
    private static final String DELETE_SITE_INSTALL_TASK_NAME = "Delete Site Installation File";

    @Test
    public void when_create_SiteInstallation_file_executed_successfully_then_the_file_is_generated_at_the_generated_artifacts_location() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_erbs_artifacts()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);

        createOtherNodeArtifactFiles(nodeMo);
        final String siteInstallArtifactId = getNodeArtifactId(nodeMo);

        // Execute the test
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEINSTALL.toString(), nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.GENERATE_PROVISIONING_ARTIFACTS.toString());

        Assertions.assertThat(taskStatus)
                .isNotNull()
                .as("Task Status should not be null");
        Assertions.assertThat(taskStatus)
                .contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        // Verify the file generation
        final File generatedFile = dataSteps.get_file_from_generated_directory(nodeMo, GENERATED_SITE_INSTALL_ARTIFACT_NAME);

        assertThat(dpsHelper).withManagedObject(CommonArtifactDefinitions.NODE_ARTIFACT_FDN, nodeMo.getFdn(), siteInstallArtifactId)
                .as("The generatedLocation attribute in the Node Artifact MO was not updated as expected")
                .withNotEmptyAttributeValue("generatedLocation")
                .withAttributeValue("generatedLocation", generatedFile.getAbsolutePath());

        Assertions.assertThat(generatedFile)
                .as("Generated Site Install file was not generated as expected at generated location: " + generatedFile.getAbsolutePath())
                .exists();

    }

    @Test
    public void when_create_SiteInstallation_file_failed_then_artifact_not_found_exception_is_thrown() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().without_site_install_artifact()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        verifyException(workflowTaskEjbTestSteps).create_generated_artifact(ArtifactType.SITEINSTALL.toString(), nodeMo.getFdn());

        Assertions.assertThat(caughtException().getCause()
                .getClass())
                .isEqualTo(ArtifactNotFoundException.class)
                .as("ArtifactNotFoundException was not thrown as expected");
    }

    @Test
    public void when_delete_SiteInstall_file_executed_successfully_then_the_artifact_is_deleted_from_smrs() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_erbs_artifacts()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createOtherNodeArtifactFiles(nodeMo);

        // Create the site install file so we can test deletion
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEINSTALL.toString(), nodeMo.getFdn());

        // Execute the test method
        workflowTaskEjbTestSteps.delete_generated_artifact(ArtifactType.SITEINSTALL.toString(), nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), DELETE_SITE_INSTALL_TASK_NAME);

        Assertions.assertThat(taskStatus)
                .isNotNull()
                .as("Task Status should not be null");
        Assertions.assertThat(taskStatus)
                .contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        final File generatedFile = dataSteps.get_file_from_generated_directory(nodeMo, GENERATED_SITE_INSTALL_ARTIFACT_NAME);
        final String siteInstallArtifactId = getNodeArtifactId(nodeMo);

        assertThat(dpsHelper).withManagedObject(CommonArtifactDefinitions.NODE_ARTIFACT_FDN, nodeMo.getFdn(), siteInstallArtifactId)
                .withBlankAttributeValue("generatedLocation")
                .as("The generatedLocation attribute in the Node Artifact MO was not reset as expected");

        Assertions.assertThat(generatedFile.listFiles())
                .as("Site Install file and node directory was not deleted as expected from generation files location: "
                        + generatedFile.getAbsolutePath())
                .isNull();

    }

    private String getNodeArtifactId(final ManagedObject nodeMo) {
        final ManagedObject siteEquipmentArtifactMo = dataSteps.getNodeArtifactMo(nodeMo.getFdn(), ArtifactType.SITEINSTALL.toString());
        return siteEquipmentArtifactMo.getAttribute(CommonArtifactDefinitions.ATTRIBUTE_NODE_ARTIFACT_ID);

    }

    private void createOtherNodeArtifactFiles(final ManagedObject nodeMo) {
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEBASIC.toString(), nodeMo.getFdn());
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeMo.getFdn());
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.RBSSUMMARY.toString(), nodeMo.getFdn());
    }

}