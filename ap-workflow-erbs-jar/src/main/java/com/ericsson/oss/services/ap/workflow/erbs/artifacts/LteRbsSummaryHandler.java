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

import static com.ericsson.oss.services.ap.common.model.NodeAttribute.NODE_IDENTIFIER;
import static com.ericsson.oss.services.ap.common.model.NodeAttribute.NODE_TYPE;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.smrs.SmrsService;
import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.util.ShmDetailsRetriever;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.NodeType;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.RbsSummaryGenerator;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.RbsSummaryHandler;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;
import com.ericsson.oss.services.ap.workflow.cpp.model.AutoIntegrationAttribute;
import com.ericsson.oss.services.ap.workflow.cpp.model.LicenseAttribute;
import com.ericsson.oss.services.ap.workflow.cpp.model.SecurityAttribute;

/**
 * Handles generation of the {@link ArtifactType#RBSSUMMARY} file.
 */
@NodeType(type = CppNodeType.ERBS)
public class LteRbsSummaryHandler implements RbsSummaryHandler {

    private static final String ARTIFACT_NAME = "AutoIntegrationRbsSummaryFile.xml";
    private static final String ERBS_NODE_TYPE = "ERBS";

    private static final String SITE_BASIC_FILE_PATH = "siteBasicFilePath";
    private static final String SITE_EQUIPMENT_FILE_PATH = "siteEquipmentFilePath";
    private static final String ATTR_UPGRADE_PACKAGE_PATH = "upgradePackageFilePath";
    private static final String ATTR_BASIC_PACKAGE_PATH = "integrationBasicPackageFilePath";
    private static final String ATTR_LICENSE_KEY_FILE_PATH = "licensingKeyFilePath";
    private static final String ATTR_ISCF_PATH = "initialSecurityConfigurationFilePath";

    @Inject
    private DpsOperations dpsOperations;

    private SmrsService smrsService;

    @Inject
    private GeneratedArtifactHandler generatedArtifactHandler;

    @Inject
    private RbsSummaryGenerator rbsSummaryGenerator;

    @Inject
    private ShmDetailsRetriever shmDetailsRetriever;

    @PostConstruct
    public void init() {
        smrsService = new ServiceFinderBean().find(SmrsService.class);
    }

    /**
     * Generates the RbsSummary file for an AP node and stores it on the SMRS.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @return the generated RbsSummary file
     */
    @Override
    public String generate(final String apNodeFdn) {
        final String generatedRbsSummaryFile = createGeneratedFileContents(apNodeFdn);
        final ArtifactDetails generatedRbsSummaryArtifact = new ArtifactDetails.ArtifactBuilder()
                .apNodeFdn(apNodeFdn)
                .artifactContent(generatedRbsSummaryFile)
                .type(ArtifactType.RBSSUMMARY.toString())
                .exportable(false)
                .name(ARTIFACT_NAME).build();
        generatedArtifactHandler.createOnSmrs(generatedRbsSummaryArtifact, ERBS_NODE_TYPE);
        return generatedRbsSummaryFile;
    }

    /**
     * Gets the path of the generated RbsSummary file relative to the SMRS root directory.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @return generated RbsSummary file path
     */
    @Override
    public String getRelativeRbsSummaryPath(final String apNodeFdn) {
        final String smrsRootDir = getSmrsRootDirectory(apNodeFdn);
        final ArtifactDetails rbsSummaryArtifact = generatedArtifactHandler.readFirstOfType(apNodeFdn, ArtifactType.RBSSUMMARY.toString());
        final String absoluteRbsSummaryPath = rbsSummaryArtifact.getLocation();
        return getPathRelativeToSmrsRootDir(absoluteRbsSummaryPath, smrsRootDir);
    }

    private String createGeneratedFileContents(final String apNodeFdn) {
        final ManagedObject nodeMo = dpsOperations.getDataPersistenceService().getLiveBucket().findMoByFdn(apNodeFdn);
        final String nodeType = nodeMo.getAttribute(NODE_TYPE.toString()).toString();
        final String nodeIdentifier = nodeMo.getAttribute(NODE_IDENTIFIER.toString()).toString();
        final String smrsRootDir = getSmrsRootDirectory(apNodeFdn);

        final Map<String, Object> rbsSummaryAttributes = getRbsSummaryAttributes(nodeMo, apNodeFdn, smrsRootDir);
        final byte[] rbsSummaryFile = rbsSummaryGenerator.generate(nodeType, nodeIdentifier, rbsSummaryAttributes);

        return new String(rbsSummaryFile);
    }

