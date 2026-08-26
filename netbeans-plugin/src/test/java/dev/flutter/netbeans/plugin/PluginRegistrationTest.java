package dev.flutter.netbeans.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.plugin.dart.DartTokenId;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PluginRegistrationTest {
    @Test
    void registersFlutterOptionsCategory() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("OptionsDialog"));
        assertTrue(layer.contains("Flutter.instance"));
        assertTrue(layer.contains("FlutterOptionsPanelController"));
        assertTrue(layer.contains("dev/flutter/netbeans/plugin/options/flutter32.svg"));
    }

    @Test
    void registersFirstStartSdkDiscovery() throws IOException {
        String services = readResource("META-INF/namedservices/Modules/Start/java.lang.Runnable");

        assertTrue(services.contains("FlutterSdkAutoDiscovery"));
    }

    @Test
    void registersFlutterProjectFactory() throws IOException {
        String services = readResource("META-INF/services/org.netbeans.spi.project.ProjectFactory");

        assertTrue(services.contains("FlutterProjectFactory"));
    }

    @Test
    void registersFlutterApplicationWizard() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("<folder name=\"Project\">"));
        assertTrue(layer.contains("<folder name=\"Flutter\">"));
        assertTrue(layer.contains("FlutterProjectWizardIterator"));
        assertTrue(layer.contains("FlutterApplicationDescription.html"));
    }

    @Test
    void registersDartClassNewFileTemplate() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("<folder name=\"Templates\">"));
        assertTrue(layer.contains("<folder name=\"Dart\">"));
        assertTrue(layer.contains("DartClassWizardIterator"));
        assertTrue(layer.contains("DartClassDescription.html"));
        assertTrue(layer.contains("name=\"templateCategory\" stringvalue=\"dart\""));
    }

    @Test
    void registersFlutterRunMenuAndDartDebugger() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("<folder name=\"Flutter\">"));
        assertTrue(layer.contains("SelectFlutterTargetAction"));
        assertTrue(layer.contains("LaunchFlutterEmulatorAction"));
        assertTrue(layer.contains("RunFlutterAction"));
        assertTrue(layer.contains("DebugFlutterAction"));
        assertTrue(layer.contains("HotReloadFlutterAction"));
        assertTrue(layer.contains("HotRestartFlutterAction"));
        assertTrue(layer.contains("OpenDevToolsAction"));
        assertTrue(layer.contains("StopDevToolsAction"));
        assertTrue(layer.contains("StopFlutterAction"));
        assertTrue(layer.contains("FlutterPubGetAction"));
        assertTrue(layer.contains("FlutterAnalyzeAction"));
        assertTrue(layer.contains("FlutterTestAction"));
        assertTrue(layer.contains("FlutterTestFileAction"));
        assertTrue(layer.contains("FlutterTestAtCaretAction"));
        assertTrue(layer.contains("text/x-dart"));
        assertTrue(layer.contains("RegisterDAPBreakpoints"));
    }

    @Test
    void registersHotReloadAndRestartInTheBuildToolbar() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");
        String toolbar = folderRegistration(layer, "Toolbars");
        String buildToolbar = folderRegistration(toolbar, "Build");

        String hotReload = fileRegistration(
                buildToolbar,
                "dev-flutter-netbeans-plugin-run-HotReloadFlutterAction.shadow");
        assertTrue(hotReload.contains(
                "Actions/Flutter/dev-flutter-netbeans-plugin-run-HotReloadFlutterAction.instance"));
        assertTrue(hotReload.contains("intvalue=\"360\" name=\"position\""));
        String separator = fileRegistration(
                buildToolbar,
                "dev-flutter-netbeans-plugin-run-HotReloadFlutterAction-separatorBefore.instance");
        assertTrue(separator.contains("intvalue=\"355\" name=\"position\""));

        String hotRestart = fileRegistration(
                buildToolbar,
                "dev-flutter-netbeans-plugin-run-HotRestartFlutterAction.shadow");
        assertTrue(hotRestart.contains(
                "Actions/Flutter/dev-flutter-netbeans-plugin-run-HotRestartFlutterAction.instance"));
        assertTrue(hotRestart.contains("intvalue=\"370\" name=\"position\""));

        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotReload.svg", "16", "16");
        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotReload_dark.svg", "16", "16");
        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotReload24.svg", "24", "24");
        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotReload24_dark.svg", "24", "24");
        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotRestart.svg", "16", "16");
        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotRestart_dark.svg", "16", "16");
        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotRestart24.svg", "24", "24");
        assertSvgIcon("dev/flutter/netbeans/plugin/run/hotRestart24_dark.svg", "24", "24");
    }

    @Test
    void registersPubspecCompletionAndDiagnostics() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("<folder name=\"x-yaml\">"));
        assertTrue(layer.contains("PubspecCompletionProvider"));
        assertTrue(layer.contains("PubspecErrorProvider"));
    }

    @Test
    void registersDartLanguageServerProvider() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("DartLanguageServerProvider"));
        assertTrue(layer.contains(
                "org.netbeans.modules.lsp.client.spi.LanguageServerProvider"));
        assertTrue(layer.contains("text/x-dart"));
    }

    @Test
    void registersDartMimeLexerAndEditorKit() throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");

        assertTrue(layer.contains("DartMimeRegistration-Extension.xml"));
        assertTrue(layer.contains("name=\"ext.0\" stringvalue=\"dart\""));
        assertTrue(layer.contains("name=\"mimeType\" stringvalue=\"text/x-dart\""));
        assertTrue(layer.contains("DartTokenId-language.instance"));
        assertTrue(layer.contains("DartTokenId.language"));
        assertTrue(layer.contains(
                "name=\"instanceOf\" stringvalue=\"org.netbeans.api.lexer.Language\""));
        assertTrue(layer.contains("DartEditorKit.instance"));
        assertTrue(layer.contains(
                "name=\"instanceOf\" stringvalue=\"javax.swing.text.EditorKit\""));
    }

    @Test
    void registersDartDesignerUndoableEditWrapperAtTheNativeDocumentBoundary()
            throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");
        String registration = fileRegistration(
                layer,
                "dev-flutter-netbeans-plugin-designer-DesignerUndoableEditWrapper.instance");

        assertTrue(registration.contains(
                "name=\"instanceOf\" stringvalue=\"org.netbeans.spi.editor.document.UndoableEditWrapper\""));
    }

    @Test
    void registersDartFontColorsAndPreview() throws IOException {
        String layer = readResource("dev/flutter/netbeans/plugin/layer.xml");
        String colors = readResource(
                "dev/flutter/netbeans/plugin/dart/DartFontColors.xml");
        String preview = readResource(
                "dev/flutter/netbeans/plugin/dart/DartExample.dart");

        assertTrue(layer.contains("<folder name=\"x-dart\">"));
        assertTrue(layer.contains("<folder name=\"FontsColors\">"));
        assertTrue(layer.contains("url=\"dart/DartFontColors.xml\""));
        assertTrue(layer.contains("<folder name=\"PreviewExamples\">"));
        assertTrue(layer.contains("url=\"dart/DartExample.dart\""));
        assertTrue(colors.contains("name=\"string-interpolation\" default=\"identifier\""));
        assertTrue(colors.contains("name=\"doc-comment\" default=\"comment\""));
        assertTrue(colors.contains("name=\"error\" default=\"error\""));
        for (DartTokenId tokenId : DartTokenId.values()) {
            assertTrue(
                    colors.contains("name=\"" + tokenId.primaryCategory() + "\" default=\""),
                    () -> "missing theme default for " + tokenId.primaryCategory());
        }
        assertFalse(colors.contains("foreColor="));
        assertFalse(colors.contains("bgColor="));
        assertTrue(preview.contains("class CounterCard"));
        assertTrue(preview.contains("'Count: $count'"));
    }

    @Test
    void registersDartIndentationTypingHooksAndDefaults() throws IOException {
        String generatedLayer = readResource("META-INF/generated-layer.xml");
        String moduleLayer = readResource("dev/flutter/netbeans/plugin/layer.xml");
        String preferences = readResource(
                "dev/flutter/netbeans/plugin/dart/DartPreferences.xml");

        assertTrue(generatedLayer.contains("DartIndentTask$Factory"));
        assertTrue(generatedLayer.contains("DartTypingHooks$BreakFactory"));
        assertTrue(generatedLayer.contains("DartTypingHooks$TextFactory"));
        assertTrue(moduleLayer.contains("url=\"dart/DartPreferences.xml\""));
        assertTrue(preferences.contains("name=\"indent-shift-width\""));
        assertTrue(preferences.contains("<![CDATA[2]]>"));
    }

    @Test
    void mimeRegistrationsDoNotCreatePartialOrDuplicateOrderingRules()
            throws IOException {
        String layer = readResource("META-INF/generated-layer.xml");
        for (String registration : new String[]{
            "dev-flutter-netbeans-plugin-dart-DartEditorKit.instance",
            "dev-flutter-netbeans-plugin-dart-DartIndentTask$Factory.instance",
            "dev-flutter-netbeans-plugin-dart-DartLanguageServerProvider.instance",
            "dev-flutter-netbeans-plugin-dart-DartTokenId-language.instance",
            "dev-flutter-netbeans-plugin-dart-DartTypingHooks$BreakFactory.instance",
            "dev-flutter-netbeans-plugin-dart-DartTypingHooks$TextFactory.instance",
            "dev-flutter-netbeans-plugin-designer-DesignerUndoableEditWrapper.instance",
            "dev-flutter-netbeans-plugin-designer-guard-DartGuardedSectionsFactory.instance",
            "dev-flutter-netbeans-plugin-pubspec-PubspecErrorProvider.instance"
        }) {
            assertFalse(
                    fileRegistration(layer, registration).contains("name=\"position\""),
                    () -> registration + " must remain unpositioned alongside other MIME services");
        }
    }

    private static String fileRegistration(String layer, String fileName) {
        String marker = "<file name=\"" + fileName + "\">";
        int start = layer.indexOf(marker);
        assertTrue(start >= 0, "missing generated registration: " + fileName);
        int end = layer.indexOf("</file>", start);
        assertTrue(end >= 0, "unterminated generated registration: " + fileName);
        return layer.substring(start, end);
    }

    private static String folderRegistration(String layer, String folderName) {
        String marker = "<folder name=\"" + folderName + "\">";
        int start = layer.indexOf(marker);
        assertTrue(start >= 0, "missing generated folder: " + folderName);
        int depth = 1;
        int cursor = start + marker.length();
        while (depth > 0) {
            int nextOpen = layer.indexOf("<folder ", cursor);
            int nextClose = layer.indexOf("</folder>", cursor);
            assertTrue(nextClose >= 0, "unterminated generated folder: " + folderName);
            if (nextOpen >= 0 && nextOpen < nextClose) {
                depth++;
                cursor = nextOpen + 8;
            } else {
                depth--;
                cursor = nextClose + "</folder>".length();
            }
        }
        return layer.substring(start, cursor);
    }

    private static void assertSvgIcon(String resource, String width, String height)
            throws IOException {
        String svg = readResource(resource);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("width=\"" + width + "\""));
        assertTrue(svg.contains("height=\"" + height + "\""));
    }

    private static String readResource(String name) throws IOException {
        try (InputStream input = PluginRegistrationTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, "missing generated registration: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
