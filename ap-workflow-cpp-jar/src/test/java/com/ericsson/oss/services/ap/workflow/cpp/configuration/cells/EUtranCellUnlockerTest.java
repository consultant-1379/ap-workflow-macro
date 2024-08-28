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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.junit.Assert.assertTrue;
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
 * Unit tests for {@link EUtranCellUnlocker}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EUtranCellUnlockerTest {

    private final static String EUTRANCELLFDD = "EUtranCellFDD";
    private final static String EUTRANCELLTDD = "EUtranCellTDD";
    private final static String EUTRANCELL_RDN1 = "EUtranCellFDD=Cell1";
    private final static String EUTRANCELL_RDN2 = "EUtranCellFDD=Cell2";
    private final static String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private final static String ENODEB_FUNCTION_FDN = MECONTEXT_FDN + ",ManagedElement=1,ENodeBFunction=1";
    private final static String EUTRANCELLFDD_FDN_1 = ENODEB_FUNCTION_FDN + "," + EUTRANCELL_RDN1;
    private final static String EUTRANCELLFDD_FDN_2 = ENODEB_FUNCTION_FDN + "," + EUTRANCELL_RDN2;
    private final static String MO_ADMINISTRATIVE_STATE = "administrativeState";
    private final static String CELL_UNLOCKED = "UNLOCKED";

    @Mock
    private DpsQueries dpsQueries;

    @Mock
    private DpsQueryExecutor<ManagedObject> dpsQueryExecutor;

    @Mock
    private DpsOperations dpsOperations;

    @Mock
    private Logger logger; // NOPMD

    @Mock
    private AlarmSender alarmSender; // NOPMD

    @InjectMocks
    private EUtranCellUnlocker eUtranCellUnlocker;

    @Mock
    private ManagedObject eUtranCellMo;

    @Before
    public void setupDpsQueryReturn() {
        final List<ManagedObject> eutranCellMos = new ArrayList<>();
        eutranCellMos.add(eUtranCellMo);
        eutranCellMos.add(eUtranCellMo);

        when(dpsQueries.findChildMosOfTypesInOwnTransaction(ENODEB_FUNCTION_FDN, "ERBS_NODE_MODEL", EUTRANCELLFDD, EUTRANCELLTDD))
                .thenReturn(dpsQueryExecutor);
        when(eUtranCellMo.getFdn()).thenReturn(EUTRANCELLFDD_FDN_1).thenReturn(EUTRANCELLFDD_FDN_2);
        when(dpsQueryExecutor.execute()).thenReturn(eutranCellMos.iterator());
    }

    @Test
    public void when_all_cells_are_unlocked_then_return_administrativeState_of_cell_is_updated() {
        eUtranCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN);
        verify(dpsOperations).updateMo(EUTRANCELLFDD_FDN_1, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        verify(dpsOperations).updateMo(EUTRANCELLFDD_FDN_2, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
    }

    @Test
    public void when_a_cell_fails_to_unlock_then_alarm_is_sent() {
        doThrow(DpsPersistenceException.class).when(dpsOperations).updateMo(EUTRANCELLFDD_FDN_1, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        eUtranCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN);

        verify(alarmSender, times(1)).sendError(any(EventNotification.class));
    }

    @Test
    public void when_retrieving_cellMos_fails_then_alarm_is_sent() {
        doThrow(DpsPersistenceException.class).when(dpsQueries).findChildMosOfTypesInOwnTransaction(ENODEB_FUNCTION_FDN, "ERBS_NODE_MODEL",
                "EUtranCellFDD",
                "EUtranCellTDD");
        eUtranCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN);

        verify(alarmSender, times(1)).sendError(any(EventNotification.class));
    }

    @Test
    public void when_cells_fail_to_unlock_a_list_of_failed_cells_is_returned() {
        doThrow(DpsPersistenceException.class).when(dpsOperations).updateMo(EUTRANCELLFDD_FDN_1, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);
        doThrow(DpsPersistenceException.class).when(dpsOperations).updateMo(EUTRANCELLFDD_FDN_2, MO_ADMINISTRATIVE_STATE, CELL_UNLOCKED);

        final UnlockCellsResult unlockCellsResult = eUtranCellUnlocker.unlockCells(ENODEB_FUNCTION_FDN);
        final List<String> failedCells = unlockCellsResult.getFailedCellIds();
        assertTrue(failedCells.contains(EUTRANCELL_RDN1));
        assertTrue(failedCells.contains(EUTRANCELL_RDN2));

    }

}