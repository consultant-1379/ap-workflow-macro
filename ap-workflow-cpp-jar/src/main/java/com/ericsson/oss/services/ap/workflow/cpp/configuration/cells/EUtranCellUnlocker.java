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

import static com.ericsson.oss.services.ap.common.util.alarm.AlarmDataBuilder.SEVERITY_CRITICAL;

import java.util.Collections;
import java.util.Iterator;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmDataBuilder;

/**
 * Unlocks EUtranCellFDD/EUtranCellTDD MOs. An alarm is sent to FM if the unlocking of any of the MOs fails.
 */
public class EUtranCellUnlocker extends CellUnlocker {

    /**
     * Unlocks EUtranCell MOs. If cells fail to unlock or cell MOs cannot be retrieved an alarm is raised with FM.
     *
     * @param eNodeBFunctionFdn
     *            the FDN of the parent ENodeBFunction MO
     * @return cells that failed to unlock with corresponding error messages
     */
    public UnlockCellsResult unlockCells(final String eNodeBFunctionFdn) {
        final Iterator<ManagedObject> eUtranCellsMos = getCells(eNodeBFunctionFdn);
        final UnlockCellsResult unlockCellsResult = new UnlockCellsResult();

        while (eUtranCellsMos.hasNext()) {
            final String eutranCellFdn = eUtranCellsMos.next().getFdn();
            unlockCellsResult.incNumberOfCellsToUnlock();

            unlockIndividualCell(unlockCellsResult, eutranCellFdn);
        }

        if (unlockCellsResult.isPartialFailure()) {
            sendFailedToUnlockCellsAlarm(eNodeBFunctionFdn, unlockCellsResult);
        }

        return unlockCellsResult;
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

    private void sendFailedToUnlockCellsAlarm(final String eNodeBFunctionFdn, final UnlockCellsResult unlockCellsResult) {
        alarmSender.sendError(new AlarmDataBuilder().setManagedObjectInstance(eNodeBFunctionFdn)
                .setPerceivedSeverity(SEVERITY_CRITICAL)
                .setEventType("Node integration error")
                .setSpecificProblem("Failed to unlock cells")
                .setProbableCause(
                        "Error setting administrativeState to UNLOCKED on the following cells " + unlockCellsResult.getFailedCellIdsAsString())
                .setDescription(
                        "AUTO_PROVISIONING.INTEGRATE - When trying to unlock the cells, could not set administrativeState to UNLOCKED on the EUtranCellFDD MOs")
                .build());
    }

    @Override
    protected Iterator<ManagedObject> getCells(final String cellParentFdn) {
        try {
            return dpsQueries.findChildMosOfTypesInOwnTransaction(cellParentFdn, "ERBS_NODE_MODEL", "EUtranCellFDD", "EUtranCellTDD").execute();
        } catch (final Exception e) {
            logger.warn("Failed to get EUtranCell MOs for {}: {}", cellParentFdn, e.getMessage(), e);
            alarmSender.sendError(new AlarmDataBuilder()
                    .setManagedObjectInstance(cellParentFdn)
                    .setPerceivedSeverity(SEVERITY_CRITICAL)
                    .setEventType("Node integration error")
                    .setSpecificProblem("Failed to unlock cells")
                    .setProbableCause("Error retrieving EUtranCell MOs for " + cellParentFdn)
                    .setDescription(
                            "AUTO_PROVISIONING.INTEGRATE - When trying to unlock the cells, EUtranCell MOs could not be retrieved from database")
                    .build());
        }
        return Collections.<ManagedObject> emptyIterator();
    }
}