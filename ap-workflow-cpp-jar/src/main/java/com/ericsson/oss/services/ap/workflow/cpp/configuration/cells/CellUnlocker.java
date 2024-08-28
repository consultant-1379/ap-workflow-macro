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

import java.util.Iterator;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.cm.DpsQueries;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmSender;

/**
 * Abstract class for unlocking cells.
 */
abstract class CellUnlocker {

    @Inject
    protected Logger logger;

    @Inject
    protected DpsQueries dpsQueries;

    @Inject
    protected AlarmSender alarmSender;

    @Inject
    private DpsOperations dpsOperations;

    /**
     * Attempts to unlock individual cell. In the case of a failure the {@link UnlockCellsResult} is updated with failure.
     *
     * @param unlockCellsResult
     *            the result to be updated in the case of a failure to unlock cell
     * @param cellFdn
     *            the FDN of the cell to be unlocked
     */
    protected abstract void unlockIndividualCell(final UnlockCellsResult unlockCellsResult, final String cellFdn);

    /**
     * Retrieves the cell MOs from DPS. If no MOs are retrieved, an alarm is raised with FM.
     *
     * @param cellParentFdn
     *            the FDN of the parent MO of the cells
     * @return a collection of cell MOs
     */
    protected abstract Iterator<ManagedObject> getCells(final String cellParentFdn);

    /**
     * Updates the cell MO in DPS to set the <i>administrativeState</i> to <b>UNLOCKED</b>.
     *
     * @param cellFdn
     *            the FDN of the cell to unlock
     */
    protected void setAdministrativeStateToUnlockedInNewTx(final String cellFdn) {
        dpsOperations.updateMo(cellFdn, "administrativeState", "UNLOCKED");
        logger.debug("Executing unlock cells on {}", cellFdn);
    }
}
