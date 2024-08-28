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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.AutoProvisioningService;

import ru.yandex.qatools.allure.annotations.Step;

/**
 * Test steps for the {@link AutoProvisioningService}.
 */
public class AutoProvisioningServiceTestSteps {

    private AutoProvisioningService service;

    @Step("Download the sample project for the given node type.")
    public String downloadSampleProjectFile(final String nodeType) {
        return getAutoProvisioningService().downloadSchemaAndSamples(nodeType);
    }

    @Step("Executes an order for a single node")
    public void orderNode(final String nodeFdn) {
        getAutoProvisioningService().orderNode(nodeFdn);
    }

    @Step("Executes an order for a project")
    public void orderProject(final String projectFdn) {
        getAutoProvisioningService().orderProject(projectFdn);
    }

    @Step("Executes an order for a project archive")
    public String orderProject(final String fileName, final byte[] projectZip) {
        return getAutoProvisioningService().orderProject(fileName, projectZip, true);
    }

    @Step("Bind node")
    public void bindNode(final String nodeFdn, final String hardwareSerialNumber) {
        getAutoProvisioningService().bind(nodeFdn, hardwareSerialNumber);
    }

    @Step("Executes AP Delete project")
    public void delete_project(final String projectFdn) { getAutoProvisioningService().deleteProject(projectFdn, false); }

    @Step("Executes AP Delete node")
    public void delete_node(final String nodeFdn) {
        getAutoProvisioningService().deleteNode(nodeFdn, false);
    }

    private AutoProvisioningService getAutoProvisioningService() {
        if (service == null) {
            service = new ServiceFinderBean().find(AutoProvisioningService.class, "apcore");
        }
        return service;
    }
}
