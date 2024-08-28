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
package com.ericsson.oss.services.ap.workflow.cpp.erbs.test.validation;

import static java.lang.String.format;

import org.assertj.core.api.AbstractAssert;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.ap.arquillian.util.Dps;

/**
 * Class used to facilitate asserting MO existence in DPS.
 */
public class DpsAssert extends AbstractAssert<DpsAssert, Dps> {

    private DpsAssert(final Dps actual) {
        super(actual, DpsAssert.class);
    }

    public static DpsAssert assertThat(final Dps actual) {
        return new DpsAssert(actual);
    }

    public ManagedObjectAssert withManagedObject(final String format, final Object... args) {
        return withManagedObject(format(format, args));
    }

    public ManagedObjectAssert withManagedObject(final String fdn) {
        isNotNull();

        final ManagedObject managedObject = actual.findMoByFdn(fdn);
        if (managedObject == null) {
            failWithMessage("Expected to have ManagedObject with FDN <%s> but was null.", fdn);
        }

        return new ManagedObjectAssert(managedObject);
    }

    public DpsAssert hasManagedObject(final String format, final Object... args) {
        return hasManagedObject(format(format, args));
    }

    public DpsAssert hasManagedObject(final String fdn) {
        isNotNull();

        final ManagedObject managedObject = actual.findMoByFdn(fdn);
        if (managedObject == null) {
            failWithMessage("Expected to have ManagedObject with FDN <%s> but was null.", fdn);
        }

        return this;
    }

    public DpsAssert hasNotManagedObject(final String format, final Object... args) {
        return hasNotManagedObject(format(format, args));
    }

    public DpsAssert hasNotManagedObject(final String fdn) {
        isNotNull();

        final ManagedObject managedObject = actual.findMoByFdn(fdn);
        if (managedObject != null) {
            failWithMessage("Expected to not have ManagedObject with FDN <%s>.", fdn);
        }

        return this;
    }
}
