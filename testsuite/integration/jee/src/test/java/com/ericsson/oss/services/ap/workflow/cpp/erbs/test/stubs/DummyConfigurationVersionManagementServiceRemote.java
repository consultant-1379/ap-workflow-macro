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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.backupservice.remote.api.ConfigurationVersionManagementServiceRemote;

/**
 * Stub EJB class for SHM interface {@link ConfigurationVersionManagementServiceRemote}.
 */
@Stateless
@EService
@Remote(ConfigurationVersionManagementServiceRemote.class)
public class DummyConfigurationVersionManagementServiceRemote implements ConfigurationVersionManagementServiceRemote {

    /**
     * Stub method for SHM interface CreateCv, that mocks successful behaviour.
     */
    @Override
    public boolean createCV(final String nodeName, final String cvName, final String cv, final String comment) throws CVOperationRemoteException {
        return true;
    }

    /**
     * Stub method for SHM interface setCVFirstInRollBackList, that mocks successful behaviour.
     */
    @Override
    public boolean setCVFirstInRollBackList(final String arg0, final String arg1) throws CVOperationRemoteException {
        return true;
    }

    /**
     * Stub method for SHM interface setStartableCV, that mocks successful behaviour.
     */
    @Override
    public boolean setStartableCV(final String arg0, final String arg1) throws CVOperationRemoteException {
        return true;
    }

    /**
     * Stub method for SHM interface uploadCV, that mocks successful behaviour.
     */
    @Override
    public boolean uploadCV(final String arg0, final String arg1) throws CVOperationRemoteException {
        return true;
    }
}
