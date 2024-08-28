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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util;

import static com.ericsson.oss.services.ap.arquillian.util.data.dps.model.DetachedManagedObject.Builder.newDetachedManagedObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.data.dps.model.DetachedManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.data.managedobject.OSSMosGenerator;

/**
 * Generates the required MOs for the integration workflow based on the node name.
 */
public class AutoIntegrationMosGenerator {

    private final static String INITIAL_VALUE = "1";
    private static final String FIRST_CELL = "Cell_1";
    private static final String SECOND_CELL = "Cell_2";
    private static final String THIRD_CELL = "Cell_3";

    /* Versions */
    private final static String MIM_VERSION = "10.2.150";
    private final static String OSS_TOP_VERSION = "3.0.0";
    private final static String NETWORK_ELEMENT_VERSION = "2.0.0";
    private final static String CM_NODE_HEARTBEAT_SUPERVISION_MO_VERSION = "1.0.1";
    private final static String CM_FUNCTION_MO_VERSION = "1.0.1";

    /* Namespace Model Names */
    private static final String NAMESPACE_OSS_TOP = "OSS_TOP";
    private static final String NAMESPACE_ERBS = "ERBS_NODE_MODEL";
    private static final String NAMESPACE_OSS_NE_DEF = "OSS_NE_DEF";
    private static final String NAMESPACE_OSS_NE_CM_DEF = "OSS_NE_CM_DEF";

    /* MO Types */
    private static final String TYPE_MECONTEXT = "MeContext";
    private static final String TYPE_MANAGEDELEMENT = "ManagedElement";
    private static final String TYPE_ENODEBFUNCTION = "ENodeBFunction";
    private static final String TYPE_EUTRANCELLFDD = "EUtranCellFDD";
    private static final String TYPE_EUTRANCELLTDD = "EUtranCellTDD";
    private static final String TYPE_NBIOTCELL = "NbIotCell";
    private static final String TYPE_NODEMANAGEMENTFUNCTION = "NodeManagementFunction";
    private static final String TYPE_RBSCONFIGURATION = "RbsConfiguration";
    private static final String TYPE_NETWORKELEMENT = "NetworkElement";
    private static final String TYPE_CMNODEHEARTBEATSUPERVISION = "CmNodeHeartbeatSupervision";
    private static final String TYPE_CMFUNCTION = "CmFunction";
    private static final String TYPE_SWMANAGEMENT = "SwManagement";
    private static final String TYPE_CONFIGURATIONVERSION = "ConfigurationVersion";
    private static final String TYPE_UPGRADEPACKAGE = "UpgradePackage";
    private static final String TYPE_SYSTEMFUNCTIONS = "SystemFunctions";
    private static final String TYPE_LICENSING = "Licensing";
    private static final String TYPE_OPTIONALFEATURELICENSE = "OptionalFeatureLicense";

    private static final String NETYPE_ERBS = "ERBS";
    private static final String PLATFORM_TYPE_CPP = "CPP";

    @Inject
    private Dps dpsHelper;

    /**
     * Generates the required MeContext MO and attributes as well as its children MOs for the Integration Workflow, based on the supplied Node Name.
     *
     * @param nodeName
     *            the name of the AP node
     * @return the generated MeContext ManagedObject which is the parent to all child MOs generated
     */
    public ManagedObject generateMeContextMoAndChildren(final String nodeName) {
        final ManagedObject meContextMo = generateMeContext(nodeName, null);
        generateMeContextChildMos(meContextMo, nodeName);
        return meContextMo;
    }

    /**
     * Generates the required MeContext MO and attributes as well as its children MOs for the Integration Workflow, based on the supplied Node Name.
     * <p>
     * Creates the <code>MeContext</code> with two <code>SubNetworks</code>.
     *
     * @param nodeName
     *            the name of the AP node
     * @return the generated MeContext ManagedObject which is the parent to all child MOs generated
     */
    public ManagedObject generateMeContextMoAndChildrenWithSubNetworks(final String nodeName) {
        final ManagedObject enmSnSubNetworkMo = createSubNetworkMo("EnmSn", null);
        final ManagedObject apSubNetworkMo = createSubNetworkMo("AP_1", enmSnSubNetworkMo);
        final ManagedObject meContextMo = generateMeContext(nodeName, apSubNetworkMo);
        generateMeContextChildMos(meContextMo, nodeName);

        return meContextMo;
    }

