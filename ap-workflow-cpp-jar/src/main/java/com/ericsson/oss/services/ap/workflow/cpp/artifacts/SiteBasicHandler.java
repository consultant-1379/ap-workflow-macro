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

import javax.inject.Inject;

import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.raw.RawArtifactHandler;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Handler to generate a {@link ArtifactType#SITEBASIC} file.
 */
public class SiteBasicHandler {

    private static final String ARTIFACT_NAME = "SiteBasic.xml";

    @Inject
    private RawArtifactHandler rawArtifactHandler;

    @Inject
    private GeneratedArtifactHandler generatedArtifactHandler;

    /**
     * Generates the {@link ArtifactType#SITEBASIC} artifact and stores on SMRS.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param nodeType
     *            the type of CPP Node
     * @return the generated SiteBasic file
     */
    public String generate(final String apNodeFdn, final CppNodeType nodeType) {
        final String generatedSiteBasicFile = readRawSiteBasicArtifact(apNodeFdn);
        final ArtifactDetails generatedSiteInstallArtifact = new ArtifactDetails.ArtifactBuilder()
                .apNodeFdn(apNodeFdn)
                .apNodeFdn(apNodeFdn)
                .exportable(false)
                .artifactContent(generatedSiteBasicFile)
                .type(ArtifactType.SITEBASIC.toString())
                .name(ARTIFACT_NAME)
                .build();
        generatedArtifactHandler.createOnSmrs(generatedSiteInstallArtifact, nodeType.toString());
        return generatedSiteBasicFile;
    }

    private String readRawSiteBasicArtifact(final String apNodeFdn) {
        final ArtifactDetails artifactDetails = rawArtifactHandler.readFirstOfType(apNodeFdn, ArtifactType.SITEBASIC.toString());
        return artifactDetails.getArtifactContent();
    }
}
