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

import java.io.Serializable;

/**
 * Object representing an <code>OptionalFeatureLicense</code> MO.
 */
public class OptionalFeatureLicense implements Serializable {

    private static final long serialVersionUID = 207862258388207397L;
    private static final String LICENSE_STATE_ENABLED_VALUE = "ENABLED";

    private final String optionalFeatureLicenseFdn;
    private final boolean isLicenseStateEnabled;

    public OptionalFeatureLicense(final String optionalFeatureFdn, final String licenseState) {
        optionalFeatureLicenseFdn = optionalFeatureFdn;
        isLicenseStateEnabled = LICENSE_STATE_ENABLED_VALUE.equals(licenseState);
    }

    /**
     * The FDN of the <code>OptionalFeatureLicense</code> MO.
     *
     * @return the FDN
     */
    public String getFdn() {
        return optionalFeatureLicenseFdn;
    }

    /**
     * Checks if the <i>licenseState</i> attribute of the <code>OptionalFeatureLicense</code> MO is set to <b>ENABLED</b>.
     *
     * @return true if the <i>licenseState</i> is <b>ENABLED</b>
     */
    public boolean isLicenseStateEnabled() {
        return isLicenseStateEnabled;
    }
}
