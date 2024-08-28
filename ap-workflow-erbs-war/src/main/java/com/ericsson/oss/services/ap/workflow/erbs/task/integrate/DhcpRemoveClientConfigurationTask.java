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
import com.ericsson.oss.services.ap.common.workflow.task.integrate.AbstractDhcpRemoveClientConfigurationTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

/**
 * Dhcp Remove Client Configuration Task.
 */
public class DhcpRemoveClientConfigurationTask extends AbstractDhcpRemoveClientConfigurationTask {

    @Override
    protected void dhcpRemoveClient(final String apNodeFdn, final String clientIdentifier) {
        final WorkflowTaskFacade workflowTaskFacade = new ServiceFinderBean().find(WorkflowTaskFacade.class);
        workflowTaskFacade.removeDhcpClient(apNodeFdn, clientIdentifier);
    }
}
