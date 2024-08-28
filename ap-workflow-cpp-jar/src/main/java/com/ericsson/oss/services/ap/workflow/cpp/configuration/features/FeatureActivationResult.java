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
package com.ericsson.oss.services.ap.workflow.cpp.configuration.features;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.ericsson.oss.services.ap.common.util.string.FDN;

/**
 * Data class containing the results of the Activating Optional Features operation
 */
class FeatureActivationResult {

    private static final String NO_FEATURES_TO_ACTIVATE = "No Optional Feature(s) available to be activated.";
    private static final String FEATURES_ACTIVATION_ALL_FAILED = "Failed to activate all feature(s). %s";
    private static final String FAILED_TO_ACTIVATE_FEATURE = "Failed to Activate the following feature : %s. Reason: %s";
    private static final String FEATURES_ACTIVATION_FAILED = "Failed to activate %s of %s feature(s). %s";

    private final StringBuilder errorMessages = new StringBuilder();

    private int totalNumberOfFeatures;
    private int failedActivations;

    boolean allFeaturesActivated() {
        return totalNumberOfFeatures > 0 && failedActivations == 0;
    }

    public void incNumberOfFeatures() {
        ++totalNumberOfFeatures;
    }

    int getNumberOfFailedFeatureActivations() {
        return failedActivations;
    }

    boolean allFeaturesFailedToActivate() {
        return totalNumberOfFeatures == failedActivations;
    }

    void featureActivationFailed(final String fdn, final Exception e) {
        ++failedActivations;
        final String errorMessage = getErrorRootCauseMessage(fdn, e);
        errorMessages.append('\n').append(errorMessage);
    }

    private static String getErrorRootCauseMessage(final String fdn, final Exception e) {
        final String rootCause;
        if (ExceptionUtils.getRootCause(e) == null) {
            rootCause = e.getMessage();
        } else {
            rootCause = ExceptionUtils.getRootCauseMessage(e);
        }
        return String.format(FAILED_TO_ACTIVATE_FEATURE, FDN.get(fdn).getRdn(), rootCause);
    }

    String getErrorMessage() {
        if (totalNumberOfFeatures == 0) {
            return NO_FEATURES_TO_ACTIVATE;
        } else if (allFeaturesFailedToActivate()) {
            return String.format(FEATURES_ACTIVATION_ALL_FAILED, errorMessages);
        } else {
            return String.format(FEATURES_ACTIVATION_FAILED, failedActivations, totalNumberOfFeatures, errorMessages);
        }
    }
}
