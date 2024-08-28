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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.raw.RawArtifactHandler;
import com.ericsson.oss.services.ap.common.util.cdi.Transactional;
import com.ericsson.oss.services.ap.common.util.cdi.Transactional.TxType;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Handles creation, deletion, reading of CPP artifacts.
 */
public class ArtifactsHandler {

    @Inject
    private RawArtifactHandler rawArtifactHandler;

    @Inject
    private GeneratedArtifactHandler generatedArtifactHandler;

    @Inject
    private RbsSummaryHandlerResolver rbsSummaryHandlerResolver;

    @Inject
    private SiteBasicHandler siteBasicHandler;

    @Inject
    private SiteEquipmentHandler siteEquipmentHandler;

    @Inject
    private SiteInstallHandler siteInstallHandler;

    @Inject
    private SiteInstallForBindHandler siteInstallForBindHandler;

    public void createGeneratedArtifact(final String artifactType, final String apNodeFdn, final CppNodeType nodeType) {
        if (ArtifactType.RBSSUMMARY.toString().equalsIgnoreCase(artifactType)) {
            final RbsSummaryHandler rbsSummaryHandler = rbsSummaryHandlerResolver.getRbsSummaryHandler(nodeType);
            rbsSummaryHandler.generate(apNodeFdn);
        } else if (ArtifactType.SITEBASIC.toString().equalsIgnoreCase(artifactType)) {
            siteBasicHandler.generate(apNodeFdn, nodeType);
        } else if (ArtifactType.SITEEQUIPMENT.toString().equalsIgnoreCase(artifactType)) {
            siteEquipmentHandler.generate(apNodeFdn, nodeType);
        } else if (ArtifactType.SITEINSTALL.toString().equalsIgnoreCase(artifactType)) {
            siteInstallHandler.generate(apNodeFdn, nodeType);
        } else if (ArtifactType.SITEINSTALLFORBIND.toString().equalsIgnoreCase(artifactType)) {
            siteInstallForBindHandler.generate(apNodeFdn, nodeType);
        }
    }

    /**
     * Updates the specified generated artifact. Currently only supported for siteInstallForBind.
     *
     * @param artifactType
     *            the type of the artifact to check
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param nodeType
     *            the type of Cpp Node
     */
    public void updateGeneratedArtifact(final String artifactType, final String apNodeFdn, final CppNodeType nodeType) {
        if (ArtifactType.SITEINSTALLFORBIND.toString().equals(artifactType)) {
            siteInstallForBindHandler.update(apNodeFdn, nodeType);
        }
    }

    /**
     * Deletes the generated artifact for the given type for the given AP node.
     *
     * @param artifactType
     *            the type of the artifact to check
     * @param apNodeFdn
     *            the FDN of the AP node
     */
    public void deleteGeneratedArtifact(final String artifactType, final String apNodeFdn) {
        generatedArtifactHandler.deleteAllOfType(apNodeFdn, artifactType);
    }

    /**
     * Deletes all raw and generated artifacts for the given AP node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     */
    public void deleteAllArtifacts(final String apNodeFdn) {
        generatedArtifactHandler.deleteAllForNode(apNodeFdn);
        rawArtifactHandler.deleteAllForNode(apNodeFdn);
    }

    /**
     * Get the location on the file system of all artifacts of the given type.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param artifactType
     *            the type of the artifact to check
     * @return a list of all filepaths for the raw artifacts
     */
    @Transactional(txType = TxType.REQUIRES)
    public List<String> getRawArtifactsLocation(final String apNodeFdn, final String artifactType) {
        final Collection<ArtifactDetails> rawArtifacts = rawArtifactHandler.readAllOfType(apNodeFdn, artifactType);
        final List<String> rawArtifactsFilesPaths = new ArrayList<>(rawArtifacts.size());

        for (final ArtifactDetails rawArtifact : rawArtifacts) {
            rawArtifactsFilesPaths.add(rawArtifact.getLocation());
        }

        return rawArtifactsFilesPaths;
    }
}
