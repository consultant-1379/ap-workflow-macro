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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.w3c.dom.Document;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.security.identitymgmtservices.IdentityManagementService;
import com.ericsson.oss.itpf.smrs.SmrsAccount;
import com.ericsson.oss.itpf.smrs.SmrsService;
import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.configuration.DirectoryConfiguration;
import com.ericsson.oss.services.ap.common.model.NodeAttribute;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.common.util.xml.DocumentBuilder;
import com.ericsson.oss.services.ap.common.util.xml.DocumentWriter;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Handler for the generated {@link ArtifactType#SITEINSTALLFORBIND} file. This file is generated from the SiteInstall file on a bind, and is updated
 * with the SMRS account name and password, and then stored with the hardware serial number as its name.
 */
public class SiteInstallForBindHandler {

    private static final String SMRS_DATA_ELEMENT = "SmrsData";
    private static final String SMRS_USERNAME_ATTRIBUTE = "userName";
    @SuppressWarnings("squid:S2068")
    private static final String SMRS_PASSWORD_ATTRIBUTE = "password";

    @Inject
    private GeneratedArtifactHandler generatedArtifactHandler;

    private IdentityManagementService identityMgtService;

    private SmrsService smrsService;

    @Inject
    private DpsOperations dpsOperations;

    @PostConstruct
    public void init() {
        identityMgtService = new ServiceFinderBean().find(IdentityManagementService.class);
        smrsService = new ServiceFinderBean().find(SmrsService.class);
    }

    /**
     * Creates a 'generated' SiteInstall artifact with additional information for SMRS user and password. The updated file is stored in the bind
     * directory and named according to the hardware serial number of the AP node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param nodeType
     *            the type of CPP node
     * @return the generated siteInstall bind file
     */
    public String generate(final String apNodeFdn, final CppNodeType nodeType) {
        final byte[] generatedSiteInstallBindFile = createEncryptedGeneratedFileContents(apNodeFdn, nodeType);
        final ArtifactDetails generatedSiteInstallArtifact = createArtifactDetails(apNodeFdn, generatedSiteInstallBindFile);
        generatedArtifactHandler.create(generatedSiteInstallArtifact);
        return new String(generatedSiteInstallBindFile);
    }

    /**
     * Updates an already generated SiteInstall artifact.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param nodeType
     *            the type of Cpp node
     */
    public void update(final String apNodeFdn, final CppNodeType nodeType) {
        final byte[] generatedSiteInstallBindFile = createEncryptedGeneratedFileContents(apNodeFdn, nodeType);
        final ArtifactDetails generatedSiteInstallArtifact = createArtifactDetails(apNodeFdn, generatedSiteInstallBindFile);
        generatedArtifactHandler.updateArtifact(generatedSiteInstallArtifact);
    }

    private String getBindFilename(final String apNodeFdn) {
        final ManagedObject nodeMo = dpsOperations.getDataPersistenceService().getLiveBucket().findMoByFdn(apNodeFdn);
        final Object hardwareSerialNumberObject = nodeMo.getAttribute(NodeAttribute.HARDWARE_SERIAL_NUMBER.toString());
        if (hardwareSerialNumberObject == null) {
            throw new IllegalArgumentException("Hardware serial number not set");
        }
        final String hardwareSerialNumber = (String) hardwareSerialNumberObject;
        return hardwareSerialNumber + ".xml";
    }

    private byte[] createEncryptedGeneratedFileContents(final String apNodeFdn, final CppNodeType nodeType) {
        final String userName = getSmrsUserName(apNodeFdn, nodeType);
        final String password = getUserPassword(userName);
        final String siteInstallContents = getGeneratedSiteInstallContents(apNodeFdn);
        final String siteInstallAfterBind = updateSiteInstallWithSmrsData(siteInstallContents, userName, password);

        return siteInstallAfterBind.getBytes();
    }

    private String getSmrsUserName(final String apNodeFdn, final CppNodeType nodeType) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        final SmrsAccount smrsAccount = smrsService.getNodeSpecificAccount("AI", nodeType.toString(), nodeName);
        return smrsAccount.getUserName();
    }

    private String getUserPassword(final String userName) {
        final char[] password = identityMgtService.getM2MPassword(userName);
        return new String(password);
    }

    private String getGeneratedSiteInstallContents(final String apNodeFdn) {
        final ArtifactDetails artifactDetails = generatedArtifactHandler.readFirstOfType(apNodeFdn, ArtifactType.SITEINSTALL.toString());
        return artifactDetails.getArtifactContent();
    }

    private static String updateSiteInstallWithSmrsData(final String siteInstallContents, final String userName, final String password) {
        final Map<String, String> smrsAttributes = new HashMap<>();
        smrsAttributes.put(SMRS_USERNAME_ATTRIBUTE, userName);
        smrsAttributes.put(SMRS_PASSWORD_ATTRIBUTE, password);

        final Document siteInstallDoc = DocumentBuilder.getDocument(siteInstallContents);
        final DocumentWriter documentWriter = new DocumentWriter(siteInstallDoc);
        documentWriter.setElementAttributesValues(SMRS_DATA_ELEMENT, smrsAttributes);

        return DocumentBuilder.getDocumentAsString(siteInstallDoc);
    }

    private ArtifactDetails createArtifactDetails(final String apNodeFdn, final byte[] generatedSiteInstallBindFile) {
        final String siteInstallBindFileName = getBindFilename(apNodeFdn);
        final String siteInstallBindFilePath = DirectoryConfiguration.getBindDirectory() + File.separator + siteInstallBindFileName;
        return new ArtifactDetails.ArtifactBuilder().apNodeFdn(apNodeFdn)
                .artifactContent(generatedSiteInstallBindFile)
                .type(ArtifactType.SITEINSTALLFORBIND.toString())
                .location(siteInstallBindFilePath)
                .name(siteInstallBindFileName)
                .exportable(false)
                .encrypted(true)
                .build();
    }
}