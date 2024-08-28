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

import static com.ericsson.oss.services.ap.common.model.MoType.SECURITY;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_NAME;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;
import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.common.cm.DpsOperations;
import com.ericsson.oss.services.shm.backupservice.remote.api.CVOperationRemoteException;
import com.ericsson.oss.services.shm.backupservice.remote.api.ConfigurationVersionManagementServiceRemote;

/**
 * Unit tests for {@link CvManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CvManagerTest {

    private static final String MECONTEXT_FDN = "MeContext=" + NODE_NAME;
    private static final String CONFIGURATION_VERSION_FDN = MECONTEXT_FDN + ",ManagedElement=1,SwManagement=1,ConfigurationVersion=1";
    private static final String UPGRADE_PACKAGE_FDN = MECONTEXT_FDN + ",ManagedElement=1,SwManagement=1,UpgradePackage=123";

    private static final String PRODUCT_NUMBER = "CXP102051%22";
    private static final String PRODUCT_REVISION = "R45%DL";
    private static final String PRODUCT_NUMBER_REVISION_ACTUAL_PREFIX = PRODUCT_REVISION + "_" + PRODUCT_NUMBER;
    private static final String PRODUCT_NUMBER_REVISION_DEFAULT_PREFIX = "ProductNo_ProductRev_";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private Logger logger; // NOPMD

    @InjectMocks
    private CvManager cvManager;

    @Mock
    private ConfigurationVersionManagementServiceRemote shmCvMgtService;

    @InjectMocks
    private DpsOperations dpsOperations;

    private final ArgumentMatcher<String> cvNameArgMatcher = new CvNameContainsProductNumberAndVersion();
    private final ArgumentMatcher<String> defaultCvNamevArgMatcher = new CvNameIsDefault();

    private RuntimeConfigurableDps configurableDps;

    @Before
    public void setUp() {
        configurableDps = new RuntimeConfigurableDps();
        final DataPersistenceService dps = configurableDps.build();
        Whitebox.setInternalState(dpsOperations, "dps", dps);
        Whitebox.setInternalState(cvManager, "dpsOperations", dpsOperations);
    }

    @Test
    public void when_cv_successfully_created_then_cv_is_startable() throws CVOperationRemoteException {
        addConfigurationVersionMo();
        addUpgradePackageMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(cvNameArgMatcher), argThat(cvNameArgMatcher), eq("AP"))).thenReturn(true);
        when(shmCvMgtService.setStartableCV(eq(NODE_NAME), argThat(cvNameArgMatcher))).thenReturn(true);
        when(shmCvMgtService.setCVFirstInRollBackList(eq(NODE_NAME), argThat(cvNameArgMatcher))).thenReturn(true);

        cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        verify(shmCvMgtService).setStartableCV(eq(NODE_NAME), argThat(cvNameArgMatcher));
    }

    @Test
    public void when_cv_successfully_created_then_cv_is_first_in_rollback_list() throws CVOperationRemoteException {
        addConfigurationVersionMo();
        addUpgradePackageMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(cvNameArgMatcher), argThat(cvNameArgMatcher), eq("AP"))).thenReturn(true);
        when(shmCvMgtService.setStartableCV(eq(NODE_NAME), argThat(cvNameArgMatcher))).thenReturn(true);
        when(shmCvMgtService.setCVFirstInRollBackList(eq(NODE_NAME), argThat(cvNameArgMatcher))).thenReturn(true);

        cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        verify(shmCvMgtService).setCVFirstInRollBackList(eq(NODE_NAME), argThat(cvNameArgMatcher));
    }

    @Test
    public void when_no_configurationVersion_mo_found_then_cv_created_with_default_name() throws CVOperationRemoteException {
        addUpgradePackageMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher), argThat(defaultCvNamevArgMatcher), eq("AP")))
                .thenReturn(true);
        when(shmCvMgtService.setStartableCV(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher))).thenReturn(true);
        when(shmCvMgtService.setCVFirstInRollBackList(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher))).thenReturn(true);

        final String createdCvName = cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        assertTrue(createdCvName.startsWith(PRODUCT_NUMBER_REVISION_DEFAULT_PREFIX));
    }

    @Test
    public void when_currentUpgradePackage_is_null_then_cv_created_with_default_name() throws CVOperationRemoteException {

        addConfigurationVersionMo(null);

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher), argThat(defaultCvNamevArgMatcher), eq("AP")))
                .thenReturn(true);
        when(shmCvMgtService.setStartableCV(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher))).thenReturn(true);
        when(shmCvMgtService.setCVFirstInRollBackList(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher))).thenReturn(true);

        final String createdCvName = cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        assertTrue(createdCvName.startsWith(PRODUCT_NUMBER_REVISION_DEFAULT_PREFIX));
    }

    @Test
    public void when_no_currentUpgradePackage_mo_found_then_cv_created_with_default_name() throws CVOperationRemoteException {
        addConfigurationVersionMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher), argThat(defaultCvNamevArgMatcher), eq("AP")))
                .thenReturn(true);
        when(shmCvMgtService.setStartableCV(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher))).thenReturn(true);
        when(shmCvMgtService.setCVFirstInRollBackList(eq(NODE_NAME), argThat(defaultCvNamevArgMatcher))).thenReturn(true);

        final String createdCvName = cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        assertTrue(createdCvName.startsWith(PRODUCT_NUMBER_REVISION_DEFAULT_PREFIX));
    }

    @Test
    public void when_currentUpgradePackage_mo_found_then_cv_created_with_name_equal_to_the_administrativeData_upgrade_product_number_and_version()
            throws CVOperationRemoteException {
        addConfigurationVersionMo();
        addUpgradePackageMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(cvNameArgMatcher), argThat(cvNameArgMatcher), eq("AP"))).thenReturn(true);
        when(shmCvMgtService.setStartableCV(eq(NODE_NAME), argThat(cvNameArgMatcher))).thenReturn(true);
        when(shmCvMgtService.setCVFirstInRollBackList(eq(NODE_NAME), argThat(cvNameArgMatcher))).thenReturn(true);

        final String createdCvName = cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        assertTrue(createdCvName.startsWith(PRODUCT_NUMBER_REVISION_ACTUAL_PREFIX));
    }

    @Test
    public void when_create_cv_fails_then_no_attempt_to_set_cv_as_startable() throws CVOperationRemoteException {
        addConfigurationVersionMo();
        addUpgradePackageMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(cvNameArgMatcher), argThat(cvNameArgMatcher), eq("AP"))).thenReturn(false);
        thrown.expect(ApServiceException.class);
        cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        verify(shmCvMgtService, never()).setStartableCV(eq(NODE_NAME), argThat(cvNameArgMatcher));
    }

    @Test
    public void when_create_cv_fails_then_no_attempt_to_set_cv_first_in_rollback_list() throws CVOperationRemoteException {
        addConfigurationVersionMo();
        addUpgradePackageMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(cvNameArgMatcher), argThat(cvNameArgMatcher), eq("AP"))).thenReturn(false);
        thrown.expect(ApServiceException.class);
        cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        verify(shmCvMgtService).setCVFirstInRollBackList(eq(NODE_NAME), argThat(cvNameArgMatcher));
    }

    @Test
    public void when_cv_created_and_failed_to_set_as_startable_then_no_attempt_to_set_first_in_rollback_list() throws CVOperationRemoteException {
        addConfigurationVersionMo();
        addUpgradePackageMo();

        when(shmCvMgtService.createCV(eq(NODE_NAME), argThat(cvNameArgMatcher), argThat(cvNameArgMatcher), eq("AP"))).thenReturn(true);
        when(shmCvMgtService.setStartableCV(eq(NODE_NAME), argThat(cvNameArgMatcher))).thenReturn(false);
        thrown.expect(ApServiceException.class);
        cvManager.createCv(NODE_NAME, MECONTEXT_FDN, "AP");
        verify(shmCvMgtService).setCVFirstInRollBackList(eq(NODE_NAME), argThat(cvNameArgMatcher));
    }

    private static class CvNameContainsProductNumberAndVersion extends ArgumentMatcher<String> {

        @Override
        public boolean matches(final Object cvName) {
            return ((String) cvName).startsWith(PRODUCT_NUMBER_REVISION_ACTUAL_PREFIX);
        }
    }

    private static class CvNameIsDefault extends ArgumentMatcher<String> {

        @Override
        public boolean matches(final Object cvName) {
            return ((String) cvName).startsWith(PRODUCT_NUMBER_REVISION_DEFAULT_PREFIX);
        }
    }

    private void addConfigurationVersionMo() {
        addConfigurationVersionMo(UPGRADE_PACKAGE_FDN);
    }

    private void addConfigurationVersionMo(final String upFdn) {
        final Map<String, Object> cvAttributes = new HashMap<>();
        cvAttributes.put("currentUpgradePackage", upFdn);

        configurableDps.addManagedObject()
                .withFdn(CONFIGURATION_VERSION_FDN)
                .type("ConfigurationVersion")
                .addAttributes(cvAttributes)
                .build();
    }

    private void addUpgradePackageMo() {
        final Map<String, Object> administrativeData = new HashMap<>();
        administrativeData.put("productRevision", PRODUCT_NUMBER);
        administrativeData.put("productNumber", PRODUCT_REVISION);

        final Map<String, Object> updatedAttributes = new HashMap<>();
        updatedAttributes.put("administrativeData", administrativeData);

        configurableDps.addManagedObject()
                .withFdn(UPGRADE_PACKAGE_FDN)
                .type(SECURITY.toString())
                .addAttributes(updatedAttributes)
                .build();
    }
}
