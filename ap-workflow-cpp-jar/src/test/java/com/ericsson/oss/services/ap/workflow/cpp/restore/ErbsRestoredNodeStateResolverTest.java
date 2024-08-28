/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.restore;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.api.status.State;
import com.ericsson.oss.services.ap.common.cm.DpsQueries;
import com.ericsson.oss.services.ap.common.cm.DpsQueries.DpsQueryExecutor;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigurationAttribute;

/**
 * Unit tests for {@link ErbsRestoredNodeStateResolver}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ErbsRestoredNodeStateResolverTest {

    private static final String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String NETWORK_ELEMENT_FDN = "NetworkElement=" + NODE_NAME;

    @Mock
    private DataPersistenceService dps;

    @Mock
    private DataBucket liveBucket;

    @Mock
    private DpsQueries dpsQueries;

    @Mock
    private DpsQueryExecutor<ManagedObject> dpsQueryExecutor;

    @Mock
    private ManagedObject rbsConfigMo;

    @InjectMocks
    private ErbsRestoredNodeStateResolver erbsRestoredNodeStateResolver;

    @Before
    public void setUp() {
        final List<ManagedObject> meContextMos = new ArrayList<>();
        final ManagedObject meContextMo = Mockito.mock(ManagedObject.class);
        meContextMos.add(meContextMo);

        when(dpsQueries.findMoByName(NODE_NAME, "MeContext", "OSS_TOP")).thenReturn(dpsQueryExecutor);
        when(dpsQueryExecutor.execute()).thenReturn(meContextMos.iterator());
        when(meContextMo.getFdn()).thenReturn(MECONTEXT_FDN);

        when(dps.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(MECONTEXT_FDN + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1")).thenReturn(rbsConfigMo);
    }

    @Test
    public void when_rbsConfig_is_OSS_CONFIGURATION_FAILED_then_state_is_INTEGRATION_FAILED() {
        when(rbsConfigMo.getAttribute(RbsConfigurationAttribute.RBS_CONFIG_LEVEL.toString()))
                .thenReturn(RbsConfigLevel.OSS_CONFIGURATION_FAILED.toString());
        final State actualState = erbsRestoredNodeStateResolver.resolveNodeState(NETWORK_ELEMENT_FDN);

        assertEquals(State.INTEGRATION_FAILED, actualState);
    }

    @Test
    public void when_rbsConfig_is_INTEGRATION_COMPLETE_then_state_is_INTEGRATION_FAILED() {
        when(rbsConfigMo.getAttribute(RbsConfigurationAttribute.RBS_CONFIG_LEVEL.toString()))
                .thenReturn(RbsConfigLevel.INTEGRATION_COMPLETE.toString());
        final State actualState = erbsRestoredNodeStateResolver.resolveNodeState(NETWORK_ELEMENT_FDN);

        assertEquals(State.INTEGRATION_COMPLETED, actualState);
    }

    @Test
    public void when_rbsConfig_is_not_mapped_then_state_is_UNKNOWN() {
        when(rbsConfigMo.getAttribute(RbsConfigurationAttribute.RBS_CONFIG_LEVEL.toString())).thenReturn("unMappedRbsConfigLevel");
        final State actualState = erbsRestoredNodeStateResolver.resolveNodeState(NETWORK_ELEMENT_FDN);
        assertEquals(State.UNKNOWN, actualState);
    }

    @Test
    public void when_rbsConfig_mo_does_not_exist_then_state_is_UNKNOWN() {
        when(liveBucket.findMoByFdn(MECONTEXT_FDN + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1")).thenReturn(null);
        final State actualState = erbsRestoredNodeStateResolver.resolveNodeState(NETWORK_ELEMENT_FDN);
        assertEquals(State.UNKNOWN, actualState);
    }
}
