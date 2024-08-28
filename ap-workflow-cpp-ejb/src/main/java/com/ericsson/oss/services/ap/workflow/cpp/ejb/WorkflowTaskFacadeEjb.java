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
package com.ericsson.oss.services.ap.workflow.cpp.ejb;

import static com.ericsson.oss.services.ap.api.status.StatusEntryNames.GENERATE_PROVISIONING_ARTIFACTS;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_CHECK_POSITION;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.services.ap.api.exception.DhcpRestServiceException;
import com.ericsson.oss.services.ap.api.exception.ValidationException;
import com.ericsson.oss.services.ap.api.model.DhcpConfiguration;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.api.validation.ValidationContext;
import com.ericsson.oss.services.ap.api.validation.ValidationEngine;
import com.ericsson.oss.services.ap.api.workflow.DhcpRestClientService;
import com.ericsson.oss.services.ap.common.cm.AddNodeRequest;
import com.ericsson.oss.services.ap.common.cm.NodeOperations;
import com.ericsson.oss.services.ap.common.cm.TargetGroupOperations;
import com.ericsson.oss.services.ap.common.cm.UserOperations;
import com.ericsson.oss.services.ap.common.model.NetworkElementAttribute;
import com.ericsson.oss.services.ap.common.model.SupervisionMoType;
import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.common.validation.ValidationRuleGroups;
import com.ericsson.oss.services.ap.common.workflow.recording.ErrorRecorder;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.ArtifactsHandler;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.BindNode;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.ConfigurationsImporter;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.CvManager;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.RbsConfigLevelUpdater;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.UnbindNode;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.cells.CellUnlockManager;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.features.FeatureActivator;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.ap.workflow.cpp.security.SecurityManager;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.ImportLicenseRemoteResponse;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.LicenseFileManagerService;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.errorcodes.LicenseErrorCodes;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.DeleteLicenseException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.ImportLicenseException;

/**
 * Facade to handle workflow order and integration tasks on a node.
 */
