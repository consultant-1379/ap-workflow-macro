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

import static com.ericsson.oss.services.ap.api.status.State.BIND_COMPLETED;
import static com.ericsson.oss.services.ap.api.status.State.BIND_STARTED;
import static com.ericsson.oss.services.ap.api.status.State.ORDER_STARTED;
import static com.ericsson.oss.services.ap.arquillian.util.data.validation.DpsAssert.assertThat;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.CatchException.verifyException;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.api.status.StatusEntryProgress;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Files;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.common.model.NodeAttribute;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.CommonArtifactDefinitions;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

public class BindNodeTest extends Arquillian {

    private static final String HARDWARE_SERIAL_NUMBER = "HWS12345678";
    private static final String HARDWARE_SERIAL_NUMBER_REBIND = "HWS87654321";
    private static final String BIND_TASK_NAME = "Hardware Bind";
    private static final String ATTR_SMRS_USER_NAME = "userName";
    private static final String ATTR_SMRS_PASSWORD = "password";

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    @Inject
    private Dps dps;

    @Inject
    private Files files;

    @Test
    public void when_bind_manually_after_order_then_task_completes_successfully_and_artifacts_exist_in_bind_directory() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeFdn = nodeMo.getFdn();
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createSiteInstallFile(nodeFdn);
        dataSteps.update_node_state_mo(nodeFdn, BIND_STARTED);

