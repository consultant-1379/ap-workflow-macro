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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ericsson.nms.security.nscs.api.CredentialService;
import com.ericsson.nms.security.nscs.api.IscfService;
import com.ericsson.nms.security.nscs.api.credentials.CredentialAttributes;
import com.ericsson.nms.security.nscs.api.enums.EnrollmentMode;
import com.ericsson.nms.security.nscs.api.enums.SecurityLevel;
import com.ericsson.nms.security.nscs.api.exception.CredentialServiceException;
import com.ericsson.nms.security.nscs.api.exception.IscfServiceException;
import com.ericsson.nms.security.nscs.api.iscf.IpsecArea;
import com.ericsson.nms.security.nscs.api.iscf.IscfResponse;
import com.ericsson.nms.security.nscs.api.iscf.SubjectAltNameParam;
import com.ericsson.nms.security.nscs.api.model.NodeModelInformation;
import com.ericsson.oss.itpf.sdk.resources.ResourcesException;
import com.ericsson.oss.services.ap.api.resource.ResourceService;
import com.ericsson.oss.services.ap.api.workflow.DhcpRestClientService;
import com.ericsson.oss.services.ap.arquillian.util.Stubs;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs.DummyISCFService;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.backupservice.remote.api.ConfigurationVersionManagementServiceRemote;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.BasicPackageDetails;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.LicenseFileManagerService;

import ru.yandex.qatools.allure.annotations.Step;

/**
 * Stubs for external services used in workflow arquillian tests.
 */
public class WorkflowStubbedServicesSteps {

    @Inject
    private Stubs stubs;

    @Step("Create the stubs used for successful scenarios without IPSec tests")
    public void create_default_stubs() {
        create_iscf_service_stub_with_OAM();
        create_license_file_manager_service_stub();
        createDhcpConfigurationStub();
    }

    @Step("Creates a DHCP Configuration service stub used to mock DHCP support")
    public void createDhcpConfigurationStub() {
        DhcpRestClientService dhcpRestClientService = stubs.injectIntoSystem(DhcpRestClientService.class);
        doNothing().when(dhcpRestClientService).create(anyString(), anyString(), anyString(), anyString(), any(), any());
        doNothing().when(dhcpRestClientService).delete(anyString());
    }

    @Step("Throws an exception when WorkflowTaskFacade#removeNode invoked")
    public void create_flawed_workflowTaskFacade_spy_for_remove_node() {
        final WorkflowTaskFacade workflowTaskFacadeMock = stubs.injectIntoSystem(WorkflowTaskFacade.class);
        doThrow(new IllegalStateException("Create File Artifact Error")).when(workflowTaskFacadeMock)
            .createGeneratedArtifact(anyString(),
                anyString(), any(CppNodeType.class));
        doThrow(new IllegalStateException("Remove Node Error")).when(workflowTaskFacadeMock).removeNode(anyString());
    }

    @Step("Creates a LicenseFileManagerService stub")
    public void create_license_file_manager_service_stub() {
        final LicenseFileManagerService licenseFileManagerService = stubs
            .injectIntoSystem(LicenseFileManagerService.class);

        doReturn("/dummy/path/to/licensekey/file").when(licenseFileManagerService)
            .getLicenseKeyFilePathByNode(anyString());
    }

    @Step("Creates a RemoteSoftwarePackageService stub")
    public void create_remote_software_package_service_stub() {
        final RemoteSoftwarePackageService remoteSoftwarePackageService = stubs
            .injectIntoSystem(RemoteSoftwarePackageService.class);

        doReturn(newArrayList("/home/smrs/lran/upgrade_package")).when(remoteSoftwarePackageService)
            .getUpgradePackageDetails(anyString());

        doReturn(newArrayList()).when(remoteSoftwarePackageService).getUpgradePackageDetails("INVALID_UPGRADE_PACKAGE");

        doReturn(new BasicPackageDetails("/home/smrs/lran/basic_package", null)).when(remoteSoftwarePackageService)
            .getBasicPackageDetails(anyString());
    }

    @Step("Create a flawed IscfService mock instance to throw exception")
    public void create_flawed_iscf_service_stub() {
        final IscfService iscfServiceStub = stubs.injectIntoSystem(IscfService.class);
        doThrow(new IscfServiceException("Test Exception")).when(iscfServiceStub)
            .generate(anyString(), anyString(), any(SecurityLevel.class),
                any(SecurityLevel.class), anyString(), any(SubjectAltNameParam.class), anySetOf(IpsecArea.class), any(EnrollmentMode.class),
                any(NodeModelInformation.class));

        doThrow(new IscfServiceException("Test Exception")).when(iscfServiceStub).cancel(anyString());
    }

