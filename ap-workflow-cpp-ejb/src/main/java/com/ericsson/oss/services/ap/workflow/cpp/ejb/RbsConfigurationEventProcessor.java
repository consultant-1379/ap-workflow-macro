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

import static com.ericsson.oss.services.ap.common.model.Namespace.AP;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigurationAttribute.RBS_CONFIG_LEVEL;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.ap.common.cm.DpsQueries;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.common.workflow.BusinessKeyGenerator;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

@Singleton
public class RbsConfigurationEventProcessor {

    private static final String CHANGE_EVENT_DETAILS = "Attribute Name: %s, Old Value: %s, New Value: %s";
    private static final String DPS_NOTIFICATION_EVENT = "DPS Notification Event";
    private static final String DPS_SERVICE = "Data Persistence Service";

    @EServiceRef
    private WorkflowInstanceServiceLocal wfsInstanceService;

    @Inject
    private DpsQueries dpsQueries;

    @Inject
    private Logger logger;

    @Inject
    private SystemRecorder recorder;

    /**
     * Listens for RbsConfiguration MO changes.
     *
     * @param payload
     */
    public void processNotification(final DpsAttributeChangedEvent payload) {

        try {
            processRbsConfigNotification(payload);
        } catch (final Exception e) {
            logger.warn("Error processing notification -> {}", payload, e);
        }
    }

    private void processRbsConfigNotification(final DpsAttributeChangedEvent payload) {
        final String rbsConfigurationFdn = payload.getFdn();
        final String nodeName = FDN.get(rbsConfigurationFdn).getRdnValueOfType(MoType.MECONTEXT.toString());

        logger.debug("Processing notification -> {}", payload);
        final Set<AttributeChangeData> changeAttrSet = payload.getChangedAttributes();

        for (final AttributeChangeData changeAttr : changeAttrSet) {
            final String newAttrValue = String.valueOf(changeAttr.getNewValue());
            final String eventDetails = String.format(CHANGE_EVENT_DETAILS, changeAttr.getName(), changeAttr.getOldValue(), newAttrValue);
            logger.info(eventDetails);
            recorder.recordEvent(DPS_NOTIFICATION_EVENT, EventLevel.COARSE, DPS_SERVICE, rbsConfigurationFdn, eventDetails);

            if (isRbsConfigLevelChangeCorrelatedWithWorkflow(changeAttr)) {
                resumeWorkflowIfInApModel(nodeName, newAttrValue);
            }
        }
    }

    private static boolean isRbsConfigLevelChangeCorrelatedWithWorkflow(final AttributeChangeData changeAttr) {
        return RBS_CONFIG_LEVEL.toString().equals(changeAttr.getName())
                && RbsConfigLevelsForCorrelation.isForCorrelation(changeAttr.getNewValue().toString());
    }

    private void resumeWorkflowIfInApModel(final String nodeName, final String newAttrValue) {
        if (!isNodeInApModel(nodeName)) {
            logger.debug("Ignoring RbsConfiguration notification, node {} is not in AP model", nodeName);
            return;
        }
        resumeWorkflow(nodeName, newAttrValue);
    }

    private boolean isNodeInApModel(final String nodeName) {
        final long nodeCount = dpsQueries.findMoByName(nodeName, MoType.NODE.toString(), AP.toString()).executeCount();
        return nodeCount > 0;
    }

    private void resumeWorkflow(final String nodeName, final String newAttrValue) {
        final String message = getMessageToCorrelate(newAttrValue);
        final String businessKey = BusinessKeyGenerator.generateBusinessKeyFromNodeName(nodeName);
        final Map<String, Object> additionalWorkflowVariables = getWorkflowVariables(newAttrValue);

        try {
            wfsInstanceService.correlateMessage(message, businessKey, additionalWorkflowVariables);
        } catch (final WorkflowMessageCorrelationException e) {
            logger.warn("Failed to correlate {} message to workflow", message, e);
        }
    }

    private static String getMessageToCorrelate(final String newAttrValue) {
        if (RbsConfigLevelsForCorrelation.isForGpsPositionCheck(newAttrValue)) {
            return "GPS_POSITION_CHECK_COMPLETE";
        }

        return newAttrValue;
    }

    private static Map<String, Object> getWorkflowVariables(final String newAttrValue) {
        final Map<String, Object> workflowVariables = new HashMap<>();

        if (RbsConfigLevelsForCorrelation.isForGpsPositionCheck(newAttrValue)) {
            workflowVariables.put("gps_check_result", newAttrValue);
        }

        return workflowVariables;
    }
}
