package dev.flutter.netbeans.plugin.designer.icons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class FlutterWidgetIconRegistryTest {
    private static final String ICON_ROOT =
            "dev/flutter/netbeans/plugin/designer/icons/widgets/";
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private static final Set<String> PAINT_ATTRIBUTES = Set.of(
            "color", "fill", "fill-opacity", "opacity", "stroke", "stroke-opacity");
    private static final Set<String> FORBIDDEN_ELEMENTS = Set.of(
            "font", "font-face", "foreignobject", "image", "script", "style", "text");

    private static final Map<String, String> EXPECTED = expectedMappings();

    @Test
    void mapsExactlyTheSixReviewedCoreWidgetsToUniqueIconBases() {
        LinkedHashMap<String, String> actual = new LinkedHashMap<>();
        BuiltInWidgetCatalog.getDefault().definitions().forEach(definition ->
                FlutterWidgetIconRegistry.findIconPath(definition.typeId())
                        .ifPresent(path -> actual.put(definition.typeId().value(), path)));

        assertEquals(EXPECTED, actual);
        assertEquals(EXPECTED.size(), new HashSet<>(actual.values()).size(),
                "each CORE_V1 widget must have a dedicated icon base");
    }

    @Test
    void leavesUnknownAndUnreviewedTypesUnmapped() {
        assertTrue(FlutterWidgetIconRegistry.findIconPath(
                new WidgetTypeId("example.extension.Calendar")).isEmpty());
        assertTrue(FlutterWidgetIconRegistry.findIconPath(
                new WidgetTypeId("flutter.material.AppBar")).isEmpty());
        assertTrue(FlutterWidgetIconRegistry.findIconPath(
                new WidgetTypeId("flutter.widgets.Icon")).isEmpty());
        assertTrue(FlutterWidgetIconRegistry.findIconPath(
                new WidgetTypeId("flutter.widgets.SizedBox")).isEmpty());
        assertTrue(FlutterWidgetIconRegistry.findIconPath(
                new WidgetTypeId("flutter.material.ElevatedButton")).isEmpty());
    }

    @Test
    void packagesSafeLightDarkSixteenAndThirtyTwoPixelSvgFamilies() throws Exception {
        Set<List<String>> lightSixteenGeometry = new HashSet<>();
        for (String base : EXPECTED.values()) {
            SvgResource light16 = readSvg(base, 16);
            SvgResource dark16 = readSvg(variant(base, false, true), 16);
            SvgResource light32 = readSvg(variant(base, true, false), 32);
            SvgResource dark32 = readSvg(variant(base, true, true), 32);

            assertTrue(lightSixteenGeometry.add(light16.geometry()),
                    () -> base + " must not reuse another CORE_V1 widget geometry");
            assertEquals(light16.geometry(), dark16.geometry(),
                    () -> base + " light and dark 16 px variants must share geometry");
            assertEquals(light32.geometry(), dark32.geometry(),
                    () -> base + " light and dark 32 px variants must share geometry");
            assertNotEquals(light16.paint(), dark16.paint(),
                    () -> base + " dark 16 px variant must use theme-specific paint");
            assertNotEquals(light32.paint(), dark32.paint(),
                    () -> base + " dark 32 px variant must use theme-specific paint");
            assertEquals(light16.topology(), light32.topology(),
                    () -> base + " 16 and 32 px variants must share shape topology");
            assertEquals(dark16.topology(), dark32.topology(),
                    () -> base + " dark 16 and 32 px variants must share shape topology");
        }
        assertEquals(EXPECTED.size(), lightSixteenGeometry.size());
    }

    private static SvgResource readSvg(String resource, int expectedSize) throws Exception {
        byte[] bytes = readResource(resource);
        String source = new String(bytes, StandardCharsets.UTF_8);
        assertFalse(source.toLowerCase(Locale.ROOT).contains("<!doctype"),
                () -> resource + " must not declare a DOCTYPE");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(bytes));
        Element root = document.getDocumentElement();

        assertEquals("svg", localName(root), resource);
        assertEquals(SVG_NAMESPACE, root.getNamespaceURI(), resource);
        assertEquals(Integer.toString(expectedSize), root.getAttribute("width"), resource);
        assertEquals(Integer.toString(expectedSize), root.getAttribute("height"), resource);
        assertEquals("0 0 " + expectedSize + " " + expectedSize,
                root.getAttribute("viewBox"), resource);

        assertSafeSvg(document, resource);
        return new SvgResource(
                geometrySignature(document, true),
                geometrySignature(document, false),
                paintSignature(document));
    }

    private static void assertSafeSvg(Document document, String resource) {
        NodeList elements = document.getElementsByTagNameNS("*", "*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            String elementName = localName(element).toLowerCase(Locale.ROOT);
            assertFalse(FORBIDDEN_ELEMENTS.contains(elementName),
                    () -> resource + " contains forbidden <" + elementName + "> content");

            NamedNodeMap attributes = element.getAttributes();
            for (int attributeIndex = 0; attributeIndex < attributes.getLength(); attributeIndex++) {
                Node attribute = attributes.item(attributeIndex);
                String attributeName = localName(attribute).toLowerCase(Locale.ROOT);
                String value = attribute.getNodeValue().trim();
                String lowerValue = value.toLowerCase(Locale.ROOT);

                assertFalse(attributeName.startsWith("on"),
                        () -> resource + " contains script event attribute " + attributeName);
                assertFalse(attributeName.contains("font") || lowerValue.contains("font-family"),
                        () -> resource + " must not rely on an external font");
                if (attributeName.equals("href")) {
                    assertTrue(value.startsWith("#"),
                            () -> resource + " contains external href " + value);
                }
                if (lowerValue.contains("url(")) {
                    assertTrue(lowerValue.matches(".*url\\(\\s*#[^)]+\\).*"),
                            () -> resource + " contains an external URL reference");
                }
            }
        }
    }

    private static List<String> geometrySignature(
            Document document,
            boolean includeValues) {
        ArrayList<String> result = new ArrayList<>();
        NodeList elements = document.getElementsByTagNameNS(SVG_NAMESPACE, "*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (element == document.getDocumentElement()) {
                continue;
            }
            ArrayList<String> attributes = new ArrayList<>();
            NamedNodeMap sourceAttributes = element.getAttributes();
            for (int attributeIndex = 0;
                    attributeIndex < sourceAttributes.getLength();
                    attributeIndex++) {
                Node attribute = sourceAttributes.item(attributeIndex);
                String name = localName(attribute);
                if (PAINT_ATTRIBUTES.contains(name) || name.equals("stroke-width")) {
                    continue;
                }
                attributes.add(name + (includeValues ? "=" + attribute.getNodeValue() : ""));
            }
            if (element.hasAttribute("stroke")) {
                String width = element.hasAttribute("stroke-width")
                        ? element.getAttribute("stroke-width")
                        : "1";
                attributes.add("stroke-width" + (includeValues ? "=" + width : ""));
            }
            attributes.sort(String::compareTo);
            result.add(localName(element) + attributes);
        }
        return List.copyOf(result);
    }

    private static List<String> paintSignature(Document document) {
        ArrayList<String> result = new ArrayList<>();
        NodeList elements = document.getElementsByTagNameNS(SVG_NAMESPACE, "*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (element == document.getDocumentElement()) {
                continue;
            }
            ArrayList<String> attributes = new ArrayList<>();
            NamedNodeMap sourceAttributes = element.getAttributes();
            for (int attributeIndex = 0;
                    attributeIndex < sourceAttributes.getLength();
                    attributeIndex++) {
                Node attribute = sourceAttributes.item(attributeIndex);
                String name = localName(attribute);
                if (PAINT_ATTRIBUTES.contains(name)) {
                    attributes.add(name + "=" + attribute.getNodeValue());
                }
            }
            attributes.sort(String::compareTo);
            result.add(localName(element) + attributes);
        }
        return List.copyOf(result);
    }

    private static byte[] readResource(String resource) throws IOException {
        try (InputStream input = FlutterWidgetIconRegistryTest.class
                .getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(input, "missing widget SVG resource: " + resource);
            return input.readAllBytes();
        }
    }

    private static String variant(String base, boolean thirtyTwo, boolean dark) {
        String stem = base.substring(0, base.length() - ".svg".length());
        return stem + (thirtyTwo ? "32" : "") + (dark ? "_dark" : "") + ".svg";
    }

    private static String localName(Node node) {
        return node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
    }

    private static Map<String, String> expectedMappings() {
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("flutter.material.Scaffold", ICON_ROOT + "scaffold.svg");
        expected.put("flutter.widgets.Center", ICON_ROOT + "center.svg");
        expected.put("flutter.widgets.Column", ICON_ROOT + "column.svg");
        expected.put("flutter.widgets.Padding", ICON_ROOT + "padding.svg");
        expected.put("flutter.widgets.Row", ICON_ROOT + "row.svg");
        expected.put("flutter.widgets.Text", ICON_ROOT + "text.svg");
        return Map.copyOf(expected);
    }

    private record SvgResource(
            List<String> geometry,
            List<String> topology,
            List<String> paint) {
    }
}
