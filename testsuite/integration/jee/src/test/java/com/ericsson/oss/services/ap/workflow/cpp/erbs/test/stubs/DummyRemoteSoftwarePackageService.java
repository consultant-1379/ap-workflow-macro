/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.BasicPackageDetails;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.PackageNotFoundException;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.RemoteSoftwarePackageService;
import com.ericsson.oss.services.shm.filestore.swpackage.remote.api.ShmServiceFailureException;

@Stateless
@EService
@Remote
public class DummyRemoteSoftwarePackageService implements RemoteSoftwarePackageService {

    private static final String DUMMY_PACKAGE_NAME = "dummy_upgrade_package";
    private static final String DUMMY_PACKAGE_NAME_HW_REPLACE = "identityDummy-revisionDummy";

    @Override
    public List<String> getSoftwarePackageDetails(final String packageName) {
        return null;

    }

    @Override
    public BasicPackageDetails getBasicPackageDetails(final String basicPackageName) throws PackageNotFoundException, ShmServiceFailureException {
        return new BasicPackageDetails("/home/smrs/lran/basic_package", null);
    }

    @Override
    public List<String> getUpgradePackageDetails(final String packageName) throws PackageNotFoundException, ShmServiceFailureException {
        if (DUMMY_PACKAGE_NAME.equals(packageName)) {
            return newArrayList("/home/smrs/lran/upgrade_package");
        } else if (DUMMY_PACKAGE_NAME_HW_REPLACE.equals(packageName)) {
            return newArrayList("/home/smrs/lran/upgrade_package");
        }
        return newArrayList();
    }

    @Override
    public String getSmoInfoFilePathFromSmrs(final String packageName) {
        return null;
    }

    @Override
    public String getSoftwarPackageReleaseVersion(String packageName) {
        return null;
    }
}