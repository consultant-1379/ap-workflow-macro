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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.common.util.alarm.AlarmSender;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.RbsConfigLevelUpdater;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;

/**
 * Unit tests for {@link EUtranCellUnlocker}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CellUnlockManagerTest {

    private final static String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private final static String ENODEB_FUNCTION_FDN = MECONTEXT_FDN + ",ManagedElement=1,ENodeBFunction=1";
    private final static String EUTRANCELLFDD_FDN_1 = ENODEB_FUNCTION_FDN + ",EUtranCellFDD=Cell1";
    private final static String EUTRANCELLFDD_FDN_2 = ENODEB_FUNCTION_FDN + ",EUtranCellFDD=Cell2";
    private final static String EUTRANCELLFDD_FDN_3 = ENODEB_FUNCTION_FDN + ",EUtranCellFDD=Cell3";
    private final static String NBIOTCELL_FDN = ENODEB_FUNCTION_FDN + ",NbIotCell=NbIotCell1";
    private final static String ERROR_MESSAGE = "error unlocking Cell ";
    private final static String EXAMPLE_EXCEPTION = "DpsPersistenceException";
    private final static String EUTRAN_ERROR_MESSAGE = "Failed to unlock: %s. Reason: %s";
    private final static String FAILED_TO_UNLOCK_CELL_AS_DONOR_CELL_FAILED = "Failed to unlock: %s. Reason: donor cell %s is not unlocked";
    private final static String NO_CELLS_TO_UNLOCK = "No cells exist to unlock.";
    private final static String INVALID_EUTRANCELLREF_ERROR = String.format("Failed to unlock: %s. Reason: Donor cell is not valid.", NBIOTCELL_FDN);

    @Mock
    private RbsConfigLevelUpdater rbsConfigLevelUpdater;

    @Mock
    private EUtranCellUnlocker eUtranCellUnlocker;

    @Mock
    private NbIotCellUnlocker nbIotCellUnlocker;

    @InjectMocks
    private CellUnlockManager cellUnlockManager;

    @Mock
    private Logger logger; // NOPMD

    @Mock
    private AlarmSender alarmSender; // NOPMD

    private final UnlockCellsResult eUtranUnlockCellsResult = new UnlockCellsResult();
    private final UnlockCellsResult nbIotUnlockCellsResult = new UnlockCellsResult();

    final List<String> failedEutranIds = new ArrayList<>();

    @Before
    public void setup() {
        when(eUtranCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN)).thenReturn(eUtranUnlockCellsResult);
    }

    @Test
    public void when_all_cells_are_unlocked_then_RbsConfigLevel_is_set_from_UNLOCKING_CELL_to_CELLS_UNLOCKED() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        nbIotUnlockCellsResult.incNumberOfCellsToUnlock();

        cellUnlockManager.unlock(MECONTEXT_FDN);

        final InOrder order = Mockito.inOrder(rbsConfigLevelUpdater);
        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.UNLOCKING_CELLS);
        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.CELLS_UNLOCKED);
    }

    @Test(expected = ApServiceException.class)
    public void when_one_of_two_cells_fails_to_be_unlocked_then_RbsConfigLevel_is_set_from_UNLOCKING_CELL_to_CELLS_UNLOCKED() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1, ERROR_MESSAGE);
        cellUnlockManager.unlock(MECONTEXT_FDN);

        final InOrder order = Mockito.inOrder(rbsConfigLevelUpdater);
        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.UNLOCKING_CELLS);
        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.CELLS_UNLOCKED);
    }

    @Test(expected = ApServiceException.class)
    public void when_all_cells_fail_to_be_unlocked_then_RbsConfigLevel_is_set_from_UNLOCKING_CELL_to_UNLOCKING_CELLS_FAILED() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1, ERROR_MESSAGE);
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_2, ERROR_MESSAGE);
        cellUnlockManager.unlock(MECONTEXT_FDN);

        final InOrder order = Mockito.inOrder(rbsConfigLevelUpdater);
        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.UNLOCKING_CELLS);
        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.UNLOCKING_CELLS_FAILED);
    }

    @Test
    public void when_no_cells_to_unlock_then_RbsConfigLevel_is_set_from_UNLOCKING_CELL_to_UNLOCKING_CELLS_FAILED() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        cellUnlockManager.unlock(MECONTEXT_FDN);
        final InOrder order = Mockito.inOrder(rbsConfigLevelUpdater);

        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.UNLOCKING_CELLS);
        order.verify(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.UNLOCKING_CELLS_FAILED);
    }

    @Test(expected = ApServiceException.class)
    public void when_setting_RbsConfigLevel_fails_APService_Exception_is_thrown() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        doThrow(NullPointerException.class).when(rbsConfigLevelUpdater).updateRbsConfigLevel(MECONTEXT_FDN, RbsConfigLevel.UNLOCKING_CELLS);
        cellUnlockManager.unlock(MECONTEXT_FDN);

        verify(alarmSender, times(1)).sendError(any(EventNotification.class));
    }

    @Test
    public void when_cells_fail_to_unlock_then_error_message_contains_correct_number_of_failures() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        nbIotUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();

        nbIotUnlockCellsResult.unlockCellFailed(NBIOTCELL_FDN,
                String.format(FAILED_TO_UNLOCK_CELL_AS_DONOR_CELL_FAILED, NBIOTCELL_FDN, EUTRANCELLFDD_FDN_1));
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1,
                String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_1, "DpsPersistenceException"));
        final int totalCells = (eUtranUnlockCellsResult.getTotalNumberOfCellsToUnlock() + nbIotUnlockCellsResult.getTotalNumberOfCellsToUnlock());
        final int totalFails = (eUtranUnlockCellsResult.getUnlockFailures() + nbIotUnlockCellsResult.getUnlockFailures());
        try {
            cellUnlockManager.unlock(MECONTEXT_FDN);
        } catch (final Exception e) {
            assertThat(e.getMessage(), containsString(String.format("Failed to unlock %d of %d cell(s).", totalFails, totalCells)));
        }
    }

    @Test
    public void when_EUtrancell_fail_to_unlock_then_error_message_for_NbIotCells_contains_reference_to_EUtranCell() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        nbIotUnlockCellsResult.incNumberOfCellsToUnlock();

        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1,
                String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_1, EXAMPLE_EXCEPTION));
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_2,
                String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_2, EXAMPLE_EXCEPTION));
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_3,
                String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_3, EXAMPLE_EXCEPTION));
        nbIotUnlockCellsResult.unlockCellFailed(NBIOTCELL_FDN,
                String.format(FAILED_TO_UNLOCK_CELL_AS_DONOR_CELL_FAILED, NBIOTCELL_FDN, EUTRANCELLFDD_FDN_1));

        try {
            cellUnlockManager.unlock(MECONTEXT_FDN);
        } catch (final Exception e) {
            assertThat(e.getMessage(), containsString(String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_1, EXAMPLE_EXCEPTION)));
            assertThat(e.getMessage(), containsString(String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_2, EXAMPLE_EXCEPTION)));
            assertThat(e.getMessage(), containsString(String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_3, EXAMPLE_EXCEPTION)));
            assertThat(e.getMessage(),
                    containsString(String.format(FAILED_TO_UNLOCK_CELL_AS_DONOR_CELL_FAILED, NBIOTCELL_FDN, EUTRANCELLFDD_FDN_1)));
        }
    }

    @Test
    public void when_no_cells_to_unlock_no_cells_error_message_returned() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        try {
            cellUnlockManager.unlock(MECONTEXT_FDN);
        } catch (final Exception e) {
            assertThat(e.getMessage(), containsString(NO_CELLS_TO_UNLOCK));
        }
    }

    @Test
    public void when_combination_of_EUtran_and_NbIot_cells_failures_and_success_error_message_returns_correct_combination_and_number_of_cells() {
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        eUtranUnlockCellsResult.incNumberOfCellsToUnlock();
        nbIotUnlockCellsResult.incNumberOfCellsToUnlock();
        nbIotUnlockCellsResult.incNumberOfCellsToUnlock();

        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_1,
                String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_1, EXAMPLE_EXCEPTION));
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_2,
                String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_2, EXAMPLE_EXCEPTION));
        eUtranUnlockCellsResult.unlockCellFailed(EUTRANCELLFDD_FDN_3,
                String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_3, EXAMPLE_EXCEPTION));
        nbIotUnlockCellsResult.unlockCellFailed(NBIOTCELL_FDN,
                String.format(FAILED_TO_UNLOCK_CELL_AS_DONOR_CELL_FAILED, NBIOTCELL_FDN, EUTRANCELLFDD_FDN_1));

        final int totalCells = (eUtranUnlockCellsResult.getTotalNumberOfCellsToUnlock() + nbIotUnlockCellsResult.getTotalNumberOfCellsToUnlock());
        final int totalFails = (eUtranUnlockCellsResult.getUnlockFailures() + nbIotUnlockCellsResult.getUnlockFailures());

        try {
            cellUnlockManager.unlock(MECONTEXT_FDN);
        } catch (final Exception e) {
            assertThat(e.getMessage(), containsString(String.format("Failed to unlock %d of %d cell(s).", totalFails, totalCells)));
            assertThat(e.getMessage(), containsString(String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_1, EXAMPLE_EXCEPTION)));
            assertThat(e.getMessage(), containsString(String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_2, EXAMPLE_EXCEPTION)));
            assertThat(e.getMessage(), containsString(String.format(EUTRAN_ERROR_MESSAGE, EUTRANCELLFDD_FDN_3, EXAMPLE_EXCEPTION)));
            assertThat(e.getMessage(),
                    containsString(String.format(FAILED_TO_UNLOCK_CELL_AS_DONOR_CELL_FAILED, NBIOTCELL_FDN, EUTRANCELLFDD_FDN_1)));
        }
    }

    @Test
    public void when_only_nbiotCell_exists_with_no_eutranCellRef_then_unlock_cells_fails_with_error_message_lists_nbiot_cell() {
        nbIotUnlockCellsResult.unlockCellFailed(NBIOTCELL_FDN, INVALID_EUTRANCELLREF_ERROR);
        nbIotUnlockCellsResult.incNumberOfCellsToUnlock();
        when(nbIotCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN, eUtranUnlockCellsResult)).thenReturn(nbIotUnlockCellsResult);

        try {
            cellUnlockManager.unlock(MECONTEXT_FDN);
        } catch (final Exception e) {
            assertThat(e.getMessage(), containsString(INVALID_EUTRANCELLREF_ERROR));
        }
    }
}
