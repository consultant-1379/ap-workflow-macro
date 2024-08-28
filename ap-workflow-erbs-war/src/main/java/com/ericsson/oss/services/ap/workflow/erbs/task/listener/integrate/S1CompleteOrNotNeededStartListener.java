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
 * Listener attached to the <b>start</b> event for the <code>S1 Complete/Not Needed</code> BPMN wait point.
 * <p>
 * Creates a new status entry on the AP node.
 * <p>
 * <b>Note:</b> Either {@link S1CompleteEndListener} or {@link S1NotNeededEndListener} will update the status entry.
 */
public class S1CompleteOrNotNeededStartListener extends TaskExecutionListener {

    @Override
    public void updateStatus(final String nodeFdn) {
        getStatusEntryManager().waitingForNotification(nodeFdn, NotificationsForStatus.S1_COMPLETE_OR_NOT_NEEDED.toString());
    }
}
