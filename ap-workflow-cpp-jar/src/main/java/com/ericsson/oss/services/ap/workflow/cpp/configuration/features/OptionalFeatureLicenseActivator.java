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

import static com.ericsson.oss.services.ap.workflow.cpp.model.OptionalFeatureLicenseAttributes.FEATURE_STATE;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.workflow.cpp.api.FeaturesRetriever;
import com.ericsson.oss.services.ap.workflow.cpp.api.OptionalFeatureLicense;

/**
 * Class responsible for activating any <code>OptionalFeatureLicense</code> MOs whose <i>licenseState</i> attribute has been set to <b>ENABLED</b>.
 * <p>
 * These features are found as child MOs of the ManagedElement,SystemFunctions,Licensing MO.
 * <p>
 * Features are activated by setting the <i>featureState</i> attribute to <b>ACTIVATED</b>.
 */
public class OptionalFeatureLicenseActivator {

    private static final String FEATURE_STATE_ACTIVATED_VALUE = "ACTIVATED";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private FeaturesRetriever featuresRetriever;

    @Inject
    private DpsOperations dpsOperations;

    @PostConstruct
    public void init() {
        featuresRetriever = new ServiceFinderBean().find(FeaturesRetriever.class);
    }

    /**
     * Activates all OptionalFeatureLicense MOs, by setting the <code>featureState</code> attribute to <b>ACTIVATED</b>.
     * <p>
     * Will only attempt to activate MOs whose <code>licenseState</code> attribute value is <b>ENABLED</b>.
     *
     * @param meContextFdn
     *            the FDN of the MeContext of the node being integrated
     * @return a {@link FeatureActivationResult} object containing the results of the feature activation
     */
    public FeatureActivationResult activateOptionalFeatures(final String meContextFdn) {
        final List<OptionalFeatureLicense> optionalFeatureLicenseMos = featuresRetriever.getOptionalFeatureLicenseMos(meContextFdn);
        final FeatureActivationResult result = new FeatureActivationResult();

        for (final OptionalFeatureLicense optionalFeatureLicenseMo : optionalFeatureLicenseMos) {
            result.incNumberOfFeatures();

            try {
                activateIndividualOptionalFeatureLicense(optionalFeatureLicenseMo);
            } catch (final Exception e) {
                logger.error("Activation of {} failed", optionalFeatureLicenseMo.getFdn(), e);
                result.featureActivationFailed(optionalFeatureLicenseMo.getFdn(), e);
            }
        }
        return result;
    }

    private void activateIndividualOptionalFeatureLicense(final OptionalFeatureLicense optionalFeatureLicenseMo) {
        if (optionalFeatureLicenseMo.isLicenseStateEnabled()) {
            activateFeatureInNewTx(optionalFeatureLicenseMo.getFdn(), FEATURE_STATE.toString());
        }
    }

    private void activateFeatureInNewTx(final String optionalFeatureFdn, final String featureStateAttributeName) {
        logger.debug("Executing activation of optional features on {}", featureStateAttributeName);
        dpsOperations.updateMo(optionalFeatureFdn, featureStateAttributeName, FEATURE_STATE_ACTIVATED_VALUE);
    }
}
