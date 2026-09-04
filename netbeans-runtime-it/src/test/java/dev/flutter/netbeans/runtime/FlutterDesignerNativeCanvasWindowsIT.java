package dev.flutter.netbeans.runtime;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.core.api.multiview.MultiViewHandler;
import org.netbeans.core.api.multiview.MultiViewPerspective;
import org.netbeans.core.api.multiview.MultiViews;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestCase;
import org.openide.awt.Actions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;
import org.openide.windows.CloneableTopComponent;

/**
 * Opt-in assembled-runtime acceptance for the real Windows Flutter Canvas.
 *
 * <p>The gate opens production Flutter Designer {@link DataObject} MultiViews;
 * it does not construct a Canvas session or a surrogate AWT frame itself.
 * Release verification supplies
 * {@code -Dcanvas.runner.acceptance.flutter.sdk=<absolute SDK root>} together
 * with {@code -Dnetbeans.runtime.it.fork.timeout.seconds=900} for a cold build,
 * and must
 * observe the real AWT HWND, isolated runner process and FLUTTERVIEW child.</p>
 *
 * <p>The harness does not treat programmatic {@code requestActive()} as physical
 * foreground authority. After the first frame is rendered it resolves and
 * raises the exact assembled-runtime root HWND without activating it, establishes
 * Swing focus with a real {@link Robot} click, and only then clicks the Canvas
 * surface. Production must continue to refuse focus while an unrelated process
 * owns the foreground window.</p>
 *
 * <p>This scope proves live divider-resize convergence, two-view isolation,
 * exact native attachment/DPR/DPI-awareness facts, one tab peer-loss
 * teardown/recreation cycle, two simultaneous production Split Document Design
 * surfaces with bounded split/clear/re-split peer churn, an actual heavyweight
 * Preview popup above the child HWND, explicit AWT-parent HWND retirement, the
 * explicit crash Retry UI and close cleanup. A real per-monitor DPI
 * transition still requires a physical second monitor with a different scale;
 * this gate records the exact current HWND DPI but deliberately does not
 * synthesize {@code WM_DPICHANGED}. It also makes no claim about IME
 * composition, broader native menu/popup paths beyond the accepted Preview
 * selector and Window → Services menu, or unbounded heavyweight peer-recreation
 * patterns outside the accepted tab and Split Document matrix.</p>
 */
final class FlutterDesignerNativeCanvasWindowsIT {
    private static final String FLUTTER_MODULE =
            "dev.flutter.netbeans.netbeans.plugin";
    private static final String SDK_PROPERTY =
            "canvas.runner.acceptance.flutter.sdk";

    @Test
    void realDesignMultiViewsSurviveCrashRetryAndCloseInAssembledWindowsRuntime() {
        Assumptions.assumeTrue(
                System.getProperty("os.name", "").startsWith("Windows"),
                "The native HWND acceptance gate requires Windows");
        String configuredSdk = System.getProperty(SDK_PROPERTY, "").trim();
        Assumptions.assumeTrue(
                !configuredSdk.isEmpty(),
                "Supply -D" + SDK_PROPERTY + "=<Flutter SDK root>");
        Path sdk = Path.of(configuredSdk).toAbsolutePath().normalize();
        Assumptions.assumeTrue(
                Files.isRegularFile(sdk.resolve("bin").resolve("flutter.bat")),
                "The configured Flutter SDK has no bin/flutter.bat: " + sdk);
        System.setProperty(SDK_PROPERTY, sdk.toString());

        junit.framework.Test suite = NbModuleSuite.createConfiguration(
                        NativeCanvasRuntimeCase.class)
                .clusters("ide|harness|extra")
                .enableModules("extra", Pattern.quote(FLUTTER_MODULE))
                .enableClasspathModules(false)
                .honorAutoloadEager(true)
                .failOnMessage(java.util.logging.Level.SEVERE)
                .failOnException(java.util.logging.Level.SEVERE)
                .gui(true)
                .suite();

        TestResult result = new TestResult();
        suite.run(result);
        if (!result.wasSuccessful()) {
            Assertions.fail(runtimeFailures(result));
        }
        Assertions.assertEquals(
                1,
                result.runCount(),
                "The native Canvas assembled-runtime case did not run exactly once");
    }

    private static String runtimeFailures(TestResult result) {
        StringBuilder message = new StringBuilder(
                "Native Flutter Canvas assembled-runtime gate failed");
        appendFailures(message, "error", result.errors());
        appendFailures(message, "failure", result.failures());
        return message.toString();
    }

    private static void appendFailures(
            StringBuilder message,
            String kind,
            Enumeration<TestFailure> failures) {
        while (failures.hasMoreElements()) {
            message.append(System.lineSeparator())
                    .append(kind)
                    .append(": ")
                    .append(failures.nextElement().trace());
        }
    }

    public static final class NativeCanvasRuntimeCase extends NbTestCase {
        private static final String DESIGNER_DATA_OBJECT =
                "dev.flutter.netbeans.plugin.designer.FlutterDesignerDataObject";
        private static final String DESIGNER_MIME = "text/x-flutter-designer";
        private static final String DESIGN_PERSPECTIVE = "flutter.designer.design";
        private static final String HOST_CLASS =
                "dev.flutter.netbeans.plugin.designer.canvas.WindowsNativeCanvasHost";
        private static final String HOST_ACCESSIBLE_NAME =
                "Native Flutter Canvas host";
        private static final String STATUS_ACCESSIBLE_NAME =
                "Native Flutter Canvas status";
        private static final String RETRY_ACCESSIBLE_NAME =
                "Retry Native Flutter Canvas";
        private static final String SURFACE_ACCESSIBLE_NAME =
                "Native Flutter Canvas surface";
        private static final String PREVIEW_TARGET_ACCESSIBLE_NAME =
                "Flutter Canvas preview target";
        private static final String WIDGET_TREE_ACCESSIBLE_NAME =
                "Flutter Designer widget tree";
        private static final String INPUT_SYNC_ACCESSIBLE_NAME =
                "Native Canvas input synchronization status";
        private static final String SPLIT_DOCUMENT_HORIZONTALLY_ACTION =
                "org.netbeans.core.multiview.SplitDocumentHorizontallyAction";
        private static final String CLEAR_SPLIT_ACTION =
                "org.netbeans.core.multiview.ClearSplitAction";
        private static final String RENDERED_STATUS =
                "Native Flutter Canvas rendered.";
        private static final int DPI_AWARENESS_PER_MONITOR_AWARE = 2;
        private static final Duration START_TIMEOUT = Duration.ofMinutes(3);
        private static final Duration STOP_TIMEOUT = Duration.ofSeconds(20);
        private static final Duration FOCUS_STABILITY_WINDOW =
                Duration.ofMillis(750);
        private static final int GA_ROOT = 2;
        private static final int SWP_NOSIZE = 0x0001;
        private static final int SWP_NOMOVE = 0x0002;
        private static final int SWP_NOACTIVATE = 0x0010;
        private static final int SWP_NOOWNERZORDER = 0x0200;
        private static final int SWP_ASYNCWINDOWPOS = 0x4000;
        private static final int TEST_ROOT_Z_ORDER_FLAGS = SWP_NOSIZE
                | SWP_NOMOVE
                | SWP_NOACTIVATE
                | SWP_NOOWNERZORDER
                | SWP_ASYNCWINDOWPOS;
        private static final String ACCEPTED_MAIN_MENU = "Window";
        private static final String ACCEPTED_MAIN_MENU_ITEM = "Services";
        private static final int MAIN_MENU_CANVAS_OVERLAP_PIXELS = 32;

        private ClassLoader loader;
        private Object nativeApi;
        private Method nativeIsWindow;
        private Method nativeOwnerProcessId;
        private Method nativeClientBounds;
        private Method nativeWindowDpi;
        private Method nativeForegroundFocusedWindow;
        private Constructor<?> jnaPointerConstructor;
        private Constructor<?> jnaPointByValueConstructor;
        private Method jnaPointerNativeValue;
        private Object getWindowDpiAwarenessContext;
        private Object getAwarenessFromDpiAwarenessContext;
        private Object getAncestorFunction;
        private Object isIconicFunction;
        private Object setWindowPosFunction;
        private Object windowFromPointFunction;
        private Method jnaInvokePointer;
        private Method jnaInvokeInt;

        public NativeCanvasRuntimeCase(String name) {
            super(name);
        }

        public void testRealDesignMultiViewsOwnIndependentCrashRetryCloseLifecycles()
                throws Exception {
            clearWorkDir();
            ModuleInfo module = flutterModule();
            loader = module.getClassLoader();
            configureFlutterSdk(Path.of(System.getProperty(SDK_PROPERTY)));
            initializeNativeWindowProbe();

            Path projectRoot = createFlutterProject(
                    getWorkDir().toPath().resolve("native-multiview"));
            provisionDefaultProjectTheme(projectRoot);
            Pair pair = createPair(projectRoot, "native_canvas_gate");
            DataObject owner = DataObject.find(pair.dart());
            assertEquals("The paired fixture is not owned by the packaged Designer",
                    DESIGNER_DATA_OBJECT, owner.getClass().getName());

            NativeView first = openDesignView(owner, "first");
            NativeView second = null;
            NativeIdentity firstIdentity = null;
            NativeIdentity secondIdentity = null;
            NativeIdentity recreatedFirstIdentity = null;
            NativeIdentity restartedSecondIdentity = null;
            try {
                awaitRendered(first, START_TIMEOUT);
                firstIdentity = first.identity();
                assertNativeIdentity("first Design MultiView", firstIdentity);
                assertExactSurfaceMetrics(
                        "first Design MultiView", firstIdentity, first.surfaceMetrics());
                first.establishTestedIdeForeground(firstIdentity);
                first.clickCanvas();
                awaitRunnerFocused(first, firstIdentity);
                assertFocusRoundTrip(first, firstIdentity);
                assertPreviewPopupRoundTrip(first, firstIdentity);
                assertNetBeansMainMenuPopupRoundTrip(first, firstIdentity);
                assertLiveResizeBurst(first, firstIdentity);
                firstIdentity = assertSplitDocumentSurfaceLifecycle(
                        first, firstIdentity);

                second = openDesignView(owner, "second");
                assertNotSame("Two requested Designer MultiViews reused one TopComponent",
                        first.multiView(), second.multiView());
                assertNotSame("Two requested Designer MultiViews reused one native host",
                        first.host(), second.host());
                second.focusNetBeansControl();
                await(second.label()
                                + " could not establish physical NetBeans focus "
                                + "while its Canvas was attaching; "
                                + second.focusDiagnostic(),
                        STOP_TIMEOUT,
                        second::netBeansPhysicallyFocused);
                awaitRendered(second, START_TIMEOUT);
                secondIdentity = second.identity();
                assertNativeIdentity("second Design MultiView", secondIdentity);
                assertExactSurfaceMetrics(
                        "second Design MultiView", secondIdentity, second.surfaceMetrics());
                assertNetBeansFocusStable(second);
                second.clickCanvas();
                awaitRunnerFocused(second, secondIdentity);
                assertIndependent(firstIdentity, secondIdentity);
                awaitRetiredIncludingParent(
                        "hidden first Design MultiView after AWT peer loss",
                        firstIdentity);
                assertTrue("Switching tabs closed the first Designer MultiView",
                        first.isOpened());

                ProcessHandle crashedProcess = secondIdentity.process();
                assertTrue("The second Canvas runner exited before the crash probe; "
                        + second.diagnostic(secondIdentity),
                        crashedProcess.isAlive());
                assertTrue("Windows rejected termination of the second Canvas runner",
                        crashedProcess.destroyForcibly());
                await("The crashed Canvas runner process remained alive",
                        STOP_TIMEOUT, () -> !crashedProcess.isAlive());
                await("The crashed Canvas did not expose an enabled Retry action",
                        STOP_TIMEOUT, second::retryAvailable);
                String terminalStatus = second.statusText();
                assertFalse("The crashed Canvas retained a rendered status",
                        RENDERED_STATUS.equals(terminalStatus));
                assertFalse("The crashed Canvas terminal status is blank",
                        terminalStatus == null || terminalStatus.isBlank());
                String retryDescription = second.retryDescription();
                assertNotNull("The Retry action has no terminal-state description",
                        retryDescription);
                assertTrue("The Retry action has no concrete terminal-state description",
                        retryDescription.startsWith("Retry "));
                awaitGone("crashed second Design MultiView", secondIdentity);
                assertTrue("Crashing the second Canvas closed the first MultiView",
                        first.isOpened());
                assertTrue("Crashing the second Canvas closed its own MultiView",
                        second.isOpened());

                first.activate();
                awaitRendered(first, START_TIMEOUT);
                recreatedFirstIdentity = first.identity();
                assertTrue("AWT peer recreation reused the retired first process identity",
                        recreatedFirstIdentity.processId() != firstIdentity.processId());
                assertTrue("The recreated first Canvas reused the crashed second process identity",
                        recreatedFirstIdentity.processId() != secondIdentity.processId());
                assertNativeIdentity(
                        "recreated first Design MultiView", recreatedFirstIdentity);
                assertExactSurfaceMetrics(
                        "recreated first Design MultiView",
                        recreatedFirstIdentity,
                        first.surfaceMetrics());
                awaitRunnerFocused(first, recreatedFirstIdentity);
                assertTrue("Recreating the first Canvas cleared the second Canvas Retry state",
                        second.retryAvailable());

                first.close();
                awaitRetiredIncludingParent(
                        "recreated first Design MultiView after close",
                        recreatedFirstIdentity);
                // Retired-owner status callbacks are epoch-fenced. The session
                // protocol gate proves the authenticated host.close handshake;
                // this assembled-runtime gate proves peer and process-tree retirement.
                assertFalse("The closed first Design MultiView remained open",
                        first.isOpened());

                second.activate();
                await("The activated failed second Canvas did not keep Retry available",
                        STOP_TIMEOUT, second::retryAvailable);
                second.clickRetry();
                awaitRendered(second, START_TIMEOUT);
                restartedSecondIdentity = second.identity();
                assertTrue("Retry reused the crashed second process identity",
                        restartedSecondIdentity.processId() != secondIdentity.processId());
                assertNativeIdentity(
                        "retried second Design MultiView", restartedSecondIdentity);
                assertExactSurfaceMetrics(
                        "retried second Design MultiView",
                        restartedSecondIdentity,
                        second.surfaceMetrics());
                awaitRunnerFocused(second, restartedSecondIdentity);
                second.close();
                awaitRetiredIncludingParent(
                        "retried second Design MultiView after close",
                        restartedSecondIdentity);
                assertFalse("The closed second Design MultiView remained open",
                        second.isOpened());
            } finally {
                try {
                    closeQuietly(first);
                } finally {
                    closeQuietly(second);
                }
                awaitRetiredIncludingParentIfPresent(
                        "recreated first Design MultiView final cleanup",
                        recreatedFirstIdentity);
                awaitRetiredIncludingParentIfPresent(
                        "first retired generation final cleanup", firstIdentity);
                awaitRetiredIncludingParentIfPresent(
                        "second crashed generation final cleanup", secondIdentity);
                awaitRetiredIncludingParentIfPresent(
                        "retried second Design MultiView final cleanup",
                        restartedSecondIdentity);
            }
        }

