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
 * Modelled attribute for the <i>rbsConfigLevel</i> attribute of the <code>RbsConfiguration</code> MO.
 */
public enum RbsConfigurationAttribute {

    RBS_CONFIG_LEVEL("rbsConfigLevel");

    private final String attributeName;

    private RbsConfigurationAttribute(final String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public String toString() {
        return attributeName;
    }
}
