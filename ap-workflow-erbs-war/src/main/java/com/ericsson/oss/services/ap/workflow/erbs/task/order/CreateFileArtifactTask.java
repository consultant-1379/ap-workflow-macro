/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.task.order;

import com.ericsson.oss.services.ap.common.workflow.task.order.AbstractCreateFileArtifactTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

/**
 * Common Service Task for creation of Auto Provisioning artifacts.
 */
public class CreateFileArtifactTask extends AbstractCreateFileArtifactTask {

    @Override
    protected void createArtifact(final String apNodeFdn, final String artifactType) {
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        workflowTaskFacade.createGeneratedArtifact(artifactType, apNodeFdn, CppNodeType.ERBS);
    }
}
