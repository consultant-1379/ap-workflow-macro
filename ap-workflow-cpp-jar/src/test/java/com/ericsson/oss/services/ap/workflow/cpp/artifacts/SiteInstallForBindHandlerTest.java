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
package com.ericsson.oss.services.ap.workflow.cpp.artifacts;

import static com.ericsson.oss.services.ap.common.model.NodeAttribute.HARDWARE_SERIAL_NUMBER;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.HARDWARE_SERIAL_NUMBER_VALUE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NodeDescriptorBuilder.createDefaultNode;
import static com.ericsson.oss.services.ap.model.NodeType.ERBS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService;
import com.ericsson.oss.itpf.smrs.SmrsAccount;
import com.ericsson.oss.itpf.smrs.SmrsService;
import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor;
import com.ericsson.oss.services.ap.common.test.stubs.dps.StubbedDpsGenerator;
import com.ericsson.oss.services.ap.common.util.xml.DocumentBuilder;
import com.ericsson.oss.services.ap.common.util.xml.DocumentReader;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Unit tests for {@link SiteInstallHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteInstallForBindHandlerTest {

    private static final String SMRS_ADDRESS_VALUE = "192.168.0.8";
    private static final String SMRS_SUMMARY_FILE_PATH_VALUE = "/nedssv4/AIF/" + NODE_NAME + "/AutoIntegrationRbsSummaryFile.xml";
    private static final String SMRS_USERNAME = "Jimmy";
    private static final char[] SMRS_PASSWORD = "bobby".toCharArray();

    private static final String SMRS_DATA_ELEMENT = "SmrsData";
    private static final String ATTR_USERNAME = "userName";
    private static final String ATTR_PASSWORD = "password";
    private static final String ATTR_ADDRESS = "ipAddress";
    private static final String ATTR_SUMMARY_FILE_PATH = "summaryFilePath";

    private static final String SITE_INSTALL_CONTENTS = "<RbsSiteInstallationFile xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='SiteInstallation.xsd'>"
            + "<Format revision='J'/>"
            + "<InstallationData logicalName='LRBS6010' vlanId='3100' rbsIntegrationCode=''>"
            + "<OamIpConfigurationData ipAddress='1.2.3.4' subnetMask='255.255.240.0' defaultRouter0='10.200.0.1'>"
            + "<DnsServer ipAddress='10.212.100.10'/>"
            + "</OamIpConfigurationData>"
            + "<SmrsData "
            + ATTR_ADDRESS
            + "='"
            + SMRS_ADDRESS_VALUE
            + "' "
            + ATTR_SUMMARY_FILE_PATH
            + "='"
            + SMRS_SUMMARY_FILE_PATH_VALUE
            + "'/>"
            + "</InstallationData>"
            + "</RbsSiteInstallationFile>";

    private final StubbedDpsGenerator dpsGenerator = new StubbedDpsGenerator();

    @Mock
    protected SmrsService smrsService;

    @Mock
    private GeneratedArtifactHandler generatedArtifactHandler;

    @Mock
    private IdentityManagementService identityMgtService;

    @InjectMocks
    private SiteInstallForBindHandler siteInstallForBindHandler;

    @InjectMocks
    private DpsOperations dpsOperations;

    @Before
    public void setUp() {
        final DataPersistenceService dps = dpsGenerator.getStubbedDps();
        Whitebox.setInternalState(dpsOperations, "dps", dps);
        Whitebox.setInternalState(siteInstallForBindHandler, "dpsOperations", dpsOperations);

        final SmrsAccount smrsAccount = new SmrsAccount(SMRS_USERNAME, "/dir");
        when(smrsService.getNodeSpecificAccount("AI", "ERBS", NODE_NAME)).thenReturn(smrsAccount);

        when(identityMgtService.getM2MPassword(SMRS_USERNAME)).thenReturn(SMRS_PASSWORD);
    }

    @Test
    public void when_create_bind_file_then_file_is_created_with_correct_arguments() {
        addNodeMo(HARDWARE_SERIAL_NUMBER_VALUE);
        createSiteInstallForBindGeneratedFileContents();

        verify(generatedArtifactHandler).create(argThat(new ArgumentMatcher<ArtifactDetails>() {

            @Override
            public boolean matches(final Object argument) {
                final ArtifactDetails artifact = (ArtifactDetails) argument;
                return artifact.getApNodeFdn().equals(NODE_FDN)
                        && artifact.getName().equals(HARDWARE_SERIAL_NUMBER_VALUE)
                        && artifact.getType().equals(ArtifactType.SITEINSTALLFORBIND.toString());
            }
        }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void when_create_bind_file_and_hardware_serial_number_not_set_then_throw_IllegalArgumentException() {
        addNodeMo(null);

        createSiteInstallForBindGeneratedFileContents();
    }

    @Test
    public void when_create_bind_file_then_site_install_file_read_and_updated_with_smrs_username() {
        addNodeMo(HARDWARE_SERIAL_NUMBER_VALUE);
        final Element smrsDataElement = createSiteInstallForBindGeneratedFileContents();

        final String smrsUserName = smrsDataElement.getAttribute(ATTR_USERNAME).toString();
        assertEquals(smrsUserName, SMRS_USERNAME);
    }

    @Test
    public void when_create_bind_file_then_site_install_file_read_and_updated_with_smrs_password() {
        addNodeMo(HARDWARE_SERIAL_NUMBER_VALUE);
        final Element smrsDataElement = createSiteInstallForBindGeneratedFileContents();

        final String smrsPassword = smrsDataElement.getAttribute(ATTR_PASSWORD).toString();
        assertEquals(smrsPassword, new String(SMRS_PASSWORD));
    }

    @Test
    public void when_create_bind_file_then_site_install_contains_old_smrsData_attributes_unchanged() {
        addNodeMo(HARDWARE_SERIAL_NUMBER_VALUE);
        final Element smrsDataElement = createSiteInstallForBindGeneratedFileContents();

        final String address = smrsDataElement.getAttribute(ATTR_ADDRESS).toString();
        assertEquals(address, SMRS_ADDRESS_VALUE);

        final String summaryFilePath = smrsDataElement.getAttribute(ATTR_SUMMARY_FILE_PATH).toString();
        assertEquals(summaryFilePath, SMRS_SUMMARY_FILE_PATH_VALUE);
    }

    private Element createSiteInstallForBindGeneratedFileContents() {
        final ArtifactDetails siteInstallArtifact = new ArtifactDetails.ArtifactBuilder().apNodeFdn(NODE_FDN)
                .name(NODE_NAME)
                .type("ERBS")
                .location("dir")
                .exportable(true)
                .artifactContent(SITE_INSTALL_CONTENTS)
                .build();
        when(generatedArtifactHandler.readFirstOfType(NODE_FDN, ArtifactType.SITEINSTALL.toString())).thenReturn(siteInstallArtifact);

        final String generatedBindContents = siteInstallForBindHandler.generate(NODE_FDN, CppNodeType.ERBS);

        final Document siteInstallDoc = DocumentBuilder.getDocument(generatedBindContents);
        final DocumentReader docReader = new DocumentReader(siteInstallDoc);
        final Element smrsDataElement = docReader.getElement(SMRS_DATA_ELEMENT);
        return smrsDataElement;
    }

    private void addNodeMo(final String hardwareSerialNumber) {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withNodeFdn(NODE_FDN)
                .withNodeAttribute(HARDWARE_SERIAL_NUMBER.toString(), hardwareSerialNumber)
                .build();
        dpsGenerator.generate(nodeDescriptor);
    }
}
