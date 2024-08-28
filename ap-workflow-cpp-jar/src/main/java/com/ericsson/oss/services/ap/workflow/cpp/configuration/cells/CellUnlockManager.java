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
package com.ericsson.oss.services.ap.workflow.cpp.configuration.cells;

import static com.ericsson.oss.services.ap.common.util.alarm.AlarmDataBuilder.SEVERITY_CRITICAL;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.CELLS_UNLOCKED;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.UNLOCKING_CELLS;
import static com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel.UNLOCKING_CELLS_FAILED;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmDataBuilder;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmSender;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.RbsConfigLevelUpdater;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;

/**
 * Calls relevant classes to unlock EUtranCellFDD/EUtranCellTDD MOs and their NbIotCell MOs, and updates the <code>rbsConfigLevel</code> attribute in
 * the RbsConfiguration MO before and after.
 */
public class CellUnlockManager {

    private static final String FAILED_TO_SET_RBS_CONFIG_LEVEL_ERROR_MESSAGE = "Failed to set rbsConfigLevel for %s to %s: %s";
    private static final String NO_CELLS_TO_UNLOCK_ERROR_MESSAGE = "No cells exist to unlock.";
    private static final String UNLOCK_CELLS_ALL_FAILED_ERROR_MESSAGE = "Failed to unlock all cell(s). %s ";
    private static final String UNLOCK_CELLS_FAILED_ERROR_MESSAGE = "Failed to unlock %s of %s cell(s). %s";

    @Inject
    private AlarmSender alarmSender;

    @Inject
    private EUtranCellUnlocker eutranCellUnlocker;

    @Inject
    private Logger logger;

    @Inject
    private NbIotCellUnlocker nbIotCellUnlocker;

    @Inject
    private RbsConfigLevelUpdater rbsConfigLevelUpdater;

    /**
     * Calls EUtranCellUnlocker {@link EUtranCellUnlocker} and NbIotCellUnlocker {@link NbIotCellUnlocker} to unlock the respective cells, and updates
     * the <code>rbsConfigLevel</code> attribute in the RbsConfiguration MO.
     * <p>
     * The <code>rbsConfigLevel</code> is first set to {@link RbsConfigLevel#UNLOCKING_CELLS} when the unlocking begins.
     * <p>
     * The <code>rbsConfigLevel</code> is then set to either {@link RbsConfigLevel#CELLS_UNLOCKED} or {@link RbsConfigLevel#UNLOCKING_CELLS_FAILED},
     * depending on the result.
     *
     * @param meContextFdn
     *            the MeContext FDN
     * @throws ApServiceException
     *             ApServiceException is thrown when updating of the RbsConfiguration MO unexpectedly fails
     */
    public void unlock(final String meContextFdn) {

        setRbsConfigLevel(meContextFdn, UNLOCKING_CELLS);
        final String eNodeBFunctionFdn = meContextFdn + ",ManagedElement=1,ENodeBFunction=1";
        UnlockCellsResult unlockNbIotCellsResult = new UnlockCellsResult();
        final UnlockCellsResult unlockEutranCellsResult = eutranCellUnlocker.unlockCells(eNodeBFunctionFdn);

        if (anyEUtranCellUnlocksSuccessful(unlockEutranCellsResult)) {
            unlockNbIotCellsResult = nbIotCellUnlocker.unlockCells(eNodeBFunctionFdn, unlockEutranCellsResult);
        }

        final RbsConfigLevel rbsConfigAfterUnlock = resolveNewRbsConfigLevel(unlockEutranCellsResult);
        setRbsConfigLevel(meContextFdn, rbsConfigAfterUnlock);

        if (anyCellUnlocksFailed(unlockEutranCellsResult, unlockNbIotCellsResult)) {
            final String responseMessage = createErrorMessage(unlockEutranCellsResult, unlockNbIotCellsResult);
            throw new ApServiceException(responseMessage);
        }

    }

