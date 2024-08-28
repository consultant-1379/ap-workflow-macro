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
package com.ericsson.oss.services.ap.workflow.cpp.ejb;

import static com.ericsson.oss.services.ap.workflow.cpp.model.MoType.LICENSING;
import static com.ericsson.oss.services.ap.workflow.cpp.model.MoType.MANAGEDELEMENT;
import static com.ericsson.oss.services.ap.workflow.cpp.model.MoType.OPTIONALFEATURELICENSE;
import static com.ericsson.oss.services.ap.workflow.cpp.model.MoType.SYSTEMFUNCTIONS;
import static com.ericsson.oss.services.ap.workflow.cpp.model.OptionalFeatureLicenseAttributes.LICENSE_STATE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.common.cm.DpsQueries;
import com.ericsson.oss.services.ap.workflow.cpp.api.FeaturesRetriever;
import com.ericsson.oss.services.ap.workflow.cpp.api.OptionalFeatureLicense;

/**
 * Implementation of {@link FeaturesRetriever} used to retrieve <code>OptionalFeatureLicense</code> MOs.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class FeaturesRetrieverEjb implements FeaturesRetriever {

    private static final String LICENSING_FDN = "%s," + MANAGEDELEMENT.toString() + "=1," + SYSTEMFUNCTIONS.toString() + "=1," + LICENSING.toString()
            + "=1";

    @Inject
    private DpsQueries dpsQueries;

    @Override
    public List<OptionalFeatureLicense> getOptionalFeatureLicenseMos(final String meContextFdn) {
        final String licensingFdn = String.format(LICENSING_FDN, meContextFdn, MANAGEDELEMENT.toString());
        final Iterator<ManagedObject> optionalFeatureLicenseMos = dpsQueries
                .findChildMosOfTypesInOwnTransaction(licensingFdn, OPTIONALFEATURELICENSE.toString())
                .execute();
        final List<OptionalFeatureLicense> optionalFeatureLicenses = new ArrayList<>();

        while (optionalFeatureLicenseMos.hasNext()) {
            final ManagedObject optionalFeatureLicenseMo = optionalFeatureLicenseMos.next();
            final String optionalFeatureLicenseFdn = optionalFeatureLicenseMo.getFdn();
            final String licenseState = optionalFeatureLicenseMo.getAttribute(LICENSE_STATE.toString());

            optionalFeatureLicenses.add(new OptionalFeatureLicense(optionalFeatureLicenseFdn, licenseState));
        }

        return optionalFeatureLicenses;
    }
}
