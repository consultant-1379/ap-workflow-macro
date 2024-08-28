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
package com.ericsson.oss.services.ap.workflow.erbs.artifacts;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_IDENTIFIER_VALUE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NodeDescriptorBuilder.createDefaultNode;
import static com.ericsson.oss.services.ap.model.NodeType.ERBS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.smrs.SmrsAccount;
import com.ericsson.oss.itpf.smrs.SmrsService;
import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.util.ShmDetailsRetriever;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor;
import com.ericsson.oss.services.ap.common.test.stubs.dps.StubbedDpsGenerator;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.RbsSummaryGenerator;
import com.ericsson.oss.services.ap.workflow.cpp.model.AutoIntegrationAttribute;
import com.ericsson.oss.services.ap.workflow.cpp.model.LicenseAttribute;
import com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute;

/**
 * Unit tests for {@link LteRbsSummaryHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LteRbsSummaryHandlerTest {

    private static final String NEW_FILE_CONTENT = "hello";

    private static final String SMRS_ROOT_DIR = "/home/smrs";
    private static final String SMRS_NETWORK_DIR = SMRS_ROOT_DIR + "/lran/";

    private static final String HOME_DIRECTORY = "/home/smrs/lran/ai/erbs/" + NODE_NAME + "/";
    private static final String ICSF_ABSOLUTE_PATH = SMRS_NETWORK_DIR + "ISCF.xml";
    private static final String ICSF_RELATIVE_PATH = "lran/ISCF.xml";

    private static final String LICENSE_KEY_RELATIVE_PATH = "lran/" + NODE_NAME + "_090827_085012.xml";

    private static final String SITE_BASIC_ABSOLUTE_PATH = SMRS_NETWORK_DIR + "SiteBasic.xml";
    private static final String SITE_BASIC_RELATIVE_PATH = "lran/SiteBasic.xml";

    private static final String SITE_EQUIPMENT_ABSOLUTE_PATH = SMRS_NETWORK_DIR + "RbsEquipment.xml";
    private static final String SITE_EQUIPMENT_RELATIVE_PATH = "lran/RbsEquipment.xml";

    private static final String UPGRADE_PACKAGE_NAME = "CXP1020511_R4D25";
    private static final String UPGRADE_PACKAGE_RELATIVE_PATH = "lran/" + UPGRADE_PACKAGE_NAME + ".xml";

    private static final String BASIC_PACKAGE_NAME = "CXP1020512_R4D25";
    private static final String BASIC_PACKAGE_RELATIVE_PATH = "lran/" + BASIC_PACKAGE_NAME + ".xml";

    private static final ArtifactDetails SITE_BASIC_ARTIFACT = new ArtifactDetails.ArtifactBuilder().location(SITE_BASIC_ABSOLUTE_PATH).build();
    private static final ArtifactDetails SITE_EQUIPMENT_ARTIFACT = new ArtifactDetails.ArtifactBuilder().location(SITE_EQUIPMENT_ABSOLUTE_PATH)
            .build();

    private final Map<String, Object> rbsSummaryAttributes = new HashMap<>();
    private final StubbedDpsGenerator dpsGenerator = new StubbedDpsGenerator();

    @Mock
    private SmrsService smrsService;

    @Mock
    private RbsSummaryGenerator rbsSummaryFileGenerator;

    @Mock
    private ShmDetailsRetriever shmDetailsRetriever;

    @Mock
    private GeneratedArtifactHandler generatedArtifactHandler;

    @InjectMocks
    private final LteRbsSummaryHandler rbsSummaryHandler = new LteRbsSummaryHandler();

    @InjectMocks
    private DpsOperations dpsOperations;

    @Before
    public void setUp() {
        final DataPersistenceService dps = dpsGenerator.getStubbedDps();
        Whitebox.setInternalState(dpsOperations, "dps", dps);
        Whitebox.setInternalState(rbsSummaryHandler, "dpsOperations", dpsOperations);

        final SmrsAccount smrsAccount = new SmrsAccount();
        smrsAccount.setHomeDirectory(HOME_DIRECTORY);
        smrsAccount.setSmrsRootDirectory(SMRS_ROOT_DIR);
        when(smrsService.getNodeSpecificAccount("AI", "ERBS", NODE_NAME)).thenReturn(smrsAccount);

        when(generatedArtifactHandler.readFirstOfType(NODE_FDN, "SiteBasic")).thenReturn(SITE_BASIC_ARTIFACT);
        when(generatedArtifactHandler.readFirstOfType(NODE_FDN, "SiteEquipment")).thenReturn(SITE_EQUIPMENT_ARTIFACT);

        rbsSummaryAttributes.put("siteBasicFilePath", SITE_BASIC_RELATIVE_PATH);
        rbsSummaryAttributes.put("siteEquipmentFilePath", SITE_EQUIPMENT_RELATIVE_PATH);
    }

    @Test
    public void when_generate_rbssummary_file_then_siteBasic_and_siteEquipment_paths_included_in_rbs_attributes() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
            .withLicenseOption(LicenseAttribute.INSTALL_LICENSE.toString(), Boolean.FALSE)
            .withAutoIntegrationOption(AutoIntegrationAttribute.BASIC_PACKAGE.toString(), null)
            .withAutoIntegrationOption(AutoIntegrationAttribute.UPGRADE_PACKAGE.toString(), null)
            .build();
        dpsGenerator.generate(nodeDescriptor);

        when(rbsSummaryFileGenerator.generate(ERBS.toString(), NODE_IDENTIFIER_VALUE, rbsSummaryAttributes)).thenReturn(NEW_FILE_CONTENT.getBytes());

        final String fileContents = rbsSummaryHandler.generate(NODE_FDN);

        assertEquals(NEW_FILE_CONTENT, fileContents);
        verify(shmDetailsRetriever, never()).getBasicPackageFilePath(anyString(), anyString());
        verify(shmDetailsRetriever, never()).getUpgradePackageFilePath(anyString(), anyString());
        verify(shmDetailsRetriever, never()).getLicenseKeyFilePath(anyString(), anyString());
    }

    @Test
    public void when_generate_rbssummary_file_and_upgrade_package_set_then_upgradePackageName_included_in_rbs_attributes() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
            .withLicenseOption(LicenseAttribute.INSTALL_LICENSE.toString(),Boolean.FALSE)
            .withAutoIntegrationOption(AutoIntegrationAttribute.BASIC_PACKAGE.toString(), null)
            .withAutoIntegrationOption(AutoIntegrationAttribute.UPGRADE_PACKAGE.toString(), UPGRADE_PACKAGE_NAME)
            .build();
        dpsGenerator.generate(nodeDescriptor);

        when(shmDetailsRetriever.getUpgradePackageFilePath(UPGRADE_PACKAGE_NAME, SMRS_ROOT_DIR)).thenReturn(UPGRADE_PACKAGE_RELATIVE_PATH);
        rbsSummaryAttributes.put("upgradePackageFilePath", UPGRADE_PACKAGE_RELATIVE_PATH);
        when(rbsSummaryFileGenerator.generate(ERBS.toString(), NODE_IDENTIFIER_VALUE, rbsSummaryAttributes)).thenReturn(NEW_FILE_CONTENT.getBytes());

        final String fileContents = rbsSummaryHandler.generate(NODE_FDN);
        assertEquals(NEW_FILE_CONTENT, fileContents);
    }

    @Test
    public void when_generate_rbssummary_file_and_bacic_package_set_then_basicPackageName_included_in_rbs_attributes() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
            .withLicenseOption(LicenseAttribute.INSTALL_LICENSE.toString(), Boolean.FALSE)
            .withAutoIntegrationOption(AutoIntegrationAttribute.BASIC_PACKAGE.toString(), BASIC_PACKAGE_NAME)
            .withAutoIntegrationOption(AutoIntegrationAttribute.UPGRADE_PACKAGE.toString(), null)
            .build();
        dpsGenerator.generate(nodeDescriptor);
        when(shmDetailsRetriever.getBasicPackageFilePath(BASIC_PACKAGE_NAME, SMRS_ROOT_DIR)).thenReturn(BASIC_PACKAGE_RELATIVE_PATH);

        rbsSummaryAttributes.put("integrationBasicPackageFilePath", BASIC_PACKAGE_RELATIVE_PATH);
        when(rbsSummaryFileGenerator.generate(ERBS.toString(), NODE_IDENTIFIER_VALUE, rbsSummaryAttributes)).thenReturn(NEW_FILE_CONTENT.getBytes());

        final String fileContents = rbsSummaryHandler.generate(NODE_FDN);
        assertEquals(NEW_FILE_CONTENT, fileContents);
    }

    @Test
    public void when_generate_rbssummary_file_and_installLicense_set_then_licensingKeyFilePath_included_in_rbs_attributes() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
            .withLicenseOption(LicenseAttribute.INSTALL_LICENSE.toString(), Boolean.TRUE)
            .withAutoIntegrationOption(AutoIntegrationAttribute.BASIC_PACKAGE.toString(), null)
            .withAutoIntegrationOption(AutoIntegrationAttribute.UPGRADE_PACKAGE.toString(), null)
            .build();
        dpsGenerator.generate(nodeDescriptor);

        when(shmDetailsRetriever.getLicenseKeyFilePath(NODE_NAME, SMRS_ROOT_DIR)).thenReturn(LICENSE_KEY_RELATIVE_PATH);
        rbsSummaryAttributes.put("licensingKeyFilePath", LICENSE_KEY_RELATIVE_PATH);
        when(rbsSummaryFileGenerator.generate(ERBS.toString(), NODE_IDENTIFIER_VALUE, rbsSummaryAttributes)).thenReturn(NEW_FILE_CONTENT.getBytes());

        final String fileContents = rbsSummaryHandler.generate(NODE_FDN);
        assertEquals(NEW_FILE_CONTENT, fileContents);
    }

    @Test
    public void when_generate_rbssummary_file_and_securityMo_exists_then_initialSecurityConfigurationFilePath_included_in_rbs_attributes() {
        final NodeDescriptor nodeDescriptor = createDefaultNode(ERBS)
            .withLicenseOption(LicenseAttribute.INSTALL_LICENSE.toString(), Boolean.FALSE)
            .withAutoIntegrationOption(AutoIntegrationAttribute.BASIC_PACKAGE.toString(), null)
            .withAutoIntegrationOption(AutoIntegrationAttribute.UPGRADE_PACKAGE.toString(), null)
            .withSecurityOption(SecurityAttribute.ISCF_FILE_LOCATION.toString(), ICSF_ABSOLUTE_PATH)
            .build();
        dpsGenerator.generate(nodeDescriptor);

        rbsSummaryAttributes.put("initialSecurityConfigurationFilePath", ICSF_RELATIVE_PATH);
        when(rbsSummaryFileGenerator.generate(ERBS.toString(), NODE_IDENTIFIER_VALUE, rbsSummaryAttributes)).thenReturn(NEW_FILE_CONTENT.getBytes());

        final String fileContents = rbsSummaryHandler.generate(NODE_FDN);
        assertEquals(NEW_FILE_CONTENT, fileContents);
    }

    @Test
    public void when_get_generated_relative_file_path_requested_the_relative_path_returned() {
        final String absoluteRbsSummaryFilePath = HOME_DIRECTORY + "AutoIntegrationRbsSummaryFile.xml";
        final ArtifactDetails rbsSummaryArtifact = new ArtifactDetails.ArtifactBuilder().location(absoluteRbsSummaryFilePath).build();
        when(generatedArtifactHandler.readFirstOfType(NODE_FDN, "RbsSummary")).thenReturn(rbsSummaryArtifact);

        final String filePath = rbsSummaryHandler.getRelativeRbsSummaryPath(NODE_FDN);
        assertEquals("lran/ai/erbs/" + NODE_NAME + "/AutoIntegrationRbsSummaryFile.xml", filePath);
    }
}
