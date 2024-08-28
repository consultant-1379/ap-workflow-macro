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
package com.ericsson.oss.services.ap.workflow.erbs.task;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ericsson.oss.services.ap.common.model.SupervisionMoType;

/**
 * Unit tests for {@link ErbsWorkflowVariables}.
 */
public class ErbsWorkflowVariablesTest {

    @Test
    public void whenVariablesCreated_andOssPrefixIsNull_thenMeContextHasNoPrefix() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setApNodeFdn(NODE_FDN);
        variables.setOssPrefix(null);

        final String result = variables.getMeContextFdn();

        assertEquals("MeContext=" + NODE_NAME, result);
    }

    @Test
    public void whenVariablesCreated_andOssPrefixContainsMeContext_thenMeContextIsEqualToThePrefix() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setApNodeFdn(NODE_FDN);
        variables.setOssPrefix("SubNetwork=1,MeContext=" + NODE_NAME);

        final String result = variables.getMeContextFdn();

        assertEquals("SubNetwork=1,MeContext=" + NODE_NAME, result);
    }

    @Test
    public void whenVariablesCreated_andOssPrefixDoesNotContainMeContext_thenMeContextIsAppendedToThePrefix() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setApNodeFdn(NODE_FDN);
        variables.setOssPrefix("SubNetwork=1");

        final String result = variables.getMeContextFdn();

        assertEquals("SubNetwork=1,MeContext=" + NODE_NAME, result);
    }

    @Test
    public void whenGpsPositionCheckFailed_thenIntegrationTaskWarningIsSetToTrue() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setGpsPositionCheckFailed(true);
        final boolean result = variables.isIntegrationTaskWarning();
        assertTrue(result);
    }

    @Test
    public void whenOptionalFeaturesOrUnlockCellsFailed_thenIntegrationTaskWarningIsSetToTrue() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.flagFailureOnOptionalFeaturesOrUnlockCells();
        final boolean result = variables.isIntegrationTaskWarning();
        assertTrue(result);
    }

    @Test
    public void whenGpsPositionCheckFailed_thenSecondCvShouldNotBeCreated() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setGpsPositionCheckFailed(true);
        final boolean result = variables.shouldCreateSecondCV();
        assertFalse(result);
    }

    @Test
    public void whenOptionalFeaturesOrUnlockCellsFailed_thenSecondCvShouldNotBeCreated() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.flagFailureOnOptionalFeaturesOrUnlockCells();
        final boolean result = variables.shouldCreateSecondCV();
        assertFalse(result);
    }

    @Test
    public void whenEnableFmSupervisionIsTrue_thenEnableSupervisionIsTrue() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setEnableSupervision(SupervisionMoType.FM, true);
        final boolean result = variables.isEnableSupervision();
        assertTrue(result);
    }

    @Test
    public void whenEnablePmSupervisionIsTrue_thenEnableSupervisionIsTrue() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setEnableSupervision(SupervisionMoType.PM, true);
        final boolean result = variables.isEnableSupervision();
        assertTrue(result);
    }

    @Test
    public void whenEnableInventorySupervisionIsTrue_thenEnableSupervisionIsTrue() {
        final ErbsWorkflowVariables variables = new ErbsWorkflowVariables();
        variables.setEnableSupervision(SupervisionMoType.INVENTORY, true);
        final boolean result = variables.isEnableSupervision();
        assertTrue(result);
    }
}