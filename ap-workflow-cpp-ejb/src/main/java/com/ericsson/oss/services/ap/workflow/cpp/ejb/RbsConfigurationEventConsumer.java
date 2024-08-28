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
package com.ericsson.oss.services.ap.workflow.cpp.ejb;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.notification.DpsNotificationConfiguration;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;


/**
 * Handles notifications for RbsConfiguration MOs. Resumes the workflow for a node in cases where the node is being integrated and the value of the
 * rbsConfigLevel attribute affects the workflow.
 */
@Singleton
@LocalBean
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class RbsConfigurationEventConsumer {

    @Inject
    private Logger logger;

    @Inject
    private RbsConfigurationEventHandler rbsConfigurationEventHandler;

    /**
     * Listens for RbsConfiguration MO changes.
     *
     * @param payload
     */
    public void listenForRbsConfigurationNotification(@Observes @Consumes(endpoint = DpsNotificationConfiguration.DPS_EVENT_NOTIFICATION_CHANNEL_URI, filter = "type='RbsConfiguration'") final DpsAttributeChangedEvent payload) {

        try {
            rbsConfigurationEventHandler.handleNotification(payload);
        } catch (final Exception e) {
            logger.warn("Error processing notification -> {}", payload, e);
    	}
    }
}
