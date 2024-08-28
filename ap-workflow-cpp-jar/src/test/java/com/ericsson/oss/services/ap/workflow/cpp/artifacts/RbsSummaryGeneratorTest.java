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

import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.NODE_IDENTIFIER_VALUE;
import static com.ericsson.oss.services.ap.common.test.stubs.dps.NodeDescriptor.VALID_NODE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.ericsson.oss.services.ap.api.exception.ApServiceException;
import com.ericsson.oss.services.ap.api.schema.SchemaData;
import com.ericsson.oss.services.ap.api.schema.SchemaService;

/**
 * Unit tests for {@link RbsSummaryGenerator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RbsSummaryGeneratorTest {

    private static final String ISCF_ATTRIBUTE = "initialSecurityConfigurationFilePath";
    private static final String LICENSE_KEY_ATTRIBUTE = "licensingKeyFilePath";
    private static final String REVISION_ATTRIBUTE = "revision";
    private static final String SITE_BASIC_ATTRIBUTE = "siteBasicFilePath";
    private static final String SITE_EQUIPMENT_ATTRIBUTE = "siteEquipmentFilePath";
    private static final String UPGRADE_PACKAGE_ATTRIBUTE = "upgradePackageFilePath";

    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private final RbsSummaryGenerator generator = new RbsSummaryGenerator();

    @Before
    public void setUp() throws IOException {
        final byte[] fileContents = IOUtils.toByteArray(getClass().getResourceAsStream("/xml/RbsSummary.xsd"));
        final SchemaData schema = new SchemaData("name", "type", "id", fileContents, "/schema_location");
        when(schemaService.readSchema(anyString(), anyString(), anyString())).thenReturn(schema);
    }

    @Test
    public void testRequiredAttributesSet() {
        final Map<String, Object> suppliedAttributes = new HashMap<>();
        suppliedAttributes.put(SITE_BASIC_ATTRIBUTE, "SiteBasic.xml");
        suppliedAttributes.put(SITE_EQUIPMENT_ATTRIBUTE, "RbsEquipment.xml");

        final byte[] rbsSummaryFile = generator.generate(VALID_NODE_TYPE, NODE_IDENTIFIER_VALUE, suppliedAttributes);
        final Map<String, Object> generatedAttributes = readXmlAttributes(rbsSummaryFile);
        assertEquals("SiteBasic.xml", generatedAttributes.get(SITE_BASIC_ATTRIBUTE));
        assertEquals("RbsEquipment.xml", generatedAttributes.get(SITE_EQUIPMENT_ATTRIBUTE));
        assertEquals("F", generatedAttributes.get(REVISION_ATTRIBUTE));
    }

    @Test
    public void testOptionalAttributesSet() {
        final Map<String, Object> suppliedAttributes = new HashMap<>();
        suppliedAttributes.put(SITE_BASIC_ATTRIBUTE, "SiteBasic.xml");
        suppliedAttributes.put(SITE_EQUIPMENT_ATTRIBUTE, "RbsEquipment.xml");
        suppliedAttributes.put(LICENSE_KEY_ATTRIBUTE, "lic.xml");
        suppliedAttributes.put(UPGRADE_PACKAGE_ATTRIBUTE, "upgradefile");
        suppliedAttributes.put(ISCF_ATTRIBUTE, "icf.xml");

        final byte[] rbsSummaryFile = generator.generate(VALID_NODE_TYPE, NODE_IDENTIFIER_VALUE, suppliedAttributes);

        final Map<String, Object> generatedAttributes = readXmlAttributes(rbsSummaryFile);
        assertEquals("lic.xml", generatedAttributes.get(LICENSE_KEY_ATTRIBUTE));
        assertEquals("upgradefile", generatedAttributes.get(UPGRADE_PACKAGE_ATTRIBUTE));
        assertEquals("icf.xml", generatedAttributes.get(ISCF_ATTRIBUTE));
    }

    @Test
    public void testOptionalAttributesIgnoredWhenNoValueSupplied() {
        final Map<String, Object> suppliedAttributes = new HashMap<>();
        suppliedAttributes.put(SITE_BASIC_ATTRIBUTE, "SiteBasic.xml");
        suppliedAttributes.put(SITE_EQUIPMENT_ATTRIBUTE, "RbsEquipment.xml");

        final byte[] rbsSummaryFile = generator.generate(VALID_NODE_TYPE, NODE_IDENTIFIER_VALUE, suppliedAttributes);

        final Map<String, Object> generatedAttributes = readXmlAttributes(rbsSummaryFile);
        assertNull(generatedAttributes.get(LICENSE_KEY_ATTRIBUTE));
        assertNull(generatedAttributes.get(UPGRADE_PACKAGE_ATTRIBUTE));
        assertNull(generatedAttributes.get(ISCF_ATTRIBUTE));
    }

    @Test(expected = ApServiceException.class)
    public void testMissingRequiredAttribute() {
        final Map<String, Object> attributes = new HashMap<>();
        generator.generate(VALID_NODE_TYPE, NODE_IDENTIFIER_VALUE, attributes);
    }

    private Map<String, Object> readXmlAttributes(final byte[] rbsSummaryFile) {
        final Map<String, Object> attributes = new HashMap<>();
        final DocumentBuilderFactory documentbuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            final DocumentBuilder documentBuilder = documentbuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(new ByteArrayInputStream(rbsSummaryFile));

            final NamedNodeMap configurationFiles = document.getElementsByTagName("ConfigurationFiles").item(0).getAttributes();
            final NamedNodeMap format = document.getElementsByTagName("Format").item(0).getAttributes();

            attributes.putAll(readNamedNodeMap(configurationFiles));
            attributes.putAll(readNamedNodeMap(format));
        } catch (final IOException | ParserConfigurationException | SAXException e) {
            fail("Error reading attributes in generated xml");
        }
        return attributes;
    }

    private Map<String, Object> readNamedNodeMap(final NamedNodeMap nodeMap) {
        final Map<String, Object> attributes = new HashMap<>();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            final Node node = nodeMap.item(i);
            attributes.put(node.getNodeName(), node.getNodeValue());
        }
        return attributes;
    }
}
