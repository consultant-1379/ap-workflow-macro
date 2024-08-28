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
package com.ericsson.oss.services.ap.workflow.cpp.configuration.cells;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.ericsson.oss.services.ap.common.util.string.FDN;

/**
 * Data class containing the results of the Unlock Cells operation.
 */
public class UnlockCellsResult {

    private static final String FAILED_TO_UNLOCK_CELL = "Failed to unlock: %s. Reason: %s";

    private final StringBuilder fullErrorMessage = new StringBuilder();
    private final List<String> failedCellIds = new ArrayList<>();

    private int totalNumberOfCellsToUnlock;
    private int unlockFailures;

    public int getUnlockFailures() {
        return unlockFailures;
    }

    public void setUnlockFailures(final int unlockFailures) {
        this.unlockFailures = unlockFailures;
    }

    /**
     * Increments the number of cells to be unlocked.
     */
    public void incNumberOfCellsToUnlock() {
        ++totalNumberOfCellsToUnlock;
    }

    /**
     * Check if any cells failed to unlock.
     *
     * @return true if any cells failed to unlock
     */
    public boolean isPartialFailure() {
        return unlockFailures > 0;
    }

    /**
     * Check if all cells successfully unlocked.
     *
     * @return true if all cells unlocked
     */
    public boolean isSuccess() {
        return unlockFailures == 0;
    }

    /**
     * Gets the failed cell IDs.
     *
     * @return list of failed cell IDs
     */
    public List<String> getFailedCellIds() {
        return failedCellIds;
    }

    /**
     * Gets the failed cell IDs.
     *
     * @return list of failed cell IDs
     */
    public String getFailedCellIdsAsString() {
        final StringBuilder builder = new StringBuilder();
        for (final String cellId : failedCellIds) {
            builder.append(" [").append(cellId).append(']');
        }
        return builder.toString();
    }

    /**
     * Checks if all cells failed to unlock.
     *
     * @return true if all cells failed to unlock
     */
    public boolean isFailure() {
        return totalNumberOfCellsToUnlock == unlockFailures;
    }

    /**
     * Increments the number of failed cells, extracts root cause from the exception thrown and logs it in the error messages. Records the failed cell
     * fdn ID in a list.
     *
     * @param cellFdn
     *            the fdn of the failed cell
     * @param e
     *            the exception thrown causing the cell to fail
     */
    public void unlockCellFailed(final String cellFdn, final Exception e) {
        ++unlockFailures;
        final String errorMessage = getErrorRootCauseMessage(cellFdn, e);
        fullErrorMessage.append('\n').append(errorMessage).append('.');
        failedCellIds.add(FDN.get(cellFdn).getRdn());
    }

    /**
     * Increments the number of failed cells, records the error message and records the failed cell FDN ID in a list.
     *
     * @param cellFdn
     *            the fdn of the failed cell
     * @param errorMessage
     *            the error causing the cell to fail
     */
    public void unlockCellFailed(final String cellFdn, final String errorMessage) {
        ++unlockFailures;
        fullErrorMessage.append('\n').append(errorMessage).append('.');
        failedCellIds.add(FDN.get(cellFdn).getRdn());
    }

    /**
     * Decides the appropriate error message based on whether cells failed to unlock or not.
     *
     * @return String error message
     */
    public String getErrorMessage() {
        return fullErrorMessage.toString();
    }

    /**
     * Returns the total number of cells to unlock.
     *
     * @return int number of cells to unlock
     */
    public int getTotalNumberOfCellsToUnlock() {
        return totalNumberOfCellsToUnlock;
    }

    private static String getErrorRootCauseMessage(final String fdn, final Exception e) {
        final String rootCause;
        if (ExceptionUtils.getRootCause(e) == null) {
            rootCause = e.getMessage();
        } else {
            rootCause = ExceptionUtils.getRootCauseMessage(e);
        }
        return String.format(FAILED_TO_UNLOCK_CELL, FDN.get(fdn).getRdn(), rootCause);
    }
}
