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

import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_MISMATCH_ERROR;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_POSITION_UNAVAILABLE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_SUCCESSFULLY_MATCHED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_WANTED_POSITION_NOT_SET;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.S1_COMPLETE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.S1_NOT_NEEDED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.SITE_CONFIG_COMPLETE;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains RbsConfigLevel values used by RbsConfigurationEventConsumer to correlate message to workflow service instance.
 */
public final class RbsConfigLevelsForCorrelation {

    private static final Set<String> CORRELATION_MESSAGES = new HashSet<>();
    private static final Set<String> GPS_CHECK_POSITION_RESULTS = new HashSet<>();
    private static final Set<String> S1_STATE_VALUES = new HashSet<>();

    static {
        CORRELATION_MESSAGES.add(SITE_CONFIG_COMPLETE.toString());
        CORRELATION_MESSAGES.add(S1_COMPLETE.toString());
        CORRELATION_MESSAGES.add(S1_NOT_NEEDED.toString());
        CORRELATION_MESSAGES.add(GPS_SUCCESSFULLY_MATCHED.toString());
        CORRELATION_MESSAGES.add(GPS_WANTED_POSITION_NOT_SET.toString());
        CORRELATION_MESSAGES.add(GPS_MISMATCH_ERROR.toString());
        CORRELATION_MESSAGES.add(GPS_POSITION_UNAVAILABLE.toString());

        S1_STATE_VALUES.add(S1_COMPLETE.toString());
        S1_STATE_VALUES.add(S1_NOT_NEEDED.toString());

        GPS_CHECK_POSITION_RESULTS.add(GPS_SUCCESSFULLY_MATCHED.toString());
        GPS_CHECK_POSITION_RESULTS.add(GPS_WANTED_POSITION_NOT_SET.toString());
        GPS_CHECK_POSITION_RESULTS.add(GPS_MISMATCH_ERROR.toString());
        GPS_CHECK_POSITION_RESULTS.add(GPS_POSITION_UNAVAILABLE.toString());
    }

    private RbsConfigLevelsForCorrelation() {

    }

    /**
     * Check whether rbsConfigLevel value is for correlating message.
     *
     * @param rbsConfigLevel
     *            the rbsConfigLevel to check
     * @return true if rbsConfigLevel value is for correlating message
     */
    public static boolean isForCorrelation(final String rbsConfigLevel) {
        return CORRELATION_MESSAGES.contains(rbsConfigLevel);
    }

    /**
     * Check whether rbsConfigLevel value is S1 state message.
     *
     * @param rbsConfigLevel
     *            the rbsConfigLevel to check
     * @return true if rbsConfigLevel value is S1 state message
     */
    public static boolean isForS1(final String rbsConfigLevel) {
        return S1_STATE_VALUES.contains(rbsConfigLevel);
    }

    /**
     * Check whether rbsConfigLevel value is gps check position result
     *
     * @param rbsConfigLevel
     *            the rbsConfigLevel to check
     * @return true if rbsConfigLevel value is gps check position result
     */
    public static boolean isForGpsPositionCheck(final String rbsConfigLevel) {
        return GPS_CHECK_POSITION_RESULTS.contains(rbsConfigLevel);
    }
}
