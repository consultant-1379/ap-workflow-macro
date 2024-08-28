/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.ejb;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.ap.api.cluster.APServiceClusterMember;

@Stateless
public class RbsConfigurationEventHandler {

    @EServiceRef
    private APServiceClusterMember apServiceClusterMember;

    @Inject
    private Logger logger;

    @Inject
    private RbsConfigurationEventProcessor rbsConfigurationEventProcessor;


    /**
     * Listens for RbsConfiguration MO changes.
     *
     * @param payload
     */
    @Asynchronous
    public void handleNotification(final DpsAttributeChangedEvent payload) {

        if (!apServiceClusterMember.isMasterNode()) {
            logger.debug("Ignoring RbsConfiguration notification, not master node");
            return;
        }

        try {
            rbsConfigurationEventProcessor.processNotification(payload);
        } catch (final Exception e) {
            logger.warn("Error processing notification -> {}", payload, e);
        }
    }
}
