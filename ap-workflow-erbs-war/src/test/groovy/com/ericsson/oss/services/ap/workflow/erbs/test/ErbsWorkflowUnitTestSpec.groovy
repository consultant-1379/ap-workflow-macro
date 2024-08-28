/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.erbs.test

import org.camunda.bpm.engine.runtime.ProcessInstance

import com.ericsson.oss.services.ap.common.workflow.messages.ImportConfigurationMessage
import com.ericsson.oss.services.ap.workflow.test.WorkflowUnitTestSpec

/**
 * Parent test spec for ERBS workflow tests.
 * Contains methods that may be common across workflow tests
 */
class ErbsWorkflowUnitTestSpec extends WorkflowUnitTestSpec {

    protected void executeImportConfigurations(final boolean importSuccessful, final ProcessInstance importProcessInstance) {
        final Map<String, Object> importVariables = new HashMap<>()
        if (importSuccessful) {
            importVariables.put("importConfigurationsSuccessful", true)
        } else {
            importVariables.put("importConfigurationsSuccessful", false)
            importVariables.put(ImportConfigurationMessage.getFailurePointKey(), "configuration")
        }
        waitForTask("wait_for_import_config_complete", importProcessInstance)
        correlateMessage("IMPORT_CONFIGURATIONS_COMPLETION", importVariables)
    }
}