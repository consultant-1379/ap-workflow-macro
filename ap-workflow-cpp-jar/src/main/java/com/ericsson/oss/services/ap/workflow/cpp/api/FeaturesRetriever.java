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
package com.ericsson.oss.services.ap.workflow.cpp.api;

import java.util.List;

import javax.ejb.Local;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * Used to retrieve OptionalFeatureLicense MOs
 */
@EService
@Local
public interface FeaturesRetriever {

    /**
     * Retrieves all MOs of type <code>OptionalFeatureLicense</code> for the node.
     *
     * @param meContextFdn
     *            the FDN of the MeContext
     * @return a list of {@link OptionalFeatureLicense}
     */
    List<OptionalFeatureLicense> getOptionalFeatureLicenseMos(final String meContextFdn);

}
