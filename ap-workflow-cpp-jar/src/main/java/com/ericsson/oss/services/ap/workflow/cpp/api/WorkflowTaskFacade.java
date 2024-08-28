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
package com.ericsson.oss.services.ap.workflow.cpp.api;

import java.util.List;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.ap.api.model.DhcpConfiguration;
import com.ericsson.oss.services.ap.api.status.StatusEntryNames;
import com.ericsson.oss.services.ap.common.model.SupervisionMoType;
import com.ericsson.oss.services.ap.workflow.cpp.model.RbsConfigLevel;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.ImportLicenseRemoteResponse;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.DeleteLicenseException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.ImportLicenseException;

/**
 * Facade to handle workflow order and integration tasks on a node.
 */
@EService
@Remote
public interface WorkflowTaskFacade {

    /**
     * Validates the BulkCM configuration files of a specified AP project.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     */
    void validateBulkConfiguration(final String apNodeFdn);

    /**
     * Creates the <code>NetworkElement</code> and <code>CppConnectivityInformation</code> MOs.
     * <p>
     * A mediation flow will be triggered to create other child MOs once the <code>CppConnectivityInformation</code> MO is created.
     *
     * @param apNodeFdn
     *            the FDN of AP node
     */
    void addNode(final String apNodeFdn);

    /**
     * Removes the <code>NetworkElement</code> MO, including all children.
     *
     * @param nodeName
     *            the name (RDN) of the AP node
     */
    void removeNode(final String nodeName);

    /**
     * Binds the node during order in a new transaction.
     *
     * @param apNodeFdn
     *            the FDN of AP node
     * @param hardwareSerialNumber
     *            the hardware serial number to be associated with the node
     * @param nodeType
     *            the CPP node type, e.g ERBS, RBS, etc..
     */
    void bindNodeDuringOrder(final String apNodeFdn, final String hardwareSerialNumber, final CppNodeType nodeType);

    /**
     * Binds the node manually in a new transaction.
     *
     * @param apNodeFdn
     *            the FDN of AP node
     * @param hardwareSerialNumber
     *            the hardware serial number to be associated with the node
     * @param nodeType
     *            the CPP node type, e.g ERBS, RBS, etc..
     */
    void bindNodeManually(final String apNodeFdn, final String hardwareSerialNumber, final CppNodeType nodeType);

    /**
     * Resets the hardware serial number and deletes the site installation file for bind.
     *
     * @param apNodeFdn
     *            the FDN of AP node
     */
    void unbindNode(final String apNodeFdn);

    /**
     * Performs sync action for the specified node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     */
    void syncNode(final String apNodeFdn);

    /**
     * Enables the specified supervision for the specified node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param supervisionMoType
     *            one of {@link SupervisionMoType#FM}, {@link SupervisionMoType#PM} {@link SupervisionMoType#INVENTORY}
     */
    void enableSupervision(final String apNodeFdn, final SupervisionMoType supervisionMoType);

    /**
     * Creates a CV on the node. The created CV is set as startable and added to first in the rollback list. The name of the created cv is set to
     * 'product number/revision' of the current upgrade package.
     * <p>
     * If no upgrade package found then the name defaults to <code>ProductNo_ProductRev_%date%</code>.
     *
     * @param apNodeFdn
     *            the FDN of the AP Node
     * @param meContextFdn
     *            the MeContext FDN
     * @param cvComment
     *            the comment to be applied to the created cv
     * @return the name of the created CV
     */
    String createCV(final String apNodeFdn, final String meContextFdn, final String cvComment);

    /**
     * Asynchronously uploads the CV from the node. Also updates the statusEntry on completion or failure.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param cvName
     *            the name of the CV on the node
     */
    void uploadCV(final String apNodeFdn, final String cvName);

    /**
     * Unlocks all cells for the specified node. Sets the <i>rbsConfigLevel</i> to <b>UNLOCKING_CELLS</b> prior to unlocking them. If at least one of
     * the cells are successfully unlocked then the <i>rbsConfigLevel</i> is set to <b>CELLS_UNLOCKED</b>. In case all cells fail to be unlocked then
     * the <i>rbsConfigLevel</i> is set to <b>UNLOCKING_CELLS_FAILED</b>.
     * <p>
     * An alarm is sent to report any errors during unlocking of the cells.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param meContextFdn
     *            the MeContext FDN of the node
     */
    void unlockCells(final String apNodeFdn, final String meContextFdn);

    /**
     * Asynchronously activates all optional features for the specified node. Sets the <i>rbsConfigLevel</i> to <b>ACTIVATING_FEATURES</b> prior to
     * activating the features. If at least one of the features is successfully activated then <i>rbsConfigLevel</i> is set to
     * <b>FEATURES_ACTIVATED</b>. <i>rbsConfigLevel</i> is set to <b>ACTIVATING_FEATURES_FAILED</b> only in case activation fails for all features.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param meContextFdn
     *            the MeContext FDN of the node
     */
    void activateOptionalFeatures(final String apNodeFdn, final String meContextFdn);

