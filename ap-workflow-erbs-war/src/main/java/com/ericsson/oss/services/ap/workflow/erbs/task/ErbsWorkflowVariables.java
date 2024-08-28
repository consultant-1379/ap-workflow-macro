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
package com.ericsson.oss.services.ap.workflow.erbs.task;

import com.ericsson.oss.services.ap.common.model.SupervisionMoType;
import com.ericsson.oss.services.ap.common.workflow.AbstractWorkflowVariables;

/**
 * This contains all workflow variables required for the ERBS service tasks.
 */
public class ErbsWorkflowVariables extends AbstractWorkflowVariables {

    private static final long serialVersionUID = 1L;

    private boolean uploadCvAfterIntegrationEnabled;
    private boolean createCvSuccessful;
    private boolean gpsPositionCheckFailed;
    private boolean optionalFeaturesOrUnlockCellsFailed;
    private boolean optionalFeaturesOrUnlockCellsExecuted;

    public String getMeContextFdn() {
        if (getOssPrefix() == null) {
            return "MeContext=" + getNodeName();
        } else if (getOssPrefix().contains("MeContext")) {
            return getOssPrefix();
        } else {
            return getOssPrefix() + ",MeContext=" + getNodeName();
        }
    }

    public void setCreateCvSuccessful(final boolean createCvSuccessful) {
        this.createCvSuccessful = createCvSuccessful;
    }

    public boolean isCreateCvSuccessful() {
        return createCvSuccessful;
    }

    public boolean isUploadCvAfterIntegrationEnabled() {
        return uploadCvAfterIntegrationEnabled;
    }

    public void setUploadCvAfterIntegrationEnabled(final boolean uploadCvAfterIntegrationEnabled) {
        this.uploadCvAfterIntegrationEnabled = uploadCvAfterIntegrationEnabled;
    }

    public boolean isGpsPositionCheckFailed() {
        return gpsPositionCheckFailed;
    }

    public void setGpsPositionCheckFailed(final boolean gpsPositionCheckFailed) {
        this.gpsPositionCheckFailed = gpsPositionCheckFailed;
        setIntegrationTaskWarning(gpsPositionCheckFailed);
    }

    public void flagFailureOnOptionalFeaturesOrUnlockCells() {
        optionalFeaturesOrUnlockCellsFailed = true;
        setIntegrationTaskWarning(true);
    }

    public boolean shouldCreateSecondCV() {
        return optionalFeaturesOrUnlockCellsExecuted && !optionalFeaturesOrUnlockCellsFailed && !gpsPositionCheckFailed;
    }

    public void setOptionalFeaturesOrUnlockCellsExecuted(final boolean optionalFeaturesOrUnlockCellsExecuted) {
        this.optionalFeaturesOrUnlockCellsExecuted = optionalFeaturesOrUnlockCellsExecuted;
    }

    /**
     * Identifies if supervision attributes need to be enabled or disabled
     *
     * @return true if PM, FM or Inventory supervision attributes are present for ERBS node
     */
    public boolean isEnableSupervision() {
        return super.isEnableSupervision(SupervisionMoType.FM) || super.isEnableSupervision(SupervisionMoType.PM)
                || super.isEnableSupervision(SupervisionMoType.INVENTORY);
    }
}
