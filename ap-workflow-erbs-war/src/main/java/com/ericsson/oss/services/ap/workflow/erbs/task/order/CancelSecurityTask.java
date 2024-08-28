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

import com.ericsson.oss.services.ap.common.workflow.task.order.AbstractCancelSecurityTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

/**
 * Task class to cancel the provisioning of node security for an ERBS AP node.
 */
public class CancelSecurityTask extends AbstractCancelSecurityTask {

    @Override
    protected void cancelSecurity(final String apNodeFdn) {
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        workflowTaskFacade.cancelSecurity(apNodeFdn, CppNodeType.ERBS);
    }
}
