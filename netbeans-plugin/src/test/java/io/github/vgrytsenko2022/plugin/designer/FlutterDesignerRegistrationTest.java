package io.github.vgrytsenko2022.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Generated-layer contract for the dedicated Flutter Designer file type. */
class FlutterDesignerRegistrationTest {
    private static final String DESIGN_REGISTRATION =
            "io-github-vgrytsenko2022-plugin-designer-FlutterDesignerMultiViewDesign.instance";
    private static final String SOURCE_REGISTRATION =
            "io-github-vgrytsenko2022-plugin-designer-FlutterDesignerMultiViewSource.instance";

    @Test
    void registersDedicatedFdMimeAndPairAwareDataLoader() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("<folder name=\"MIMEResolver\">"),
                "missing MIME resolver registration folder");
        assertTrue(layer.contains("name=\"ext.0\" stringvalue=\"fd\""),
                "the designer MIME resolver must own only the .fd extension");
        assertTrue(layer.contains(
                "name=\"mimeType\" stringvalue=\"text/x-flutter-designer\""),
                "the .fd extension must use its dedicated MIME type");
        assertPosition(
                fileRegistration(
                        layer,
                        "io-github-vgrytsenko2022-plugin-designer-"
                        + "FlutterDesignerDataLoader-Extension.xml"),
                352,
                "Flutter Designer MIME resolver");
        assertTrue(layer.contains("FlutterDesignerDataLoader"),
                "missing pair-aware Flutter Designer data loader");
        assertTrue(layer.contains("<folder name=\"Loaders\">"));
        assertTrue(layer.contains("<folder name=\"x-flutter-designer\">"));
        String dartFactories = folderRegistration(
                layer, "Loaders", "text", "x-dart", "Factories");
        assertTrue(dartFactories.contains("FlutterDesignerDataLoader.instance"),
                "the pair-aware loader must be eligible when NetBeans sees Dart first");
        String pairedDartRegistration = fileRegistration(
                dartFactories,
                "io-github-vgrytsenko2022-plugin-designer-FlutterDesignerDataLoader.instance");
        assertPosition(pairedDartRegistration, 100, "pair-aware Dart loader");
        assertTrue(pairedDartRegistration.contains(
                "name=\"iconBase\" stringvalue=\"io/github/vgrytsenko2022/plugin/ui/icons/dartFile16.svg\""),
                "the pair-aware Dart loader must advertise the Dart file-type icon");

        String ordinaryDartRegistration = fileRegistration(
                dartFactories,
                "io-github-vgrytsenko2022-plugin-dart-DartDataObject.instance");
        assertPosition(ordinaryDartRegistration, 200, "ordinary Dart loader");
        assertTrue(ordinaryDartRegistration.contains(
                "name=\"dataObjectClass\" stringvalue=\"io.github.vgrytsenko2022.plugin.dart.DartDataObject\""),
                "ordinary Dart files must use the dedicated DartDataObject");
        assertTrue(ordinaryDartRegistration.contains(
                "name=\"iconBase\" stringvalue=\"io/github/vgrytsenko2022/plugin/ui/icons/dartFile16.svg\""),
                "ordinary Dart files must advertise the Dart file-type icon");

        String modelFactories = folderRegistration(
                layer, "Loaders", "text", "x-flutter-designer", "Factories");
        assertTrue(modelFactories.contains("FlutterDesignerModelDataObject"),
                "the visible .fd model must have its own delegating DataObject");
        assertFalse(modelFactories.contains("FlutterDesignerDataLoader.instance"),
                "claiming .fd as a Dart-side secondary hides it from the Files view");
        String modelRegistration = fileRegistration(
                modelFactories,
                "io-github-vgrytsenko2022-plugin-designer-"
                + "FlutterDesignerModelDataObject.instance");
        assertTrue(modelRegistration.contains(
                "name=\"iconBase\" stringvalue=\"io/github/vgrytsenko2022/plugin/ui/icons/flutterDesignerFile16.svg\""),
                "physical .fd files must advertise the Flutter Designer file-type icon");
    }

    @Test
    void registersStandardDartFileActionsWithOpenAsThePreferredAction()
            throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");
        String actions = folderRegistration(
                layer, "Loaders", "text", "x-dart", "Actions");

        assertActionReference(
                actions, "OpenAction", "System", 100);
        assertSeparatorAfter(actions, "OpenAction", 200);
        assertActionReference(
                actions, "CutAction", "Edit", 300);
        assertActionReference(
                actions, "CopyAction", "Edit", 400);
        assertActionReference(
                actions, "PasteAction", "Edit", 500);
        assertSeparatorAfter(actions, "PasteAction", 600);
        assertActionReference(
                actions, "DeleteAction", "Edit", 700);
        assertActionReference(
                actions, "RenameAction", "System", 800);
        assertSeparatorAfter(actions, "RenameAction", 900);
        assertActionReference(
                actions, "SaveAsTemplateAction", "System", 1000);
        assertSeparatorAfter(actions, "SaveAsTemplateAction", 1100);
        assertActionReference(
                actions, "FileSystemAction", "System", 1200);
        assertSeparatorAfter(actions, "FileSystemAction", 1300);
        assertActionReference(
                actions, "ToolsAction", "System", 1400);
        assertActionReference(
                actions, "PropertiesAction", "System", 1500);

        assertEquals(10, occurrences(actions, ".shadow"),
                "the Dart popup must contain exactly the standard file-action set");
        assertEquals(5, occurrences(actions, "separatorAfter.instance"),
                "the Dart popup has the wrong separator set");
    }

    @Test
    void registersDesignBeforeSourceOnlyForTheDesignerMime() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");
        String design = fileRegistration(layer, DESIGN_REGISTRATION);
        String source = fileRegistration(layer, SOURCE_REGISTRATION);

        assertPosition(design, 100, DESIGN_REGISTRATION);
        assertPosition(source, 200, SOURCE_REGISTRATION);
        assertTrue(design.contains("flutter.designer.design"),
                "Design has the wrong preferredID");
        assertTrue(source.contains("flutter.designer.source"),
                "Source has the wrong preferredID");
        assertFalse(design.contains("name=\"sourceview\""),
                "Design must not be marked as the source perspective");
        assertTrue(source.contains("boolvalue=\"true\" name=\"sourceview\""),
                "Source must be marked as the source perspective");
        assertEquals(1, occurrences(layer, DESIGN_REGISTRATION),
                "Design must be registered for one MIME only");
        assertEquals(1, occurrences(layer, SOURCE_REGISTRATION),
                "Source must be registered for one MIME only");

        int designerMime = layer.indexOf("<folder name=\"x-flutter-designer\">");
        int designerMultiView = layer.indexOf("<folder name=\"MultiView\">", designerMime);
        assertTrue(designerMime >= 0 && designerMultiView > designerMime,
                "missing designer MIME MultiView folder");
        assertTrue(layer.indexOf(DESIGN_REGISTRATION, designerMultiView) >= 0);
        assertTrue(layer.indexOf(SOURCE_REGISTRATION, designerMultiView) >= 0);
    }

    @Test
    void doesNotAttachDesignerViewsToEveryDartFile() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        String dartEditors = folderRegistration(layer, "Editors", "text", "x-dart");
        assertFalse(dartEditors.contains("FlutterDesignerMultiViewDesign"),
                "ordinary Dart files must not receive the Design view");
        assertFalse(dartEditors.contains("FlutterDesignerMultiViewSource"),
                "ordinary Dart files must not receive the designer Source view");
    }

    @Test
    void registersGuardPersistenceForTheDartEditorMime() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");
        String dartEditors = folderRegistration(layer, "Editors", "text", "x-dart");

        assertTrue(dartEditors.contains("DartGuardedSectionsFactory"),
                "the Dart editor MIME has no guarded-section persistence factory");
        assertTrue(dartEditors.contains(
                "org.netbeans.spi.editor.guards.GuardedSectionsFactory"),
                "the guarded-section registration exposes the wrong service");
    }

    private static void assertPosition(String registration, int position, String name) {
        String firstOrder = "name=\"position\" intvalue=\"" + position + "\"";
        String secondOrder = "intvalue=\"" + position + "\" name=\"position\"";
        assertTrue(registration.contains(firstOrder) || registration.contains(secondOrder),
                () -> name + " must have position " + position);
    }

    private static void assertActionReference(
            String actions,
            String actionName,
            String category,
            int position) {
        String registration = fileRegistration(
                actions, "org-openide-actions-" + actionName + ".shadow");
        assertTrue(registration.contains(
                "stringvalue=\"Actions/" + category
                + "/org-openide-actions-" + actionName + ".instance\""),
                () -> actionName + " points to the wrong global action");
        assertPosition(registration, position, actionName);
    }

    private static void assertSeparatorAfter(
            String actions,
            String actionName,
            int position) {
        String separatorName = "org-openide-actions-" + actionName
                + "-separatorAfter.instance";
        String registration = fileRegistration(actions, separatorName);
        assertTrue(registration.contains(
                "newvalue=\"javax.swing.JSeparator\""),
                () -> separatorName + " is not a Swing separator");
        assertPosition(registration, position, separatorName);
    }

    private static String fileRegistration(String layer, String fileName) {
        String marker = "<file name=\"" + fileName + "\">";
        int start = layer.indexOf(marker);
        assertTrue(start >= 0, "missing generated registration: " + fileName);
        int end = layer.indexOf("</file>", start);
        assertTrue(end >= 0, "unterminated generated registration: " + fileName);
        return layer.substring(start, end + "</file>".length());
    }

    private static String folderRegistration(String layer, String... path) {
        int cursor = 0;
        for (String segment : path) {
            String marker = "<folder name=\"" + segment + "\">";
            cursor = layer.indexOf(marker, cursor);
            assertTrue(cursor >= 0, "missing generated layer folder: " + String.join("/", path));
            cursor += marker.length();
        }
        int end = matchingFolderEnd(layer, cursor);
        return layer.substring(cursor, end);
    }

    private static int matchingFolderEnd(String xml, int contentStart) {
        int depth = 1;
        int cursor = contentStart;
        while (depth > 0) {
            int open = xml.indexOf("<folder ", cursor);
            int close = xml.indexOf("</folder>", cursor);
            assertTrue(close >= 0, "unterminated generated layer folder");
            if (open >= 0 && open < close) {
                depth++;
                cursor = open + 8;
            } else {
                depth--;
                cursor = close + "</folder>".length();
            }
        }
        return cursor;
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int cursor = 0;
        while ((cursor = value.indexOf(needle, cursor)) >= 0) {
            count++;
            cursor += needle.length();
        }
        return count;
    }

    private static String readResource(String name) throws IOException {
        try (InputStream input = FlutterDesignerRegistrationTest.class
                .getClassLoader()
                .getResourceAsStream(name)) {
            assertNotNull(input, "missing generated registration: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
