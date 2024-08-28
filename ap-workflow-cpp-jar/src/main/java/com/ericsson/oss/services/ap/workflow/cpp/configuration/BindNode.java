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

import static com.ericsson.oss.services.ap.api.status.StateTransitionEvent.BIND_FAILED;
import static com.ericsson.oss.services.ap.api.status.StateTransitionEvent.BIND_SUCCESSFUL;
import static com.ericsson.oss.services.ap.common.model.NodeAttribute.HARDWARE_SERIAL_NUMBER;
import static com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType.SITEINSTALLFORBIND;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.api.status.StateTransitionManagerLocal;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.ap.common.model.NodeAttribute;
import com.ericsson.oss.services.ap.common.util.string.FDN;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.artifacts.ArtifactsHandler;

/**
 * Binds the node to associated Hardware Serial Number, using the ENM AP command:
 *
 * <pre>
 * ap bind -n &lt;nodeName&gt; -s &lt;hardwareSerialNumber&gt;
 * </pre>
 */
public class BindNode {

    private static final String EARLY_BIND_TYPE = "Early";
    private static final String LATE_BIND_TYPE = "Late";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private StateTransitionManagerLocal stateTransitionManagerLocal;

    @Inject
    private ArtifactsHandler artifactsHandler;

    @Inject
    private DpsOperations dpsOperations;

    @Inject
    private SystemRecorder systemRecorder;

    @PostConstruct
    public void init() {
        stateTransitionManagerLocal = new ServiceFinderBean().find(StateTransitionManagerLocal.class);
    }

    /**
     * Binds the node to the provided hardware serial number. The bind will not update node state when the node is during order.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param nodeType
     *            the node type
     */
    public void executeBindDuringOrder(final String apNodeFdn, final CppNodeType nodeType) {
        logger.info("Executing bind during order for node {}", apNodeFdn);

        try {
            ddpCaptureEventForBind(apNodeFdn, nodeType.toString(), EARLY_BIND_TYPE);
            artifactsHandler.createGeneratedArtifact(SITEINSTALLFORBIND.toString(), apNodeFdn, nodeType);
            logger.info("Bind during order completed successfully for node {}", apNodeFdn);
        } catch (final Exception e) {
            throw new ApServiceException(e.getMessage(), e);
        }
    }

    /**
     * Binds the node to the provided hardware serial number. The node state will be updated to <b>BIND_COMPLETED</b> or <b>BIND_FAILED</b> depending
     * on the success of the bind.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param hardwareSerialNumber
     *            the hardware serial number of the physical node
     * @param nodeType
     *            the type of CPP node to bind
     */
    public void executeManualBind(final String apNodeFdn, final String hardwareSerialNumber, final CppNodeType nodeType) {
        logger.info("Executing bind for node {}, using hardwareSerialNumber {}", apNodeFdn, hardwareSerialNumber);
        final String currentHardwareSerialNumber = getCurrentHardwareSerialNumber(apNodeFdn);

        try {
            ddpCaptureEventForBind(apNodeFdn, nodeType.toString(), LATE_BIND_TYPE);
            updateMoWithHardwareSerialNumberInNewTx(apNodeFdn, hardwareSerialNumber);
        } catch (final Exception e) {
            handleError(hardwareSerialNumber, apNodeFdn, e);
        }
        try {
            artifactsHandler.updateGeneratedArtifact(SITEINSTALLFORBIND.toString(), apNodeFdn, nodeType);
            logger.info("Bind completed successfully with <state> updated to BIND_COMPLETED for node {}, using hardwareSerialNumber {}", apNodeFdn,
                    hardwareSerialNumber);
        } catch (final Exception e) {
            updateMoWithHardwareSerialNumberInNewTx(apNodeFdn, currentHardwareSerialNumber);
            handleError(hardwareSerialNumber, apNodeFdn, e);
        }

        stateTransitionManagerLocal.validateAndSetNextState(apNodeFdn, BIND_SUCCESSFUL);
    }

    private String getCurrentHardwareSerialNumber(final String apNodeFdn) {
        final Map<String, Object> moAttributes = dpsOperations.readMoAttributes(apNodeFdn);
        final Object hardwareSerialNumberRaw = moAttributes.get(NodeAttribute.HARDWARE_SERIAL_NUMBER.toString());
        return hardwareSerialNumberRaw == null ? "" : hardwareSerialNumberRaw.toString();
    }

    private void handleError(final String hardwareSerialNumber, final String apNodeFdn, final Exception e) {
        stateTransitionManagerLocal.validateAndSetNextState(apNodeFdn, BIND_FAILED);
        logger.warn("Bind failed with <state> retained at ORDER_COMPLETED for node {}, using hardwareSerialNumber {}", apNodeFdn,
                hardwareSerialNumber, e);
        throw new ApServiceException(e.getMessage(), e);
    }

    private void updateMoWithHardwareSerialNumberInNewTx(final String apNodeFdn, final String serialNumber) {
        dpsOperations.updateMo(apNodeFdn, HARDWARE_SERIAL_NUMBER.toString(), serialNumber);
    }

    private void ddpCaptureEventForBind(final String apNodeFdn, final String nodeType, final String bindType){
        final String nodeName = FDN.get(apNodeFdn).getRdnValue();
        final String projectName = FDN.get(apNodeFdn).getRoot().split("=")[1];

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put("PROJECT_NAME", projectName);
        eventData.put("NODE_NAME", nodeName);
        eventData.put("NODE_TYPE", nodeType);
        eventData.put("BIND_TYPE", bindType);
        systemRecorder.recordEventData("INTEGRATION_BIND", eventData);
    }
}
