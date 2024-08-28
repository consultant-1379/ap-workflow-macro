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
package com.ericsson.oss.services.ap.workflow.erbs.task.integrate;

import com.ericsson.oss.services.ap.common.workflow.task.integrate.AbstractBindTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

/**
 * Bind Service Task.
 */
public class BindTask extends AbstractBindTask {

    @Override
    protected void bindNode(final String apNodeFdn, final String hardwareSerialNumber) {
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        workflowTaskFacade.bindNodeManually(apNodeFdn, hardwareSerialNumber, CppNodeType.ERBS);
    }
}