    private ManagedObject createSubNetworkMo(final String subNetworkName, final ManagedObject parentMo) {
        final Map<String, Object> subNetworkAttributes = new HashMap<>();
        subNetworkAttributes.put("SubNetworkId", subNetworkName);

        final ManagedObject subNetworkMo = newDetachedManagedObject()
                .namespace(NAMESPACE_OSS_TOP)
                .type("SubNetwork")
                .version(OSS_TOP_VERSION)
                .name(subNetworkName)
                .attributes(subNetworkAttributes)
                .parent(parentMo)
                .mibRoot(true)
                .build();

        return dpsHelper.createMo(subNetworkMo);
    }

    /**
     * Generates the required MeContext MO and attributes as well as its children MOs for the Integration Workflow, based on the supplied Node Name.
     * <p>
     * The <code>NbiotCell</code> MOs will be incorrectly created to cause failure.
     *
     * @param nodeName
     *            the name of the AP node
     * @return the generated MeContext ManagedObject which is the parent to all child MOs generated
     */
    public ManagedObject generateMeContextMoAndFlawedNbiotChildren(final String nodeName) {
        final ManagedObject generatedMeContext = generateMeContext(nodeName, null);
        generateMeContextChildMosWithFlawedNbiotCells(nodeName);
        return generatedMeContext;
    }

    /**
     * Generates the required NetworkElement MO and Attributes as well as its children MOs for the Integration Workflow, based on the supplied Node
     * Name.
     *
     * @param nodeName
     *            the name of the AP node
     * @return the generated NetworkElement ManagedObject which is the parent to all child MOs generated
     */
    public ManagedObject generateNetworkElementMoAndChildren(final String nodeName) {
        final ManagedObject generatedNetworkElement = generateNetworkElement(nodeName);
        generateNetworkElementChildMos(nodeName);

        return generatedNetworkElement;
    }

    public void generateConnectivityInfo(final String networkElementName, final String version, final String namespace,
            final String connectivityInformationMoType) {
        final ManagedObject networkElement = dpsHelper.findMoByFdn(String.format("NetworkElement=%s", new Object[] { networkElementName }));
        final HashMap<String, Object> connectivityAttributes = new HashMap<>();
        connectivityAttributes.put("port", Integer.valueOf(80));
        connectivityAttributes.put("ipAddress", "1.1.1.1");
        final DetachedManagedObject connectivityInfo = DetachedManagedObject.Builder.newDetachedManagedObject().mibRoot(true).parent(networkElement)
                .name("1").namespace(namespace).version(version).type(connectivityInformationMoType).attributes(connectivityAttributes).build();
        dpsHelper.createMo(connectivityInfo);
    }

    /**
     * Generates the required MeContext child MOs for the AutoIntegration Workflow, based on the supplied Node Name.
     *
     * @param nodeName
     *            the name of the AP node
     */
    public void generateMeContextChildMos(final ManagedObject meContextMo, final String nodeName) {
        final ManagedObject generatedManagedElement = generateManagedElementMo(meContextMo);
        final ManagedObject generatedNodeManagementFunction = generateNodeManagementFunction(generatedManagedElement);

        generateRbsConfiguration(generatedNodeManagementFunction);

        final ManagedObject generatedENodeBFunction = generateENodeBFunction(generatedManagedElement);

        generateEUtranCellFDD(generatedENodeBFunction);
        generateEUtranCellTDD(generatedENodeBFunction);
        generateNbIotCell(generatedENodeBFunction);

        // MOs for Create CV and Upload CV tasks
        final ManagedObject generatedSwManagement = generateSwMangementMo(generatedManagedElement);
        generateUpgradePackageMo(generatedSwManagement);
        generateConfigurationVersionMo(generatedSwManagement);

        // MOs required for ActivateOptionalFeatures task
        final ManagedObject generatedSystemFunctions = generateSystemFunctionsMo(generatedManagedElement);
        final ManagedObject generatedLicensing = generateLicensingMo(generatedSystemFunctions);
        generateOptionalFeatureLicenseMo(generatedLicensing);
    }

