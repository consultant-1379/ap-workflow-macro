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

import static com.ericsson.oss.services.ap.api.status.State.ORDER_COMPLETED;
import static com.ericsson.oss.services.ap.api.status.State.ORDER_FAILED;
import static com.ericsson.oss.services.ap.api.status.State.ORDER_ROLLBACK_FAILED;
import static com.ericsson.oss.services.ap.api.status.State.READY_FOR_ORDER;
import static com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener.getOrderCompleteStates;
import static com.ericsson.oss.services.ap.arquillian.util.data.workflow.WorkflowFunctionsFactory.newCreateCiRefAssociationFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.validation.DpsAssert.assertThat;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Files;
import com.ericsson.oss.services.ap.arquillian.util.data.managedobject.OSSMosGenerator;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesResult;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.AutoProvisioningServiceTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Arquillian test class to test the Order Workflow for ERBS nodes.
 */
public class OrderErbsWorkflowTest extends Arquillian {

    private static final String NETWORK_ELEMENT_MO = "NetworkElement=%s";
    private static final String CPP_CONNECTIVITY_INFO_MO = NETWORK_ELEMENT_MO + ",CppConnectivityInformation=1";

    private static final String RBSSUMMARY_FILENAME = "AutoIntegrationRbsSummaryFile.xml";

    private static final String SITE_INSTALLATION_FILENAME = "SiteInstall.xml";
    private static final String HARDWARE_SERIAL_NUMBER = "hardwareSerialNumber";
    private static final int NO_OF_SUCCESSFUL_ARTIFACTS = 5;

    private static final String ERBS_NODE_TYPE = "ERBS";
    private static final String ERBS_PROJECT_ZIP_NAME = "erbs_SchemasAndSamples";

    @Inject
    private Dps dps;

    @Inject
    private Files files;

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    @Inject
    private OSSMosGenerator ossMosGenerator;

    @Inject
    private AutoProvisioningServiceTestSteps apServiceSteps;

    @Test
    @Features("AP Order")
    @Stories({
        "WHEN order macro workflow is executed WITH all sample files within downloaded erbs project file THEN the project is validated and the order workflow is started successfully" })
    public void when_order_project_with_erbs_sample_project_files_then_the_project_is_validated_and_order_is_started_successfully() {

        final String downloadedProjectId = apServiceSteps.downloadSampleProjectFile(ERBS_NODE_TYPE);
        assertThat(downloadedProjectId).endsWith(".zip");
        assertThat(downloadedProjectId).contains(ERBS_PROJECT_ZIP_NAME);

        assertThat(dataSteps.order_downloaded_sample_projects(downloadedProjectId))
            .isTrue()
            .as("All downloaded sample ERBS projects should be successfully validated");
    }

    /**
     * Test is verified by OrderErbsWorkflowSpec
     */
    @Test
    @Features("AP Order")
    @Stories({ "WHEN order node with invalid configuration files THEN validation fails, Node state is ORDER_FAILED" })
    public void when_order_node_with_invalid_config_files_then_validation_fails() {
        stubsSteps.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_invalid_configurations_rn_file()
            .with_default_erbs_artifacts()
            .with_default_security_options()
            .with_default_erbs_auto_integration_options()
            .with_default_supervision_options()
            .with_user_node_credentials()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        final String nodeFdn = node.getFdn();
        dataSteps.update_node_state_mo(nodeFdn, READY_FOR_ORDER);

        apServiceSteps.orderNode(nodeFdn);

        final NodeStatusEntriesResult statusEntries = getNodeStatusEntriesResults(node);

        assertThat(statusEntries.getSuccessfulEntries()).as("All tasks should succeed for Node: " + nodeFdn).isEmpty();

        assertThat(statusEntries.getFailedEntries()).as("Validate Configuration task should fail for Node: " + nodeFdn)
            .containsOnly(StatusEntryNames.VALIDATE_CONFIGURATIONS_TASK.toString());

        final String nodeStateFdn = format("%s,NodeStatus=1", nodeFdn);
        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", ORDER_FAILED.toString());
    }

    /**
     * Test is verified by OrderErbsWorkflowSpec
     */
    @Test
    @Features("AP Order")
    @Stories({ "WHEN user orders node with all options enabled THEN order succeeds" })
    public void when_user_orders_node_with_all_options_enabled_then_order_succeeds() {
        stubsSteps.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .with_default_security_options()
            .with_default_erbs_auto_integration_options()
            .with_default_supervision_options()
            .with_user_node_credentials()
            .withDefaultDhcpOptions()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        dataSteps.update_node_state_mo(node.getFdn(), READY_FOR_ORDER);

        apServiceSteps.orderNode(node.getFdn());

        verifyOrderSucceeded(node);
    }

