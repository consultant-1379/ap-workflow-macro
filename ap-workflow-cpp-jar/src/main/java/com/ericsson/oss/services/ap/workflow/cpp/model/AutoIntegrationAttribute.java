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
package com.ericsson.oss.services.ap.workflow.cpp.model;

/**
 * A modelled AutoIntegration attribute for CPP based nodes.
 */
public enum AutoIntegrationAttribute {

    BASIC_PACKAGE("basicPackageName"),
    UNLOCK_CELLS("unlockCells"),
    UPGRADE_PACKAGE("upgradePackageName");

    private final String attributeName;

    private AutoIntegrationAttribute(final String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public String toString() {
        return attributeName;
    }
}
