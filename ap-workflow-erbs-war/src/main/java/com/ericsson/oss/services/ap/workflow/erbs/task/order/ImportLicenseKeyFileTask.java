/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.task.order;

import com.ericsson.oss.services.ap.common.workflow.task.order.AbstractImportLicenseKeyFileTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.ImportLicenseRemoteResponse;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.ImportLicenseException;

/**
 * Provides import license key file to shm for a given AP node.
 */
public class ImportLicenseKeyFileTask extends AbstractImportLicenseKeyFileTask {

    @Override
    protected ImportLicenseRemoteResponse importLicenseKeyFile(final String apNodeFdn) throws ImportLicenseException {
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        return workflowTaskFacade.importLicenseKeyFile(apNodeFdn);
    }
}