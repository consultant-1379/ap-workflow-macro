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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsPersistenceException;
import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.common.util.log.DdpTimer;
import com.ericsson.oss.services.ap.common.workflow.recording.ErrorRecorder;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.RbsConfigLevelUpdater;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.wfs.api.WorkflowMessageCorrelationException;
import com.ericsson.oss.services.wfs.jee.api.WorkflowInstanceServiceLocal;

/**
 * Unit tests for {@link FeatureActivator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FeatureActivatorTest {

    @Mock
    private ErrorRecorder errorRecorder;

    @Mock
    private OptionalFeatureLicenseActivator optionalFeatureLicenseActivatorMock;

    @Mock
    private RbsConfigLevelUpdater rbsConfigLevelUpdater;

    @Mock
    private DdpTimer ddpTimer; // NOPMD

    @Mock
    private StatusEntryManagerLocal statusEntryManager; // NOPMD

    @Mock
    private WorkflowInstanceServiceLocal wfsInstanceService;

    @InjectMocks
    private FeatureActivator featureActivator;

    private static final String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String LICENSING_FDN = MECONTEXT_FDN + ",ManagedElement=1,SystemFunctions=1,Licensing=1";

    private FeatureActivationResult activationWithNoFeatures;
    private FeatureActivationResult activationWithTwoFailedFeatures;
    private FeatureActivationResult activationWithTwoSuccessfulFeatures;
    private FeatureActivationResult activationWithTwoSuccessfulTwoFailedFeatures;

    @Before
    public void setUp() {
        activationWithNoFeatures = new FeatureActivationResult();

        activationWithTwoFailedFeatures = new FeatureActivationResult();
        activationWithTwoFailedFeatures.incNumberOfFeatures();
        activationWithTwoFailedFeatures.incNumberOfFeatures();
        activationWithTwoFailedFeatures.featureActivationFailed(LICENSING_FDN, new Exception("ERROR1"));
        activationWithTwoFailedFeatures.featureActivationFailed(LICENSING_FDN, new Exception("ERROR2"));

        activationWithTwoSuccessfulFeatures = new FeatureActivationResult();
        activationWithTwoSuccessfulFeatures.incNumberOfFeatures();
        activationWithTwoSuccessfulFeatures.incNumberOfFeatures();

        activationWithTwoSuccessfulTwoFailedFeatures = new FeatureActivationResult();
        activationWithTwoSuccessfulTwoFailedFeatures.incNumberOfFeatures();
        activationWithTwoSuccessfulTwoFailedFeatures.incNumberOfFeatures();
        activationWithTwoSuccessfulTwoFailedFeatures.incNumberOfFeatures();
        activationWithTwoSuccessfulTwoFailedFeatures.incNumberOfFeatures();
        activationWithTwoSuccessfulTwoFailedFeatures.featureActivationFailed(LICENSING_FDN, new Exception("ERROR1"));
        activationWithTwoSuccessfulTwoFailedFeatures.featureActivationFailed(LICENSING_FDN, new Exception("ERROR2"));
    }

    @Test
    public void when_all_features_activate_then_RbsConfigLevel_set_to_FEATURE_ACTIVATED_and_workflow_is_resumed()
            throws WorkflowMessageCorrelationException {
        when(optionalFeatureLicenseActivatorMock.activateOptionalFeatures(MECONTEXT_FDN)).thenReturn(activationWithTwoSuccessfulFeatures);
        featureActivator.activateOptionalFeatures(NODE_FDN, MECONTEXT_FDN);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.FEATURES_ACTIVATED);
        verify(wfsInstanceService).correlateMessage("ACTIVATE_OPTIONAL_FEATURES_COMPLETION", "AP_Node=" + NODE_NAME);
    }

    @Test
    public void when_some_features_fail_to_activate_then_RbsConfigLevel_set_to_FEATURES_ACTIVATED_and_workflow_is_resumed_and_error_is_logged() {
        when(optionalFeatureLicenseActivatorMock.activateOptionalFeatures(MECONTEXT_FDN)).thenReturn(activationWithTwoSuccessfulTwoFailedFeatures);
        featureActivator.activateOptionalFeatures(NODE_FDN, MECONTEXT_FDN);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.FEATURES_ACTIVATED);
        verify(errorRecorder).activateOptionalFeaturesFailed(anyString(), any(Exception.class));
    }

    @Test
    public void when_all_features_fail_to_activate_then_RbsConfigLevel_set_to_FEATURE_ACTIVATION_FAILED() {
        when(optionalFeatureLicenseActivatorMock.activateOptionalFeatures(MECONTEXT_FDN)).thenReturn(activationWithTwoFailedFeatures);
        featureActivator.activateOptionalFeatures(NODE_FDN, MECONTEXT_FDN);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.ACTIVATING_FEATURES_FAILED);
    }

    @Test
    public void when_no_features_exist_then_rbs_config_level_set_to_FEATURE_ACTIVATION_FAILED() {
        when(optionalFeatureLicenseActivatorMock.activateOptionalFeatures(MECONTEXT_FDN)).thenReturn(activationWithNoFeatures);
        featureActivator.activateOptionalFeatures(NODE_FDN, MECONTEXT_FDN);
        verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.ACTIVATING_FEATURES_FAILED);
    }

    @Test
    public void when_update_RbsConfigLevel_fails_then_no_exception_is_propagated() {
        when(optionalFeatureLicenseActivatorMock.activateOptionalFeatures(MECONTEXT_FDN)).thenReturn(activationWithTwoSuccessfulFeatures);
        doThrow(DpsPersistenceException.class).when(rbsConfigLevelUpdater).updateRbsConfigLevel(anyString(), any(RbsConfigLevel.class));
        featureActivator.activateOptionalFeatures(NODE_FDN, MECONTEXT_FDN);
    }
}
