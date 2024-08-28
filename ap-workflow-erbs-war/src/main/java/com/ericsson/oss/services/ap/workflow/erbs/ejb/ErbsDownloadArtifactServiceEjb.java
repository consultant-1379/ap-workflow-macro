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
package com.ericsson.oss.services.ap.workflow.erbs.ejb;

import javax.ejb.Local;
import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.services.ap.api.download.DownloadArtifactService;

@Local
@Stateless
@EServiceQualifier("erbs")
public class ErbsDownloadArtifactServiceEjb implements DownloadArtifactService {

    @Override
    public boolean isOrderedArtifactSupported() {
        return true;
    }
}
