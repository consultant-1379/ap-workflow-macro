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

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.NodeType;

/**
 * Resolver class to find and return an instance of {@link RbsSummaryHandler} for the given node type.
 */
public class RbsSummaryHandlerResolver {

    @Inject
    @Any
    private Instance<RbsSummaryHandler> rbsSummaryHandlers;

    /**
     * Retrieves an instance of {@link RbsSummaryHandler} for the given node type.
     *
     * @param nodeType
     *            the node type
     * @return the RbsSummaryHandler interface
     */
    public RbsSummaryHandler getRbsSummaryHandler(final CppNodeType nodeType) {
        for (final RbsSummaryHandler handler : rbsSummaryHandlers) {
            if (isHandlerForNodeType(handler, nodeType)) {
                return handler;
            }
        }

        throw new ApApplicationException(
                String.format("Unable to find %s for %s node", RbsSummaryHandler.class.getSimpleName(), nodeType.toString()));
    }

    private static boolean isHandlerForNodeType(final RbsSummaryHandler handler, final CppNodeType nodeType) {
        final Class<? extends RbsSummaryHandler> handlerClass = handler.getClass();
        return handlerClass.isAnnotationPresent(NodeType.class) && handlerClass.getAnnotation(NodeType.class).type() == nodeType;
    }
}