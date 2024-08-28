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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util;

import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;

/**
 * This builder class is used to create process variables needed for workflow, these will be passed into the workflow when it is initiated.
 * <p>
 * <b>Note:</b> All boolean values are <code>false</code> and all string values are <code>null</code> by default.
 */
public class ErbsWorkflowVariableBuilder {

    private boolean securityEnabled;
    private boolean uploadCvAfterIntegrationEnabled;
    private boolean installLicenseEnabled;
    private boolean activateLicenseEnabled;
    private boolean unlockCellsEnabled;
    private boolean orderSuccessful;
    private boolean createCvSuccessful;
    private boolean integrationTaskWarning;
    private boolean dhcpConfiguration;
    private String userId;
    private String apNodeFdn;
    private String cvName;
    private String hardwareSerialNumber;
    private String oldHardwareSerialNumber;

    public static ErbsWorkflowVariableBuilder newErbsWorkflowVariables() {
        return new ErbsWorkflowVariableBuilder();
    }

    private ErbsWorkflowVariableBuilder() {

    }

    public ErbsWorkflowVariableBuilder userId(final String userId) {
        this.userId = userId;
        return this;
    }

    public ErbsWorkflowVariableBuilder apNodeFdn(final String apNodeFdn) {
        this.apNodeFdn = apNodeFdn;
        return this;
    }

    public ErbsWorkflowVariableBuilder cvName(final String cvName) {
        this.cvName = cvName;
        return this;
    }

    public ErbsWorkflowVariableBuilder securityEnabled(final boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
        return this;
    }

    public ErbsWorkflowVariableBuilder uploadCvAfterIntegrationEnabled(final boolean uploadCvAfterIntegrationEnabled) {
        this.uploadCvAfterIntegrationEnabled = uploadCvAfterIntegrationEnabled;
        return this;
    }

    public ErbsWorkflowVariableBuilder installLicenseEnabled(final boolean installLicenseEnabled) {
        this.installLicenseEnabled = installLicenseEnabled;
        return this;
    }

    public ErbsWorkflowVariableBuilder activateLicenseEnabled(final boolean activateLicenseEnabled) {
        this.activateLicenseEnabled = activateLicenseEnabled;
        return this;
    }

    public ErbsWorkflowVariableBuilder unlockCellsEnabled(final boolean unlockCellsEnabled) {
        this.unlockCellsEnabled = unlockCellsEnabled;
        return this;
    }

    public ErbsWorkflowVariableBuilder integrationTaskWarning(final boolean integrationTaskWarning) {
        this.integrationTaskWarning = integrationTaskWarning;
        return this;
    }

    public ErbsWorkflowVariableBuilder orderSuccessful() {
        orderSuccessful = true;
        return this;
    }

    public ErbsWorkflowVariableBuilder setDhcpConfiguration() {
        dhcpConfiguration = true;
        return this;
    }

    public ErbsWorkflowVariableBuilder serialHardwareNumber(final String serialHardwareNumber) {
        this.hardwareSerialNumber = serialHardwareNumber;
        return this;
    }

    public ErbsWorkflowVariableBuilder oldHardwareSerialNumber(final String oldHardwareSerialNumber) {
        this.oldHardwareSerialNumber = oldHardwareSerialNumber;
        return this;
    }

    public ErbsWorkflowVariables build() {
        final ErbsWorkflowVariables workflowVariables = new ErbsWorkflowVariables();
        workflowVariables.setApNodeFdn(apNodeFdn);
        workflowVariables.setUserId(userId);
        workflowVariables.setSecurityEnabled(securityEnabled);
        workflowVariables.setLastBackupName(cvName);
        workflowVariables.setUnlockCells(unlockCellsEnabled);
        workflowVariables.setUploadCvAfterIntegrationEnabled(uploadCvAfterIntegrationEnabled);
        workflowVariables.setActivateOptionalFeatures(activateLicenseEnabled);
        workflowVariables.setInstallLicense(installLicenseEnabled);
        workflowVariables.setOrderSuccessful(orderSuccessful);
        workflowVariables.setIntegrationTaskWarning(integrationTaskWarning);
        workflowVariables.setCreateCvSuccessful(createCvSuccessful);
        workflowVariables.setDhcpConfiguration(dhcpConfiguration);
        workflowVariables.setHardwareSerialNumber(hardwareSerialNumber);
        workflowVariables.setOldHardwareSerialNumber(oldHardwareSerialNumber);

        return workflowVariables;
    }
}
