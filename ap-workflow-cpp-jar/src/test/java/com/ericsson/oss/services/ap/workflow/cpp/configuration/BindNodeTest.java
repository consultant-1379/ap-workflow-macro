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
package com.ericsson.oss.services.ap.workflow.cpp.configuration;

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.HARDWARE_SERIAL_NUMBER_VALUE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_FDN;
import static com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType.SITEINSTALLFORBIND;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.api.resource.ResourceService;
import com.ericsson.oss.services.ap.api.status.StateTransitionEvent;
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.model.NodeArtifactAttribute;
import com.ericsson.oss.services.ap.common.model.NodeAttribute;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.ArtifactsHandler;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Unit tests for {@link BindNode}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BindNodeTest {

    private static final String GENERATED_LOCATION = "dummyFilePath";
    private static final String NODE_ARTIFACT_CONTAINER_FDN = NODE_FDN + ",NodeArtifactContainer=1";

    @Mock
    private ServiceFinderBean serviceFinder;

    @Mock
    private StateTransitionManagerLocal stateTransitionManagerLocal;

    @Mock
    private ArtifactsHandler artifactsHandler;

    @Mock
    private DataPersistenceService dps;

    @Mock
    private DataBucket dataBucket;

    @Mock
    private DpsOperations dpsOperations;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ManagedObject nodeMo;

    @Mock
    private ManagedObject nodeArtifactContainerMo;

    @Mock
    private ManagedObject nodeArtifactMo;

    @Mock
    private SystemRecorder systemRecorder;

    @InjectMocks
    private BindNode bindNode;

    @Before
    public void setUp() {
        when(serviceFinder.find(StateTransitionManagerLocal.class)).thenReturn(stateTransitionManagerLocal);
        when(nodeMo.getFdn()).thenReturn(NODE_FDN);
        when(dps.getLiveBucket()).thenReturn(dataBucket);
        when(dataBucket.findMoByFdn(NODE_FDN)).thenReturn(nodeMo);
        when(dataBucket.findMoByFdn(NODE_ARTIFACT_CONTAINER_FDN)).thenReturn(nodeArtifactContainerMo);
        when(nodeArtifactMo.getAttribute(NodeArtifactAttribute.TYPE.toString())).thenReturn(ArtifactType.SITEINSTALLFORBIND.toString());
    }

    @Test
    public void whenNodeIsInOrderStartedState_thenNewBindFileIsCreated() {
        bindNode.executeBindDuringOrder(NODE_FDN, CppNodeType.ERBS);
        verify(artifactsHandler).createGeneratedArtifact(SITEINSTALLFORBIND.toString(), NODE_FDN, CppNodeType.ERBS);
        verify(resourceService, never()).delete(GENERATED_LOCATION);
    }

    @Test(expected = ApServiceException.class)
    public void whenNodeIsInOrderStartedState_andBindFails_thenApServiceExceptionIsThrown() {
        doThrow(ApApplicationException.class).when(artifactsHandler).createGeneratedArtifact(SITEINSTALLFORBIND.toString(), NODE_FDN,
                CppNodeType.ERBS);
        bindNode.executeBindDuringOrder(NODE_FDN, CppNodeType.ERBS);
    }

    @Test
    public void whenNodeIsInOrderCompletedState_thenNewBindFileIsCreated_andSerialNumberIsUpdated_andOldBindFileIsDeleted_andStateIsBindCompleted() {
        final Map<String, Object> moAttributes = new HashMap<>();
        moAttributes.put("hardwareSerialNumber", HARDWARE_SERIAL_NUMBER_VALUE);
        when(dpsOperations.readMoAttributes(NODE_FDN)).thenReturn(moAttributes);
        final List<ManagedObject> nodeArtifactMos = new ArrayList<>();
        nodeArtifactMos.add(nodeArtifactMo);

        when(nodeArtifactMo.getAttribute(NodeArtifactAttribute.GEN_LOCATION.toString())).thenReturn(GENERATED_LOCATION);
        when(nodeArtifactContainerMo.getChildren()).thenReturn(nodeArtifactMos);

        bindNode.executeManualBind(NODE_FDN, HARDWARE_SERIAL_NUMBER_VALUE, CppNodeType.ERBS);

        final InOrder inOrder = inOrder(artifactsHandler, dpsOperations, stateTransitionManagerLocal);
        inOrder.verify(dpsOperations).updateMo(NODE_FDN, NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(), HARDWARE_SERIAL_NUMBER_VALUE);
        inOrder.verify(artifactsHandler).updateGeneratedArtifact(SITEINSTALLFORBIND.toString(), NODE_FDN, CppNodeType.ERBS);
        inOrder.verify(stateTransitionManagerLocal).validateAndSetNextState(NODE_FDN, StateTransitionEvent.BIND_SUCCESSFUL);
    }

    @Test(expected = ApServiceException.class)
    public void whenNodeIsInOrderCompletedState_andExceptionThrownCreatingNewBindFile_thenSerialNumberIsNotUpdated_andStateIsBindFailed_andApServiceExceptionIsThrown() {
        final Map<String, Object> moAttributes = new HashMap<>();
        moAttributes.put("hardwareSerialNumber", HARDWARE_SERIAL_NUMBER_VALUE);
        when(dpsOperations.readMoAttributes(NODE_FDN)).thenReturn(moAttributes);
        final List<ManagedObject> nodeArtifactMos = new ArrayList<>();
        nodeArtifactMos.add(nodeArtifactMo);
        when(nodeMo.getFdn()).thenReturn(NODE_FDN);
        when(nodeArtifactMo.getAttribute(NodeArtifactAttribute.GEN_LOCATION.toString())).thenReturn(GENERATED_LOCATION);
        when(nodeArtifactContainerMo.getChildren()).thenReturn(nodeArtifactMos);
        doThrow(ApApplicationException.class).when(artifactsHandler).updateGeneratedArtifact(SITEINSTALLFORBIND.toString(), NODE_FDN,
                CppNodeType.ERBS);

        bindNode.executeManualBind(NODE_FDN, HARDWARE_SERIAL_NUMBER_VALUE, CppNodeType.ERBS);

        verify(stateTransitionManagerLocal).validateAndSetNextState(NODE_FDN, StateTransitionEvent.BIND_FAILED);
    }

    @Test(expected = ApServiceException.class)
    public void whenNodeIsInOrderCompletedState_andExceptionThrownUpdatingSerialNumber_thenOldBindFileIsNotDeleted_andStateIsBindFailed_andApServiceExceptionIsThrown() {
        final Map<String, Object> moAttributes = new HashMap<>();
        moAttributes.put("hardwareSerialNumber", HARDWARE_SERIAL_NUMBER_VALUE);
        when(dpsOperations.readMoAttributes(NODE_FDN)).thenReturn(moAttributes);
        final List<ManagedObject> nodeArtifactMos = new ArrayList<>();
        nodeArtifactMos.add(nodeArtifactMo);
        when(nodeArtifactMo.getAttribute(NodeArtifactAttribute.GEN_LOCATION.toString())).thenReturn(GENERATED_LOCATION);
        when(nodeArtifactContainerMo.getChildren()).thenReturn(nodeArtifactMos);
        doThrow(ApApplicationException.class).when(dpsOperations).updateMo(NODE_FDN, NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(),
                HARDWARE_SERIAL_NUMBER_VALUE);

        bindNode.executeManualBind(NODE_FDN, HARDWARE_SERIAL_NUMBER_VALUE, CppNodeType.ERBS);

        verify(stateTransitionManagerLocal).validateAndSetNextState(NODE_FDN, StateTransitionEvent.BIND_FAILED);
        verify(artifactsHandler, never()).updateGeneratedArtifact(SITEINSTALLFORBIND.toString(), NODE_FDN, CppNodeType.ERBS);
    }

    @Test(expected = ApServiceException.class)
    public void whenNodeIsInOrderCompletedState_andErrorOccursWhenGeneratingBindFile_thenHardwareSerialNumberIsSetToOriginalValue_andApServiceExceptionIsThrown() {
        final Map<String, Object> moAttributes = new HashMap<>();
        final String originalHardwareSerialNumberValue = HARDWARE_SERIAL_NUMBER_VALUE + "1";
        moAttributes.put("hardwareSerialNumber", originalHardwareSerialNumberValue);
        when(dpsOperations.readMoAttributes(NODE_FDN)).thenReturn(moAttributes);
        final List<ManagedObject> nodeArtifactMos = new ArrayList<>();
        nodeArtifactMos.add(nodeArtifactMo);
        when(nodeArtifactMo.getAttribute(NodeArtifactAttribute.GEN_LOCATION.toString())).thenReturn(GENERATED_LOCATION);
        when(nodeArtifactContainerMo.getChildren()).thenReturn(nodeArtifactMos);
        doThrow(ApApplicationException.class).when(artifactsHandler).updateGeneratedArtifact(SITEINSTALLFORBIND.toString(), NODE_FDN,
                CppNodeType.ERBS);

        bindNode.executeManualBind(NODE_FDN, HARDWARE_SERIAL_NUMBER_VALUE, CppNodeType.ERBS);

        verify(stateTransitionManagerLocal).validateAndSetNextState(NODE_FDN, StateTransitionEvent.BIND_FAILED);
        verify(dpsOperations).updateMo(NODE_FDN, NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(), HARDWARE_SERIAL_NUMBER_VALUE);
        verify(dpsOperations).updateMo(NODE_FDN, NodeAttribute.HARDWARE_SERIAL_NUMBER.toString(), originalHardwareSerialNumberValue);
    }
}
