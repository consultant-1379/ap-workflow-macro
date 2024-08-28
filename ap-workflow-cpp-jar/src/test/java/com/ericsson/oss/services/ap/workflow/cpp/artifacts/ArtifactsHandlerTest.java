/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails.ArtifactBuilder;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.raw.RawArtifactHandler;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;

/**
 * Unit tests for {@link ArtifactsHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ArtifactsHandlerTest {

    @Mock
    private RbsSummaryHandler rbsSummaryHandler;

    @Mock
    private RbsSummaryHandlerResolver rbsSummaryHandlerResolver;

    @Mock
    private SiteBasicHandler siteBasicHandler;

    @Mock
    private SiteEquipmentHandler siteEquipmentHandler;

    @Mock
    private SiteInstallHandler siteInstallHandler;

    @Mock
    private SiteInstallForBindHandler siteInstallForBindHandler;

    @Mock
    private GeneratedArtifactHandler generatedArtifactHandler;

    @Mock
    private RawArtifactHandler rawArtifactHandler;

    @InjectMocks
    private ArtifactsHandler artifactsHandler;

    @Test
    public void createGeneratedArtifact_calls_generate_for_RbsSummary() {
        when(rbsSummaryHandlerResolver.getRbsSummaryHandler(CppNodeType.ERBS)).thenReturn(rbsSummaryHandler);
        artifactsHandler.createGeneratedArtifact("RbsSummary", NODE_FDN, CppNodeType.ERBS);
        verify(rbsSummaryHandler).generate(NODE_FDN);
    }

    @Test
    public void createGeneratedArtifact_calls_generate_for_SiteBasic() {
        artifactsHandler.createGeneratedArtifact("SiteBasic", NODE_FDN, CppNodeType.ERBS);
        verify(siteBasicHandler).generate(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void createGeneratedArtifact_calls_generate_for_SiteEquipment() {
        artifactsHandler.createGeneratedArtifact("SiteEquipment", NODE_FDN, CppNodeType.ERBS);
        verify(siteEquipmentHandler).generate(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void createGeneratedArtifact_calls_generate_for_SiteIntall() {
        artifactsHandler.createGeneratedArtifact("SiteInstallation", NODE_FDN, CppNodeType.ERBS);
        verify(siteInstallHandler).generate(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void createGeneratedArtifact_calls_generate_for_SiteIntallForBind() {
        artifactsHandler.createGeneratedArtifact("SiteInstallForBind", NODE_FDN, CppNodeType.ERBS);
        verify(siteInstallForBindHandler).generate(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void when_updateGeneratedArtifact_for_SiteInstallForBind_then_update_called() {
        artifactsHandler.updateGeneratedArtifact("SiteInstallForBind", NODE_FDN, CppNodeType.ERBS);
        verify(siteInstallForBindHandler).update(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void when_updateGeneratedArtifact_for_unsupported_handler_then_update_not_called() {
        artifactsHandler.updateGeneratedArtifact("SiteEquipment", NODE_FDN, CppNodeType.ERBS);
        verify(siteInstallForBindHandler, never()).update(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void when_delete_generated_they_are_passed_to_delete() {
        artifactsHandler.deleteGeneratedArtifact("SiteInstallation", NODE_FDN);
        verify(generatedArtifactHandler).deleteAllOfType(NODE_FDN, "SiteInstallation");
    }

    @Test
    public void when_delete_all_then_raw_and_generated_deleted() {
        artifactsHandler.deleteAllArtifacts(NODE_FDN);
        verify(generatedArtifactHandler).deleteAllForNode(NODE_FDN);
        verify(rawArtifactHandler).deleteAllForNode(NODE_FDN);
    }

    @Test
    public void when_invalid_artifactType_passed_to_create_then_nothing_called() {
        artifactsHandler.createGeneratedArtifact("InvalidArtifactType", NODE_FDN, CppNodeType.ERBS);
        verify(rbsSummaryHandler, never()).generate(NODE_FDN);
        verify(siteBasicHandler, never()).generate(NODE_FDN, CppNodeType.ERBS);
        verify(siteEquipmentHandler, never()).generate(NODE_FDN, CppNodeType.ERBS);
        verify(siteInstallHandler, never()).generate(NODE_FDN, CppNodeType.ERBS);
        verify(siteInstallForBindHandler, never()).generate(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void when_get_raw_artifacts_location_return_valid_collection_of_locations() {
        final List<ArtifactDetails> artifacts = new ArrayList<>();
        final ArtifactBuilder artifactBuilder1 = new ArtifactBuilder();
        artifactBuilder1.location("Test1");
        artifacts.add(artifactBuilder1.build());
        final ArtifactBuilder artifactBuilder2 = new ArtifactBuilder();
        artifactBuilder2.location("Test2");
        artifacts.add(artifactBuilder2.build());
        when(rawArtifactHandler.readAllOfType(NODE_FDN, CppNodeType.ERBS.toString())).thenReturn(artifacts);

        final List<String> artifactsLocationsResult = artifactsHandler.getRawArtifactsLocation(NODE_FDN, CppNodeType.ERBS.toString());

        final List<String> expectedArtifactsLocations = new ArrayList<>();
        expectedArtifactsLocations.add("Test1");
        expectedArtifactsLocations.add("Test2");

        assertTrue(artifactsLocationsResult.containsAll(expectedArtifactsLocations));
    }
}