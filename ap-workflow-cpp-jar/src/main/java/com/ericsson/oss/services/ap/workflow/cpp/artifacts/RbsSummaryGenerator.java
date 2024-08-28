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
package com.ericsson.oss.services.ap.workflow.cpp.artifacts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.api.schema.SchemaService;
import com.ericsson.oss.services.ap.workflow.cpp.model.ArtifactType;

/**
 * Generates an RbsSummary file from the schema definition.
 * <p>
 * Supplied attribute values will be set for any matching attributes in the generated XML file.
 */
public class RbsSummaryGenerator {

    private static final String ERROR_GENERATING_FILE = "Error generating RbsSummary file";
    private static final String ERROR_CLOSING_RESOURCES = "Unexpected error closing resources";
    private static final String ERROR_MISSING_ATTRIBUTE = "Missing value for required attribute %s";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    private final XMLOutputFactory factory = XMLOutputFactory.newInstance();

    @Inject
    private SchemaService schemaService;

    private XMLStreamReader reader = null;
    private XMLStreamWriter writer = null;

    /**
     * Generates an RbsSummaryFile from the supplied schema definition.
     * <p>
     * The values of the supplied attributes will be set for any matching attributes in the schema. If no value is supplied for an attribute then the
     * default or fixed value will be used if defined in the schema. In case of a required attribute, an exception will be thrown if no value is
     * supplied and there is no fixed or default value defined in the schema. Optional attributes will be ignored if no value is supplied and there is
     * no fixed or default value defined in the schema.
     *
     * @param nodeType
     *            the type of node
     * @param nodeIdentifier
     *            the node identifier
     * @param attributes
     *            the values to be set for attributes in the schema
     * @return the generated RbsSummary file
     */
    public byte[] generate(final String nodeType, final String nodeIdentifier, final Map<String, Object> attributes) {
        final byte[] rbsSummarySchema = schemaService.readSchema(nodeType, nodeIdentifier, ArtifactType.RBSSUMMARY.toString()).getData();
        final InputStream schemaInputStream = new ByteArrayInputStream(rbsSummarySchema);

        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            return generateFileContent(attributes, schemaInputStream, output);
        } catch (final XMLStreamException | IOException e) {
            throw new ApServiceException(ERROR_GENERATING_FILE, e);
        } finally {
            closeStreams();
        }
    }

    private byte[] generateFileContent(final Map<String, Object> attributes, final InputStream schemaInputStream, final ByteArrayOutputStream output)
            throws XMLStreamException {
        reader = xmlInputFactory.createXMLStreamReader(schemaInputStream);
        writer = factory.createXMLStreamWriter(output);
        writer.writeStartDocument();
        parseSchema(attributes);
        writer.writeEndDocument();
        writer.flush();
        return XmlFormatter.formatXmlFile(output.toByteArray());
    }

    private void parseSchema(final Map<String, Object> attributes) throws XMLStreamException {
        while (reader.hasNext()) {
            reader.next();
            if (isStartElementEvent()) {
                writeStartElement();
            } else if (isStartAttributeEvent()) {
                writeAttribute(attributes);
            } else if (isEndElementEvent()) {
                writeEndElement();
            }
        }
    }

    private boolean isStartElementEvent() {
        return reader.getEventType() == 1 && isElement();
    }

    private boolean isEndElementEvent() {
        return reader.getEventType() == 2 && isElement();
    }

    private boolean isStartAttributeEvent() {
        return reader.getEventType() == 1 && isAttribute();
    }

    private void writeStartElement() throws XMLStreamException {
        final String elementName = reader.getAttributeValue(null, "name");
        writer.writeStartElement(elementName);
    }

    private void writeAttribute(final Map<String, Object> attributes) throws XMLStreamException {
        final String elementName = reader.getAttributeValue(null, "name");
        final String suppliedValue = (String) attributes.get(elementName);
        final boolean required = isRequiredAttribute();
        final String defaultValue = getDefaultAttributeValue();

        if (isMissingRequiredAttribute(required, suppliedValue, defaultValue)) {
            throw new ApServiceException(String.format(ERROR_MISSING_ATTRIBUTE, elementName));
        }

        if (ignoreOptionalAttribute(required, suppliedValue, defaultValue)) {
            return;
        }

        final String attrValue = suppliedValue != null ? suppliedValue : defaultValue;
        writer.writeAttribute(elementName, attrValue);
    }

    private void writeEndElement() throws XMLStreamException {
        writer.writeEndElement();
    }

    private void closeStreams() {
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (final XMLStreamException e) {
            logger.error(ERROR_CLOSING_RESOURCES, e);
        }
    }

    private boolean isElement() {
        return "element".equals(reader.getName().getLocalPart());
    }

    private boolean isAttribute() {
        return "attribute".equals(reader.getName().getLocalPart());
    }

    private String getDefaultAttributeValue() {
        final String defaultValue = reader.getAttributeValue(null, "default");
        final String fixedValue = reader.getAttributeValue(null, "fixed");
        return fixedValue == null ? defaultValue : fixedValue;
    }

    private boolean isRequiredAttribute() {
        final String use = reader.getAttributeValue(null, "use");
        return use != null && "required".equals(use);
    }

    private static boolean ignoreOptionalAttribute(final boolean required, final String suppliedValue, final String defaultValue) {
        return !required && suppliedValue == null && defaultValue == null;
    }

    private static boolean isMissingRequiredAttribute(final boolean required, final String suppliedValue, final String defaultValue) {
        return required && suppliedValue == null && defaultValue == null;
    }
}
