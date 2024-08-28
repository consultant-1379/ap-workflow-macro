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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test;

import static com.ericsson.oss.services.ap.arquillian.util.data.validation.DpsAssert.assertThat;
import static com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener.getIntegrationCompleteStates;
import static com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener.getOrderCompleteStates;
import static com.ericsson.oss.services.ap.arquillian.util.data.workflow.WorkflowFunctionsFactory.newCreateCiRefAssociationFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newCreateErbsNodeMosFunctionWithSubNetworks;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newGpsPositionSuccessfullyMatchedFunctionWithSubNetworks;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newNodeStateIntegrationStartedFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newNodeUpFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newS1CompleteFunctionWithSubNetworks;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.FunctionsFactory.newSiteConfigCompleteFunction;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Jndi;
import com.ericsson.oss.services.ap.arquillian.util.data.managedobject.OSSMosGenerator;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesResult;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.AutoProvisioningServiceTestSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowStubbedServicesSteps;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Arquillian test class to test the end to end AutoIntegration initiated through AutoProvisioningService.
 */
public class EndToEndErbsAutoIntegrationTest extends Arquillian {

    private static final String NETWORK_ELEMENT_MO = "NetworkElement=%s";

    @Inject
    private Dps dps;

    @Inject
    private Jndi jndi;

    @Inject
    private AutoProvisioningServiceTestSteps apServiceSteps;

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowStubbedServicesSteps stubsSteps;

    @Inject
    private OSSMosGenerator ossMosGenerator;

    /**
     * Test is verified by EndToEndErbsWorkflowSpec
     */
    @Test(enabled = false) 
    @Features("AP Auto Integration")
    @Stories({ "WHEN integrating node THEN auto-integration succeeds" })
    public void when_integrating_node_then_auto_integration_succeeds() throws InterruptedException {
        stubsSteps.create_default_stubs();
        dataSteps.setApUser();

        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_empty_configurations()
            .with_default_erbs_artifacts()
            .with_default_security_options()
            .with_default_supervision_options()
            .with_default_erbs_auto_integration_options()
            .withDefaultDhcpOptions()
            .build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        final String nodeStatusFdn = node.getFdn() + ",NodeStatus=1";
        assertThat(dps).withManagedObject(nodeStatusFdn).withAttributeValue("state", "READY_FOR_ORDER");

        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onEnd(StatusEntryNames.ADD_NODE_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator))
            .onStart(StatusEntryNames.NODE_UP.toString(), newNodeUpFunction(jndi))
            .onEnd(StatusEntryNames.NODE_UP.toString(), newNodeStateIntegrationStartedFunction(dps))
            .onStart(StatusEntryNames.SITE_CONFIG_COMPLETE.toString(), newSiteConfigCompleteFunction(jndi))
            .onEnd(StatusEntryNames.SYNC_NODE.toString(), newCreateErbsNodeMosFunctionWithSubNetworks(dataSteps))
            .onStart("S1 Complete or S1 Not Needed Notification", newS1CompleteFunctionWithSubNetworks(dps))
            .onStart(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), newGpsPositionSuccessfullyMatchedFunctionWithSubNetworks(dps));

        statusEntriesListener.listenUntilStateChanges(node.getFdn(), getIntegrationCompleteStates());

        apServiceSteps.orderNode(node.getFdn());

        statusEntriesListener.waitForResults(120L, TimeUnit.SECONDS);

        final String supervisionFdn = node.getFdn() + ",SupervisionOptions=1";

        assertThat(dps).withManagedObject(supervisionFdn).withAttributeValue("managementState", "AUTOMATIC");
        final String networkElementMo = String.format(NETWORK_ELEMENT_MO, node.getName());
        assertThat(dps).withManagedObject(networkElementMo).withAttributeValue("managementState", "NORMAL");

        assertThat(dps).withManagedObject(nodeStatusFdn).withAttributeValue("state", "INTEGRATION_COMPLETED");
    }

    @Test
    @Features("AP Auto Integration")
    @Stories({ "WHEN user deletes the last node in a project THEN the project is deleted" })
    public void when_user_deletes_the_last_node_in_a_project_then_the_project_is_deleted() throws InterruptedException {
        stubsSteps.create_default_stubs();
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
            .with_default_erbs_artifacts()
            .with_default_security_options()
            .with_default_supervision_options()
            .with_default_erbs_auto_integration_options().build();

        final ManagedObject node = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        final String projectFdn = node.getParent().getFdn();

        orderProject(projectFdn, node.getFdn());
        apServiceSteps.delete_node(node.getFdn());

        assertThat(dps).hasNotManagedObject(projectFdn);
        assertThat(dps).hasNotManagedObject(NETWORK_ELEMENT_MO, node.getName());
    }

    private NodeStatusEntriesResult orderProject(final String projectFdn, final String nodeFdn) throws InterruptedException {
        final NodeStatusEntriesListener statusEntriesListener = new NodeStatusEntriesListener(dps)
            .onEnd(StatusEntryNames.ADD_NODE_TASK.toString(), newCreateCiRefAssociationFunction(ossMosGenerator));
        statusEntriesListener.listenUntilStateChanges(nodeFdn, getOrderCompleteStates());
        apServiceSteps.orderProject(projectFdn);
        final NodeStatusEntriesResult statusEntries = statusEntriesListener.waitForResults(120L, TimeUnit.SECONDS);
        return statusEntries;
    }

}