@Remote
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class WorkflowTaskFacadeEjb implements WorkflowTaskFacade { // NOPMD TooManyFields

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @EServiceRef
    private StatusEntryManagerLocal statusEntryManager;

    @Inject
    private NodeOperations nodeOperations;

    @Inject
    private TargetGroupOperations targetGroupOperations;

    @Inject
    private UserOperations userOperations;

    @Inject
    private CvManager cvManager;

    @Inject
    private CellUnlockManager cellUnlockManager;

    @Inject
    private FeatureActivator featureActivator;

    @Inject
    private RbsConfigLevelUpdater rbsConfigLevelUpdater;

    @Inject
    private ConfigurationsImporter configurationImporter;

    @Inject
    private SecurityManager securityManager;

    @Inject
    private ArtifactsHandler artifactsHandler;

    @Inject
    private BindNode bindNode;

    @Inject
    private UnbindNode unbindNode;

    @Inject
    private ErrorRecorder errorRecorder;

    @Inject
    private ValidationEngine validationEngine;

    @EServiceRef
    private LicenseFileManagerService licenseFileManagerService;

    @EServiceRef
    private DhcpRestClientService dhcpRestClientService;

    @Override
    public void validateBulkConfiguration(final String apNodeFdn) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.VALIDATE_CONFIGURATIONS_TASK.toString());

        final List<String> bulkConfigurationFiles = artifactsHandler.getRawArtifactsLocation(apNodeFdn, "configuration");
        final Map<String, Object> validationTarget = new HashMap<>();

        validationTarget.put("nodeFdn", apNodeFdn);
        validationTarget.put("configFiles", bulkConfigurationFiles);
        final ValidationContext context = new ValidationContext(ValidationRuleGroups.ORDER_WORKFLOW, validationTarget);
        try {
            final boolean isValidationSuccess = validationEngine.validate(context);
            handleValidationErrors(isValidationSuccess, apNodeFdn, context);
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.VALIDATE_CONFIGURATIONS_TASK.toString(), e.getMessage());
            errorRecorder.validationFailed(apNodeFdn, e);
            throw e;
        }
    }

    private void handleValidationErrors(final boolean validationSuccess, final String apNodeFdn, final ValidationContext context) {
        if (!validationSuccess) {
            final List<String> validationErrors = context.getValidationErrors();
            throw new ValidationException(validationErrors.toString());
        } else {
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.VALIDATE_CONFIGURATIONS_TASK.toString());
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void addNode(final String apNodeFdn) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.ADD_NODE_TASK.toString());

        final AddNodeRequest addNodeRequest = new AddNodeRequest.Builder(apNodeFdn).connInfoNamespace("CPP_MED")
            .connInfoModelName("CppConnectivityInformation")
            .addNetworkElementAttribute(NetworkElementAttribute.PLATFORM_TYPE.toString(), "CPP")
            .addConnInformationAttribute("port", 80)
            .build();

        try {
            nodeOperations.addNode(addNodeRequest);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.ADD_NODE_TASK.toString());
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.ADD_NODE_TASK.toString(), getRootCause(e));
            errorRecorder.addNodeFailed(apNodeFdn, e);
            throw e;
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void removeNode(final String apNodeFdn) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.REMOVE_NODE_TASK.toString());

        try {
            nodeOperations.removeNode(nodeName);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.REMOVE_NODE_TASK.toString());
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.REMOVE_NODE_TASK.toString(), getRootCause(e));
            errorRecorder.removeNodeFailed(apNodeFdn, e);
            throw e;
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void syncNode(final String apNodeFdn) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.SYNC_NODE.toString());
        try {
            final String nodeName = FDN.get(apNodeFdn).getRdnValue();
            nodeOperations.activateCmNodeHeartbeatSupervision(nodeName);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.SYNC_NODE.toString());
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.SYNC_NODE.toString(), getRootCause(e));
            errorRecorder.syncNodeFailed(apNodeFdn, e);
        }
    }

    @Override
    public void enableSupervision(final String apNodeFdn, final SupervisionMoType supervisionMoType) {
        final String taskName = String.format("Enable %s Supervision", supervisionMoType.toString());
        statusEntryManager.taskStarted(apNodeFdn, taskName);
        try {
            final String nodeName = FDN.get(apNodeFdn).getRdnValue();
            nodeOperations.setSupervisionStatus(nodeName, supervisionMoType,true);
            statusEntryManager.taskCompleted(apNodeFdn, taskName);
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, taskName, e.getMessage());
            errorRecorder.updateSupervisionStatusFailed(apNodeFdn, supervisionMoType.toString(), e,CommandLogName.ENABLE.toString(),CommandLogName.INTEGRATE.toString());
            throw e;
        }
    }

    @Override
    public void bindNodeDuringOrder(final String apNodeFdn, final String hardwareSerialNumber, final CppNodeType nodeType) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.HARDWARE_BIND_TASK.toString());
        try {
            bindNode.executeBindDuringOrder(apNodeFdn, nodeType);
            final String additionalInfo = "Hardware Serial Number: " + hardwareSerialNumber;
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.HARDWARE_BIND_TASK.toString(), additionalInfo);
        } catch (final Exception e) {
            errorRecorder.bindNodeFailed(apNodeFdn, hardwareSerialNumber, e);
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.HARDWARE_BIND_TASK.toString(), getRootCause(e));
            throw e;
        }
    }

    @Override
    public void bindNodeManually(final String apNodeFdn, final String hardwareSerialNumber, final CppNodeType nodeType) {
        try {
            bindNode.executeManualBind(apNodeFdn, hardwareSerialNumber, nodeType);
            final String additionalInfo = "Hardware Serial Number: " + hardwareSerialNumber;
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.HARDWARE_BIND_TASK.toString(), additionalInfo);
        } catch (final Exception e) {
            errorRecorder.bindNodeFailed(apNodeFdn, hardwareSerialNumber, e);
            throw e;
        }
    }

    @Override
    public void unbindNode(final String apNodeFdn) {
        unbindNode.execute(apNodeFdn);
    }

    @Override
    public String createCV(final String apNodeFdn, final String meContextFdn, final String cvComment) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.CREATE_CV_TASK.toString());

        String createdCvName = null;
        try {
            final String nodeName = FDN.get(apNodeFdn).getRdnValue();
            createdCvName = cvManager.createCv(nodeName, meContextFdn, cvComment);
        } catch (final Exception e) {
            errorRecorder.createCvFailed(apNodeFdn, e);
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.CREATE_CV_TASK.toString(), getRootCause(e));
            throw e;
        }

        statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.CREATE_CV_TASK.toString(), "CV Name: " + createdCvName);
        return createdCvName;
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void uploadCV(final String apNodeFdn, final String cvName) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.UPLOAD_CV_TASK.toString());

        try {
            final String nodeName = FDN.get(apNodeFdn).getRdnValue();
            final boolean uploadSuccessful = cvManager.uploadCv(nodeName, cvName);
            if (uploadSuccessful) {
                statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.UPLOAD_CV_TASK.toString());
            } else {
                errorRecorder.uploadCvFailed(apNodeFdn);
                statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.UPLOAD_CV_TASK.toString(), "Upload CV failed");
            }
        } catch (final Exception e) {
            errorRecorder.uploadCvFailed(apNodeFdn, e);
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.UPLOAD_CV_TASK.toString(), getRootCause(e));
        }
    }

    @Override
    public void unlockCells(final String apNodeFdn, final String meContextFdn) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.UNLOCK_CELLS.toString());

        try {
            cellUnlockManager.unlock(meContextFdn);
        } catch (final Exception e) {
            errorRecorder.unlockCellsFailed(apNodeFdn, e);
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.UNLOCK_CELLS.toString(), getRootCause(e));
            throw e;
        }

        statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.UNLOCK_CELLS.toString());
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void activateOptionalFeatures(final String apNodeFdn, final String meContextFdn) {
        featureActivator.activateOptionalFeatures(apNodeFdn, meContextFdn);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void updateRbsConfigLevel(final String meContextFdn, final RbsConfigLevel newRbsConfigLevel) {
        rbsConfigLevelUpdater.updateRbsConfigLevel(meContextFdn, newRbsConfigLevel);
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void importConfigurations(final String apNodeFdn, final String meContextFdn, final String userId) {
        configurationImporter.importConfigurations(apNodeFdn, meContextFdn, userId);
    }

    @Override
    public void createGeneratedArtifact(final String artifactType, final String apNodeFdn, final CppNodeType nodeType) {
        statusEntryManager.taskStarted(apNodeFdn, GENERATE_PROVISIONING_ARTIFACTS.toString());
        try {
            artifactsHandler.createGeneratedArtifact(artifactType, apNodeFdn, nodeType);
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, GENERATE_PROVISIONING_ARTIFACTS.toString(), getRootCause(e));
            errorRecorder.createdGeneratedArtifactFailed(apNodeFdn, artifactType, e);
            throw e;
        }
        if(ArtifactType.getArtifactType(artifactType).equals(ArtifactType.SITEINSTALL)){
            statusEntryManager.taskCompleted(apNodeFdn, GENERATE_PROVISIONING_ARTIFACTS.toString());
        }
    }

    @Override
    public void uploadGeneratedArtifact(final String artifactType, final String apNodeFdn, final CppNodeType nodeType) {
        try {
            artifactsHandler.createGeneratedArtifact(artifactType, apNodeFdn, nodeType);
        } catch (final Exception e) {
            errorRecorder.createdGeneratedArtifactFailed(apNodeFdn, artifactType, e);
            throw e;
        }
    }

    @Override
    public void deleteGeneratedArtifact(final String artifactType, final String apNodeFdn) {
        final String[] artifactName = StringUtils.splitByCharacterTypeCamelCase(artifactType);
        final String taskName = String.format("Delete %s File", StringUtils.join(artifactName, " "));
        statusEntryManager.taskStarted(apNodeFdn, taskName);

        try {
            artifactsHandler.deleteGeneratedArtifact(artifactType, apNodeFdn);
            statusEntryManager.taskCompleted(apNodeFdn, taskName);
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, taskName, getRootCause(e));
            errorRecorder.deleteGeneratedArtifactFailed(apNodeFdn, artifactType, e);
            throw e;
        }
    }

    @Override
    public List<String> getRawArtifactsLocation(final String apNodeFdn, final String artifactType) {
        return artifactsHandler.getRawArtifactsLocation(apNodeFdn, artifactType);
    }

    @Override
    public void enableSecurity(final String apNodeFdn, final CppNodeType cppNodeType) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.GENERATE_SECURITY_TASK.toString());
        try {
            securityManager.enableSecurity(apNodeFdn, cppNodeType);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.GENERATE_SECURITY_TASK.toString());
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.GENERATE_SECURITY_TASK.toString(), getRootCause(e));
            errorRecorder.generatedSecurityFailed(apNodeFdn, e);
            throw e;
        }
    }

    @Override
    public void cancelSecurity(final String apNodeFdn, final CppNodeType cppNodeType) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.CANCEL_SECURITY_TASK.toString());
        try {
            securityManager.cancelSecurity(apNodeFdn, cppNodeType);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.CANCEL_SECURITY_TASK.toString());
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.CANCEL_SECURITY_TASK.toString(), getRootCause(e));
            errorRecorder.cancelSecurityFailed(apNodeFdn, e);
            throw e;
        }
    }

    @Override
    public void cleanUpOnCompletion(final String apNodeFdn, final boolean securityEnabled, final CppNodeType cppNodeType) {
        logger.info("Executing cleanup for node {}", apNodeFdn);

        if (securityEnabled) {
            cleanUpSecurity(apNodeFdn, cppNodeType);
        }

        // Must delete artifacts after workflow, or else cancelling security will regenerate SMRS directory
        cleanUpNodeArtifacts(apNodeFdn);
    }

    private void cleanUpNodeArtifacts(final String apNodeFdn) {
        try {
            logger.info("Deleting all raw and generated artifacts for node {}", apNodeFdn);
            artifactsHandler.deleteAllArtifacts(apNodeFdn);
        } catch (final Exception e) {
            logger.warn("Error deleting node artifacts after integration", e);
        }
    }

    private void cleanUpSecurity(final String apNodeFdn, final CppNodeType cppNodeType) {
        try {
            logger.info("Cancelling security for node {}", apNodeFdn);
            securityManager.cancelSecurity(apNodeFdn, cppNodeType);
        } catch (final Exception e) {
            logger.warn("Error cancelling security after integration", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void initiateGpsPositionCheck(final String apNodeFdn, final String meContextFdn) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.GPS_POSITION_CHECK_TASK.toString());

        try {
            rbsConfigLevelUpdater.updateRbsConfigLevel(meContextFdn, GPS_CHECK_POSITION);
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), getRootCause(e));
            throw e;
        }
    }

    private static String getRootCause(final Exception e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        return rootCause == null ? e.getMessage() : rootCause.getMessage();
    }

    @Override
    public void createNodeUserCredentials(final String apNodeFdn) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.CREATE_NODE_USER_CREDENTIALS.toString());

        try {
            userOperations.createNodeCredentials(apNodeFdn);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.CREATE_NODE_USER_CREDENTIALS.toString());
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.CREATE_NODE_USER_CREDENTIALS.toString(), e.getMessage());
            errorRecorder.createNodeCredentialsFailed(apNodeFdn, e);
            throw e;
        }
    }

    @Override
    public ImportLicenseRemoteResponse importLicenseKeyFile(final String apNodeFdn) throws ImportLicenseException {
        logger.info("Import License Key to SHM for the node {}", apNodeFdn);
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());
        try {
            final List<String> findRawArtifactLocationsOfType = getRawArtifactsLocation(apNodeFdn, ArtifactType.LICENSEFILE.toString());
            final String rawFilePath = findRawArtifactLocationsOfType.get(0);
            logger.info("License File Path located {} for the node {}", rawFilePath, apNodeFdn);
            final ImportLicenseRemoteResponse importLicenseRemoteResponse = licenseFileManagerService.importLicenseKeyFile(rawFilePath);
            logger.info("License key file imported to SHM for the node {}", apNodeFdn);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());
            return importLicenseRemoteResponse;
        } catch (final ImportLicenseException importLicenseException) {
            return handleImportLicenseException(apNodeFdn, importLicenseException);
        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString(), e.getMessage());
            errorRecorder.importLicenseKeyFileFailed(apNodeFdn, e);
            throw e;
        }
    }

    private ImportLicenseRemoteResponse handleImportLicenseException(final String apNodeFdn, final ImportLicenseException importLicenseException)
        throws ImportLicenseException {
        if (importLicenseException.getErrorCode().equals(LicenseErrorCodes.FILE_ALREADY_EXISTS.getErrorCode())) {
            logger.info("License File already exists for the node {}", apNodeFdn);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString(), "License Key File already exists");
            return new ImportLicenseRemoteResponse("", "");
        } else {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString(), importLicenseException.getMessage());
            errorRecorder.importLicenseKeyFileFailed(apNodeFdn, importLicenseException);
            throw importLicenseException;
        }
    }

    @Override
    public void deleteLicenseKeyFile(final String fingerPrint, final String sequenceNumber, final String apNodeFdn) throws DeleteLicenseException {
        logger.info("Delete LKF for node {} with fingerPrint {} and sequenceNumber {}", apNodeFdn, fingerPrint, sequenceNumber);
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.DELETE_LICENSE_KEY_FILE_TASK.toString());
        try {
            licenseFileManagerService.deleteLicense(fingerPrint, sequenceNumber);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.DELETE_LICENSE_KEY_FILE_TASK.toString());

        } catch (final Exception e) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.DELETE_LICENSE_KEY_FILE_TASK.toString(), e.getMessage());
            errorRecorder.deleteLicenseKeyFileFailed(apNodeFdn, e);
            throw e;
        }
    }

    @Override
    public void configureManagementState(final String apNodeFdn, final String maintenanceValue) {
        logger.info("Configure Management State for node {}", apNodeFdn);
        try {
            final boolean isUpdateRequired = nodeOperations.isUpdateValid(maintenanceValue, apNodeFdn);

            if (isUpdateRequired) {
                statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.SET_MANAGEMENT_STATE.toString());
                nodeOperations.setManagementState(apNodeFdn, maintenanceValue);
                statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.SET_MANAGEMENT_STATE.toString(),
                    String.format("Management State: %s", maintenanceValue));
            }
        } catch (final Exception e) {
            logger.warn("Exception updating managementState for node {} and exception stacktrace {}", apNodeFdn, e.getStackTrace());
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.SET_MANAGEMENT_STATE.toString(), e.getMessage());
        }
    }

    @Override
    public boolean configureDhcp(final String apNodeFdn, final String oldHardwareSerialNumber, final DhcpConfiguration dhcpConfiguration) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.DHCP_CONFIGURATION.toString());
        final String hostname = FDN.get(apNodeFdn).getRdnValue();
        try {
            if (isBlank(oldHardwareSerialNumber)) {
                logger.info("Create DHCP configuration: {}", dhcpConfiguration);
                dhcpRestClientService.create(hostname, dhcpConfiguration.getClientIdentifier(), dhcpConfiguration.getFixedAddress(),
                    dhcpConfiguration.getDefaultRouter(), dhcpConfiguration.getNtpServers(), dhcpConfiguration.getDomainNameServers());
                statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.DHCP_CONFIGURATION.toString());
            } else {
                logger.info("Update already existing DHCP configuration for clientId {} to new configuration {}", oldHardwareSerialNumber,
                    dhcpConfiguration);
                dhcpRestClientService
                    .update(hostname, oldHardwareSerialNumber, dhcpConfiguration.getClientIdentifier(), dhcpConfiguration.getFixedAddress(),
                        dhcpConfiguration.getDefaultRouter(), dhcpConfiguration.getNtpServers(), dhcpConfiguration.getDomainNameServers());
                statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.DHCP_CONFIGURATION.toString());
            }
            return true;
        } catch (final DhcpRestServiceException ex) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.DHCP_CONFIGURATION.toString(), ex.getMessage());
        }
        return false;
    }

    @Override
    public void removeDhcpClient(final String apNodeFdn, final String hardwareSerialNumber) {
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.DHCP_REMOVE_CLIENT.toString());
        try {
            logger.info("Remove DHCP configuration for node: {}, clientId: {}", apNodeFdn, hardwareSerialNumber);
            dhcpRestClientService.delete(hardwareSerialNumber);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.DHCP_REMOVE_CLIENT.toString());
        } catch (final DhcpRestServiceException ex) {
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.DHCP_REMOVE_CLIENT.toString(), ex.getMessage());
        }
    }

    @Override
    public void assignTargetGroups(String apNodeFdn) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        logger.info("Assigning Target groups for Node{}", nodeName);
        final List<String> targetGroupList = nodeOperations.getTargetGroup(apNodeFdn);
        if(targetGroupList.isEmpty()){
            return;
        }
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.ASSIGN_TARGET_GROUP.toString());

        try {
            logger.info("Adding List of Target groups for targetGroupList {}", targetGroupList);
            targetGroupOperations.addTargetsToTargetGroup(Arrays.asList(nodeName), targetGroupList);
        }
        catch (final RetriableCommandException  e) {
            final Throwable rootCause = e.getCause();
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.ASSIGN_TARGET_GROUP.toString(), rootCause.getMessage());
            errorRecorder.assignTargetGroupFailed(apNodeFdn, (Exception) rootCause);
            throw e;
        }
        statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.ASSIGN_TARGET_GROUP.toString(),"Target Groups: "+String.join(" ", targetGroupList));
    }
}