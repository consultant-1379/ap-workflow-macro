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

import static com.ericsson.oss.services.ap.api.status.State.INTEGRATION_COMPLETED;
import static com.ericsson.oss.services.ap.api.status.State.INTEGRATION_FAILED;
import static com.ericsson.oss.services.ap.api.status.State.UNKNOWN;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.ap.api.restore.RestoredNodeStateResolver;
import com.ericsson.oss.services.ap.api.status.State;
import com.ericsson.oss.services.ap.common.cm.DpsQueries;
import com.ericsson.oss.services.ap.common.model.MoType;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigurationAttribute;

/**
 * Resolves what node state should be set when a restore from a backup has completed.
 */
@Stateless
@Local
@EService
@EServiceQualifier("erbs")
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class ErbsRestoredNodeStateResolver implements RestoredNodeStateResolver {

    @Inject
    private DpsQueries dpsQueries;

    @EServiceRef
    private DataPersistenceService dps;

    private static Map<String, State> rbsConfigToApStateMapper;

    @Override
    public State resolveNodeState(final String apNodeFdn) {
        if (rbsConfigToApStateMapper == null) {
            initialiseRbsConfigMapper();
        }

        final String rbsConfigLevelState = getRbsConfigLevel(apNodeFdn);
        return rbsConfigToApStateMapper.containsKey(rbsConfigLevelState) ? rbsConfigToApStateMapper.get(rbsConfigLevelState) : UNKNOWN;
    }

    private static void initialiseRbsConfigMapper() {
        rbsConfigToApStateMapper = new HashMap<>(4);
        rbsConfigToApStateMapper.put(RbsConfigLevel.OSS_CONFIGURATION_FAILED.toString(), INTEGRATION_FAILED);
        rbsConfigToApStateMapper.put(RbsConfigLevel.INTEGRATION_COMPLETE.toString(), INTEGRATION_COMPLETED);
    }

    private String getRbsConfigLevel(final String apNodeFdn) {
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        final Iterator<ManagedObject> meContextMos = dpsQueries.findMoByName(nodeName, MoType.MECONTEXT.toString(), "OSS_TOP").execute();

        if (meContextMos.hasNext()) {
            final String rbsConfigurationFdn = String.format("%s,ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1",
                    meContextMos.next().getFdn());
            final ManagedObject rbsConfigurationMo = dps.getLiveBucket().findMoByFdn(rbsConfigurationFdn);
            return rbsConfigurationMo == null ? null
                    : rbsConfigurationMo.getAttribute(RbsConfigurationAttribute.RBS_CONFIG_LEVEL.toString());
        }

        return null;
    }
}
