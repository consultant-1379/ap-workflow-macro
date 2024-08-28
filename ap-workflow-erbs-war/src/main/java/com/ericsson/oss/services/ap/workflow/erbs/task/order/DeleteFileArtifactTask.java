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

import com.ericsson.oss.services.ap.common.workflow.task.order.AbstractDeleteFileArtifactTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

/**
 * Common Service Task for deleting Auto Provisioning artifacts.
 */
public class DeleteFileArtifactTask extends AbstractDeleteFileArtifactTask {

    @Override
    protected void deleteArtifact(final String apNodeFdn, final String artifactType) {
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        workflowTaskFacade.deleteGeneratedArtifact(artifactType, apNodeFdn);
    }
}
