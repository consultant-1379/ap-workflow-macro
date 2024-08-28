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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.ImportLicenseRemoteResponse;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.DeleteLicenseException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.ImportLicenseException;

import ru.yandex.qatools.allure.annotations.Step;

@Stateless
public class WorkflowTaskEjbTestSteps {

    @EJB
    private WorkflowTaskFacade workflowTaskFacade;

    @Step("Add cpp node")
    public void add_cpp_node(final String apNodeFdn) {
        workflowTaskFacade.addNode(apNodeFdn);
    }

    @Step("Remove cpp node")
    public void remove_cpp_node(final String apNodeFdn) {
        workflowTaskFacade.removeNode(apNodeFdn);
    }

    @Step("Create generated artifact")
    public void create_generated_artifact(final String artifactType, final String apNodeFdn) {
        workflowTaskFacade.createGeneratedArtifact(artifactType, apNodeFdn, CppNodeType.ERBS);
    }

    @Step("Delete generated artifact")
    public void delete_generated_artifact(final String artifactType, final String apNodeFdn) {
        workflowTaskFacade.deleteGeneratedArtifact(artifactType, apNodeFdn);
    }

    @Step("Generate Node Security")
    public void generate_node_security_data(final String apNodeFdn) {
        workflowTaskFacade.enableSecurity(apNodeFdn, CppNodeType.ERBS);
    }

    @Step("Cancel Node Security")
    public void cancel_node_security_data(final String apNodeFdn) {
        workflowTaskFacade.cancelSecurity(apNodeFdn, CppNodeType.ERBS);
    }

    @Step("Create Node User Credentials")
    public void create_node_user_credentials(final String apNodeFdn) {
        workflowTaskFacade.createNodeUserCredentials(apNodeFdn);
    }

    @Step("Bind node manually")
    public void bind_node_manually(final String apNodeFdn, final String hardwareSerialNumber) {
        workflowTaskFacade.bindNodeManually(apNodeFdn, hardwareSerialNumber, CppNodeType.ERBS);
    }

    @Step("Bind node during order")
    public void bind_node_during_order(final String apNodeFdn, final String hardwareSerialNumber) {
        workflowTaskFacade.bindNodeDuringOrder(apNodeFdn, hardwareSerialNumber, CppNodeType.ERBS);
    }

    @Step("Unbind node")
    public void unbind_node(final String apNodeFdn) {
        workflowTaskFacade.unbindNode(apNodeFdn);
    }

    @Step("Import License Key File")
    public ImportLicenseRemoteResponse import_license_key_file(final String apNodeFdn) throws ImportLicenseException {
        return workflowTaskFacade.importLicenseKeyFile(apNodeFdn);
    }

    @Step("Delete License Key File")
    public void delete_license_key_file(final String fingerPrint, final String sequenceNumber, final String apNodeFdn) throws DeleteLicenseException {
        workflowTaskFacade.deleteLicenseKeyFile(fingerPrint, sequenceNumber, apNodeFdn);
    }

    @Step("Configure Management State")
    public void configureManagementState(final String apNodeFdn, final String maintenanceValue) {
        workflowTaskFacade.configureManagementState(apNodeFdn, maintenanceValue);
    }

    @Step("Assign TargetGroup to Node")
    public void addTargetToTargetGroup(final String apNodeFdn) {
        workflowTaskFacade.assignTargetGroups(apNodeFdn);
    }

}