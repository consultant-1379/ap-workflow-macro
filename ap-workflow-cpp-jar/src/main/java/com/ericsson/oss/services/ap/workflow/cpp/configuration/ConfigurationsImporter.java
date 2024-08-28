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
package com.ericsson.oss.services.ap.workflow.cpp.configuration;

import static com.ericsson.oss.services.ap.common.util.alarm.AlarmDataBuilder.SEVERITY_CRITICAL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.OSS_ACTIVATING_CONFIGURATION;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.OSS_CONFIGURATION_FAILED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.OSS_CONFIGURATION_SUCCESSFUL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.common.cm.TransactionalExecutor;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmDataBuilder;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmSender;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.common.workflow.BusinessKeyGenerator;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.ArtifactsHandler;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.cm.bulkimport.api.ImportService;
import com.ericsson.oss.services.cm.bulkimport.dto.ImportServiceSpecification;
import com.ericsson.oss.services.cm.bulkimport.fileformat.FileFormat;
import com.ericsson.oss.services.cm.bulkimport.response.BulkImportServiceResponse;
import com.ericsson.oss.services.cm.bulkimport.response.BulkImportServiceStatusCode;
import com.ericsson.oss.services.cm.bulkimport.response.dto.BulkImportServiceErrorDetails;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.internal.WorkflowInternalConstants;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

/**
 * Imports configurations and updates the <i>rbsConfigLevel</i> for the node.
 */
public class ConfigurationsImporter {

    private static final String IMPORT_CONFIGURATIONS_COMPLETION = "IMPORT_CONFIGURATIONS_COMPLETION";
    private static final String IMPORT_CONFIGURATIONS_SUCCESSFUL = "importConfigurationsSuccessful";

    private TransactionalExecutor executor = new TransactionalExecutor(); // NOPMD

    @Inject
    private ContextService contextService;

    private ImportService importService;

    private StatusEntryManagerLocal statusEntryManager;

    private WorkflowInstanceServiceLocal wfsInstanceService;

    @Inject
    private ArtifactsHandler artifactsHandler;

    @Inject
    private AlarmSender alarmSender;

    @Inject
    private DdpTimer ddpTimer;

    @Inject
    private Logger logger;

    @Inject
    private RbsConfigLevelUpdater rbsConfigLevelUpdater;

    @PostConstruct
    public void init() {
        importService = new ServiceFinderBean().find(ImportService.class);
        statusEntryManager = new ServiceFinderBean().find(StatusEntryManagerLocal.class);
        wfsInstanceService = new ServiceFinderBean().find(WorkflowInstanceServiceLocal.class);
    }

    public void importConfigurations(final String apNodeFdn, final String meContextFdn, final String userId) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        contextService.setContextValue(WorkflowInternalConstants.USERNAME_KEY, userId);
        updateRbsConfigLevel(meContextFdn, OSS_ACTIVATING_CONFIGURATION);

