package dev.flutter.netbeans.runtime;

import java.awt.Component;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.editor.BaseDocument;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.editor.completion.CompletionImpl;
import org.netbeans.modules.editor.completion.CompletionResultSetImpl;
import org.netbeans.modules.editor.hints.AnnotationHolder;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.netbeans.modules.editor.indent.spi.ReformatTask;
import org.netbeans.modules.editor.lib2.EditorApiPackageAccessor;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.lsp.client.Utils;
import org.netbeans.modules.lsp.client.bindings.TextDocumentSyncServerCapabilityHandler;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.LazyFixList;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.modules.ModuleInfo;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;

/** Optional functional Dart editor gate against an explicitly configured real SDK. */
final class DartEditorEndToEndIT {

    private static final String FLUTTER_MODULE = "dev.flutter.netbeans.netbeans.plugin";
    private static final String DART_EXECUTABLE_PROPERTY = "dart.executable";

    @Test
    void dartEditorFeaturesWorkInsideTheAssembledNetBeansRuntime() throws Exception {
        Path dartExecutable = configuredDartExecutable();
        System.setProperty(DART_EXECUTABLE_PROPERTY, dartExecutable.toString());

        junit.framework.Test suite = NbModuleSuite.createConfiguration(DartEditorRuntimeCase.class)
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
        Assertions.assertEquals(1, result.runCount(),
                "The Dart editor runtime test did not run exactly once");
    }

    private static Path configuredDartExecutable() {
        String configured = System.getProperty(DART_EXECUTABLE_PROPERTY, "").trim();
        Assumptions.assumeTrue(!configured.isEmpty(),
                "set -Ddart.executable=<path-to-dart>");
        Path executable = Path.of(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isRegularFile(executable),
                "Dart executable does not exist: " + executable);
        return executable;
    }

