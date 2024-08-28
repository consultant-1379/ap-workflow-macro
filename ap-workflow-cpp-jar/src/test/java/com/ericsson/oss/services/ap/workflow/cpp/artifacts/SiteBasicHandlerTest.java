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
 * Unit tests for {@link SiteBasicHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteBasicHandlerTest {

    private static final String FILE_CONTENTS = "fileContents";
    private static final ArtifactDetails SITE_BASIC_ARTIFACT = new ArtifactDetails.ArtifactBuilder().artifactContent(FILE_CONTENTS).build();

    @Mock
    private RawArtifactHandler rawArtifactHandler;

    @Mock
    private GeneratedArtifactHandler generatedArtifactHandler;

    @InjectMocks
    private SiteBasicHandler siteBasicHandler;

    @Test
    public void when_generate_sitebasic_file_then_site_basic_created_with_correct_arguments() {
        when(rawArtifactHandler.readFirstOfType(NODE_FDN, "SiteBasic")).thenReturn(SITE_BASIC_ARTIFACT);
        siteBasicHandler.generate(NODE_FDN, CppNodeType.ERBS);
        verify(generatedArtifactHandler).createOnSmrs(argThat(new ArgumentMatcher<ArtifactDetails>() {

            @Override
            public boolean matches(final Object argument) {
                final ArtifactDetails artifact = (ArtifactDetails) argument;
                return artifact.getArtifactContent().equals(FILE_CONTENTS) && artifact.getApNodeFdn().equals(NODE_FDN)
                        && artifact.getName().equals("SiteBasic") && artifact.getType().equals("SiteBasic");
            }
        }), eq("ERBS"));
    }

    @Test
    public void when_generate_sitebasic_file_then_return_site_basic_contents_identical_to_raw_file() {
        when(rawArtifactHandler.readFirstOfType(NODE_FDN, "SiteBasic")).thenReturn(SITE_BASIC_ARTIFACT);
        final String contents = siteBasicHandler.generate(NODE_FDN, CppNodeType.ERBS);
        assertEquals(FILE_CONTENTS, contents);
    }
}
