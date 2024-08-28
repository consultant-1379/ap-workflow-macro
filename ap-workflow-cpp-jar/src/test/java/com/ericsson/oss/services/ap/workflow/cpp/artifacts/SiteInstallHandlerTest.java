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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.RBS_INTEGRITY_CODE_VALUE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NodeDescriptorBuilder.createDefaultNode;
import static com.ericsson.oss.services.ap.model.NodeType.ERBS;
import static com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute.RBS_INTEGRITY_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
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
import com.ericsson.oss.itpf.smrs.SmrsAddressRequest;
import com.ericsson.oss.itpf.smrs.SmrsService;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.raw.RawArtifactHandler;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor;
import com.ericsson.oss.services.ap.common.test.stubs.dps.StubbedDpsGenerator;
import com.ericsson.oss.services.ap.common.util.xml.DocumentBuilder;
import com.ericsson.oss.services.ap.common.util.xml.DocumentReader;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute;

/**
 * Unit tests for {@link SiteInstallHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteInstallHandlerTest {

    private static final String RBS_SUMMARY_PATH = "lran/ai/erbs/" + NODE_NAME + "/RbsSummary.xml";
    private static final String SMRS_ADDRESS = "1.2.3.4";
    private static final String SFTP_PORT = "1025";

    @Mock
    private RawArtifactHandler rawArtifactHandler;

    @Mock
    private GeneratedArtifactHandler generatedArtifactHandler;

    @Mock
    private RbsSummaryHandler rbsSummaryHandler;

    @Mock
    private RbsSummaryHandlerResolver rbsSummaryHandlerResolver;

    @Mock
    private SmrsService smrsService;

    @InjectMocks
    private DpsOperations dpsOperations;

    @InjectMocks
    private SiteInstallHandler siteInstallHandler;

    private static final String SITE_INSTALL_CONTENTS = "<RbsSiteInstallationFile xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='SiteInstallation.xsd'>"
            + "<Format revision='J'/>"
            + "<InstallationData logicalName='LRBS6010' vlanId='3100' rbsIntegrationCode='UNDEFINED'>"
            + "<OamIpConfigurationData ipAddress='1.2.3.4' subnetMask='255.255.240.0' defaultRouter0='10.200.0.1'>"
            + "<DnsServer ipAddress='10.212.100.10'/>"
            + "</OamIpConfigurationData>"
            + "<SmrsData address='0.0.0.0' summaryFilePath='UNDEFINED'/>"
            + "</InstallationData>"
            + "</RbsSiteInstallationFile>";

    private static final String SITE_INSTALL_CONTENTS_MISSING_SMRS_DATA = "<RbsSiteInstallationFile xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='SiteInstallation.xsd'>"
            + "<Format revision='J'/>"
            + "<InstallationData logicalName='LRBS6010' vlanId='3100' rbsIntegrationCode='UNDEFINED'>"
            + "<OamIpConfigurationData ipAddress='1.2.3.4' subnetMask='255.255.240.0' defaultRouter0='10.200.0.1'>"
            + "<DnsServer ipAddress='10.212.100.10'/>"
            + "</OamIpConfigurationData>"
            + "</InstallationData>"
            + "</RbsSiteInstallationFile>";

    private static final String SITE_INSTALL_CONTENTS_MISSING_INSTALLATION_DATA = "<RbsSiteInstallationFile xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='SiteInstallation.xsd'>"
            + "<Format revision='J'/>"
            + "<OamIpConfigurationData ipAddress='1.2.3.4' subnetMask='255.255.240.0' defaultRouter0='10.200.0.1'>"
            + "<DnsServer ipAddress='10.212.100.10'/>"
            + "</OamIpConfigurationData>"
            + "</RbsSiteInstallationFile>";

    private final StubbedDpsGenerator dpsGenerator = new StubbedDpsGenerator();

    @Before
    public void setUp() {
        final DataPersistenceService dps = dpsGenerator.getStubbedDps();
        Whitebox.setInternalState(dpsOperations, "dps", dps);
        Whitebox.setInternalState(siteInstallHandler, "dpsOperations", dpsOperations);
        System.setProperty("smrs_sftp_securePort", "22");

        final ArtifactDetails siteInstallArtifact = new ArtifactDetails.ArtifactBuilder().artifactContent(SITE_INSTALL_CONTENTS).build();
        when(rawArtifactHandler.readFirstOfType(NODE_FDN, "SiteInstallation")).thenReturn(siteInstallArtifact);
        when(rbsSummaryHandler.getRelativeRbsSummaryPath(NODE_FDN)).thenReturn(RBS_SUMMARY_PATH);
        when(rbsSummaryHandlerResolver.getRbsSummaryHandler(CppNodeType.ERBS)).thenReturn(rbsSummaryHandler);
        when(smrsService.getFileServerAddress(any(SmrsAddressRequest.class))).thenReturn(SMRS_ADDRESS);
    }

    @Test
    public void when_generate_site_install_then_the_generated_file_contains_the_rbs_summary_path() {
        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
        final Element smrsDataElement = readElement(generatedFile, "SmrsData");
        assertEquals(RBS_SUMMARY_PATH, smrsDataElement.getAttribute("summaryFilePath"));
    }

    @Test
    public void when_generate_site_install_with_missing_smrs_data_then_generated_site_install_contains_smrs_data() {
        final ArtifactDetails siteInstallArtifact = new ArtifactDetails.ArtifactBuilder()
                .artifactContent(SITE_INSTALL_CONTENTS_MISSING_SMRS_DATA).build();
        when(rawArtifactHandler.readFirstOfType(NODE_FDN, "SiteInstallation")).thenReturn(siteInstallArtifact);
        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);

        final Element smrsDataElement = readElement(generatedFile, "SmrsData");
        assertEquals(SMRS_ADDRESS, smrsDataElement.getAttribute("address"));
        assertEquals(RBS_SUMMARY_PATH, smrsDataElement.getAttribute("summaryFilePath"));
    }

    @Test
    public void when_generate_site_install_then_the_generated_file_contains_the_smrs_address() {
        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
        final Element smrsDataElement = readElement(generatedFile, "SmrsData");
        assertEquals(SMRS_ADDRESS, smrsDataElement.getAttribute("address"));
    }

    @Test(expected = ApApplicationException.class)
    public void when_generate_site_install_with_missing_installation_data_then_exception_thrown() {
        final ArtifactDetails siteInstallArtifact = new ArtifactDetails.ArtifactBuilder()
                .artifactContent(SITE_INSTALL_CONTENTS_MISSING_INSTALLATION_DATA).build();
        when(rawArtifactHandler.readFirstOfType(NODE_FDN, "SiteInstallation")).thenReturn(siteInstallArtifact);

       siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void when_generate_site_install_with_custom_sftp_port_defined_then_generated_file_contains_correct_attribute_value() {
        System.setProperty("smrs_sftp_securePort", "1025");
        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
        final Element smrsDataElement = readElement(generatedFile, "SmrsData");
        assertEquals(SFTP_PORT, smrsDataElement.getAttribute("port"));
    }

    @Test
    public void when_generate_site_install_with__default_sftp_port_defined_then_generated_file_does_not_contain_port_attribute() {
        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
        assertFalse(generatedFile.contains("port"));
    }

    @Test
    public void when_generate_site_install_without_sftp_port_defined_then_generated_file_does_not_contain_port_attribute() {
        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
        assertFalse(generatedFile.contains("port"));
    }

    @Test
    public void when_generate_site_install_then_the_generated_file_contains_the_rbs_integration_code() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(RBS_INTEGRITY_CODE.toString(), RBS_INTEGRITY_CODE_VALUE)
                .build();
        dpsGenerator.generate(nodeDescriptor);

        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
        final Element smrsDataElement = readElement(generatedFile, "InstallationData");
        assertEquals(RBS_INTEGRITY_CODE_VALUE, smrsDataElement.getAttribute("rbsIntegrationCode"));
    }

    @Test
    public void when_generate_site_install_and_no_rbs_integrity_code_set_then_attribute_is_unchanged_in_generated_file() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
                .withSecurityOption(SecurityAttribute.RBS_INTEGRITY_CODE.toString(), null)
                .build();
        dpsGenerator.generate(nodeDescriptor);

        final String generatedFile = siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);
        final Element smrsDataElement = readElement(generatedFile, "InstallationData");
        assertEquals("UNDEFINED", smrsDataElement.getAttribute("rbsIntegrationCode"));
    }

    @Test
    public void when_generate_siteinstall_file_then_siteinstall_created_with_correct_arguments() {
        siteInstallHandler.generate(NODE_FDN, CppNodeType.ERBS);

        verify(generatedArtifactHandler).create(argThat(new ArgumentMatcher<ArtifactDetails>() {

            @Override
            public boolean matches(final Object argument) {
                final ArtifactDetails artifact = (ArtifactDetails) argument;
                return artifact.getApNodeFdn().equals(NODE_FDN) && artifact.isExportable() && artifact.getName().equals("SiteInstall")
                        && artifact.getType().equals("SiteInstallation");
            }
        }));
    }

    private Element readElement(final String generatedSiteInstallContents, final String elementName) {
        final Document siteInstallDoc = DocumentBuilder.getDocument(generatedSiteInstallContents);
        final DocumentReader docReader = new DocumentReader(siteInstallDoc);
        return docReader.getElement(elementName);
    }
}
