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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps;

import static com.ericsson.oss.services.ap.arquillian.util.data.dps.model.DetachedManagedObject.Builder.newDetachedManagedObject;
import static com.ericsson.oss.services.ap.common.model.MoType.FM_ALARM_SUPERVISION;
import static com.ericsson.oss.services.ap.common.model.MoType.INV_SUPERVISION;
import static com.ericsson.oss.services.ap.common.model.MoType.PM_FUNCTION;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import org.apache.commons.lang.ArrayUtils;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.itpf.sdk.context.classic.ContextConstants;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.itpf.sdk.resources.Resources;
import com.ericsson.oss.itpf.security.cryptography.CryptographyService;
import com.ericsson.oss.services.ap.api.status.State;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Files;
import com.ericsson.oss.services.ap.arquillian.util.ProjectGenerator;
import com.ericsson.oss.services.ap.arquillian.util.data.managedobject.OSSMosGenerator;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.common.configuration.DirectoryConfiguration;
import com.ericsson.oss.services.ap.common.model.NodeArtifactAttribute;
import com.ericsson.oss.services.ap.common.model.NodeUserCredentialsAttributes;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.AutoIntegrationMosGenerator;
import com.google.common.collect.ImmutableMap;

import ru.yandex.qatools.allure.annotations.Step;

/**
 * Data related test steps for all the Workflow arquillian tests.
 */
public class WorkflowDataSteps {

    private static final String NODE_ARTIFACT_CONTAINER_DN = ",NodeArtifactContainer=1";
    private static final String ZIPPED_SAMPLES_DIR = "samples/";

    @Inject
    private AutoIntegrationMosGenerator integrationMosGenerator;

    @Inject
    private CryptographyService cyptographyService;
    
    @Inject
    private ContextService contextService;

    @Inject
    private Dps dps;

    @Inject
    private Files files;

    @Inject
    private OSSMosGenerator ossMosGenerator;

    @Inject
    private ProjectGenerator projectGenerator;

    @Inject
    private AutoProvisioningServiceTestSteps apServiceSteps;

    /**
     * Creates an AP project in DPS and returns the AP node {@link ManagedObject}.
     *
     * @param projectDescriptor
     *            the description of the AP project to create.
     * @return returns the node MO
     * @throws NoSuchElementException
     *             if the project does not contain any nodes
     * @throws IllegalArgumentException
     *             if the project contains more that one node
     */
    @Step("Create project MO and returning the node MO")
    public ManagedObject create_project_mo_and_return_node_mo(final ProjectDescriptor projectDescriptor) {
        final ManagedObject project = projectGenerator.generate(projectDescriptor);
        try {
            return getOnlyElement(project.getChildren());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(project + " contains more than one node", e);
        } catch (final NoSuchElementException e) {
            throw new IllegalArgumentException(project + " does not contain any nodes", e);
        }
    }

    @Step("Read zip from the download location and pass contents of all sample zip files into order")
    public boolean order_downloaded_sample_projects(final String downloadedProjectId) {
        final String downloadLocation = DirectoryConfiguration.getDownloadDirectory();
        final String downloadedZipFilePath = downloadLocation + "/" + downloadedProjectId;
        final Resource resource = getFileSystemResource(downloadedZipFilePath);

        if (resource != null) {
            try (final ByteArrayOutputStream zipContent = new ByteArrayOutputStream();
                    final InputStream downloadedProjectInputStream = resource.getInputStream();
                    final ZipInputStream downloadedZip = new ZipInputStream(downloadedProjectInputStream)) {

                // Extract the wanted sample project zip which is nested in the overall zip file
                ZipEntry zipEntry;
                while ((zipEntry = downloadedZip.getNextEntry()) != null) {
                    final String name = zipEntry.getName();
                    if (name.startsWith(ZIPPED_SAMPLES_DIR) && name.endsWith(".zip")) {
                        final byte[] data = new byte[1024];
                        int dataSize;

                        // Read the content of the zip entry
                        while ((dataSize = downloadedZip.read(data)) > 0) {
                            zipContent.write(data, 0, dataSize);
                        }
                        final String fileName = name.substring(ZIPPED_SAMPLES_DIR.length(), name.length());
                        apServiceSteps.orderProject(fileName, zipContent.toByteArray());
                        zipContent.reset();
                    }

                }
                return true;
            } catch (final Exception e) {
                return false;
            }
        }
        return false;
    }

    @Step("Update node state.")
    public String update_node_state_mo(final String nodeFdn, final State state) {
        final String nodeStateFdn = format("%s,NodeStatus=1", nodeFdn);
        final ImmutableMap<String, Object> newAttributes = ImmutableMap.<String, Object> of("state", state.toString());
        dps.updateMo(nodeStateFdn, newAttributes);

        return nodeStateFdn;
    }

