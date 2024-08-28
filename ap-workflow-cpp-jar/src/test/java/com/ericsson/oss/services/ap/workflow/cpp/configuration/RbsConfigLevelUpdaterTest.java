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
package com.ericsson.oss.services.ap.workflow.cpp.configuration;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsPersistenceException;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigurationAttribute;

/**
 * Unit tests for {@link RbsConfigLevelUpdater}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RbsConfigLevelUpdaterTest {

    private static final String ME_CONTEXT_FDN = MoType.MECONTEXT + "=" + NODE_NAME;
    private static final String RBS_CONFIG_FDN = ME_CONTEXT_FDN + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1";
    private static final int MAX_RETRIES = 5;

    @Mock
    private DpsOperations dpsOperations;

    @InjectMocks
    private RbsConfigLevelUpdater rbsConfigLevelUpdator;

    @Test
    public void when_update_rbsConfigLevel_then_attribute_is_successfully_updated() {
        rbsConfigLevelUpdator.updateRbsConfigLevel(ME_CONTEXT_FDN, RbsConfigLevel.INTEGRATION_COMPLETE);
        verify(dpsOperations, times(1)).updateMoWithFailure(RBS_CONFIG_FDN, RbsConfigurationAttribute.RBS_CONFIG_LEVEL.toString(),
                RbsConfigLevel.INTEGRATION_COMPLETE.name());
    }

    @Test(expected = RetriableCommandException.class)
    public void when_update_rbsConfigLevel_throws_exception_then_retry_five_times_before_failing() {
        doThrow(DpsPersistenceException.class).when(dpsOperations).updateMoWithFailure(RBS_CONFIG_FDN,
                RbsConfigurationAttribute.RBS_CONFIG_LEVEL.toString(), RbsConfigLevel.INTEGRATION_COMPLETE.name());

        rbsConfigLevelUpdator.updateRbsConfigLevel(ME_CONTEXT_FDN, RbsConfigLevel.INTEGRATION_COMPLETE);

        verify(dpsOperations, times(MAX_RETRIES)).updateMoWithFailure(RBS_CONFIG_FDN, RbsConfigurationAttribute.RBS_CONFIG_LEVEL.toString(),
                RbsConfigLevel.INTEGRATION_COMPLETE.name());
    }
}