        workflowTaskEjbTestSteps.bind_node_manually(nodeFdn, HARDWARE_SERIAL_NUMBER);

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeFdn, BIND_TASK_NAME);
        assertThat(taskStatus)
            .isNotNull()
            .as("Task Status should not be null");

        assertThat(taskStatus)
            .contains(StatusEntryProgress.COMPLETED.toString())
            .as("Expected task progress to be Completed");

        // Verify bind artifacts created
        assertThat(dps)
            .as(String.format("Expected : NodeMo has '%s' = %s.", NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(), HARDWARE_SERIAL_NUMBER))
            .withManagedObject(nodeFdn)
            .withAttributeValue(NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(), HARDWARE_SERIAL_NUMBER);

        final String generatedSiteInstallationForBind = dataSteps.get_decrypted_file_contents_from_bind_directory(HARDWARE_SERIAL_NUMBER + ".xml");

        assertThat(generatedSiteInstallationForBind)
            .as(String.format("Expected : File %s for node %s should exist.", HARDWARE_SERIAL_NUMBER + ".xml", nodeFdn))
            .isNotNull();

        assertThat(generatedSiteInstallationForBind)
            .as(String.format("Expected : File %s for node contains value for '%s'.", generatedSiteInstallationForBind,
                ATTR_SMRS_USER_NAME))
            .contains(ATTR_SMRS_USER_NAME);

        assertThat(generatedSiteInstallationForBind)
            .as(String.format("Expected : File %s for node contains value for '%s'.", generatedSiteInstallationForBind,
                ATTR_SMRS_PASSWORD))
            .contains(ATTR_SMRS_PASSWORD);
    }

    @Test
    public void when_bind_manually_with_no_hardwareSerialNumber_then_fail_correctly() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeFdn = nodeMo.getFdn();
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createSiteInstallFile(nodeFdn);
        dataSteps.update_node_state_mo(nodeFdn, BIND_STARTED);

        verifyException(workflowTaskEjbTestSteps).bind_node_manually(nodeFdn, null);

        assertThat(caughtException().getCause()
            .getClass())
                .isEqualTo(ApServiceException.class)
                .as("Manual bind should fail when hardware serial number is set to null");

    }

    @Test
    public void when_bind_during_order_then_task_completes_successfully_and_artifacts_exist_in_bind_directory() {

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeFdn = nodeMo.getFdn();
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createSiteInstallFile(nodeFdn);
        dataSteps.update_node_state_mo(nodeFdn, ORDER_STARTED);
        dataSteps.update_hardware_serial_number(nodeFdn, HARDWARE_SERIAL_NUMBER);

        workflowTaskEjbTestSteps.bind_node_during_order(nodeFdn, HARDWARE_SERIAL_NUMBER);

        final String taskStatus = dataSteps.get_ap_task_status(nodeFdn, BIND_TASK_NAME);
        assertThat(taskStatus)
            .isNotNull()
            .as("Task Status should not be null");

        assertThat(taskStatus)
            .contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
            .as("Expected task progress to be Completed");

        final File bindDirectory = files.getNodeArtifactFolder("", "bind");
        final File[] bindDirectoryContents = bindDirectory.listFiles();
        assertThat(bindDirectoryContents)
            .extracting("name")
            .contains(HARDWARE_SERIAL_NUMBER + ".xml")
            .as("Bind directory should contain files after bind during order");
    }

    @Test
    public void when_bind_during_order_with_no_hardwareSerialNumber_then_fail_correctly() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeFdn = nodeMo.getFdn();
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createSiteInstallFile(nodeFdn);
        dataSteps.update_node_state_mo(nodeFdn, ORDER_STARTED);
        dataSteps.update_hardware_serial_number(nodeFdn, null);

        verifyException(workflowTaskEjbTestSteps).bind_node_during_order(nodeFdn, null);

        final String taskStatus = dataSteps.get_ap_task_status(nodeFdn, BIND_TASK_NAME);
        assertThat(taskStatus)
            .isNotNull()
            .as("Task Status should not be null");

        assertThat(taskStatus)
            .contains(StatusEntryProgress.FAILED.toString())
            .as("Expected task progress to be Failed");

        assertThat(caughtException().getCause()
            .getClass())
                .isEqualTo(ApServiceException.class)
                .as("Bind during order should fail when no hardware serial number is specified");
    }

    @Test
    public void when_rebind_after_manual_bind_then_task_completes_successfully_and_updated_artifacts_exist_in_bind_directory() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeFdn = nodeMo.getFdn();
        final String nodeStatusFdn = nodeMo.getFdn() + ",NodeStatus=1";
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createSiteInstallFile(nodeFdn);

        //setup for initial bind
        dataSteps.update_node_state_mo(nodeFdn, ORDER_STARTED);
        dataSteps.update_hardware_serial_number(nodeFdn, HARDWARE_SERIAL_NUMBER);
        workflowTaskEjbTestSteps.bind_node_during_order(nodeFdn, HARDWARE_SERIAL_NUMBER);

        String generatedSiteInstallationForBind1 = dataSteps.get_decrypted_file_contents_from_bind_directory(HARDWARE_SERIAL_NUMBER + ".xml");

        assertThat(generatedSiteInstallationForBind1)
            .as(String.format("Expected : File %s for node %s should exist.", HARDWARE_SERIAL_NUMBER + ".xml", nodeFdn))
            .isNotNull();

        dataSteps.update_node_state_mo(nodeFdn, BIND_STARTED);

        //rebind the node with new hardware serial number
        workflowTaskEjbTestSteps.bind_node_manually(nodeFdn, HARDWARE_SERIAL_NUMBER_REBIND);

        generatedSiteInstallationForBind1 = dataSteps.get_decrypted_file_contents_from_bind_directory(HARDWARE_SERIAL_NUMBER + ".xml");

        assertThat(generatedSiteInstallationForBind1)
            .as(String.format("Expected : File %s for node %s should not exist after rebind.", HARDWARE_SERIAL_NUMBER + ".xml", nodeFdn))
            .isNull();

        final String generatedSiteInstallationForBind2 = dataSteps
            .get_decrypted_file_contents_from_bind_directory(HARDWARE_SERIAL_NUMBER_REBIND + ".xml");

        assertThat(generatedSiteInstallationForBind2)
            .as(String.format("Expected : File %s for rebound node %s should exist.", HARDWARE_SERIAL_NUMBER_REBIND + ".xml", nodeFdn))
            .isNotNull();

        assertThat(dps)
            .as(String.format("Expected : NodeMo has '%s' = %s.", NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(), HARDWARE_SERIAL_NUMBER_REBIND))
            .withManagedObject(nodeFdn)
            .withAttributeValue(NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(), HARDWARE_SERIAL_NUMBER_REBIND);

        assertThat(dps).withManagedObject(nodeStatusFdn).withAttributeValue("state", BIND_COMPLETED.name());
    }

    @Test
    public void when_rebind_fails_for_an_ordered_node_then_the_hardwareSerialNumber_is_reset_to_original_value() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeFdn = nodeMo.getFdn();
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createSiteInstallFile(nodeFdn);

        dataSteps.update_node_state_mo(nodeFdn, ORDER_STARTED);
        dataSteps.update_hardware_serial_number(nodeFdn, HARDWARE_SERIAL_NUMBER);
        workflowTaskEjbTestSteps.bind_node_during_order(nodeFdn, HARDWARE_SERIAL_NUMBER);

        dataSteps.update_node_state_mo(nodeFdn, BIND_STARTED);

        stubsSteps.create_flawed_workflowTaskFacade_spy_for_unbind(HARDWARE_SERIAL_NUMBER);
        verifyException(workflowTaskEjbTestSteps).bind_node_manually(nodeFdn, HARDWARE_SERIAL_NUMBER_REBIND);

        final File bindDirectory = files.getNodeArtifactFolder("", "bind");
        final File[] bindDirectoryContents = bindDirectory.listFiles();
        assertThat(bindDirectoryContents)
            .extracting("name")
            .contains(HARDWARE_SERIAL_NUMBER + ".xml")
            .as("Bind directory should contain updated files after rebind");

        assertThat(bindDirectoryContents)
            .extracting("name")
            .doesNotContain(HARDWARE_SERIAL_NUMBER_REBIND + ".xml")
            .as("Bind directory should not contain files from previous bind after rebind");
        assertThat(dps).withManagedObject(nodeFdn).withAttributeValue("hardwareSerialNumber", HARDWARE_SERIAL_NUMBER);
    }

    @Test
    public void when_unbind_after_bind_during_order_then_bind_directory_null_or_empty() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeFdn = nodeMo.getFdn();
        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo);
        createSiteInstallFile(nodeFdn);
        dataSteps.update_node_state_mo(nodeFdn, ORDER_STARTED);
        workflowTaskEjbTestSteps.bind_node_during_order(nodeFdn, HARDWARE_SERIAL_NUMBER);

        workflowTaskEjbTestSteps.unbind_node(nodeFdn);

        final File bindDirectory = files.getNodeArtifactFolder("", "bind");
        final File[] bindDirectoryContents = bindDirectory.listFiles();
        assertThat(bindDirectoryContents)
            .isNullOrEmpty();
    }

    private void createSiteInstallFile(final String nodeFdn) {
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEBASIC.toString(), nodeFdn);
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEEQUIPMENT.toString(), nodeFdn);
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.RBSSUMMARY.toString(), nodeFdn);
        workflowTaskEjbTestSteps.create_generated_artifact(ArtifactType.SITEINSTALL.toString(), nodeFdn);
    }
}