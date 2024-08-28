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
package com.ericsson.oss.services.ap.workflow.cpp.artifacts;

/**
 * Interface for RbsSummaryHandler implementations. RbsSummaryHandler is responsible for generating the content of the RbsSummaryFile artifact. The
 * file content may vary for each node type so each implementations of this interface will handle generation of its own node specific data.
 */
public interface RbsSummaryHandler {

    /**
     * Generates the RbsSummary file and stores on the SMRS.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @return the generated RbsSummary file
     */
    String generate(final String apNodeFdn);

    /**
     * Gets the path of the generated RbsSummary file relative to the SMRS root directory.
     *
     * @param apNodeFdn
     *            the FDN of the AP node
     * @return generated RbsSummary file path
     */
    String getRelativeRbsSummaryPath(final String apNodeFdn);
}
