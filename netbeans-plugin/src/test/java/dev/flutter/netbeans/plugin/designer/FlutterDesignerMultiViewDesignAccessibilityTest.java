package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnostic;
import dev.flutter.netbeans.designer.catalog.CatalogDiagnosticCode;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.canvas.CanvasFrameKey;
import dev.flutter.netbeans.designer.canvas.CanvasLayoutKey;
import dev.flutter.netbeans.designer.canvas.CanvasRevisionKey;
import dev.flutter.netbeans.designer.canvas.CanvasSessionId;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnostic;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnosticCode;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteDropPlanner;
import dev.flutter.netbeans.plugin.designer.palette.FlutterDesignerPaletteItem;
import dev.flutter.netbeans.plugin.designer.properties.FlutterImageAssetChoices;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetPropertiesNode;
import dev.flutter.netbeans.plugin.project.FlutterProject;
import dev.flutter.netbeans.plugin.project.FlutterProjectPlatformProvider;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.netbeans.spi.palette.PaletteController;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

class FlutterDesignerMultiViewDesignAccessibilityTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void distinguishesSwingControlsFromTheNativeCanvasFocusSubtree() {
        JPanel designRoot = new JPanel();
        JPanel toolbar = new JPanel();
        JButton swingControl = new JButton("Preview");
        JButton toolbarControl = new JButton("Mode");
        JButton treeControl = new JButton("Widget tree");
        JPanel nativeCanvasRoot = new JPanel();
        JPanel nativeSurface = new JPanel();
        JButton nativeSwingProxy = new JButton("Canvas proxy");
        java.awt.Canvas heavyweightCanvas = new java.awt.Canvas();
        JPanel unrelatedWindow = new JPanel();
        toolbar.add(swingControl);
        toolbar.add(toolbarControl);
        designRoot.add(treeControl);
        designRoot.add(heavyweightCanvas);
        designRoot.add(nativeCanvasRoot);
        nativeCanvasRoot.add(nativeSurface);
        nativeCanvasRoot.add(nativeSwingProxy);

