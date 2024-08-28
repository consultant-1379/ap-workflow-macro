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

import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.common.workflow.task.listener.TaskExecutionListener;

/**
 * Listener attached to the <b>start</b> event for the <code>Site Config Complete</code> BPMN wait point.
 * <p>
 * Creates a new status entry on the AP node.
 */
public class SiteConfigCompleteStartListener extends TaskExecutionListener {

    @Override
    public void updateStatus(final String nodeFdn) {
        getStatusEntryManager().waitingForNotification(nodeFdn, StatusEntryNames.SITE_CONFIG_COMPLETE.toString());
    }
}