    private static String runtimeFailures(TestResult result) {
        StringBuilder message = new StringBuilder("Dart editor NetBeans runtime gate failed");
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

    public static final class DartEditorRuntimeCase extends NbTestCase {

        private static final String DART_MIME_TYPE = "text/x-dart";
        private static final Duration TIMEOUT = Duration.ofSeconds(30);
        private static final String COMPLETION_PROVIDER_CLASS =
                "org.netbeans.modules.lsp.client.bindings.CompletionProviderImpl";

        private Project project;
        private Object settings;
        private Object previousSettings;
        private Method saveSettings;
        private final List<JTextPane> editorPanes = new ArrayList<>();

        public DartEditorRuntimeCase(String name) {
            super(name);
        }

        public void testDartEditorFeaturesAndProjectLifecycle() throws Exception {
            clearWorkDir();
            Path dartExecutable = Path.of(
                    System.getProperty(DART_EXECUTABLE_PROPERTY)).toAbsolutePath().normalize();
            configureStandaloneDartSdk(dartExecutable);

            Path projectPath = createFlutterProject();
            Path helperPath = projectPath.resolve("lib/helper.dart");
            Path mainPath = projectPath.resolve("lib/main.dart");
            Path completionPath = projectPath.resolve("lib/completion.dart");
            Path formatPath = projectPath.resolve("lib/format.dart");
            Path importPath = projectPath.resolve("lib/missing_import.dart");

            FileUtil.refreshFor(projectPath.toFile());
            FileObject projectDirectory = requireFileObject(projectPath);
            FileObject helperFile = requireFileObject(helperPath);
            FileObject mainFile = requireFileObject(mainPath);
            FileObject completionFile = requireFileObject(completionPath);
            FileObject formatFile = requireFileObject(formatPath);
            FileObject importFile = requireFileObject(importPath);

            project = ProjectManager.getDefault().findProject(projectDirectory);
            assertNotNull("The assembled runtime did not recognize the Flutter project", project);

            try {
                openProject();
                Object lifecycle = dartAnalysisLifecycle();
                assertTrue("Dart analysis lifecycle was not opened with the project",
                        invokeBoolean(lifecycle, "isOpen"));

                StyledDocument mainDocument = openDocument(mainFile);
                StyledDocument completionDocument = openDocument(completionFile);
                StyledDocument formatDocument = openDocument(formatFile);
                StyledDocument importDocument = openDocument(importFile);

                LSPBindings firstBinding = awaitBinding(mainFile, null);
                Process firstProcess = bindingProcess(firstBinding);
                assertNotNull("The Dart LSP binding does not own a process", firstProcess);
                assertTrue("The Dart LSP process exited during startup", firstProcess.isAlive());
                assertEquals("The Dart provider must report the Dart language id",
                        "dart", firstBinding.resolveLanguageId(mainFile));
                assertNotNull("Dart LSP did not advertise completion",
                        firstBinding.getInitResult().getCapabilities().getCompletionProvider());
                assertNotNull("Dart LSP did not advertise definition navigation",
                        firstBinding.getInitResult().getCapabilities().getDefinitionProvider());
                assertNotNull("Dart LSP did not advertise formatting",
                        firstBinding.getInitResult().getCapabilities()
                                .getDocumentFormattingProvider());
                assertTrue("Dart LSP advertised neither range nor document formatting",
                        Utils.isEnabled(firstBinding.getInitResult().getCapabilities()
                                .getDocumentRangeFormattingProvider())
                        || Utils.isEnabled(firstBinding.getInitResult().getCapabilities()
                                .getDocumentFormattingProvider()));
                assertNotNull("Dart LSP did not advertise code actions",
                        firstBinding.getInitResult().getCapabilities().getCodeActionProvider());

                registerEditorPane(mainDocument);
                JTextPane completionPane = registerEditorPane(completionDocument);
                registerEditorPane(formatDocument);
                registerEditorPane(importDocument);
                refreshOpenedFilesInServers();
                await("NetBeans editor synchronization to open every Dart document", () ->
                        firstBinding.getOpenedFiles().contains(mainFile)
                        && firstBinding.getOpenedFiles().contains(completionFile)
                        && firstBinding.getOpenedFiles().contains(formatFile)
                        && firstBinding.getOpenedFiles().contains(importFile));

                assertDefinitionTargets(firstBinding, mainFile, helperPath);
                assertReferencesAndRename(firstBinding, mainFile, helperPath);
                assertCompletionAppliesAutoImport(completionDocument, completionPane);
                assertFormattingThroughNetBeans(formatDocument);
                assertDiagnosticAndQuickFix(firstBinding, importFile, importDocument);

                closeProject();
                assertFalse("Dart analysis lifecycle remained open after project close",
                        invokeBoolean(lifecycle, "isOpen"));
                await("the first Dart LSP binding to be removed after project close",
                        () -> !LSPBindings.getAllBindings().contains(firstBinding));
                await("the Dart LSP process to exit after project close",
                        () -> !firstProcess.isAlive());

                openProject();
                assertTrue("Dart analysis lifecycle did not reopen with the project",
                        invokeBoolean(lifecycle, "isOpen"));
                LSPBindings restartedBinding = awaitBinding(mainFile, firstBinding);
                assertNotSame("Project reopen reused the closed Dart LSP binding",
                        firstBinding, restartedBinding);
                refreshOpenedFilesInServers();
                await("NetBeans editor synchronization after project reopen",
                        () -> restartedBinding.getOpenedFiles().contains(mainFile));
                assertDefinitionTargets(restartedBinding, mainFile, helperPath);
            } finally {
                releaseEditorPanes();
                closeProject();
                restoreSettings();
                closeDocument(mainFile);
                closeDocument(completionFile);
                closeDocument(formatFile);
                closeDocument(importFile);
                closeDocument(helperFile);
            }
        }

        private Path createFlutterProject() throws Exception {
            Path root = getWorkDir().toPath().resolve("dart-editor-e2e");
            Path lib = Files.createDirectories(root.resolve("lib"));
            Files.writeString(root.resolve("pubspec.yaml"), """
                    name: netbeans_editor_e2e
                    environment:
                      sdk: '>=3.0.0 <4.0.0'
                    flutter:
                      uses-material-design: true
                    """, StandardCharsets.UTF_8);
            Files.writeString(lib.resolve("helper.dart"),
                    "int answer() => 42;\n", StandardCharsets.UTF_8);
            Files.writeString(lib.resolve("main.dart"), """
                    import 'helper.dart';

                    void main() {
                      print(answer());
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(lib.resolve("completion.dart"), """
                    void main() {
                      ans
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(lib.resolve("format.dart"),
                    "void main(){print('hello');}\n", StandardCharsets.UTF_8);
            Files.writeString(lib.resolve("missing_import.dart"), """
                    void main() {
                      File('missing.txt');
                    }
                    """, StandardCharsets.UTF_8);
            return root.toAbsolutePath().normalize();
        }

        private void configureStandaloneDartSdk(Path dartExecutable) throws Exception {
            Path bin = dartExecutable.getParent();
            assertNotNull("Dart executable has no bin directory: " + dartExecutable, bin);
            Path sdkHome = bin.getParent();
            assertNotNull("Dart executable has no SDK home: " + dartExecutable, sdkHome);

            ModuleInfo flutterModule = flutterModule();
            ClassLoader moduleLoader = flutterModule.getClassLoader();
            Class<?> settingsClass = Class.forName(
                    "dev.flutter.netbeans.plugin.settings.FlutterSettings",
                    true,
                    moduleLoader);
            Class<?> configClass = Class.forName(
                    "dev.flutter.netbeans.plugin.settings.FlutterToolchainConfig",
                    true,
                    moduleLoader);
            Constructor<?> configConstructor = configClass.getConstructor(
                    String.class, boolean.class, String.class);

            settings = settingsClass.getMethod("getDefault").invoke(null);
            previousSettings = settingsClass.getMethod("load").invoke(settings);
            saveSettings = settingsClass.getMethod("save", configClass);
            Object e2eConfig = configConstructor.newInstance("", false, sdkHome.toString());
            saveSettings.invoke(settings, e2eConfig);
        }

        private ModuleInfo flutterModule() {
            Collection<? extends ModuleInfo> modules = Lookup.getDefault()
                    .lookupAll(ModuleInfo.class);
            ModuleInfo module = modules.stream()
                    .filter(candidate -> FLUTTER_MODULE.equals(candidate.getCodeNameBase()))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Flutter module is missing from the assembled runtime", module);
            assertTrue("Flutter module is not active in the assembled runtime", module.isEnabled());
            return module;
        }

        private void restoreSettings() throws Exception {
            if (settings != null && previousSettings != null && saveSettings != null) {
                saveSettings.invoke(settings, previousSettings);
            }
        }

        private void openProject() throws Exception {
            OpenProjects.getDefault().open(new Project[] {project}, false, true);
            await("the Flutter project to open",
                    () -> OpenProjects.getDefault().isProjectOpen(project));
            OpenProjects.getDefault().openProjects().get(
                    TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }

        private void closeProject() throws Exception {
            if (project == null || !OpenProjects.getDefault().isProjectOpen(project)) {
                return;
            }
            OpenProjects.getDefault().close(new Project[] {project});
            await("the Flutter project to close",
                    () -> !OpenProjects.getDefault().isProjectOpen(project));
        }

        private Object dartAnalysisLifecycle() throws Exception {
            Class<?> lifecycleClass = Class.forName(
                    "dev.flutter.netbeans.plugin.project.DartAnalysisLifecycle",
                    true,
                    flutterModule().getClassLoader());
            Object lifecycle = project.getLookup().lookup(lifecycleClass);
            assertNotNull("Flutter project lookup has no DartAnalysisLifecycle", lifecycle);
            return lifecycle;
        }

        private boolean invokeBoolean(Object target, String method) throws Exception {
            return (boolean) target.getClass().getMethod(method).invoke(target);
        }

        private LSPBindings awaitBinding(FileObject file, LSPBindings previous)
                throws Exception {
            LSPBindings[] found = new LSPBindings[1];
            await("a responsive Dart LSP binding for " + file.getPath(), () -> {
                List<LSPBindings> bindings = LSPBindings.getBindings(file);
                found[0] = bindings.stream()
                        .filter(binding -> binding != previous)
                        .findFirst()
                        .orElse(null);
                return found[0] != null;
            });
            return found[0];
        }

        private StyledDocument openDocument(FileObject file) throws Exception {
            String resolvedMimeType = FileUtil.getMIMEType(file);
            assertEquals("The assembled runtime did not resolve a Dart source MIME type",
                    DART_MIME_TYPE, resolvedMimeType);
            EditorCookie editor = DataObject.find(file).getLookup().lookup(EditorCookie.class);
            assertNotNull("Dart file has no NetBeans EditorCookie: " + file.getPath(), editor);
            assertTrue("Dart EditorCookie is not backed by CloneableEditorSupport",
                    editor instanceof CloneableEditorSupport);
            // The fallback DataObject starts its support as text/plain. In this headless
            // gate, connect the MIME resolver to that same support before it constructs
            // the document, so the registered DartEditorKit creates the editor document.
            ((CloneableEditorSupport) editor).setMIMEType(resolvedMimeType);
            StyledDocument document = editor.openDocument();
            assertEquals("The Dart editor kit did not create a Dart document",
                    resolvedMimeType, document.getProperty("mimeType"));
            assertTrue("The Dart editor kit did not create a NetBeans BaseDocument: "
                    + document.getClass().getName(), document instanceof BaseDocument);
            assertNotNull("NetBeans hints cannot attach to the Dart editor document",
                    AnnotationHolder.getInstance(file));
            return document;
        }

        private void closeDocument(FileObject file) {
            try {
                EditorCookie editor = DataObject.find(file).getLookup().lookup(EditorCookie.class);
                if (editor != null) {
                    editor.close();
                }
            } catch (Exception ignored) {
                // Cleanup must not hide a feature assertion.
            }
        }

        private JTextPane registerEditorPane(StyledDocument document) throws Exception {
            JTextPane pane = onEdt(() -> {
                JTextPane component = new JTextPane();
                component.setDocument(document);
                EditorApiPackageAccessor.get().register(component);
                Method focusGained = EditorRegistry.class.getDeclaredMethod(
                        "focusGained", JTextComponent.class, Component.class);
                focusGained.setAccessible(true);
                focusGained.invoke(null, component, null);
                return component;
            });
            editorPanes.add(pane);
            assertTrue("Headless Dart editor was not registered with NetBeans",
                    EditorRegistry.componentList().contains(pane));
            assertSame("NetBeans editor registry does not expose the Dart document",
                    pane, EditorRegistry.findComponent(document));
            return pane;
        }

        private void refreshOpenedFilesInServers() throws Exception {
            onEdt(() -> {
                TextDocumentSyncServerCapabilityHandler.refreshOpenedFilesInServers();
                return null;
            });
        }

        private void releaseEditorPanes() {
            try {
                onEdt(() -> {
                    for (JTextPane pane : List.copyOf(editorPanes)) {
                        EditorApiPackageAccessor.get().forceRelease(pane);
                    }
                    editorPanes.clear();
                    return null;
                });
            } catch (Exception ignored) {
                // Cleanup must not hide a feature assertion.
            }
        }

        private Process bindingProcess(LSPBindings binding) throws Exception {
            Field process = LSPBindings.class.getDeclaredField("process");
            process.setAccessible(true);
            return (Process) process.get(binding);
        }

        private void assertDefinitionTargets(
                LSPBindings binding,
                FileObject mainFile,
                Path helperPath) throws Exception {
            Either<List<? extends Location>, List<? extends LocationLink>> result =
                    binding.getTextDocumentService()
                             .definition(new DefinitionParams(
                                    new TextDocumentIdentifier(Utils.toURI(mainFile)),
                                    new Position(3, 10)))
                            .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull("Dart definition request returned null", result);
            boolean pointsToHelper = result.isLeft()
                    ? result.getLeft().stream()
                            .map(Location::getUri)
                            .anyMatch(uri -> samePath(uri, helperPath))
                    : result.getRight().stream()
                            .map(LocationLink::getTargetUri)
                            .anyMatch(uri -> samePath(uri, helperPath));
            assertTrue("Go to Definition did not target helper.dart: " + result,
                    pointsToHelper);
        }

        private void assertReferencesAndRename(
                LSPBindings binding,
                FileObject mainFile,
                Path helperPath) throws Exception {
            TextDocumentIdentifier document = new TextDocumentIdentifier(
                    Utils.toURI(mainFile));
            Position symbol = new Position(3, 10);
            List<? extends Location> references = binding.getTextDocumentService()
                    .references(new ReferenceParams(
                            document,
                            symbol,
                            new ReferenceContext(true)))
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull("Dart references request returned null", references);
            assertTrue("Find Usages did not return declaration and invocation: " + references,
                    references.size() >= 2);
            assertTrue("Find Usages did not include helper.dart: " + references,
                    references.stream()
                            .map(Location::getUri)
                            .anyMatch(uri -> samePath(uri, helperPath)));
            assertTrue("Find Usages did not include main.dart: " + references,
                    references.stream()
                            .map(Location::getUri)
                            .anyMatch(uri -> samePath(uri,
                                    Path.of(mainFile.toURI()))));

            WorkspaceEdit rename = binding.getTextDocumentService()
                    .rename(new RenameParams(document, symbol, "renamedAnswer"))
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull("Dart rename request returned null", rename);
            String renamePayload = rename.toString();
            assertTrue("Rename edit omitted the requested identifier: " + renamePayload,
                    renamePayload.contains("renamedAnswer"));
            assertTrue("Rename edit omitted helper.dart: " + renamePayload,
                    workspaceEditContainsPath(rename, helperPath));
            assertTrue("Rename edit omitted main.dart: " + renamePayload,
                    workspaceEditContainsPath(rename, Path.of(mainFile.toURI())));
        }

        private void assertCompletionAppliesAutoImport(
                StyledDocument document,
                JTextPane pane)
                throws Exception {
            onEdt(() -> {
                pane.setCaretPosition(document.getText(0, document.getLength())
                        .indexOf("ans") + "ans".length());
                return null;
            });

            CompletionProvider provider = MimeLookup.getLookup(DART_MIME_TYPE)
                    .lookupAll(CompletionProvider.class)
                    .stream()
                    .filter(candidate -> COMPLETION_PROVIDER_CLASS.equals(
                            candidate.getClass().getName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull("NetBeans LSP completion provider is not available for Dart", provider);

            CompletionTask task = provider.createTask(
                    CompletionProvider.COMPLETION_QUERY_TYPE, pane);
            assertNotNull("NetBeans LSP completion provider rejected the Dart document", task);
            CompletionResultSetImpl result = CompletionImpl.get().createTestResultSet(
                    task, CompletionProvider.COMPLETION_QUERY_TYPE);
            task.query(result.getResultSet());
            await("Dart completion results", result::isFinished);

            CompletionItem answer = result.getItems().stream()
                    .filter(item -> item.getInsertPrefix() != null
                            && item.getInsertPrefix().toString().startsWith("answer"))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Dart completion did not contain answer: "
                    + result.getItems().stream()
                            .map(CompletionItem::getInsertPrefix)
                            .filter(Objects::nonNull)
                            .limit(30)
                            .toList(), answer);

            onEdt(() -> {
                answer.defaultAction(pane);
                return null;
            });
            String completed = document.getText(0, document.getLength());
            assertTrue("NetBeans completion did not add helper.dart import: " + completed,
                    completed.contains("import 'helper.dart';"));
            assertFalse("NetBeans completion left the partial identifier unchanged: " + completed,
                    completed.contains("  ans\n"));
            assertTrue("NetBeans completion did not insert answer: " + completed,
                    completed.contains("answer"));
        }

        private void assertFormattingThroughNetBeans(StyledDocument document)
                throws Exception {
            assertEquals("The Dart document has the wrong NetBeans MIME type",
                    DART_MIME_TYPE, document.getProperty("mimeType"));
            assertNotNull("NetBeans cannot resolve the Dart document's FileObject",
                    NbEditorUtilities.getFileObject(document));
            assertFalse("The Dart MIME lookup has no language server provider",
                    MimeLookup.getLookup(DART_MIME_TYPE)
                            .lookupAll(LanguageServerProvider.class)
                            .isEmpty());
            List<String> factories = MimeLookup.getLookup(DART_MIME_TYPE)
                    .lookupAll(ReformatTask.Factory.class)
                    .stream()
                    .map(factory -> factory.getClass().getName())
                    .toList();
            assertTrue("NetBeans did not register its LSP formatter for Dart: " + factories,
                    factories.contains(
                            "org.netbeans.modules.lsp.client.bindings.Formatter$Factory"));

            Reformat reformat = Reformat.get(document);
            assertNotNull("NetBeans did not provide Reformat for the Dart document", reformat);
            reformat.lock();
            try {
                List<String> tasks = selectedReformatTasks(reformat);
                assertTrue("NetBeans Reformat did not select the Dart LSP task: " + tasks,
                        tasks.contains("org.netbeans.modules.lsp.client.bindings.Formatter"));
                reformat.reformat(0, document.getLength());
            } finally {
                reformat.unlock();
            }
            assertEquals("NetBeans Reformat did not apply the Dart LSP edit",
                    "void main() {\n  print('hello');\n}\n",
                    document.getText(0, document.getLength()));
        }

        private List<String> selectedReformatTasks(Reformat reformat) throws Exception {
            Field implField = Reformat.class.getDeclaredField("impl");
            implField.setAccessible(true);
            Object impl = implField.get(reformat);

            Field handlerField = impl.getClass().getDeclaredField("reformatHandler");
            handlerField.setAccessible(true);
            Object handler = handlerField.get(impl);
            if (handler == null) {
                return List.of();
            }

            Field itemsField = handler.getClass().getDeclaredField("items");
            itemsField.setAccessible(true);
            Object value = itemsField.get(handler);
            if (!(value instanceof List<?> items)) {
                return List.of();
            }

            return items.stream()
                    .map(item -> {
                        try {
                            Field taskField = item.getClass().getDeclaredField("reformatTask");
                            taskField.setAccessible(true);
                            Object task = taskField.get(item);
                            return task != null ? task.getClass().getName() : "<null>";
                        } catch (ReflectiveOperationException ex) {
                            return "<unavailable:" + ex.getClass().getSimpleName() + ">";
                        }
                    })
                    .toList();
        }

        private void assertDiagnosticAndQuickFix(
                LSPBindings binding,
                FileObject file,
                StyledDocument document) throws Exception {
            @SuppressWarnings("unchecked")
            List<ErrorDescription>[] published = new List[] {List.of()};
            await("a published Dart diagnostic in the NetBeans hints model", () -> {
                AnnotationHolder holder = AnnotationHolder.getInstance(file);
                if (holder == null) {
                    return false;
                }
                published[0] = holder.getErrors().stream().toList();
                return !published[0].isEmpty();
            });
            ErrorDescription diagnostic = published[0].stream()
                    .filter(error -> error.getDescription() != null
                            && error.getDescription().toLowerCase(Locale.ROOT)
                                    .contains("file"))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Dart diagnostics did not include the undefined File symbol: "
                    + published[0].stream()
                            .map(ErrorDescription::getDescription)
                            .toList(), diagnostic);
            assertFalse("Dart diagnostic message is blank",
                    diagnostic.getDescription().isBlank());

            LazyFixList fixes = diagnostic.getFixes();
            assertNotNull("Dart diagnostic has no NetBeans Quick Fix list", fixes);
            awaitDartIoCodeActionAvailability(binding, file, fixes);

            // RELEASE300 caches the first code-action response permanently. Trigger
            // the real lazy adapter only after the server can answer this exact
            // diagnostic, avoiding a race with Dart's initial analysis pass.
            fixes.getFixes();
            await("the NetBeans Quick Fix adapter to finish", fixes::isComputed);
            List<Fix> computedFixes = fixes.getFixes();
            assertNotNull("NetBeans Quick Fix computation returned null", computedFixes);
            Fix importFix = computedFixes.stream()
                    .filter(fix -> fix.getText() != null
                            && fix.getText().toLowerCase(Locale.ROOT).contains("dart:io"))
                    .findFirst()
                    .orElse(null);
            assertNotNull("NetBeans Quick Fixes did not offer dart:io: "
                    + computedFixes.stream().map(Fix::getText).toList(), importFix);

            importFix.implement();
            await("the dart:io Quick Fix to edit the NetBeans document", () -> {
                try {
                    return document.getText(0, document.getLength())
                            .contains("import 'dart:io';");
                } catch (Exception ex) {
                    return false;
                }
            });
        }

        private void awaitDartIoCodeActionAvailability(
                LSPBindings binding,
                FileObject file,
                LazyFixList fixes) throws Exception {
            Field diagnosticField = fixes.getClass().getDeclaredField("diagnostic");
            diagnosticField.setAccessible(true);
            Diagnostic lspDiagnostic = (Diagnostic) diagnosticField.get(fixes);
            assertNotNull("NetBeans Quick Fix list lost its LSP diagnostic", lspDiagnostic);

            Field uriField = fixes.getClass().getDeclaredField("fileUri");
            uriField.setAccessible(true);
            String fileUri = (String) uriField.get(fixes);
            assertEquals("NetBeans Quick Fix list targets a different Dart document",
                    Utils.toURI(file), fileUri);
            assertTrue("The captured LSP diagnostic does not describe File: "
                    + lspDiagnostic,
                    lspDiagnostic.getMessage() != null
                            && lspDiagnostic.getMessage().toLowerCase(Locale.ROOT)
                                    .contains("file"));

            CodeActionParams params = new CodeActionParams(
                    new TextDocumentIdentifier(fileUri),
                    lspDiagnostic.getRange(),
                    new CodeActionContext(List.of(lspDiagnostic)));
            List<String> lastTitles = List.of();
            Throwable lastFailure = null;
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                try {
                    List<Either<Command, CodeAction>> actions = binding
                            .getTextDocumentService()
                            .codeAction(params)
                            .get(5, TimeUnit.SECONDS);
                    lastTitles = actions == null ? List.of() : actions.stream()
                            .map(action -> action.isLeft()
                                    ? action.getLeft().getTitle()
                                    : action.getRight().getTitle())
                            .filter(Objects::nonNull)
                            .toList();
                    if (lastTitles.stream().anyMatch(title -> title
                            .toLowerCase(Locale.ROOT).contains("dart:io"))) {
                        return;
                    }
                } catch (Exception ex) {
                    lastFailure = ex;
                }
                Thread.sleep(250);
            }
            AssertionError unavailable = new AssertionError(
                    "Dart server did not offer dart:io for the published diagnostic; "
                    + "last actions: " + lastTitles + ", diagnostic: " + lspDiagnostic);
            if (lastFailure != null) {
                unavailable.initCause(lastFailure);
            }
            throw unavailable;
        }

        private FileObject requireFileObject(Path path) {
            File normalized = FileUtil.normalizeFile(path.toFile());
            FileObject file = FileUtil.toFileObject(normalized);
            assertNotNull("NetBeans did not map local path to FileObject: " + path, file);
            return file;
        }

        private static boolean samePath(String uri, Path expected) {
            try {
                return Path.of(URI.create(uri)).toAbsolutePath().normalize()
                        .equals(expected.toAbsolutePath().normalize());
            } catch (RuntimeException ex) {
                return false;
            }
        }

        private static boolean workspaceEditContainsPath(
                WorkspaceEdit edit,
                Path expected) {
            if (edit.getChanges() != null
                    && edit.getChanges().keySet().stream()
                            .anyMatch(uri -> samePath(uri, expected))) {
                return true;
            }
            return edit.getDocumentChanges() != null
                    && edit.getDocumentChanges().stream()
                            .filter(Either::isLeft)
                            .map(Either::getLeft)
                            .map(change -> change.getTextDocument().getUri())
                            .anyMatch(uri -> samePath(uri, expected));
        }

        private void await(String description, BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            Throwable lastFailure = null;
            while (System.nanoTime() < deadline) {
                try {
                    if (condition.getAsBoolean()) {
                        return;
                    }
                } catch (RuntimeException | AssertionError ex) {
                    lastFailure = ex;
                }
                Thread.sleep(50);
            }
            AssertionError timeout = new AssertionError("Timed out waiting for " + description);
            if (lastFailure != null) {
                timeout.initCause(lastFailure);
            }
            throw timeout;
        }

        private static <T> T onEdt(ThrowingSupplier<T> action) throws Exception {
            if (SwingUtilities.isEventDispatchThread()) {
                return action.get();
            }
            Object[] result = new Object[1];
            Throwable[] failure = new Throwable[1];
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = action.get();
                } catch (Throwable ex) {
                    failure[0] = ex;
                }
            });
            if (failure[0] instanceof Exception exception) {
                throw exception;
            }
            if (failure[0] instanceof Error error) {
                throw error;
            }
            @SuppressWarnings("unchecked")
            T typed = (T) result[0];
            return typed;
        }

        @FunctionalInterface
        private interface ThrowingSupplier<T> {
            T get() throws Exception;
        }
    }
}