        assertTrue(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                swingControl, designRoot, toolbar, nativeCanvasRoot));
        assertTrue(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                toolbarControl, null, toolbar, nativeCanvasRoot));
        assertTrue(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                treeControl, designRoot, toolbar, nativeCanvasRoot));
        assertFalse(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                nativeSurface, designRoot, toolbar, nativeCanvasRoot));
        assertFalse(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                nativeSwingProxy, designRoot, toolbar, nativeCanvasRoot));
        assertFalse(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                heavyweightCanvas, designRoot, toolbar, nativeCanvasRoot));
        assertFalse(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                unrelatedWindow, designRoot, toolbar, nativeCanvasRoot));
        assertFalse(FlutterDesignerMultiViewDesign.shouldCancelDeferredCanvasFocus(
                null, designRoot, toolbar, nativeCanvasRoot));
    }

    @Test
    void resolvesTheActualFocusableSwingTargetForMouseFocusRepair() {
        JPanel nonFocusableCell = new JPanel();
        nonFocusableCell.setFocusable(false);
        JButton focusableControl = new JButton("Widget tree");
        focusableControl.add(nonFocusableCell);

        assertSame(
                focusableControl,
                FlutterDesignerMultiViewDesign.nearestFocusableSwingComponent(
                        nonFocusableCell, focusableControl));
        assertSame(
                focusableControl,
                FlutterDesignerMultiViewDesign.nearestFocusableSwingComponent(
                        focusableControl, focusableControl));
        assertEquals(
                null,
                FlutterDesignerMultiViewDesign.nearestFocusableSwingComponent(
                        new java.awt.Canvas(), focusableControl));

        JButton disabledRequestTarget = new JButton("No focus request");
        disabledRequestTarget.setRequestFocusEnabled(false);
        JPanel disabledChild = new JPanel();
        disabledChild.setFocusable(false);
        disabledRequestTarget.add(disabledChild);
        assertEquals(
                null,
                FlutterDesignerMultiViewDesign.nearestFocusableSwingComponent(
                        disabledChild, disabledRequestTarget));
    }

    @Test
    void classifiesNewerInputOutsideDesignAsASupersedingAwtInteraction() {
        JPanel designRoot = new JPanel();
        JPanel toolbar = new JPanel();
        JPanel nativeRoot = new JPanel();
        JButton tree = new JButton("Widget tree");
        JButton globalSearch = new JButton("Global search");
        java.awt.Canvas nativeSurface = new java.awt.Canvas();
        designRoot.add(tree);
        designRoot.add(nativeRoot);
        nativeRoot.add(nativeSurface);

        assertEquals(
                FlutterDesignerMultiViewDesign.SwingInputDisposition.DESIGN_SWING,
                FlutterDesignerMultiViewDesign.classifySwingInput(
                        tree, designRoot, toolbar, nativeRoot));
        assertEquals(
                FlutterDesignerMultiViewDesign.SwingInputDisposition.NATIVE_CANVAS,
                FlutterDesignerMultiViewDesign.classifySwingInput(
                        nativeSurface, designRoot, toolbar, nativeRoot));
        assertEquals(
                FlutterDesignerMultiViewDesign.SwingInputDisposition.OTHER_AWT,
                FlutterDesignerMultiViewDesign.classifySwingInput(
                        globalSearch, designRoot, toolbar, nativeRoot));
    }

    @Test
    void limitsExternalNativeFocusReleaseToStandardSwingMenus() {
        JPanel content = new JPanel();
        JMenu menu = new JMenu("Window");
        JMenuItem item = new JMenuItem("Projects");
        menu.add(item);
        content.add(menu);
        JButton unrelated = new JButton("Global search");
        content.add(unrelated);

        assertTrue(FlutterDesignerMultiViewDesign
                .isStandardSwingMenuInteraction(menu));
        assertTrue(FlutterDesignerMultiViewDesign
                .isStandardSwingMenuInteraction(item));
        assertFalse(FlutterDesignerMultiViewDesign
                .isStandardSwingMenuInteraction(unrelated));
    }

    @Test
    void providerAssertionCannotEscapeTheGlobalAwtInputListenerBoundary() {
        assertDoesNotThrow(() -> FlutterDesignerMultiViewDesign
                .releaseRunnerFocusSafely(() -> {
                    throw new AssertionError("synthetic provider assertion");
                }));
    }

    @Test
    void releasesExactRunnerFocusBeforeStartingSwingFocusRepair() {
        java.util.List<String> actions = new java.util.ArrayList<>();

        assertTrue(FlutterDesignerMultiViewDesign.beginSwingFocusRepairFromRunner(
                () -> {
                    actions.add("release-runner");
                    return true;
                },
                () -> actions.add("clear-global-owner")));
        assertEquals(java.util.List.of(
                "release-runner", "clear-global-owner"), actions);

        actions.clear();
        assertFalse(FlutterDesignerMultiViewDesign.beginSwingFocusRepairFromRunner(
                () -> {
                    actions.add("release-refused");
                    return false;
                },
                () -> actions.add("must-not-clear")));
        assertEquals(java.util.List.of("release-refused"), actions);

        actions.clear();
        assertDoesNotThrow(() -> assertFalse(
                FlutterDesignerMultiViewDesign.beginSwingFocusRepairFromRunner(
                        () -> {
                            actions.add("release-threw");
                            throw new AssertionError("synthetic provider assertion");
                        },
                        () -> actions.add("must-not-clear"))));
        assertEquals(java.util.List.of("release-threw"), actions);

        actions.clear();
        assertFalse(FlutterDesignerMultiViewDesign.beginSwingFocusRepairFromRunner(
                () -> {
                    actions.add("release-runner");
                    return true;
                },
                () -> {
                    actions.add("clear-threw");
                    throw new UnsatisfiedLinkError("synthetic AWT linkage failure");
                }));
        assertEquals(java.util.List.of(
                "release-runner", "clear-threw"), actions);
    }

    @Test
    void preservesExactRetainedAwtOwnerAcrossVerifiedNativeRelease() {
        JButton retained = new JButton("Widget tree");
        JButton descendant = new JButton("Tree cell editor");
        retained.add(descendant);
        JButton wrongOwner = new JButton("Global search");
        java.util.List<String> actions = new java.util.ArrayList<>();

        FlutterDesignerMultiViewDesign
                .clearGlobalFocusOwnerUnlessRetainedTargetCurrent(
                        () -> retained,
                        retained,
                        () -> actions.add("clear-exact"));
        FlutterDesignerMultiViewDesign
                .clearGlobalFocusOwnerUnlessRetainedTargetCurrent(
                        () -> descendant,
                        retained,
                        () -> actions.add("clear-descendant"));
        assertTrue(actions.isEmpty(),
                "an exact retained owner/descendant must not be cleared");

        FlutterDesignerMultiViewDesign
                .clearGlobalFocusOwnerUnlessRetainedTargetCurrent(
                        () -> null,
                        retained,
                        () -> actions.add("clear-absent"));
        FlutterDesignerMultiViewDesign
                .clearGlobalFocusOwnerUnlessRetainedTargetCurrent(
                        () -> wrongOwner,
                        retained,
                        () -> actions.add("clear-wrong"));
        assertEquals(java.util.List.of(
                "clear-absent", "clear-wrong"), actions);

        actions.clear();
        assertFalse(FlutterDesignerMultiViewDesign.beginSwingFocusRepairFromRunner(
                () -> {
                    actions.add("release-runner");
                    return true;
                },
                () -> FlutterDesignerMultiViewDesign
                        .clearGlobalFocusOwnerUnlessRetainedTargetCurrent(
                                () -> {
                                    throw new IllegalStateException(
                                            "synthetic owner lookup failure");
                                },
                                retained,
                                () -> actions.add("must-not-clear"))));
        assertEquals(java.util.List.of("release-runner"), actions,
                "owner revalidation failure must stop the repair fail-closed");
    }

    @Test
    void preservesSwingClaimOnlyForCurrentIntermediateHostFocus() {
        JButton retained = new JButton("Widget tree");
        JButton replacement = new JButton("Properties");
        JPanel exactHost = new JPanel();
        JButton exactDescendant = new JButton("Native surface");
        JPanel wrongOwner = new JPanel();
        exactHost.add(exactDescendant);
        FlutterDesignerMultiViewDesign.SwingFocusRepairNativeReleaseMarker marker =
                new FlutterDesignerMultiViewDesign
                        .SwingFocusRepairNativeReleaseMarker(
                                7L, 11L, retained);

        assertTrue(FlutterDesignerMultiViewDesign
                .canPreserveSwingClaimForIntermediateNativeRelease(
                        marker, exactHost, exactHost,
                        7L, 11L, true, retained));
        assertTrue(FlutterDesignerMultiViewDesign
                .canPreserveSwingClaimForIntermediateNativeRelease(
                        marker, exactDescendant, exactHost,
                        7L, 11L, true, retained));
        assertFalse(FlutterDesignerMultiViewDesign
                .canPreserveSwingClaimForIntermediateNativeRelease(
                        marker, wrongOwner, exactHost,
                        7L, 11L, true, retained),
                "an unrelated focus owner cannot inherit repair authority");
        assertFalse(FlutterDesignerMultiViewDesign
                .canPreserveSwingClaimForIntermediateNativeRelease(
                        marker, exactHost, exactHost,
                        8L, 11L, true, retained),
                "a stale repair epoch must clear the intermediate marker");
        assertFalse(FlutterDesignerMultiViewDesign
                .canPreserveSwingClaimForIntermediateNativeRelease(
                        marker, exactHost, exactHost,
                        7L, 12L, true, retained),
                "a newer AWT input or barrier epoch must fence the marker");
        assertFalse(FlutterDesignerMultiViewDesign
                .canPreserveSwingClaimForIntermediateNativeRelease(
                        marker, exactHost, exactHost,
                        7L, 11L, false, retained));
        assertFalse(FlutterDesignerMultiViewDesign
                .canPreserveSwingClaimForIntermediateNativeRelease(
                        marker, exactHost, exactHost,
                        7L, 11L, true, replacement),
                "a different retained Swing target must not reuse the marker");
    }

    @Test
    void reusesOneVerifiedRunnerReleaseForBoundedSwingTargetRetries() {
        JButton retained = new JButton("Widget tree");
        JButton replacement = new JButton("Properties");
        java.util.List<String> actions = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicBoolean runnerFocused =
                new java.util.concurrent.atomic.AtomicBoolean(true);

        assertTrue(FlutterDesignerMultiViewDesign.beginSwingFocusRepairFromRunner(
                () -> {
                    actions.add("release-runner");
                    runnerFocused.set(false);
                    return true;
                },
                () -> actions.add("clear-global-owner")));
        FlutterDesignerMultiViewDesign.SwingFocusRepairReleasedJvmAuthority
                authority = new FlutterDesignerMultiViewDesign
                        .SwingFocusRepairReleasedJvmAuthority(7L, retained);

        assertFalse(runnerFocused.get());
        assertTrue(FlutterDesignerMultiViewDesign
                .canReuseVerifiedRunnerRelease(
                        authority, 7L, retained,
                        true, retained, true, true));
        actions.add("request-target-refused");
        assertTrue(FlutterDesignerMultiViewDesign
                .canReuseVerifiedRunnerRelease(
                        authority, 7L, retained,
                        true, retained, true, true),
                "a refused first Swing request must retain the bounded JVM authority");
        actions.add("request-target-accepted");

        assertEquals(java.util.List.of(
                "release-runner",
                "clear-global-owner",
                "request-target-refused",
                "request-target-accepted"), actions,
                "a retry must not release or clear native focus a second time");
        assertFalse(FlutterDesignerMultiViewDesign
                .canReuseVerifiedRunnerRelease(
                        authority, 8L, retained,
                        true, retained, true, true),
                "a newer input/claim epoch must reject stale release authority");
        assertFalse(FlutterDesignerMultiViewDesign
                .canReuseVerifiedRunnerRelease(
                        authority, 7L, replacement,
                        true, replacement, true, true),
                "a different target cannot inherit release authority");
        assertFalse(FlutterDesignerMultiViewDesign
                .canReuseVerifiedRunnerRelease(
                        authority, 7L, retained,
                        false, retained, true, true));
        assertFalse(FlutterDesignerMultiViewDesign
                .canReuseVerifiedRunnerRelease(
                        authority, 7L, retained,
                        true, retained, false, true));
        assertFalse(FlutterDesignerMultiViewDesign
                .canReuseVerifiedRunnerRelease(
                        authority, 7L, retained,
                        true, retained, true, false));
    }

    @Test
    void admittedRunnerInteractionClearsSwingClaimBeforeRequestingNativeFocus() {
        java.util.List<String> actions = new java.util.ArrayList<>();

        FlutterDesignerMultiViewDesign.applyAdmittedNativeInteractionFocus(
                () -> actions.add("clear-swing-claim"),
                () -> actions.add("request-native-focus"));

        assertEquals(
                java.util.List.of("clear-swing-claim", "request-native-focus"),
                actions);
    }

    @Test
    void admitsOnlyPhysicalPressInsideTheExactCanvasOrItsAwtAncestor() {
        java.awt.Canvas exactHostCanvas = new java.awt.Canvas();
        JPanel owningAncestor = new JPanel();
        JPanel sibling = new JPanel();
        owningAncestor.add(exactHostCanvas);

        assertTrue(FlutterDesignerMultiViewDesign
                .isNativeCanvasFocusBootstrapPress(
                        true, exactHostCanvas, exactHostCanvas, true));
        assertTrue(FlutterDesignerMultiViewDesign
                .isNativeCanvasFocusBootstrapPress(
                        true, owningAncestor, exactHostCanvas, true),
                "AWT may initially source the physical press from an owning ancestor");
        assertFalse(FlutterDesignerMultiViewDesign
                .isNativeCanvasFocusBootstrapPress(
                        false, exactHostCanvas, exactHostCanvas, true),
                "a key event must not bootstrap Canvas focus");
        assertFalse(FlutterDesignerMultiViewDesign
                .isNativeCanvasFocusBootstrapPress(
                        true, sibling, exactHostCanvas, true),
                "a sibling source must not bootstrap Canvas focus");
        assertFalse(FlutterDesignerMultiViewDesign
                .isNativeCanvasFocusBootstrapPress(
                        true, exactHostCanvas, exactHostCanvas, false),
                "a press outside the exact Canvas bounds must remain fail-closed");
    }

    @Test
    void completesCanvasFocusBootstrapOnlyForCurrentSynchronizedMenuFreeFrame() {
        assertTrue(FlutterDesignerMultiViewDesign
                .canScheduleNativeCanvasFocusBootstrap(
                        true, true, true, true, true,
                        true, true, 7L, 7L),
                "a lingering menu path is evaluated after the physical press dispatch");
        assertTrue(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, true, true, true, true,
                        true, true, true, 7L, 7L));
        assertFalse(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, true, true, true, true,
                        false, true, true, 7L, 7L),
                "an unrendered frame must remain fail-closed");
        assertFalse(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, true, true, true, true,
                        true, false, true, 7L, 7L),
                "a pending interaction barrier must remain fail-closed");
        assertFalse(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, false, true, true, true,
                        true, true, true, 7L, 7L),
                "an inactive Design view must not request native focus");
        assertFalse(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, true, false, true, true,
                        true, true, true, 7L, 7L),
                "a hidden host must not request native focus");
        assertFalse(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, true, true, true, false,
                        true, true, true, 7L, 7L),
                "a hidden exact Canvas must not request native focus");
        assertFalse(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, true, true, true, true,
                        true, true, false, 7L, 7L),
                "an active Swing menu path must own the interaction");
        assertFalse(FlutterDesignerMultiViewDesign
                .canCompleteNativeCanvasFocusBootstrap(
                        true, true, true, true, true,
                        true, true, true, 7L, 8L),
                "a newer AWT interaction must cancel a stale bootstrap ticket");
    }

    @Test
    void keepsThePreviewSelectorPopupHeavyweightAboveTheNativeCanvas()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JComboBox<?> previewSelector = findByType(
                    design.getToolbarRepresentation(), JComboBox.class);

            assertFalse(previewSelector.isLightWeightPopupEnabled(),
                    "The Preview popup must not be painted below the native Flutter child");
        });
    }

    @Test
    void rejectsDisabledOrNonFocusableRetainedSwingTargets() {
        JPanel designRoot = new JPanel();
        JPanel toolbar = new JPanel();
        JPanel nativeRoot = new JPanel();
        designRoot.add(nativeRoot);
        JButton target = new JButton("Widget tree") {
            @Override
            public boolean isShowing() {
                return true;
            }
        };
        designRoot.add(target);

        assertTrue(FlutterDesignerMultiViewDesign.isUsableSwingFocusTarget(
                target, designRoot, toolbar, nativeRoot));
        target.setEnabled(false);
        assertFalse(FlutterDesignerMultiViewDesign.isUsableSwingFocusTarget(
                target, designRoot, toolbar, nativeRoot));
        target.setEnabled(true);
        target.setFocusable(false);
        assertFalse(FlutterDesignerMultiViewDesign.isUsableSwingFocusTarget(
                target, designRoot, toolbar, nativeRoot));
        target.setFocusable(true);
        target.setRequestFocusEnabled(false);
        assertFalse(FlutterDesignerMultiViewDesign.isUsableSwingFocusTarget(
                target, designRoot, toolbar, nativeRoot));
        target.setRequestFocusEnabled(true);
        nativeRoot.add(target);
        assertFalse(FlutterDesignerMultiViewDesign.isUsableSwingFocusTarget(
                target, designRoot, toolbar, nativeRoot));
    }

    @Test
    void fencesBoundedSwingFocusRetriesByTypedRenderEpochTargetAndLifecycle() {
        JButton retained = new JButton("Widget tree");
        JButton replacement = new JButton("Properties");

        assertFalse(FlutterDesignerMultiViewDesign.canScheduleSwingFocusRepair(
                false, true, 7, 7, retained, retained, true, false));
        assertFalse(FlutterDesignerMultiViewDesign.canScheduleSwingFocusRepair(
                true, false, 7, 7, retained, retained, true, false));
        assertFalse(FlutterDesignerMultiViewDesign.canScheduleSwingFocusRepair(
                true, true, 6, 7, retained, retained, true, false));
        assertFalse(FlutterDesignerMultiViewDesign.canScheduleSwingFocusRepair(
                true, true, 7, 7, retained, replacement, true, false));
        assertFalse(FlutterDesignerMultiViewDesign.canScheduleSwingFocusRepair(
                true, true, 7, 7, retained, retained, false, false));
        assertFalse(FlutterDesignerMultiViewDesign.canScheduleSwingFocusRepair(
                true, true, 7, 7, retained, retained, true, true));
        assertTrue(FlutterDesignerMultiViewDesign.canScheduleSwingFocusRepair(
                true, true, 7, 7, retained, retained, true, false));

        FlutterDesignerMultiViewDesign.SwingFocusRepairRetryFence fence =
                new FlutterDesignerMultiViewDesign.SwingFocusRepairRetryFence(
                        FlutterDesignerMultiViewDesign.MAX_SWING_FOCUS_REPAIR_ATTEMPTS);
        FlutterDesignerMultiViewDesign.SwingFocusRepairTicket stale =
                fence.reserve(7, retained).orElseThrow();
        assertTrue(fence.isActive(stale));
        assertTrue(fence.reserve(7, retained).isEmpty(),
                "one exact timer ticket must remain the sole active retry");
        fence.clearActive();
        assertEquals(1, fence.attempts(),
                "fencing a pending ticket must preserve the consumed retry budget");
        FlutterDesignerMultiViewDesign.SwingFocusRepairTicket postAck =
                fence.reserve(7, retained).orElseThrow();
        assertFalse(fence.isActive(stale),
                "an ACK must fence the pending-era timer ticket");
        assertTrue(fence.isActive(postAck),
                "an ACK must issue one fresh delayed timer ticket");
        assertEquals(2, fence.attempts(),
                "ACK re-arm must preserve the bounded retry budget");
        fence.reset();
        assertFalse(fence.isActive(stale),
                "teardown must fence an already queued timer callback");
        assertFalse(fence.isActive(postAck),
                "teardown must also fence the fresh post-ACK timer callback");

        for (int attempt = 1;
                attempt <= FlutterDesignerMultiViewDesign.MAX_SWING_FOCUS_REPAIR_ATTEMPTS;
                attempt++) {
            FlutterDesignerMultiViewDesign.SwingFocusRepairTicket ticket =
                    fence.reserve(8, replacement).orElseThrow();
            assertEquals(attempt, fence.attempts());
            fence.consume(ticket);
        }
        assertTrue(fence.reserve(8, replacement).isEmpty(),
                "the late-focus polling window must be bounded");

        fence.reset();
        FlutterDesignerMultiViewDesign.SwingFocusRepairTicket current =
                fence.reserve(9, retained).orElseThrow();
        assertFalse(fence.isActive(stale));
        assertTrue(fence.isActive(current));
        assertEquals(1, fence.attempts(),
                "a new exact claim receives a fresh bounded retry window");
    }

    @Test
    void rearmsSwingFocusRepairOnceOnTheTypedRenderedTransition() {
        JButton retained = new JButton("Widget tree");
        FlutterDesignerMultiViewDesign.SwingFocusRepairRetryFence fence =
                new FlutterDesignerMultiViewDesign.SwingFocusRepairRetryFence(
                        FlutterDesignerMultiViewDesign
                                .MAX_SWING_FOCUS_REPAIR_ATTEMPTS);

        for (int attempt = 0;
                attempt < FlutterDesignerMultiViewDesign
                        .MAX_SWING_FOCUS_REPAIR_ATTEMPTS;
                attempt++) {
            FlutterDesignerMultiViewDesign.SwingFocusRepairTicket ticket =
                    fence.reserve(7L, retained).orElseThrow();
            fence.consume(ticket);
        }
        assertTrue(fence.reserve(7L, retained).isEmpty());
        assertTrue(FlutterDesignerMultiViewDesign
                .shouldRearmSwingFocusRepairOnRenderedTransition(
                        false, true, true, retained));
        fence.reset();
        FlutterDesignerMultiViewDesign.SwingFocusRepairTicket rearmed =
                fence.reserve(8L, retained).orElseThrow();
        fence.consume(rearmed);
        assertEquals(1, fence.attempts(),
                "the typed rendered transition starts one fresh bounded window");

        assertFalse(FlutterDesignerMultiViewDesign
                .shouldRearmSwingFocusRepairOnRenderedTransition(
                        true, true, true, retained),
                "repeated rendered statuses must not re-arm the retry budget");
        assertFalse(FlutterDesignerMultiViewDesign
                .shouldRearmSwingFocusRepairOnRenderedTransition(
                        false, true, false, retained));
        assertFalse(FlutterDesignerMultiViewDesign
                .shouldRearmSwingFocusRepairOnRenderedTransition(
                        false, true, true, null));
        for (int attempt = 1;
                attempt < FlutterDesignerMultiViewDesign
                        .MAX_SWING_FOCUS_REPAIR_ATTEMPTS;
                attempt++) {
            FlutterDesignerMultiViewDesign.SwingFocusRepairTicket ticket =
                    fence.reserve(8L, retained).orElseThrow();
            fence.consume(ticket);
        }
        assertTrue(fence.reserve(8L, retained).isEmpty(),
                "a repeated rendered status cannot create unbounded retries");
    }

    @Test
    void fencesQueuedSwingFocusContinuationByBarrierTransitionEpoch() {
        JButton retained = new JButton("Widget tree");
        JButton replacement = new JButton("Properties");

        assertTrue(FlutterDesignerMultiViewDesign
                .canApplySwingFocusRepairContinuation(
                        7, 7, retained, retained, 11, 11, true, true, true),
                "a bounded repair remains valid inside one synchronizing epoch");
        assertFalse(FlutterDesignerMultiViewDesign
                .canApplySwingFocusRepairContinuation(
                        7, 7, retained, retained, 11, 12, true, true, true),
                "ACK, timeout or teardown must fence a queued pre-transition repair");
        assertFalse(FlutterDesignerMultiViewDesign
                .canApplySwingFocusRepairContinuation(
                        7, 8, retained, retained, 11, 11, true, true, true));
        assertFalse(FlutterDesignerMultiViewDesign
                .canApplySwingFocusRepairContinuation(
                        7, 7, retained, replacement, 11, 11, true, true, true));
        assertFalse(FlutterDesignerMultiViewDesign
                .canApplySwingFocusRepairContinuation(
                        7, 7, retained, retained, 11, 11, false, true, true));
    }

    @Test
    void permitsRepairOnlyForTheRetainedCurrentAwtOwner() {
        JButton retained = new JButton("Widget tree");
        JPanel retainedChild = new JPanel();
        retained.add(retainedChild);
        JButton newerOwner = new JButton("Global search");

        assertTrue(FlutterDesignerMultiViewDesign.currentOwnerAllowsSwingFocusRepair(
                null, retained));
        assertTrue(FlutterDesignerMultiViewDesign.currentOwnerAllowsSwingFocusRepair(
                retained, retained));
        assertTrue(FlutterDesignerMultiViewDesign.currentOwnerAllowsSwingFocusRepair(
                retainedChild, retained));
        assertFalse(FlutterDesignerMultiViewDesign.currentOwnerAllowsSwingFocusRepair(
                newerOwner, retained));
    }

    @Test
    void registersStandardDeleteActionAndTreeKeyWithoutSelectionAuthority()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JComponent visual = design.getVisualRepresentation();
            Action delete = visual.getActionMap().get(
                    FlutterDesignerMultiViewDesign.DELETE_WIDGET_ACTION_KEY);
            assertNotNull(delete);
            assertEquals("Delete Flutter Widget", delete.getValue(Action.NAME));
            assertFalse(delete.isEnabled(),
                    "Delete must be disabled without one exact non-root selection");

            BeanTreeView tree = findByType(visual, BeanTreeView.class);
            KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            assertEquals(
                    FlutterDesignerMultiViewDesign.DELETE_WIDGET_ACTION_KEY,
                    tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                            .get(deleteKey));
            assertSame(delete, tree.getActionMap().get(
                    FlutterDesignerMultiViewDesign.DELETE_WIDGET_ACTION_KEY));

            delete.actionPerformed(new ActionEvent(
                    tree, ActionEvent.ACTION_PERFORMED, "delete"));
            assertFalse(delete.isEnabled(),
                    "Direct invocation must remain a no-op without selection");
        });
    }

    @Test
    void publishesExactCapabilityPaletteAndSelectedWidgetPropertiesInDesignLookup()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);

            PaletteController palette = design.getLookup().lookup(PaletteController.class);
            assertNotNull(palette);
            Node paletteRoot = palette.getRoot().lookup(Node.class);
            assertNotNull(paletteRoot);
            assertEquals(List.of(
                    "flutter.material.Scaffold",
                    "flutter.material.AppBar",
                    "flutter.material.ElevatedButton",
                    "flutter.material.TextField",
                    "flutter.widgets.Column",
                    "flutter.widgets.Row",
                    "flutter.widgets.Wrap",
                    "flutter.widgets.Padding",
                    "flutter.widgets.Center",
                    "flutter.widgets.SizedBox",
                    "flutter.widgets.AspectRatio",
                    "flutter.widgets.Container",
                    "flutter.widgets.Opacity",
                    "flutter.widgets.Align",
                    "flutter.widgets.FractionallySizedBox",
                    "flutter.widgets.FittedBox",
                    "flutter.widgets.ConstrainedBox",
                    "flutter.widgets.UnconstrainedBox",
                    "flutter.widgets.LimitedBox",
                    "flutter.widgets.OverflowBox",
                    "flutter.widgets.Stack",
                    "flutter.widgets.IndexedStack",
                    "flutter.widgets.Expanded",
                    "flutter.widgets.Flexible",
                    "flutter.widgets.Spacer",
                    "flutter.widgets.Baseline",
                    "flutter.widgets.IntrinsicHeight",
                    "flutter.widgets.IntrinsicWidth",
                    "flutter.widgets.Offstage",
                    "flutter.widgets.SizedOverflowBox",
                    "flutter.widgets.Transform",
                    "flutter.widgets.RotatedBox",
                    "flutter.widgets.ListBody",
                    "flutter.widgets.OverflowBar",
                    "flutter.widgets.SafeArea",
                    "flutter.widgets.ListView",
                    dev.flutter.netbeans.designer.catalog.GridViewCountWidgetPropertySchema
                            .GRID_VIEW_COUNT_TYPE.value(),
                    dev.flutter.netbeans.designer.catalog
                            .SingleChildScrollViewWidgetPropertySchema
                            .SINGLE_CHILD_SCROLL_VIEW_TYPE.value(),
                    "flutter.widgets.Text",
                    "flutter.widgets.Icon",
                    "flutter.widgets.Image",
                    "flutter.widgets.ColoredBox",
                    "flutter.widgets.Placeholder",
                    "flutter.widgets.Directionality",
                    "flutter.widgets.DecoratedBox",
                    "flutter.widgets.ExcludeSemantics"),
                    java.util.Arrays.stream(paletteRoot.getChildren().getNodes(true))
                            .flatMap(category -> java.util.Arrays.stream(
                                    category.getChildren().getNodes(true)))
                            .map(item -> item.getLookup()
                                    .lookup(FlutterDesignerPaletteItem.class)
                                    .typeId().value())
                            .toList());

            publish(design, currentState(List.of()));

            Node selected = design.getLookup().lookup(Node.class);
            assertNotNull(selected);
            assertNotNull(selected.getLookup().lookup(WidgetDefinition.class));
            assertEquals("flutter.widgets.SizedBox",
                    selected.getLookup().lookup(WidgetDefinition.class)
                            .typeId().value());
            Node.PropertySet properties = java.util.Arrays.stream(
                            selected.getPropertySets())
                    .filter(set -> FlutterWidgetPropertiesNode.PROPERTIES_SET_NAME
                            .equals(set.getName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(List.of("width", "height"),
                    java.util.Arrays.stream(properties.getProperties())
                            .map(Node.Property::getName)
                            .toList());
            for (Node.Property<?> property : properties.getProperties()) {
                assertEquals(FlutterWidgetPropertiesNode.NOT_SET,
                        property.getValue());
                assertFalse(property.canWrite());
            }
        });
    }

    @Test
    void flexiblePaletteSourceUsesExactMultiViewCanvasAuthorizationType() {
        WidgetDefinition flexible = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Flexible"))
                .orElseThrow();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<WidgetTypeId> type = new AtomicReference<>();

        assertTrue(FlutterDesignerMultiViewDesign.authorizeCanvasPaletteDragSource(
                true,
                "opaque-flexible-token",
                flexible,
                (candidateToken, candidateType) -> {
                    calls.incrementAndGet();
                    token.set(candidateToken);
                    type.set(candidateType);
                    return true;
                }));
        assertEquals(1, calls.get());
        assertEquals("opaque-flexible-token", token.get());
        assertEquals(new WidgetTypeId("flutter.widgets.Flexible"), type.get());

        assertFalse(FlutterDesignerMultiViewDesign.authorizeCanvasPaletteDragSource(
                false,
                "disabled-token",
                flexible,
                (ignoredToken, ignoredType) -> {
                    throw new AssertionError(
                            "disabled MultiView authority must not reach Canvas");
                }));
    }

    @Test
    void spacerPaletteSourceUsesExactMultiViewCanvasAuthorizationType() {
        WidgetDefinition spacer = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Spacer"))
                .orElseThrow();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<WidgetTypeId> type = new AtomicReference<>();

        assertTrue(FlutterDesignerMultiViewDesign.authorizeCanvasPaletteDragSource(
                true,
                "opaque-spacer-token",
                spacer,
                (candidateToken, candidateType) -> {
                    calls.incrementAndGet();
                    token.set(candidateToken);
                    type.set(candidateType);
                    return true;
                }));
        assertEquals(1, calls.get());
        assertEquals("opaque-spacer-token", token.get());
        assertEquals(new WidgetTypeId("flutter.widgets.Spacer"), type.get());

        assertFalse(FlutterDesignerMultiViewDesign.authorizeCanvasPaletteDragSource(
                false,
                "disabled-token",
                spacer,
                (ignoredToken, ignoredType) -> {
                    throw new AssertionError(
                            "disabled MultiView authority must not reach Canvas");
                }));
    }

    @Test
    void imagePaletteSourceUsesCanvasAuthorizationWithoutAssetPreflight() {
        WidgetDefinition image = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Image"))
                .orElseThrow();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<WidgetTypeId> type = new AtomicReference<>();

        assertTrue(FlutterDesignerMultiViewDesign.authorizeCanvasPaletteDragSource(
                true,
                "opaque-image-token",
                image,
                (candidateToken, candidateType) -> {
                    calls.incrementAndGet();
                    token.set(candidateToken);
                    type.set(candidateType);
                    return true;
                }));
        assertEquals(1, calls.get());
        assertEquals("opaque-image-token", token.get());
        assertEquals(new WidgetTypeId("flutter.widgets.Image"), type.get());
    }

    @Test
    void imagePlannerRejectionUsesGeneralInlinePaletteFeedback() {
        FlutterDesignerPaletteDropPlanner.Rejected rejected =
                new FlutterDesignerPaletteDropPlanner.Rejected(
                        FlutterDesignerPaletteDropPlanner.RejectionCode
                                .REQUIRED_CREATION_VALUE_UNAVAILABLE,
                        "The declared image inventory changed during the drag.");
        AtomicReference<String> summary = new AtomicReference<>();
        AtomicReference<String> detail = new AtomicReference<>();

        FlutterDesignerMultiViewDesign.publishPaletteDropRejection(
                "new_screen.fd — add Image to widget parent.children at index 1",
                rejected,
                (renderedSummary, renderedDetail) -> {
                    summary.set(renderedSummary);
                    detail.set(renderedDetail);
                });

        assertEquals("Flutter Palette drop was not applied.", summary.get());
        assertTrue(detail.get().startsWith(
                "Operation: apply Flutter Palette drop."));
        assertTrue(detail.get().contains("inventory changed during the drag"));
        assertFalse(detail.get().contains("assets/example.png"));
    }

    @Test
    void nonImagePaletteRejectionStaysInlineWithoutMutationResultDialog() {
        FlutterDesignerPaletteDropPlanner.Rejected rejected =
                new FlutterDesignerPaletteDropPlanner.Rejected(
                        FlutterDesignerPaletteDropPlanner.RejectionCode.SLOT_FULL,
                        "The target slot is full.");
        AtomicReference<String> summary = new AtomicReference<>();
        AtomicReference<String> detail = new AtomicReference<>();

        FlutterDesignerMultiViewDesign.publishPaletteDropRejection(
                "new_screen.fd — Palette drop on widget parent.child at index 0",
                rejected,
                (renderedSummary, renderedDetail) -> {
                    summary.set(renderedSummary);
                    detail.set(renderedDetail);
                });

        assertEquals("Flutter Palette drop was not applied.", summary.get());
        assertTrue(detail.get().startsWith(
                "Operation: apply Flutter Palette drop."));
        assertTrue(detail.get().contains("The target slot is full."));
        assertFalse(detail.get().contains("assets/example.png"));
    }

    @Test
    void imageSlotReplacementResolvesRequiredAssetBeforeStableIdAllocation() {
        WidgetDefinition definition = BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.widgets.Image"))
                .orElseThrow();
        StableId expectedId = StableId.parse(
                "23655e27-aa73-430f-9967-5344076c4fa2");
        AtomicInteger availableAllocations = new AtomicInteger();
        FlutterImageAssetChoices available = new FlutterImageAssetChoices(
                List.of(
                        new FlutterImageAssetChoices.Choice(
                                Optional.empty(), "assets/z.png", "Z"),
                        new FlutterImageAssetChoices.Choice(
                                Optional.empty(), "assets/a.png", "A")),
                Optional.empty());

        WidgetNode prototype = FlutterDesignerMultiViewDesign
                .createSlotReplacementPrototype(
                        definition,
                        available,
                        () -> {
                            availableAllocations.incrementAndGet();
                            return expectedId;
                        });

        PropertyValue.ImageProviderValue image = assertInstanceOf(
                PropertyValue.ImageProviderValue.class,
                prototype.properties().get(new PropertyName("image")));
        assertEquals(expectedId, prototype.id());
        assertEquals("assets/a.png", image.assetName());
        assertEquals(1, availableAllocations.get());

        AtomicInteger placeholderAllocations = new AtomicInteger();
        FlutterImageAssetChoices unavailable = new FlutterImageAssetChoices(
                List.of(),
                Optional.of("the current pubspec declares no safe image asset."));
        WidgetNode placeholder = FlutterDesignerMultiViewDesign
                .createSlotReplacementPrototype(
                        definition,
                        unavailable,
                        () -> {
                            placeholderAllocations.incrementAndGet();
                            return expectedId;
                        });
        PropertyValue.ImageProviderValue unresolved = assertInstanceOf(
                PropertyValue.ImageProviderValue.class,
                placeholder.properties().get(new PropertyName("image")));
        assertTrue(unresolved.isUnresolved());
        assertEquals(1, placeholderAllocations.get());
    }

    @Test
    void excludesTheFileNodeFromTheDesignLookupAndPublishesOnlyTheWidgetNode()
            throws Exception {
        onEdt(() -> {
            record ContextMarker() { }
            Node fileNode = new AbstractNode(Children.LEAF);
            fileNode.setDisplayName("new_screen.dart");
            ContextMarker retainedContextValue = new ContextMarker();
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookups.fixed(
                            fileNode,
                            retainedContextValue));

            publish(design, currentState(List.of()));

            List<? extends Node> activeNodes = List.copyOf(
                    design.getLookup().lookupAll(Node.class));
            assertEquals(1, activeNodes.size());
            assertInstanceOf(FlutterWidgetPropertiesNode.class, activeNodes.get(0));
            assertFalse(activeNodes.contains(fileNode));
            assertNotNull(activeNodes.get(0).getLookup().lookup(WidgetDefinition.class));
            assertEquals("flutter.widgets.SizedBox",
                    activeNodes.get(0).getLookup()
                            .lookup(WidgetDefinition.class)
                            .typeId()
                            .value());
            assertSame(retainedContextValue,
                    design.getLookup().lookup(ContextMarker.class));
            assertNotNull(design.getLookup().lookup(PaletteController.class));
            assertTrue(java.util.Arrays.stream(activeNodes.get(0).getPropertySets())
                    .anyMatch(set -> set.getProperties().length > 0));
        });
    }

    @Test
    void retainsEligibleDurableCanvasReadOnlyAcrossEmptyMutationSnapshots()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY, () -> true);
            FlutterDesignerDocumentState.Current current = currentState(List.of());
            publish(design, current);

            assertSame(current.decoded().document(),
                    design.currentCanvasDocumentForTests());
            assertSingleReadOnlyWidgetNode(design);

            renderMutationSnapshot(
                    design,
                    FlutterDesignerMutationController.Snapshot.waiting(
                            "Waiting for exact mutation admission."));
            assertSame(current.decoded().document(),
                    design.currentCanvasDocumentForTests());
            assertSingleReadOnlyWidgetNode(design);

            renderMutationSnapshot(
                    design,
                    FlutterDesignerMutationController.Snapshot.unavailable(
                            FlutterDesignerMutationController.Status.BLOCKED,
                            "Prepare Flutter Designer Properties",
                            "sample.fd",
                            "Flutter/Dart SDK integration is unavailable."));
            assertSame(current.decoded().document(),
                    design.currentCanvasDocumentForTests());
            assertSingleReadOnlyWidgetNode(design);
        });
    }

    @Test
    void identifiesOnlyTheConfirmedFirstFrameAsRendered() {
        FlutterDesignerNativeCanvasStatus confirmed =
                new FlutterDesignerNativeCanvasStatus(
                        FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                        "Confirmed first frame.",
                        "Validated revision 7 is visible in the embedded FlutterView.",
                        true);
        FlutterDesignerNativeCanvasStatus merelyRunning =
                new FlutterDesignerNativeCanvasStatus(
                        FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                        "Native Flutter Canvas rendered.",
                        "The runner exists, but no confirmed frame is visible yet.");

        assertTrue(confirmed.rendered());
        assertFalse(merelyRunning.rendered());
    }

    @Test
    void rendersInputSynchronizationBesideWithoutReplacingCanvasStatus()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JComponent visual = design.getVisualRepresentation();
            JLabel primary = findNamed(
                    visual, JLabel.class, "Native Flutter Canvas status");
            JLabel input = findNamed(
                    visual,
                    JLabel.class,
                    "Native Canvas interaction status");
            FlutterDesignerNativeCanvasStatus failed =
                    new FlutterDesignerNativeCanvasStatus(
                            FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                            "Native Canvas launch failed.",
                            "Target: test runner. Reason: deliberate status fixture.");
            design.renderNativeCanvasStatus(failed);
            String primaryFailure = primary.getText();
            CanvasLayoutKey synchronizedLayout = new CanvasLayoutKey(
                    new CanvasFrameKey(
                            new CanvasRevisionKey(
                                    CanvasSessionId.parse(
                                            "6a7bab32-9507-4f6d-b986-39f183742017"),
                                    3,
                                    StableId.parse(
                                            "f83e4ad8-e66f-43ae-a5b3-cb057809f17e"),
                                    5),
                            7),
                    11);

            design.renderInteractionBarrierState(
                    new FlutterDesignerNativeCanvasSession.InteractionBarrierState(
                            FlutterDesignerNativeCanvasSession
                                    .InteractionBarrierPhase.SYNCHRONIZING,
                            7,
                            Optional.empty()));
            assertEquals(primaryFailure, primary.getText());
            assertTrue(input.isVisible());
            assertEquals("Input sync…", input.getText());
            assertTrue(input.getToolTipText().contains("fence 7"));

            design.renderInteractionBarrierState(
                    new FlutterDesignerNativeCanvasSession.InteractionBarrierState(
                            FlutterDesignerNativeCanvasSession
                                    .InteractionBarrierPhase.TIMED_OUT,
                            7,
                            Optional.empty()));
            assertEquals(primaryFailure, primary.getText());
            assertTrue(input.isVisible());
            assertEquals("Input sync timed out", input.getText());
            assertTrue(input.getAccessibleContext().getAccessibleDescription()
                    .contains("Swing focus was retained"));

            design.renderInteractionBarrierState(
                    new FlutterDesignerNativeCanvasSession.InteractionBarrierState(
                            FlutterDesignerNativeCanvasSession
                                    .InteractionBarrierPhase.SYNCHRONIZED,
                            7,
                            Optional.of(synchronizedLayout)));
            assertEquals(primaryFailure, primary.getText());
            assertFalse(input.isVisible());
            assertEquals("", input.getText());
            assertNull(input.getToolTipText());

            design.renderInteractionBarrierState(
                    new FlutterDesignerNativeCanvasSession.InteractionBarrierState(
                            FlutterDesignerNativeCanvasSession
                                    .InteractionBarrierPhase.INACTIVE,
                            7,
                            Optional.empty()));
            assertEquals(primaryFailure, primary.getText());
            assertFalse(input.isVisible());
        });
    }

    @Test
    void paletteDropFeedbackDoesNotReplaceOrClearBarrierTimeout()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JComponent visual = design.getVisualRepresentation();
            JLabel input = findNamed(
                    visual,
                    JLabel.class,
                    "Native Canvas interaction status");
            JLabel paletteFeedback = findNamed(
                    visual,
                    JLabel.class,
                    "Flutter Palette drop feedback");

            design.renderInteractionBarrierState(
                    new FlutterDesignerNativeCanvasSession.InteractionBarrierState(
                            FlutterDesignerNativeCanvasSession
                                    .InteractionBarrierPhase.TIMED_OUT,
                            17,
                            Optional.empty()));
            String barrierDetail = input.getToolTipText();

            design.renderPaletteDropFeedback(
                    "Flutter Palette drop was not applied.",
                    "Operation: apply Flutter Palette drop. Reason: stale test drop.");

            assertTrue(input.isVisible());
            assertEquals("Input sync timed out", input.getText());
            assertEquals(barrierDetail, input.getToolTipText());
            assertTrue(paletteFeedback.isVisible());
            assertEquals(
                    "Flutter Palette drop was not applied.",
                    paletteFeedback.getText());

            design.clearPaletteDropFeedback();

            assertTrue(input.isVisible(),
                    "clearing Palette feedback must preserve a gated Canvas barrier");
            assertEquals("Input sync timed out", input.getText());
            assertEquals(barrierDetail, input.getToolTipText());
            assertFalse(paletteFeedback.isVisible());
            assertEquals("", paletteFeedback.getText());
            assertNull(paletteFeedback.getToolTipText());
        });
    }

    @Test
    void assetInventoryTransitionClearsExistingPaletteFeedback()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JComponent visual = design.getVisualRepresentation();
            JLabel paletteFeedback = findNamed(
                    visual,
                    JLabel.class,
                    "Flutter Palette drop feedback");
            FlutterImageAssetChoices refreshing = new FlutterImageAssetChoices(
                    List.of(), Optional.of("Refreshing image assets."));
            FlutterImageAssetChoices failed = new FlutterImageAssetChoices(
                    List.of(), Optional.of("Asset resolver failed."));

            design.refreshCanvasAfterProjectAssetChange(refreshing);
            design.renderPaletteDropFeedback(
                    "Flutter Palette drop was not applied.",
                    "Operation: apply Flutter Palette drop. Reason: temporary feedback.");
            assertTrue(paletteFeedback.isVisible());
            assertTrue(paletteFeedback.getToolTipText()
                    .contains("temporary feedback"));

            design.refreshCanvasAfterProjectAssetChange(failed);

            assertFalse(paletteFeedback.isVisible(),
                    "an asset-inventory transition must clear stale feedback "
                    + "even when no Canvas document is loaded");
            assertEquals("", paletteFeedback.getText());
            assertNull(paletteFeedback.getToolTipText());
        });
    }

    @Test
    void keepsCanvasFailureInlineConciseAndExposesFullDetailsAction() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JLabel canvasStatus = findNamed(
                    visual, JLabel.class, "Native Flutter Canvas status");
            JButton details = findNamed(
                    visual, JButton.class, "Show Native Flutter Canvas details");
            JButton retry = findNamed(
                    visual, JButton.class, "Retry Native Flutter Canvas");
            String summary = "Prepare native Flutter Canvas launch failed.";
            String detail = "Target: embedded Windows FlutterView. Reason: "
                    + "the generated runner command produced a deliberately long "
                    + "diagnostic that must remain available without stretching or "
                    + "clipping the Design view status row.";

            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.FAILED,
                    summary,
                    detail));

            assertEquals(summary, canvasStatus.getText());
            assertEquals(detail, canvasStatus.getToolTipText());
            assertEquals(summary + " " + detail,
                    canvasStatus.getAccessibleContext().getAccessibleDescription());
            assertTrue(details.isVisible());
            assertTrue(details.isEnabled());
            assertTrue(details.isFocusable());
            assertTrue(details.getAccessibleContext().getAccessibleDescription()
                    .contains(summary));
            assertFalse(retry.isVisible(),
                    "a nonterminal status publication must not expose process retry");
            assertFalse(retry.isEnabled());

            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                    "Native Flutter Canvas is running.",
                    "Verified embedded FlutterView."));
            assertEquals("Native Flutter Canvas is running.", canvasStatus.getText());
            assertFalse(details.isVisible());
            assertFalse(details.isEnabled());

            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                    "Native Flutter Canvas is unavailable.",
                    "Target: embedded FlutterView. Reason: no platform host."));
            assertEquals("Native Flutter Canvas is unavailable.",
                    canvasStatus.getText());
            assertTrue(details.isVisible());
            assertTrue(details.isEnabled());
        });
    }

    @Test
    void rendersValidatedSuccessAsOneCompactHorizontalStatusRow() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JPanel statusPanel = findNamed(
                    visual, JPanel.class, "Flutter Designer model status");
            JLabel status = findNamed(
                    statusPanel, JLabel.class, "Flutter Designer status");
            JLabel model = findNamed(
                    statusPanel, JLabel.class, "Flutter Designer model and source");
            JLabel detail = findNamed(
                    statusPanel, JLabel.class, "Flutter Designer status details");
            JLabel canvasStatus = findNamed(
                    statusPanel, JLabel.class, "Native Flutter Canvas status");
            JButton canvasDetails = findNamed(
                    statusPanel, JButton.class, "Show Native Flutter Canvas details");

            publish(design, currentState(List.of()));
            design.renderNativeCanvasStatus(new FlutterDesignerNativeCanvasStatus(
                    FlutterDesignerNativeCanvasStatus.Stage.RUNNING,
                    "Native Flutter Canvas rendered.",
                    "Validated revision 7 is visible in the embedded FlutterView."));

            assertEquals("Designer ready.", status.getText());
            assertTrue(status.isVisible());
            assertTrue(canvasStatus.isVisible());
            assertFalse(isVisibleWithin(model, statusPanel),
                    "model/source diagnostics must not occupy a second banner row");
            assertFalse(isVisibleWithin(detail, statusPanel),
                    "success details must remain metadata rather than banner copy");
            assertFalse(canvasDetails.isVisible());
            assertFalse(canvasDetails.isEnabled());
            assertNotNull(status.getToolTipText());
            assertTrue(status.getToolTipText().contains("root widget"));
            assertTrue(status.getToolTipText().contains("on-disk imports and build regions"));
            assertTrue(statusPanel.getAccessibleContext().getAccessibleDescription()
                    .contains("on-disk imports and build regions"));

            int tallestSummary = Math.max(
                    status.getPreferredSize().height,
                    canvasStatus.getPreferredSize().height);
            int verticalInsets = statusPanel.getInsets().top
                    + statusPanel.getInsets().bottom;
            assertTrue(statusPanel.getPreferredSize().height
                            <= tallestSummary + verticalInsets + 4,
                    "normal success must retain status-bar height instead of a banner");

            statusPanel.setSize(1200, statusPanel.getPreferredSize().height);
            layoutRecursively(statusPanel);
            int statusCenterY = SwingUtilities.convertPoint(
                    status, 0, status.getHeight() / 2, statusPanel).y;
            int canvasCenterY = SwingUtilities.convertPoint(
                    canvasStatus, 0, canvasStatus.getHeight() / 2, statusPanel).y;
            assertTrue(Math.abs(statusCenterY - canvasCenterY) <= 2,
                    "model and Canvas summaries must share one horizontal row");
        });
    }

    @Test
    void keepsModelFailureConciseWhileRetainingTargetAndReasonAsMetadata()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JPanel statusPanel = findNamed(
                    visual, JPanel.class, "Flutter Designer model status");
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JLabel model = findNamed(
                    visual, JLabel.class, "Flutter Designer model and source");
            JLabel detail = findNamed(
                    visual, JLabel.class, "Flutter Designer status details");

            publish(design, new FlutterDesignerDocumentState.Failure(
                    "Read Flutter Designer model",
                    "sample.fd",
                    "The file is no longer available"));

            assertEquals("Read Flutter Designer model failed.", status.getText());
            assertTrue(status.isVisible());
            assertFalse(isVisibleWithin(model, statusPanel));
            assertFalse(isVisibleWithin(detail, statusPanel));
            assertEquals(
                    "Target: sample.fd. Reason: The file is no longer available",
                    status.getToolTipText());
            assertEquals(
                    "Read Flutter Designer model failed. " + status.getToolTipText(),
                    statusPanel.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void createsScrollableSelectableReadOnlyCanvasDetails() throws Exception {
        onEdt(() -> {
            FlutterDesignerNativeCanvasStatus state =
                    new FlutterDesignerNativeCanvasStatus(
                            FlutterDesignerNativeCanvasStatus.Stage.UNAVAILABLE,
                            "Native Flutter Canvas is unavailable.",
                            "Target: embedded Windows FlutterView. Reason: Flutter SDK "
                            + "could not be resolved from the configured toolchain.");
            JScrollPane scroll = FlutterDesignerMultiViewDesign
                    .createNativeCanvasDetailsComponent(
                            state, "home.fd", "home.dart");
            JTextArea details = assertInstanceOf(
                    JTextArea.class, scroll.getViewport().getView());

            assertFalse(details.isEditable());
            assertTrue(details.getLineWrap());
            assertTrue(details.getWrapStyleWord());
            assertTrue(details.isFocusable());
            assertEquals(0, details.getCaretPosition());
            assertEquals(
                    "Native Flutter Canvas is unavailable.\n\n"
                    + "Model: home.fd\n"
                    + "Source: home.dart\n\n"
                    + "Target: embedded Windows FlutterView. Reason: Flutter SDK "
                    + "could not be resolved from the configured toolchain.",
                    details.getText());
            assertEquals("Native Flutter Canvas full status details",
                    details.getAccessibleContext().getAccessibleName());
            assertEquals("Native Flutter Canvas details",
                    scroll.getAccessibleContext().getAccessibleName());
            details.selectAll();
            assertEquals(details.getText(), details.getSelectedText());
        });
    }

    @Test
    void createsCompleteScrollablePropertyMutationResultDetails() throws Exception {
        onEdt(() -> {
            FlutterDesignerMutationController.MutationResult result =
                    FlutterDesignerMutationController.MutationResult.rejected(
                            "Set Flutter property",
                            "home.fd — widget 35ca8ca5, property data",
                            "The selected Flutter Designer revision is stale; "
                            + "select the widget again.");

            JScrollPane scroll = FlutterDesignerMultiViewDesign
                    .createMutationResultDetailsComponent(result);
            JTextArea details = assertInstanceOf(
                    JTextArea.class, scroll.getViewport().getView());

            assertFalse(details.isEditable());
            assertTrue(details.getLineWrap());
            assertTrue(details.getWrapStyleWord());
            assertTrue(details.isFocusable());
            assertEquals(0, details.getCaretPosition());
            assertEquals(
                    "Operation: Set Flutter property\n"
                    + "Target: home.fd — widget 35ca8ca5, property data\n"
                    + "Reason: The selected Flutter Designer revision is stale; "
                    + "select the widget again.",
                    details.getText());
            assertEquals("Flutter Designer change details",
                    details.getAccessibleContext().getAccessibleName());
            assertEquals("Flutter Designer change result",
                    scroll.getAccessibleContext().getAccessibleName());
            details.selectAll();
            assertEquals(details.getText(), details.getSelectedText());
        });
    }

    @Test
    void namesResetPropertyMutationInCompleteScrollableResultDetails()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMutationController.MutationResult result =
                    FlutterDesignerMutationController.MutationResult.rejected(
                            "Reset Flutter property",
                            "home.fd — widget 35ca8ca5, property softWrap",
                            "The selected Flutter Designer revision is stale; "
                            + "select the widget again.");

            JScrollPane scroll = FlutterDesignerMultiViewDesign
                    .createMutationResultDetailsComponent(result);
            JTextArea details = assertInstanceOf(
                    JTextArea.class, scroll.getViewport().getView());

            assertFalse(details.isEditable());
            assertTrue(details.getLineWrap());
            assertTrue(details.getWrapStyleWord());
            assertTrue(details.isFocusable());
            assertEquals(0, details.getCaretPosition());
            assertEquals(
                    "Operation: Reset Flutter property\n"
                    + "Target: home.fd — widget 35ca8ca5, property softWrap\n"
                    + "Reason: The selected Flutter Designer revision is stale; "
                    + "select the widget again.",
                    details.getText());
            assertEquals("Flutter Designer change details",
                    details.getAccessibleContext().getAccessibleName());
            assertEquals("Flutter Designer change result",
                    scroll.getAccessibleContext().getAccessibleName());
            details.selectAll();
            assertEquals(details.getText(), details.getSelectedText());
        });
    }

    @Test
    void namesTheDesignSurfaceAndAssociatesStatusWithLoadingProgress() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JToolBar toolbar = (JToolBar) design.getToolbarRepresentation();
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");
            BeanTreeView widgetTree = findNamed(
                    visual,
                    BeanTreeView.class,
                    "Flutter Designer widget tree");
            JComboBox<?> previewMode = findNamed(
                    toolbar,
                    JComboBox.class,
                    "Flutter Canvas preview target");

            assertEquals("Flutter Designer design view",
                    visual.getAccessibleContext().getAccessibleName());
            assertEquals("Flutter Designer toolbar",
                    toolbar.getAccessibleContext().getAccessibleName());
            assertSame(progress, status.getLabelFor());
            assertFalse(progress.isVisible());
            assertNotNull(progress.getAccessibleContext().getAccessibleDescription());
            assertTrue(widgetTree.getAccessibleContext().getAccessibleDescription()
                    .contains("Capability-reviewed properties are writable"));
            assertEquals(8, previewMode.getItemCount());
            assertTrue(previewMode.getMaximumSize().width
                    >= previewMode.getPreferredSize().width);
            assertNotNull(previewMode.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void routesWebPreviewThroughTheNativeCanvasPresentationPipeline()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            try {
                JPanel visual = (JPanel) design.getVisualRepresentation();
                JToolBar toolbar = (JToolBar) design.getToolbarRepresentation();
                publish(design, currentState(List.of()));
                JComboBox<?> previews = findNamed(
                        toolbar,
                        JComboBox.class,
                        "Flutter Canvas preview target");
                int web = java.util.stream.IntStream
                        .range(0, previews.getItemCount())
                        .filter(index -> "Web — 1440×900".equals(
                                previews.getItemAt(index).toString()))
                        .findFirst()
                        .orElseThrow();
                previews.setSelectedIndex(web);

                JLabel canvasStatus = findNamed(
                        visual,
                        JLabel.class,
                        "Native Flutter Canvas status");
                assertEquals("Native Flutter Canvas is unavailable.",
                        canvasStatus.getText());
                assertTrue(canvasStatus.getToolTipText()
                        .contains("owning Flutter project is unavailable"));
                assertFalse(canvasStatus.getToolTipText()
                        .contains("Web Canvas is unavailable"));
            } finally {
                design.componentClosed();
            }
        });
    }

    @Test
    void exactWebRouteFailsClosedWithoutSilentNativeFallback()
            throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(
                            Lookup.EMPTY,
                            () -> true,
                            () -> true,
                            new FlutterDesignerCanvasBackendSelector(true),
                            (backend, ignoredCallbacks) -> {
                                throw new FlutterDesignerCanvasSessionFactory
                                        .CreationException(
                                                backend
                                                        == FlutterDesignerCanvasBackendSelector
                                                                .Backend.EXACT_WEB
                                                                ? "exact Flutter Web Canvas"
                                                                : "test native Canvas",
                                                "simulated unavailable backend");
                            });
            try {
                JPanel visual = (JPanel) design.getVisualRepresentation();
                JToolBar toolbar = (JToolBar) design.getToolbarRepresentation();
                publish(design, currentState(List.of()));
                JComboBox<?> previews = findNamed(
                        toolbar,
                        JComboBox.class,
                        "Flutter Canvas preview target");
                int web = java.util.stream.IntStream
                        .range(0, previews.getItemCount())
                        .filter(index -> "Web — 1440×900".equals(
                                previews.getItemAt(index).toString()))
                        .findFirst()
                        .orElseThrow();
                previews.setSelectedIndex(web);

                JLabel canvasStatus = findNamed(
                        visual,
                        JLabel.class,
                        "Native Flutter Canvas status");
                assertEquals("Exact Flutter Web Canvas is unavailable.",
                        canvasStatus.getText());
                assertTrue(canvasStatus.getToolTipText().contains(
                        "simulated unavailable backend"));
                assertFalse(canvasStatus.getToolTipText().contains(
                        "owning Flutter project is unavailable"));
            } finally {
                design.componentClosed();
            }
        });
    }

    @Test
    void synchronizesPreviewChoicesWithLiveFlutterProjectPlatforms() throws Exception {
        Path root = Files.createDirectory(temporaryDirectory.resolve("desktop_only"));
        Files.createDirectories(root.resolve("lib"));
        Files.createDirectories(root.resolve(".fd_templates"));
        Files.createDirectories(root.resolve("windows"));
        Files.writeString(root.resolve("pubspec.yaml"), """
                name: desktop_only
                dependencies:
                  flutter:
                    sdk: flutter
                """, StandardCharsets.UTF_8);
        Path dartPath = root.resolve("lib/home.dart");
        Path modelPath = root.resolve(".fd_templates/home.fd");
        Files.writeString(dartPath, "class Home {}\n", StandardCharsets.UTF_8);
        Files.writeString(modelPath, "{}\n", StandardCharsets.UTF_8);
        FlutterProject project = FlutterDesignerTestProject.own(root);
        FileUtil.refreshFor(root.toFile());
        FileObject dart = FileUtil.toFileObject(dartPath.toFile());
        assertNotNull(dart);
        FlutterDesignerDataObject dataObject = FlutterDesignerTestProject.dataObject(
                dart, project);
        FlutterProjectPlatformProvider platforms = project.getLookup()
                .lookup(FlutterProjectPlatformProvider.class);
        assertNotNull(platforms);
        var start = FlutterProjectPlatformProvider.class.getDeclaredMethod("start");
        start.setAccessible(true);
        start.invoke(platforms);

        AtomicReference<FlutterDesignerMultiViewDesign> designRef =
                new AtomicReference<>();
        AtomicReference<JComboBox<?>> previewsRef = new AtomicReference<>();
        try {
            onEdt(() -> {
                FlutterDesignerMultiViewDesign design =
                        new FlutterDesignerMultiViewDesign(dataObject.getLookup());
                designRef.set(design);
                JComboBox<?> previews = findNamed(
                        (JToolBar) design.getToolbarRepresentation(),
                        JComboBox.class,
                        "Flutter Canvas preview target");
                previewsRef.set(previews);
                assertPreviewChoices(
                        previews,
                        List.of("Windows Desktop — 1280×800"),
                        "Windows Desktop — 1280×800");
                assertTrue(previews.getAccessibleContext().getAccessibleDescription()
                        .contains("Desktop"));
                assertFalse(previews.getAccessibleContext().getAccessibleDescription()
                        .contains("Mobile,"));
                design.componentOpened();
            });

            project.getProjectDirectory().createFolder("android");
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of(
                            "Android Phone — 390×844",
                            "Android Tablet — 800×1280",
                            "Windows Desktop — 1280×800"),
                    "Windows Desktop — 1280×800"));

            project.getProjectDirectory().createFolder("ios");
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of(
                            "Android Phone — 390×844",
                            "iPhone — 390×844",
                            "Android Tablet — 800×1280",
                            "iPad — 800×1280",
                            "Windows Desktop — 1280×800"),
                    "Windows Desktop — 1280×800"));

            project.getProjectDirectory().getFileObject("windows").delete();
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of(
                            "Android Phone — 390×844",
                            "iPhone — 390×844",
                            "Android Tablet — 800×1280",
                            "iPad — 800×1280"),
                    "Android Phone — 390×844"));

            project.getProjectDirectory().getFileObject("android").delete();
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of("iPhone — 390×844", "iPad — 800×1280"),
                    "iPhone — 390×844"));

            onEdt(() -> designRef.get().componentClosed());
            project.getProjectDirectory().createFolder("web");
            onEdt(() -> assertPreviewChoices(
                    previewsRef.get(),
                    List.of("iPhone — 390×844", "iPad — 800×1280"),
                    "iPhone — 390×844"));
        } finally {
            FlutterDesignerMultiViewDesign design = designRef.get();
            if (design != null) {
                onEdt(() -> design.componentClosed());
            }
            platforms.close();
        }
    }

    @Test
    void longValidatedModelStatusDoesNotLockTheWidgetTreeDivider() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JSplitPane split = findByType(visual, JSplitPane.class);

            publish(design, currentState(List.of()));
            split.setSize(1200, 700);
            split.doLayout();

            assertTrue(split.isContinuousLayout(),
                    "native resize must remain live while the divider is dragged");
            assertTrue(split.getMaximumDividerLocation()
                            > split.getMinimumDividerLocation(),
                    "complete status copy must not consume the divider's resize range");
            split.setDividerLocation(400);
            split.doLayout();
            assertEquals(400, split.getDividerLocation());
        });
    }

    @Test
    void firstReopenedViewNeverPresentsARetainedCurrentState() throws Exception {
        FlutterDesignerDocumentState.Current retained = currentState(List.of());

        FlutterDesignerDocumentState first = FlutterDesignerMultiViewDesign.openingState(
                true, retained, "home_page.fd");
        FlutterDesignerDocumentState clone = FlutterDesignerMultiViewDesign.openingState(
                false, retained, "home_page.fd");

        FlutterDesignerDocumentState.Loading loading = assertInstanceOf(
                FlutterDesignerDocumentState.Loading.class, first);
        assertEquals("home_page.fd", loading.modelFileName());
        assertSame(retained, clone);
    }

    @Test
    void refreshesAccessibleDescriptionsWithEveryRenderedState() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");

            publish(design, new FlutterDesignerDocumentState.Loading("sample.fd"));

            String loadingDetail =
                    "Reading bounded UTF-8 .fd and paired Dart snapshots, validating "
                    + "the model and source structure, generating both managed payloads "
                    + "in memory, and comparing actual, declared and generated SHA-256 "
                    + "values. No files are written.";
            assertTrue(progress.isVisible());
            assertEquals(loadingDetail,
                    progress.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "Loading Flutter Designer model... Model: the .fd model. "
                    + "Source: the paired Dart source. "
                    + loadingDetail,
                    visual.getAccessibleContext().getAccessibleDescription());

            publish(design, new FlutterDesignerDocumentState.Failure(
                    "Read Flutter Designer model",
                    "sample.fd",
                    "The file is no longer available"));

            assertFalse(progress.isVisible());
            assertEquals(
                    "Read Flutter Designer model failed. Target: sample.fd. "
                    + "Reason: The file is no longer available",
                    visual.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "Reason: The file is no longer available",
                    progress.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void exposesConcreteAccessibleReadOnlyAndInputFailureStates() throws Exception {
        onEdt(() -> {
            FlutterDesignerMultiViewDesign design =
                    new FlutterDesignerMultiViewDesign(Lookup.EMPTY);
            JPanel visual = (JPanel) design.getVisualRepresentation();
            JLabel status = findNamed(
                    visual, JLabel.class, "Flutter Designer status");
            JLabel detail = findNamed(
                    visual, JLabel.class, "Flutter Designer status details");
            JProgressBar progress = findNamed(
                    visual,
                    JProgressBar.class,
                    "Flutter Designer model loading progress");
            FdDocumentCodec codec = new FdDocumentCodec();
            FlutterDesignerDocumentState.Current current = currentState(List.of());
            publish(design, current);
            assertEquals("Designer ready.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "The on-disk imports and build regions, the SHA-256 values "
                    + "recorded in the .fd model, and the deterministic generated "
                    + "payloads agree. The validated reviewed model is published to the "
                    + "isolated native Flutter Canvas. Viewport preview, widget-tree "
                    + "selection, the capability-gated Palette and Properties are enabled. "
                    + "Reviewed properties are writable when exact mutation admission is "
                    + "ready; unsupported property slices remain read-only. Palette widget "
                    + "drag-and-drop is unavailable because "
                    + "exact mutation admission, the current rendered presentation, the "
                    + "owning-view AWT drag lifecycle, or the native Canvas drop capability "
                    + "is not ready; non-insertion drag-and-drop commands remain disabled.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.empty()));
            assertEquals("Flutter Designer three-way comparison is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "No bounded deterministic Dart-generation comparison is "
                    + "available for this loaded model.",
                    detail.getAccessibleContext().getAccessibleDescription());

            FdDecodeResult.Current driftDecoded = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    codec.decode(validDocument()
                            .replace("flutter.widgets.SizedBox", "flutter.widgets.Center")
                            .getBytes(StandardCharsets.UTF_8)));
            var driftGeneration = new DartRegionGenerator().generate(
                    driftDecoded.document(), BuiltInWidgetCatalog.getDefault());
            var driftThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    driftDecoded.document().source(),
                    driftGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    driftDecoded,
                    driftGeneration.modelValidation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(driftThreeWay)));
            assertEquals("Flutter Designer generated Dart conflict.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("GENERATED_REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"));

            DartGenerationResult unsupportedGeneration = generationFailure(
                    current.validation(),
                    DartGenerationDiagnosticCode.DART_EXPRESSION_UNSUPPORTED,
                    "/root/properties/value",
                    "Opaque Dart expressions are not generated.");
            var unsupportedThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    current.decoded().document().source(),
                    unsupportedGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(unsupportedThreeWay)));
            assertEquals("Flutter Designer Dart generation is unsupported.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("DART_EXPRESSION_UNSUPPORTED at /root/properties/value"));

            DartGenerationResult unavailableGeneration = generationFailure(
                    current.validation(),
                    DartGenerationDiagnosticCode.OUTPUT_SIZE_LIMIT,
                    "/source/managedRegions",
                    "Generated payload exceeds the configured limit.");
            var unavailableThreeWay = new DartThreeWayIntegrityGate().evaluate(
                    current.sourceIntegrity().orElseThrow(),
                    current.decoded().document().source(),
                    unavailableGeneration);
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    current.sourceIntegrity(),
                    Optional.of(unavailableThreeWay)));
            assertEquals("Flutter Designer three-way comparison is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("OUTPUT_SIZE_LIMIT at /source/managedRegions"));

            DartSourceIntegrityResult conflict = new DartSourceIntegrityScanner().scan(
                    validDartSource().replace("const SizedBox", "const Text")
                            .getBytes(StandardCharsets.UTF_8),
                    current.decoded().document().source());
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(conflict),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source conflict.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"));
            assertFalse(progress.isVisible());

            FdDecodeResult.Current statefulDecoded = assertInstanceOf(
                    FdDecodeResult.Current.class,
                    codec.decode(validDocument()
                            .replace("\"stateless\"", "\"stateful\"")
                            .getBytes(StandardCharsets.UTF_8)));
            DartSourceIntegrityResult mixedUnsupported =
                    new DartSourceIntegrityScanner().scan(
                            validDartSource()
                                    .replace("StatelessWidget", "StatefulWidget")
                                    .replace("const SizedBox", "const Text")
                                    .getBytes(StandardCharsets.UTF_8),
                            statefulDecoded.document().source());
            publish(design, new FlutterDesignerDocumentState.Current(
                    statefulDecoded,
                    new ValidationResult(List.of()),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(mixedUnsupported),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source shape is unsupported.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("STATEFUL_SOURCE_BINDING_UNSUPPORTED at /source/widgetKind"));
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("Additional source diagnostics: REGION_HASH_MISMATCH at "
                            + "/source/managedRegions/build/sha256; region build"),
                    "known conflicts must remain visible behind the primary unsupported cause");

            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(),
                    List.of(),
                    Optional.of(DartSourceIntegrityResult.readFailure("Access denied")),
                    Optional.empty()));
            assertEquals("Flutter Designer Dart source is unavailable.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains("SOURCE_READ_FAILED at /source/dartFile: "
                            + "The paired Dart source could not be read: Access denied"));

            CatalogDiagnostic catalogDiagnostic = new CatalogDiagnostic(
                    CatalogDiagnosticCode.INVALID_CONTRIBUTOR,
                    "com.example.widgets",
                    List.of("com.example"),
                    "The widget contributor could not be loaded.");
            publish(design, new FlutterDesignerDocumentState.Current(
                    current.decoded(),
                    current.validation(),
                    current.catalog(),
                    List.of(catalogDiagnostic),
                    List.of(),
                    current.sourceIntegrity(),
                    current.threeWayIntegrity()));
            assertEquals("Flutter Designer model opened read-only.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals(
                    "INVALID_CONTRIBUTOR for com.example.widgets: "
                    + "The widget contributor could not be loaded.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            FdDecodeResult.UnsupportedNewer future = assertInstanceOf(
                    FdDecodeResult.UnsupportedNewer.class,
                    codec.decode(("{\"format\":\"netbeans-flutter-designer\","
                            + "\"schemaVersion\":11}")
                            .getBytes(StandardCharsets.UTF_8)));
            publish(design, new FlutterDesignerDocumentState.UnsupportedNewer(future));
            assertEquals("Flutter Designer model opened read-only.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals("Schema version 11 is newer than supported version 10.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());

            FdDecodeResult.Invalid invalid = assertInstanceOf(
                    FdDecodeResult.Invalid.class,
                    codec.decode("{".getBytes(StandardCharsets.UTF_8)));
            publish(design, new FlutterDesignerDocumentState.Invalid(invalid));
            assertEquals("Cannot load Flutter Designer model.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertTrue(detail.getAccessibleContext().getAccessibleDescription()
                    .contains(invalid.diagnostics().get(0).code().toString()));
            assertFalse(progress.isVisible());

            publish(design, new FlutterDesignerDocumentState.InputTooLarge(
                    "sample.fd", 257, 256));
            assertEquals("Cannot read Flutter Designer model.",
                    status.getAccessibleContext().getAccessibleDescription());
            assertEquals("File size 257 bytes exceeds the 256 byte safety limit.",
                    detail.getAccessibleContext().getAccessibleDescription());
            assertFalse(progress.isVisible());
        });
    }

    private static String validDocument() {
        return """
                {
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 1,
                  "documentId": "2f04ce87-876a-4f35-8a7c-2fba3e135c7e",
                  "source": {
                    "dartFile": "home_page.dart",
                    "className": "HomePage",
                    "widgetKind": "stateless",
                    "managedRegions": {
                      "imports": {
                        "sha256": "%s"
                      },
                      "build": {
                        "sha256": "%s"
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
                """.formatted(
                DartManagedRegionHashing.normalizedSha256(importsPayload()),
                DartManagedRegionHashing.normalizedSha256(buildPayload()));
    }

    private static FlutterDesignerDocumentState.Current currentState(
            List<CatalogDiagnostic> catalogDiagnostics) throws Exception {
        FdDecodeResult.Current decodedCurrent = assertInstanceOf(
                FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(
                        validDocument().getBytes(StandardCharsets.UTF_8)));
        DartSourceIntegrityScanner sourceScanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult sourceIntegrity = sourceScanner.scan(
                validDartSource().getBytes(StandardCharsets.UTF_8),
                decodedCurrent.document().source());
        var generation = new DartRegionGenerator().generate(
                decodedCurrent.document(), BuiltInWidgetCatalog.getDefault());
        var threeWayIntegrity = new DartThreeWayIntegrityGate(sourceScanner).evaluate(
                sourceIntegrity, decodedCurrent.document().source(), generation);
        return new FlutterDesignerDocumentState.Current(
                decodedCurrent,
                new ValidationResult(List.of()),
                BuiltInWidgetCatalog.getDefault(),
                catalogDiagnostics,
                List.of(),
                Optional.of(sourceIntegrity),
                Optional.of(threeWayIntegrity));
    }

    private static String validDartSource() {
        return "// <netbeans-flutter-designer region=\"imports\">\n"
                + importsPayload()
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + buildPayload()
                + "  // </netbeans-flutter-designer>\n"
                + "}\n";
    }

    private static DartGenerationResult generationFailure(
            ValidationResult validation,
            DartGenerationDiagnosticCode code,
            String path,
            String message) {
        return new DartGenerationResult(
                validation,
                Optional.empty(),
                List.of(new DartGenerationDiagnostic(
                        code,
                        path,
                        Optional.empty(),
                        Optional.empty(),
                        message)));
    }

    private static String importsPayload() {
        return "import 'package:flutter/widgets.dart';\n";
    }

    private static String buildPayload() {
        return "  @override\n"
                + "  Widget build(BuildContext context) {\n"
                + "    return const SizedBox();\n"
                + "  }\n";
    }

    private static void publish(
            FlutterDesignerMultiViewDesign design,
            FlutterDesignerDocumentState state) {
        design.propertyChange(new PropertyChangeEvent(
                design,
                FlutterDesignerDocumentController.PROP_STATE,
                null,
                state));
    }

    private static void renderMutationSnapshot(
            FlutterDesignerMultiViewDesign design,
            FlutterDesignerMutationController.Snapshot snapshot)
            throws ReflectiveOperationException {
        var method = FlutterDesignerMultiViewDesign.class.getDeclaredMethod(
                "renderMutationSnapshot",
                FlutterDesignerMutationController.Snapshot.class);
        method.setAccessible(true);
        method.invoke(design, snapshot);
    }

    private static void assertSingleReadOnlyWidgetNode(
            FlutterDesignerMultiViewDesign design) throws Exception {
        List<? extends Node> nodes = List.copyOf(
                design.getLookup().lookupAll(Node.class));
        assertEquals(1, nodes.size());
        Node widget = assertInstanceOf(
                FlutterWidgetPropertiesNode.class, nodes.get(0));
        assertEquals("SizedBox", widget.getDisplayName());
        for (Node.PropertySet set : widget.getPropertySets()) {
            for (Node.Property<?> property : set.getProperties()) {
                assertFalse(property.canWrite(), property.getName());
            }
        }
    }

    private static void assertPreviewChoices(
            JComboBox<?> previews,
            List<String> expected,
            String selected) {
        assertEquals(expected,
                java.util.stream.IntStream.range(0, previews.getItemCount())
                        .mapToObj(index -> previews.getItemAt(index).toString())
                        .toList());
        assertNotNull(previews.getSelectedItem());
        assertEquals(selected, previews.getSelectedItem().toString());
    }

    private static <T extends Component> T findNamed(
            Container root,
            Class<T> type,
            String accessibleName) {
        for (Component component : root.getComponents()) {
            AccessibleContext context = component.getAccessibleContext();
            if (type.isInstance(component)
                    && context != null
                    && accessibleName.equals(context.getAccessibleName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findNamedOrNull(child, type, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName()
                + " named '" + accessibleName + "'");
    }

    private static <T extends Component> T findByType(
            Container root,
            Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findByTypeOrNull(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " found");
    }

    private static <T extends Component> T findByTypeOrNull(
            Container root,
            Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findByTypeOrNull(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void layoutRecursively(Container root) {
        root.doLayout();
        for (Component component : root.getComponents()) {
            if (component instanceof Container child) {
                layoutRecursively(child);
            }
        }
    }

    private static boolean isVisibleWithin(Component component, Container ancestor) {
        Component cursor = component;
        while (cursor != ancestor) {
            if (cursor == null || !cursor.isVisible()) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return ancestor.isVisible();
    }

    private static <T extends Component> T findNamedOrNull(
            Container root,
            Class<T> type,
            String accessibleName) {
        for (Component component : root.getComponents()) {
            AccessibleContext context = component.getAccessibleContext();
            if (type.isInstance(component)
                    && context != null
                    && accessibleName.equals(context.getAccessibleName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findNamedOrNull(child, type, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("EDT assertion failed", failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
