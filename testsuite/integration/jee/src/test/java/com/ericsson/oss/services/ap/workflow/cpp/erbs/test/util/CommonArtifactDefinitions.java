/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.util;

/**
 * Container class for common string definitions used in test code for verifying artifact handling.
 */
public final class CommonArtifactDefinitions {

    public static final String NODE_ARTIFACT_FDN = "%s,NodeArtifactContainer=1,NodeArtifact=%s";
    public static final String ATTRIBUTE_NODE_ARTIFACT_ID = "NodeArtifactId";
    public static final String SMRS_ERBS_DIRECTORY_TYPE = "smrs.ERBS";
    public static final String TASK_PROGRESS_SUCCESS = "Completed";

    private CommonArtifactDefinitions() {

    }
}
