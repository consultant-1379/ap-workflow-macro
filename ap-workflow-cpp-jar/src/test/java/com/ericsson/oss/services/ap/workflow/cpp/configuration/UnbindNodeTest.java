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
package com.ericsson.oss.services.ap.workflow.cpp.configuration;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.ArtifactsHandler;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Unit tests for {@link UnbindNode}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UnbindNodeTest {

    @Mock
    private ArtifactsHandler artifactsHandler;

    @InjectMocks
    private UnbindNode unbindNode;

    @Test
    public void when_unbind_node_success_siteInstall_sent_for_delete() {
        unbindNode.execute(NODE_FDN);
        verify(artifactsHandler).deleteGeneratedArtifact(ArtifactType.SITEINSTALLFORBIND.toString(), NODE_FDN);
    }

    @Test(expected = ApServiceException.class)
    public void when_unbind_node_fails_exception_wrapped_as_APServiceException() {
        doThrow(Exception.class).when(artifactsHandler).deleteGeneratedArtifact(ArtifactType.SITEINSTALLFORBIND.toString(), NODE_FDN);
        unbindNode.execute(NODE_FDN);
    }
}