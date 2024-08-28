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
package com.ericsson.oss.services.ap.workflow.cpp.security;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.nms.security.nscs.api.iscf.IscfResponse;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;

/**
 * Handles generation and cancellation of security.
 */
public class SecurityManager {

    @Inject
    private IscfDataGenerator iscfDataGenerator;

    @Inject
    private IscfFileManager iscfFileManager;

    @Inject
    private SecurityMoWriter securityMoWriter;

    @Inject
    private Logger logger;

    /**
     * Enables security for the specified node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param cppNodeType
     *            the node type of the CPP-based node
     */
    public void enableSecurity(final String apNodeFdn, final CppNodeType cppNodeType) {
        logger.info("Starting security generation for node {} via IscfService", apNodeFdn);

        final String securityFdn = getSecurityFdn(apNodeFdn);
        final IscfResponse iscfNodeSecurityData = iscfDataGenerator.generateIscfSecurityForNode(securityFdn);

        logger.info("Successfully generated security data for node {} via IscfService", apNodeFdn);
        final String securityFilePathOnSmrs = iscfFileManager.createOnSmrs(apNodeFdn, iscfNodeSecurityData.getIscfContent(), cppNodeType);
        logger.debug("Successfully created ISCF security file data for node {} on SMRS", apNodeFdn);

        try {
            securityMoWriter.updateSecurityAttributes(securityFdn, iscfNodeSecurityData, securityFilePathOnSmrs);
            logger.debug("Successfully updated AP security MO for node {}", apNodeFdn);
        } catch (final Exception e) {
            deleteIscfFileFromSmrs(apNodeFdn, cppNodeType); // File creates were in own Tx so remove separately.
            throw e;
        }
    }

    /**
     * Cancel any ongoing security processing and remove all security related data for the given node.
     *
     * @param apNodeFdn
     *            FDN of the AP node
     * @param cppNodeType
     *            the node type of the CPP-based node
     */
    public void cancelSecurity(final String apNodeFdn, final CppNodeType cppNodeType) {
        iscfDataGenerator.cancelIscfSecurity(apNodeFdn);
        logger.debug("Successfully cancelled security for node {} via IscfService", apNodeFdn);

        deleteIscfFileFromSmrs(apNodeFdn, cppNodeType);

        final String securityMoFdn = getSecurityFdn(apNodeFdn);
        securityMoWriter.resetSecurityAttributes(securityMoFdn);

        logger.debug("Successfully reset security attributes for node {}", apNodeFdn);
    }

    private void deleteIscfFileFromSmrs(final String apNodeFdn, final CppNodeType cppNodeType) {
        iscfFileManager.deleteFromSmrs(apNodeFdn, cppNodeType);
        logger.trace("Deleted ISCF file data from SMRS for node {}", apNodeFdn);
    }

    private static String getSecurityFdn(final String apNodeFdn) {
        return new StringBuilder()
                .append(apNodeFdn)
                .append(",")
                .append(MoType.SECURITY.toString())
                .append("=1")
                .toString();
    }
}
