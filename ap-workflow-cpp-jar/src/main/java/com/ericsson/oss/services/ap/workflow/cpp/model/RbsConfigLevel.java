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
package com.ericsson.oss.services.ap.workflow.cpp.model;

/**
 * An RbsConfigLevel value for ERBS nodes.
 */
public enum RbsConfigLevel {

    ACTIVATING_FEATURES,
    ACTIVATING_FEATURES_FAILED,
    CELLS_UNLOCKED,
    FEATURES_ACTIVATED,
    GPS_CHECK_POSITION,
    GPS_MISMATCH_ERROR,
    GPS_POSITION_UNAVAILABLE,
    GPS_SUCCESSFULLY_MATCHED,
    GPS_WANTED_POSITION_NOT_SET,
    INTEGRATION_COMPLETE,
    INTEGRATION_FAILED,
    OSS_ACTIVATING_CONFIGURATION,
    OSS_CONFIGURATION_SUCCESSFUL,
    OSS_CONFIGURATION_FAILED,
    S1_COMPLETE,
    S1_NOT_NEEDED,
    SITE_CONFIG_COMPLETE,
    UNLOCKING_CELLS,
    UNLOCKING_CELLS_FAILED;
}
