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

import java.util.Collections;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.backupservice.remote.api.ConfigurationVersionManagementServiceRemote;

/**
 * Manager class used to create and upload Configuration Versions (CVs). CVs are node backups that can be used to restore the node to an earlier state
 * if the node needs to be reinstalled.
 */
public class CvManager {

    private ConfigurationVersionManagementServiceRemote cvManagementService;

    @Inject
    private DpsOperations dpsOperations;

    @Inject
    private Logger logger;

    @PostConstruct
    public void init() {
        cvManagementService = new ServiceFinderBean().find(ConfigurationVersionManagementServiceRemote.class);
    }

    /**
     * Uploads a Configuration Version (CV) to SHM.
     *
     * @param nodeName
     *            the name of the AP node
     * @param cvName
     *            the name of the CV to upload
     * @return true if the upload was successful
     * @throws CVOperationRemoteException
     *             if there is an error uploading the CV
     */
    public boolean uploadCv(final String nodeName, final String cvName) throws CVOperationRemoteException {
        return cvManagementService.uploadCV(nodeName, cvName);
    }

    /**
     * Creates a Configuration Version (CV) for the node. Sets the CV to startable and then sets it to first in the rollback list.
     *
     * @param nodeName
     *            the name of the AP node
     * @param meContextFdn
     *            the FDN of the MeContext for the node
     * @param cvComment
     *            a comment describing the CV
     * @return the created CV's name
     */
    public String createCv(final String nodeName, final String meContextFdn, final String cvComment) {
        final Map<String, String> administrativeData = getUpgradePackageAdministrativeData(meContextFdn);
        final String cvName = CvNameGenerator.generateCvName(administrativeData);

        createCvUsingManagementService(nodeName, cvName, cvComment);
        setCvStartable(nodeName, cvName);
        setCvFirstInRollbackList(nodeName, cvName);

        return cvName;
    }

    private Map<String, String> getUpgradePackageAdministrativeData(final String meContextFdn) {
        final String configurationVersionFdn = meContextFdn + ",ManagedElement=1,SwManagement=1,ConfigurationVersion=1";
        final ManagedObject configurationVersionMo = readMo(configurationVersionFdn);

        if (configurationVersionMo != null) {
            final String upgradePackageFdn = configurationVersionMo.getAttribute("currentUpgradePackage");
            final ManagedObject upgradePackageMo = readMo(upgradePackageFdn);

            if (upgradePackageMo != null) {
                final Map<String, String> administrativeData = upgradePackageMo.getAttribute("administrativeData");
                if (administrativeData != null) {
                    return administrativeData;
                }
            }
        }
        logger.info("No upgrade package product number/revision found for node {}", meContextFdn);
        return Collections.<String, String> emptyMap();
    }

    private ManagedObject readMo(final String fdn) {
        return StringUtils.isEmpty(fdn) ? null : dpsOperations.getDataPersistenceService().getLiveBucket().findMoByFdn(fdn);
    }

    private void createCvUsingManagementService(final String nodeName, final String cvName, final String cvComment) {
        boolean createCvResult;
        try {
            createCvResult = cvManagementService.createCV(nodeName, cvName, cvName, cvComment);
        } catch (final CVOperationRemoteException e) {
            throw new ApServiceException(String.format("Error creating CV %s: %s", cvName, e.getMessage()), e);
        }

        if (!createCvResult) {
            throw new ApServiceException("Error creating CV " + cvName);
        }
    }

    private void setCvStartable(final String nodeName, final String cvName) {
        boolean setStartableResult;
        try {
            setStartableResult = cvManagementService.setStartableCV(nodeName, cvName);
        } catch (final CVOperationRemoteException e) {
            throw new ApServiceException(String.format("Error setting CV %s startable: %s", cvName, e.getMessage()), e);
        }

        if (!setStartableResult) {
            throw new ApServiceException(String.format("Error setting CV %s startable", cvName));
        }
    }

    private void setCvFirstInRollbackList(final String nodeName, final String cvName) {
        boolean setCvFirstRollbackResult;
        try {
            setCvFirstRollbackResult = cvManagementService.setCVFirstInRollBackList(nodeName, cvName);
        } catch (final CVOperationRemoteException e) {
            throw new ApServiceException(String.format("Error setting CV %s first in rollback list: %s", cvName, e.getMessage()), e);
        }

        if (!setCvFirstRollbackResult) {
            throw new ApServiceException(String.format("Error setting CV %s first in rollback list", cvName));
        }
    }
}
