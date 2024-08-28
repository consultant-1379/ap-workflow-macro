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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ericsson.oss.services.ap.api.exception.ApApplicationException;

/**
 * Class used to format and manipulate XMLs.
 */
final class XmlFormatter {

    private static final Logger logger = LoggerFactory.getLogger(XmlFormatter.class);
    private static final String DEFAULT_INDENT_SIZE = "4";

    private XmlFormatter() {

    }

    /**
     * Formats an input XML, correctly placing new elements on new lines, and indenting each element with a default indent size of
     * {@value #DEFAULT_INDENT_SIZE}.
     *
     * @param unformattedXml
     *            the XML to format as a byte array
     * @return the formatted XML
     */
    public static byte[] formatXmlFile(final byte[] unformattedXml) {
        try {
            final String unformattedXmlString = new String(unformattedXml, StandardCharsets.UTF_8);
            final Document xmlDocument = convertXmlStringToDocument(unformattedXmlString);
            final Document normalizedXmlDocument = getNormalisedXml(xmlDocument);
            final String formattedXmlString = transformXml(normalizedXmlDocument);
            return formattedXmlString.getBytes(StandardCharsets.UTF_8);
        } catch (final Exception e) {
            logger.warn("Error formatting XML file, returning unformatted file", e);
            return unformattedXml;
        }
    }

    private static String transformXml(final Document xmlDocument) throws TransformerException {
        final StringWriter stringWriter = new StringWriter();
        final Transformer transformer = getTransformer();
        transformer.transform(new DOMSource(xmlDocument), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    private static Document convertXmlStringToDocument(final String xmlFile) {
        try {
            logger.debug("Input XML: [{}]", xmlFile);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            return factory
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlFile.getBytes(StandardCharsets.UTF_8)));
        } catch (final SAXException | IOException | ParserConfigurationException e) {
            throw new ApApplicationException("Error loading input XML file", e);
        }
    }

    private static Document getNormalisedXml(final Document xmlDocument) throws XPathExpressionException {
        xmlDocument.normalize();
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", xmlDocument, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); ++i) {
            final Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }
        return xmlDocument;
    }

    private static Transformer getTransformer() throws TransformerFactoryConfigurationError, TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", DEFAULT_INDENT_SIZE);
        return transformer;
    }
}