    public void generateMeContextChildMosWithFlawedNbiotCells(final String nodeName) {
        final ManagedObject meContext = dpsHelper.findMoByFdn("MeContext=" + nodeName);
        final ManagedObject generatedManagedElement = generateManagedElementMo(meContext);
        final ManagedObject generatedNodeManagementFunction = generateNodeManagementFunction(generatedManagedElement);

        generateRbsConfiguration(generatedNodeManagementFunction);

        final ManagedObject generatedENodeBFunction = generateENodeBFunction(generatedManagedElement);

        generateEUtranCellFDD(generatedENodeBFunction);
        generateEUtranCellTDD(generatedENodeBFunction);
        generateFlawedNbIotCell(generatedENodeBFunction);

        // MOs for Create CV and Upload CV tasks
        final ManagedObject generatedSwManagement = generateSwMangementMo(generatedManagedElement);
        generateUpgradePackageMo(generatedSwManagement);
        generateConfigurationVersionMo(generatedSwManagement);

        // MOs required for ActivateOptionalFeatures task
        final ManagedObject generatedSystemFunctions = generateSystemFunctionsMo(generatedManagedElement);
        final ManagedObject generatedLicensing = generateLicensingMo(generatedSystemFunctions);
        generateOptionalFeatureLicenseMo(generatedLicensing);
    }

    /**
     * Generates the required NetworkElement children MOs for the AutoIntegration Workflow, based on the supplied Node Name.
     *
     * @param nodeName
     *            the name of the AP node
     */
    public void generateNetworkElementChildMos(final String nodeName) {
        final ManagedObject generatedNetworkElement = dpsHelper.findMoByFdn("NetworkElement=" + nodeName);
        generateCmNodeHeartbeatSupervision(generatedNetworkElement);
        generateCmFunction(generatedNetworkElement, Collections.<String, Object> emptyMap());
    }

    /**
     * Generates the required <code>NetworkElement</code> children MOs for the AutoIntegration Workflow, based on the supplied Node Name. And with
     * <i>syncStatus</i> set to <b>SYNCHRONIZED</b>.
     *
     * @param nodeName
     *            the name of the AP node
     */
    public void generateNetworkElementChildMosWithSync(final String nodeName) {
        final ManagedObject generatedNetworkElement = dpsHelper.findMoByFdn("NetworkElement=" + nodeName);
        generateCmNodeHeartbeatSupervision(generatedNetworkElement);
        final Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put("syncStatus", "SYNCHRONIZED");
        generateCmFunction(generatedNetworkElement, additionalAttributes);
    }

    /**
     * Generates the required <code>NetworkElement</code> children MOs except the heartBeatSupervisionMO for the AutoIntegration Workflow, based on
     * the supplied Node Name.
     *
     * @param nodeName
     *            the name of the AP node
     */
    public void generateNetworkElementChildMosWithoutCmHeartbeatSupervisionMO(final String nodeName) {
        final ManagedObject generatedNetworkElement = dpsHelper.findMoByFdn("NetworkElement=" + nodeName);
        generateCmFunction(generatedNetworkElement, Collections.<String, Object> emptyMap());
    }

    public ManagedObject generateMeContext(final String nodeName, final ManagedObject parentMo) {
        final Map<String, Object> meContextAttributes = new HashMap<>();
        meContextAttributes.put("neType", NETYPE_ERBS);
        meContextAttributes.put("platformType", PLATFORM_TYPE_CPP);

        final ManagedObject meContextMo = newDetachedManagedObject()
                .namespace(NAMESPACE_OSS_TOP)
                .type(TYPE_MECONTEXT)
                .version(OSS_TOP_VERSION)
                .name(nodeName)
                .parent(parentMo)
                .mibRoot(parentMo == null)
                .attributes(meContextAttributes)
                .build();

        return dpsHelper.createMo(meContextMo);
    }

    private ManagedObject generateManagedElementMo(final ManagedObject generatedMeContext) {
        final Map<String, Object> managedElementAttributes = new HashMap<>();
        managedElementAttributes.put("ManagedElementId", "1");
        managedElementAttributes.put("platformType", PLATFORM_TYPE_CPP);
        managedElementAttributes.put("neType", NETYPE_ERBS);

        final ManagedObject generatedManagedElementMo = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_MANAGEDELEMENT)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(managedElementAttributes)
                .parent(generatedMeContext)
                .mibRoot(true)
                .build();

