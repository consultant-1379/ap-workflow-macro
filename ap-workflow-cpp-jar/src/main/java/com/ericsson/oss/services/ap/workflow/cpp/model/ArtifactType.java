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

import static java.util.stream.Collectors.toMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Definition of the different artifacts supported by CPP based nodes.
 */
public enum ArtifactType {

    RBSSUMMARY("RbsSummary"),
    SITEBASIC("SiteBasic"),
    SITEEQUIPMENT("SiteEquipment"),
    SITEINSTALL("SiteInstallation"),
    SITEINSTALLFORBIND("SiteInstallForBind"),
    LICENSEFILE("licenseFile");

    private final String type;

    private static final Map<String, ArtifactType> stringToEnum = Stream.of(values())
        .collect(toMap(e -> e.toString().toLowerCase(), e -> e));

    ArtifactType(final String type) {
        this.type = type;
    }

    /**
     * Finds a matching {@link ArtifactType} based off the input string.
     *
     * @param artifactType
     *            the string representation of the ArtifactType to find
     * @return the {@link ArtifactType}
     * @throws IllegalArgumentException
     *             thrown if no matching ArtifactType exists
     */
    public static ArtifactType getArtifactType(final String artifactType) {
        if (stringToEnum.containsKey(artifactType.toLowerCase())) {
            return stringToEnum.get(artifactType.toLowerCase());
        }
        throw new IllegalArgumentException("No ArtifactType found for wanted type: " + artifactType);
    }

    @Override
    public String toString() {
        return type;
    }
}
