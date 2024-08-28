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
 * A modelled security attribute for CPP nodes.
 */
public enum SecurityAttribute {

    ENROLLMENT_MODE("enrollmentMode"),
    IPSEC_LEVEL("ipSecLevel"),
    ISCF_FILE_LOCATION("iscfFileLocation"),
    MIN_SEC_LEVEL("minimumSecurityLevel"),
    OPT_SEC_LEVEL("optimumSecurityLevel"),
    RBS_INTEGRITY_CODE("rbsIntegrityCode"),
    SECURITY_CONFIG_CHECKSUM("securityConfigChecksum"),
    SUBJECT_ALT_NAME("subjectAltName"),
    SUBJECT_ALT_NAME_TYPE("subjectAltNameType");

    private final String attributeName;

    private SecurityAttribute(final String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public String toString() {
        return attributeName;
    }
}
