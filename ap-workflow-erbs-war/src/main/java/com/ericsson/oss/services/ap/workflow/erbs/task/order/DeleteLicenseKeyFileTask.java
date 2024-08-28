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

import com.ericsson.oss.services.ap.common.workflow.task.order.AbstractDeleteLicenseKeyFileTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.shm.licenseservice.remoteapi.exception.DeleteLicenseException;

/**
 * Task to delete the LicenseKeyFile if imported via AP
 */
public class DeleteLicenseKeyFileTask extends AbstractDeleteLicenseKeyFileTask {

    @Override
    protected void deleteLicenseKeyFile(final String fingerPrint, final String sequenceNumber, final String apNodeFdn) throws DeleteLicenseException {
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        workflowTaskFacade.deleteLicenseKeyFile(fingerPrint, sequenceNumber, apNodeFdn);
    }
}
