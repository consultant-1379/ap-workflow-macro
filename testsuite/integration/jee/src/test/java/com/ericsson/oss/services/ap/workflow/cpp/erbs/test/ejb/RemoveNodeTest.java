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
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowTaskEjbTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.AutoIntegrationMosGenerator;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.CommonArtifactDefinitions;

/**
 * Arquillian test class to test the remove node method.
 */
public class RemoveNodeTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowTaskEjbTestSteps workflowTaskEjbTestSteps;

    @Inject
    private Dps dpsHelper;

    @Inject
    private AutoIntegrationMosGenerator integrationMosGenerator;

    private static final String REMOVE_NODE_TASK_NAME = "Remove Node";

    @Test
    public void when_remove_node_successfully_then_networkElement_and_cppconnectivityinformation_mos_should_not_exist() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        integrationMosGenerator.generateNetworkElementChildMos(nodeMo.getFdn());

        workflowTaskEjbTestSteps.remove_cpp_node(nodeMo.getFdn());

        assertThat(dpsHelper).hasNotManagedObject("NetworkElement=%s", nodeMo.getName())
                .hasNotManagedObject("NetworkElement=%s,CppConnectivityInformation=1", nodeMo.getName());

        final String taskStatus = dataSteps.get_ap_task_status(nodeMo.getFdn(), REMOVE_NODE_TASK_NAME);

        assertThat(taskStatus).contains(CommonArtifactDefinitions.TASK_PROGRESS_SUCCESS).as("Expected task progress to be Completed");
    }

    @Test
    public void when_no_network_element_exists_then_no_exception_is_thrown() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode().build();
        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        workflowTaskEjbTestSteps.remove_cpp_node(nodeMo.getFdn());
    }

}
