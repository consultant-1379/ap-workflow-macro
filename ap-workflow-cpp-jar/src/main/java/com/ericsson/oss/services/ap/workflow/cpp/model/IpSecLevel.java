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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.ericsson.nms.security.nscs.api.iscf.IpsecArea;
import com.ericsson.oss.services.ap.api.exception.ApApplicationException;

/**
 * IPSec values for CPP based nodes.
 */
public enum IpSecLevel {

    CUS(IpsecArea.TRANSPORT),
    CUSOAM(IpsecArea.TRANSPORT, IpsecArea.OM),
    OAM(IpsecArea.OM);

    private final Set<IpsecArea> wantedIpSecAreas;

    private IpSecLevel(final IpsecArea... wantedIpSecAreas) {
        this.wantedIpSecAreas = EnumSet.copyOf(Arrays.asList(wantedIpSecAreas));
    }

    public static IpSecLevel getIpSecLevel(final String ipSecLevel) {
        if (ipSecLevel.equals(CUS.toString())) {
            return CUS;
        } else if (ipSecLevel.equals(OAM.toString())) {
            return OAM;
        } else if (ipSecLevel.equals(CUSOAM.toString())) {
            return CUSOAM;
        }
        throw new ApApplicationException("Invalid IpSecLevel: " + ipSecLevel);
    }

    public Set<IpsecArea> getWantedIpSecAreas() {
        return wantedIpSecAreas;
    }
}
