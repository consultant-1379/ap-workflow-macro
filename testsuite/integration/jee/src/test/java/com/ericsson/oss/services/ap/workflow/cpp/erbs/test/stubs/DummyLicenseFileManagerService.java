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

import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.ImportLicenseRemoteResponse;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.LicenseAccessException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.LicenseFileManagerService;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.LicenseValidationException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.errorcodes.LicenseErrorCodes;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.DeleteLicenseException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.ImportLicenseException;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.InstallLicenseException;

@Stateless
@EService
@Remote
public class DummyLicenseFileManagerService implements LicenseFileManagerService {

    private static final String LICENSE_KEY_FILE_PATH = "/dummy/path/to/licensekey/file";
    private static final String EXCEPTION_CAUSE_MESSAGE = "Dummy DummyLicenseFileManagerService Exception Cause";
    private static final String EXCEPTION_SOLUTION_SUMMARY = "Dummy DummyLicenseFileManagerService Exception Solution Summary";
    private static final String EXCEPTION_ERROR_CODE = "Dummy DummyLicenseFileManagerService Exception Error Code";

    public static boolean throwImportLicenseException = false;
    public static boolean throwExceptionOnDeleteLicense = false;

    @Override
    public String getLicenseKeyFilePathByNode(final String networkType, final String nodeType, final String nodeFdn) {
        return LICENSE_KEY_FILE_PATH;
    }

    @Override
    public String getLicenseKeyFilePathByNode(final String nodeFdn) {
        return LICENSE_KEY_FILE_PATH;
    }

    @Override
    public String getLicenseKeyFilePathByFingerprint(final String fingerprint) {
        return LICENSE_KEY_FILE_PATH;
    }

    @Override
    public String getAssociatedNode(final String fingerprint) {
        return null;
    }

    @Override
    public void validateLicenseKeys(final String nodeName, final List<String> licenseKeysToCheck)
            throws LicenseAccessException, LicenseValidationException {

    }

    @Override
    public ImportLicenseRemoteResponse importLicenseKeyFile(final String arg0) throws ImportLicenseException {
        if (throwImportLicenseException) {
            throw new ImportLicenseException("File already imported", "File already imported", LicenseErrorCodes.FILE_ALREADY_EXISTS.getErrorCode());
        }
        return new ImportLicenseRemoteResponse("dummyFingerPrint", "12345");
    }

    @Override
    public String installLicense(final String nodeName) throws InstallLicenseException {
        return "DummyLicenseFile installed";
    }

    @Override
    public void deleteLicense(final String fingerPrint, final String sequenceNumber) throws DeleteLicenseException {
        if (throwExceptionOnDeleteLicense) {
            throw new DeleteLicenseException(EXCEPTION_CAUSE_MESSAGE, EXCEPTION_SOLUTION_SUMMARY, EXCEPTION_ERROR_CODE);
        }
    }
}
