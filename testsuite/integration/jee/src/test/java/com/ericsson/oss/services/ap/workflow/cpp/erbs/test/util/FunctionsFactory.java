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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util;

import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_MISMATCH_ERROR;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_POSITION_UNAVAILABLE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_SUCCESSFULLY_MATCHED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.GPS_WANTED_POSITION_NOT_SET;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.S1_COMPLETE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.S1_NOT_NEEDED;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.ap.api.status.StatusEntryProgress;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.Jndi;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.NodeStatusEntriesListener;
import com.ericsson.oss.services.ap.arquillian.util.data.workflow.WorkflowFunctionsFactory;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

/**
 * A factory for {@link Function} instances to be used along with {@link NodeStatusEntriesListener#onStart(String, Function)} method.
 */
public class FunctionsFactory {

    private FunctionsFactory() {

    }

    public static Function<String, Void> newNodeUpFunction(final Jndi jndi) {
        final Map<String, Object> workflowVariables = new HashMap<>();
        workflowVariables.put("NodeUpNotification", StatusEntryProgress.RECEIVED.toString());
        return WorkflowFunctionsFactory.newCorrelateMessageFunction(jndi, "NODE_UP", workflowVariables);
    }

    public static Function<String, Void> newCorrelateBindMessageFunction(final Jndi jndi, final String hardwareSerialNumber) {
        return WorkflowFunctionsFactory.newCorrelateMessageFunction(jndi, "BIND",
                ImmutableMap.<String, Object> of("hardwareSerialNumber", hardwareSerialNumber));
    }

    public static Function<String, Void> newSiteConfigCompleteFunction(final Jndi jndi) {
        return WorkflowFunctionsFactory.newCorrelateMessageFunction(jndi, "SITE_CONFIG_COMPLETE");
    }

    public static Function<String, Void> newNodeStateIntegrationStartedFunction(final Dps dps) {
        return new UpdateNodeStateFunction(dps, "INTEGRATION_STARTED");
    }

    public static Function<String, Void> newNodeStateBindFunction(final Dps dps) {
        return new UpdateNodeStateFunction(dps, "BIND_STARTED");
    }

    public static Function<String, Void> newCreateNetworkElementChildMosFunction(final WorkflowDataSteps steps) {
        return new Function<String, Void>() {

            @Override
            public Void apply(final String nodeFdn) {
                final String nodeName = FDN.get(nodeFdn).getRdnValue();
                steps.create_network_element_child_mos(nodeName);
                return null;
            }
        };
    }

    public static Function<String, Void> newCreateErbsNodeMosFunction(final WorkflowDataSteps steps) {
        return new Function<String, Void>() {

            @Override
            public Void apply(final String nodeFdn) {
                final String nodeName = FDN.get(nodeFdn).getRdnValue();
                steps.create_me_context_and_all_erbs_node_mos(nodeName);
                return null;
            }
        };
    }

    public static Function<String, Void> newCreateErbsNodeMosFunctionWithSubNetworks(final WorkflowDataSteps steps) {
        return new Function<String, Void>() {

            @Override
            public Void apply(final String nodeFdn) {
                final String nodeName = FDN.get(nodeFdn).getRdnValue();
                steps.create_me_context_and_all_erbs_node_mos_with_subnetworks(nodeName);
                return null;
            }
        };
    }

    public static Function<String, Void> newCreateErbsNodeMosFunctionWithSync(final WorkflowDataSteps steps) {
        return new Function<String, Void>() {

            @Override
            public Void apply(final String nodeFdn) {
                final String nodeName = FDN.get(nodeFdn).getRdnValue();
                steps.create_all_erbs_node_mos_with_sync(nodeName);
                return null;
            }
        };
    }

    public static Function<String, Void> newCreateErbsNodeMosWithoutHearbeatSupervisionFunction(final WorkflowDataSteps steps) {
        return new Function<String, Void>() {

            @Override
            public Void apply(final String nodeFdn) {
                final String nodeName = FDN.get(nodeFdn).getRdnValue();
                steps.create_all_erbs_node_mos_expect_heartbeat_supervision(nodeName);
                return null;
            }
        };
    }

