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
    void imageFamilyUsesExactReviewedRasterFrameGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "image.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=11, rx=1.5, stroke-width=1, width=13, x=1.5, y=2.5]",
                "circle[cx=11.5, cy=5.5, r=1.4, stroke-width=.7]",
                "path[d=M2.5 11l3.2-3 2.2 2 2.2-2.4 3.4 3.4v1.5h-11z, stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=22, rx=3, stroke-width=2, width=26, x=3, y=5]",
                "circle[cx=23, cy=11, r=2.8, stroke-width=1.4]",
                "path[d=M5 22l6.4-6 4.4 4 4.4-4.8L27 22v3H5z, stroke-linejoin=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "circle[fill=#FFB74D, stroke=#D97706]",
                "path[fill=#80DEEA, stroke=#1565C0]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "circle[fill=#FFCC80, stroke=#FFB74D]",
                "path[fill=#4DD0E1, stroke=#90CAF9]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void textFieldFamilyUsesExactReviewedInputCaretGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "textfield.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=11, rx=1.5, stroke-width=1, width=13, x=1.5, y=2.5]",
                "path[d=M3.8 7.2h6.4M3.8 10.2h8.4, stroke-linecap=round, stroke-width=1.2]",
                "path[d=M11.1 5.3v4.1, stroke-linecap=round, stroke-width=1.3]",
                "path[d=M2.5 12.5h11, stroke-linecap=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=22, rx=3, stroke-width=2, width=26, x=3, y=5]",
                "path[d=M7.6 14.4h12.8M7.6 20.4h16.8, stroke-linecap=round, stroke-width=2.4]",
                "path[d=M22.2 10.6v8.2, stroke-linecap=round, stroke-width=2.6]",
                "path[d=M5 25h22, stroke-linecap=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "path[fill=none, stroke=#1565C0]",
                "path[fill=none, stroke=#26C6DA]",
                "path[fill=none, stroke=#42A5F5]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "path[fill=none, stroke=#64B5F6]",
                "path[fill=none, stroke=#80DEEA]",
                "path[fill=none, stroke=#42A5F5]"), dark16.paint());
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

    @Test
    void expandedFamilyUsesExactReviewedTightFlexGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "expanded.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=11, rx=1.5, stroke-width=1, width=13, x=1.5, y=2.5]",
                "rect[height=9, rx=1, stroke-width=1, width=6, x=5, y=3.5]",
                "path[d=M4.5 8H2.75M3.75 6.75 2.5 8l1.25 1.25M11.5 8h1.75m-1-1.25L13.5 8l-1.25 1.25, stroke-linecap=round, stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=22, rx=3, stroke-width=2, width=26, x=3, y=5]",
                "rect[height=18, rx=2, stroke-width=2, width=12, x=10, y=7]",
                "path[d=M9 16H5.5m2-2.5L5 16l2.5 2.5M23 16h3.5m-2-2.5L27 16l-2.5 2.5, stroke-linecap=round, stroke-linejoin=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#80DEEA, stroke=#1565C0]",
                "path[fill=none, stroke=#42A5F5]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#4DD0E1, stroke=#90CAF9]",
                "path[fill=none, stroke=#64B5F6]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void listViewFamilyUsesExactReviewedScrollableListGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "listview.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-width=1, width=13, x=1.5, y=1.5]",
                "rect[height=2.2, rx=.6, width=8.5, x=3, y=3.2]",
                "rect[height=2.2, rx=.6, width=8.5, x=3, y=6.9]",
                "rect[height=2.2, rx=.6, width=8.5, x=3, y=10.6]",
                "rect[height=9.6, rx=.4, width=.8, x=12.4, y=3.2]",
                "rect[height=3.8, rx=.6, width=1.2, x=12.2, y=5]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-width=2, width=26, x=3, y=3]",
                "rect[height=4.4, rx=1.2, width=17, x=6, y=6.4]",
                "rect[height=4.4, rx=1.2, width=17, x=6, y=13.8]",
                "rect[height=4.4, rx=1.2, width=17, x=6, y=21.2]",
                "rect[height=19.2, rx=.8, width=1.6, x=24.8, y=6.4]",
                "rect[height=7.6, rx=1.2, width=2.4, x=24.4, y=10]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#42A5F5]",
                "rect[fill=#1565C0]",
                "rect[fill=#26C6DA]",
                "rect[fill=#B0BEC5]",
                "rect[fill=#40566D]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#64B5F6]",
                "rect[fill=#29B6F6]",
                "rect[fill=#80DEEA]",
                "rect[fill=#607D8B]",
                "rect[fill=#E0E8EF]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void wrapFamilyUsesExactReviewedRunGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "wrap.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-width=1, width=13, x=1.5, y=1.5]",
                "rect[height=3, rx=.6, width=3, x=3, y=3.5]",
                "rect[height=3, rx=.6, width=3, x=7, y=3.5]",
                "rect[height=3, rx=.6, width=2, x=11, y=3.5]",
                "rect[height=3, rx=.6, width=4, x=3, y=9.5]",
                "rect[height=3, rx=.6, width=3, x=8, y=9.5]",
                "path[d=M12.5 7.4v1.1h-1.1, stroke-linecap=round, "
                + "stroke-linejoin=round, stroke-width=1]"), light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-width=2, width=26, x=3, y=3]",
                "rect[height=6, rx=1.2, width=6, x=6, y=7]",
                "rect[height=6, rx=1.2, width=6, x=14, y=7]",
                "rect[height=6, rx=1.2, width=4, x=22, y=7]",
                "rect[height=6, rx=1.2, width=8, x=6, y=19]",
                "rect[height=6, rx=1.2, width=6, x=16, y=19]",
                "path[d=M25 14.8V17h-2.2, stroke-linecap=round, "
                + "stroke-linejoin=round, stroke-width=2]"), light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#42A5F5]",
                "rect[fill=#1565C0]",
                "rect[fill=#26C6DA]",
                "rect[fill=#1565C0]",
                "rect[fill=#26C6DA]",
                "path[fill=none, stroke=#40566D]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#64B5F6]",
                "rect[fill=#29B6F6]",
                "rect[fill=#80DEEA]",
                "rect[fill=#29B6F6]",
                "rect[fill=#80DEEA]",
                "path[fill=none, stroke=#C5D3DF]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void fittedBoxFamilyUsesExactReviewedScaleGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "fittedbox.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-width=1, width=13, x=1.5, y=1.5]",
                "rect[height=6, rx=1, stroke-width=1, width=8, x=4, y=5]",
                "path[d=M5.3 5.8L3.2 3.7m0 0h2m-2 0v2M10.7 10.2l2.1 2.1m0 0h-2m2 0v-2, "
                + "stroke-linecap=round, stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-width=2, width=26, x=3, y=3]",
                "rect[height=12, rx=2, stroke-width=2, width=16, x=8, y=10]",
                "path[d=M10.6 11.6L6.4 7.4m0 0h4m-4 0v4M21.4 20.4l4.2 4.2m0 0h-4m4 0v-4, "
                + "stroke-linecap=round, stroke-linejoin=round, stroke-width=2]"),
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
    void constrainedBoxFamilyUsesExactReviewedBoundsGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "constrainedbox.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-width=1, width=13, x=1.5, y=1.5]",
                "rect[height=8, rx=1, stroke-width=1, width=6, x=5, y=4]",
                "path[d=M3 8h2m-1-1 1 1-1 1M13 8h-2m1-1-1 1 1 1, "
                + "stroke-linecap=round, stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-width=2, width=26, x=3, y=3]",
                "rect[height=16, rx=2, stroke-width=2, width=12, x=10, y=8]",
                "path[d=M6 16h4m-2-2 2 2-2 2M26 16h-4m2-2-2 2 2 2, "
                + "stroke-linecap=round, stroke-linejoin=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[fill=none, stroke=#D97706]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[fill=none, stroke=#FFB74D]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void unconstrainedBoxFamilyUsesExactReviewedOverflowGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "unconstrainedbox.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=10, rx=1.5, stroke-dasharray=1.5 1.2, "
                + "stroke-width=1, width=10, x=3, y=3]",
                "rect[height=6, rx=1, stroke-width=1, width=13, x=1.5, y=5]",
                "path[d=M4.5 8h-2m1-1-1 1 1 1M11.5 8h2m-1-1 1 1-1 1, "
                + "stroke-linecap=round, stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=20, rx=3, stroke-dasharray=3 2.4, "
                + "stroke-width=2, width=20, x=6, y=6]",
                "rect[height=12, rx=2, stroke-width=2, width=26, x=3, y=10]",
                "path[d=M9 16H5m2-2-2 2 2 2M23 16h4m-2-2 2 2-2 2, "
                + "stroke-linecap=round, stroke-linejoin=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[fill=none, stroke=#D97706]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[fill=none, stroke=#FFB74D]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void limitedBoxFamilyUsesExactReviewedFallbackLimitGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "limitedbox.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=13, rx=1.5, stroke-dasharray=1.5 1.2, "
                + "stroke-width=1, width=13, x=1.5, y=1.5]",
                "rect[height=7, rx=1, stroke-width=1, width=7, x=4, y=4]",
                "path[d=M4 12.5h7m-7-.8v1.6m7-1.6v1.6M12.5 4v7m-.8-7h1.6m-1.6 7h1.6, "
                + "stroke-linecap=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=26, rx=3, stroke-dasharray=3 2.4, "
                + "stroke-width=2, width=26, x=3, y=3]",
                "rect[height=14, rx=2, stroke-width=2, width=14, x=8, y=8]",
                "path[d=M8 25h14m-14-1.6v3.2m14-3.2v3.2M25 8v14m-1.6-14h3.2m-3.2 14h3.2, "
                + "stroke-linecap=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[fill=none, stroke=#D97706]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[fill=none, stroke=#FFB74D]"), dark16.paint());
        assertEquals(light16.paint(), light32.paint());
        assertEquals(dark16.paint(), dark32.paint());
    }

    @Test
    void overflowBoxFamilyUsesExactReviewedConstraintOverrideGeometryAndThemePaint()
            throws Exception {
        String base = ICON_ROOT + "overflowbox.svg";
        SvgResource light16 = readSvg(base, 16);
        SvgResource dark16 = readSvg(variant(base, false, true), 16);
        SvgResource light32 = readSvg(variant(base, true, false), 32);
        SvgResource dark32 = readSvg(variant(base, true, true), 32);

        assertEquals(List.of(
                "rect[height=10, rx=1.5, stroke-dasharray=1.5 1.2, "
                + "stroke-width=1, width=8, x=4, y=3]",
                "rect[height=6, rx=1, stroke-width=1, width=13, x=1.5, y=5]",
                "path[d=M4 8H2m1-1-1 1 1 1M12 8h2m-1-1 1 1-1 1M8 5V3.5m-1 1 1-1 "
                + "1 1M8 11v1.5m-1-1 1 1 1-1, stroke-linecap=round, "
                + "stroke-linejoin=round, stroke-width=1]"),
                light16.geometry());
        assertEquals(List.of(
                "rect[height=20, rx=3, stroke-dasharray=3 2.4, "
                + "stroke-width=2, width=16, x=8, y=6]",
                "rect[height=12, rx=2, stroke-width=2, width=26, x=3, y=10]",
                "path[d=M8 16H4m2-2-2 2 2 2M24 16h4m-2-2 2 2-2 2M16 10V7m-2 2 2-2 "
                + "2 2M16 22v3m-2-2 2 2 2-2, stroke-linecap=round, "
                + "stroke-linejoin=round, stroke-width=2]"),
                light32.geometry());
        assertEquals(List.of(
                "rect[fill=#EAF5FC, stroke=#40566D]",
                "rect[fill=#D7F1FC, stroke=#42A5F5]",
                "path[fill=none, stroke=#D97706]"), light16.paint());
        assertEquals(List.of(
                "rect[fill=#253746, stroke=#C5D3DF]",
                "rect[fill=#294B5C, stroke=#64B5F6]",
                "path[fill=none, stroke=#FFB74D]"), dark16.paint());
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
        expected.put("flutter.material.TextField", ICON_ROOT + "textfield.svg");
        expected.put("flutter.widgets.Center", ICON_ROOT + "center.svg");
        expected.put("flutter.widgets.Column", ICON_ROOT + "column.svg");
        expected.put("flutter.widgets.Icon", ICON_ROOT + "icon.svg");
        expected.put("flutter.widgets.Image", ICON_ROOT + "image.svg");
        expected.put("flutter.widgets.Padding", ICON_ROOT + "padding.svg");
        expected.put("flutter.widgets.Row", ICON_ROOT + "row.svg");
        expected.put("flutter.widgets.Wrap", ICON_ROOT + "wrap.svg");
        expected.put("flutter.widgets.SizedBox", ICON_ROOT + "sizedbox.svg");
        expected.put("flutter.widgets.AspectRatio", ICON_ROOT + "aspectratio.svg");
        expected.put("flutter.widgets.Container", ICON_ROOT + "container.svg");
        expected.put("flutter.widgets.Opacity", ICON_ROOT + "opacity.svg");
        expected.put("flutter.widgets.Align", ICON_ROOT + "align.svg");
        expected.put("flutter.widgets.FractionallySizedBox",
                ICON_ROOT + "fractionallysizedbox.svg");
        expected.put("flutter.widgets.FittedBox", ICON_ROOT + "fittedbox.svg");
        expected.put("flutter.widgets.ConstrainedBox",
                ICON_ROOT + "constrainedbox.svg");
        expected.put("flutter.widgets.UnconstrainedBox",
                ICON_ROOT + "unconstrainedbox.svg");
        expected.put("flutter.widgets.LimitedBox", ICON_ROOT + "limitedbox.svg");
        expected.put("flutter.widgets.OverflowBox", ICON_ROOT + "overflowbox.svg");
        expected.put("flutter.widgets.Stack", ICON_ROOT + "stack.svg");
        expected.put("flutter.widgets.Expanded", ICON_ROOT + "expanded.svg");
        expected.put("flutter.widgets.ListView", ICON_ROOT + "listview.svg");
        expected.put("flutter.widgets.Text", ICON_ROOT + "text.svg");
        return Map.copyOf(expected);
    }

    private record SvgResource(
            List<String> geometry,
            List<String> topology,
            List<String> paint) {
    }
}