    private Map<String, Object> getRbsSummaryAttributes(final ManagedObject nodeMo, final String apNodeFdn, final String smrsRootDir) {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(SITE_BASIC_FILE_PATH, getSiteBasicFilePath(apNodeFdn, smrsRootDir));
        attributes.put(SITE_EQUIPMENT_FILE_PATH, getSiteEquipmentFilePath(apNodeFdn, smrsRootDir));

        final ManagedObject autoIntegrationMo = nodeMo.getChild(MoType.AI_OPTIONS.toString() + "=1");

        final String upgradePackageName = getUpgradePackageName(autoIntegrationMo);
        if (StringUtils.isNotBlank(upgradePackageName)) {
            attributes.put(ATTR_UPGRADE_PACKAGE_PATH, shmDetailsRetriever.getUpgradePackageFilePath(upgradePackageName, smrsRootDir));
        }

        final String basicPackageName = getBasicPackageName(autoIntegrationMo);
        if (StringUtils.isNotBlank(basicPackageName)) {
            attributes.put(ATTR_BASIC_PACKAGE_PATH, shmDetailsRetriever.getBasicPackageFilePath(basicPackageName, smrsRootDir));
        }

        final ManagedObject licenseMo = nodeMo.getChild(MoType.LICENSE_OPTIONS.toString() + "=1");
        if (isLicenceInstalled(licenseMo)) {
            attributes.put(ATTR_LICENSE_KEY_FILE_PATH, shmDetailsRetriever.getLicenseKeyFilePath(nodeMo.getName(), smrsRootDir));
        }

        final ManagedObject securityMo = nodeMo.getChild(MoType.SECURITY.toString() + "=1");
        if (securityMo != null) {
            attributes.put(ATTR_ISCF_PATH, getIscfFilePath(securityMo, smrsRootDir));
        }
        return attributes;
    }

    private static String getUpgradePackageName(final ManagedObject autoIntegrationMo) {
        return autoIntegrationMo.getAttribute(AutoIntegrationAttribute.UPGRADE_PACKAGE.toString());
    }

    private static String getBasicPackageName(final ManagedObject autoIntegrationMo) {
        return autoIntegrationMo.getAttribute(AutoIntegrationAttribute.BASIC_PACKAGE.toString());
    }

    private static boolean isLicenceInstalled(final ManagedObject licenseMo) {
        return licenseMo.getAttribute(LicenseAttribute.INSTALL_LICENSE.toString());
    }

    private String getSiteBasicFilePath(final String apNodeFdn, final String smrsRootDir) {
        final ArtifactDetails siteBasicArtifact = generatedArtifactHandler.readFirstOfType(apNodeFdn, ArtifactType.SITEBASIC.toString());
        final String absoluteSiteBasicPath = siteBasicArtifact.getLocation();
        return getPathRelativeToSmrsRootDir(absoluteSiteBasicPath, smrsRootDir);
    }

    private String getSiteEquipmentFilePath(final String apNodeFdn, final String smrsRootDir) {
        final ArtifactDetails siteEquipmentArtifact = generatedArtifactHandler.readFirstOfType(apNodeFdn, ArtifactType.SITEEQUIPMENT.toString());
        final String absoluteSiteEquiptmentPath = siteEquipmentArtifact.getLocation();
        return getPathRelativeToSmrsRootDir(absoluteSiteEquiptmentPath, smrsRootDir);
    }

    private static String getIscfFilePath(final ManagedObject securityMo, final String smrsRootDir) {
        final String iscfLocation = securityMo.getAttribute(SecurityAttribute.ISCF_FILE_LOCATION.toString());
        return getPathRelativeToSmrsRootDir(iscfLocation, smrsRootDir);
    }

    private static String getPathRelativeToSmrsRootDir(final String absolutePath, final String smrsRootDir) {
        return absolutePath.substring(smrsRootDir.length() + 1);
    }

    private String getSmrsRootDirectory(final String apNodeFdn) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        return smrsService.getNodeSpecificAccount("AI", ERBS_NODE_TYPE, nodeName).getSmrsRootDirectory();
    }
}
