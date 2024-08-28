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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.assertj.core.api.AbstractAssert;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

/**
 * Class used to facilitate asserting MO attribute values in DPS.
 */
public class ManagedObjectAssert extends AbstractAssert<ManagedObjectAssert, ManagedObject> {

    ManagedObjectAssert(final ManagedObject managedObject) {
        super(managedObject, ManagedObjectAssert.class);
    }

    public ManagedObjectAssert withFdnAttributeValue(final String fdn, final String name, final Object expected) {
        isNotNull();

        final Object attributeValue = actual.getAttribute(name);

        if (attributeValue == null) {
            failWithMessage("Expected ManagedObject: <%s> to have valid value for attribute: <%s> but it does not.", fdn, name);
        }

        if (!expected.equals(attributeValue)) {
            failWithMessage("Expected ManagedObject: <%s> attribute: <%s> to be [<%s>] but it was [<%s>].", fdn, name, expected, attributeValue);
        }

        return this;
    }

    public ManagedObjectAssert withNotEmptyAttributeValue(final String name) {
        isNotNull();

        final Object attributeValue = actual.getAttribute(name);

        if (!(attributeValue instanceof String) || isBlank((String) attributeValue) || attributeValue == null) {
            failWithMessage("Expected ManagedObject's attribute <%s> to be a non blank string but it was not.", name);
        }

        return this;
    }

    public ManagedObjectAssert withBlankAttributeValue(final String name) {
        isNotNull();

        final Object attributeValue = actual.getAttribute(name);

        if (attributeValue != null) {
            failWithMessage("Expected ManagedObject to not have attribute <%s> but it does [<%s>].", name, attributeValue);
        }

        if (attributeValue instanceof String && isNotBlank((String) attributeValue)) {
            failWithMessage("Expected ManagedObject's attribute <%s> to be a blank string but it was [<%s>].", name, attributeValue);
        }

        return this;
    }
}