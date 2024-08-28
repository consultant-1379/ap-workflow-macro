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
package com.ericsson.oss.services.ap.workflow.erbs.task.listener.integrate;

import com.ericsson.oss.services.ap.common.workflow.task.listener.TaskExecutionListener;
import com.ericsson.oss.services.ap.workflow.erbs.task.listener.NotificationsForStatus;

/**
 * Listener attached to the <b>end</b> event for the <code>S1 Not Needed</code> BPMN wait point.
 * <p>
 * Updates the existing status entry created by {@link S1CompleteOrNotNeededStartListener}, and notes the S1_NOT_NEEDED message in the additional
 * information.
 */
public class S1NotNeededEndListener extends TaskExecutionListener {

    private static final String ADDITIONAL_INFORMATION = "Received S1 Not Needed";

    @Override
    public void updateStatus(final String nodeFdn) {
        getStatusEntryManager().notificationReceived(nodeFdn, NotificationsForStatus.S1_COMPLETE_OR_NOT_NEEDED.toString(), ADDITIONAL_INFORMATION);
    }
}
