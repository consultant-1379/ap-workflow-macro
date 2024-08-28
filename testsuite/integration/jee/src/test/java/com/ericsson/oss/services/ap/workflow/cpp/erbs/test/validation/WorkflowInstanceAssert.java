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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.validation;

import static com.ericsson.oss.services.wfs.api.query.QueryBuilderFactory.getDefaultQueryBuilder;
import static com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes.QueryParameters.STATE;
import static com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes.QueryParameters.WORKFLOW_INSTANCE_ID;
import static com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes.State.ACTIVE;
import static com.ericsson.oss.services.wfs.api.query.instance.WorkflowInstanceQueryAttributes.State.SUSPENDED;

import org.assertj.core.api.AbstractAssert;

import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.wfs.api.instance.WorkflowInstance;
import com.ericsson.oss.services.wfs.api.query.Query;
import com.ericsson.oss.services.wfs.api.query.QueryType;
import com.ericsson.oss.services.wfs.api.query.RestrictionBuilder;
import com.ericsson.oss.services.wfs.jee.api.WorkflowQueryServiceLocal;

/**
 * Set of assertions to assert the state of a workflow by its WorkflowInstance.
 */
public class WorkflowInstanceAssert extends AbstractAssert<WorkflowInstanceAssert, WorkflowInstance> {

    private WorkflowInstanceAssert(final WorkflowInstance actual) {
        super(actual, WorkflowInstanceAssert.class);
    }

    public static WorkflowInstanceAssert assertThat(final WorkflowInstance actual) {
        return new WorkflowInstanceAssert(actual);
    }

    private boolean isWorkflowInState(final String state) {
        final WorkflowQueryServiceLocal queryService = new ServiceFinderBean().find(WorkflowQueryServiceLocal.class);
        final Query query = getDefaultQueryBuilder()
                .createTypeQuery(QueryType.WORKFLOW_INSTANCE_QUERY);

        final RestrictionBuilder builder = query.getRestrictionBuilder();

        query.setRestriction(builder.allOf(
                builder.isEqual(STATE, state),
                builder.isEqual(WORKFLOW_INSTANCE_ID, actual.getId())));

        return queryService.executeCountQuery(query) == 1;
    }

    public WorkflowInstanceAssert isInSuspendedState() {
        isNotNull();

        if (!isWorkflowInState(SUSPENDED)) {
            failWithMessage("Expected WorkflowInstance[%s] to be in suspended state but it is not.", actual.getWorkflowDefinitionId());
        }

        return this;
    }

    public WorkflowInstanceAssert isInActiveState() {
        isNotNull();

        if (!isWorkflowInState(ACTIVE)) {
            failWithMessage("Expected WorkflowInstance[%s] to be in active state but it is not.", actual.getWorkflowDefinitionId());
        }

        return this;
    }

    public WorkflowInstanceAssert isCancelled() {
        isNotNull();

        final boolean cancelled = !isWorkflowInState(ACTIVE) && !isWorkflowInState(SUSPENDED);

        if (!cancelled) {
            failWithMessage("Expected WorkflowInstance[%s] to be in cancelled state but it is not.", actual.getWorkflowDefinitionId());
        }

        return this;
    }
}
