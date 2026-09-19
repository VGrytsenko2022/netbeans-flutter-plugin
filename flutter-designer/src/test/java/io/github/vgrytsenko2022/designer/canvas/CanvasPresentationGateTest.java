package io.github.vgrytsenko2022.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetCatalog;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.validation.ValidationLimits;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CanvasPresentationGateTest {
    private static final CanvasSessionId SESSION_A = CanvasSessionId.parse(
            "80ef60ed-b108-4674-99a6-c1f3102f01ab");
    private static final CanvasSessionId SESSION_B = CanvasSessionId.parse(
            "0d236d13-7048-4bc6-a11f-bf03a85d1724");
    private static final StableId DOCUMENT_A = StableId.parse(
            "83ed3c05-88e7-4220-8377-29fa1f21a99e");
    private static final StableId DOCUMENT_B = StableId.parse(
            "32e43896-5be1-46ad-9e29-b9bf976da690");
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();

    @Test
    void issuesFreshPresentationIdentityAcrossUndoRedoAndPreviewModes() {
        DesignerDocument document = document(DOCUMENT_A);
        CanvasPresentationGate gate = new CanvasPresentationGate(SESSION_A, DOCUMENT_A);

        CanvasRenderRequest initial = gate.present(
                profile(CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID),
                snapshot(7, document));
        CanvasRenderRequest undo = gate.present(
                profile(CanvasPreviewMode.TABLET, CanvasTargetPlatform.IOS),
                snapshot(3, document));
        CanvasRenderRequest redo = gate.present(
                profile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.WINDOWS),
                snapshot(7, document));
        CanvasRenderRequest web = gate.present(
                profile(CanvasPreviewMode.WEB, CanvasTargetPlatform.WEB),
                snapshot(7, document));

        assertEquals(0, initial.revisionKey().presentationSequence());
        assertEquals(1, undo.revisionKey().presentationSequence());
        assertEquals(2, redo.revisionKey().presentationSequence());
        assertEquals(3, web.revisionKey().presentationSequence());
        assertEquals(7, initial.revisionKey().logicalRevisionId());
        assertEquals(3, undo.revisionKey().logicalRevisionId());
        assertEquals(7, redo.revisionKey().logicalRevisionId());
        assertEquals(CanvasPreviewMode.MOBILE,
                initial.renderProfile().previewMode());
        assertEquals(CanvasPreviewMode.TABLET,
                undo.renderProfile().previewMode());
        assertEquals(CanvasPreviewMode.DESKTOP,
                redo.renderProfile().previewMode());
        assertEquals(CanvasPreviewMode.WEB,
                web.renderProfile().previewMode());
        assertSame(document, web.snapshot().document());
        assertSame(CATALOG, web.snapshot().catalog());
        assertTrue(web.snapshot().validation().valid());
        assertEquals(Optional.of(web.revisionKey()), gate.currentRevision());
        assertEquals(Optional.empty(), gate.currentFrame());
        assertEquals(Optional.empty(), gate.currentLayout());
    }

    @Test
    void documentGateCanContinueASessionOwnedPresentationSequence() {
        DesignerDocument document = document(DOCUMENT_B);
        CanvasPresentationGate gate = new CanvasPresentationGate(
                SESSION_A, DOCUMENT_B, 41);

        CanvasRenderRequest request = gate.present(
                profile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.WINDOWS),
                snapshot(9, document));

        assertEquals(41, request.revisionKey().presentationSequence());
        assertEquals(DOCUMENT_B, request.revisionKey().documentId());
        assertThrows(IllegalArgumentException.class, () ->
                new CanvasPresentationGate(SESSION_A, DOCUMENT_B, -1));
        CanvasPresentationGate exhausted = new CanvasPresentationGate(
                SESSION_A, DOCUMENT_B, Long.MAX_VALUE);
        assertThrows(IllegalStateException.class, () -> exhausted.present(
                profile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.WINDOWS),
                snapshot(10, document)));
    }

    @Test
    void acceptsOnlyContiguousFramesLayoutsAndExactLatestSelection() {
        DesignerDocument document = document(DOCUMENT_A);
        CanvasPresentationGate gate = new CanvasPresentationGate(SESSION_A, DOCUMENT_A);
        CanvasRevisionKey revision = gate.present(
                profile(CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID),
                snapshot(0, document)).revisionKey();
        CanvasFrameKey frameZero = new CanvasFrameKey(revision, 0);
        CanvasFrameKey frameOne = new CanvasFrameKey(revision, 1);
        CanvasFrameKey frameTwo = new CanvasFrameKey(revision, 2);
        CanvasLayoutKey layoutZero = new CanvasLayoutKey(frameZero, 0);
        CanvasLayoutKey layoutZeroOne = new CanvasLayoutKey(frameZero, 1);
        CanvasLayoutKey layoutZeroJump = new CanvasLayoutKey(frameZero, 3);
        CanvasLayoutKey layoutOne = new CanvasLayoutKey(frameOne, 0);

        assertEquals(CanvasAdmission.NOT_READY, gate.admitSelection(layoutZero));
        assertEquals(CanvasAdmission.OUT_OF_ORDER_FRAME, gate.admitFrame(frameTwo));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitFrame(frameZero));
        assertEquals(CanvasAdmission.NOT_READY, gate.admitSelection(layoutZero));
        assertEquals(CanvasAdmission.OUT_OF_ORDER_LAYOUT,
                gate.admitLayout(layoutZeroJump));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitLayout(layoutZero));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitSelection(layoutZero));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitLayout(layoutZeroOne));
        assertEquals(CanvasAdmission.STALE_LAYOUT,
                gate.admitSelection(layoutZero));
        assertEquals(CanvasAdmission.ACCEPTED,
                gate.admitSelection(layoutZeroOne));
        assertEquals(CanvasAdmission.STALE_FRAME, gate.admitFrame(frameZero));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitFrame(frameOne));
        assertEquals(CanvasAdmission.STALE_FRAME,
                gate.admitSelection(layoutZeroOne));
        assertEquals(CanvasAdmission.NOT_READY, gate.admitSelection(layoutOne));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitLayout(layoutOne));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitSelection(layoutOne));
        assertEquals(Optional.of(frameOne), gate.currentFrame());
        assertEquals(Optional.of(layoutOne), gate.currentLayout());
    }

    @Test
    void newPresentationRejectsDelayedAbaFramesFromTheSameLogicalRevision() {
        DesignerDocument document = document(DOCUMENT_A);
        CanvasPresentationGate gate = new CanvasPresentationGate(SESSION_A, DOCUMENT_A);
        CanvasRevisionKey firstRevision = gate.present(
                profile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.WINDOWS),
                snapshot(5, document)).revisionKey();
        CanvasFrameKey firstFrame = new CanvasFrameKey(firstRevision, 0);
        CanvasLayoutKey firstLayout = new CanvasLayoutKey(firstFrame, 0);
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitFrame(firstFrame));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitLayout(firstLayout));

        CanvasRevisionKey revisitedRevision = gate.present(
                profile(CanvasPreviewMode.DESKTOP, CanvasTargetPlatform.WINDOWS),
                snapshot(5, document)).revisionKey();
        CanvasFrameKey revisitedFrame = new CanvasFrameKey(revisitedRevision, 0);
        CanvasLayoutKey revisitedLayout = new CanvasLayoutKey(revisitedFrame, 0);

        assertEquals(CanvasAdmission.STALE_REVISION, gate.admitFrame(firstFrame));
        assertEquals(CanvasAdmission.STALE_REVISION,
                gate.admitSelection(firstLayout));
        assertEquals(CanvasAdmission.NOT_READY,
                gate.admitSelection(revisitedLayout));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitFrame(revisitedFrame));
        assertEquals(CanvasAdmission.ACCEPTED, gate.admitLayout(revisitedLayout));
        assertEquals(CanvasAdmission.ACCEPTED,
                gate.admitSelection(revisitedLayout));
    }

    @Test
    void atomicallyAdmitsOnlyAnExactInitialFrameAndLayoutPair() {
        DesignerDocument document = document(DOCUMENT_A);
        CanvasPresentationGate gate = new CanvasPresentationGate(SESSION_A, DOCUMENT_A);
        CanvasRevisionKey revision = gate.present(
                profile(CanvasPreviewMode.WEB, CanvasTargetPlatform.WEB),
                snapshot(0, document)).revisionKey();
        CanvasFrameKey frameZero = new CanvasFrameKey(revision, 0);
        CanvasFrameKey frameOne = new CanvasFrameKey(revision, 1);

        assertThrows(IllegalArgumentException.class, () -> gate.admitPresentation(
                frameZero, new CanvasLayoutKey(frameOne, 0)));
        assertEquals(CanvasAdmission.OUT_OF_ORDER_LAYOUT,
                gate.admitPresentation(
                        frameZero, new CanvasLayoutKey(frameZero, 1)));
        assertEquals(Optional.empty(), gate.currentFrame());
        assertEquals(Optional.empty(), gate.currentLayout());

        CanvasLayoutKey exactLayout = new CanvasLayoutKey(frameZero, 0);
        assertEquals(CanvasAdmission.ACCEPTED,
                gate.admitPresentation(frameZero, exactLayout));
        assertEquals(Optional.of(frameZero), gate.currentFrame());
        assertEquals(Optional.of(exactLayout), gate.currentLayout());

        CanvasFrameKey jump = new CanvasFrameKey(revision, Long.MAX_VALUE);
        assertEquals(CanvasAdmission.OUT_OF_ORDER_FRAME,
                gate.admitPresentation(jump, new CanvasLayoutKey(jump, 0)));
        assertEquals(Optional.of(frameZero), gate.currentFrame());
        assertEquals(Optional.of(exactLayout), gate.currentLayout());
    }

    @Test
    void sessionDocumentAndCloseFenceLateRuntimeWork() {
        DesignerDocument document = document(DOCUMENT_A);
        CanvasPresentationGate gate = new CanvasPresentationGate(SESSION_A, DOCUMENT_A);
        CanvasRevisionKey current = gate.present(
                profile(CanvasPreviewMode.WEB, CanvasTargetPlatform.WEB),
                snapshot(0, document)).revisionKey();

        CanvasRevisionKey foreignSession = new CanvasRevisionKey(
                SESSION_B,
                current.presentationSequence(),
                DOCUMENT_A,
                current.logicalRevisionId());
        CanvasRevisionKey foreignDocument = new CanvasRevisionKey(
                SESSION_A,
                current.presentationSequence(),
                DOCUMENT_B,
                current.logicalRevisionId());

        assertEquals(CanvasAdmission.STALE_SESSION,
                gate.admitFrame(new CanvasFrameKey(foreignSession, 0)));
        assertEquals(CanvasAdmission.STALE_DOCUMENT,
                gate.admitFrame(new CanvasFrameKey(foreignDocument, 0)));

        gate.close();
        gate.close();

        assertTrue(gate.closed());
        assertEquals(Optional.empty(), gate.currentRevision());
        assertEquals(Optional.empty(), gate.currentFrame());
        assertEquals(Optional.empty(), gate.currentLayout());
        assertEquals(CanvasAdmission.CLOSED,
                gate.admitFrame(new CanvasFrameKey(current, 0)));
        assertEquals(CanvasAdmission.CLOSED,
                gate.admitSelection(new CanvasLayoutKey(
                        new CanvasFrameKey(current, 0), 0)));
        assertThrows(IllegalStateException.class, () -> gate.present(
                profile(CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID),
                snapshot(1, document)));
    }

    @Test
    void rejectsInvalidOrMismatchedProtocolValues() {
        DesignerDocument documentA = document(DOCUMENT_A);
        DesignerDocument documentB = document(DOCUMENT_B);
        CanvasRevisionKey key = new CanvasRevisionKey(SESSION_A, 0, DOCUMENT_A, 0);
        CanvasPresentationGate gate = new CanvasPresentationGate(SESSION_A, DOCUMENT_A);
        ValidatedCanvasRevisionSnapshot snapshotA = snapshot(0, documentA);
        ValidatedCanvasRevisionSnapshot snapshotB = snapshot(0, documentB);

        assertThrows(IllegalArgumentException.class,
                () -> new CanvasRevisionKey(SESSION_A, -1, DOCUMENT_A, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasRevisionKey(SESSION_A, 0, DOCUMENT_A, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasFrameKey(key, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasRenderRequest(
                        key,
                        profile(CanvasPreviewMode.MOBILE,
                                CanvasTargetPlatform.ANDROID),
                        snapshotB));
        assertThrows(IllegalArgumentException.class,
                () -> new CanvasRenderRequest(
                        new CanvasRevisionKey(SESSION_A, 0, DOCUMENT_A, 1),
                        profile(CanvasPreviewMode.MOBILE,
                                CanvasTargetPlatform.ANDROID),
                        snapshotA));
        assertThrows(IllegalArgumentException.class, () -> gate.present(
                profile(CanvasPreviewMode.MOBILE, CanvasTargetPlatform.ANDROID),
                snapshotB));
        assertThrows(NullPointerException.class, () -> gate.present(
                null, snapshotA));
        assertThrows(IllegalArgumentException.class,
                () -> CanvasSessionId.parse(SESSION_A.toString().toUpperCase()));

        assertFalse(gate.closed());
    }

    private static DesignerDocument document(StableId documentId) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        DartSourceDescriptor source = new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("0.1.3-SNAPSHOT"),
                new ManagedRegions(region, region));
        WidgetNode root = WidgetNode.empty(
                StableId.parse("5b814fc1-ecc1-4255-898d-3111f10673a4"),
                new WidgetTypeId("flutter.material.Scaffold"));
        return new DesignerDocument(documentId, source, root);
    }

    private static ValidatedCanvasRevisionSnapshot snapshot(
            long logicalRevisionId,
            DesignerDocument document) {
        return ValidatedCanvasRevisionSnapshot.validate(
                logicalRevisionId,
                document,
                CATALOG,
                ValidationLimits.defaults());
    }

    private static CanvasRenderProfile profile(
            CanvasPreviewMode previewMode,
            CanvasTargetPlatform targetPlatform) {
        return new CanvasRenderProfile(
                previewMode,
                targetPlatform,
                new CanvasViewport(390.0d, 844.0d),
                new CanvasDevicePixelRatio(3.0d),
                new CanvasResolvedTheme(
                        "material_light_default_v1",
                        0xFF6750A4,
                        CanvasThemeBrightness.LIGHT,
                        "A".repeat(64)),
                new CanvasLocale("uk-UA"),
                new CanvasTextScaleFactor(1.0d),
                new CanvasEngineIdentity(
                        "3.44.8",
                        "framework-revision",
                        "engine-revision",
                        "3.12.0"));
    }
}