        try {
            ddpTimer.start(CommandLogName.IMPORT_CONFIGURATIONS.toString());
            statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString());
            importConfigFilesToLive(apNodeFdn, meContextFdn);
            updateRbsConfigLevel(meContextFdn, OSS_CONFIGURATION_SUCCESSFUL);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString());
            notifyWorkflowOfImportCompletion(nodeName, true);
            ddpTimer.end(apNodeFdn);
        } catch (final Exception e) {
            logger.error("Failed to import configuration on node {}", apNodeFdn, e);
            updateRbsConfigLevel(meContextFdn, OSS_CONFIGURATION_FAILED);
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString(), e.getMessage());
            notifyWorkflowOfImportCompletion(nodeName, false);
            ddpTimer.endWithError(apNodeFdn);
        }
    }

    private void updateRbsConfigLevel(final String meContextFdn, final RbsConfigLevel rbsConfigLevel) {

        try {
            rbsConfigLevelUpdater.updateRbsConfigLevel(meContextFdn, rbsConfigLevel);
        } catch (final Exception e) {
            final String rbsConfigurationFdn = meContextFdn + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1";
            final String probableCause = String.format("Error setting rbsConfigLevel for %s to %s", rbsConfigurationFdn, rbsConfigLevel);
            final String description = "When trying to import a configuration, could not set rbsConfigLevel on the RbsConfiguration MO";
            sendAlarm(rbsConfigurationFdn, probableCause, description);
            logger.warn("Failed to set RBS Configuration level for {} to {}", rbsConfigurationFdn, rbsConfigLevel, e);
        }
    }

    private void importConfigFilesToLive(final String apNodeFdn, final String meContextFdn) {
        final List<String> importConfigurationFiles = artifactsHandler.getRawArtifactsLocation(apNodeFdn, "configuration");
        for (final String importConfigurationFileName : importConfigurationFiles) {
            importConfigFileToLive(importConfigurationFileName, meContextFdn);
        }
    }

    private void importConfigFileToLive(final String configurationFileLocation, final String meContextFdn) {
        logger.info("Importing configuration artifacts of type {} for node {}", configurationFileLocation, meContextFdn);
        final ImportServiceSpecification importServiceSpec = new ImportServiceSpecification(configurationFileLocation, FileFormat.THREE_GPP, "Live");
        importConfigFileInNewTx(meContextFdn, importServiceSpec);
    }

    private void importConfigFileInNewTx(final String meContextFdn, final ImportServiceSpecification importServiceSpec) {
        final Callable<BulkImportServiceResponse> updateMoCallable = createImportCallable(importServiceSpec);
        BulkImportServiceResponse response = null;

        try {
            response = executor.execute(updateMoCallable);
        } catch (final Exception e) {
            sendAlarmOnImportConfigFailure(meContextFdn);
            throw new ApServiceException(String.format("Import %s failed, %s", FilenameUtils.getName(importServiceSpec.getFilePath()), e.getMessage()), e);
        }

        final BulkImportServiceStatusCode responseStatusCode = response.getStatusCode();
        if (responseStatusCode.statusCode() < 0) {
            sendAlarmOnImportConfigFailure(meContextFdn);
            throw new ApServiceException(String.format("Import %s failed, %s", FilenameUtils.getName(importServiceSpec.getFilePath()), getErrorMsgFromResponse(response)));
        }
    }

    private Callable<BulkImportServiceResponse> createImportCallable(final ImportServiceSpecification importServiceSpec) {
        return () -> importService.bulkImport(importServiceSpec);
    }

    private static String getErrorMsgFromResponse(final BulkImportServiceResponse response) {
        final BulkImportServiceErrorDetails importErrorDetails = response.getErrorDetails();
        if (importErrorDetails != null) {
            return importErrorDetails.getErrorMessage();
        }
        return "Unknown error encountered";
    }

    private void notifyWorkflowOfImportCompletion(final String nodeName, final boolean importSuccessful) {
        logger.info("Notify workflow of import completion, success={}", importSuccessful);
        final Map<String, Object> additionalWorkflowVariables = new HashMap<>();
        additionalWorkflowVariables.put(IMPORT_CONFIGURATIONS_SUCCESSFUL, importSuccessful);

        try {
            final String businessKey = BusinessKeyGenerator.generateBusinessKeyFromNodeName(nodeName);
            wfsInstanceService.correlateMessage(IMPORT_CONFIGURATIONS_COMPLETION, businessKey, additionalWorkflowVariables);
        } catch (final WorkflowMessageCorrelationException e) {
            logger.warn("Failed to correlate import success message to workflow", e);
        }
    }

    private void sendAlarmOnImportConfigFailure(final String apNodeFdn) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        final String probableCause = "Configuration import file may not be compatible with configuration on the node";
        final String description = "When trying to import a configuration, import service reported an error";
        sendAlarm(MoType.MECONTEXT + "=" + nodeName, probableCause, description);
    }

    private void sendAlarm(final String moFdn, final String probableCause, final String description) {
        try {
            alarmSender.sendError(new AlarmDataBuilder()
                    .setManagedObjectInstance(moFdn)
                    .setPerceivedSeverity(SEVERITY_CRITICAL)
                    .setEventType("Node integration error")
                    .setSpecificProblem("Failed to import configuration on node")
                    .setProbableCause(probableCause)
                    .setDescription(CommandLogName.INTEGRATE.toString() + " - " + description).build());
        } catch (final Exception e) {
            logger.warn("Error sending alarm: {}", e.getMessage(), e);
        }
    }
}