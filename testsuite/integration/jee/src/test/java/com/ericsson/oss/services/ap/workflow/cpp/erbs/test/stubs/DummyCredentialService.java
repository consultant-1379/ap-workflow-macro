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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.stubs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import com.ericsson.nms.security.nscs.api.CredentialService;
import com.ericsson.nms.security.nscs.api.credentials.CredentialAttributes;
import com.ericsson.nms.security.nscs.api.credentials.SnmpV3Attributes;
import com.ericsson.nms.security.nscs.api.enums.EnrollmentMode;
import com.ericsson.nms.security.nscs.api.enums.SnmpSecurityLevel;
import com.ericsson.nms.security.nscs.api.exception.CredentialServiceException;
import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * Stub for the {@link CredentialService}.
 */
@Stateless
@EService
public class DummyCredentialService implements CredentialService {

    public static boolean throwExceptionOnCreateNodeCredentials;
    private static boolean throwExceptionOnConfigureSnmpV3;

    /**
     * @return the throwExceptionOnConfigureSnmpV3
     */
    public static Boolean getThrowExceptionOnConfigureSnmpV3() {
        return throwExceptionOnConfigureSnmpV3;
    }

    /**
     * @param throwExceptionOnConfigureSnmpV3
     *            the throwExceptionOnConfigureSnmpV3 to set
     */
    public static void setThrowExceptionOnConfigureSnmpV3(final Boolean throwExceptionOnConfigureSnmpV3) {
        DummyCredentialService.throwExceptionOnConfigureSnmpV3 = throwExceptionOnConfigureSnmpV3;
    }

    @Override
    public void configureEnrollmentMode(final EnrollmentMode arg0, final String arg1) throws CredentialServiceException {
    }

    @Override
    public void createNodeCredentials(final CredentialAttributes arg0, final String arg1) throws CredentialServiceException {
        if (throwExceptionOnCreateNodeCredentials) {
            throwExceptionOnCreateNodeCredentials = false;
            throw new CredentialServiceException("Dummy Credential Service Exception");
        }
    }

    @Override
    public void createNodeCredentials(CredentialAttributes credentialAttributes, String s, String s1) throws CredentialServiceException {

    }

    @Override
    public boolean validateAttributes(String s, CredentialAttributes credentialAttributes) {
        return false;
    }

    @Override
    public void configureSnmpV3(final SnmpSecurityLevel securityLevel, final SnmpV3Attributes snmpV3Attributes, final List<String> nodes) {
        if (throwExceptionOnConfigureSnmpV3) {
            throw new CredentialServiceException("Dummy Credential Service Exception");
        }
    }

    @Override
    public Map<String, SnmpV3Attributes> getSnmpV3Configuration(final boolean isPlainText, final List<String> nodes){
        return new HashMap<>();
    }
}
