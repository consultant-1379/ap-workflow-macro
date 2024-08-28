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
package com.ericsson.oss.services.ap.workflow.erbs.task.order;

import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.workflow.task.common.AbstractAutoBindTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

/**
 * Bind Service Task for binding a node during order.
 */
public class BindDuringOrderTask extends AbstractAutoBindTask {

    private final DdpTimer ddpTimer = new DdpTimer();

    @Override
    protected void bindNode(final String apNodeFdn, final String hardwareSerialNumber, final boolean bindWithNodeName) {
        try {
            ddpTimer.start(CommandLogName.BIND_DURING_ORDER.toString());
            final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
            workflowTaskFacade.bindNodeDuringOrder(apNodeFdn, hardwareSerialNumber, CppNodeType.ERBS);
            ddpTimer.end(apNodeFdn);
        } catch (final Exception e) {
            ddpTimer.endWithError(apNodeFdn);
            throw e;
        }
    }
}
