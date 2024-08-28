/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.ejb;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.services.ap.api.workflow.AutoProvisioningWorkflowService;
import com.ericsson.oss.services.ap.workflow.cpp.configuration.SupportedMessages;

/**
 * Service implementation to access data related to the ERBS workflows.
 */
@Local
@Stateless
@EServiceQualifier("erbs")
public class ErbsWorkflowServiceEjb implements AutoProvisioningWorkflowService {

    private static final String ORDER_WORKFLOW = "auto_integrate_erbs_d.1.44_4";
    private static final String DELETE_WORKFLOW = "delete_erbs_d.1.44_4";

    @Override
    public String getOrderWorkflowName() {
        return ORDER_WORKFLOW;
    }

    @Override
    public String getDeleteWorkflowName() {
        return DELETE_WORKFLOW;
    }

    @Override
    public boolean isSupported(final String messageKey) {
        return SupportedMessages.isMessageSupported(messageKey);
    }

    @Override
    public String getHardwareReplaceWorkflowName() {
        return null;
    }

    @Override
    public String getReconfigurationOrderWorkflowName() {
        return null;
    }

    @Override
    public String getExpansionOrderWorkflowName() {
        return null;
    }

    @Override
    public String getMigrationWorkflowName() {
        return null;
    }

    @Override
    public String getEoiIntegrationWorkflow() {
        return null;
    }

    @Override
    public List<String> getAllWorkflowNames() {
        final List<String> workflowNames = new ArrayList<>();
        workflowNames.add(ORDER_WORKFLOW);
        workflowNames.add(DELETE_WORKFLOW);
        return workflowNames;
    }

}
