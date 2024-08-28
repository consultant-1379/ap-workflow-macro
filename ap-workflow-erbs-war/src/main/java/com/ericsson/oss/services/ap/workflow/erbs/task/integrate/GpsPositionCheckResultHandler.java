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

import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_MISMATCH_ERROR;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_POSITION_UNAVAILABLE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_SUCCESSFULLY_MATCHED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_WANTED_POSITION_NOT_SET;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;

/**
 * Class that handles the result from {@link InitiateGPSPositionCheckTask}.
 * <ul>
 * <li>Updates the workflow variable <code>gpsPositionCheckFailed</code> to <i>true</i> if result is failure</li>
 * <li>Updates the status entry created by {@link InitiateGPSPositionCheckTask}</li>
 * </ul>
 */
public class GpsPositionCheckResultHandler {

    private static final String ADDITIONAL_INFO_GPS_SUCCESSFULLY_MATCHED = "GPS successfully matched";
    private static final String ADDITIONAL_INFO_GPS_WANTED_POSITION_NOT_SET = "Latitude, longitude and altitude values not set in SiteBasic file";
    private static final String ADDITIONAL_INFO_GPS_UNAVAILABLE = "GPS service not available, not possible to determine GPS position - ENM alarm raised";
    private static final String ADDITIONAL_INFO_GPS_MISMATCH = "Latitude, longitude and altitude values in SiteBasic file do not match GPS position - ENM alarm raised";

    private static final Map<String, String> SUCCESSFUL_RESULTS_STATUS_TEXT = new HashMap<>(4);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private StatusEntryManagerLocal statusEntryManager;

    static {
        SUCCESSFUL_RESULTS_STATUS_TEXT.put(GPS_SUCCESSFULLY_MATCHED.toString(), ADDITIONAL_INFO_GPS_SUCCESSFULLY_MATCHED);
        SUCCESSFUL_RESULTS_STATUS_TEXT.put(GPS_WANTED_POSITION_NOT_SET.toString(), ADDITIONAL_INFO_GPS_WANTED_POSITION_NOT_SET);
    }

    public void handleResult(final String messageReceived, final ErbsWorkflowVariables workflowVariables) {
        final String nodeFdn = workflowVariables.getApNodeFdn();

        if (isGpsCheckFailure(messageReceived)) {
            updateWorkflowVariables(workflowVariables);
            updateStatusFailure(nodeFdn, messageReceived);
        } else {
            updateStatusSuccess(nodeFdn, messageReceived);
        }
    }

    private static boolean isGpsCheckFailure(final String messageReceived) {
        return GPS_MISMATCH_ERROR.toString().equalsIgnoreCase(messageReceived)
                || GPS_POSITION_UNAVAILABLE.toString().equalsIgnoreCase(messageReceived);
    }

    private static void updateWorkflowVariables(final ErbsWorkflowVariables workflowVariables) {
        workflowVariables.setGpsPositionCheckFailed(true);
    }

    private void updateStatusFailure(final String nodeFdn, final String messageReceived) {
        final String additionalInfo = getFailureAdditionalInfo(messageReceived);
        getStatusEntryManager().taskFailed(nodeFdn, StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), additionalInfo);
        logger.error("Error on GPS Check Position for node {}: {}", nodeFdn, additionalInfo);
    }

    private static String getFailureAdditionalInfo(final String messageReceived) {
        return isGpsMismatchError(messageReceived) ? ADDITIONAL_INFO_GPS_MISMATCH : ADDITIONAL_INFO_GPS_UNAVAILABLE;
    }

    private void updateStatusSuccess(final String nodeFdn, final String messageReceived) {
        final String additionalInfo = SUCCESSFUL_RESULTS_STATUS_TEXT.get(messageReceived);
        getStatusEntryManager().taskCompleted(nodeFdn, StatusEntryNames.GPS_POSITION_CHECK_TASK.toString(), additionalInfo);
        if (RbsConfigLevel.GPS_WANTED_POSITION_NOT_SET.toString().equalsIgnoreCase(messageReceived)) {
            logger.warn("Node {} received GPS Check Position result: {}", nodeFdn, additionalInfo);
        }
    }

    private static boolean isGpsMismatchError(final String messageReceived) {
        return GPS_MISMATCH_ERROR.toString().equalsIgnoreCase(messageReceived);
    }

    private StatusEntryManagerLocal getStatusEntryManager() {
        if (statusEntryManager == null) {
            final ServiceFinderBean serviceFinder = new ServiceFinderBean();
            statusEntryManager = serviceFinder.find(StatusEntryManagerLocal.class);
        }
        return statusEntryManager;
    }
}
