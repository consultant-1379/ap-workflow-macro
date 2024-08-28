/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.task.integrate;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.common.workflow.task.common.AbstractConfigureManagementStateTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

public class ConfigureManagementStateTask extends AbstractConfigureManagementStateTask {

    @Override
    public void configureManagementState(final String apNodeFdn, final String maintenanceValue) {
        final WorkflowTaskFacade workflowTaskFacade = new ServiceFinderBean().find(WorkflowTaskFacade.class);
        workflowTaskFacade.configureManagementState(apNodeFdn, maintenanceValue);
    }
}
