package io.github.vgrytsenko2022.plugin.theme;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FlutterThemeRegistrationTest {
    @Test
    void registersFdthemeMimeDataObjectPreferredOpenAndDistinctIcon()
            throws IOException {
        String layer = resource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("name=\"ext.0\" stringvalue=\"fdtheme\""));
        assertTrue(layer.contains(
                "name=\"mimeType\" stringvalue=\"text/x-flutter-project-theme\""));
        assertTrue(layer.contains("FlutterThemeDataObject"));
        assertTrue(layer.contains(
                "io/github/vgrytsenko2022/plugin/ui/icons/flutterThemeFile16.svg"));
        String actions = folder(layer, "Loaders", "text",
                "x-flutter-project-theme", "Actions");
        assertTrue(actions.contains("org-openide-actions-OpenAction.shadow"));
        assertTrue(actions.contains("intvalue=\"100\" name=\"position\"")
                        || actions.contains("name=\"position\" intvalue=\"100\""));
    }

    @Test
    void registersEditActionInFlutterAndProjectMenus() throws IOException {
        String layer = resource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("EditFlutterThemesAction"));
        assertTrue(folder(layer, "Menu", "Flutter")
                .contains("EditFlutterThemesAction.shadow"));
        assertTrue(folder(layer, "Projects", "Actions")
                .contains("EditFlutterThemesAction.shadow"));
    }

    @Test
    void registersDockedThemeEditorBesidePaletteWithoutOpeningAtStartup()
            throws IOException {
        String layer = resource("META-INF/generated-layer.xml");

        String components = folder(layer, "Windows2", "Components");
        assertTrue(components.contains("FlutterThemesTopComponent.settings"));
        assertTrue(components.contains(
                "io.github.vgrytsenko2022.plugin.theme.FlutterThemesTopComponent"));

        String paletteMode = folder(layer, "Windows2", "Modes", "commonpalette");
        assertTrue(paletteMode.contains("FlutterThemesTopComponent.wstcref"));
        assertTrue(paletteMode.contains("<tc-id id=\"FlutterThemesTopComponent\"/>"));
        assertTrue(paletteMode.contains("<state opened=\"false\"/>"));
        assertTrue(paletteMode.contains("intvalue=\"120\" name=\"position\"")
                        || paletteMode.contains(
                                "name=\"position\" intvalue=\"120\""));

        assertTrue(folder(layer, "Menu", "Window", "Tools")
                .contains("FlutterThemesTopComponent.shadow"));
    }

    @Test
    void packagesLightAndDarkThemeFileIcons() throws IOException {
        for (String resource : new String[]{
            "io/github/vgrytsenko2022/plugin/ui/icons/flutterThemeFile16.svg",
            "io/github/vgrytsenko2022/plugin/ui/icons/flutterThemeFile16_dark.svg"
        }) {
            String svg = resource(resource);
            assertTrue(svg.contains("<svg"));
            assertTrue(svg.contains("width=\"16\""));
            assertTrue(svg.contains("height=\"16\""));
        }
    }

    private static String resource(String name) throws IOException {
        try (InputStream input = FlutterThemeRegistrationTest.class
                .getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, "missing resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String folder(String layer, String... path) {
        int cursor = 0;
        for (String segment : path) {
            String marker = "<folder name=\"" + segment + "\">";
            cursor = layer.indexOf(marker, cursor);
            assertTrue(cursor >= 0, "missing layer folder: " + String.join("/", path));
            cursor += marker.length();
        }
        int start = cursor;
        int depth = 1;
        while (depth > 0) {
            int open = layer.indexOf("<folder ", cursor);
            int close = layer.indexOf("</folder>", cursor);
            assertTrue(close >= 0, "unterminated layer folder");
            if (open >= 0 && open < close) {
                depth++;
                cursor = open + 8;
            } else {
                depth--;
                cursor = close + "</folder>".length();
            }
        }
        return layer.substring(start, cursor);
    }
}
