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

import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.googlecode.catchexception.CatchException.verifyException;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.CommonArtifactDefinitions;

public class CancelSecurityTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private Dps dpsHelper;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    private static final String CANCEL_SECURITY_TASK_NAME = "Cancel Security";
    private static final String AP_SECURITY_MO_FDN = "%s,Security=1";
    private static final String AP_NODE_SMRS_LOCATION = "/home/smrs/smrsroot/ai/erbs/%s/";
    private static final String ISCF_FILE_NAME = "Iscf.xml";

    @Test
    public void when_cancel_security_is_successful_then_security_mo_attributes_are_reset_and_iscf_is_deleted() {
        stubsSteps.create_dummy_iscf_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options()
                .build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        workflowTaskEjbTestSteps.generate_node_security_data(nodeMo.getFdn());

        // Execute the test
        workflowTaskEjbTestSteps.cancel_node_security_data(nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), CANCEL_SECURITY_TASK_NAME);
        assertThat(taskStatus).as("Task Status should not be null")
                .isNotNull();
        assertThat(taskStatus).contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        // Verify the Security MO attributes were reset
        final ManagedObject securityMO = dpsHelper.findMoByFdn(String.format(AP_SECURITY_MO_FDN, nodeMo.getFdn()));
        assertThat((String) securityMO.getAllAttributes()
                .get("iscfFileLocation")).isNull();
        assertThat((String) securityMO.getAllAttributes()
                .get("securityConfigChecksum")).isNull();
        assertThat((String) securityMO.getAllAttributes()
                .get("rbsIntegrityCode")).isNull();

        // Verify the file was deleted from the file system
        final File expectedIscfLocation = new File(String.format(AP_NODE_SMRS_LOCATION, nodeMo.getName()));
        assertThat(expectedIscfLocation.listFiles())
                .as("Expected Iscf file at smrs location " + AP_NODE_SMRS_LOCATION)
                .isNull();

    }

    @Test
    public void when_cancel_security_fails_then_the_iscf_file_is_not_deleted() {
        stubsSteps.create_flawed_iscf_for_cancel_only_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);
        workflowTaskEjbTestSteps.generate_node_security_data(nodeMo.getFdn());

        // Execute the test
        verifyException(workflowTaskEjbTestSteps).cancel_node_security_data(nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), CANCEL_SECURITY_TASK_NAME);
        assertThat(taskStatus).as("Task Status should not be null")
                .isNotNull();
        assertThat(taskStatus).contains("Failed")
                .as("Expected task progress to be Failed");

        final String expectedIscfLocation = String.format(AP_NODE_SMRS_LOCATION, nodeMo.getName());

        assertThat(new File(expectedIscfLocation).listFiles())
                .extracting("name")
                .contains(ISCF_FILE_NAME)
                .as("Expected Iscf file at smrs location " + expectedIscfLocation);

    }

}
