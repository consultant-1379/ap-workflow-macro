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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.common.usecase.CommandLogName;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmDataBuilder;
import com.ericsson.oss.services.ap.common.util.string.FDN;

/**
 * Unlocks NbIoT cell MOs.
 */
public class NbIotCellUnlocker extends CellUnlocker {

    private static final String DONOR_CELL_FAILED_MESSAGE = "Failed to unlock: %s. Reason: Donor cell %s is not unlocked";
    private static final String INVALID_EUTRANCELL_REF_MESSAGE = "Failed to unlock: %s. Reason: Donor cell reference is not valid";

    private final UnlockCellsResult unlockCellsResult = new UnlockCellsResult();

    /**
     * Unlocks NbIotCell MOs. If cell MOs cannot be retrieved an alarm is raised with FM.
     *
     * @param eNodeBFunctionFdn
     *            the FDN of the parent ENodeBFunction MO
     * @param unlockEutranCellsResult
     *            IDs of the Eutran cells that failed to unlock
     * @return UnlockCellsResult object containing cells that failed to unlock and error messages
     */
    public UnlockCellsResult unlockCells(final String eNodeBFunctionFdn, final UnlockCellsResult unlockEutranCellsResult) {
        final List<String> failedEutranIds = unlockEutranCellsResult.getFailedCellIds();
        final Iterator<ManagedObject> nbIotCells = getCells(eNodeBFunctionFdn);
        return unlockCellsAndHandleFailures(nbIotCells, failedEutranIds);
    }

    private UnlockCellsResult unlockCellsAndHandleFailures(final Iterator<ManagedObject> nbIotCells, final List<String> failedEutranIds) {
        while (nbIotCells.hasNext()) {
            final ManagedObject nbIotCellMo = nbIotCells.next();
            final String nbIotCellFdn = nbIotCellMo.getFdn();

            unlockCellsResult.incNumberOfCellsToUnlock();

            if (isValidCellToUnlock(failedEutranIds, nbIotCellMo, nbIotCellFdn)) {
                unlockIndividualCell(unlockCellsResult, nbIotCellFdn);
            }
        }

        return unlockCellsResult;
    }

    private boolean isValidCellToUnlock(final List<String> failedEutranIds, final ManagedObject nbIotCellMo, final String nbIotCellFdn) {
        final String eutranCellMoRefFdn = nbIotCellMo.getAttribute("eutranCellRef");

        if (StringUtils.isBlank(eutranCellMoRefFdn)) {
            unlockCellsResult.unlockCellFailed(nbIotCellFdn, String.format(INVALID_EUTRANCELL_REF_MESSAGE, nbIotCellFdn));
            return false;
        }

        if (isDonorEutranCellUnlocked(failedEutranIds, eutranCellMoRefFdn)) {
            unlockCellsResult.unlockCellFailed(nbIotCellFdn, String.format(DONOR_CELL_FAILED_MESSAGE, nbIotCellFdn, eutranCellMoRefFdn));
            return false;
        }

        return true;
    }

    private static boolean isDonorEutranCellUnlocked(final List<String> failedEutranIds, final String eutranCellMoRefFdn) {
        final String eutranCellMoRefId = FDN.get(eutranCellMoRefFdn).getRdn();
        for (final String failedId : failedEutranIds) {
            if (eutranCellMoRefId.equalsIgnoreCase(failedId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void unlockIndividualCell(final UnlockCellsResult unlockCellsResult, final String cellFdn) {
        try {
            setAdministrativeStateToUnlockedInNewTx(cellFdn);
        } catch (final Exception e) {
            logger.warn("Cell unlock failed", e);
            unlockCellsResult.unlockCellFailed(cellFdn, e);
        }
    }

    @Override
    protected Iterator<ManagedObject> getCells(final String eNodeBFunctionFdn) {
        try {
            return dpsQueries.findChildMosOfTypesInOwnTransaction(eNodeBFunctionFdn, "ERBS_NODE_MODEL", "NbIotCell").execute();
        } catch (final Exception e) {
            logger.warn("Failed to get NbIotCell MOs for {}: {}", eNodeBFunctionFdn, e.getMessage(), e);
            alarmSender.sendError(new AlarmDataBuilder()
                    .setManagedObjectInstance(eNodeBFunctionFdn)
                    .setPerceivedSeverity(SEVERITY_CRITICAL)
                    .setEventType("Node integration error")
                    .setSpecificProblem("Failed to unlock cells")
                    .setProbableCause("Error retrieving NbIotCell MOs for " + eNodeBFunctionFdn)
                    .setDescription(CommandLogName.INTEGRATE.toString()
                            + " - When trying to unlock the cells, NbIotCell MOs could not be retrieved from database")
                    .build());
        }
        return Collections.<ManagedObject> emptyIterator();
    }
}
