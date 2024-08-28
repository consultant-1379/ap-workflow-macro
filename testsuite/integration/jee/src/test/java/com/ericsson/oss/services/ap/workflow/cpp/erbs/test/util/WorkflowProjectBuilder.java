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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util;

import static com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor.ProjectDescriptorBuilder.usingNodeType;

import java.util.Collections;
import java.util.Map;

import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor.ProjectDescriptorBuilder;

/**
 * Convenient builder for {@link ProjectDescriptor}.
 */
public class WorkflowProjectBuilder {

    private final ProjectDescriptorBuilder builder;

    private WorkflowProjectBuilder(final String nodeType, final String nodeIdentifier) {
        builder = usingNodeType(nodeType).withNodeCount(1)
                .withNodeIdentifier(nodeIdentifier);
    }

    public static WorkflowProjectBuilder createErbsProjectWithOneNode() {
        return new WorkflowProjectBuilder("ERBS", "18.Q3-J.2.150"); // model needed for NbIoTCell MO
    }

    public WorkflowProjectBuilder with_default_erbs_artifacts() {
        builder.withArtifact("SiteBasic", "node-artifacts/SiteBasic.xml")
                .withArtifact("SiteInstallation", "node-artifacts/SiteInstallation.xml")
                .withArtifact("SiteEquipment", "node-artifacts/SiteEquipment.xml");
        return this;
    }

    public WorkflowProjectBuilder with_valid_configurations() {
        builder.withArtifact("configuration", "node-artifacts/TN_Data.xml")
                .withArtifact("configuration", "node-artifacts/RN_Data.xml");
        return this;
    }

    public WorkflowProjectBuilder with_license_artifact() {
        builder.withArtifact("licenseFile", "node-artifacts/license_key_file.zip");
        return this;
    }

    public WorkflowProjectBuilder with_empty_configurations() {
        builder.withArtifact("configuration", "node-artifacts/TN_Data_Empty.xml")
                .withArtifact("configuration", "node-artifacts/RN_Data_Empty.xml");
        return this;
    }

    public WorkflowProjectBuilder with_invalid_configurations_rn_file() {
        builder.withArtifact("configuration", "node-artifacts/TN_Data.xml")
                .withArtifact("configuration", "node-artifacts/RN_Data_Invalid.xml");
        return this;
    }

    public WorkflowProjectBuilder without_site_equipment_artifact() {
        builder.withArtifact("SiteBasic", "node-artifacts/SiteBasic.xml")
                .withArtifact("SiteInstallation", "node-artifacts/SiteInstallation.xml");
        return this;
    }

    public WorkflowProjectBuilder without_site_basic_artifact() {
        builder.withArtifact("SiteInstallation", "node-artifacts/SiteInstallation.xml")
                .withArtifact("SiteEquipment", "node-artifacts/SiteEquipment.xml");
        return this;
    }

    public WorkflowProjectBuilder without_site_install_artifact() {
        builder.withArtifact("SiteBasic", "node-artifacts/SiteBasic.xml")
                .withArtifact("SiteEquipment", "node-artifacts/SiteEquipment.xml");
        return this;
    }

    public WorkflowProjectBuilder with_default_security_options() {
        builder.withSecurityOption("ipSecLevel", "CUSOAM")
                .withSecurityOption("subjectAltNameType", "FQDN")
                .withSecurityOption("subjectAltName", "")
                .withSecurityOption("minimumSecurityLevel", "1")
                .withSecurityOption("optimumSecurityLevel", "2")
                .withSecurityOption("enrollmentMode", "CMPv2_VC");
        return this;
    }

    public WorkflowProjectBuilder with_default_security_options_targetGroup() {//NOSONAR
        builder.withSecurityOption("ipSecLevel", "CUSOAM")
                .withSecurityOption("subjectAltNameType", "FQDN")
                .withSecurityOption("subjectAltName", "")
                .withSecurityOption("minimumSecurityLevel", "1")
                .withSecurityOption("optimumSecurityLevel", "2")
                .withSecurityOption("enrollmentMode", "CMPv2_VC")
                .withSecurityOption("targetGroups", "AthloneTargetGroup");
        return this;
    }

    public WorkflowProjectBuilder with_default_supervision_options() {
        builder.withSupervisionOptions("fm", "enabled")
                .withSupervisionOptions("pm", "enabled")
                .withSupervisionOptions("inventory", "enabled");
        return this;
    }

    public WorkflowProjectBuilder withSupervisionOptionsManual() {
        builder.withSupervisionOptions("fm", "enabled")
                .withSupervisionOptions("pm", "enabled")
                .withSupervisionOptions("inventory", "enabled")
                .withSupervisionOptions("managementState", "MANUAL");
        return this;
    }

    public WorkflowProjectBuilder withSupervisionOptionsAutomatic() {
        builder.withSupervisionOptions("fm", "enabled")
            .withSupervisionOptions("pm", "enabled")
            .withSupervisionOptions("inventory", "enabled")
            .withSupervisionOptions("managementState", "AUTOMATIC");
        return this;
    }

    public WorkflowProjectBuilder withDefaultDhcpOptions() {
        builder
            .withDhcpOptions("initialIpAddress", "150.221.4.5/24")
            .withDhcpOptions("defaultRouter", "150.221.1.1");
        return this;
    }

    public WorkflowProjectBuilder with_default_erbs_auto_integration_options() {
        builder.withAutoIntegrationOption("uploadCVAfterIntegration", true)
                .withAutoIntegrationOption("unlockCells", true)
                .withAutoIntegrationOption("upgradePackageName", "dummy_upgrade_package")
                .withAutoIntegrationOption("basicPackageName", "dummy_basic_package");
        return this;
    }

    public WorkflowProjectBuilder with_invalid_upgrade_package_name() {
        builder.withAutoIntegrationOption("upgradePackageName", "INVALID_UPGRADE_PACKAGE");
        return this;
    }

    public WorkflowProjectBuilder with_default_license_options() {
        builder.withLicenseOptions("installLicense", Boolean.TRUE)
        .withLicenseOptions("licenseFile", "license_key_file");
        return this;
    }

    public WorkflowProjectBuilder with_user_node_credentials(final Map<String, Object> credentialData) {
        for (final Map.Entry<String, Object> entry : credentialData.entrySet()) {
            builder.withNodeUserCredentials(entry.getKey(), entry.getValue());
        }

        return this;
    }

    public WorkflowProjectBuilder with_user_node_credentials() {
        builder
                .withNodeUserCredentials("secureUserName", "user1")
                .withNodeUserCredentials("securePassword", Collections.<Byte> emptyList());

        return this;
    }

    public ProjectDescriptor build() {
        return builder.build();
    }
}