        return dpsHelper.createMo(generatedManagedElementMo);
    }

    private ManagedObject generateENodeBFunction(final ManagedObject generatedManagedElement) {
        final Map<String, Object> eNodeBPlmnIdAttributes = new HashMap<>();
        eNodeBPlmnIdAttributes.put("mcc", 1);
        eNodeBPlmnIdAttributes.put("mnc", 1);
        eNodeBPlmnIdAttributes.put("mncLength", 2);

        final Map<String, Object> eNodeBFunctionAttributes = new HashMap<>();
        eNodeBFunctionAttributes.put("ENodeBFunctionId", "1");
        eNodeBFunctionAttributes.put("eNodeBPlmnId", eNodeBPlmnIdAttributes);

        final ManagedObject generatedENodeBFunction = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_ENODEBFUNCTION)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(eNodeBFunctionAttributes)
                .parent(generatedManagedElement)
                .mibRoot(true)
                .build();

        return dpsHelper.createMo(generatedENodeBFunction);
    }

    private void generateEUtranCellFDD(final ManagedObject generatedENodeBFunction) {
        final Map<String, Object> eUtranCellFDDAttributes = new HashMap<>();
        eUtranCellFDDAttributes.put("EUtranCellFDDId", "1");
        eUtranCellFDDAttributes.put("physicalLayerSubCellId", 1);
        eUtranCellFDDAttributes.put("earfcnul", 19000);
        eUtranCellFDDAttributes.put("cellId", 1);
        eUtranCellFDDAttributes.put("tac", 1);
        eUtranCellFDDAttributes.put("physicalLayerCellIdGroup", 40);
        eUtranCellFDDAttributes.put("earfcndl", 1000);

        final ManagedObject generatedEUtranCellFDD = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_EUTRANCELLFDD)
                .version(MIM_VERSION)
                .name(FIRST_CELL)
                .attributes(eUtranCellFDDAttributes)
                .parent(generatedENodeBFunction)
                .mibRoot(false)
                .build();

        dpsHelper.createMo(generatedEUtranCellFDD);
    }

    private void generateEUtranCellTDD(final ManagedObject generatedENodeBFunction) {
        final Map<String, Object> eUtranCellTDDAttributes = new HashMap<>();
        eUtranCellTDDAttributes.put("EUtranCellTDDId", "1");
        eUtranCellTDDAttributes.put("physicalLayerSubCellId", 1);
        eUtranCellTDDAttributes.put("cellId", 1);
        eUtranCellTDDAttributes.put("tac", 1);
        eUtranCellTDDAttributes.put("physicalLayerCellIdGroup", 40);
        eUtranCellTDDAttributes.put("earfcn", 36000);
        eUtranCellTDDAttributes.put("subframeAssignment", 1);

        final ManagedObject generatedEUtranCellTDD = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_EUTRANCELLTDD)
                .version(MIM_VERSION)
                .name(SECOND_CELL)
                .attributes(eUtranCellTDDAttributes)
                .parent(generatedENodeBFunction)
                .mibRoot(false)
                .build();

        dpsHelper.createMo(generatedEUtranCellTDD);
    }

    private void generateNbIotCell(final ManagedObject generatedENodeBFunction) {
        final Map<String, Object> nbIotCellAttributes = new HashMap<>();
        final Map<String, Object> plmnIds = new HashMap<>();
        plmnIds.put("mcc", 2);
        plmnIds.put("mnc", 3);
        plmnIds.put("mncLength", 2);
        final List<Map<String, Object>> plmnIdList = new ArrayList<>();
        plmnIdList.add(plmnIds);
        nbIotCellAttributes.put("cellId", 2);
        nbIotCellAttributes.put("earfcndl", 17000);
        nbIotCellAttributes.put("NbIotCellId", "1");
        nbIotCellAttributes.put("nbIotCellType", "NBIOT_INBAND");
        nbIotCellAttributes.put("physicalLayerCellId", 1);
        nbIotCellAttributes.put("plmnIdList", plmnIdList);
        nbIotCellAttributes.put("tac", 1);
        nbIotCellAttributes.put("eutranCellRef", (generatedENodeBFunction + "EUtranCellFDD=1"));

        final ManagedObject generatedNbIotCell = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_NBIOTCELL)
                .version(MIM_VERSION)
                .name(THIRD_CELL)
                .attributes(nbIotCellAttributes)
                .parent(generatedENodeBFunction)
                .mibRoot(false)
                .build();

        dpsHelper.createMo(generatedNbIotCell);
    }

    private void generateFlawedNbIotCell(final ManagedObject generatedENodeBFunction) {
        final Map<String, Object> nbIotCellAttributes = new HashMap<>();
        final Map<String, Object> plmnIds = new HashMap<>();
        plmnIds.put("mcc", 2);
        plmnIds.put("mnc", 3);
        plmnIds.put("mncLength", 2);
        final List<Map<String, Object>> plmnIdList = new ArrayList<>();
        plmnIdList.add(plmnIds);
        nbIotCellAttributes.put("cellId", 2);
        nbIotCellAttributes.put("earfcndl", 17000);
        nbIotCellAttributes.put("NbIotCellId", "1");
        nbIotCellAttributes.put("nbIotCellType", "NBIOT_INBAND");
        nbIotCellAttributes.put("plmnIdList", plmnIdList);
        nbIotCellAttributes.put("physicalLayerCellId", 1);
        nbIotCellAttributes.put("tac", 1);

        final ManagedObject generatedNbIotCell = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_NBIOTCELL)
                .version(MIM_VERSION)
                .name(THIRD_CELL)
                .attributes(nbIotCellAttributes)
                .parent(generatedENodeBFunction)
                .mibRoot(false)
                .build();

        dpsHelper.createMo(generatedNbIotCell);
    }

    public ManagedObject generateNetworkElement(final String nodeName) {
        final Map<String, Object> networkElementAttributes = new HashMap<>();
        networkElementAttributes.put("neType", NETYPE_ERBS);
        networkElementAttributes.put("platformType", PLATFORM_TYPE_CPP);
        networkElementAttributes.put("ossModelIdentity", OSSMosGenerator.ERBS_OSS_MODEL_IDENTITY);

        final ManagedObject networkElementMo = newDetachedManagedObject()
                .namespace(NAMESPACE_OSS_NE_DEF)
                .type(TYPE_NETWORKELEMENT)
                .version(NETWORK_ELEMENT_VERSION)
                .name(nodeName)
                .mibRoot(true)
                .parent(null)
                .attributes(networkElementAttributes)
                .build();
        return dpsHelper.createMo(networkElementMo);
    }

    private void generateCmNodeHeartbeatSupervision(final ManagedObject generatedNetworkElement) {
        final Map<String, Object> cmNodeHeartbeatSupervisionAttributes = new HashMap<>();
        cmNodeHeartbeatSupervisionAttributes.put("CmNodeHeartbeatSupervisionId", "1");
        cmNodeHeartbeatSupervisionAttributes.put("active", false);

        final ManagedObject generatedCmHeartbeatSupervision = newDetachedManagedObject()
                .namespace(NAMESPACE_OSS_NE_CM_DEF)
                .type(TYPE_CMNODEHEARTBEATSUPERVISION)
                .version(CM_NODE_HEARTBEAT_SUPERVISION_MO_VERSION)
                .name(INITIAL_VALUE)
                .attributes(cmNodeHeartbeatSupervisionAttributes)
                .parent(generatedNetworkElement)
                .mibRoot(true)
                .build();

        dpsHelper.createMo(generatedCmHeartbeatSupervision);
    }

    private void generateCmFunction(final ManagedObject generatedNetworkElement, final Map<String, Object> additionalAttributes) {
        final Map<String, Object> cmFunctionAttributes = new HashMap<>();
        cmFunctionAttributes.put("CmFunctionId", "1");
        cmFunctionAttributes.putAll(additionalAttributes);

        final ManagedObject generatedCmFunction = newDetachedManagedObject()
                .namespace(NAMESPACE_OSS_NE_CM_DEF)
                .type(TYPE_CMFUNCTION)
                .version(CM_FUNCTION_MO_VERSION)
                .name(INITIAL_VALUE)
                .attributes(cmFunctionAttributes)
                .parent(generatedNetworkElement)
                .mibRoot(true)
                .build();

        dpsHelper.createMo(generatedCmFunction);
    }

    private ManagedObject generateNodeManagementFunction(final ManagedObject createdManagedElement) {
        final Map<String, Object> nodeManagementFunctionAttributes = new HashMap<>();
        nodeManagementFunctionAttributes.put("NodeManagementFunctionId", "1");

        final ManagedObject generatedNodeManagementFunction = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_NODEMANAGEMENTFUNCTION)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(nodeManagementFunctionAttributes)
                .parent(createdManagedElement)
                .mibRoot(true)
                .build();

        return dpsHelper.createMo(generatedNodeManagementFunction);
    }

    private void generateRbsConfiguration(final ManagedObject generatedNodeManagementFunction) {
        final Map<String, Object> rbsConfigurationAttributes = new HashMap<>();
        rbsConfigurationAttributes.put("RbsConfigurationId", "1");

        final ManagedObject generatedRbsConfiguration = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_RBSCONFIGURATION)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(rbsConfigurationAttributes)
                .parent(generatedNodeManagementFunction)
                .mibRoot(false)
                .build();

        dpsHelper.createMo(generatedRbsConfiguration);
    }

    private ManagedObject generateSwMangementMo(final ManagedObject managedElementMo) {
        final Map<String, Object> swManagementAttributes = new HashMap<>();
        swManagementAttributes.put("SwManagementId", "1");

        final ManagedObject generatedSwManagement = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_SWMANAGEMENT)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(swManagementAttributes)
                .parent(managedElementMo)
                .mibRoot(false)
                .build();

        return dpsHelper.createMo(generatedSwManagement);
    }

    private ManagedObject generateConfigurationVersionMo(final ManagedObject swManagementMo) {
        final Map<String, Object> configurationVersionAttributes = new HashMap<>();
        configurationVersionAttributes.put("ConfigurationVersionId", "1");

        final ManagedObject generatedConfigurationVersion = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_CONFIGURATIONVERSION)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(configurationVersionAttributes)
                .parent(swManagementMo)
                .mibRoot(false)
                .build();

        return dpsHelper.createMo(generatedConfigurationVersion);
    }

    private ManagedObject generateUpgradePackageMo(final ManagedObject swManagementMo) {
        final Map<String, Object> upgradePackageAttributes = new HashMap<>();
        upgradePackageAttributes.put("UpgradePackageId", "1");
        upgradePackageAttributes.put("ftpServerIpAddress", "1.1.1.1");
        upgradePackageAttributes.put("upFilePathOnFtpServer", "/path/to/upgradepkg");
        upgradePackageAttributes.put("user", "user");
        upgradePackageAttributes.put("password", "password");

        final ManagedObject generatedConfigurationVersion = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_UPGRADEPACKAGE)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(upgradePackageAttributes)
                .parent(swManagementMo)
                .mibRoot(false)
                .build();

        return dpsHelper.createMo(generatedConfigurationVersion);
    }

    private ManagedObject generateSystemFunctionsMo(final ManagedObject managedElementMo) {
        final Map<String, Object> systemFunctionsAttributes = new HashMap<>();
        systemFunctionsAttributes.put("SystemFunctionsId", "1");

        final ManagedObject generatedSystemFunctions = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_SYSTEMFUNCTIONS)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(systemFunctionsAttributes)
                .parent(managedElementMo)
                .mibRoot(false)
                .build();

        return dpsHelper.createMo(generatedSystemFunctions);
    }

    private ManagedObject generateLicensingMo(final ManagedObject systemFunctionsMo) {
        final Map<String, Object> licensingAttributes = new HashMap<>();
        licensingAttributes.put("LicensingId", "1");

        final ManagedObject generatedOptionalFeatures = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_LICENSING)
                .version(MIM_VERSION)
                .name(INITIAL_VALUE)
                .attributes(licensingAttributes)
                .parent(systemFunctionsMo)
                .mibRoot(false)
                .build();

        return dpsHelper.createMo(generatedOptionalFeatures);
    }

    private ManagedObject generateOptionalFeatureLicenseMo(final ManagedObject licensingMo) {
        final Map<String, Object> optionalFeatureLicenseAttributes = new HashMap<>();
        optionalFeatureLicenseAttributes.put("OptionalFeatureLicenseId", "BestNeighborSI");
        optionalFeatureLicenseAttributes.put("keyId", "BestNeighborSI");

        final ManagedObject generatedOptionalFeatures = newDetachedManagedObject()
                .namespace(NAMESPACE_ERBS)
                .type(TYPE_OPTIONALFEATURELICENSE)
                .version(MIM_VERSION)
                .name("BestNeighborSI")
                .attributes(optionalFeatureLicenseAttributes)
                .parent(licensingMo)
                .mibRoot(true)
                .build();

        return dpsHelper.createMo(generatedOptionalFeatures);
    }
}
