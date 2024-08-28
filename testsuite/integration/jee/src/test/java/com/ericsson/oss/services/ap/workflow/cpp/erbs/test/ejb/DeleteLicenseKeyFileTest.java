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
import static org.assertj.core.api.Assertions.assertThat;

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
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs.DummyLicenseFileManagerService;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.ImportLicenseRemoteResponse;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.DeleteLicenseException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.ImportLicenseException;

/**
 * Arquillian test class to test the delete LKF method.
 */
public class DeleteLicenseKeyFileTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Test
    public void whenDeleteLicenseKeyFileIsExecuted_thenLKFShouldBeDeleted() throws DeleteLicenseException, ImportLicenseException {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_security_options()
                .with_license_artifact()
                .with_default_license_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        DummyLicenseFileManagerService.throwExceptionOnDeleteLicense = false;

        final NodeStatusEntriesListener firstStatusEntriesListener = new NodeStatusEntriesListener();
        firstStatusEntriesListener.listenUntilEntryCompletes(nodeMo.getFdn(), StatusEntryNames.DELETE_LICENSE_KEY_FILE_TASK.toString());

        // import LKF to be deleted
        final ImportLicenseRemoteResponse importLicenseRemoteResponse = workflowTaskEjbTestSteps.import_license_key_file(nodeMo.getFdn());

        final String fingerPrint = importLicenseRemoteResponse.getFingerPrint();
        final String sequenceNumber = importLicenseRemoteResponse.getSequenceNumber();

        workflowTaskEjbTestSteps.delete_license_key_file(fingerPrint, sequenceNumber, nodeMo.getFdn());

        firstStatusEntriesListener.waitForResults(5, TimeUnit.SECONDS);

        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.DELETE_LICENSE_KEY_FILE_TASK.toString());

        assertThat(taskStatus).isNotNull().as("Task Status should not be null");
        assertThat(taskStatus).contains(StatusEntryProgress.COMPLETED.toString()).as("Expected task progress to be Completed");
    }

    @Test
    public void whenDeleteLicenseKeyFileIsExecuted_andErrorOccurs_thenTaskIsExpectedToFail() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_security_options()
                .with_license_artifact()
                .with_default_license_options()
                .build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        DummyLicenseFileManagerService.throwExceptionOnDeleteLicense = true;
        try {
            final ImportLicenseRemoteResponse importLicenseRemoteResponse = workflowTaskEjbTestSteps.import_license_key_file(nodeMo.getFdn());
            final String fingerPrint = importLicenseRemoteResponse.getFingerPrint();
            final String sequenceNumber = importLicenseRemoteResponse.getSequenceNumber();

            workflowTaskEjbTestSteps.delete_license_key_file(fingerPrint, sequenceNumber, nodeMo.getFdn());

        } catch (final Exception e) {
            final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), StatusEntryNames.DELETE_LICENSE_KEY_FILE_TASK.toString());

            assertThat(taskStatus).isNotNull().as("Task Status should not be null");

            assertThat(taskStatus).contains(StatusEntryProgress.FAILED.toString()).as("Expected task progress to be Failed");
        }
    }

}
