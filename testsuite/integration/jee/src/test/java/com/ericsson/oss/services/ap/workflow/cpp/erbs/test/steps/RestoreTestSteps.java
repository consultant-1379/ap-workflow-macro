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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.restore.RestoreController;
import com.ericsson.oss.services.ap.common.configuration.DirectoryConfiguration;
import com.ericsson.oss.services.wfs.internal.processengine.ProcessEngineConstants;

import ru.yandex.qatools.allure.annotations.Step;

/**
 * Restore related test steps for Restore test.
 */
public class RestoreTestSteps {

    private static final String SFWK_RESTORE_BURFOLDER_LOCATION_ABSOLUTE_PATH = DirectoryConfiguration.getRestoreDirectory() + "/";

    @Inject
    private Logger logger;

    @Resource(mappedName = ProcessEngineConstants.JNDI_DEFAULT_PROCESS_ENGINE)
    private ProcessEngine processEngine;

    @Step("Suspend the autoprovisioning workflow")
    public void suspend_workflow(final String workflowId) {
        processEngine.getRuntimeService().suspendProcessInstanceById(workflowId);
    }

    @Step("Prepare bur resource")
    public void prepare_resource_for_restore() {
        System.setProperty("sfwk.restore.burfolder.location.absolute.path", SFWK_RESTORE_BURFOLDER_LOCATION_ABSOLUTE_PATH);

        final File burFolder = new File(SFWK_RESTORE_BURFOLDER_LOCATION_ABSOLUTE_PATH);
        final File burFile = Paths.get(SFWK_RESTORE_BURFOLDER_LOCATION_ABSOLUTE_PATH, "enmrestoredata.txt").toFile();
        final File restoreInfoFolder = Paths.get(SFWK_RESTORE_BURFOLDER_LOCATION_ABSOLUTE_PATH, "sfwk-restoreinfo").toFile();

        try {
            FileUtils.deleteDirectory(burFolder);
        } catch (final IOException e) {
            logger.warn("Failed to delete restore folder", e);
        }
        try {
            restoreInfoFolder.mkdirs();
            burFile.createNewFile();
        } catch (final IOException e) {
            logger.warn("Failed to set up folders for restore to take place", e);
        }
    }

    public void start_restore() {
        final RestoreController restoreController = new ServiceFinderBean().find(RestoreController.class);
        restoreController.setMaxRestoreRetryAttempts(1);
        restoreController.setRestoreRetryInterval(1);
        restoreController.startRestore();
    }
}
