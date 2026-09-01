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
    void mapsExactlyTheReviewedWidgetsToUniqueIconBases() {
        LinkedHashMap<String, String> actual = new LinkedHashMap<>();
        BuiltInWidgetCatalog.getDefault().definitions().forEach(definition ->
                FlutterWidgetIconRegistry.findIconPath(definition.typeId())
                        .ifPresent(path -> actual.put(definition.typeId().value(), path)));

        assertEquals(EXPECTED, actual);
        assertEquals(EXPECTED.size(), new HashSet<>(actual.values()).size(),
                "each reviewed widget must have a dedicated icon base");
    }

    @Test
    void leavesUnknownAndUnreviewedTypesUnmapped() {
        assertTrue(FlutterWidgetIconRegistry.findIconPath(
                new WidgetTypeId("example.extension.Calendar")).isEmpty());
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
                    () -> base + " must not reuse another reviewed widget geometry");
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

    @Test
    void sizedBoxFamilyUsesExactReviewedDimensionGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "sizedbox.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-width=1, width=13, x=1.5, y=1.5]",
                "rect[height=7, rx=1, stroke-width=1, width=7, x=4, y=4]",
                "path[d=M4 12.5h7M4 11.7v1.6M11 11.7v1.6M12.5 4v7M11.7 4h1.6M11.7 11h1.6, stroke-linecap=round, stroke-width=1]",
                "circle[cx=4, cy=4, r=.8]",
                "circle[cx=11, cy=11, r=.8]"), light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-width=2, width=26, x=3, y=3]",
                "rect[height=14, rx=2, stroke-width=2, width=14, x=8, y=8]",
                "path[d=M8 25h14M8 23.4v3.2M22 23.4v3.2M25 8v14M23.4 8h3.2M23.4 22h3.2, stroke-linecap=round, stroke-width=2]",
                "circle[cx=8, cy=8, r=1.6]",
                "circle[cx=22, cy=22, r=1.6]"), light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[stroke=#1565C0]",
                "circle[fill=#26C6DA]",
                "circle[fill=#26C6DA]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[stroke=#29B6F6]",
                "circle[fill=#80DEEA]",
                "circle[fill=#80DEEA]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void aspectRatioFamilyUsesExactReviewedRatioGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "aspectratio.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=11, rx=1.5, stroke-width=1, width=13, x=1.5, y=2.5]",
                "rect[height=6, rx=1, stroke-width=1, width=9, x=3.5, y=5]",
                "path[d=M4.5 10.5l7-4M4 6.5v4h4M12 9.5v-4H8, stroke-linecap=round, stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=22, rx=3, stroke-width=2, width=26, x=3, y=5]",
                "rect[height=12, rx=2, stroke-width=2, width=18, x=7, y=10]",
                "path[d=M9 21l14-8M8 13v8h8M24 19v-8h-8, stroke-linecap=round, stroke-linejoin=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[fill=none, stroke=#1565C0]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[fill=none, stroke=#29B6F6]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void containerFamilyUsesExactReviewedLayerGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "container.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "path[d=M3 14.5h10.5V4, stroke-linecap=round, stroke-width=1]",
                "rect[height=12, rx=2, stroke-width=1, width=12, x=1.5, y=1.5]",
                "rect[height=6, rx=1, stroke-width=1, width=6, x=4.5, y=4.5]",
                "path[d=M2.5 5V3.5a1 1 0 0 1 1-1H5, stroke-linecap=round, stroke-width=1.2]"),
                light16.geometry());
        assertEquals(List.of(
                "path[d=M6 29h21V8, stroke-linecap=round, stroke-width=2]",
                "rect[height=24, rx=4, stroke-width=2, width=24, x=3, y=3]",
                "rect[height=12, rx=2, stroke-width=2, width=12, x=9, y=9]",
                "path[d=M5 10V7a2 2 0 0 1 2-2h3, stroke-linecap=round, stroke-width=2.4]"),
                light32.geometry());
        assertEquals(List.of(
                "path[fill=none, opacity=.75, stroke=#D97706]",
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[fill=none, stroke=#26C6DA]"), light16.paint());
        assertEquals(List.of(
                "path[fill=none, opacity=.9, stroke=#FFB74D]",
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[fill=none, stroke=#29B6F6]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void alignFamilyUsesExactReviewedAnchorGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "align.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-width=1, width=13, x=1.5, y=1.5]",
                "path[d=M8 3.5v9M3.5 8h9, stroke-dasharray=1 1.5, stroke-linecap=round, stroke-width=1]",
                "rect[height=4, rx=.8, stroke-width=1, width=4, x=8.5, y=3.5]",
                "circle[cx=10.5, cy=5.5, r=.7]"), light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-width=2, width=26, x=3, y=3]",
                "path[d=M16 7v18M7 16h18, stroke-dasharray=2 3, stroke-linecap=round, stroke-width=2]",
                "rect[height=8, rx=1.6, stroke-width=2, width=8, x=17, y=7]",
                "circle[cx=21, cy=11, r=1.4]"), light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "path[fill=none, stroke=#42A5F5]",
                "rect[fill=#80DEEA, stroke=#1565C0]",
                "circle[fill=#00838F]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "path[fill=none, stroke=#64B5F6]",
                "rect[fill=#4DD0E1, stroke=#90CAF9]",
                "circle[fill=#B2EBF2]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void fractionallySizedBoxFamilyUsesExactReviewedFractionGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "fractionallysizedbox.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-width=1, width=13, x=1.5, y=1.5]",
                "rect[height=6, rx=1, stroke-width=1, width=7, x=3.5, y=4.5]",
                "path[d=M3.5 12.5h7M3.5 11.7v1.6M10.5 11.7v1.6M12.5 4.5v6M11.7 4.5h1.6M11.7 10.5h1.6, stroke-linecap=round, stroke-width=1]",
                "path[d=M5.2 7.5h3.6M5.2 7.5l1-1M5.2 7.5l1 1M8.8 7.5l-1-1M8.8 7.5l-1 1, stroke-linecap=round, stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-width=2, width=26, x=3, y=3]",
                "rect[height=12, rx=2, stroke-width=2, width=14, x=7, y=9]",
                "path[d=M7 25h14M7 23.4v3.2M21 23.4v3.2M25 9v12M23.4 9h3.2M23.4 21h3.2, stroke-linecap=round, stroke-width=2]",
                "path[d=M10.4 15h7.2M10.4 15l2-2M10.4 15l2 2M17.6 15l-2-2M17.6 15l-2 2, stroke-linecap=round, stroke-linejoin=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[fill=none, stroke=#1565C0]",
                "path[fill=none, stroke=#26C6DA]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[fill=none, stroke=#29B6F6]",
                "path[fill=none, stroke=#80DEEA]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void opacityFamilyUsesExactReviewedTransparencyGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "opacity.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=2, stroke-width=1, width=13, x=1.5, y=1.5]",
                "path[d=M8 3.2C6.5 5.4 4.5 7.5 4.5 10a3.5 3.5 0 0 0 7 0C11.5 7.5 9.5 5.4 8 3.2z, stroke-linejoin=round, stroke-width=1]",
                "path[d=M5.2 10c1.1-.7 2-.7 2.8 0s1.7.7 2.8 0, stroke-linecap=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=4, stroke-width=2, width=26, x=3, y=3]",
                "path[d=M16 6.4C13 10.8 9 15 9 20a7 7 0 0 0 14 0C23 15 19 10.8 16 6.4z, stroke-linejoin=round, stroke-width=2]",
                "path[d=M10.4 20c2.2-1.4 4-1.4 5.6 0s3.4 1.4 5.6 0, stroke-linecap=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "path[fill-opacity=.65, fill=#80DEEA, stroke=#1565C0]",
                "path[fill=none, stroke=#00838F]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "path[fill-opacity=.75, fill=#4DD0E1, stroke=#64B5F6]",
                "path[fill=none, stroke=#B2EBF2]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void stackFamilyUsesExactReviewedLayerGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "stack.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=9, rx=1.5, stroke-width=1, width=9, x=1.5, y=1.5]",
                "rect[height=9, rx=1.5, stroke-width=1, width=9, x=3.5, y=3.5]",
                "rect[height=9, rx=1.5, stroke-width=1, width=9, x=5.5, y=5.5]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=18, rx=3, stroke-width=2, width=18, x=3, y=3]",
                "rect[height=18, rx=3, stroke-width=2, width=18, x=7, y=7]",
                "rect[height=18, rx=3, stroke-width=2, width=18, x=11, y=11]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "rect[fill=#80DEEA, stroke=#1565C0]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "rect[fill=#4DD0E1, stroke=#90CAF9]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
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
        expected.put("flutter.material.AppBar", ICON_ROOT + "appbar.svg");
        expected.put("flutter.material.ElevatedButton",
                ICON_ROOT + "elevatedbutton.svg");
        expected.put("flutter.material.Scaffold", ICON_ROOT + "scaffold.svg");
        expected.put("flutter.widgets.Center", ICON_ROOT + "center.svg");
        expected.put("flutter.widgets.Column", ICON_ROOT + "column.svg");
        expected.put("flutter.widgets.Icon", ICON_ROOT + "icon.svg");
        expected.put("flutter.widgets.Padding", ICON_ROOT + "padding.svg");
        expected.put("flutter.widgets.Row", ICON_ROOT + "row.svg");
        expected.put("flutter.widgets.SizedBox", ICON_ROOT + "sizedbox.svg");
        expected.put("flutter.widgets.AspectRatio", ICON_ROOT + "aspectratio.svg");
        expected.put("flutter.widgets.Container", ICON_ROOT + "container.svg");
        expected.put("flutter.widgets.Opacity", ICON_ROOT + "opacity.svg");
        expected.put("flutter.widgets.Align", ICON_ROOT + "align.svg");
        expected.put("flutter.widgets.FractionallySizedBox",
                ICON_ROOT + "fractionallysizedbox.svg");
        expected.put("flutter.widgets.Stack", ICON_ROOT + "stack.svg");
        expected.put("flutter.widgets.Text", ICON_ROOT + "text.svg");
        return Map.copyOf(expected);
    }

    private record SvgResource(
            List<String> geometry,
            List<String> topology,
            List<String> paint) {
    }
}
