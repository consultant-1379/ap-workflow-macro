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
package com.ericsson.oss.services.ap.workflow.cpp.configuration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.ArtifactsHandler;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Unbinds the node by deleting the site installation file.
 */
public class UnbindNode {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private ArtifactsHandler artifactsHandler;

    public void execute(final String apNodeFdn) {
        logger.info("Unbinding node: {}", apNodeFdn);
        final boolean deleteSiteInstall = deleteSiteInstallForBind(apNodeFdn);

        if (!deleteSiteInstall) {
            throw new ApServiceException(String.format("Error executing unbind for node %s", apNodeFdn));
        }
    }

    private boolean deleteSiteInstallForBind(final String apNodeFdn) {
        try {
            artifactsHandler.deleteGeneratedArtifact(ArtifactType.SITEINSTALLFORBIND.toString(), apNodeFdn);
            return true;
        } catch (final Exception e) {
            logger.error("Error deleting {} artifact: {}", ArtifactType.SITEINSTALLFORBIND, e.getMessage(), e);
        }
        return false;
    }
}
