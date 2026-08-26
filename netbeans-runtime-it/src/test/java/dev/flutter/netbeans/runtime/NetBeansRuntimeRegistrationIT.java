package dev.flutter.netbeans.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInfo;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class NetBeansRuntimeRegistrationIT {

    private static final String FLUTTER_MODULE = "dev.flutter.netbeans.netbeans.plugin";
    private static final String FLUTTER_MODULE_CONFIG =
            "extra/config/Modules/dev-flutter-netbeans-netbeans-plugin.xml";
    private static final String FLUTTER_MODULE_JAR =
            "extra/modules/dev-flutter-netbeans-netbeans-plugin.jar";

    private static final List<ActionRegistration> FLUTTER_MENU_ACTIONS = List.of(
            action(
                    "dev-flutter-netbeans-plugin-CheckFlutterSdkAction.shadow",
                    "Actions/Tools/dev-flutter-netbeans-plugin-CheckFlutterSdkAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-OpenFlutterOptionsAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-OpenFlutterOptionsAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-DebugFlutterAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-DebugFlutterAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-HotReloadFlutterAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-HotReloadFlutterAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-HotRestartFlutterAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-HotRestartFlutterAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-LaunchFlutterEmulatorAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-LaunchFlutterEmulatorAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-OpenDevToolsAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-OpenDevToolsAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-RunFlutterAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-RunFlutterAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-SelectFlutterTargetAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-SelectFlutterTargetAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-StopDevToolsAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-StopDevToolsAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-run-StopFlutterAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-StopFlutterAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-tooling-FlutterAnalyzeAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-tooling-FlutterAnalyzeAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-tooling-FlutterPubGetAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-tooling-FlutterPubGetAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-tooling-FlutterTestAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-tooling-FlutterTestAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-tooling-FlutterTestAtCaretAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-tooling-FlutterTestAtCaretAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-tooling-FlutterTestFileAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-tooling-FlutterTestFileAction.instance"),
            action(
                    "dev-flutter-netbeans-plugin-device-FlutterDeviceManagerTopComponent.shadow",
                    "Actions/Window/dev-flutter-netbeans-plugin-device-FlutterDeviceManagerTopComponent.instance"));

    private static final List<ToolbarActionRegistration> FLUTTER_TOOLBAR_ACTIONS = List.of(
            toolbarAction(
                    "dev-flutter-netbeans-plugin-run-HotReloadFlutterAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-HotReloadFlutterAction.instance",
                    "Hot Reload",
                    "dev/flutter/netbeans/plugin/run/hotReload.svg",
                    360),
            toolbarAction(
                    "dev-flutter-netbeans-plugin-run-HotRestartFlutterAction.shadow",
                    "Actions/Flutter/dev-flutter-netbeans-plugin-run-HotRestartFlutterAction.instance",
                    "Hot Restart",
                    "dev/flutter/netbeans/plugin/run/hotRestart.svg",
                    370));

    private static final List<LayerRegistration> DART_EDITOR_REGISTRATIONS = List.of(
            registration(
                    "Editors/text/x-dart/dev-flutter-netbeans-plugin-dart-DartEditorKit.instance",
                    "javax.swing.text.EditorKit"),
            registration(
                    "Editors/text/x-dart/dev-flutter-netbeans-plugin-dart-DartTokenId-language.instance",
                    "org.netbeans.api.lexer.Language"),
            registration(
                    "Editors/text/x-dart/dev-flutter-netbeans-plugin-dart-DartLanguageServerProvider.instance",
                    "org.netbeans.modules.lsp.client.spi.LanguageServerProvider"),
            registration(
                    "Editors/text/x-dart/dev-flutter-netbeans-plugin-dart-DartIndentTask$Factory.instance",
                    "org.netbeans.modules.editor.indent.spi.IndentTask$Factory"),
            registration(
                    "Editors/text/x-dart/dev-flutter-netbeans-plugin-dart-DartTypingHooks$BreakFactory.instance",
                    "org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor$Factory"),
            registration(
                    "Editors/text/x-dart/dev-flutter-netbeans-plugin-dart-DartTypingHooks$TextFactory.instance",
                    "org.netbeans.spi.editor.typinghooks.TypedTextInterceptor$Factory"),
            registration(
                    "Editors/text/x-dart/breakpoints.instance",
                    "org.netbeans.modules.lsp.client.debugger.api.RegisterDAPBreakpoints"));

    @Test
    void flutterRegistrationsAreAvailableInTheRuntimeContainer() throws Exception {
        assertPackagedModuleEnabledByDefault();

        junit.framework.Test suite = NbModuleSuite.createConfiguration(RuntimeRegistrationCase.class)
                .clusters("ide|harness|extra")
                .enableModules("extra", Pattern.quote(FLUTTER_MODULE))
                .enableClasspathModules(false)
                .honorAutoloadEager(true)
                .failOnMessage(Level.SEVERE)
                .failOnException(Level.SEVERE)
                .gui(false)
                .suite();

        TestResult result = new TestResult();
        suite.run(result);

        if (!result.wasSuccessful()) {
            Assertions.fail(runtimeFailures(result));
        }
        Assertions.assertEquals(
                1,
                result.runCount(),
                "The runtime registration test did not run exactly once");
    }

    private static void assertPackagedModuleEnabledByDefault() throws Exception {
        String runtimeProperty = System.getProperty("all.clusters");
        Assertions.assertNotNull(runtimeProperty, "Failsafe did not provide the assembled NetBeans runtime path");
        Path runtime = Path.of(runtimeProperty).toAbsolutePath().normalize();
        Path config = runtime.resolve(FLUTTER_MODULE_CONFIG);
        Path moduleJar = runtime.resolve(FLUTTER_MODULE_JAR);

        Assertions.assertTrue(
                Files.isRegularFile(config),
                () -> "Packaged Flutter module config is missing: " + config);
        Assertions.assertTrue(
                Files.isRegularFile(moduleJar),
                () -> "Packaged Flutter module JAR is missing: " + moduleJar);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Element module = factory.newDocumentBuilder().parse(config.toFile()).getDocumentElement();

        Assertions.assertEquals("module", module.getTagName(), "Unexpected module config root element");
        Assertions.assertEquals(
                FLUTTER_MODULE,
                module.getAttribute("name"),
                "Packaged module config has the wrong code name base");
        Assertions.assertEquals(
                "false", moduleParameter(module, "autoload"), "Flutter module must not be an autoload");
        Assertions.assertEquals(
                "false", moduleParameter(module, "eager"), "Flutter module must not be eager");
        Assertions.assertEquals(
                "true", moduleParameter(module, "enabled"), "Flutter module must be enabled by default");
        Assertions.assertEquals(
                "modules/dev-flutter-netbeans-netbeans-plugin.jar",
                moduleParameter(module, "jar"),
                "Packaged module config points at the wrong JAR");
    }

    private static String moduleParameter(Element module, String name) {
        NodeList parameters = module.getElementsByTagName("param");
        for (int index = 0; index < parameters.getLength(); index++) {
            Element parameter = (Element) parameters.item(index);
            if (name.equals(parameter.getAttribute("name"))) {
                return parameter.getTextContent().trim();
            }
        }
        Assertions.fail("Packaged Flutter module config is missing parameter: " + name);
        return null;
    }

    private static String runtimeFailures(TestResult result) {
        StringBuilder message = new StringBuilder("NetBeans runtime registration gate failed");
        appendFailures(message, "error", result.errors());
        appendFailures(message, "failure", result.failures());
        return message.toString();
    }

    private static void appendFailures(
            StringBuilder message,
            String kind,
            Enumeration<TestFailure> failures) {
        while (failures.hasMoreElements()) {
            TestFailure failure = failures.nextElement();
            message.append(System.lineSeparator())
                    .append(kind)
                    .append(": ")
                    .append(failure.trace());
        }
    }

    private static ActionRegistration action(String menuFile, String originalFile) {
        return new ActionRegistration(menuFile, originalFile);
    }

    private static LayerRegistration registration(String path, String instanceOf) {
        return new LayerRegistration(path, instanceOf);
    }

    private static ToolbarActionRegistration toolbarAction(
            String toolbarFile,
            String originalFile,
            String displayName,
            String iconBase,
            int position) {
        return new ToolbarActionRegistration(
                toolbarFile, originalFile, displayName, iconBase, position);
    }

    public static final class RuntimeRegistrationCase extends NbTestCase {

        public RuntimeRegistrationCase(String name) {
            super(name);
        }

        public void testFlutterRegistrations() throws Exception {
            assertFlutterModuleActive();
            assertDartMimeAndEditorRegistrations();
            assertFlutterMenuActions();
            assertFlutterToolbarActions();
        }

        private void assertFlutterModuleActive() {
            Collection<? extends ModuleInfo> modules = Lookup.getDefault().lookupAll(ModuleInfo.class);
            ModuleInfo flutterModule = modules.stream()
                    .filter(module -> FLUTTER_MODULE.equals(module.getCodeNameBase()))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Flutter module is absent; available modules: "
                    + modules.stream()
                            .map(ModuleInfo::getCodeNameBase)
                            .sorted()
                            .collect(java.util.stream.Collectors.joining(", ")), flutterModule);

            assertTrue("Flutter module must be active in the runtime container", flutterModule.isEnabled());
        }

        private void assertDartMimeAndEditorRegistrations() throws Exception {
            assertConfigFile("Editors/text/x-dart");

            FileObject mimeResolver = assertConfigFile(
                    "Services/MIMEResolver/dev-flutter-netbeans-plugin-dart-DartMimeRegistration-Extension.xml");
            assertEquals("Dart MIME resolver must declare text/x-dart",
                    "text/x-dart", mimeResolver.getAttribute("mimeType"));
            assertEquals("Dart MIME resolver must own the .dart extension",
                    "dart", mimeResolver.getAttribute("ext.0"));

            FileSystem memory = FileUtil.createMemoryFileSystem();
            FileObject dartFile = memory.getRoot().createData("runtime_gate.dart");
            assertEquals("The .dart extension must resolve to the Dart MIME type",
                    "text/x-dart", FileUtil.getMIMEType(dartFile));

            for (LayerRegistration registration : DART_EDITOR_REGISTRATIONS) {
                FileObject file = assertConfigFile(registration.path());
                assertEquals(
                        "Unexpected instanceOf declaration for " + registration.path(),
                        registration.instanceOf(),
                        file.getAttribute("instanceOf"));
            }

            FileObject breakpointAction = assertConfigFile(
                    "Editors/text/x-dart/GlyphGutterActions/"
                            + "org-netbeans-modules-debugger-ui-actions-ToggleBreakpointAction.shadow");
            assertEquals(
                    "Dart glyph gutter must delegate to the NetBeans breakpoint action",
                    "Actions/Debug/org-netbeans-modules-debugger-ui-actions-ToggleBreakpointAction.instance",
                    breakpointAction.getAttribute("originalFile"));
            assertConfigFile((String) breakpointAction.getAttribute("originalFile"));
        }

        private void assertFlutterMenuActions() {
            FileObject menu = assertConfigFile("Menu/Flutter");
            Set<String> expectedShadows = FLUTTER_MENU_ACTIONS.stream()
                    .map(ActionRegistration::menuFile)
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            Set<String> actualShadows = Arrays.stream(menu.getChildren())
                    .filter(FileObject::isData)
                    .map(FileObject::getNameExt)
                    .filter(name -> name.endsWith(".shadow"))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            assertEquals("Unexpected Flutter menu action shadow set", expectedShadows, actualShadows);

            for (ActionRegistration registration : FLUTTER_MENU_ACTIONS) {
                String shadowPath = "Menu/Flutter/" + registration.menuFile();
                FileObject shadow = assertConfigFile(shadowPath);
                assertEquals(
                        "Flutter menu shadow points at the wrong action: " + shadowPath,
                        registration.originalFile(),
                        shadow.getAttribute("originalFile"));

                assertConfigFile(registration.originalFile());
                Action action = FileUtil.getConfigObject(registration.originalFile(), Action.class);
                assertNotNull(
                        "Flutter action cannot be resolved as javax.swing.Action: "
                                + registration.originalFile(),
                        action);
            }
        }

        private void assertFlutterToolbarActions() {
            assertConfigFile("Toolbars/Build");
            for (ToolbarActionRegistration registration : FLUTTER_TOOLBAR_ACTIONS) {
                String shadowPath = "Toolbars/Build/" + registration.toolbarFile();
                FileObject shadow = assertConfigFile(shadowPath);
                assertEquals(
                        "Flutter toolbar shadow points at the wrong action: " + shadowPath,
                        registration.originalFile(),
                        shadow.getAttribute("originalFile"));
                assertEquals(
                        "Flutter toolbar action has the wrong position: " + shadowPath,
                        registration.position(),
                        shadow.getAttribute("position"));

                Action action = FileUtil.getConfigObject(registration.originalFile(), Action.class);
                assertNotNull(
                        "Flutter toolbar action cannot be resolved: " + registration.originalFile(),
                        action);
                assertEquals(
                        "Flutter toolbar action has the wrong accessible name",
                        registration.displayName(),
                        action.getValue(Action.NAME));
                assertEquals(
                        "Flutter toolbar action does not expose its SVG iconBase",
                        registration.iconBase(),
                        action.getValue("iconBase"));
                assertEquals(
                        "Adding the toolbar button must not add icons to the Flutter menu",
                        Boolean.TRUE,
                        action.getValue("noIconInMenu"));
                assertActionIconResource(action, registration.iconBase());
            }

            FileObject separator = assertConfigFile(
                    "Toolbars/Build/"
                            + "dev-flutter-netbeans-plugin-run-HotReloadFlutterAction-separatorBefore.instance");
            assertEquals("Hot Reload toolbar separator has the wrong position",
                    355, separator.getAttribute("position"));
        }

        private void assertActionIconResource(Action action, String iconBase) {
            ClassLoader loader = action.getClass().getClassLoader();
            for (String path : List.of(
                    iconBase,
                    beforeExtension(iconBase, "_dark"),
                    beforeExtension(iconBase, "24"),
                    beforeExtension(iconBase, "24_dark"))) {
                assertNotNull("Flutter toolbar icon resource is missing: " + path,
                        loader.getResource(path));
                assertNotNull("NetBeans cannot load the Flutter toolbar SVG: " + path,
                        ImageUtilities.loadImageIcon(path, true));
            }
        }

        private String beforeExtension(String path, String suffix) {
            int extension = path.lastIndexOf('.');
            return extension < 0
                    ? path + suffix
                    : path.substring(0, extension) + suffix + path.substring(extension);
        }

        private FileObject assertConfigFile(String path) {
            FileObject file = FileUtil.getConfigFile(path);
            assertNotNull("NetBeans layer registration is missing: " + path, file);
            return file;
        }
    }

    private record ActionRegistration(String menuFile, String originalFile) {
    }

    private record ToolbarActionRegistration(
            String toolbarFile,
            String originalFile,
            String displayName,
            String iconBase,
            int position) {
    }

    private record LayerRegistration(String path, String instanceOf) {
    }
}
