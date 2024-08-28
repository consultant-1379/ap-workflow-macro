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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.itpf.security.identitymgmtservices.*;
import com.ericsson.oss.itpf.security.identitymgmtservices.enums.ProxyAgentAccountAdminStatus;

/**
 * Stub for the {@link IdentityManagementService}. IdentityManagementService is not used directly by AP. It is used by <code>SmrsService</code> to
 * create and delete user accounts.
 */
@Stateless
@EService
@Remote
public class DummyIdentityManagementService implements IdentityManagementService {

    public static final String DEFAULT_PASSWORD = "password";
    private final Map<String, M2MUser> createdUsers = new HashMap<>();

    @Override
    public M2MUser createM2MUser(final String userName, final String groupName, final String homeDir, final int validDays)
            throws IdentityManagementServiceException {
        final M2MUser newUser = new M2MUser(userName, groupName, 1, 1, homeDir, "20160319141844Z");
        createdUsers.put(userName, newUser);
        return newUser;
    }

    @Override
    public boolean deleteM2MUser(final String userName) throws IdentityManagementServiceException {
        createdUsers.remove(userName);
        return true;
    }

    @Override
    public M2MUser getM2MUser(final String userName) throws IdentityManagementServiceException {
        return createdUsers.get(userName);

    }

    @Override
    public boolean isExistingM2MUser(final String userName) throws IdentityManagementServiceException {
        return createdUsers.containsKey(userName);
    }

    @Override
    public char[] getM2MPassword(final String userName) throws IdentityManagementServiceException {
        return DEFAULT_PASSWORD.toCharArray();
    }

    @Override
    public char[] updateM2MPassword(final String userName) throws IdentityManagementServiceException {
        return DEFAULT_PASSWORD.toCharArray();
    }

    @Override
    public List<String> getAllTargetGroups() throws IdentityManagementServiceException {
        return Collections.emptyList();
    }

    @Override
    public String getDefaultTargetGroup() throws IdentityManagementServiceException {
        return "";
    }

    @Override
    public List<String> validateTargetGroups(final List<String> targetGroups) throws IdentityManagementServiceException {
        return Collections.emptyList();
    }

    @Override
    public ProxyAgentAccountData createProxyAgentAccount() throws IdentityManagementServiceException {
        return null;
    }

    @Override
    public boolean deleteProxyAgentAccount(final String userDN) throws IdentityManagementServiceException {
        return true;
    }

    @Override
    public ProxyAgentAccountGetData getProxyAgentAccount(Boolean aBoolean, Boolean aBoolean1) {
        return null;
    }

    @Override
    public ProxyAgentAccountGetData getProxyAgentAccountByAdminStatus(ProxyAgentAccountAdminStatus proxyAgentAccountAdminStatus, Boolean aBoolean, Boolean aBoolean1) {
        return null;
    }

    @Override
    public ProxyAgentAccountGetData getProxyAgentAccountByInactivityPeriod(Long aLong, Boolean aBoolean, Boolean aBoolean1) {
        return null;
    }

    @Override
    public ProxyAgentAccountDetails getProxyAgentAccountDetails(String s) {
        return null;
    }

    @Override
    public Boolean updateProxyAgentAccountAdminStatus(String s, ProxyAgentAccountAdminStatus proxyAgentAccountAdminStatus) {
        return null;
    }

    @Override
    public M2MUserPassword createM2MUserPassword(String userName, String groupName, String homeDir, int validDays)
            throws IdentityManagementServiceException {
        M2MUser m2mUser = new M2MUser(userName, groupName, 0, 0, null, null);
        M2MUserPassword m2mUserPassword = new M2MUserPassword(m2mUser, DEFAULT_PASSWORD);
        return m2mUserPassword;
    }

}
