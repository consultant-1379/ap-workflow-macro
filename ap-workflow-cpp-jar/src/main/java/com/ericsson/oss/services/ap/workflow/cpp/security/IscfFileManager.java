/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.security;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.smrs.SmrsAccount;
import com.ericsson.oss.itpf.smrs.SmrsService;
import com.ericsson.oss.services.ap.common.artifacts.util.ArtifactResourceOperations;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;

/**
 * Manage creation and deletion of ISCF security file on SMRS.
 */
class IscfFileManager {

    private static final String AUTO_PROVISIONING_ACCOUNT_TYPE = "AI";
    private static final String ISCF_FILE_NAME = "Iscf.xml";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SmrsService smrsService;

    @Inject
    private ArtifactResourceOperations artifactResourceOperations;

    @PostConstruct
    public void init() {
        smrsService = new ServiceFinderBean().find(SmrsService.class);
    }

    /**
     * Stores the ISCF security file on the SMRS.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param fileContents
     *            the file contents for the ISCF file
     * @param cppNodeType
     *            the node type of the CPP-based node
     * @return the file path of the created file
     */
    public String createOnSmrs(final String apNodeFdn, final byte[] fileContents, final CppNodeType cppNodeType) {
        final String filePath = getSecurityFilePath(apNodeFdn, cppNodeType);
        logger.debug("Creating generated ISCF file {}", filePath);
        artifactResourceOperations.writeArtifact(filePath, fileContents);

        return filePath;
    }

    /**
     * Removes the ISCF security file and its parent security directory from the SMRS.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param cppNodeType
     *            the node type of the CPP-based node
     */
    public void deleteFromSmrs(final String apNodeFdn, final CppNodeType cppNodeType) {
        final String filePath = getSecurityFilePath(apNodeFdn, cppNodeType);
        logger.debug("Deleting file {}", filePath);
        artifactResourceOperations.deleteFile(filePath);
    }

    private String getSecurityFilePath(final String apNodeFdn, final CppNodeType cppNodeType) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        final SmrsAccount smrsAccount = smrsService.getNodeSpecificAccount(AUTO_PROVISIONING_ACCOUNT_TYPE, cppNodeType.toString(), nodeName);
        return smrsAccount.getHomeDirectory() + ISCF_FILE_NAME;
    }
}