    @SuppressWarnings("unchecked")
    @Step("Get the status entry for a task")
    public final String get_ap_task_status(final String nodeFdn, final String taskName) {
        final String nodeStateFdn = format("%s,NodeStatus=1", nodeFdn);
        final ManagedObject nodeStatusMo = dps.findMoByFdn(nodeStateFdn);
        final List<String> status = (List<String>) nodeStatusMo.getAttribute("statusEntries");
        return getTaskProgressEntry(status, taskName);

    }

    @Step("Update rbsConfigLevel")
    public void update_rbs_config_level(final String nodeName, final String rbsConfigLevel) {
        final String rbsConfigLevelFdn = "MeContext=" + nodeName + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1";
        final ImmutableMap<String, Object> newAttributes = ImmutableMap.<String, Object> of("rbsConfigLevel", rbsConfigLevel);
        dps.updateMo(rbsConfigLevelFdn, newAttributes);
    }

    @Step("Create the NetworkElement MO only")
    public ManagedObject create_network_element_mo(final String nodeName) {
        return integrationMosGenerator.generateNetworkElement(nodeName);
    }

    @Step("Get the node user credential data")
    public Map<String, Object> get_encrypted_node_credential_data(final String securePassword) {
        final Map<String, Object> nodeUserCredentialData = new HashMap<>();

        if (securePassword != null && !securePassword.isEmpty()) {
            final byte[] encryptedSecurePassword = cyptographyService.encrypt(securePassword.getBytes(StandardCharsets.ISO_8859_1));
            final List<Byte> encryptedSecurePasswordByteList = Arrays.asList(ArrayUtils.toObject(encryptedSecurePassword));
            nodeUserCredentialData.put(NodeUserCredentialsAttributes.SECURE_USERNAME.toString(), "user1");
            nodeUserCredentialData.put(NodeUserCredentialsAttributes.SECURE_PASSWORD.toString(), encryptedSecurePasswordByteList);
        }

        return nodeUserCredentialData;
    }

    @Step("Create the FmAlarmSupervision MO")
    public ManagedObject createFmAlarmSupervisionMo(final ManagedObject nodeMo) {
        final String networElementFdn = "NetworkElement=" + nodeMo.getName();
        final ManagedObject networkElementMo = dps.findMoByFdn(networElementFdn);
        final Map<String, Object> fmAttributes = new HashMap<>();
        fmAttributes.put("active", false);

        final ManagedObject fmSupervisionMo = newDetachedManagedObject().name("1")
                .namespace("OSS_NE_FM_DEF")
                .type(FM_ALARM_SUPERVISION.toString())
                .version("1.1.0")
                .parent(networkElementMo)
                .mibRoot(true)
                .attributes(fmAttributes)
                .build();

        return dps.createMo(fmSupervisionMo);
    }

    @Step("Create the PmFunction MO")
    public ManagedObject createPmFunctionMo(final ManagedObject nodeMo) {
        final String networElementFdn = "NetworkElement=" + nodeMo.getName();
        final ManagedObject networkElementMo = dps.findMoByFdn(networElementFdn);
        final Map<String, Object> pmAttributes = new HashMap<>();
        pmAttributes.put("pmEnabled", false);

        final ManagedObject pmSupervisionMo = newDetachedManagedObject().name("1")
                .namespace("OSS_NE_PM_DEF")
                .type(PM_FUNCTION.toString())
                .version("1.0.0")
                .parent(networkElementMo)
                .mibRoot(true)
                .attributes(pmAttributes)
                .build();

        return dps.createMo(pmSupervisionMo);
    }

    @Step("Create the InventorySupervision MO")
    public ManagedObject createInventorySupervisionMo(final ManagedObject nodeMo) {
        final String networElementFdn = "NetworkElement=" + nodeMo.getName();
        final ManagedObject networkElementMo = dps.findMoByFdn(networElementFdn);
        final Map<String, Object> invAttributes = new HashMap<>();
        invAttributes.put("active", false);

        final ManagedObject invSupervisionMo = newDetachedManagedObject().name("1")
                .namespace("CM")
                .type(INV_SUPERVISION.toString())
                .version("1.0.0")
                .parent(networkElementMo)
                .mibRoot(true)
                .attributes(invAttributes)
                .build();

        return dps.createMo(invSupervisionMo);
    }

    @Step("Update Hardware Serial Number")
    public void update_hardware_serial_number(final String nodeFdn, final String hwNumber) {
        final Map<String, Object> newAttributes = new HashMap<>();
        newAttributes.put("hardwareSerialNumber", hwNumber);
        dps.updateMo(nodeFdn, newAttributes);
    }

    @Step("Create the Node MOs that are pre-requisite for auto-integration")
    public void create_me_context_and_all_erbs_node_mos(final String nodeName) {
        integrationMosGenerator.generateMeContextMoAndChildren(nodeName);
        integrationMosGenerator.generateNetworkElementMoAndChildren(nodeName);
        integrationMosGenerator.generateConnectivityInfo(nodeName, "1.1.0", "COM_MED", "ComConnectivityInformation");

    }

