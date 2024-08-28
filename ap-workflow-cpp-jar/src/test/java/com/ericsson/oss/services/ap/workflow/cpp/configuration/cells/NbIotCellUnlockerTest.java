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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.exception.general.DpsPersistenceException;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.cm.DpsQueries;
import com.ericsson.oss.services.ap.common.cm.DpsQueries.DpsQueryExecutor;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmSender;

/**
 * Unit tests for {@link NbIotCellUnlocker}.
 */
@RunWith(MockitoJUnitRunner.class)
public class NbIotCellUnlockerTest {

    private final static String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private final static String ENODEB_FUNCTION_FDN = MECONTEXT_FDN + ",ManagedElement=1,ENodeBFunction=1";
    private final static String NBIOTCELL_RDN = "NbIotCell=Cell1";
    private final static String NBIOTCELL_FDN_1 = ENODEB_FUNCTION_FDN + ",NbIotCell=Cell1";
    private final static String NBIOTCELL_FDN_2 = ENODEB_FUNCTION_FDN + ",NbIotCell=Cell2";
    private final static String EUTRANCELLFDD_FDN_1 = ENODEB_FUNCTION_FDN + ",EUtranCellFDD=Cell1";
    private final static String EUTRANCELLFDD_FDN_2 = ENODEB_FUNCTION_FDN + ",EUtranCellFDD=Cell2";
    private final static String NBIOTCELL_ID = "NbIotCell";
    private final static String MO_ADMINISTRATIVE_STATE = "administrativeState";
    private final static String CELL_UNLOCKED = "UNLOCKED";
    private final static String DONOR_CELL_FAILED_ERROR = String.format("Failed to unlock: %s. Reason: Donor cell %s is not unlocked.",
            NBIOTCELL_FDN_1, EUTRANCELLFDD_FDN_1);
    private final static String INVALID_EUTRANREF_ERROR_1 = String.format("Failed to unlock: %s. Reason: Donor cell reference is not valid.",
            NBIOTCELL_FDN_1);
    private final static String INVALID_EUTRANREF_ERROR_2 = String.format("Failed to unlock: %s. Reason: Donor cell reference is not valid.",
            NBIOTCELL_FDN_2);

    @Mock
    private ManagedObject nbIotCellMo;

    @Mock
    private DpsQueries dpsQueries;

    @Mock
    private Logger logger; // NOPMD

    @Mock
    private AlarmSender alarmSender; // NOPMD

    @Mock
    private DpsQueryExecutor<ManagedObject> dpsQueryExecutor;

    @Mock
    private DpsOperations dpsOperations;

    @InjectMocks
    private NbIotCellUnlocker nbIotCellUnlocker;

    final UnlockCellsResult unlockEutranCellsResult = new UnlockCellsResult();

    @Before
    public void setup() {
        final List<ManagedObject> nbIotCellMos = new ArrayList<>();
        nbIotCellMos.add(nbIotCellMo);
        nbIotCellMos.add(nbIotCellMo);
        when(dpsQueries.findChildMosOfTypesInOwnTransaction(ENODEB_FUNCTION_FDN, "ERBS_NODE_MODEL", NBIOTCELL_ID)).thenReturn(dpsQueryExecutor);
        when(nbIotCellMo.getFdn()).thenReturn(NBIOTCELL_FDN_1).thenReturn(NBIOTCELL_FDN_2);
        when(dpsQueryExecutor.execute()).thenReturn(nbIotCellMos.iterator());

    }

    @Test
    public void when_no_donor_cells_fail_to_unlock_then_all_nbiot_cells_are_unlocked() {
        when(nbIotCellMo.getAttribute("eutranCellRef")).thenReturn(EUTRANCELLFDD_FDN_1).thenReturn(EUTRANCELLFDD_FDN_2);
        nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, unlockEutranCellsResult);
        verify(dpsOperations).updateMo(NBIOTCELL_FDN_1, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        verify(dpsOperations).updateMo(NBIOTCELL_FDN_2, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
    }

    @Test
    public void when_donor_cell_fails_to_unlock_nbiot_cell_is_not_unlocked() {
        unlockEutranCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1, "cell failed to unlock");
        when(nbIotCellMo.getAttribute("eutranCellRef")).thenReturn(EUTRANCELLFDD_FDN_1).thenReturn(EUTRANCELLFDD_FDN_2);
        nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, unlockEutranCellsResult);

        verify(dpsOperations, never()).updateMo(NBIOTCELL_FDN_1, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        verify(dpsOperations).updateMo(NBIOTCELL_FDN_2, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
    }

    @Test
    public void when_nbiot_cell_fails_to_unlock_list_of_failed_cells_is_returned() {
        doThrow(DpsPersistenceException.class).when(dpsOperations).updateMo(NBIOTCELL_FDN_1, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        final UnlockCellsResult unlockCellsResult = nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, unlockEutranCellsResult);
        final List<String> failedCells = unlockCellsResult.getFailedCellIds();
        assertTrue(failedCells.contains(NBIOTCELL_RDN));

    }

    @Test
    public void when_no_nbiotCellMos_are_retrieved_then_alarm_is_sent() {
        doThrow(DpsPersistenceException.class).when(dpsQueries).findChildMosOfTypesInOwnTransaction(ENODEB_FUNCTION_FDN, "ERBS_NODE_MODEL", "NbIotCell");
        nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, unlockEutranCellsResult);
        verify(alarmSender, times(1)).sendError(any(EventNotification.class));
    }

    @Test
    public void when_nbiot_cell_unlocking_fails_exception_returned_contains_eutran_cell_reference() {
        unlockEutranCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1, "cell failed to unlock");
        when(nbIotCellMo.getAttribute("eutranCellRef")).thenReturn(EUTRANCELLFDD_FDN_1);

        final UnlockCellsResult nbIotUnlockCellsResult = nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, unlockEutranCellsResult);
        assertThat(nbIotUnlockCellsResult.getErrorMessage(), containsString(EUTRANCELLFDD_FDN_1));

    }

    @Test
    public void when_nbiot_cell_retrieves_no_valid_eutran_cell_reference_then_nbiot_cell_is_not_unlocked() {
        unlockEutranCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1, "cell failed to unlock");
        when(nbIotCellMo.getAttribute("eutranCellRef")).thenReturn(EUTRANCELLFDD_FDN_1).thenReturn(null);
        final UnlockCellsResult nbIotUnlockCellsResult = nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, unlockEutranCellsResult);
        assertThat(nbIotUnlockCellsResult.getErrorMessage(), containsString(DONOR_CELL_FAILED_ERROR));
        assertThat(nbIotUnlockCellsResult.getErrorMessage(), containsString(INVALID_EUTRANREF_ERROR_2));
    }

    @Test
    public void when_all_eutran_cells_have_unlocked_but_eutran_refs_are_not_valid_related_nbiot_cells_should_not_be_unlocked() {
        when(nbIotCellMo.getAttribute("eutranCellRef")).thenReturn(null).thenReturn(null);
        final UnlockCellsResult nbIotUnlockCellsResult = nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, unlockEutranCellsResult);
        verify(dpsOperations, never()).updateMo(NBIOTCELL_FDN_1, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        verify(dpsOperations, never()).updateMo(NBIOTCELL_FDN_2, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        assertThat(nbIotUnlockCellsResult.getErrorMessage(), containsString(INVALID_EUTRANREF_ERROR_1));
        assertThat(nbIotUnlockCellsResult.getErrorMessage(), containsString(INVALID_EUTRANREF_ERROR_1));
    }

}
