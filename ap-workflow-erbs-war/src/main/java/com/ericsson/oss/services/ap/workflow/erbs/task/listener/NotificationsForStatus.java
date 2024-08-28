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
package com.ericsson.oss.services.ap.workflow.erbs.task.listener;

/**
 * Status entry text for notification wait points in a BPMN workflow.
 */
public enum NotificationsForStatus {

    S1_COMPLETE_OR_NOT_NEEDED("S1 Complete or S1 Not Needed Notification");

    private String notificationEntry;

    private NotificationsForStatus(final String notificationEntry) {
        this.notificationEntry = notificationEntry;
    }

    @Override
    public String toString() {
        return notificationEntry;
    }
}