    /**
     * Test is verified by OrderErbsWorkflowSpec
     */
    @Test
    @Features("AP Order")
    @Stories({ "WHEN order rollback fails THEN node status is order rollback failed" })
    public void when_order_rollback_fails_then_node_status_is_order_rollback_failed() {
        stubsSteps.create_default_stubs();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .without_site_install_artifact()
            .with_default_security_options()
            .with_default_erbs_auto_integration_options()
            .with_default_supervision_options()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final String nodeStateFdn = dataSteps.update_node_state_mo(node.getFdn(), READY_FOR_ORDER);

        stubsSteps.create_flawed_workflowTaskFacade_spy_for_remove_node();
        apServiceSteps.orderNode(node.getFdn());

        getNodeStatusEntriesResults(node);

        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", ORDER_ROLLBACK_FAILED.toString());
    }

    private void verifyOrderSucceeded(final ManagedObject node) {
        final NodeStatusEntriesResult statusEntries = getNodeStatusEntriesResults(node);

        assertThat(statusEntries.getFailedEntries()).as("No task should fail").isEmpty();

        assertThat(statusEntries.getSuccessfulEntries()).as("All tasks should succeed").containsExactlyInAnyOrder(
            StatusEntryNames.VALIDATE_CONFIGURATIONS_TASK.toString(),
            StatusEntryNames.ADD_NODE_TASK.toString(),
            StatusEntryNames.GENERATE_SECURITY_TASK.toString(),
            StatusEntryNames.GENERATE_PROVISIONING_ARTIFACTS.toString(),
            StatusEntryNames.CREATE_NODE_USER_CREDENTIALS.toString(),
            StatusEntryNames.HARDWARE_BIND_TASK.toString(),
            StatusEntryNames.DHCP_CONFIGURATION.toString());

        final String nodeFdn = node.getFdn();
        final String nodeStateFdn = format("%s,NodeStatus=1", nodeFdn);
        assertThat(dps).withManagedObject(nodeStateFdn)
                .withFdnAttributeValue(nodeStateFdn, "state", ORDER_COMPLETED.toString());

        assertThat(dps).hasManagedObject(NETWORK_ELEMENT_MO, node.getName())
            .hasManagedObject(CPP_CONNECTIVITY_INFO_MO, node.getName());

        assertThat(dps).withManagedObject(nodeFdn).withNotEmptyAttributeValue(HARDWARE_SERIAL_NUMBER);

        for (int nodeArtifactRdnValue = 1; nodeArtifactRdnValue <= NO_OF_SUCCESSFUL_ARTIFACTS; ++nodeArtifactRdnValue) {
            assertThat(dps)
                .withManagedObject("%s,NodeArtifactContainer=1,NodeArtifact=" + nodeArtifactRdnValue, nodeFdn)
                .withNotEmptyAttributeValue("generatedLocation");
        }

        final File smrsArtifactsDirectory = files.getNodeArtifactFolder(node.getName(), "smrs." + ERBS_NODE_TYPE);
        assertThat(smrsArtifactsDirectory.listFiles()).extracting("name")
            .contains("Iscf.xml");

        final File rbsSummaryFile = new File(smrsArtifactsDirectory, RBSSUMMARY_FILENAME);
        assertThat(contentOf(rbsSummaryFile)).contains("Iscf.xml");

        final String nodeDirectory = dataSteps.get_node_relative_path(node);
        final File generatedArtifactsDirectory = files.getNodeArtifactFolder(nodeDirectory, "generated");
        final File installationFile = new File(generatedArtifactsDirectory, SITE_INSTALLATION_FILENAME);
        assertThat(contentOf(installationFile)).contains("rbsIntegrationCode");

        final File bindDirectory = files.getBindArtifactFolder();
        assertThat(bindDirectory.listFiles()).isNotEmpty();
    }

    private NodeStatusEntriesResult getNodeStatusEntriesResults(final ManagedObject node) {
        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps);
        statusEntriesListener.onEnd(StatusEntryNames.ADD_NODE_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator));
        statusEntriesListener.listenUntilStateChanges(node.getFdn(), getOrderCompleteStates());
        return statusEntriesListener.waitForResults(120L, TimeUnit.SECONDS);
    }
}
