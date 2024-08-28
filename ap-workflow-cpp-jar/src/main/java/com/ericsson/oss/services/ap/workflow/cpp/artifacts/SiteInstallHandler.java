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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.smrs.SmrsAddressRequest;
import com.ericsson.oss.itpf.smrs.SmrsService;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.raw.RawArtifactHandler;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.common.util.xml.DocumentBuilder;
import com.ericsson.oss.services.ap.common.util.xml.DocumentWriter;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Handler to generate a {@link ArtifactType#SITEINSTALL} file.
 */
public class SiteInstallHandler {

    private static final String ARTIFACT_NAME = "SiteInstall.xml";
    private static final String RBS_INTEGRITY_CODE = "rbsIntegrityCode";
    private static final String RBS_INTEGRATION_CODE = "rbsIntegrationCode";
    private static final String INSTALLATION_DATA = "InstallationData";
    private static final String SMRS_DATA = "SmrsData";
    private static final String RBSSUMMARYFILE_PATH = "summaryFilePath";
    private static final String SMRS_ADDRESS = "address";
    private static final String SFTP_PORT_PROPERTY = "smrs_sftp_securePort";
    private static final String SMRS_PORT = "port";

    @Inject
    private DpsOperations dpsOperations;

    private SmrsService smrsService;

    @Inject
    private GeneratedArtifactHandler generatedArtifactHandler;

    @Inject
    private RawArtifactHandler rawArtifactHandler;

    @Inject
    private RbsSummaryHandlerResolver rbsSummaryHandlerResolver;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PostConstruct
    public void init() {
        smrsService = new ServiceFinderBean().find(SmrsService.class);
    }

    /**
     * Generates the {@link ArtifactType#SITEINSTALL} artifact and stores on SFS.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param nodeType
     *            the node type
     * @return the generated SiteInstallation file
     */
    public String generate(final String apNodeFdn, final CppNodeType nodeType) {
        final String generatedSiteInstallFile = createGeneratedFileContents(apNodeFdn, nodeType);
        final ArtifactDetails generatedSiteInstallArtifact = new ArtifactDetails.ArtifactBuilder()
                .apNodeFdn(apNodeFdn)
                .artifactContent(generatedSiteInstallFile)
                .exportable(true)
                .type(ArtifactType.SITEINSTALL.toString())
                .name(ARTIFACT_NAME)
                .encrypted(true)
                .build();
        generatedArtifactHandler.create(generatedSiteInstallArtifact);
        return generatedSiteInstallFile;
    }

    private String createGeneratedFileContents(final String apNodeFdn, final CppNodeType nodeType) {
        final ArtifactDetails rawSiteInstallArtifact = rawArtifactHandler.readFirstOfType(apNodeFdn, ArtifactType.SITEINSTALL.toString());

        final String smrsAddress = getSmrsAddress(apNodeFdn);
        final RbsSummaryHandler rbsSummaryHandler = rbsSummaryHandlerResolver.getRbsSummaryHandler(nodeType);
        final String rbsSummaryPath = rbsSummaryHandler.getRelativeRbsSummaryPath(apNodeFdn);
        final String sftpPort = System.getProperty(SFTP_PORT_PROPERTY);
        final String rbsIntegrationCode = findRbsIntegrityCode(apNodeFdn);

        final Map<String, String> substitutionAttributes = new HashMap<>(4);
        substitutionAttributes.put(SMRS_ADDRESS, smrsAddress);
        substitutionAttributes.put(RBSSUMMARYFILE_PATH, rbsSummaryPath);
        final boolean customSftpPortDefined = sftpPort != null && !sftpPort.isEmpty() && !sftpPort.equals("22");
        String rawFile = "";

        if (rbsIntegrationCode != null) {
            substitutionAttributes.put(RBS_INTEGRATION_CODE, rbsIntegrationCode);
        }

        if (customSftpPortDefined) {
            logger.info("Custom SFTP port has been defined with value: {}", sftpPort);
            substitutionAttributes.put(SMRS_PORT, sftpPort);
            rawFile = createAttributesInSmrsDataElement(rawSiteInstallArtifact.getArtifactContent(), SMRS_DATA, SMRS_ADDRESS,
                    SMRS_PORT,
                    RBSSUMMARYFILE_PATH);
        }

        else {
            rawFile = createAttributesInSmrsDataElement(rawSiteInstallArtifact.getArtifactContent(), SMRS_DATA, SMRS_ADDRESS,
                    RBSSUMMARYFILE_PATH);
        }
        return substituteAttributeValues(rawFile, substitutionAttributes);
    }

    private static String createAttributesInSmrsDataElement(final String artifactContent, final String elementToUpdate,
            final String... attributesToAdd) {
        final Document siteInstallDoc = DocumentBuilder.getDocument(artifactContent);
        Node smrsDataElement = siteInstallDoc.getElementsByTagName(elementToUpdate).item(0);

        if (smrsDataElement == null) {
            smrsDataElement = createSmrsDataElement(siteInstallDoc);
        }

        for (final String attributeToAdd : attributesToAdd) {
            ((Element) smrsDataElement).setAttribute(attributeToAdd, "");
        }

        return DocumentBuilder.getDocumentAsString(siteInstallDoc);
    }

    private static Node createSmrsDataElement(final Document siteInstallDoc) {
        final Node siteInstall = siteInstallDoc.getElementsByTagName(INSTALLATION_DATA).item(0);
        if (siteInstall == null) {
            throw new ApApplicationException(INSTALLATION_DATA + " tag cannot be found in site installation file");
        }
        siteInstall.appendChild(siteInstallDoc.createElement(SMRS_DATA));
        return siteInstallDoc.getElementsByTagName(SMRS_DATA).item(0);
    }

    private String getSmrsAddress(final String apNodeFdn) {
        final SmrsAddressRequest addressRequest = new SmrsAddressRequest();
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();

        addressRequest.setAccountType("AI");
        addressRequest.setNeName(nodeName);

        return smrsService.getFileServerAddress(addressRequest);
    }

    private String findRbsIntegrityCode(final String apNodeFdn) {
        String integrationCode = null;
        final String securityMoFdn = apNodeFdn + "," + MoType.SECURITY.toString() + "=1";
        final ManagedObject securityMo = dpsOperations.getDataPersistenceService().getLiveBucket().findMoByFdn(securityMoFdn);

        if (securityMo != null) {
            integrationCode = securityMo.getAttribute(RBS_INTEGRITY_CODE);
        }
        return integrationCode;
    }

    private static String substituteAttributeValues(final String siteInstallFileString, final Map<String, String> substitutionAttributes) {
        final Document siteInstallDoc = DocumentBuilder.getDocument(siteInstallFileString);
        final DocumentWriter documentWriter = new DocumentWriter(siteInstallDoc);
        documentWriter.setAttributeValues(substitutionAttributes);
        return DocumentBuilder.getDocumentAsString(siteInstallDoc);
    }

}
