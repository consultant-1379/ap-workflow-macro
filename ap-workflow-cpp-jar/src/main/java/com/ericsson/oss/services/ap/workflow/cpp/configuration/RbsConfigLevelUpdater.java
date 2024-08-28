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

import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigurationAttribute.RBS_CONFIG_LEVEL;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.classic.RetryManagerNonCDIImpl;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.workflow.cpp.model.MoType;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;

/**
 * Updates the <i>rbsConfigLevel</i> value for an ERBS node.
 */
public class RbsConfigLevelUpdater {

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_INTERVAL_IN_MS = 100;
    private static final double RETRY_EXPONENTIAL_BACKOFF = 1.5;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private DpsOperations dpsOperations;

    /**
     * Updates the <i>rbsConfigLevel</i> attribute on the <code>RbsConfiguration</code> MO.
     * <p>
     * This will retry on any {@link Exception}.
     *
     * @param meContextFdn
     *            the FDN of the <code>MeContext</code>
     * @param newRbsConfigLevel
     *            the new value of the <i>rbsConfigLevel</i>
     */
    public void updateRbsConfigLevel(final String meContextFdn, final RbsConfigLevel newRbsConfigLevel) {
        logger.debug("Updating MO {} with rbsConfigLevel {}", meContextFdn, newRbsConfigLevel);

        final RetryPolicy policy = RetryPolicy.builder()
                .attempts(MAX_RETRIES)
                .waitInterval(RETRY_INTERVAL_IN_MS, TimeUnit.MILLISECONDS)
                .exponentialBackoff(RETRY_EXPONENTIAL_BACKOFF)
                .retryOn(Exception.class)
                .build();

        final String rbsConfigurationFdn = getRbsConfigurationFdn(meContextFdn);

        final RetryManager retryManager = new RetryManagerNonCDIImpl();

        retryManager.executeCommand(policy, (RetriableCommand<Void>) retryContext -> {
            setAttributeInNewTx(rbsConfigurationFdn, newRbsConfigLevel);
            return null;
        });
        logger.debug("Updated MO {} with rbsConfigLevel {}", meContextFdn, newRbsConfigLevel);
    }

    private static String getRbsConfigurationFdn(final String meContextFdn) {
        return new StringBuilder()
                .append(meContextFdn)
                .append(",")
                .append(MoType.MANAGEDELEMENT.toString())
                .append("=1,")
                .append(MoType.NODEMANAGEDFUNCTION.toString())
                .append("=1,")
                .append(MoType.RBSCONFIGURATION.toString())
                .append("=1")
                .toString();
    }

    private void setAttributeInNewTx(final String fdn, final RbsConfigLevel rbsConfigLevel) {
        logger.info("Updating MO {} with rbsConfigLevel {}", fdn, rbsConfigLevel);
        dpsOperations.updateMoWithFailure(fdn, RBS_CONFIG_LEVEL.toString(), rbsConfigLevel.name());
    }
}
