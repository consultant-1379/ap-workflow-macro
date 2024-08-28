/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.ejb;

import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.api.status.StatusEntryProgress;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.ImportLicenseException;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs.DummyLicenseFileManagerService;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.ImportLicenseRemoteResponse;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

public class ImportLicenseKeyFileTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Test
    @Features("Import License Key File to SHM Success")
    @Stories({"WHEN import license key file is called THEN import file to shm And success task completed"})
    public void when_import_license_key_file_is_called_then_import_file_to_shm_and_success_task_completed() throws ImportLicenseException {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_license_artifact()
                .with_default_license_options()
                .build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        final NodeStatusEntriesListener firstStatusEntriesListener = new NodeStatusEntriesListener();
        firstStatusEntriesListener.listenUntilEntryCompletes(nodeMo.getFdn(), StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());

        // Execute the Test
        workflowTaskEjbTestSteps.import_license_key_file(nodeMo.getFdn());

        firstStatusEntriesListener.waitForResults(5, TimeUnit.SECONDS);

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());

        assertThat(taskStatus).isNotNull().as("Task Status should not be null");

        assertThat(taskStatus).contains(StatusEntryProgress.COMPLETED.toString()).as("Expected task progress to be Completed");
    }

    @Test
    @Features("Import License Key File to SHM Failed")
    @Stories({"WHEN import license key file is called THEN error import file to shm And task failed"})
    public void when_import_license_key_file_is_called_then_error_import_file_to_shm_and_task_failed() throws ImportLicenseException {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_default_erbs_artifacts()
                .with_default_license_options()
                .build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        final NodeStatusEntriesListener firstStatusEntriesListener = new NodeStatusEntriesListener();
        firstStatusEntriesListener.listenUntilEntryCompletes(nodeMo.getFdn(), StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());

        try {
            // Execute the Test
            workflowTaskEjbTestSteps.import_license_key_file(nodeMo.getFdn());

            firstStatusEntriesListener.waitForResults(5, TimeUnit.SECONDS);
        } catch (final Exception e) {
            // Verify the status updates for the ejb call
            final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());

            assertThat(taskStatus).isNotNull().as("Task Status should not be null");

            assertThat(taskStatus).contains(StatusEntryProgress.FAILED.toString()).as("Expected task progress to be Failed");
        }
    }

    @Test
    @Features("Import License Key File to SHM Throws License File Already Exists Exception")
    @Stories({"WHEN import license key file is called THEN license file already exists exception is thrown by SHM and Task completed successfully"})
    public void when_import_license_key_file_is_called_then_license_file_already_exist_exception_is_thrown_by_shm() throws ImportLicenseException {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_license_artifact()
                .with_default_license_options()
                .build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        final NodeStatusEntriesListener firstStatusEntriesListener = new NodeStatusEntriesListener();
        firstStatusEntriesListener.listenUntilEntryCompletes(nodeMo.getFdn(), StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());
        DummyLicenseFileManagerService.throwImportLicenseException = true;
        final ImportLicenseRemoteResponse importLicenseRemoteResponse = workflowTaskEjbTestSteps.import_license_key_file(nodeMo.getFdn());
        firstStatusEntriesListener.waitForResults(5, TimeUnit.SECONDS);
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.IMPORT_LICENSE_KEY_FILE_TASK.toString());

        assertThat(importLicenseRemoteResponse.getSequenceNumber()).isNotNull().isEqualTo("");
        assertThat(importLicenseRemoteResponse.getFingerPrint()).isNotNull().isEqualTo("");
        assertThat(taskStatus).isNotNull().as("Task Status should not be null");

        assertThat(taskStatus).contains(StatusEntryProgress.COMPLETED.toString()).as("License Key File already exists");
    }
}