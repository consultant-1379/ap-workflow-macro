/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test;

import static com.ericsson.oss.services.ap.arquillian.util.data.validation.DpsAssert.assertThat;
import static com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util.WorkflowProjectBuilder.createErbsProjectWithOneNode;

import javax.inject.Inject;

import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.Dps;
import com.ericsson.oss.services.ap.arquillian.util.data.project.ProjectDescriptor;
import com.ericsson.oss.services.ap.common.model.SupervisionMoType;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowDataSteps;
import com.ericsson.oss.services.ap.workflow.cpp.erbs.test.steps.WorkflowServiceTestSteps;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Arquillian test class to test the enabling of FM, PM and Inventory Supervision for ERBS nodes.
 */
public class EnableErbsSupervisionTest extends Arquillian {

    @Inject
    private WorkflowDataSteps dataSteps;

    @Inject
    private WorkflowServiceTestSteps wfsSteps;

    @Inject
    private Dps dps;

    @Test
    @Features("Enable Supervision")
    @Stories({ "WHEN fm pm and inventory supervision is enabled THEN fmAlarmSupervisionMo pmFunctionMo and inventoryMo has attribute set to true" })
    public void when_fm_pm_and_inventory_supervision_is_enabled_then_fmAlarmSupervisionMo_pmFunctionMo_and_inventoryMo_has_attribute_set_to_true() {
        final ProjectDescriptor projectDescriptor = createErbsProjectWithOneNode()
                .with_valid_configurations()
                .with_default_erbs_artifacts()
                .with_default_security_options()
                .with_default_supervision_options()
                .with_default_erbs_auto_integration_options()
                .build();

        final ManagedObject nodeMo = dataSteps.create_project_mo_and_return_node_mo(projectDescriptor);

        dataSteps.create_me_context_and_all_erbs_node_mos(nodeMo.getName());

        final ManagedObject fmAlarmSupervisionMo = dataSteps.createFmAlarmSupervisionMo(nodeMo);
        final ManagedObject pmFunctionMo = dataSteps.createPmFunctionMo(nodeMo);
        final ManagedObject inventoryMo = dataSteps.createInventorySupervisionMo(nodeMo);

        wfsSteps.enable_supervision_options(nodeMo.getFdn(), SupervisionMoType.FM);
        wfsSteps.enable_supervision_options(nodeMo.getFdn(), SupervisionMoType.PM);
        wfsSteps.enable_supervision_options(nodeMo.getFdn(), SupervisionMoType.INVENTORY);

        assertThat(dps).withManagedObject(fmAlarmSupervisionMo.getFdn()).withAttributeValue("active", true);
        assertThat(dps).withManagedObject(pmFunctionMo.getFdn()).withAttributeValue("pmEnabled", true);
        assertThat(dps).withManagedObject(inventoryMo.getFdn()).withAttributeValue("active", true);
    }
}