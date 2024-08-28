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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.classic.ServiceFinderBean;
import com.ericsson.oss.services.ap.api.UploadArtifactService;
import com.ericsson.oss.services.ap.api.status.State;
import com.ericsson.oss.services.ap.workflow.cpp.api.CppNodeType;
import com.ericsson.oss.services.ap.workflow.cpp.api.WorkflowTaskFacade;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

@Local
@Stateless
@EServiceQualifier("erbs")
public class ErbsUploadArtifactService implements UploadArtifactService {

    protected final ServiceFinderBean serviceFinder = new ServiceFinderBean();

    private static final Map<String, Set<String>> supportedUploadTypes = new HashMap<>();
    private static final Set<String> nodeFileArtifacts = new HashSet<>();

    private static final Set<String> fileArtifactStates = new HashSet<>();
    private static final Set<String> configurationArtifactStates = new HashSet<>();
    private static final Set<String> licenseArtifactStates = new HashSet<>();

    static {
        fileArtifactStates.add(State.ORDER_COMPLETED.toString());
        fileArtifactStates.add(State.ORDER_FAILED.toString());
        fileArtifactStates.add(State.BIND_STARTED.toString());
        fileArtifactStates.add(State.BIND_COMPLETED.toString());

        licenseArtifactStates.add(State.ORDER_FAILED.toString());
        licenseArtifactStates.add(State.ORDER_ROLLBACK_FAILED.toString());

        for (final State state : State.values()) {
            configurationArtifactStates.add(state.toString());
        }

        supportedUploadTypes.put("siteBasic", fileArtifactStates);
        supportedUploadTypes.put("siteEquipment", fileArtifactStates);
        supportedUploadTypes.put("configuration", configurationArtifactStates);
        supportedUploadTypes.put("licenseFile", licenseArtifactStates);

        nodeFileArtifacts.add("siteBasic");
        nodeFileArtifacts.add("siteEquipment");
        nodeFileArtifacts.add("licenseFile");
    }

    @Override
    public Set<String> getSupportedUploadTypes() {
        return new HashSet<>(supportedUploadTypes.keySet());
    }

    @Override
    public Set<String> getValidStatesForUpload(final String artifactType) {
        return supportedUploadTypes.get(artifactType);
    }

    @Override
    public boolean isNodeArtifactFile(final String artifactType) {
        return nodeFileArtifacts.contains(artifactType);
    }

    @Override
    public void createGeneratedArtifact(final String artifactType, final String apNodeFdn) {
        if (isNodeArtifactFile(artifactType) && !ArtifactType.LICENSEFILE.toString().equals(artifactType)) {
            final WorkflowTaskFacade workflowTaskFacade = serviceFinder.find(WorkflowTaskFacade.class);
            workflowTaskFacade.uploadGeneratedArtifact(artifactType, apNodeFdn, CppNodeType.ERBS);
        }
    }
}
