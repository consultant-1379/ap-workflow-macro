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
package com.ericsson.oss.services.ap.workflow.erbs.task.integrate;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.ap.api.status.StatusEntryManagerLocal;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.ap.workflow.erbs.task.ErbsWorkflowVariables;

/**
 * Unit tests for {@link GpsPositionCheckResultHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GpsPositionCheckResultHandlerTest {

    private static final String GPS_SUCCESSFULLY_MATCHED_ADDITIONAL_INFO = "GPS successfully matched";
    private static final String GPS_WANTED_POSITION_NOT_SET_ADDITIONAL_INFO = "Latitude, longitude and altitude values not set in SiteBasic file";
    private static final String GPS_MISMATCH_ERROR_ADDITIONAL_INFO = "Latitude, longitude and altitude values in SiteBasic file do not match GPS position - ENM alarm raised";
    private static final String GPS_POSITION_UNAVAILABLE_ADDITIONAL_INFO = "GPS service not available, not possible to determine GPS position - ENM alarm raised";

    private final ErbsWorkflowVariables workflowVariables = new ErbsWorkflowVariables();

    @Mock
    private StatusEntryManagerLocal statusEntryManagerMock;

    @InjectMocks
    private GpsPositionCheckResultHandler gpsResultHandler;

    @Before
    public void setUp() {
        workflowVariables.setApNodeFdn(NODE_FDN);
        workflowVariables.setUserId("test");
    }

    @Test
    public void whenTaskExecutes_WithSuccessfulMessageGpsSuccessfullyMatched_thenStatusIsUpdatedToCompleted() {
        gpsResultHandler.handleResult(RbsConfigLevel.GPS_SUCCESSFULLY_MATCHED.toString(), workflowVariables);
        verify(statusEntryManagerMock).taskCompleted(eq(NODE_FDN), eq(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString()),
                eq(GPS_SUCCESSFULLY_MATCHED_ADDITIONAL_INFO));
    }

    @Test
    public void whenTaskExecutes_WithSuccessfulMessageGpsWantedPositionNotSet_thenStatusIsUpdatedToCompleted() {
        gpsResultHandler.handleResult(RbsConfigLevel.GPS_WANTED_POSITION_NOT_SET.toString(), workflowVariables);
        verify(statusEntryManagerMock).taskCompleted(eq(NODE_FDN), eq(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString()),
                eq(GPS_WANTED_POSITION_NOT_SET_ADDITIONAL_INFO));
    }

    @Test
    public void whenTaskExecutes_WithUnsuccessfulMessageGpsUnavailable_thenStatusIsUpdatedToFailed() {
        gpsResultHandler.handleResult(RbsConfigLevel.GPS_POSITION_UNAVAILABLE.toString(), workflowVariables);
        verify(statusEntryManagerMock).taskFailed(eq(NODE_FDN), eq(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString()),
                eq(GPS_POSITION_UNAVAILABLE_ADDITIONAL_INFO));
    }

    @Test
    public void whenTaskExecutes_WithUnsuccessfulMessageGpsMismatch_thenStatusIsUpdatedToFailed() {
        gpsResultHandler.handleResult(RbsConfigLevel.GPS_MISMATCH_ERROR.toString(), workflowVariables);
        verify(statusEntryManagerMock).taskFailed(eq(NODE_FDN), eq(StatusEntryNames.GPS_POSITION_CHECK_TASK.toString()),
                eq(GPS_MISMATCH_ERROR_ADDITIONAL_INFO));
    }
}