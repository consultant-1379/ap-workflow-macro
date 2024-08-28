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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.ap.common.artifacts.ArtifactDetails;
import com.ericsson.oss.services.ap.common.artifacts.generated.GeneratedArtifactHandler;
import com.ericsson.oss.services.ap.common.artifacts.raw.RawArtifactHandler;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;

/**
 * Unit tests for {@link SiteEquipmentHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteEquipmentHandlerTest {

    private static final String FILE_CONTENTS = "fileContents";
    private static final ArtifactDetails SITE_EQUIPMENT_ARTIFACT = new ArtifactDetails.ArtifactBuilder().artifactContent(FILE_CONTENTS).build();

    @Mock
    private RawArtifactHandler rawArtifactHandler;

    @Mock
    private GeneratedArtifactHandler generatedArtifactHandler;

    @InjectMocks
    private SiteEquipmentHandler siteEquipmentHandler;

    @Test
    public void when_generate_siteequipment_file_then_site_equipment_created_with_correct_arguments() {
        when(rawArtifactHandler.readFirstOfType(NODE_FDN, "SiteEquipment")).thenReturn(SITE_EQUIPMENT_ARTIFACT);

        siteEquipmentHandler.generate(NODE_FDN, CppNodeType.ERBS);
        verify(generatedArtifactHandler).createOnSmrs(argThat(new ArgumentMatcher<ArtifactDetails>() {

            @Override
            public boolean matches(final Object argument) {
                final ArtifactDetails artifact = (ArtifactDetails) argument;
                return artifact.getArtifactContent().equals(FILE_CONTENTS) && artifact.getApNodeFdn().equals(NODE_FDN)
                        && artifact.getName().equals("RbsEquipment") && artifact.getType().equals("SiteEquipment");
            }
        }), eq(CppNodeType.ERBS.toString()));
    }

    @Test
    public void when_generate_siteequipment_file_then_return_siteequipment_contents_identical_to_raw_file() {
        when(rawArtifactHandler.readFirstOfType(NODE_FDN, "SiteEquipment")).thenReturn(SITE_EQUIPMENT_ARTIFACT);

        final String contents = siteEquipmentHandler.generate(NODE_FDN, CppNodeType.ERBS);
        assertEquals(FILE_CONTENTS, contents);
    }
}