    public static Function<String, Void> newS1CompleteFunction(final Dps dps) {
        return new RbsConfigLevelFunction(dps, S1_COMPLETE, false);
    }

    public static Function<String, Void> newS1CompleteFunctionWithSubNetworks(final Dps dps) {
        return new RbsConfigLevelFunction(dps, S1_COMPLETE, true);
    }

    public static Function<String, Void> newS1NotNeededFunction(final Dps dps) {
        return new RbsConfigLevelFunction(dps, S1_NOT_NEEDED, false);
    }

    public static Function<String, Void> newGpsPositionSuccessfullyMatchedFunction(final Dps dps) {
        return new RbsConfigLevelFunction(dps, GPS_SUCCESSFULLY_MATCHED, false);
    }

    public static Function<String, Void> newGpsPositionSuccessfullyMatchedFunctionWithSubNetworks(final Dps dps) {
        return new RbsConfigLevelFunction(dps, GPS_SUCCESSFULLY_MATCHED, true);
    }

    public static Function<String, Void> newGpsPositionMismatchErrorFunction(final Dps dps) {
        return new RbsConfigLevelFunction(dps, GPS_MISMATCH_ERROR, false);
    }

    public static Function<String, Void> newGpsPositionUnavailableFunction(final Dps dps) {
        return new RbsConfigLevelFunction(dps, GPS_POSITION_UNAVAILABLE, false);
    }

    public static Function<String, Void> newGpsWantedPositionNotSetFunction(final Dps dps) {
        return new RbsConfigLevelFunction(dps, GPS_WANTED_POSITION_NOT_SET, false);
    }

    private static class RbsConfigLevelFunction implements Function<String, Void> {

        private final Logger logger = LoggerFactory.getLogger(RbsConfigLevelFunction.class);

        private final boolean withSubNetwork;
        private final Dps dps;
        private final RbsConfigLevel configLevel;

        private RbsConfigLevelFunction(final Dps dps, final RbsConfigLevel configLevel, final boolean withSubNetwork) {
            this.dps = dps;
            this.configLevel = configLevel;
            this.withSubNetwork = withSubNetwork;
        }

        private String getRbsConfigurationFdn(final String nodeName) {
            if (withSubNetwork) {
                return String.format("SubNetwork=EnmSn,SubNetwork=AP_1,MeContext=%s,ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1",
                        nodeName);
            }
            return String.format("MeContext=%s,ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1", nodeName);
        }

        @Override
        public Void apply(final String nodeFdn) {
            try {
                Thread.sleep(1000);
                final String nodeName = FDN.get(nodeFdn).getRdnValue();
                logger.debug("Updating RbsConfigLevel to {} for node {}", configLevel, nodeName);

                final String fdn = getRbsConfigurationFdn(nodeName);
                final Map<String, Object> attribute = ImmutableMap.<String, Object> of("rbsConfigLevel", configLevel.toString());

                dps.updateMo(fdn, attribute);
            } catch (final InterruptedException e) {

            }

            return null;
        }
    }

    private static class UpdateNodeStateFunction implements Function<String, Void> {

        private final Logger logger = LoggerFactory.getLogger(UpdateNodeStateFunction.class);

        private final Dps dps;
        private final String nodeState;

        private UpdateNodeStateFunction(final Dps dps, final String nodeState) {
            this.dps = dps;
            this.nodeState = nodeState;
        }

        @Override
        public Void apply(final String nodeFdn) {
            logger.debug("Updating state to {} for node {}", nodeState, nodeFdn);

            final String fdn = format("%s,NodeStatus=1", nodeFdn);
            final ImmutableMap<String, Object> newAttributes = ImmutableMap.<String, Object> of("state", nodeState);
            dps.updateMo(fdn, newAttributes);

            return null;
        }
    }

}