    @Step("Create the Node MOs that are pre-requisite for auto-integration with SubNetworks")
    public void create_me_context_and_all_erbs_node_mos_with_subnetworks(final String nodeName) {
        integrationMosGenerator.generateMeContextMoAndChildrenWithSubNetworks(nodeName);
    }

    @Step("Create the Node MOs that are pre-requisite for auto-integration")
    public void create_me_context_and_all_erbs_node_mos(final ManagedObject nodeMo) {
        create_me_context_and_all_erbs_node_mos(nodeMo.getName());
        ossMosGenerator.generateCiRefAssociation(nodeMo.getFdn());
    }

    @Step("Create flawed Node MOs that are pre-requisite for auto-integration")
    public void create_me_context_and_all_erbs_node_mos_with_flawed_nbiot_cell(final String nodeName) {
        integrationMosGenerator.generateMeContextMoAndFlawedNbiotChildren(nodeName);
        integrationMosGenerator.generateNetworkElementMoAndChildren(nodeName);
    }

    @Step("Create NetworkElement child mos that would be created by mediation")
    public void create_network_element_child_mos(final String nodeName) {
        integrationMosGenerator.generateNetworkElementChildMos(nodeName);
    }

    @Step("Create the Node MOs that are pre-requisite for auto-integration with sync")
    public void create_all_erbs_node_mos_with_sync(final String nodeName) {
        integrationMosGenerator.generateMeContextMoAndChildren(nodeName);
        integrationMosGenerator.generateNetworkElementChildMosWithSync(nodeName);
    }

    @Step("Create the Node MOs that are pre-requisite for auto-integration")
    public void create_all_erbs_node_mos_expect_heartbeat_supervision(final String nodeName) {
        integrationMosGenerator.generateMeContextMoAndChildren(nodeName);
        integrationMosGenerator.generateNetworkElementChildMosWithoutCmHeartbeatSupervisionMO(nodeName);
    }

    @Step("Update the workflowInstanceIdList attribute on Node Mo")
    public void update_workflow_instance_id_on_node_mo(final String fdn, final String wfId) {
        final Map<String, Object> updateParameters = ImmutableMap.<String, Object> of("workflowInstanceIdList", newArrayList(wfId),
                "activeWorkflowInstanceId", wfId);
        dps.updateMo(fdn, updateParameters);
    }

    @Step("Get the relative path of node")
    public String get_node_relative_path(final ManagedObject node) {
        final String apNodeFdn = node.getFdn();
        final String nodeName = FDN.get(apNodeFdn)
                .getRdnValue();
        final String projectFdn = FDN.get(apNodeFdn)
                .getParent();
        final String projectName = FDN.get(projectFdn)
                .getRdnValue();

        return projectName + File.separator + nodeName;
    }

    @Step("Get file from generated directory")
    public File get_file_from_generated_directory(final ManagedObject nodeMo, final String fileName) {
        final String nodeDirectory = get_node_relative_path(nodeMo);
        final File generatedArtifactsDirectory = files.getNodeArtifactFolder(nodeDirectory, "generated");
        return new File(generatedArtifactsDirectory, fileName);
    }

    @Step("Get file contents from Bind directory")
    public String get_decrypted_file_contents_from_bind_directory(final String fileName) {
        final File bindDirectory = files.getNodeArtifactFolder("", "bind");
        final File file = new File(bindDirectory.getAbsolutePath(), fileName);

        if (!file.exists()) {
            return null;
        }

        try (final FileInputStream fin = new FileInputStream(file)) {
            final byte fileContent[] = new byte[(int) file.length()];
            fin.read(fileContent);
            return new String(cyptographyService.decrypt(fileContent));
        } catch (final Exception e) {
            return null;
        }
    }

    public Resource getFileSystemResource(final String fileResourceUri) {
        return Resources.getFileSystemResource(fileResourceUri);
    }

    @Step("Get the NodeArtifact MO instance for the given artifact type")
    public ManagedObject getNodeArtifactMo(final String nodeMo, final String artifactType) {
        final String nodeArtifactContainerMoFDN = nodeMo + NODE_ARTIFACT_CONTAINER_DN;
        final ManagedObject nodeArtifactContainer = dps.findMoByFdn(nodeArtifactContainerMoFDN);
        for (final ManagedObject nodeArtifactMo : nodeArtifactContainer.getChildren()) {
            final String type = nodeArtifactMo.getAttribute(NodeArtifactAttribute.TYPE.toString());
            if (type.equals(artifactType)) {
                return nodeArtifactMo;
            }
        }

        return null;
    }

    @Step("Set AP_User in context service")
    public void setApUser() {
        contextService.setContextValue(ContextConstants.HTTP_HEADER_USERNAME_KEY, "ap-user");
    }

    private String getTaskProgressEntry(final List<String> statusEntries, final String taskName) {
        for (final String taskStatus : statusEntries) {
            if (taskStatus.contains(taskName)) {
                return taskStatus;
            }
        }
        return null;
    }

}
