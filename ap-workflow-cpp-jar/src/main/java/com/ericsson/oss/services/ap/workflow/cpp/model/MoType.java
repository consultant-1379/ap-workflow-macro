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
 * MO types used in the system for CPP based nodes.
 */
public enum MoType {

    ENODEBFUNCTION("ENodeBFunction"),
    LICENSING("Licensing"),
    MANAGEDELEMENT("ManagedElement"),
    NODEMANAGEDFUNCTION("NodeManagementFunction"),
    OPTIONALFEATURELICENSE("OptionalFeatureLicense"),
    OPTIONALFEATURES("OptionalFeatures"),
    RBSCONFIGURATION("RbsConfiguration"),
    SYSTEMFUNCTIONS("SystemFunctions");

    private final String moName;

    private MoType(final String moName) {
        this.moName = moName;
    }

    @Override
    public String toString() {
        return moName;
    }
}
