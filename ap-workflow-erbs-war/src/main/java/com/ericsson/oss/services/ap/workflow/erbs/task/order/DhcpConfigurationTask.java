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
package com.ericsson.oss.services.ap.workflow.erbs.task.order;

import com.ericsson.oss.services.ap.api.model.DhcpConfiguration;
import com.ericsson.oss.services.ap.common.workflow.task.order.AbstractDhcpConfigurationTask;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;

/**
 * Dhcp Configuration Task.
 */
public class DhcpConfigurationTask extends AbstractDhcpConfigurationTask {

    @Override
    protected boolean dhcpConfiguration(final String apNodeFdn, final String oldHardwareSerialNumber,final DhcpConfiguration dhcpConfiguration) {
        final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
        return workflowTaskFacade.configureDhcp(apNodeFdn, oldHardwareSerialNumber, dhcpConfiguration);
    }
}