    private void setRbsConfigLevel(final String meContextFdn, final RbsConfigLevel rbsConfigLevelValue) {
        try {
            rbsConfigLevelUpdater.updateRbsConfigLevel(meContextFdn, rbsConfigLevelValue);
        } catch (final Exception e) {
            final String message = String.format(FAILED_TO_SET_RBS_CONFIG_LEVEL_ERROR_MESSAGE, meContextFdn, rbsConfigLevelValue, e.getMessage());
            logger.warn(message, e);
            final String rbsConfigurationFdn = meContextFdn + ",ManagedElement=1,NodeManagementFunction=1,RbsConfiguration=1";
            alarmSender.sendError(new AlarmDataBuilder()
                    .setManagedObjectInstance(rbsConfigurationFdn)
                    .setPerceivedSeverity(SEVERITY_CRITICAL)
                    .setEventType("Node integration error")
                    .setSpecificProblem("Failed to unlock cells")
                    .setProbableCause("Error setting rbsConfigLevel for " + rbsConfigurationFdn + " to " + rbsConfigLevelValue)
                    .setDescription(
                            "AUTO_PROVISIONING.INTEGRATE - When trying to unlock the cells, could not set rbsConfigLevel on the RbsConfiguration MO")
                    .build());
            throw new ApServiceException(message);
        }
    }

    private static RbsConfigLevel resolveNewRbsConfigLevel(final UnlockCellsResult unlockEutranCellsResult) {
        return unlockEutranCellsResult.isFailure() ? UNLOCKING_CELLS_FAILED : CELLS_UNLOCKED;
    }

    private static boolean anyEUtranCellUnlocksSuccessful(final UnlockCellsResult unlockEutranCellsResult) {
        return unlockEutranCellsResult.isSuccess() || unlockEutranCellsResult.isPartialFailure();
    }

    private static boolean anyCellUnlocksFailed(final UnlockCellsResult unlockEutranCellsResult, final UnlockCellsResult unlockNbIotCellsResult) {
        return unlockEutranCellsResult.isPartialFailure() || unlockNbIotCellsResult.isPartialFailure();
    }

    private static String createErrorMessage(final UnlockCellsResult... unlockCellsResults) {
        final int noOfTotalFailedCells = computeTotalFailures(unlockCellsResults);
        final int noOfTotalCells = computeTotalCells(unlockCellsResults);
        final String totalErrorMessages = consolidateErrorMessages(unlockCellsResults);

        if (noOfTotalCells == 0) {
            return NO_CELLS_TO_UNLOCK_ERROR_MESSAGE;
        } else if (noOfTotalFailedCells == noOfTotalCells) {
            return String.format(UNLOCK_CELLS_ALL_FAILED_ERROR_MESSAGE, totalErrorMessages);
        } else {
            return String.format(UNLOCK_CELLS_FAILED_ERROR_MESSAGE,
                    noOfTotalFailedCells,
                    noOfTotalCells,
                    totalErrorMessages);
        }
    }

    private static int computeTotalCells(final UnlockCellsResult... unlockResults) {
        int numberOfCells = 0;
        for (final UnlockCellsResult unlockResult : unlockResults) {
            numberOfCells += unlockResult.getTotalNumberOfCellsToUnlock();
        }
        return numberOfCells;
    }

    private static int computeTotalFailures(final UnlockCellsResult... unlockResults) {
        int numberOfFailures = 0;
        for (final UnlockCellsResult unlockResult : unlockResults) {
            numberOfFailures += unlockResult.getUnlockFailures();
        }
        return numberOfFailures;
    }

    private static String consolidateErrorMessages(final UnlockCellsResult... unlockResults) {
        final StringBuilder allErrorMessages = new StringBuilder();
        for (final UnlockCellsResult unlockResult : unlockResults) {
            allErrorMessages.append(unlockResult.getErrorMessage());
        }
        return allErrorMessages.toString();
    }
}
