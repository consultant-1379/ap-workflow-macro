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
package com.ericsson.oss.services.ap.workflow.cpp.artifacts;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Instance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.NodeType;

/**
 * Unit tests for {@link RbsSummaryHandlerResolver}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RbsSummaryHandlerResolverTest {

    @Mock
    private Instance<RbsSummaryHandler> rbsSummaryHandlers;

    @InjectMocks
    private RbsSummaryHandlerResolver rbsSummaryHandlerResovler;

    @Test
    public void whenGetRbsSummarryHandler_andHandlerForNodeTypeExists_thenManagerIsReturned() {
        final List<RbsSummaryHandler> rbsSummaryHandlersList = new ArrayList<>();
        rbsSummaryHandlersList.add(new ErbsRbsSummaryHandler());
        when(rbsSummaryHandlers.iterator()).thenReturn(rbsSummaryHandlersList.iterator());

        final RbsSummaryHandler resut = rbsSummaryHandlerResovler.getRbsSummaryHandler(CppNodeType.ERBS);

        assertEquals(CppNodeType.ERBS, resut.getClass().getAnnotation(NodeType.class).type());
    }

    @Test(expected = ApApplicationException.class)
    public void whenGetRbsSummaryHandler_andHandlerExistsWithNoAnnotation_thenApApplicationExceptionIsThrown() {
        final List<RbsSummaryHandler> rbsSummaryHandlersList = new ArrayList<>();
        rbsSummaryHandlersList.add(new NoAnnotationRbsSummaryHandler());
        when(rbsSummaryHandlers.iterator()).thenReturn(rbsSummaryHandlersList.iterator());
        rbsSummaryHandlerResovler.getRbsSummaryHandler(CppNodeType.ERBS);
    }

    @Test(expected = ApApplicationException.class)
    public void whenGetRbsSummarryHandler_andHandlerForNodeTypeDoesNotExist_thenApApplicationExceptionIsThrown() {
        when(rbsSummaryHandlers.iterator()).thenReturn(Collections.<RbsSummaryHandler> emptyIterator());
        rbsSummaryHandlerResovler.getRbsSummaryHandler(CppNodeType.RBS);
    }

    @NodeType(type = CppNodeType.ERBS)
    private static class ErbsRbsSummaryHandler implements RbsSummaryHandler {

        @Override
        public String generate(final String apNodeFdn) {
            return null;
        }

        @Override
        public String getRelativeRbsSummaryPath(final String apNodeFdn) {
            return null;
        }
    }

    private static class NoAnnotationRbsSummaryHandler implements RbsSummaryHandler {

        @Override
        public String generate(final String apNodeFdn) {
            return null;
        }

        @Override
        public String getRelativeRbsSummaryPath(final String apNodeFdn) {
            return null;
        }
    }
}
