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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static com.ericsson.oss.services.ap.workflow.cpp.model.MoType.OPTIONALFEATURELICENSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsPersistenceException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.workflow.cpp.api.FeaturesRetriever;
import com.ericsson.oss.services.ap.workflow.cpp.api.OptionalFeatureLicense;

/**
 * Unit tests for {@link OptionalFeatureLicenseActivator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OptionalFeatureLicenseActivatorTest {

    private static final String ME_CONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String LICENSING_FDN = ME_CONTEXT_FDN + ",ManagedElement=1,SystemFunctions=1,Licensing=1";

    @Mock
    private DpsOperations dpsOperations;

    @Mock
    private FeaturesRetriever featuresRetriever;

    @InjectMocks
    private OptionalFeatureLicenseActivator optionalFeatureLicenseActivator;

    @Test
    public void when_all_features_successfully_activated_then_result_returns_no_failed_activations() {
        final List<OptionalFeatureLicense> optionalFeatureLicenseMos = new ArrayList<>();
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("1", "ENABLED"));
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("2", "ENABLED"));
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("3", "ENABLED"));
        when(featuresRetriever.getOptionalFeatureLicenseMos(ME_CONTEXT_FDN)).thenReturn(optionalFeatureLicenseMos);

        final FeatureActivationResult featureActivationResult = optionalFeatureLicenseActivator.activateOptionalFeatures(ME_CONTEXT_FDN);

        assertTrue("Result had at least 1 feature fail to activate", featureActivationResult.allFeaturesActivated());
        verify(dpsOperations).updateMo(optionalFeatureLicenseMos.get(0).getFdn(), "featureState", "ACTIVATED");
        verify(dpsOperations).updateMo(optionalFeatureLicenseMos.get(1).getFdn(), "featureState", "ACTIVATED");
        verify(dpsOperations).updateMo(optionalFeatureLicenseMos.get(2).getFdn(), "featureState", "ACTIVATED");
    }

    @Test
    public void when_fail_to_activate_all_features_then_result_returns_all_failed_activations() {
        final List<OptionalFeatureLicense> optionalFeatureLicenseMos = new ArrayList<>();
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("1", "ENABLED"));
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("2", "ENABLED"));
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("3", "ENABLED"));
        when(featuresRetriever.getOptionalFeatureLicenseMos(ME_CONTEXT_FDN)).thenReturn(optionalFeatureLicenseMos);
        doThrow(DpsPersistenceException.class).when(dpsOperations).updateMo(anyString(), anyString(), anyString());

        final FeatureActivationResult featureActivationResult = optionalFeatureLicenseActivator.activateOptionalFeatures(ME_CONTEXT_FDN);

        assertTrue("Result did not return all features failing to activate", featureActivationResult.allFeaturesFailedToActivate());
        assertEquals(3, featureActivationResult.getNumberOfFailedFeatureActivations());
    }

    @Test
    public void when_partial_success_activating_features_then_result_returns_all_successful_and_failed_activations() {
        final List<OptionalFeatureLicense> optionalFeatureLicenseMos = new ArrayList<>();
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("1", "ENABLED"));
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("2", "ENABLED"));
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("3", "ENABLED"));
        when(featuresRetriever.getOptionalFeatureLicenseMos(ME_CONTEXT_FDN)).thenReturn(optionalFeatureLicenseMos);
        final String fistLicenseMoFdn = optionalFeatureLicenseMos.get(0).getFdn();
        doThrow(DpsPersistenceException.class).when(dpsOperations).updateMo(eq(fistLicenseMoFdn), anyString(), anyString());

        final FeatureActivationResult featureActivationResult = optionalFeatureLicenseActivator.activateOptionalFeatures(ME_CONTEXT_FDN);

        assertFalse("Result returned all features failing to activate", featureActivationResult.allFeaturesFailedToActivate());
        assertEquals(1, featureActivationResult.getNumberOfFailedFeatureActivations());
    }

    @Test
    public void when_OptionalFeatureLicense_mo_has_licenseState_disabled_then_there_is_no_attempt_to_active_the_feature() {
        final List<OptionalFeatureLicense> optionalFeatureLicenseMos = new ArrayList<>();
        optionalFeatureLicenseMos.add(createMockOptionalFeatureLicenseMo("1", "DISABLED"));
        when(featuresRetriever.getOptionalFeatureLicenseMos(ME_CONTEXT_FDN)).thenReturn(optionalFeatureLicenseMos);

        optionalFeatureLicenseActivator.activateOptionalFeatures(ME_CONTEXT_FDN);

        verify(dpsOperations, never()).updateMo(anyString(), anyString(), anyString());
    }

    private OptionalFeatureLicense createMockOptionalFeatureLicenseMo(final String featureMoRdn, final String licenseState) {
        final String featureMoFdn = String.format("%s,%s=%s", LICENSING_FDN, OPTIONALFEATURELICENSE.toString(), featureMoRdn);
        return new OptionalFeatureLicense(featureMoFdn, licenseState);
    }
}
