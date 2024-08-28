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

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link CvNameGenerator}.
 */
public class CvNameGeneratorTest {

    private static final String CV_NAME_DOES_NOT_ADHERE_TO_VALID_PATTERN = "CV Name contains value that does not adhere to createCV naming pattern";
    private static final String PRODUCT_NUMBER_ATTRIBUTE = "productNumber";
    private static final String PRODUCT_REVISION_ATTRIBUTE = "productRevision";
    private static final String CREATECV_NAMING_PATTERN_POSITIVE = "[A-Za-z0-9|\\-|\\_|\\.|\\%|\\:]*";

    @Test
    public void when_UpgradePackage_name_contains_Forward_Slash_then_Replace_with_Percentage() {
        final Map<String, String> administrativeData = new HashMap<>();
        final String productNumber = "CXP102051/22";
        final String productRevision = "R45/DL";

        administrativeData.put(PRODUCT_NUMBER_ATTRIBUTE, productNumber);
        administrativeData.put(PRODUCT_REVISION_ATTRIBUTE, productRevision);
        assertTrue(CV_NAME_DOES_NOT_ADHERE_TO_VALID_PATTERN,
                CvNameGenerator.generateCvName(administrativeData).matches(CREATECV_NAMING_PATTERN_POSITIVE));
    }

    @Test
    public void when_UpgradePackage_name_contains_Ampersand_then_Replace_with_Percentage() {
        final Map<String, String> administrativeData = new HashMap<>();
        final String productNumber = "CXP102051&22";
        final String productRevision = "R45&DL";

        administrativeData.put(PRODUCT_NUMBER_ATTRIBUTE, productNumber);
        administrativeData.put(PRODUCT_REVISION_ATTRIBUTE, productRevision);
        assertTrue(CV_NAME_DOES_NOT_ADHERE_TO_VALID_PATTERN,
                CvNameGenerator.generateCvName(administrativeData).matches(CREATECV_NAMING_PATTERN_POSITIVE));
    }

    @Test
    public void when_UpgradePackage_name_contains_Plus_then_Replace_with_Percentage() {
        final Map<String, String> administrativeData = new HashMap<>();
        final String productNumber = "CXP102051+22";
        final String productRevision = "R45+DL";

        administrativeData.put(PRODUCT_NUMBER_ATTRIBUTE, productNumber);
        administrativeData.put(PRODUCT_REVISION_ATTRIBUTE, productRevision);
        assertTrue(CV_NAME_DOES_NOT_ADHERE_TO_VALID_PATTERN,
                CvNameGenerator.generateCvName(administrativeData).matches(CREATECV_NAMING_PATTERN_POSITIVE));
    }

    @Test
    public void when_UpgradePackage_name_contains_Dollar_then_Replace_with_Percentage() {
        final Map<String, String> administrativeData = new HashMap<>();
        final String productNumber = "CXP102051$22";
        final String productRevision = "R45$DL";

        administrativeData.put(PRODUCT_NUMBER_ATTRIBUTE, productNumber);
        administrativeData.put(PRODUCT_REVISION_ATTRIBUTE, productRevision);
        assertTrue(CV_NAME_DOES_NOT_ADHERE_TO_VALID_PATTERN,
                CvNameGenerator.generateCvName(administrativeData).matches(CREATECV_NAMING_PATTERN_POSITIVE));
    }

    @Test
    public void when_UpgradePackage_name_contains_Multiple_Invalid_then_Replace_with_Percentage() {
        final Map<String, String> administrativeData = new HashMap<>();
        final String productNumber = "C?P1(02051$22";
        final String productRevision = "R4*5$DL";

        administrativeData.put(PRODUCT_NUMBER_ATTRIBUTE, productNumber);
        administrativeData.put(PRODUCT_REVISION_ATTRIBUTE, productRevision);
        assertTrue(CV_NAME_DOES_NOT_ADHERE_TO_VALID_PATTERN,
                CvNameGenerator.generateCvName(administrativeData).matches(CREATECV_NAMING_PATTERN_POSITIVE));
    }
}
