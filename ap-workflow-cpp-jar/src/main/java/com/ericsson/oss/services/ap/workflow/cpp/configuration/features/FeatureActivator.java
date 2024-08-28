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

import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.ACTIVATING_FEATURES;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.ACTIVATING_FEATURES_FAILED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.FEATURES_ACTIVATED;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.workflow.BusinessKeyGenerator;
import com.ericsson.oss.services.ap.common.workflow.recording.ErrorRecorder;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.RbsConfigLevelUpdater;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

/**
 * This class is responsible for activating <code>OptionalFeatureLicense</code> MOs, and updating the <i>rbsConfigLevel</i> attribute in the
 * <code>RbsConfiguration</code> MO.
 */
public class FeatureActivator {

    private static final String ACTIVATE_OPTIONAL_FEATURES_COMPLETION_CORRELATION_MESSAGE = "ACTIVATE_OPTIONAL_FEATURES_COMPLETION";
    private static final String FAILED_TO_SET_RBS_CONFIG_LEVEL_FOR_NODE = "Failed to set rbsConfigLevel for %s to %s. Reason: %s";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private StatusEntryManagerLocal statusEntryManager;

    private WorkflowInstanceServiceLocal wfsInstanceService;

    @Inject
    private DdpTimer ddpTimer;

    @Inject
    private ErrorRecorder errorRecorder;

    @Inject
    private OptionalFeatureLicenseActivator optionalFeatureLicenseActivator;

    @Inject
    private RbsConfigLevelUpdater rbsConfigLevelUpdater;

    @PostConstruct
    public void init() {
        statusEntryManager = new ServiceFinderBean().find(StatusEntryManagerLocal.class);
        wfsInstanceService = new ServiceFinderBean().find(WorkflowInstanceServiceLocal.class);
    }
    /**
     * Activates the optional feature MOs for the input node, and updates the <i>rbsConfigLevel</i> to the following states:
     * <ul>
     * <li>ACTIVATING_FEATURES - when activation has started</li>
     * <li>FEATURES ACTIVATED - if at least one MO was successfully activated</li>
     * <li>ACTIVATING_FEATURES_FAILED - if all MOs failed to activate</li>
     * </ul>
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param meContextFdn
     *            the MeContext FDN
     * @throws ApServiceException
     *             thrown if there is any error updating the <i>rbsConfigLevel</i> of the <code>RbsConfiguration</code> MO
     */
    public void activateOptionalFeatures(final String apNodeFdn, final String meContextFdn) {
        ddpTimer.start(CommandLogName.ACTIVATE_OPTIONAL_FEATURES.toString());
        statusEntryManager.taskStarted(apNodeFdn, StatusEntryNames.ACTIVATE_OPTIONAL_FEATURES.toString());

        try {
            activateFeatures(meContextFdn);
            statusEntryManager.taskCompleted(apNodeFdn, StatusEntryNames.ACTIVATE_OPTIONAL_FEATURES.toString());
            notifyWorkflowOfActivateFeaturesCompletion(apNodeFdn);
            ddpTimer.end(apNodeFdn);
        } catch (final Exception e) {
            errorRecorder.activateOptionalFeaturesFailed(apNodeFdn, e);
            statusEntryManager.taskFailed(apNodeFdn, StatusEntryNames.ACTIVATE_OPTIONAL_FEATURES.toString(), e.getMessage());
            notifyWorkflowOfActivateFeaturesCompletion(apNodeFdn);
            ddpTimer.endWithError(apNodeFdn);
        }
    }

    private void activateFeatures(final String meContextFdn) {
        setRbsConfigLevel(meContextFdn, ACTIVATING_FEATURES);

        final FeatureActivationResult featureActivationResult = optionalFeatureLicenseActivator.activateOptionalFeatures(meContextFdn);

        setRbsConfigLevelAfterActivatingFeatures(meContextFdn, featureActivationResult);

        if (!featureActivationResult.allFeaturesActivated()) {
            throw new ApServiceException(featureActivationResult.getErrorMessage());
        }
    }

    private void setRbsConfigLevelAfterActivatingFeatures(final String meContextFdn, final FeatureActivationResult featureActivationResult) {
        if (featureActivationResult.allFeaturesFailedToActivate()) {
            setRbsConfigLevel(meContextFdn, ACTIVATING_FEATURES_FAILED);
        } else {
            setRbsConfigLevel(meContextFdn, FEATURES_ACTIVATED);
        }
    }

    private void setRbsConfigLevel(final String meContextFdn, final RbsConfigLevel rbsConfigLevel) {
        try {
            rbsConfigLevelUpdater.updateRbsConfigLevel(meContextFdn, rbsConfigLevel);
        } catch (final Exception e) {
            final String message = String.format(FAILED_TO_SET_RBS_CONFIG_LEVEL_FOR_NODE, meContextFdn, rbsConfigLevel, e.getMessage());
            logger.error(message, e);
            throw new ApServiceException(message);
        }
    }

    private void notifyWorkflowOfActivateFeaturesCompletion(final String apNodeFdn) {
        logger.info("Notify workflow of activation optional features completion");

        try {
            final String businessKey = BusinessKeyGenerator.generateBusinessKeyFromFdn(apNodeFdn);
            wfsInstanceService.correlateMessage(ACTIVATE_OPTIONAL_FEATURES_COMPLETION_CORRELATION_MESSAGE, businessKey);
        } catch (final WorkflowMessageCorrelationException e) {
            logger.warn("Failed to correlate activation optional features message to workflow", e);
        }
    }
}
