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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.OSS_ACTIVATING_CONFIGURATION;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.OSS_CONFIGURATION_FAILED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.OSS_CONFIGURATION_SUCCESSFUL;
import static com.ericsson.oss.services.cm.bulkimport.fileformat.FileFormat.THREE_GPP;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.common.cm.TransactionalExecutor;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmSender;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.ArtifactsHandler;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.cm.bulkimport.api.ImportService;
import com.ericsson.oss.services.cm.bulkimport.dto.ImportServiceSpecification;
import com.ericsson.oss.services.cm.bulkimport.response.BulkImportServiceResponse;
import com.ericsson.oss.services.cm.bulkimport.response.BulkImportServiceStatusCode;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

/**
 * Unit tests for {@link ConfigurationsImporter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationsImporterTest {

    private static final String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String ARTIFACT_TYPE = "configuration";
    private static final String BUSINESS_KEY = "AP_Node=" + NODE_NAME;

    private static final String ARTIFACT_FILE_PATH_FOR_RN = "/tmp/radio.xml";
    private static final String ARTIFACT_FILE_PATH_FOR_TN = "/tmp/transport.xml";

    private static final String IMPORT_CONFIGURATIONS_COMPLETE = "IMPORT_CONFIGURATIONS_COMPLETION";
    private static final String IMPORT_CONFIGURATIONS_SUCCESSFUL = "importConfigurationsSuccessful";

    private static final String USER_ID = "userId";

    private final ImportServiceSpecification importSpecRadio = new ImportServiceSpecification(ARTIFACT_FILE_PATH_FOR_RN, THREE_GPP, "Live");
    private final ImportServiceSpecification importSpecTransport = new ImportServiceSpecification(ARTIFACT_FILE_PATH_FOR_TN, THREE_GPP, "Live");

    @Mock
    private ImportService importService;

    @Mock
    private BulkImportServiceResponse bulkImportResponse;

    @Mock
    private Logger logger; // NOPMD

    @Mock
    private DdpTimer ddpTimer; // NOPMD

    @Mock
    private ArtifactsHandler artifactsHandler;

    @Mock
    private AlarmSender alarmSenderMock;

    @Mock
    private WorkflowInstanceServiceLocal wfsInstanceService;

    @Mock
    private RbsConfigLevelUpdater rbsConfigLevelUpdater;

    @Mock
    private TransactionalExecutor executor;

    @Mock
    private StatusEntryManagerLocal statusEntryManager;

    @Mock
    private ContextService contextService; //NOPMD

    @InjectMocks
    private final ConfigurationsImporter configurationsImporter = new ConfigurationsImporter();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception { // NOPMD
        final List<String> importFileNames = new ArrayList<>();
        importFileNames.add(ARTIFACT_FILE_PATH_FOR_TN);
        importFileNames.add(ARTIFACT_FILE_PATH_FOR_RN);

        when(artifactsHandler.getRawArtifactsLocation(NODE_FDN, ARTIFACT_TYPE)).thenReturn(importFileNames);
        when(importService.bulkImport(importSpecRadio)).thenReturn(bulkImportResponse);
        when(importService.bulkImport(importSpecTransport)).thenReturn(bulkImportResponse);
        when(executor.execute(any(Callable.class))).thenReturn(bulkImportResponse);
        when(bulkImportResponse.getStatusCode()).thenReturn(BulkImportServiceStatusCode.SUCCESS_RESPONSE);
    }

    @Test
    public void when_import_is_started_then_rbsConfig_set_to_activating() {
        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN,USER_ID);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, OSS_ACTIVATING_CONFIGURATION);
    }

    @Test
    public void when_import_configuration_files_succeeds_then_status_reported_as_successful() {
        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);
        verify(statusEntryManager).taskCompleted(eq(NODE_FDN), eq(StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString()));
    }

    @Test
    public void when_import_configuration_files_succeeds_then_rbsConfig_set_to_oss_configuration_successful() {
        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, OSS_CONFIGURATION_SUCCESSFUL);
    }

    @Test
    public void when_import_succeeds_then_successful_correlation_message_is_sent() throws WorkflowMessageCorrelationException {
        final Map<String, Object> additionalWorkflowVariables = new HashMap<>();
        additionalWorkflowVariables.put(IMPORT_CONFIGURATIONS_SUCCESSFUL, true);

        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);

        verify(wfsInstanceService).correlateMessage(IMPORT_CONFIGURATIONS_COMPLETE, BUSINESS_KEY, additionalWorkflowVariables);
    }

    @Test
    public void when_import_configuration_files_fails_then_rbsConfig_set_to_oss_configuration_failed() {
        when(bulkImportResponse.getStatusCode()).thenReturn(BulkImportServiceStatusCode.SUCCESS_RESPONSE).thenReturn(
                BulkImportServiceStatusCode.UNEXPECTED_ERROR);

        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);

        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, OSS_CONFIGURATION_FAILED);
    }

    @Test
    public void when_import_configuration_files_fails_then_status_reported_as_failed() {
        when(bulkImportResponse.getStatusCode()).thenReturn(BulkImportServiceStatusCode.SUCCESS_RESPONSE).thenReturn(
                BulkImportServiceStatusCode.UNEXPECTED_ERROR);

        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);

        verify(statusEntryManager).taskFailed(eq(NODE_FDN), eq(StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString()), anyString());
    }

    @Test
    public void when_import_configuration_files_fails_then_alarm_is_sent() {
        when(bulkImportResponse.getStatusCode()).thenReturn(BulkImportServiceStatusCode.SUCCESS_RESPONSE).thenReturn(
                BulkImportServiceStatusCode.UNEXPECTED_ERROR);

        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);

        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, OSS_CONFIGURATION_FAILED);
    }

    @Test
    public void when_import_configuration_files_fails_then_unsuccessful_correlation_message_is_sent() throws WorkflowMessageCorrelationException {
        final Map<String, Object> additionalWorkflowVariables = new HashMap<>();
        additionalWorkflowVariables.put(IMPORT_CONFIGURATIONS_SUCCESSFUL, false);

        when(bulkImportResponse.getStatusCode()).thenReturn(BulkImportServiceStatusCode.SUCCESS_RESPONSE).thenReturn(
                BulkImportServiceStatusCode.UNEXPECTED_ERROR);

        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);

        verify(wfsInstanceService).correlateMessage(IMPORT_CONFIGURATIONS_COMPLETE, BUSINESS_KEY, additionalWorkflowVariables);
    }

    @Test
    public void when_import_configuration_files_fails_then_no_attempt_to_import_remaining_files() {
        when(bulkImportResponse.getStatusCode()).thenReturn(BulkImportServiceStatusCode.UNEXPECTED_ERROR);
        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);
        verify(importService, never()).bulkImport(importSpecRadio);
    }

    @Test
    public void when_error_resolving_configuration_file_locations_then_unsuccessful_correlate_message_is_sent_and_rbsConfig_set_to_oss_configuration_failed()
            throws WorkflowMessageCorrelationException {
        final Map<String, Object> additionalWorkflowVariables = new HashMap<>();
        additionalWorkflowVariables.put(IMPORT_CONFIGURATIONS_SUCCESSFUL, false);

        doThrow(Exception.class).when(artifactsHandler).getRawArtifactsLocation(NODE_FDN, ARTIFACT_TYPE);

        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);
        verify(wfsInstanceService).correlateMessage(IMPORT_CONFIGURATIONS_COMPLETE, BUSINESS_KEY, additionalWorkflowVariables);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, OSS_CONFIGURATION_FAILED);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void when_exception_thrown_by_import_service_then_unsuccessful_correlate_message_is_sent_and_rbsConfig_set_to_oss_configuration_failed()
            throws Exception { // NOPMD
        final Map<String, Object> additionalWorkflowVariables = new HashMap<>();
        additionalWorkflowVariables.put(IMPORT_CONFIGURATIONS_SUCCESSFUL, false);
        when(executor.execute(any(Callable.class))).thenThrow(new IllegalStateException());

        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);

        verify(wfsInstanceService).correlateMessage(IMPORT_CONFIGURATIONS_COMPLETE, BUSINESS_KEY, additionalWorkflowVariables);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, OSS_CONFIGURATION_FAILED);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void when_exception_thrown_by_import_service__then_alarm_is_sent() throws Exception { // NOPMD
        when(executor.execute(any(Callable.class))).thenThrow(new IllegalStateException());
        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);
        verify(alarmSenderMock, times(1)).sendError(any(EventNotification.class));
    }

    @Test
    public void when_error_updating_rbs_config_level_then_alarm_is_sent() {
        doThrow(Exception.class).when(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.OSS_CONFIGURATION_SUCCESSFUL);
        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);
        verify(alarmSenderMock, times(1)).sendError(any(EventNotification.class));
    }

    @Test
    public void when_error_sending_alarm_after_import_failure_then_status_reported_as_failed() {
        when(bulkImportResponse.getStatusCode()).thenReturn(BulkImportServiceStatusCode.UNEXPECTED_ERROR);
        doThrow(new IllegalStateException()).when(alarmSenderMock).sendError(any(EventNotification.class));
        configurationsImporter.importConfigurations(NODE_FDN, MECONTEXT_FDN, USER_ID);
        verify(statusEntryManager).taskFailed(eq(NODE_FDN), eq(StatusEntryNames.IMPORT_CONFIGURATIONS_TASK.toString()), anyString());
    }
}
