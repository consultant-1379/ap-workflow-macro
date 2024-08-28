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

import static com.ericsson.oss.services.ap.arquillian.util.data.validation.DpsAssert.assertThat;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.CatchException.verifyException;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.exception.NodeNotFoundException;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs.DummyISCFService;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.CommonArtifactDefinitions;

public class GenerateSecurityTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private Dps dpsHelper;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    private static final String GENERATE_SECURITY_TASK_NAME = "Generate Security";
    private static final String AP_SECURITY_MO_FDN = "%s,Security=1";
    private static final String AP_NODE_SMRS_LOCATION = "/home/smrs/smrsroot/ai/erbs/%s/Iscf.xml";

    @Test
    public void when_generate_oam_and_ipsec_security_is_successful_then_security_mo_contains_iscf_path_and_checksum() {
        stubsSteps.create_dummy_iscf_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        // Execute the test
        workflowTaskEjbTestSteps.generate_node_security_data(nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), GENERATE_SECURITY_TASK_NAME);
        assertThat(taskStatus).as("Task Status should not be null")
                .isNotNull();
        assertThat(taskStatus).contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        final String expectedIscfLocation = String.format(AP_NODE_SMRS_LOCATION, nodeMo.getName());
        assertThat(dpsHelper).withManagedObject(AP_SECURITY_MO_FDN, nodeMo.getFdn())
                .as("Assertions for Security MO Updates")
                .withAttributeValue("iscfFileLocation", expectedIscfLocation)
                .withAttributeValue("securityConfigChecksum", DummyISCFService.SECURITY_CHECKSUM)
                .withAttributeValue("rbsIntegrityCode", DummyISCFService.RBS_INTEGRATION_CODE);

    }

    @Test
    public void when_generate_oam_security_is_successful_then_security_mo_contains_iscf_path_and_checksum() {
        stubsSteps.create_dummy_iscf_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        // Execute the test
        workflowTaskEjbTestSteps.generate_node_security_data(nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), GENERATE_SECURITY_TASK_NAME);
        assertThat(taskStatus).as("Task Status should not be null")
                .isNotNull();
        assertThat(taskStatus).contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS)
                .as("Expected task progress to be Completed");

        final String expectedIscfLocation = String.format(AP_NODE_SMRS_LOCATION, nodeMo.getName());
        assertThat(dpsHelper).withManagedObject(AP_SECURITY_MO_FDN, nodeMo.getFdn())
                .as("Assertions for Security MO Updates")
                .withAttributeValue("iscfFileLocation", expectedIscfLocation)
                .withAttributeValue("securityConfigChecksum", DummyISCFService.SECURITY_CHECKSUM)
                .withAttributeValue("rbsIntegrityCode", DummyISCFService.RBS_INTEGRATION_CODE);

    }

    @Test
    public void when_iscf_service_throws_exception_then_the_iscf_file_is_not_generated() {
        stubsSteps.create_flawed_iscf_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        // Execute the test
        verifyException(workflowTaskEjbTestSteps).generate_node_security_data(nodeMo.getFdn());

        // Verify the status updates for the ejb call
        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), GENERATE_SECURITY_TASK_NAME);
        assertThat(taskStatus).as("Task Status should not be null")
                .isNotNull();
        assertThat(taskStatus).contains("Failed")
                .as("Expected task progress to be Failed");

        final ManagedObject securityMO = dpsHelper.findMoByFdn(String.format(AP_SECURITY_MO_FDN, nodeMo.getFdn()));
        assertThat((String) securityMO.getAllAttributes().get("iscfFileLocation")).isNull();

    }

    @Test
    public void when_security_mo_not_found_then_then_the_iscf_file_is_not_generated() {
        stubsSteps.create_flawed_iscf_service_stub();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().with_default_security_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        // Execute the test
        verifyException(workflowTaskEjbTestSteps).generate_node_security_data(nodeMo.getFdn() + "111111");

        assertThat(caughtException().getCause()
                .getClass())
                        .isEqualTo(NodeNotFoundException.class)
                        .as("NodeNotFoundException was expected");

        final ManagedObject securityMO = dpsHelper.findMoByFdn(String.format(AP_SECURITY_MO_FDN, nodeMo.getFdn()));
        assertThat((String) securityMO.getAllAttributes().get("iscfFileLocation")).isNull();

    }

}
