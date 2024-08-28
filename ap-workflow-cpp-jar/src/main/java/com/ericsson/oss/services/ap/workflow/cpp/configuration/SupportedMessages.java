/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.ap.workflow.cpp.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Supported correlation message keys for ERBS workflows. Each entry maps to a message key defined in its corresponding
 * <code>WorkflowCorrelationMessage</code> instance.
 */
public enum SupportedMessages {

    BIND,
    DELETE,
    NODE_UP;

    private static final List<SupportedMessages> VALUES_AS_LIST = Collections.unmodifiableList(Arrays.asList(values()));

    /**
     * Checks that the message key passed is supported.
     *
     * @param messageKey
     *            the message key to check
     * @return true if the message is supported
     */
    public static boolean isMessageSupported(final String messageKey) {
        for (final SupportedMessages supportedMessage : SupportedMessages.valuesAsList()) {
            if (supportedMessage.toString().equalsIgnoreCase(messageKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all {@link SupportedMessages} values as a {@link List}.
     * <p>
     * To be used instead of {@link #values()}, as it does not create a new array for each invocation.
     *
     * @return all enum values
     */
    public static List<SupportedMessages> valuesAsList() {
        return VALUES_AS_LIST;
    }
}
