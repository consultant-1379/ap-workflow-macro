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
package com.ericsson.oss.services.ap.workflow.cpp.artifacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link XmlFormatter}.
 */
public class XmlFormatterTest {

    @Test
    public void whenFormattingXmlFile_thenWhitespaceIsRemoved_andFormattedAndIndentedFileIsReturned() {
        final String inputXml = "<Element1>    </Element1>";
        final byte[] result = XmlFormatter.formatXmlFile(inputXml.getBytes());
        final String expected = "<Element1/>";
        assertEqualsWithoutWhitespace(expected, new String(result));
    }

    @Test
    public void whenFormattingXmlFile_andErrorOccurs_thenInputIsReturned() {
        final byte[] result = XmlFormatter.formatXmlFile(null);
        assertNull(result);
    }

    private static void assertEqualsWithoutWhitespace(final String expected, final String actual) {
        assertEquals(removeWhitespace(expected), removeWhitespace(actual));
    }

    private static String removeWhitespace(final String input) {
        return input.replaceAll("\\s", "");
    }
}