    /**
     * Updates the <i>rbsConfigLevel</i> attribute of the <code>RbsConfiguration</code> MO. Will retry a maximum of 5 times if setting of the
     * attribute fails for any reason.
     *
     * @param meContextFdn
     *            the MeContext FDN of the node
     * @param rbsConfigLevel
     *            the updated rbsConfigLevel attribute value
     */
    void updateRbsConfigLevel(final String meContextFdn, final RbsConfigLevel rbsConfigLevel);

    /**
     * Asynchronously executes import of the configurations and notifies the workflow on completion. Also updates the
     * {@link StatusEntryNames#IMPORT_CONFIGURATIONS_TASK} statusEntry on completion or failure.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param meContextFdn
     *            the MeContext FDN of the node
     * @param userId
     *            the user who started the workflow (Import service implements TBAC)
     */
    void importConfigurations(final String apNodeFdn, final String meContextFdn, final String userId);

    /**
     * Create a generated artifact of the given type for the given node.
     *
     * @param artifactType
     *            type of the artifact
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param nodeType
     *            type of Node
     */
    void createGeneratedArtifact(final String artifactType, final String apNodeFdn, final CppNodeType nodeType);

    /**
     * Upload a generated artifact of the given type for the given node.
     *
     * @param artifactType
     *            type of the artifact
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param nodeType
     *            the cppNodeType of the Node
     */
    void uploadGeneratedArtifact(final String artifactType, final String apNodeFdn, final CppNodeType nodeType);

    /**
     * Delete the generated artifact related data and database resources for the given node.
     *
     * @param artifactType
     *            type of the artifact
     * @param apNodeFdn
     *            FDN of the AP Node
     */
    void deleteGeneratedArtifact(final String artifactType, String apNodeFdn);

    /**
     * Gets the location of all raw artifacts for the node.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param artifactType
     *            type of the artifact
     * @return returns the list of strings, with the location of the artifacts
     */
    List<String> getRawArtifactsLocation(final String apNodeFdn, final String artifactType);

    /**
     * Enable/provision security for a node by generating/setting all the security related data for that node.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param cppNodeType
     *            the CPP node type, e.g ERBS, RBS, etc
     */
    void enableSecurity(final String apNodeFdn, final CppNodeType cppNodeType);

    /**
     * Cancel any ongoing security processing and remove all security related data for the given node.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param cppNodeType
     *            the CPP node type, e.g ERBS, RBS, etc
     */
    void cancelSecurity(final String apNodeFdn, final CppNodeType cppNodeType);

    /**
     * Initiates GPS position check on the node.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param meContextFdn
     *            the MeContext FDN of the node
     */
    void initiateGpsPositionCheck(final String apNodeFdn, final String meContextFdn);

    /**
     * Performs clean-up activities after successful integration.
     *
     * @param apNodeFdn
     *            FDN of the AP Node
     * @param securityEnabled
     *            is security enabled for this AP node
     * @param cppNodeType
     *            the CPP node type, e.g ERBS, RBS, etc
     */
    void cleanUpOnCompletion(final String apNodeFdn, final boolean securityEnabled, final CppNodeType cppNodeType);

    /**
     * Creates the node user credentials for the given node.
     *
     * @param apNodeFdn
     *            the FDN of AP node
     */
    void createNodeUserCredentials(final String apNodeFdn);

    /**
     * Imports license key file to shm of a specified AP project.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     *
     * @return ImportLicenseRemoteResponse
     *            the Import License Remote Response from SHM
     * @throws ImportLicenseException
     *             thrown if there is any error importing LKF
     */
    ImportLicenseRemoteResponse importLicenseKeyFile(final String apNodeFdn) throws ImportLicenseException;

    /**
     * Invokes shm interface to delete imported license key file with given fingerPrint and sequence number, in the event of order rollback.
     *
     * @param fingerPrint
     *            the fingerPrint of the node
     * @param sequenceNumber
     *            the sequenceNumber of the node
     * @param apNodeFdn
     *            the FDN of the AP node
     * @throws DeleteLicenseException
     *             thrown if there is any error deleting LKF
     */
    void deleteLicenseKeyFile(final String fingerPrint, final String sequenceNumber, final String apNodeFdn) throws DeleteLicenseException;

    /**
     * Configures the managementState for the node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param maintenanceValue
     *            the value of maintenance to be used.
     */
    void configureManagementState(final String apNodeFdn, final String maintenanceValue);

    /**
     * Creates the DHCP Configuration for the specified AP node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param oldHardwareSerialNumber
     *            previous hardware serial number of the node
     * @param dhcpConfiguration
     *            dto for DHCP service
     * @return result of creates the DHCP Configuration
     */
    boolean configureDhcp(final String apNodeFdn, final String oldHardwareSerialNumber,final DhcpConfiguration dhcpConfiguration);

    /**
     * Removes DHCP Configuration for the node.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @param hardwareSerialNumber
     *            the hardware serial number to be associated with the node
     */
    void removeDhcpClient(final String apNodeFdn, final String hardwareSerialNumber);

    /**
     * Add target to TargetGroups
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     */
    void assignTargetGroups(final String apNodeFdn);
}