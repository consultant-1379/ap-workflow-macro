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
package com.ericsson.oss.services.ap.workflow.cpp.configuration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Provides method to read in the upgrade package product number and revision details from the <code>AdministrativeData</code> MO, and process these
 * values so that it returns a name that adheres to the Configuration Version naming convention. Only the characters "A-Z", "a-z", "0-9", "-", "_",
 * ".", "%" and ":" are allowed.
 */
final class CvNameGenerator {

    private static final String NO_UPGRADEPACKAGE_PRODUCT_INFO_FOUND = "ProductNo_ProductRev_";
    private static final String PRODUCT_REVISION = "productRevision";
    private static final String PRODUCT_NUMBER = "productNumber";

    private CvNameGenerator() {

    }

    /**
     * Uses the node's upgrade package information to generate a Configuration Version name in a format that matches the supported naming convention.
     * <p>
     * The CV name will be in the format:
     * <p>
     * <b>{@literal <}productNumber{@literal >}_{@literal <}productRevision{@literal >}_{@literal <}timeStamp{@literal >}</b>, where any unsupported
     * characters will be replaced with a "%" symbol
     * <p>
     * If the <i>productNumber</i> or <i>productRevision</i> attributes are empty or not found, the CV name will use the default format:
     * <p>
     * <b>{@literal <}ProductNo_ProductRev_{@literal <}timeStamp{@literal >}</b>
     *
     * @param administrativeData
     *            the attributes from the <code>AdministrativeData</code> MO
     * @return a valid CV name
     */
    public static String generateCvName(final Map<String, String> administrativeData) {
        final String productNumberAndRevision = getProductNumberAndRevision(administrativeData);
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyyy_HHmmss");
        return productNumberAndRevision + dateFormatter.format(new Date());
    }

    private static String getProductNumberAndRevision(final Map<String, String> administrativeData) {
        final String productNumber = administrativeData.get(PRODUCT_NUMBER);
        final String productRevision = administrativeData.get(PRODUCT_REVISION);

        if (StringUtils.isNotBlank(productNumber) && StringUtils.isNotBlank(productRevision)) {
            return removeInvalidCharacters(productNumber + "_" + productRevision + "_");
        } else {
            return NO_UPGRADEPACKAGE_PRODUCT_INFO_FOUND;
        }
    }

    private static String removeInvalidCharacters(final String productNumberAndRevisionOriginal) {
        return productNumberAndRevisionOriginal.replaceAll("[^A-Za-z0-9|^-|^_|^\\.|^\\%|^\\:]", "%");
    }
}