    @Step("Create a flawed IscfService that only fails for cancel")
    public void create_flawed_iscf_for_cancel_only_service_stub() {
        final IscfService iscfServiceStub = stubs.injectIntoSystem(IscfService.class);
        doReturn(mock(IscfResponse.class)).when(iscfServiceStub)
            .generate(anyString(), anyString(), any(SecurityLevel.class),
                any(SecurityLevel.class), anyString(), any(SubjectAltNameParam.class), anySetOf(IpsecArea.class), any(EnrollmentMode.class),
                any(NodeModelInformation.class));

        doThrow(new IscfServiceException("Test Exception")).when(iscfServiceStub).cancel(anyString());
    }

    @Step("Creates a ISCF Service dummy implementation instance")
    public void create_dummy_iscf_service_stub() {
        stubs.injectIntoSystem(DummyISCFService.class);
    }

    @Step("Creates a Credential Service dummy implementation instance")
    public void create_credential_service_stub() {
        stubs.injectIntoSystem(CredentialService.class);
    }

    @Step("Creates a flawed Credential Service stub that throws a CredentialServiceException")
    public void create_flawed_credential_service_stub() {
        final CredentialService credentialService = stubs.injectIntoSystem(CredentialService.class);
        doThrow(CredentialServiceException.class).when(credentialService)
            .createNodeCredentials(any(CredentialAttributes.class), anyString());
    }

    @Step("Creates an IscfService stub for OAM profile")
    public void create_iscf_service_stub_with_OAM() {
        final IscfService iscfServiceStub = stubs.injectIntoSystem(IscfService.class);
        doReturn(mock(IscfResponse.class)).when(iscfServiceStub)
            .generate(anyString(), anyString(), any(SecurityLevel.class),
                any(SecurityLevel.class), any(EnrollmentMode.class), any(NodeModelInformation.class));

        doReturn(mock(IscfResponse.class)).when(iscfServiceStub)
            .generate(anyString(), anyString(), any(SecurityLevel.class),
                any(SecurityLevel.class), anyString(), any(SubjectAltNameParam.class), anySetOf(IpsecArea.class), any(EnrollmentMode.class),
                any(NodeModelInformation.class));

    }

    @Step("Creates a flawed stub for ConfigurationVersionManagementServiceRemote")
    public void create_flawed_cv_management_service_stub() {
        final ConfigurationVersionManagementServiceRemote shmStub = stubs
            .injectIntoSystem(ConfigurationVersionManagementServiceRemote.class);

        try {
            doReturn(false).when(shmStub).createCV(anyString(), anyString(), anyString(), anyString());

            doReturn(false).when(shmStub).uploadCV(anyString(), anyString());
        } catch (final CVOperationRemoteException e) {
            throw new IllegalStateException("Unable to mock ConfigurationVersionManagementServiceRemote.", e);
        }

    }

    @Step("Creates a flawed stub for ConfigurationVersionManagementServiceRemote")
    public void create_flawed_after_integration_cv_management_service_stub() {

        final ConfigurationVersionManagementServiceRemote shmStub = stubs
            .injectIntoSystem(ConfigurationVersionManagementServiceRemote.class);

        try {
            when(shmStub.createCV(anyString(), anyString(), anyString(), anyString())).then(new Answer<Boolean>() {

                @Override
                public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                    final String cvName = invocation.getArgumentAt(3, String.class);
                    return cvName.contains("import");
                }
            });

            doReturn(true).when(shmStub).setStartableCV(anyString(), anyString());

            doReturn(true).when(shmStub).setCVFirstInRollBackList(anyString(), anyString());

        } catch (final CVOperationRemoteException e) {
            throw new IllegalStateException("Unable to mock ConfigurationVersionManagementServiceRemote.", e);
        }
    }

    @Step("Throws an exception when WorkflowTaskFacade#deleteGeneratedArtifact invoked second time for SiteInstallForBind artifact")
    public void create_flawed_workflowTaskFacade_spy_for_unbind(final String hardwareSerialNumber) {
        final ResourceService resourceServiceStub = stubs.injectIntoSystem(ResourceService.class);
        doThrow(ResourcesException.class).when(resourceServiceStub).delete(endsWith(hardwareSerialNumber + ".xml"));
    }

    @Step("Throws an exception when WorkflowTaskFacade#bindNodeDuringOrder invoked")
    public void create_flawed_workflow_task_facade_for_bind_node(final String apNodeFdn, final String hardwareSerialNumber) {
        final WorkflowTaskFacade workflowFacadeStub = stubs.injectIntoSystem(WorkflowTaskFacade.class);
        doThrow(RuntimeException.class).when(workflowFacadeStub)
            .bindNodeDuringOrder(apNodeFdn, hardwareSerialNumber, CppNodeType.ERBS);

    }
}