        private NativeView openDesignView(DataObject owner, String label)
                throws Exception {
            Box<CloneableTopComponent> opened = new Box<>();
            onEdt(() -> {
                CloneableTopComponent multiView = MultiViews.createCloneableMultiView(
                        DESIGNER_MIME, owner);
                opened.value = multiView;
                multiView.open();
                assertTrue("The " + label + " Designer MultiView did not open",
                        multiView.isOpened());
                requestDesignVisible(multiView);
                multiView.requestActive();
            });
            CloneableTopComponent multiView = opened.value;
            await("The " + label + " Designer MultiView did not assemble its real Canvas UI",
                    Duration.ofSeconds(30),
                    () -> hasCanvasUi(multiView));
            return locateNativeView(label, multiView);
        }

        private boolean hasCanvasUi(CloneableTopComponent multiView) {
            try {
                return onEdtValue(() -> findClassComponentOrNull(
                                multiView, HOST_CLASS) != null
                        && findAccessibleComponentOrNull(
                                multiView, JLabel.class, STATUS_ACCESSIBLE_NAME) != null
                        && findAccessibleComponentOrNull(
                                multiView, JButton.class, RETRY_ACCESSIBLE_NAME) != null);
            } catch (Exception failure) {
                throw new AssertionError(
                        "Cannot inspect the assembled Designer component tree", failure);
            }
        }

        private NativeView locateNativeView(
                String label,
                CloneableTopComponent multiView) throws Exception {
            Component host = onEdtValue(() ->
                    findClassComponentOrNull(multiView, HOST_CLASS));
            assertNotNull("The " + label
                    + " Designer component tree has no production WindowsNativeCanvasHost",
                    host);
            return locateNativeView(label, multiView, host);
        }

        private NativeView locateNativeView(
                String label,
                CloneableTopComponent multiView,
                Component host) throws Exception {
            return onEdtValue(() -> {
                assertTrue("The " + label
                                + " native host is not inside its Designer MultiView",
                        host == multiView
                        || SwingUtilities.isDescendingFrom(host, multiView));
                assertEquals("The production native host has the wrong accessible name",
                        HOST_ACCESSIBLE_NAME,
                        accessibleName(host));
                Component viewRoot = singleNativeViewRoot(multiView, host);
                JLabel status = findAccessibleComponentOrNull(
                        viewRoot, JLabel.class, STATUS_ACCESSIBLE_NAME);
                assertNotNull("The " + label
                        + " Designer component tree has no native Canvas status label",
                        status);
                JButton retry = findAccessibleComponentOrNull(
                        viewRoot, JButton.class, RETRY_ACCESSIBLE_NAME);
                assertNotNull("The " + label
                        + " Designer component tree has no native Canvas Retry action",
                        retry);
                java.awt.Canvas surface = findAccessibleComponentOrNull(
                        host, java.awt.Canvas.class, SURFACE_ACCESSIBLE_NAME);
                assertNotNull("The " + label
                        + " Designer component tree has no real AWT Canvas surface",
                        surface);
                JComboBox<?> previewTarget = findAccessibleComponentOrNull(
                        viewRoot, JComboBox.class, PREVIEW_TARGET_ACCESSIBLE_NAME);
                assertNotNull("The " + label
                        + " Designer component tree has no preview target focus control",
                        previewTarget);
                Component widgetTreeHost = findAccessibleComponentOrNull(
                        viewRoot, Component.class, WIDGET_TREE_ACCESSIBLE_NAME);
                assertNotNull("The " + label
                        + " Designer component tree has no widget tree",
                        widgetTreeHost);
                JTree widgetTree = findTypeComponentOrNull(widgetTreeHost, JTree.class);
                assertNotNull("The " + label
                        + " Designer widget tree has no real Swing JTree focus target",
                        widgetTree);
                JLabel inputSyncStatus = findAccessibleComponentOrNull(
                        viewRoot, JLabel.class, INPUT_SYNC_ACCESSIBLE_NAME);
                assertNotNull("The " + label
                        + " Designer component tree has no input synchronization status",
                        inputSyncStatus);
                JSplitPane resizeSplit = findResizeSplit(host);
                assertNotNull("The " + label
                        + " native host has no production resize split ancestor",
                        resizeSplit);
                return new NativeView(
                        label,
                        multiView,
                        host,
                        status,
                        retry,
                        surface,
                        previewTarget,
                        widgetTree,
                        inputSyncStatus,
                        resizeSplit);
            });
        }

        private NativeView splitDesignView(NativeView primary, int cycle)
                throws Exception {
            onEdt(() -> {
                primary.multiView().requestActive();
                Action split = Actions.forID(
                        "Window", SPLIT_DOCUMENT_HORIZONTALLY_ACTION);
                assertNotNull("NetBeans did not register the production Split Document "
                        + "Horizontally action", split);
                split.actionPerformed(new ActionEvent(
                        primary.multiView(),
                        ActionEvent.ACTION_PERFORMED,
                        "native-canvas-split-" + cycle));
            });
            await("Split Document cycle " + cycle
                            + " did not create exactly two native Design surfaces",
                    Duration.ofSeconds(30),
                    () -> nativeHostCount(primary.multiView()) == 2);
            Component splitHost = onEdtValue(() -> {
                List<Component> hosts = nativeHosts(primary.multiView());
                assertEquals("Split Document cycle " + cycle
                                + " assembled an unexpected native host count",
                        2, hosts.size());
                assertTrue("Split Document cycle " + cycle
                                + " removed the original native host",
                        hosts.contains(primary.host()));
                return hosts.stream()
                        .filter(candidate -> candidate != primary.host())
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Split Document cycle " + cycle
                                + " did not expose a distinct second native host"));
            });
            return locateNativeView(
                    primary.label() + " split Design surface #" + cycle,
                    primary.multiView(),
                    splitHost);
        }

        private void clearDesignSplit(
                NativeView primary,
                NativeView split,
                int cycle) throws Exception {
            onEdt(() -> {
                primary.multiView().requestActive();
                Action clear = Actions.forID("Window", CLEAR_SPLIT_ACTION);
                assertNotNull("NetBeans did not register the production Clear Split action",
                        clear);
                clear.actionPerformed(new ActionEvent(
                        primary.multiView(),
                        ActionEvent.ACTION_PERFORMED,
                        "native-canvas-clear-split-" + cycle));
            });
            await("Clear Split cycle " + cycle
                            + " did not retire the removed native Design surface",
                    Duration.ofSeconds(30),
                    () -> nativeHostCount(primary.multiView()) == 1);
            onEdt(() -> {
                List<Component> survivors = nativeHosts(primary.multiView());
                assertEquals("Clear Split cycle " + cycle
                                + " left an unexpected native host count",
                        1, survivors.size());
                assertSame("Clear Split cycle " + cycle
                                + " retained the wrong native Design surface",
                        primary.host(), survivors.get(0));
                assertFalse("Clear Split cycle " + cycle
                                + " left the removed native host showing",
                        split.host().isShowing());
            });
        }

        private void awaitRunnerFocused(NativeView view, NativeIdentity identity)
                throws Exception {
            boolean focused = waitUntil(STOP_TIMEOUT, view::runnerFocused);
            assertTrue(view.label()
                            + " activation did not focus its exact FLUTTERVIEW HWND; "
                            + view.diagnostic(identity) + "; "
                            + view.focusDiagnostic(),
                    focused);
            NativeIdentity focusedIdentity = view.identity();
            assertEquals(view.label() + " focused a stale runner generation",
                    identity.processId(), focusedIdentity.processId());
            assertEquals(view.label() + " focused a stale FLUTTERVIEW HWND",
                    identity.flutterViewWindow(), focusedIdentity.flutterViewWindow());
        }

        private void assertFocusRoundTrip(
                NativeView view,
                NativeIdentity identity) throws Exception {
            view.focusNetBeansControl();
            await(view.label() + " kept native focus after a Swing control requested it",
                    STOP_TIMEOUT,
                    view::netBeansPhysicallyFocused);
            assertNetBeansFocusStable(view);

            view.clickCanvas();
            awaitRunnerFocused(view, identity);

            view.focusNetBeansControl();
            await(view.label() + " could not return focus from Canvas to NetBeans; "
                            + view.focusDiagnostic(),
                    STOP_TIMEOUT,
                    view::netBeansPhysicallyFocused);
            assertNetBeansFocusStable(view);
            assertTrue(view.label() + " focus round-trip terminated its healthy runner",
                    identity.process().isAlive());
        }

        private void assertPreviewPopupRoundTrip(
                NativeView view,
                NativeIdentity identity) throws Exception {
            PreviewSelection selection = view.previewSelection();
            assertTrue(view.label() + " preview fixture has fewer than two profiles",
                    selection.itemCount() >= 2);
            assertTrue(view.label() + " preview selection target did not change",
                    selection.initialIndex() != selection.targetIndex());
            assertFalse(view.label() + " preview target label did not change",
                    selection.initialLabel().equals(selection.targetLabel()));
            assertSameNativeGeneration(
                    view.label() + " changed native generation before Preview popup",
                    identity,
                    view.identity());

            view.openPreviewPopup();
            await(view.label() + " did not show a heavyweight Preview popup",
                    STOP_TIMEOUT,
                    () -> view.previewPopup(selection.targetIndex()).isPresent());
            PreviewPopup popup = view.previewPopup(selection.targetIndex())
                    .orElseThrow(() -> new AssertionError(
                            view.label() + " heavyweight Preview popup disappeared"));
            Rectangle canvasBounds = view.surfaceScreenBounds();
            Rectangle overlap = popup.windowBounds().intersection(canvasBounds);
            assertTrue(view.label()
                            + " Preview popup did not physically overlap the embedded "
                            + "FlutterView; popup=" + popup.windowBounds()
                            + ", Canvas=" + canvasBounds,
                    overlap.width > 0 && overlap.height > 0);
            assertSameNativeGeneration(
                    view.label() + " Preview popup replaced the native generation",
                    identity,
                    view.identity());
            assertFalse(view.label() + " Preview popup exposed Retry",
                    view.retryAvailable());

            view.clickPreviewItem(popup);
            await(view.label() + " did not select Preview profile "
                            + selection.targetLabel(),
                    STOP_TIMEOUT,
                    () -> view.previewSelectedIndex() == selection.targetIndex());
            await(view.label() + " heavyweight Preview popup remained visible after selection",
                    STOP_TIMEOUT,
                    () -> view.previewPopupClosed(popup));
            awaitRendered(view, START_TIMEOUT);
            await(view.label() + " Preview profile change did not restore input sync",
                    STOP_TIMEOUT,
                    view::inputSynchronized);

            NativeIdentity afterSelection = view.identity();
            assertSameNativeGeneration(
                    view.label() + " Preview profile change replaced the native generation",
                    identity,
                    afterSelection);
            assertNativeIdentity(
                    view.label() + " after Preview profile change",
                    afterSelection);
            assertExactSurfaceMetrics(
                    view.label() + " after Preview profile change",
                    afterSelection,
                    view.surfaceMetrics());
            assertFalse(view.label() + " Preview profile change exposed Retry",
                    view.retryAvailable());

            view.clickCanvas();
            awaitRunnerFocused(view, identity);
            assertTrue(view.label() + " runner exited during Preview popup round-trip",
                    identity.process().isAlive());
        }

        private void assertNetBeansMainMenuPopupRoundTrip(
                NativeView view,
                NativeIdentity identity) throws Exception {
            assertSameNativeGeneration(
                    view.label() + " changed native generation before main-menu popup",
                    identity,
                    view.identity());

            NativeSurfaceMetrics beforeLayout = view.surfaceMetrics();
            if (view.prepareMainMenuPopupOverlap()) {
                await(view.label() + " main-menu overlap layout did not converge; "
                                + view.diagnostic(identity),
                        STOP_TIMEOUT,
                        () -> exactResizedSurfaceReady(
                                view, identity, beforeLayout));
            }
            view.clickCanvas();
            awaitRunnerFocused(view, identity);
            await(view.label() + " did not restore exact FLUTTERVIEW focus after "
                            + "main-menu overlap preparation; "
                            + view.focusDiagnostic(),
                    STOP_TIMEOUT,
                    () -> view.exactFlutterViewPhysicallyFocused(identity));
            NativeSurfaceMetrics before = view.surfaceMetrics();
            assertExactSurfaceMetrics(
                    view.label() + " before main-menu popup",
                    identity,
                    before);

            MainMenuPopup popup = view.openOverlappingMainMenuPopup();
            Rectangle canvasBounds = view.surfaceScreenBounds();
            Rectangle popupOverlap = popup.popupBounds().intersection(canvasBounds);
            Rectangle itemOverlap = popup.itemBounds().intersection(canvasBounds);
            assertTrue(view.label() + " NetBeans " + popup.menuLabel()
                            + " menu popup did not overlap the embedded FlutterView; popup="
                            + popup.popupBounds() + ", Canvas=" + canvasBounds,
                    popupOverlap.width > 0 && popupOverlap.height > 0);
            assertTrue(view.label() + " NetBeans " + popup.menuLabel()
                            + " menu has no interactive item over the embedded FlutterView; item="
                            + popup.itemBounds() + ", Canvas=" + canvasBounds,
                    itemOverlap.width > 0 && itemOverlap.height > 0);
            assertSameNativeGeneration(
                    view.label() + " main-menu popup replaced the native generation",
                    identity,
                    view.identity());
            assertFalse(view.label() + " main-menu popup exposed Retry",
                    view.retryAvailable());

            view.hoverMainMenuItem(popup);
            await(view.label() + " NetBeans " + popup.menuLabel()
                            + " menu did not receive physical pointer interaction at item "
                            + popup.itemLabel() + "; popupWindow="
                            + popup.popupWindowClass() + "; separatePopupWindow="
                            + popup.separatePopupWindow()
                            + "; lightWeightPopupEnabled="
                            + popup.lightWeightPopupEnabled(),
                    STOP_TIMEOUT,
                    () -> view.mainMenuInteractionReceived(popup));
            System.out.println("Accepted NetBeans main-menu popup interaction: menu="
                    + popup.menuLabel() + ", item=" + popup.itemLabel()
                    + ", popupWindow=" + popup.popupWindowClass()
                    + ", separatePopupWindow=" + popup.separatePopupWindow()
                    + ", lightWeightPopupEnabled="
                    + popup.lightWeightPopupEnabled());
            await(view.label() + " did not transfer physical focus away from Canvas "
                            + "while the NetBeans main menu was active; "
                            + view.focusDiagnostic(),
                    STOP_TIMEOUT,
                    () -> view.netBeansProcessOwnsPhysicalFocus(identity));

            view.dismissMainMenuPopup();
            await(view.label() + " NetBeans " + popup.menuLabel()
                            + " menu remained visible after Escape",
                    STOP_TIMEOUT,
                    () -> view.mainMenuPopupClosed(popup));
            awaitRendered(view, START_TIMEOUT);
            await(view.label() + " main-menu popup did not restore input sync",
                    STOP_TIMEOUT,
                    view::inputSynchronized);

            NativeIdentity afterPopup = view.identity();
            assertSameNativeGeneration(
                    view.label() + " main-menu round-trip replaced the native generation",
                    identity,
                    afterPopup);
            assertEquals(view.label() + " main-menu round-trip changed surface metrics",
                    before,
                    view.surfaceMetrics());
            assertNativeIdentity(
                    view.label() + " after main-menu popup round-trip",
                    afterPopup);
            assertExactSurfaceMetrics(
                    view.label() + " after main-menu popup round-trip",
                    afterPopup,
                    view.surfaceMetrics());
            assertFalse(view.label() + " main-menu round-trip exposed Retry",
                    view.retryAvailable());

            view.clickCanvas();
            awaitRunnerFocused(view, identity);
            await(view.label() + " did not restore exact FLUTTERVIEW focus after "
                            + "main-menu popup; " + view.focusDiagnostic(),
                    STOP_TIMEOUT,
                    () -> view.exactFlutterViewPhysicallyFocused(identity));
            assertTrue(view.label() + " runner exited during main-menu popup round-trip",
                    identity.process().isAlive());
        }

        private void assertNetBeansFocusStable(NativeView view) throws Exception {
            long deadline = System.nanoTime() + FOCUS_STABILITY_WINDOW.toNanos();
            do {
                drainEdt();
                assertTrue(view.label()
                                + " lost physical NetBeans focus inside the bounded "
                                + "Canvas retry window; "
                                + view.focusDiagnostic(),
                        view.netBeansPhysicallyFocused());
                Thread.sleep(25L);
            } while (System.nanoTime() < deadline);
        }

        private void awaitRendered(NativeView view, Duration timeout)
                throws Exception {
            await(view.label() + " did not reach a rendered native presentation",
                    timeout,
                    () -> RENDERED_STATUS.equals(view.statusText())
                            && view.hasAttachment());
        }

        private void assertIndependent(
                NativeIdentity first,
                NativeIdentity second) {
            assertTrue("Two Design MultiViews reused one Canvas process",
                    first.processId() != second.processId());
            assertTrue("Two Design MultiViews reused one AWT parent HWND",
                    first.parentWindow() != second.parentWindow());
            assertTrue("Two Design MultiViews reused one runner HWND",
                    first.runnerWindow() != second.runnerWindow());
            assertTrue("Two Design MultiViews reused one FLUTTERVIEW HWND",
                    first.flutterViewWindow() != second.flutterViewWindow());
        }

        private void assertNativeIdentity(String label, NativeIdentity identity) {
            assertTrue(label + " has no live Canvas process", identity.process().isAlive());
            assertTrue(label + " AWT parent HWND is not live",
                    isWindow(identity.parentWindow()));
            assertTrue(label + " runner HWND is not live",
                    isWindow(identity.runnerWindow()));
            assertTrue(label + " FLUTTERVIEW HWND is not live",
                    isWindow(identity.flutterViewWindow()));
            assertEquals(label + " AWT parent HWND belongs to another JVM",
                    ProcessHandle.current().pid(), ownerProcessId(identity.parentWindow()));
            assertEquals(label + " runner HWND belongs to another process",
                    identity.processId(), ownerProcessId(identity.runnerWindow()));
            assertEquals(label + " FLUTTERVIEW HWND belongs to another process",
                    identity.processId(), ownerProcessId(identity.flutterViewWindow()));
        }

        private void assertExactSurfaceMetrics(
                String label,
                NativeIdentity identity,
                NativeSurfaceMetrics metrics) {
            assertTrue(label + " surface width must be positive", metrics.width() > 0);
            assertTrue(label + " surface height must be positive", metrics.height() > 0);
            NativeWindowBounds parent = clientBounds(identity.parentWindow());
            NativeWindowBounds runner = clientBounds(identity.runnerWindow());
            NativeWindowBounds flutter = clientBounds(identity.flutterViewWindow());
            assertEquals(label + " runner client differs from its AWT parent client",
                    parent, runner);
            assertEquals(label + " FLUTTERVIEW client differs from its runner client",
                    runner, flutter);
            assertEquals(label + " published host width differs from native clients",
                    parent.width(), metrics.width());
            assertEquals(label + " published host height differs from native clients",
                    parent.height(), metrics.height());
            int dpi = windowDpi(identity.flutterViewWindow());
            assertTrue(label + " FlutterView HWND reported an invalid DPI", dpi > 0);
            int expectedDprMicros = Math.toIntExact((long) dpi * 1_000_000L / 96L);
            assertEquals(label + " published a DPR that differs from its exact HWND DPI",
                    expectedDprMicros, metrics.devicePixelRatioMicros());
            assertEquals(label + " AWT parent HWND is not per-monitor DPI aware",
                    DPI_AWARENESS_PER_MONITOR_AWARE,
                    windowDpiAwareness(identity.parentWindow()));
            assertEquals(label + " runner HWND is not per-monitor DPI aware",
                    DPI_AWARENESS_PER_MONITOR_AWARE,
                    windowDpiAwareness(identity.runnerWindow()));
            assertEquals(label + " FLUTTERVIEW HWND is not per-monitor DPI aware",
                    DPI_AWARENESS_PER_MONITOR_AWARE,
                    windowDpiAwareness(identity.flutterViewWindow()));
        }

        private void assertLiveResizeBurst(
                NativeView view,
                NativeIdentity identity) throws Exception {
            NativeSurfaceMetrics before = view.surfaceMetrics();
            assertTrue(view.label() + " runner exited before live resize",
                    identity.process().isAlive());

            int finalDivider = view.performDividerResizeBurst();
            await(view.label() + " live resize did not converge at divider "
                            + finalDivider + "; " + view.diagnostic(identity),
                    STOP_TIMEOUT,
                    () -> exactResizedSurfaceReady(view, identity, before));

            NativeIdentity after = view.identity();
            assertSameNativeGeneration(
                    view.label() + " live resize replaced its native generation",
                    identity,
                    after);
            NativeSurfaceMetrics settled = view.surfaceMetrics();
            assertFalse(view.label() + " divider burst did not change surface bounds",
                    before.width() == settled.width()
                    && before.height() == settled.height());
            assertExactSurfaceMetrics(
                    view.label() + " after live resize", after, settled);
            assertEquals(view.label() + " did not return to rendered state",
                    RENDERED_STATUS, view.statusText());
            assertTrue(view.label() + " interaction barrier did not return to idle",
                    view.inputSynchronized());
            assertTrue(view.label() + " runner exited during live resize",
                    identity.process().isAlive());

            view.clickCanvas();
            awaitRunnerFocused(view, identity);
        }

        private NativeIdentity assertSplitDocumentSurfaceLifecycle(
                NativeView primary,
                NativeIdentity initialPrimaryIdentity) throws Exception {
            NativeIdentity primaryIdentity = initialPrimaryIdentity;
            NativeIdentity retiredSplitIdentity = null;
            for (int cycle = 1; cycle <= 2; cycle++) {
                NativeView split = splitDesignView(primary, cycle);
                awaitRendered(primary, START_TIMEOUT);
                awaitRendered(split, START_TIMEOUT);

                NativeIdentity currentPrimary = primary.identity();
                if (!sameNativeGeneration(primaryIdentity, currentPrimary)) {
                    awaitRetiredIncludingParent(
                            primary.label() + " old generation during Split Document #"
                                    + cycle,
                            primaryIdentity);
                    primaryIdentity = currentPrimary;
                }
                NativeIdentity splitIdentity = split.identity();
                if (retiredSplitIdentity != null) {
                    assertTrue("Split Document reused its retired runner process in cycle "
                                    + cycle,
                            splitIdentity.processId()
                                    != retiredSplitIdentity.processId());
                }

                assertIndependent(primaryIdentity, splitIdentity);
                assertHealthyRenderedSurface(
                        primary.label() + " during Split Document #" + cycle,
                        primary,
                        primaryIdentity);
                assertHealthyRenderedSurface(
                        split.label() + " during Split Document #" + cycle,
                        split,
                        splitIdentity);

                primary.clickCanvas();
                awaitRunnerFocused(primary, primaryIdentity);
                assertTrue(split.label() + " exited while the other split surface focused",
                        splitIdentity.process().isAlive());
                split.clickCanvas();
                awaitRunnerFocused(split, splitIdentity);
                assertTrue(primary.label()
                                + " exited while the split surface focused",
                        primaryIdentity.process().isAlive());

                // Clear Split retains the active element. A Swing click in the
                // original half updates NetBeans' production split model; a
                // native child-window click alone cannot provide that AWT fact.
                primary.focusNetBeansControl();
                clearDesignSplit(primary, split, cycle);
                awaitRetiredIncludingParent(
                        split.label() + " after Clear Split #" + cycle,
                        splitIdentity);
                retiredSplitIdentity = splitIdentity;

                awaitRendered(primary, START_TIMEOUT);
                NativeIdentity afterClear = primary.identity();
                if (!sameNativeGeneration(primaryIdentity, afterClear)) {
                    awaitRetiredIncludingParent(
                            primary.label() + " replaced survivor generation after Clear Split #"
                                    + cycle,
                            primaryIdentity);
                    primaryIdentity = afterClear;
                }
                assertHealthyRenderedSurface(
                        primary.label() + " after Clear Split #" + cycle,
                        primary,
                        primaryIdentity);
                assertTrue(primary.label() + " MultiView closed during split churn",
                        primary.isOpened());
            }
            return primaryIdentity;
        }

        private void assertHealthyRenderedSurface(
                String label,
                NativeView view,
                NativeIdentity identity) throws Exception {
            assertNativeIdentity(label, identity);
            assertExactSurfaceMetrics(label, identity, view.surfaceMetrics());
            assertEquals(label + " is not rendered",
                    RENDERED_STATUS, view.statusText());
            assertTrue(label + " interaction barrier is not idle",
                    view.inputSynchronized());
            assertFalse(label + " unexpectedly exposes Retry",
                    view.retryAvailable());
        }

        private boolean exactResizedSurfaceReady(
                NativeView view,
                NativeIdentity expected,
                NativeSurfaceMetrics before) {
            try {
                NativeIdentity current = view.identity();
                if (!sameNativeGeneration(expected, current)
                        || !expected.process().isAlive()) {
                    return false;
                }
                NativeSurfaceMetrics metrics = view.surfaceMetrics();
                if (before.width() == metrics.width()
                        && before.height() == metrics.height()) {
                    return false;
                }
                NativeWindowBounds parent = clientBounds(current.parentWindow());
                NativeWindowBounds runner = clientBounds(current.runnerWindow());
                NativeWindowBounds flutter = clientBounds(
                        current.flutterViewWindow());
                return parent.equals(runner)
                        && runner.equals(flutter)
                        && parent.width() == metrics.width()
                        && parent.height() == metrics.height()
                        && RENDERED_STATUS.equals(view.statusText())
                        && view.inputSynchronized();
            } catch (Exception | AssertionError transientState) {
                return false;
            }
        }

        private void assertSameNativeGeneration(
                String message,
                NativeIdentity expected,
                NativeIdentity current) {
            assertTrue(message, sameNativeGeneration(expected, current));
        }

        private static boolean sameNativeGeneration(
                NativeIdentity expected,
                NativeIdentity current) {
            return expected.processId() == current.processId()
                    && expected.parentWindow() == current.parentWindow()
                    && expected.runnerWindow() == current.runnerWindow()
                    && expected.flutterViewWindow()
                            == current.flutterViewWindow();
        }

        private void awaitGone(String label, NativeIdentity identity) throws Exception {
            await(label + " process remained alive", STOP_TIMEOUT,
                    () -> !identity.process().isAlive());
            await(label + " runner HWND remained live", STOP_TIMEOUT,
                    () -> !isWindow(identity.runnerWindow()));
            await(label + " FLUTTERVIEW HWND remained live", STOP_TIMEOUT,
                    () -> !isWindow(identity.flutterViewWindow()));
        }

        private void awaitRetiredIncludingParent(
                String label,
                NativeIdentity identity) throws Exception {
            awaitGone(label, identity);
            await(label + " AWT parent HWND remained live", STOP_TIMEOUT,
                    () -> !isWindow(identity.parentWindow()));
        }

        private void awaitRetiredIncludingParentIfPresent(
                String label,
                NativeIdentity identity)
                throws Exception {
            if (identity != null) {
                awaitRetiredIncludingParent(label, identity);
            }
        }

        private void closeQuietly(NativeView view) throws Exception {
            if (view != null) {
                view.close();
            }
        }

        private void requestDesignVisible(CloneableTopComponent multiView) {
            MultiViewHandler handler = MultiViews.findMultiViewHandler(multiView);
            assertNotNull("The opened Designer has no MultiViewHandler", handler);
            MultiViewPerspective perspective = java.util.Arrays.stream(
                            handler.getPerspectives())
                    .filter(candidate -> DESIGN_PERSPECTIVE.equals(
                            candidate.preferredID()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "The opened Designer has no Design perspective"));
            handler.requestVisible(perspective);
            assertEquals("The requested Designer perspective did not become visible",
                    DESIGN_PERSPECTIVE,
                    handler.getSelectedPerspective().preferredID());
        }

        private void configureFlutterSdk(Path sdk) throws Exception {
            Class<?> settingsType = loader.loadClass(
                    "dev.flutter.netbeans.plugin.settings.FlutterSettings");
            Class<?> configType = loader.loadClass(
                    "dev.flutter.netbeans.plugin.settings.FlutterToolchainConfig");
            Object settings = settingsType.getMethod("getDefault").invoke(null);
            Object config = configType
                    .getConstructor(String.class, boolean.class, String.class)
                    .newInstance(sdk.toAbsolutePath().normalize().toString(), true, "");
            settingsType.getMethod("save", configType).invoke(settings, config);
        }

        private void provisionDefaultProjectTheme(Path projectRoot) throws Exception {
            Class<?> provisionerType = loader.loadClass(
                    "dev.flutter.netbeans.project.theme.FlutterProjectThemeProvisioner");
            Object provisioner = provisionerType.getConstructor().newInstance();
            try {
                provisionerType.getMethod("createDefaultIfMissing", Path.class)
                        .invoke(provisioner, projectRoot);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                throw new AssertionError(
                        "Could not provision the real project theme for " + projectRoot
                        + ": " + (cause == null ? failure : cause),
                        cause == null ? failure : cause);
            }
            FileUtil.refreshFor(projectRoot.toFile());
            assertTrue("Theme provisioning did not create project.fdtheme",
                    Files.isRegularFile(projectRoot.resolve(
                            ".fd_templates/project.fdtheme")));
            assertTrue("Theme provisioning did not create app_theme.dart",
                    Files.isRegularFile(projectRoot.resolve(
                            "lib/theme/app_theme.dart")));
            String main = Files.readString(
                    projectRoot.resolve("lib/main.dart"), StandardCharsets.UTF_8);
            assertTrue("Theme provisioning did not wire the generated theme into main.dart",
                    main.contains("import 'theme/app_theme.dart';")
                    && main.contains("theme: AppTheme.light")
                    && main.contains("darkTheme: AppTheme.dark")
                    && main.contains("themeMode: AppTheme.mode"));
        }

        private Pair createPair(Path directory, String baseName) throws Exception {
            FileObject dart = createFile(directory.resolve("lib/" + baseName + ".dart"), """
                    // <netbeans-flutter-designer region="imports">
                    import 'package:flutter/widgets.dart';
                    // </netbeans-flutter-designer>

                    class SampleView extends StatelessWidget {
                      const SampleView({super.key});

                      // <netbeans-flutter-designer region="build">
                      @override
                      Widget build(BuildContext context) {
                        return const SizedBox();
                      }
                      // </netbeans-flutter-designer>
                    }
                    """);
            FileObject fd = createFile(directory.resolve(
                    ".fd_templates/" + baseName + ".fd"), """
                    {
                      "format": "netbeans-flutter-designer",
                      "schemaVersion": 12,
                      "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                      "source": {
                        "dartFile": "%s.dart",
                        "className": "SampleView",
                        "widgetKind": "stateless",
                        "managedRegions": {
                          "imports": {
                            "sha256": "2FC35DE54B0A58A3211FDB1CAC2ABE866DD32B3279FE2E5EF1C3282848D85BFB"
                          },
                          "build": {
                            "sha256": "57D0BB0F067B9DC678CD4DF00243350043286785B7B88DE1C247E10641DFB8F5"
                          }
                        }
                      },
                      "root": {
                        "id": "35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce",
                        "type": "flutter.widgets.SizedBox",
                        "properties": {},
                        "slots": {}
                      }
                    }
                    """.formatted(baseName));
            return new Pair(dart, fd);
        }

        private Path createFlutterProject(Path root) throws Exception {
            Files.createDirectories(root.resolve("lib"));
            Files.createDirectories(root.resolve(".fd_templates"));
            Files.createDirectories(root.resolve(".dart_tool"));
            Files.createDirectories(root.resolve("android"));
            Files.createDirectories(root.resolve("windows"));
            Files.writeString(root.resolve("pubspec.yaml"), """
                    name: native_canvas_runtime_app
                    environment:
                      sdk: '>=3.0.0 <4.0.0'
                    dependencies:
                      flutter:
                        sdk: flutter
                    """, StandardCharsets.UTF_8);
            Files.writeString(root.resolve("lib/main.dart"), """
                    import 'package:flutter/material.dart';

                    void main() {
                      runApp(const NativeCanvasGateApp());
                    }

                    class NativeCanvasGateApp extends StatelessWidget {
                      const NativeCanvasGateApp({super.key});

                      @override
                      Widget build(BuildContext context) {
                        return MaterialApp(
                          title: 'Native Canvas Runtime Gate',
                          theme: ThemeData(
                            colorScheme: .fromSeed(seedColor: Colors.deepPurple),
                          ),
                          home: const Scaffold(
                            body: Center(child: Text('Native Canvas Runtime Gate')),
                          ),
                        );
                      }
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(root.resolve(".dart_tool/package_config.json"), """
                    {
                      "configVersion": 2,
                      "packages": [
                        {
                          "name": "native_canvas_runtime_app",
                          "rootUri": "../",
                          "packageUri": "lib/",
                          "languageVersion": "3.0"
                        }
                      ]
                    }
                    """, StandardCharsets.UTF_8);
            FileUtil.refreshFor(root.toFile());
            FileObject projectDirectory = FileUtil.toFileObject(root.toFile());
            assertNotNull("No FileObject for Flutter project " + root, projectDirectory);
            assertNotNull("Runtime fixture was not recognized as a Flutter project",
                    ProjectManager.getDefault().findProject(projectDirectory));
            return root;
        }

        private FileObject createFile(Path path, String content) throws Exception {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            FileUtil.refreshFor(path.toFile());
            FileObject file = FileUtil.toFileObject(path.toFile());
            assertNotNull("No FileObject for " + path, file);
            return file;
        }

        private ModuleInfo flutterModule() {
            Collection<? extends ModuleInfo> modules =
                    Lookup.getDefault().lookupAll(ModuleInfo.class);
            ModuleInfo module = modules.stream()
                    .filter(candidate -> FLUTTER_MODULE.equals(
                            candidate.getCodeNameBase()))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Flutter module is absent; available modules: "
                    + modules.stream()
                            .map(ModuleInfo::getCodeNameBase)
                            .sorted()
                            .collect(Collectors.joining(", ")), module);
            assertTrue("Flutter module must be enabled", module.isEnabled());
            return module;
        }

        private void initializeNativeWindowProbe() throws Exception {
            Class<?> type = loader.loadClass(
                    "dev.flutter.netbeans.plugin.designer.canvas.JnaWindowsNativeCanvasApi");
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            nativeApi = constructor.newInstance();
            nativeIsWindow = declaredMethod(type, "isWindow", long.class);
            nativeOwnerProcessId = declaredMethod(
                    type, "ownerProcessId", long.class);
            nativeClientBounds = declaredMethod(type, "clientBounds", long.class);
            nativeWindowDpi = declaredMethod(type, "windowDpi", long.class);
            nativeForegroundFocusedWindow = declaredMethod(
                    type, "foregroundFocusedWindow");

            Class<?> nativeLibraryType = loader.loadClass("com.sun.jna.NativeLibrary");
            Class<?> functionType = loader.loadClass("com.sun.jna.Function");
            Class<?> pointerType = loader.loadClass("com.sun.jna.Pointer");
            Class<?> pointByValueType = loader.loadClass(
                    "com.sun.jna.platform.win32.WinDef$POINT$ByValue");
            Object user32 = nativeLibraryType
                    .getMethod("getInstance", String.class, ClassLoader.class)
                    .invoke(null, "user32", loader);
            int alternateConvention = functionType
                    .getField("ALT_CONVENTION")
                    .getInt(null);
            Method getFunction = nativeLibraryType.getMethod(
                    "getFunction", String.class, int.class);
            getWindowDpiAwarenessContext = getFunction.invoke(
                    user32, "GetWindowDpiAwarenessContext", alternateConvention);
            getAwarenessFromDpiAwarenessContext = getFunction.invoke(
                    user32,
                    "GetAwarenessFromDpiAwarenessContext",
                    alternateConvention);
            getAncestorFunction = getFunction.invoke(
                    user32, "GetAncestor", alternateConvention);
            isIconicFunction = getFunction.invoke(
                    user32, "IsIconic", alternateConvention);
            setWindowPosFunction = getFunction.invoke(
                    user32, "SetWindowPos", alternateConvention);
            windowFromPointFunction = getFunction.invoke(
                    user32, "WindowFromPoint", alternateConvention);
            jnaPointerConstructor = pointerType.getConstructor(long.class);
            jnaPointByValueConstructor = pointByValueType.getConstructor(
                    int.class, int.class);
            jnaPointerNativeValue = pointerType.getMethod(
                    "nativeValue", pointerType);
            jnaInvokePointer = functionType.getMethod("invokePointer", Object[].class);
            jnaInvokeInt = functionType.getMethod("invokeInt", Object[].class);
        }

        private boolean isWindow(long window) {
            try {
                return (boolean) nativeIsWindow.invoke(nativeApi, window);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("Cannot query Win32 HWND " + window, failure);
            }
        }

        private long ownerProcessId(long window) {
            try {
                return (long) nativeOwnerProcessId.invoke(nativeApi, window);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot query owner PID for Win32 HWND " + window,
                        failure);
            }
        }

        private NativeWindowBounds clientBounds(long window) {
            try {
                Object bounds = nativeClientBounds.invoke(nativeApi, window);
                return new NativeWindowBounds(
                        invokeIntValue(bounds, "width"),
                        invokeIntValue(bounds, "height"));
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot query client bounds for Win32 HWND " + window,
                        failure);
            }
        }

        private int windowDpi(long window) {
            try {
                return (int) nativeWindowDpi.invoke(nativeApi, window);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot query DPI for Win32 HWND " + window,
                        failure);
            }
        }

        private int windowDpiAwareness(long window) {
            try {
                Object hwnd = jnaPointerConstructor.newInstance(window);
                Object awarenessContext = jnaInvokePointer.invoke(
                        getWindowDpiAwarenessContext,
                        (Object) new Object[] {hwnd});
                if (awarenessContext == null) {
                    throw new AssertionError(
                            "GetWindowDpiAwarenessContext returned null for HWND "
                            + window);
                }
                int awareness = (int) jnaInvokeInt.invoke(
                        getAwarenessFromDpiAwarenessContext,
                        (Object) new Object[] {awarenessContext});
                if (awareness < 0) {
                    throw new AssertionError(
                            "GetAwarenessFromDpiAwarenessContext returned "
                            + awareness + " for HWND " + window);
                }
                return awareness;
            } catch (InvocationTargetException failure) {
                throw new AssertionError(
                        "Cannot query actual DPI-awareness for Win32 HWND " + window,
                        failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot query actual DPI-awareness for Win32 HWND " + window,
                        failure);
            }
        }

        private long rootWindow(long window) {
            try {
                Object hwnd = jnaPointerConstructor.newInstance(window);
                Object root = jnaInvokePointer.invoke(
                        getAncestorFunction,
                        (Object) new Object[] {hwnd, GA_ROOT});
                return nativePointerValue(root);
            } catch (InvocationTargetException failure) {
                throw new AssertionError(
                        "Cannot query GA_ROOT for Win32 HWND " + window,
                        failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot query GA_ROOT for Win32 HWND " + window,
                        failure);
            }
        }

        private boolean isIconic(long window) {
            try {
                Object hwnd = jnaPointerConstructor.newInstance(window);
                int result = (int) jnaInvokeInt.invoke(
                        isIconicFunction,
                        (Object) new Object[] {hwnd});
                return result != 0;
            } catch (InvocationTargetException failure) {
                throw new AssertionError(
                        "Cannot query minimized state for root HWND " + window,
                        failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot query minimized state for root HWND " + window,
                        failure);
            }
        }

        private long windowAtPoint(Point point) {
            try {
                Object nativePoint = jnaPointByValueConstructor.newInstance(
                        point.x, point.y);
                Object window = jnaInvokePointer.invoke(
                        windowFromPointFunction,
                        (Object) new Object[] {nativePoint});
                return nativePointerValue(window);
            } catch (InvocationTargetException failure) {
                throw new AssertionError(
                        "Cannot hit-test Win32 screen point " + point,
                        failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot hit-test Win32 screen point " + point,
                        failure);
            }
        }

        private long nativePointerValue(Object pointer)
                throws ReflectiveOperationException {
            if (pointer == null) {
                return 0L;
            }
            return ((Number) jnaPointerNativeValue.invoke(null, pointer))
                    .longValue();
        }

        private boolean raiseRootWindowWithoutActivation(long window) {
            try {
                Object hwnd = jnaPointerConstructor.newInstance(window);
                int result = (int) jnaInvokeInt.invoke(
                        setWindowPosFunction,
                        (Object) new Object[] {
                            hwnd,
                            null, // HWND_TOP
                            0,
                            0,
                            0,
                            0,
                            TEST_ROOT_Z_ORDER_FLAGS
                        });
                return result != 0;
            } catch (InvocationTargetException failure) {
                throw new AssertionError(
                        "Cannot raise root HWND " + window
                                + " without activation through SetWindowPos",
                        failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot raise root HWND " + window
                                + " without activation through SetWindowPos",
                        failure);
            }
        }

        private long foregroundFocusedWindow() {
            try {
                return (long) nativeForegroundFocusedWindow.invoke(nativeApi);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(
                        "Cannot query the foreground GUI thread focus HWND",
                        failure);
            }
        }

        private static Method declaredMethod(
                Class<?> type,
                String name,
                Class<?>... parameters) throws Exception {
            Method method = type.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        }

        private static int invokeIntValue(Object value, String methodName)
                throws ReflectiveOperationException {
            Method method = value.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (int) method.invoke(value);
        }

        private static String accessibleName(Component component) {
            AccessibleContext accessible = component.getAccessibleContext();
            return accessible == null ? null : accessible.getAccessibleName();
        }

        private static String accessibleDescription(Component component) {
            AccessibleContext accessible = component.getAccessibleContext();
            return accessible == null ? null : accessible.getAccessibleDescription();
        }

        private static Component findClassComponentOrNull(
                Component root,
                String className) {
            if (className.equals(root.getClass().getName())) {
                return root;
            }
            if (root instanceof Container container) {
                for (Component child : container.getComponents()) {
                    Component found = findClassComponentOrNull(child, className);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }

        private int nativeHostCount(CloneableTopComponent multiView) {
            try {
                return onEdtValue(() -> nativeHosts(multiView).size());
            } catch (Exception failure) {
                throw new AssertionError(
                        "Cannot inspect native hosts inside the Designer MultiView",
                        failure);
            }
        }

        private static List<Component> nativeHosts(Component root) {
            List<Component> hosts = new ArrayList<>();
            collectClassComponents(root, HOST_CLASS, hosts);
            return List.copyOf(hosts);
        }

        private static void collectClassComponents(
                Component root,
                String className,
                List<Component> result) {
            if (className.equals(root.getClass().getName())) {
                result.add(root);
            }
            if (root instanceof Container container) {
                for (Component child : container.getComponents()) {
                    collectClassComponents(child, className, result);
                }
            }
        }

        private static Component singleNativeViewRoot(
                Component multiView,
                Component host) {
            Component result = host;
            Container ancestor = host.getParent();
            while (ancestor != null && ancestor != multiView) {
                if (nativeHosts(ancestor).size() != 1) {
                    break;
                }
                result = ancestor;
                ancestor = ancestor.getParent();
            }
            return result;
        }

        private static <T extends Component> T findAccessibleComponentOrNull(
                Component root,
                Class<T> type,
                String expectedAccessibleName) {
            if (type.isInstance(root)
                    && expectedAccessibleName.equals(accessibleName(root))) {
                return type.cast(root);
            }
            if (root instanceof Container container) {
                for (Component child : container.getComponents()) {
                    T found = findAccessibleComponentOrNull(
                            child, type, expectedAccessibleName);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }

        private static <T extends Component> T findTypeComponentOrNull(
                Component root,
                Class<T> type) {
            if (type.isInstance(root)) {
                return type.cast(root);
            }
            if (root instanceof Container container) {
                for (Component child : container.getComponents()) {
                    T found = findTypeComponentOrNull(child, type);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }

        private static Rectangle componentScreenBounds(Component component) {
            Point location = component.getLocationOnScreen();
            return new Rectangle(
                    location.x,
                    location.y,
                    component.getWidth(),
                    component.getHeight());
        }

        private static boolean physicallyOverlaps(
                Rectangle first,
                Rectangle second) {
            Rectangle overlap = first.intersection(second);
            return overlap.width > 0 && overlap.height > 0;
        }

        private static boolean menuLabelEquals(String expected, String actual) {
            String normalized = actual == null
                    ? ""
                    : actual.replace("&", "").trim();
            return expected.equalsIgnoreCase(normalized);
        }

        private static JSplitPane findResizeSplit(Component host) {
            Container ancestor = host.getParent();
            while (ancestor != null) {
                if (ancestor instanceof JSplitPane split) {
                    Component right = split.getRightComponent();
                    if (right == host
                            || (right != null
                            && SwingUtilities.isDescendingFrom(host, right))) {
                        return split;
                    }
                }
                ancestor = ancestor.getParent();
            }
            return null;
        }

        private static void await(
                String message,
                Duration timeout,
                BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + timeout.toNanos();
            do {
                drainEdt();
                if (condition.getAsBoolean()) {
                    return;
                }
                Thread.sleep(25);
            } while (System.nanoTime() < deadline);
            fail(message);
        }

        private static boolean waitUntil(
                Duration timeout,
                BooleanSupplier condition) throws Exception {
            long deadline = System.nanoTime() + timeout.toNanos();
            do {
                drainEdt();
                if (condition.getAsBoolean()) {
                    return true;
                }
                Thread.sleep(25);
            } while (System.nanoTime() < deadline);
            return false;
        }

        private static void drainEdt() throws Exception {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> { });
            }
        }

        private static void onEdt(ThrowingRunnable action) throws Exception {
            onEdtValue(() -> {
                action.run();
                return null;
            });
        }

        private static <T> T onEdtValue(ThrowingSupplier<T> action)
                throws Exception {
            if (SwingUtilities.isEventDispatchThread()) {
                return action.get();
            }
            Box<T> result = new Box<>();
            Box<Throwable> failure = new Box<>();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.value = action.get();
                } catch (Throwable thrown) {
                    failure.value = thrown;
                }
            });
            if (failure.value instanceof Exception exception) {
                throw exception;
            }
            if (failure.value instanceof Error error) {
                throw error;
            }
            if (failure.value != null) {
                throw new AssertionError(failure.value);
            }
            return result.value;
        }

        @FunctionalInterface
        private interface ThrowingRunnable {
            void run() throws Exception;
        }

        @FunctionalInterface
        private interface ThrowingSupplier<T> {
            T get() throws Exception;
        }

        private static final class Box<T> {
            private volatile T value;
        }

        private final class NativeView {
            private final String label;
            private final CloneableTopComponent multiView;
            private final Component host;
            private final JLabel status;
            private final JButton retry;
            private final java.awt.Canvas surface;
            private final JComboBox<?> previewTarget;
            private final JTree netBeansFocusTarget;
            private final JLabel inputSyncStatus;
            private final JSplitPane resizeSplit;
            private final ConcurrentLinkedQueue<String> lifecycleEvents =
                    new ConcurrentLinkedQueue<>();
            private volatile boolean closed;

            private NativeView(
                    String label,
                    CloneableTopComponent multiView,
                    Component host,
                    JLabel status,
                    JButton retry,
                    java.awt.Canvas surface,
                    JComboBox<?> previewTarget,
                    JTree netBeansFocusTarget,
                    JLabel inputSyncStatus,
                    JSplitPane resizeSplit) {
                this.label = label;
                this.multiView = multiView;
                this.host = host;
                this.status = status;
                this.retry = retry;
                this.surface = surface;
                this.previewTarget = previewTarget;
                this.netBeansFocusTarget = netBeansFocusTarget;
                this.inputSyncStatus = inputSyncStatus;
                this.resizeSplit = resizeSplit;
                lifecycleEvents.add("created(showing=" + multiView.isShowing() + ")");
                multiView.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentShown(ComponentEvent event) {
                        lifecycleEvents.add("shown");
                    }

                    @Override
                    public void componentHidden(ComponentEvent event) {
                        lifecycleEvents.add("hidden");
                    }
                });
            }

            private String label() {
                return label;
            }

            private CloneableTopComponent multiView() {
                return multiView;
            }

            private Component host() {
                return host;
            }

            private boolean isOpened() {
                try {
                    return onEdtValue(multiView::isOpened);
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " open state cannot be read", failure);
                }
            }

            private boolean hasAttachment() {
                try {
                    return onEdtValue(() -> ((Optional<?>) host.getClass()
                            .getMethod("attachment")
                            .invoke(host)).isPresent());
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " native attachment could not be inspected",
                            failure);
                }
            }

            private NativeIdentity identity() throws Exception {
                return onEdtValue(() -> {
                    Optional<?> current = (Optional<?>) host.getClass()
                            .getMethod("attachment")
                            .invoke(host);
                    assertTrue(label + " has no verified native attachment",
                            current.isPresent());
                    Object attachment = current.orElseThrow();
                    long parent = invokeLongRecord(attachment, "parentWindow");
                    long runner = invokeLongRecord(attachment, "runnerWindow");
                    long flutter = invokeLongRecord(attachment, "flutterViewWindow");
                    long processId = invokeLongRecord(
                            attachment, "runnerProcessId");
                    ProcessHandle process = ProcessHandle.of(processId)
                            .orElseThrow(() -> new AssertionError(
                                    label + " attachment names missing process "
                                    + processId));
                    return new NativeIdentity(
                            process, parent, runner, flutter, processId);
                });
            }

            private NativeSurfaceMetrics surfaceMetrics() throws Exception {
                return onEdtValue(() -> {
                    Optional<?> current = (Optional<?>) host.getClass()
                            .getMethod("surfaceMetrics")
                            .invoke(host);
                    assertTrue(label + " has no native surface metrics",
                            current.isPresent());
                    Object metrics = current.orElseThrow();
                    return new NativeSurfaceMetrics(
                            invokeIntRecord(metrics, "width"),
                            invokeIntRecord(metrics, "height"),
                            invokeIntRecord(metrics, "devicePixelRatioMicros"));
                });
            }

            private PreviewSelection previewSelection() throws Exception {
                return onEdtValue(() -> {
                    int itemCount = previewTarget.getItemCount();
                    int initialIndex = previewTarget.getSelectedIndex();
                    assertTrue(label + " Preview has no selected profile",
                            initialIndex >= 0 && initialIndex < itemCount);
                    int targetIndex = initialIndex + 1 < itemCount
                            ? initialIndex + 1
                            : initialIndex - 1;
                    String initialLabel = String.valueOf(
                            previewTarget.getItemAt(initialIndex));
                    String targetLabel = targetIndex >= 0
                            ? String.valueOf(previewTarget.getItemAt(targetIndex))
                            : "<none>";
                    return new PreviewSelection(
                            itemCount,
                            initialIndex,
                            targetIndex,
                            initialLabel,
                            targetLabel);
                });
            }

            private void openPreviewPopup() throws Exception {
                Point point = onEdtValue(() -> {
                    assertTrue(label + " Preview selector is not showing",
                            previewTarget.isShowing());
                    assertTrue(label + " Preview selector is not enabled",
                            previewTarget.isEnabled());
                    Point location = previewTarget.getLocationOnScreen();
                    location.translate(
                            Math.max(1, previewTarget.getWidth() - 8),
                            Math.max(1, previewTarget.getHeight() / 2));
                    return location;
                });
                clickScreenPoint(point);
                drainEdt();
            }

            private Optional<PreviewPopup> previewPopup(int targetIndex) {
                try {
                    return onEdtValue(() -> {
                        if (!previewTarget.isPopupVisible()) {
                            return Optional.empty();
                        }
                        Window owner = SwingUtilities.getWindowAncestor(previewTarget);
                        for (Window window : Window.getWindows()) {
                            if (window == owner || !window.isShowing()) {
                                continue;
                            }
                            JList<?> list = findTypeComponentOrNull(window, JList.class);
                            if (list == null
                                    || !list.isShowing()
                                    || !previewPopupListMatches(list)
                                    || targetIndex < 0
                                    || targetIndex >= list.getModel().getSize()) {
                                continue;
                            }
                            Rectangle cell = list.getCellBounds(
                                    targetIndex, targetIndex);
                            if (cell == null) {
                                continue;
                            }
                            Point location = list.getLocationOnScreen();
                            Point target = new Point(
                                    location.x + cell.x
                                    + Math.max(1, cell.width / 2),
                                    location.y + cell.y
                                    + Math.max(1, cell.height / 2));
                            return Optional.of(new PreviewPopup(
                                    window,
                                    new Rectangle(window.getBounds()),
                                    target));
                        }
                        return Optional.empty();
                    });
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " heavyweight Preview popup cannot be inspected",
                            failure);
                }
            }

            private boolean previewPopupListMatches(JList<?> list) {
                if (list.getModel().getSize() != previewTarget.getItemCount()) {
                    return false;
                }
                for (int index = 0; index < previewTarget.getItemCount(); index++) {
                    if (!Objects.equals(
                            list.getModel().getElementAt(index),
                            previewTarget.getItemAt(index))) {
                        return false;
                    }
                }
                return true;
            }

            private void clickPreviewItem(PreviewPopup popup) throws Exception {
                clickScreenPoint(popup.targetPoint());
                drainEdt();
            }

            private int previewSelectedIndex() {
                try {
                    return onEdtValue(previewTarget::getSelectedIndex);
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " Preview selection cannot be read", failure);
                }
            }

            private boolean previewPopupClosed(PreviewPopup popup) {
                try {
                    return onEdtValue(() -> !previewTarget.isPopupVisible()
                            && !popup.window().isShowing());
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " Preview popup close state cannot be read",
                            failure);
                }
            }

            private boolean prepareMainMenuPopupOverlap() throws Exception {
                JMenu menu = acceptedMainMenu();
                clickMainMenu(menu);
                assertTrue(label + " NetBeans " + ACCEPTED_MAIN_MENU
                                + " menu did not open for overlap layout preparation",
                        waitUntil(Duration.ofSeconds(3),
                                () -> mainMenuPopupShowing(menu)));

                MainMenuGeometry geometry = mainMenuGeometry(menu);
                Rectangle canvasBounds = surfaceScreenBounds();
                dismissMainMenuPopup();
                assertTrue(label + " could not close " + ACCEPTED_MAIN_MENU
                                + " menu after overlap layout preparation",
                        waitUntil(Duration.ofSeconds(3),
                                () -> !mainMenuPopupShowing(menu)));

                int requiredOverlap = Math.min(
                        MAIN_MENU_CANVAS_OVERLAP_PIXELS,
                        geometry.itemBounds().width);
                Rectangle currentItemOverlap = geometry.itemBounds()
                        .intersection(canvasBounds);
                if (physicallyOverlaps(geometry.popupBounds(), canvasBounds)
                        && currentItemOverlap.width >= requiredOverlap
                        && currentItemOverlap.height > 0) {
                    return false;
                }

                int verticalOverlap = Math.min(
                        geometry.itemBounds().y + geometry.itemBounds().height,
                        canvasBounds.y + canvasBounds.height)
                        - Math.max(geometry.itemBounds().y, canvasBounds.y);
                assertTrue(label + " NetBeans " + ACCEPTED_MAIN_MENU + " → "
                                + ACCEPTED_MAIN_MENU_ITEM
                                + " cannot overlap the Canvas vertically; item="
                                + geometry.itemBounds() + ", Canvas=" + canvasBounds,
                        verticalOverlap > 0);

                int desiredCanvasX = geometry.itemBounds().x
                        + geometry.itemBounds().width
                        - requiredOverlap;
                int requiredShift = canvasBounds.x - desiredCanvasX;
                assertTrue(label + " main-menu overlap requires moving the Canvas "
                                + "right, which the production right-hand Canvas split "
                                + "cannot provide; item=" + geometry.itemBounds()
                                + ", Canvas=" + canvasBounds,
                        requiredShift > 0);
                shiftCanvasDividerLeft(requiredShift);
                await(label + " could not prepare a deterministic "
                                + MAIN_MENU_CANVAS_OVERLAP_PIXELS
                                + "px overlap for NetBeans " + ACCEPTED_MAIN_MENU
                                + " → " + ACCEPTED_MAIN_MENU_ITEM + "; item="
                                + geometry.itemBounds(),
                        STOP_TIMEOUT,
                        () -> {
                            try {
                                Rectangle shiftedCanvas = surfaceScreenBounds();
                                Rectangle shiftedItemOverlap = geometry.itemBounds()
                                        .intersection(shiftedCanvas);
                                return physicallyOverlaps(
                                                geometry.popupBounds(), shiftedCanvas)
                                        && shiftedItemOverlap.width >= requiredOverlap
                                        && shiftedItemOverlap.height > 0;
                            } catch (Exception transientState) {
                                return false;
                            }
                        });
                return true;
            }

            private MainMenuPopup openOverlappingMainMenuPopup()
                    throws Exception {
                Rectangle canvasBounds = surfaceScreenBounds();
                JMenu menu = acceptedMainMenu();
                clickMainMenu(menu);
                assertTrue(label + " NetBeans " + ACCEPTED_MAIN_MENU
                                + " menu did not open",
                        waitUntil(Duration.ofSeconds(3),
                                () -> mainMenuPopupShowing(menu)));
                MainMenuGeometry geometry = mainMenuGeometry(menu);
                Rectangle popupOverlap = geometry.popupBounds()
                        .intersection(canvasBounds);
                Rectangle itemOverlap = geometry.itemBounds()
                        .intersection(canvasBounds);
                if (popupOverlap.width <= 0 || popupOverlap.height <= 0
                        || itemOverlap.width <= 0 || itemOverlap.height <= 0) {
                    String diagnostic = mainMenuPopupDiagnostic(
                            menu, ACCEPTED_MAIN_MENU);
                    dismissMainMenuPopup();
                    throw new AssertionError(label + " NetBeans "
                            + ACCEPTED_MAIN_MENU + " → " + ACCEPTED_MAIN_MENU_ITEM
                            + " did not physically overlap the embedded FlutterView "
                            + "after deterministic layout preparation; Canvas="
                            + canvasBounds + "; menu=" + diagnostic
                            + "; item=" + geometry.itemBounds());
                }
                Point target = new Point(
                        itemOverlap.x + Math.max(0, itemOverlap.width / 2),
                        itemOverlap.y + Math.max(0, itemOverlap.height / 2));
                return onEdtValue(() -> {
                    JPopupMenu popup = menu.getPopupMenu();
                    JMenuItem item = acceptedMainMenuItem(popup);
                    Window popupWindow = SwingUtilities.getWindowAncestor(popup);
                    assertNotNull(label + " showing main-menu popup has no Window",
                            popupWindow);
                    Window mainWindow = SwingUtilities.getWindowAncestor(multiView);
                    return new MainMenuPopup(
                            menu,
                            popup,
                            item,
                            ACCEPTED_MAIN_MENU,
                            ACCEPTED_MAIN_MENU_ITEM,
                            geometry.popupBounds(),
                            geometry.itemBounds(),
                            target,
                            popupWindow.getClass().getName(),
                            popupWindow != mainWindow,
                            popup.isLightWeightPopupEnabled());
                });
            }

            private JMenu acceptedMainMenu() throws Exception {
                return onEdtValue(() -> {
                    Window mainWindow = SwingUtilities.getWindowAncestor(multiView);
                    assertNotNull(label + " Designer has no NetBeans main window",
                            mainWindow);
                    JMenuBar menuBar = findTypeComponentOrNull(
                            mainWindow, JMenuBar.class);
                    assertNotNull(label + " NetBeans main window has no JMenuBar",
                            menuBar);
                    for (int index = 0; index < menuBar.getMenuCount(); index++) {
                        JMenu menu = menuBar.getMenu(index);
                        if (menu != null
                                && menu.isShowing()
                                && menu.isVisible()
                                && menu.isEnabled()
                                && menuLabelEquals(
                                        ACCEPTED_MAIN_MENU, menu.getText())) {
                            return menu;
                        }
                    }
                    throw new AssertionError(label + " has no enabled production "
                            + ACCEPTED_MAIN_MENU + " menu");
                });
            }

            private void clickMainMenu(JMenu menu) throws Exception {
                Point center = onEdtValue(() -> {
                    assertTrue(label + " main menu is not showing: " + menu.getText(),
                            menu.isShowing());
                    Point location = menu.getLocationOnScreen();
                    location.translate(
                            Math.max(1, menu.getWidth() / 2),
                            Math.max(1, menu.getHeight() / 2));
                    return location;
                });
                clickScreenPoint(center);
                drainEdt();
            }

            private boolean mainMenuPopupShowing(JMenu menu) {
                try {
                    return onEdtValue(() -> menu.isPopupMenuVisible()
                            && menu.getPopupMenu().isShowing());
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " main-menu popup state cannot be read", failure);
                }
            }

            private MainMenuGeometry mainMenuGeometry(JMenu menu)
                    throws Exception {
                return onEdtValue(() -> {
                    JPopupMenu popup = menu.getPopupMenu();
                    assertTrue(label + " NetBeans " + ACCEPTED_MAIN_MENU
                                    + " popup is not showing",
                            popup.isShowing());
                    JMenuItem item = acceptedMainMenuItem(popup);
                    return new MainMenuGeometry(
                            componentScreenBounds(popup),
                            componentScreenBounds(item));
                });
            }

            private JMenuItem acceptedMainMenuItem(JPopupMenu popup) {
                for (Component component : popup.getComponents()) {
                    if (component instanceof JMenuItem item
                            && !(component instanceof JMenu)
                            && item.isShowing()
                            && item.isEnabled()
                            && menuLabelEquals(
                                    ACCEPTED_MAIN_MENU_ITEM, item.getText())) {
                        return item;
                    }
                }
                throw new AssertionError(label + " NetBeans "
                        + ACCEPTED_MAIN_MENU + " menu has no enabled "
                        + ACCEPTED_MAIN_MENU_ITEM + " item");
            }

            private void shiftCanvasDividerLeft(int requiredShift)
                    throws Exception {
                onEdt(() -> {
                    assertTrue(label + " resize split is not showing",
                            resizeSplit.isShowing());
                    int current = resizeSplit.getDividerLocation();
                    int minimum = resizeSplit.getMinimumDividerLocation();
                    int availableShift = current - minimum;
                    assertTrue(label + " production divider cannot shift the Canvas "
                                    + requiredShift + "px left for main-menu overlap; "
                                    + "available=" + availableShift + "px, divider="
                                    + minimum + ".." + current,
                            availableShift >= requiredShift);
                    resizeSplit.setDividerLocation(current - requiredShift);
                    resizeSplit.revalidate();
                    Container top = resizeSplit.getTopLevelAncestor();
                    if (top != null) {
                        top.validate();
                    }
                });
                drainEdt();
            }

            private String mainMenuPopupDiagnostic(
                    JMenu menu,
                    String menuLabel) throws Exception {
                return onEdtValue(() -> {
                    JPopupMenu popup = menu.getPopupMenu();
                    if (!popup.isShowing()) {
                        return menuLabel + "=<closed before inspection>";
                    }
                    Window popupWindow = SwingUtilities.getWindowAncestor(popup);
                    return menuLabel + "={bounds=" + componentScreenBounds(popup)
                            + ", popupWindow="
                            + (popupWindow == null
                                    ? "<none>"
                                    : popupWindow.getClass().getName())
                            + ", lightWeightPopupEnabled="
                            + popup.isLightWeightPopupEnabled()
                            + "}";
                });
            }

            private void hoverMainMenuItem(MainMenuPopup popup) throws Exception {
                moveScreenPoint(popup.targetPoint());
                drainEdt();
            }

            private boolean mainMenuInteractionReceived(MainMenuPopup popup) {
                try {
                    return onEdtValue(() -> {
                        if (!popup.popup().isShowing()
                                || !popup.item().getModel().isArmed()) {
                            return false;
                        }
                        for (MenuElement selected
                                : MenuSelectionManager.defaultManager()
                                        .getSelectedPath()) {
                            if (selected.getComponent() == popup.item()) {
                                return true;
                            }
                        }
                        return false;
                    });
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " main-menu interaction state cannot be read",
                            failure);
                }
            }

            private void dismissMainMenuPopup() throws Exception {
                Robot robot = new Robot();
                robot.setAutoDelay(25);
                robot.keyPress(KeyEvent.VK_ESCAPE);
                robot.keyRelease(KeyEvent.VK_ESCAPE);
                robot.waitForIdle();
                drainEdt();
            }

            private boolean mainMenuPopupClosed(MainMenuPopup popup) {
                try {
                    return onEdtValue(() -> !popup.menu().isPopupMenuVisible()
                            && !popup.popup().isShowing());
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " main-menu close state cannot be read", failure);
                }
            }

            private boolean netBeansProcessOwnsPhysicalFocus(
                    NativeIdentity identity) {
                long focusedWindow = NativeCanvasRuntimeCase.this
                        .foregroundFocusedWindow();
                if (focusedWindow == 0L) {
                    return false;
                }
                long focusedOwner = ownerProcessId(focusedWindow);
                long awtParentOwner = ownerProcessId(identity.parentWindow());
                return focusedOwner == awtParentOwner
                        && focusedOwner != identity.processId();
            }

            private boolean exactFlutterViewPhysicallyFocused(
                    NativeIdentity identity) {
                return runnerFocused()
                        && NativeCanvasRuntimeCase.this.foregroundFocusedWindow()
                        == identity.flutterViewWindow();
            }

            private Rectangle surfaceScreenBounds() throws Exception {
                return onEdtValue(() -> {
                    Point location = surface.getLocationOnScreen();
                    return new Rectangle(
                            location.x,
                            location.y,
                            surface.getWidth(),
                            surface.getHeight());
                });
            }

            private int performDividerResizeBurst() throws Exception {
                int[] locations = onEdtValue(() -> {
                    assertTrue(label + " resize split is not showing",
                            resizeSplit.isShowing());
                    int minimum = resizeSplit.getMinimumDividerLocation();
                    int maximum = resizeSplit.getMaximumDividerLocation();
                    int span = maximum - minimum;
                    assertTrue(label + " resize split has no useful divider range: "
                                    + minimum + ".." + maximum,
                            span >= 160);
                    int initial = resizeSplit.getDividerLocation();
                    int low = minimum + span / 4;
                    int middle = minimum + span / 2;
                    int high = minimum + (span * 3) / 4;
                    int target = Math.abs(high - initial)
                            >= Math.abs(low - initial) ? high : low;
                    assertTrue(label + " final divider target is not a real resize",
                            Math.abs(target - initial) >= 32);
                    int alternate = target == high ? low : high;
                    return new int[]{alternate, middle, target, alternate, target};
                });
                for (int location : locations) {
                    onEdt(() -> {
                        resizeSplit.setDividerLocation(location);
                        resizeSplit.revalidate();
                        Container top = resizeSplit.getTopLevelAncestor();
                        if (top != null) {
                            top.validate();
                        }
                    });
                }
                lifecycleEvents.add("divider-resize-burst(final="
                        + locations[locations.length - 1] + ")");
                return locations[locations.length - 1];
            }

            private boolean inputSynchronized() {
                try {
                    return onEdtValue(() -> !inputSyncStatus.isVisible()
                            && inputSyncStatus.getToolTipText() == null
                            && accessibleDescription(inputSyncStatus) != null
                            && accessibleDescription(inputSyncStatus)
                                    .contains("synchronization is idle"));
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " input synchronization cannot be read",
                            failure);
                }
            }

            private boolean runnerFocused() {
                try {
                    return onEdtValue(() -> (boolean) host.getClass()
                            .getMethod("isRunnerFocused")
                            .invoke(host));
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " exact native focus state cannot be read",
                            failure);
                }
            }

            private void establishTestedIdeForeground(NativeIdentity identity)
                    throws Exception {
                onEdt(() -> {
                    Window window = SwingUtilities.getWindowAncestor(
                            netBeansFocusTarget);
                    assertNotNull(label + " has no assembled-runtime top-level window",
                            window);
                    assertTrue(label + " assembled-runtime top-level window is not showing",
                            window.isShowing());
                });
                drainEdt();

                long expectedOwner = ProcessHandle.current().pid();
                long targetOwner = ownerProcessId(identity.parentWindow());
                assertEquals(label + " assembled-runtime parent HWND belongs to "
                                + "another process",
                        expectedOwner, targetOwner);

                long rootWindow = NativeCanvasRuntimeCase.this
                        .rootWindow(identity.parentWindow());
                assertTrue(label + " GetAncestor(GA_ROOT) returned no HWND; "
                                + "targetParentHwnd=" + identity.parentWindow(),
                        rootWindow != 0L);
                assertTrue(label + " assembled-runtime root HWND is not live; "
                                + "targetParentHwnd=" + identity.parentWindow()
                                + "; targetRootHwnd=" + rootWindow,
                        isWindow(rootWindow));
                long rootOwner = ownerProcessId(rootWindow);
                assertEquals(label + " assembled-runtime root HWND belongs to "
                                + "another process; targetParentHwnd="
                                + identity.parentWindow() + "; targetRootHwnd="
                                + rootWindow,
                        expectedOwner, rootOwner);
                assertFalse(label + " assembled-runtime root HWND is minimized; "
                                + "targetRootHwnd=" + rootWindow,
                        NativeCanvasRuntimeCase.this.isIconic(rootWindow));

                long initialForegroundWindow = NativeCanvasRuntimeCase.this
                        .foregroundFocusedWindow();
                long initialForegroundOwner = initialForegroundWindow == 0L
                        ? 0L
                        : ownerProcessId(initialForegroundWindow);
                boolean startedBehindUnrelatedProcess =
                        initialForegroundOwner != targetOwner
                        && initialForegroundOwner != identity.processId();
                assertTrue(label + " could not raise the exact assembled-runtime "
                                + "root HWND to HWND_TOP without activation; "
                                + "targetParentHwnd=" + identity.parentWindow()
                                + "; targetRootHwnd=" + rootWindow
                                + "; targetOwnerPid=" + targetOwner
                                + "; rootOwnerPid=" + rootOwner
                                + "; initialForegroundHwnd="
                                + initialForegroundWindow
                                + "; initialForegroundOwnerPid="
                                + initialForegroundOwner
                                + "; runnerPid=" + identity.processId()
                                + "; flutterViewHwnd="
                                + identity.flutterViewWindow(),
                        NativeCanvasRuntimeCase.this
                                .raiseRootWindowWithoutActivation(rootWindow));
                assertTrue(label + " assembled-runtime root HWND retired during "
                                + "test-only z-order activation; targetRootHwnd="
                                + rootWindow,
                        isWindow(rootWindow));
                assertEquals(label + " assembled-runtime root HWND changed owner "
                                + "during test-only z-order activation; targetRootHwnd="
                                + rootWindow,
                        expectedOwner, ownerProcessId(rootWindow));
                assertFalse(label + " assembled-runtime root HWND became minimized "
                                + "during test-only z-order activation; targetRootHwnd="
                                + rootWindow,
                        NativeCanvasRuntimeCase.this.isIconic(rootWindow));
                assertStagingDidNotActivateTarget(
                        "immediately after SetWindowPos",
                        identity,
                        targetOwner,
                        startedBehindUnrelatedProcess,
                        initialForegroundWindow,
                        initialForegroundOwner);

                Point focusTarget = awaitSwingTargetExposed(
                        rootWindow, targetOwner, identity);
                assertStagingDidNotActivateTarget(
                        "after HWND_TOP exposure and before Robot click",
                        identity,
                        targetOwner,
                        startedBehindUnrelatedProcess,
                        initialForegroundWindow,
                        initialForegroundOwner);
                lifecycleEvents.add("test-root-z-order(rootHwnd=" + rootWindow
                        + ",targetPid=" + targetOwner
                        + ",runnerPid=" + identity.processId()
                        + ",flutterViewHwnd=" + identity.flutterViewWindow()
                        + ",focusTarget=" + focusTarget
                        + ",initialForegroundHwnd=" + initialForegroundWindow
                        + ",initialForegroundOwnerPid=" + initialForegroundOwner
                        + ")");

                focusNetBeansControl(focusTarget);
                await(label + " could not establish physical Swing focus before the "
                                + "first Canvas click; targetParentHwnd="
                                + identity.parentWindow() + "; targetRootHwnd="
                                + rootWindow + "; targetOwnerPid=" + targetOwner
                                + "; initialForegroundHwnd="
                                + initialForegroundWindow
                                + "; initialForegroundOwnerPid="
                                + initialForegroundOwner + "; " + focusDiagnostic(),
                        STOP_TIMEOUT,
                        this::netBeansPhysicallyFocused);
                long focusedWindow = NativeCanvasRuntimeCase.this
                        .foregroundFocusedWindow();
                long focusedOwner = focusedWindow == 0L
                        ? 0L
                        : ownerProcessId(focusedWindow);
                assertEquals(label + " physical Swing click focused another process",
                        targetOwner, focusedOwner);
                lifecycleEvents.add("physical-foreground(targetPid=" + targetOwner
                        + ",focusedHwnd=" + focusedWindow
                        + ",rootHwnd=" + rootWindow + ")");
            }

            private void assertStagingDidNotActivateTarget(
                    String phase,
                    NativeIdentity identity,
                    long targetOwner,
                    boolean startedBehindUnrelatedProcess,
                    long initialForegroundWindow,
                    long initialForegroundOwner) {
                if (!startedBehindUnrelatedProcess) {
                    return;
                }
                long focusedWindow = NativeCanvasRuntimeCase.this
                        .foregroundFocusedWindow();
                long focusedOwner = focusedWindow == 0L
                        ? 0L
                        : ownerProcessId(focusedWindow);
                assertTrue(label + " test-only HWND_TOP staging activated the "
                                + "assembled runtime " + phase + "; targetOwnerPid="
                                + targetOwner + "; runnerPid="
                                + identity.processId() + "; flutterViewHwnd="
                                + identity.flutterViewWindow()
                                + "; initialForegroundHwnd="
                                + initialForegroundWindow
                                + "; initialForegroundOwnerPid="
                                + initialForegroundOwner
                                + "; currentForegroundHwnd=" + focusedWindow
                                + "; currentForegroundOwnerPid=" + focusedOwner,
                        focusedOwner != targetOwner
                                && focusedOwner != identity.processId()
                                && focusedWindow != identity.flutterViewWindow());
            }

            private Point awaitSwingTargetExposed(
                    long expectedRoot,
                    long expectedOwner,
                    NativeIdentity identity) throws Exception {
                long deadline = System.nanoTime() + STOP_TIMEOUT.toNanos();
                NativeWindowHit lastHit = null;
                Throwable lastFailure = null;
                do {
                    drainEdt();
                    assertTrue(label + " assembled-runtime root HWND retired while "
                                    + "waiting for HWND_TOP exposure; targetRootHwnd="
                                    + expectedRoot,
                            isWindow(expectedRoot));
                    assertFalse(label + " assembled-runtime root HWND minimized while "
                                    + "waiting for HWND_TOP exposure; targetRootHwnd="
                                    + expectedRoot,
                            NativeCanvasRuntimeCase.this.isIconic(expectedRoot));
                    try {
                        Point point = netBeansFocusTargetScreenPoint();
                        long hitWindow = NativeCanvasRuntimeCase.this
                                .windowAtPoint(point);
                        long hitRoot = hitWindow == 0L
                                ? 0L
                                : NativeCanvasRuntimeCase.this.rootWindow(hitWindow);
                        long hitOwner = hitWindow == 0L
                                ? 0L
                                : ownerProcessId(hitWindow);
                        lastHit = new NativeWindowHit(
                                point, hitWindow, hitRoot, hitOwner);
                        if (hitRoot == expectedRoot
                                && hitOwner == expectedOwner) {
                            return point;
                        }
                        lastFailure = null;
                    } catch (Exception | AssertionError transientFailure) {
                        lastFailure = transientFailure;
                    }
                    Thread.sleep(25);
                } while (System.nanoTime() < deadline);

                long focusedWindow = NativeCanvasRuntimeCase.this
                        .foregroundFocusedWindow();
                long focusedOwner = focusedWindow == 0L
                        ? 0L
                        : ownerProcessId(focusedWindow);
                throw new AssertionError(label + " test-only HWND_TOP request did "
                        + "not expose the exact Swing target before Robot click; "
                        + "targetParentHwnd=" + identity.parentWindow()
                        + "; targetRootHwnd=" + expectedRoot
                        + "; targetOwnerPid=" + expectedOwner
                        + "; runnerPid=" + identity.processId()
                        + "; flutterViewHwnd=" + identity.flutterViewWindow()
                        + "; currentForegroundHwnd=" + focusedWindow
                        + "; currentForegroundOwnerPid=" + focusedOwner
                        + "; lastHit=" + lastHit,
                        lastFailure);
            }

            private void focusNetBeansControl() throws Exception {
                focusNetBeansControl(netBeansFocusTargetScreenPoint());
            }

            private Point netBeansFocusTargetScreenPoint() throws Exception {
                return onEdtValue(() -> {
                    assertTrue(label + " widget tree focus target is not showing",
                            netBeansFocusTarget.isShowing());
                    Point location = netBeansFocusTarget.getLocationOnScreen();
                    Rectangle firstRow = netBeansFocusTarget.getRowBounds(0);
                    int x = firstRow == null
                            ? Math.max(1, netBeansFocusTarget.getWidth() / 2)
                            : Math.max(1, firstRow.x + firstRow.width / 2);
                    int y = firstRow == null
                            ? Math.max(1, netBeansFocusTarget.getHeight() / 2)
                            : Math.max(1, firstRow.y + firstRow.height / 2);
                    location.translate(
                            x,
                            y);
                    return location;
                });
            }

            private void focusNetBeansControl(Point target) throws Exception {
                clickScreenPoint(target);
                drainEdt();
                lifecycleEvents.add("swing-click(showing=" + multiView.isShowing()
                        + ",focused=" + netBeansControlFocused() + ")");
            }

            private boolean netBeansControlFocused() {
                try {
                    return onEdtValue(() -> {
                        Component owner = KeyboardFocusManager
                                .getCurrentKeyboardFocusManager()
                                .getFocusOwner();
                        return owner == netBeansFocusTarget
                                || (owner != null
                                && SwingUtilities.isDescendingFrom(
                                        owner, netBeansFocusTarget));
                    });
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " Swing focus owner cannot be read",
                            failure);
                }
            }

            private boolean netBeansPhysicallyFocused() {
                if (!netBeansControlFocused() || runnerFocused()) {
                    return false;
                }
                long focusedWindow = NativeCanvasRuntimeCase.this
                        .foregroundFocusedWindow();
                return focusedWindow != 0L
                        && ownerProcessId(focusedWindow)
                        == ProcessHandle.current().pid();
            }

            private String focusDiagnostic() {
                try {
                    String swing = onEdtValue(() -> {
                        KeyboardFocusManager manager = KeyboardFocusManager
                                .getCurrentKeyboardFocusManager();
                        Component owner = manager.getFocusOwner();
                        java.awt.Window window = SwingUtilities.getWindowAncestor(
                                netBeansFocusTarget);
                        String ownerName = owner == null
                                ? "<none>"
                                : owner.getClass().getName()
                                + "[accessible=" + accessibleName(owner) + "]";
                        return "awtOwner=" + ownerName
                                + "; treeFocusOwner=" + netBeansFocusTarget.isFocusOwner()
                                + "; windowActive=" + (window != null && window.isActive())
                                + "; multiViewShowing=" + multiView.isShowing();
                    });
                    long focusedWindow = NativeCanvasRuntimeCase.this
                            .foregroundFocusedWindow();
                    long focusedOwner = focusedWindow == 0L
                            ? 0L
                            : ownerProcessId(focusedWindow);
                    return swing
                            + "; runnerFocused=" + runnerFocused()
                            + "; foregroundFocusHwnd=" + focusedWindow
                            + "; foregroundFocusOwnerPid=" + focusedOwner
                            + "; lifecycle=" + lifecycleEvents;
                } catch (Exception failure) {
                    return "focus diagnostic failed: " + failure;
                }
            }

            private void clickCanvas() throws Exception {
                Point center = onEdtValue(() -> {
                    assertTrue(label + " native Canvas surface is not showing",
                            surface.isShowing());
                    Point location = surface.getLocationOnScreen();
                    location.translate(
                            Math.max(1, surface.getWidth() / 2),
                            Math.max(1, surface.getHeight() / 2));
                    return location;
                });
                clickScreenPoint(center);
                drainEdt();
            }

            private void clickScreenPoint(Point point) throws Exception {
                Robot robot = new Robot();
                robot.setAutoDelay(25);
                robot.mouseMove(point.x, point.y);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                robot.waitForIdle();
            }

            private void moveScreenPoint(Point point) throws Exception {
                Robot robot = new Robot();
                robot.setAutoDelay(25);
                robot.mouseMove(point.x, point.y);
                robot.waitForIdle();
            }

            private String statusText() {
                try {
                    return onEdtValue(status::getText);
                } catch (Exception failure) {
                    throw new AssertionError(label + " status cannot be read", failure);
                }
            }

            private String diagnostic(NativeIdentity identity) {
                try {
                    return onEdtValue(() -> "status=" + status.getText()
                            + "; detail=" + status.getToolTipText()
                            + "; retryVisible=" + retry.isVisible()
                            + "; retryEnabled=" + retry.isEnabled()
                            + "; inputSyncVisible=" + inputSyncStatus.isVisible()
                            + "; inputSyncText=" + inputSyncStatus.getText()
                            + "; inputSyncDetail="
                            + inputSyncStatus.getToolTipText()
                            + "; multiViewOpened=" + multiView.isOpened()
                            + "; multiViewShowing=" + multiView.isShowing()
                            + "; hostDisplayable=" + host.isDisplayable()
                            + "; hostShowing=" + host.isShowing()
                            + "; attachment=" + ((Optional<?>) host.getClass()
                                    .getMethod("attachment")
                                    .invoke(host)).isPresent()
                            + "; parentHwndLive=" + isWindow(identity.parentWindow())
                            + "; runnerHwndLive=" + isWindow(identity.runnerWindow())
                            + "; flutterViewHwndLive="
                            + isWindow(identity.flutterViewWindow()));
                } catch (Exception failure) {
                    return "diagnostic unavailable: " + failure;
                }
            }

            private boolean retryAvailable() {
                try {
                    return onEdtValue(() -> retry.isVisible() && retry.isEnabled());
                } catch (Exception failure) {
                    throw new AssertionError(
                            label + " Retry state cannot be read", failure);
                }
            }

            private String retryDescription() throws Exception {
                return onEdtValue(() -> accessibleDescription(retry));
            }

            private void clickRetry() throws Exception {
                onEdt(() -> {
                    assertTrue(label + " Retry is not visible", retry.isVisible());
                    assertTrue(label + " Retry is not enabled", retry.isEnabled());
                    retry.doClick();
                });
            }

            private void activate() throws Exception {
                onEdt(() -> {
                    requestDesignVisible(multiView);
                    multiView.requestActive();
                });
                drainEdt();
            }

            private void close() throws Exception {
                if (closed) {
                    return;
                }
                closed = true;
                onEdt(() -> assertTrue(
                        label + " Designer MultiView did not close",
                        multiView.close()));
            }

            private long invokeLongRecord(Object record, String methodName)
                    throws Exception {
                Method method = record.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                return (long) method.invoke(record);
            }

            private int invokeIntRecord(Object record, String methodName)
                    throws Exception {
                Method method = record.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                return (int) method.invoke(record);
            }
        }

        private record PreviewSelection(
                int itemCount,
                int initialIndex,
                int targetIndex,
                String initialLabel,
                String targetLabel) {
        }

        private record PreviewPopup(
                Window window,
                Rectangle windowBounds,
                Point targetPoint) {
        }

        private record MainMenuPopup(
                JMenu menu,
                JPopupMenu popup,
                JMenuItem item,
                String menuLabel,
                String itemLabel,
                Rectangle popupBounds,
                Rectangle itemBounds,
                Point targetPoint,
                String popupWindowClass,
                boolean separatePopupWindow,
                boolean lightWeightPopupEnabled) {
        }

        private record MainMenuGeometry(
                Rectangle popupBounds,
                Rectangle itemBounds) {
        }

        private record NativeWindowHit(
                Point point,
                long window,
                long rootWindow,
                long ownerProcessId) {
        }

        private record NativeIdentity(
                ProcessHandle process,
                long parentWindow,
                long runnerWindow,
                long flutterViewWindow,
                long processId) {
        }

        private record NativeSurfaceMetrics(
                int width,
                int height,
                int devicePixelRatioMicros) {
        }

        private record NativeWindowBounds(int width, int height) {
        }

        private record Pair(FileObject dart, FileObject fd) {
        }
    }
}
