package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.DartParameter;
import dev.flutter.netbeans.designer.catalog.PaletteMetadata;
import dev.flutter.netbeans.designer.catalog.SlotAcceptance;
import dev.flutter.netbeans.designer.catalog.SlotDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdCodecLimits;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartGenerationLimits;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ColorSource;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.SlotCardinality;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationPlanner;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.validation.ValidationLimits;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignerCommandSessionTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId DOCUMENT_ID = id("00000000-0000-4000-8000-000000000001");
    private static final StableId ROOT_ID = id("00000000-0000-4000-8000-000000000010");
    private static final StableId FIRST_ID = id("00000000-0000-4000-8000-000000000011");
    private static final StableId SECOND_ID = id("00000000-0000-4000-8000-000000000012");
    private static final StableId THIRD_ID = id("00000000-0000-4000-8000-000000000013");
    private static final StableId WRAPPER_ID = id("00000000-0000-4000-8000-000000000014");
    private static final PropertyName DATA = property("data");
    private static final PropertyName MAX_LINES = property("maxLines");
    private static final PropertyName WIDTH = property("width");
    private static final PropertyName HEIGHT = property("height");
    private static final PropertyName ASPECT_RATIO = property("aspectRatio");
    private static final PropertyName OPACITY = property("opacity");
    private static final PropertyName ALWAYS_INCLUDE_SEMANTICS =
            property("alwaysIncludeSemantics");
    private static final PropertyName ALIGNMENT = property("alignment");
    private static final PropertyName WIDTH_FACTOR = property("widthFactor");
    private static final PropertyName HEIGHT_FACTOR = property("heightFactor");
    private static final PropertyName TEXT_DIRECTION = property("textDirection");
    private static final PropertyName FIT = property("fit");
    private static final PropertyName COLOR = property("color");
    private static final PropertyName DECORATION = property("decoration");
    private static final PropertyName CLIP_BEHAVIOR = property("clipBehavior");
    private static final SlotName CHILDREN = slot("children");
    private static final SlotName CHILD = slot("child");

    @Test
    void opensExactCatalogBoundSessionAndApplyUndoRedoAreByteExact() throws Exception {
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DesignerCommandSessionOpenResult opened = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG);

        assertTrue(opened.ready());
        DesignerCommandSession initial = opened.session().orElseThrow();
        assertSame(CATALOG, initial.catalog());
        assertFalse(initial.dirty());
        assertEquals(0, initial.cursor());
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                initial.current().persistenceKind());

        AddWidget command = new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 1),
                text(THIRD_ID, "third"));
        DesignerCommandSessionResult appliedResult = initial.apply(command);

        assertEquals(DesignerCommandStatus.APPLIED, appliedResult.status());
        DesignerCommandSession applied = appliedResult.session();
        DesignerCommandEdit edit = appliedResult.edit().orElseThrow();
        assertSame(command, edit.forward());
        assertSame(initial.current(), edit.beforeRevision());
        assertSame(applied.current(), edit.afterRevision());
        assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                applied.current().persistenceKind());
        assertTrue(applied.current().preparedPair().isPresent());
        assertTrue(applied.dirty());
        assertEquals(Optional.of(edit), applied.undoEdit());
        assertTrue(edit.inverse().canApplyTo(applied.current().document()));
        assertEquals(initial.current().document(),
                edit.inverse().applyTo(applied.current().document()));
        assertThrows(IllegalStateException.class,
                () -> edit.inverse().applyTo(initial.current().document()));

        DesignerCommandSessionResult undoneResult = applied.undo();
        DesignerCommandSession undone = undoneResult.session();
        assertEquals(DesignerCommandStatus.UNDONE, undoneResult.status());
        assertFalse(undone.dirty());
        assertArrayEquals(fixture.fd().copyBytes(), undone.current().fdBytes());
        assertArrayEquals(fixture.dart(), undone.current().dartCandidateBytes());
        assertEquals(Optional.of(edit), undone.redoEdit());

        DesignerCommandSession redone = undone.redo().session();
        assertEquals(applied.current().document(), redone.current().document());
        assertArrayEquals(applied.current().fdBytes(), redone.current().fdBytes());
        assertArrayEquals(applied.current().dartCandidateBytes(),
                redone.current().dartCandidateBytes());
        assertEquals(applied.current().generation(), redone.current().generation());
    }

    @Test
    void addRemoveMoveWrapAndPropertyCommandsChainAsSemanticActions() throws Exception {
        DesignerCommandSession session = session(
                fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second")));

        session = applied(session, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 2),
                text(THIRD_ID, "third")));
        assertEquals(List.of(FIRST_ID, SECOND_ID, THIRD_ID), rootChildIds(session));

        session = applied(session, new MoveWidget(
                THIRD_ID, new WidgetPlacement(ROOT_ID, CHILDREN, 0)));
        assertEquals(List.of(THIRD_ID, FIRST_ID, SECOND_ID), rootChildIds(session));

        session = applied(session, new WrapWidget(
                THIRD_ID,
                WidgetNode.empty(WRAPPER_ID, type("flutter.widgets.Center")),
                CHILD,
                0));
        WidgetNode wrapper = rootChildren(session).getFirst();
        assertEquals(WRAPPER_ID, wrapper.id());
        assertEquals(THIRD_ID,
                ((WidgetSlot.SingleSlot) wrapper.slots().get(CHILD))
                        .child().orElseThrow().id());

        session = applied(session, new SetProperty(
                THIRD_ID, DATA, new PropertyValue.StringValue("changed")));
        assertTrue(new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8)
                .contains("'changed'"));

        session = applied(session, new SetProperty(
                THIRD_ID,
                MAX_LINES,
                new PropertyValue.IntegerValue(BigInteger.valueOf(3))));
        session = applied(session, new ResetProperty(THIRD_ID, MAX_LINES));
        WidgetNode changedText = find(session.current().document().root(), THIRD_ID);
        assertFalse(changedText.properties().containsKey(MAX_LINES));

        session = applied(session, new RemoveWidget(WRAPPER_ID));
        assertEquals(List.of(FIRST_ID, SECOND_ID), rootChildIds(session));
        assertEquals(7, session.cursor());
        assertEquals(8, session.revisionCount());
    }

    @Test
    void sizedBoxPropertyChildSaveAndReopenLifecycleIsByteExact() throws Exception {
        DesignerCommandSession initial = session(fixture());
        WidgetNode sizedBox = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.SizedBox")).orElseThrow(),
                WRAPPER_ID);

        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), sizedBox));
        DesignerCommandSession widthSet = applied(added, new SetProperty(
                WRAPPER_ID,
                WIDTH,
                new PropertyValue.IntegerValue(BigInteger.valueOf(320))));
        DesignerCommandSession dimensionsSet = applied(widthSet, new SetProperty(
                WRAPPER_ID,
                HEIGHT,
                new PropertyValue.DoubleValue(new BigDecimal("180.5"))));

        DesignerCommandSession widthReset = applied(dimensionsSet, new ResetProperty(
                WRAPPER_ID, WIDTH));
        assertFalse(find(widthReset.current().document().root(), WRAPPER_ID)
                .properties().containsKey(WIDTH));
        DesignerCommandSession dimensionsRestored = widthReset.undo().session();
        assertArrayEquals(dimensionsSet.current().fdBytes(),
                dimensionsRestored.current().fdBytes());
        DesignerCommandSession resetRedone = dimensionsRestored.redo().session();
        assertArrayEquals(widthReset.current().fdBytes(),
                resetRedone.current().fdBytes());
        dimensionsRestored = resetRedone.undo().session();

        DesignerCommandSession childAdded = applied(dimensionsRestored, new AddWidget(
                new WidgetPlacement(WRAPPER_ID, CHILD, 0),
                text(THIRD_ID, "Inside")));
        WidgetNode finalSizedBox = find(
                childAdded.current().document().root(), WRAPPER_ID);
        assertEquals(new PropertyValue.IntegerValue(BigInteger.valueOf(320)),
                finalSizedBox.properties().get(WIDTH));
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("180.5")),
                finalSizedBox.properties().get(HEIGHT));
        assertEquals(THIRD_ID,
                ((WidgetSlot.SingleSlot) finalSizedBox.slots().get(CHILD))
                        .child().orElseThrow().id());
        String dart = new String(
                childAdded.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("const SizedBox("), dart);
        assertTrue(dart.contains("width: 320"), dart);
        assertTrue(dart.contains("height: 180.5"), dart);
        assertTrue(dart.contains("child: const Text('Inside')"), dart);

        DesignerCommandSession saved = childAdded.markSaved();
        assertFalse(saved.dirty());
        DesignerCommandSession childUndone = saved.undo().session();
        assertTrue(childUndone.dirty());
        assertTrue(((WidgetSlot.SingleSlot) find(
                childUndone.current().document().root(), WRAPPER_ID)
                .slots().get(CHILD)).child().isEmpty());
        DesignerCommandSession childRedone = childUndone.redo().session();
        assertFalse(childRedone.dirty());
        assertArrayEquals(saved.current().fdBytes(), childRedone.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                childRedone.current().dartCandidateBytes());

        OriginalFdBytes reopenedFd = OriginalFdBytes.copyOf(
                saved.current().fdBytes(), FdCodecLimits.defaults());
        byte[] reopenedDart = saved.current().dartCandidateBytes();
        DesignerCommandSessionOpenResult reopenedResult = DesignerCommandSession.open(
                reopenedFd, reopenedDart, CATALOG);
        assertTrue(reopenedResult.ready(), () -> reopenedResult.diagnostics().toString());
        DesignerCommandSession reopened = reopenedResult.session().orElseThrow();
        assertFalse(reopened.dirty());
        assertEquals(saved.current().document(), reopened.current().document());
        assertArrayEquals(saved.current().fdBytes(), reopened.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                reopened.current().dartCandidateBytes());
    }

    @Test
    void aspectRatioPrototypeEditChildAndReopenLifecycleIsByteExact()
            throws Exception {
        DesignerCommandSession initial = session(fixture());
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.AspectRatio")).orElseThrow(),
                WRAPPER_ID);
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ONE),
                prototype.properties().get(ASPECT_RATIO));

        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), prototype));
        DesignerCommandSession childAdded = applied(added, new AddWidget(
                new WidgetPlacement(WRAPPER_ID, CHILD, 0),
                text(THIRD_ID, "Inside")));
        DesignerCommandSession edited = applied(childAdded, new SetProperty(
                WRAPPER_ID,
                ASPECT_RATIO,
                new PropertyValue.DoubleValue(new BigDecimal("1.5"))));

        assertRejected(edited, new SetProperty(
                WRAPPER_ID,
                ASPECT_RATIO,
                new PropertyValue.DoubleValue(BigDecimal.ZERO)),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        WidgetNode finalAspectRatio = find(
                edited.current().document().root(), WRAPPER_ID);
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("1.5")),
                finalAspectRatio.properties().get(ASPECT_RATIO));
        assertEquals(THIRD_ID,
                ((WidgetSlot.SingleSlot) finalAspectRatio.slots().get(CHILD))
                        .child().orElseThrow().id());

        String dart = new String(
                edited.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("const AspectRatio("), dart);
        assertTrue(dart.contains("aspectRatio: 1.5"), dart);
        assertTrue(dart.contains("child: const Text('Inside')"), dart);

        DesignerCommandSession saved = edited.markSaved();
        OriginalFdBytes reopenedFd = OriginalFdBytes.copyOf(
                saved.current().fdBytes(), FdCodecLimits.defaults());
        DesignerCommandSessionOpenResult reopenedResult = DesignerCommandSession.open(
                reopenedFd, saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(reopenedResult.ready(), () -> reopenedResult.diagnostics().toString());
        DesignerCommandSession reopened = reopenedResult.session().orElseThrow();
        assertEquals(saved.current().document(), reopened.current().document());
        assertArrayEquals(saved.current().fdBytes(), reopened.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                reopened.current().dartCandidateBytes());
    }

    @Test
    void opacityPrototypeEditResetChildUndoRedoAndReopenAreByteExact()
            throws Exception {
        DesignerCommandSession initial = session(fixture());
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.Opacity")).orElseThrow(),
                WRAPPER_ID);
        assertEquals(new PropertyValue.DoubleValue(BigDecimal.ONE),
                prototype.properties().get(OPACITY));
        assertFalse(prototype.properties().containsKey(ALWAYS_INCLUDE_SEMANTICS));

        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), prototype));
        DesignerCommandSession childAdded = applied(added, new AddWidget(
                new WidgetPlacement(WRAPPER_ID, CHILD, 0),
                text(THIRD_ID, "Inside")));
        DesignerCommandSession faded = applied(childAdded, new SetProperty(
                WRAPPER_ID,
                OPACITY,
                new PropertyValue.DoubleValue(new BigDecimal("0.5"))));
        DesignerCommandSession semantic = applied(faded, new SetProperty(
                WRAPPER_ID,
                ALWAYS_INCLUDE_SEMANTICS,
                new PropertyValue.BooleanValue(true)));

        assertRejected(semantic, new SetProperty(
                WRAPPER_ID,
                OPACITY,
                new PropertyValue.DoubleValue(new BigDecimal("1.01"))),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        DesignerCommandSession reset = applied(semantic, new ResetProperty(
                WRAPPER_ID, ALWAYS_INCLUDE_SEMANTICS));
        WidgetNode finalOpacity = find(reset.current().document().root(), WRAPPER_ID);
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("0.5")),
                finalOpacity.properties().get(OPACITY));
        assertFalse(finalOpacity.properties().containsKey(ALWAYS_INCLUDE_SEMANTICS));
        assertEquals(THIRD_ID,
                ((WidgetSlot.SingleSlot) finalOpacity.slots().get(CHILD))
                        .child().orElseThrow().id());

        String dart = new String(
                reset.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("const Opacity("), dart);
        assertTrue(dart.contains("opacity: 0.5"), dart);
        assertFalse(dart.contains("alwaysIncludeSemantics:"), dart);
        assertTrue(dart.contains("child: const Text('Inside')"), dart);

        DesignerCommandSession semanticRestored = reset.undo().session();
        assertEquals(new PropertyValue.BooleanValue(true),
                find(semanticRestored.current().document().root(), WRAPPER_ID)
                        .properties().get(ALWAYS_INCLUDE_SEMANTICS));
        DesignerCommandSession resetRedone = semanticRestored.redo().session();
        assertArrayEquals(reset.current().fdBytes(), resetRedone.current().fdBytes());
        assertArrayEquals(reset.current().dartCandidateBytes(),
                resetRedone.current().dartCandidateBytes());

        DesignerCommandSession saved = reset.markSaved();
        OriginalFdBytes reopenedFd = OriginalFdBytes.copyOf(
                saved.current().fdBytes(), FdCodecLimits.defaults());
        DesignerCommandSessionOpenResult reopenedResult = DesignerCommandSession.open(
                reopenedFd, saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(reopenedResult.ready(), () -> reopenedResult.diagnostics().toString());
        DesignerCommandSession reopened = reopenedResult.session().orElseThrow();
        assertEquals(saved.current().document(), reopened.current().document());
        assertArrayEquals(saved.current().fdBytes(), reopened.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                reopened.current().dartCandidateBytes());
    }

    @Test
    void alignPrototypeEditResetChildUndoRedoAndReopenAreByteExact()
            throws Exception {
        DesignerCommandSession initial = session(fixture());
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.Align")).orElseThrow(),
                WRAPPER_ID);
        assertTrue(prototype.properties().isEmpty());
        assertTrue(((WidgetSlot.SingleSlot) prototype.slots().get(CHILD))
                .child().isEmpty());

        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), prototype));
        DesignerCommandSession childAdded = applied(added, new AddWidget(
                new WidgetPlacement(WRAPPER_ID, CHILD, 0),
                text(THIRD_ID, "Inside")));
        DesignerCommandSession aligned = applied(childAdded, new SetProperty(
                WRAPPER_ID,
                ALIGNMENT,
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE.negate(),
                        BigDecimal.ONE)));
        DesignerCommandSession widthSet = applied(aligned, new SetProperty(
                WRAPPER_ID,
                WIDTH_FACTOR,
                new PropertyValue.IntegerValue(BigInteger.ZERO)));
        DesignerCommandSession heightSet = applied(widthSet, new SetProperty(
                WRAPPER_ID,
                HEIGHT_FACTOR,
                new PropertyValue.DoubleValue(new BigDecimal("1.5"))));

        assertRejected(heightSet, new SetProperty(
                WRAPPER_ID,
                WIDTH_FACTOR,
                new PropertyValue.IntegerValue(BigInteger.ONE.negate())),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejected(heightSet, new SetProperty(
                WRAPPER_ID,
                HEIGHT_FACTOR,
                new PropertyValue.DoubleValue(new BigDecimal("-0.001"))),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);

        DesignerCommandSession reset = applied(heightSet, new ResetProperty(
                WRAPPER_ID, WIDTH_FACTOR));
        WidgetNode finalAlign = find(reset.current().document().root(), WRAPPER_ID);
        assertEquals(new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE.negate(),
                        BigDecimal.ONE),
                finalAlign.properties().get(ALIGNMENT));
        assertFalse(finalAlign.properties().containsKey(WIDTH_FACTOR));
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("1.5")),
                finalAlign.properties().get(HEIGHT_FACTOR));
        assertEquals(THIRD_ID,
                ((WidgetSlot.SingleSlot) finalAlign.slots().get(CHILD))
                        .child().orElseThrow().id());

        String dart = new String(
                reset.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("const Align("), dart);
        assertTrue(dart.contains(
                "alignment: const AlignmentDirectional(-1.0, 1.0)"), dart);
        assertFalse(dart.contains("widthFactor:"), dart);
        assertTrue(dart.contains("heightFactor: 1.5"), dart);
        assertTrue(dart.contains("child: const Text('Inside')"), dart);

        DesignerCommandSession widthRestored = reset.undo().session();
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),
                find(widthRestored.current().document().root(), WRAPPER_ID)
                        .properties().get(WIDTH_FACTOR));
        DesignerCommandSession resetRedone = widthRestored.redo().session();
        assertArrayEquals(reset.current().fdBytes(), resetRedone.current().fdBytes());
        assertArrayEquals(reset.current().dartCandidateBytes(),
                resetRedone.current().dartCandidateBytes());

        DesignerCommandSession saved = reset.markSaved();
        OriginalFdBytes reopenedFd = OriginalFdBytes.copyOf(
                saved.current().fdBytes(), FdCodecLimits.defaults());
        DesignerCommandSessionOpenResult reopenedResult = DesignerCommandSession.open(
                reopenedFd, saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(reopenedResult.ready(), () -> reopenedResult.diagnostics().toString());
        DesignerCommandSession reopened = reopenedResult.session().orElseThrow();
        assertEquals(saved.current().document(), reopened.current().document());
        assertArrayEquals(saved.current().fdBytes(), reopened.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                reopened.current().dartCandidateBytes());
    }

    @Test
    void fractionallySizedBoxEditResetChildUndoRedoAndReopenAreByteExact()
            throws Exception {
        DesignerCommandSession initial = session(fixture());
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.FractionallySizedBox"))
                        .orElseThrow(),
                WRAPPER_ID);
        assertTrue(prototype.properties().isEmpty());
        assertTrue(((WidgetSlot.SingleSlot) prototype.slots().get(CHILD))
                .child().isEmpty());

        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), prototype));
        DesignerCommandSession childAdded = applied(added, new AddWidget(
                new WidgetPlacement(WRAPPER_ID, CHILD, 0),
                text(THIRD_ID, "Inside fraction")));
        DesignerCommandSession aligned = applied(childAdded, new SetProperty(
                WRAPPER_ID,
                ALIGNMENT,
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE.negate(),
                        BigDecimal.ONE)));
        DesignerCommandSession widthSet = applied(aligned, new SetProperty(
                WRAPPER_ID,
                WIDTH_FACTOR,
                new PropertyValue.IntegerValue(BigInteger.ZERO)));
        DesignerCommandSession heightSet = applied(widthSet, new SetProperty(
                WRAPPER_ID,
                HEIGHT_FACTOR,
                new PropertyValue.DoubleValue(new BigDecimal("1.25"))));

        assertRejected(heightSet, new SetProperty(
                WRAPPER_ID,
                WIDTH_FACTOR,
                new PropertyValue.IntegerValue(BigInteger.ONE.negate())),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejected(heightSet, new SetProperty(
                WRAPPER_ID,
                HEIGHT_FACTOR,
                new PropertyValue.DoubleValue(new BigDecimal("-0.001"))),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);

        DesignerCommandSession reset = applied(heightSet, new ResetProperty(
                WRAPPER_ID, WIDTH_FACTOR));
        WidgetNode finalBox = find(reset.current().document().root(), WRAPPER_ID);
        assertEquals(new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE.negate(),
                        BigDecimal.ONE),
                finalBox.properties().get(ALIGNMENT));
        assertFalse(finalBox.properties().containsKey(WIDTH_FACTOR));
        assertEquals(new PropertyValue.DoubleValue(new BigDecimal("1.25")),
                finalBox.properties().get(HEIGHT_FACTOR));
        assertEquals(THIRD_ID,
                ((WidgetSlot.SingleSlot) finalBox.slots().get(CHILD))
                        .child().orElseThrow().id());

        String dart = new String(
                reset.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("const FractionallySizedBox("), dart);
        assertTrue(dart.contains(
                "alignment: const AlignmentDirectional(-1.0, 1.0)"), dart);
        assertFalse(dart.contains("widthFactor:"), dart);
        assertTrue(dart.contains("heightFactor: 1.25"), dart);
        assertTrue(dart.contains("child: const Text('Inside fraction')"), dart);

        DesignerCommandSession widthRestored = reset.undo().session();
        assertEquals(new PropertyValue.IntegerValue(BigInteger.ZERO),
                find(widthRestored.current().document().root(), WRAPPER_ID)
                        .properties().get(WIDTH_FACTOR));
        DesignerCommandSession resetRedone = widthRestored.redo().session();
        assertArrayEquals(reset.current().fdBytes(), resetRedone.current().fdBytes());
        assertArrayEquals(reset.current().dartCandidateBytes(),
                resetRedone.current().dartCandidateBytes());

        DesignerCommandSession saved = reset.markSaved();
        OriginalFdBytes reopenedFd = OriginalFdBytes.copyOf(
                saved.current().fdBytes(), FdCodecLimits.defaults());
        DesignerCommandSessionOpenResult reopenedResult = DesignerCommandSession.open(
                reopenedFd, saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(reopenedResult.ready(), () -> reopenedResult.diagnostics().toString());
        DesignerCommandSession reopened = reopenedResult.session().orElseThrow();
        assertEquals(saved.current().document(), reopened.current().document());
        assertArrayEquals(saved.current().fdBytes(), reopened.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                reopened.current().dartCandidateBytes());
    }

    @Test
    void stackEditResetChildrenUndoRedoAndReopenAreByteExact() throws Exception {
        DesignerCommandSession initial = session(fixture());
        WidgetNode prototype = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.Stack")).orElseThrow(),
                WRAPPER_ID);
        assertTrue(prototype.properties().isEmpty());
        assertTrue(((WidgetSlot.ListSlot) prototype.slots().get(CHILDREN))
                .children().isEmpty());

        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), prototype));
        DesignerCommandSession childAdded = applied(added, new AddWidget(
                new WidgetPlacement(WRAPPER_ID, CHILDREN, 0),
                text(THIRD_ID, "Stack layer")));
        DesignerCommandSession aligned = applied(childAdded, new SetProperty(
                WRAPPER_ID,
                ALIGNMENT,
                new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE.negate(),
                        BigDecimal.ONE)));
        DesignerCommandSession directed = applied(aligned, new SetProperty(
                WRAPPER_ID,
                TEXT_DIRECTION,
                new PropertyValue.EnumValue("TextDirection", "rtl")));
        DesignerCommandSession fitted = applied(directed, new SetProperty(
                WRAPPER_ID,
                FIT,
                new PropertyValue.EnumValue("StackFit", "passthrough")));
        DesignerCommandSession clipped = applied(fitted, new SetProperty(
                WRAPPER_ID,
                CLIP_BEHAVIOR,
                new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer")));

        assertRejected(clipped, new SetProperty(
                WRAPPER_ID,
                FIT,
                new PropertyValue.EnumValue("StackFit", "cover")),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejected(clipped, new SetProperty(
                WRAPPER_ID,
                CLIP_BEHAVIOR,
                new PropertyValue.EnumValue("Clip", "visible")),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);

        DesignerCommandSession reset = applied(clipped, new ResetProperty(
                WRAPPER_ID, FIT));
        WidgetNode finalStack = find(reset.current().document().root(), WRAPPER_ID);
        assertEquals(new PropertyValue.AlignmentGeometryValue(
                        PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,
                        BigDecimal.ONE.negate(),
                        BigDecimal.ONE),
                finalStack.properties().get(ALIGNMENT));
        assertEquals(new PropertyValue.EnumValue("TextDirection", "rtl"),
                finalStack.properties().get(TEXT_DIRECTION));
        assertFalse(finalStack.properties().containsKey(FIT));
        assertEquals(new PropertyValue.EnumValue("Clip", "antiAliasWithSaveLayer"),
                finalStack.properties().get(CLIP_BEHAVIOR));
        assertEquals(List.of(THIRD_ID),
                ((WidgetSlot.ListSlot) finalStack.slots().get(CHILDREN))
                        .children().stream().map(WidgetNode::id).toList());

        String dart = new String(
                reset.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("const Stack("), dart);
        assertTrue(dart.contains(
                "alignment: const AlignmentDirectional(-1.0, 1.0)"), dart);
        assertTrue(dart.contains("textDirection: TextDirection.rtl"), dart);
        assertFalse(dart.contains("fit:"), dart);
        assertTrue(dart.contains(
                "clipBehavior: Clip.antiAliasWithSaveLayer"), dart);
        assertTrue(dart.contains("children: ["), dart);
        assertTrue(dart.contains("const Text('Stack layer')"), dart);

        DesignerCommandSession fitRestored = reset.undo().session();
        assertEquals(new PropertyValue.EnumValue("StackFit", "passthrough"),
                find(fitRestored.current().document().root(), WRAPPER_ID)
                        .properties().get(FIT));
        DesignerCommandSession resetRedone = fitRestored.redo().session();
        assertArrayEquals(reset.current().fdBytes(), resetRedone.current().fdBytes());
        assertArrayEquals(reset.current().dartCandidateBytes(),
                resetRedone.current().dartCandidateBytes());

        DesignerCommandSession saved = reset.markSaved();
        OriginalFdBytes reopenedFd = OriginalFdBytes.copyOf(
                saved.current().fdBytes(), FdCodecLimits.defaults());
        DesignerCommandSessionOpenResult reopenedResult = DesignerCommandSession.open(
                reopenedFd, saved.current().dartCandidateBytes(), CATALOG);
        assertTrue(reopenedResult.ready(), () -> reopenedResult.diagnostics().toString());
        DesignerCommandSession reopened = reopenedResult.session().orElseThrow();
        assertEquals(saved.current().document(), reopened.current().document());
        assertArrayEquals(saved.current().fdBytes(), reopened.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                reopened.current().dartCandidateBytes());
    }

    @Test
    void patchPropertiesAtomicallyTransitionsContainerBackgroundAndClip()
            throws Exception {
        DesignerCommandSession initial = session(fixture());
        WidgetNode container = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.Container")).orElseThrow(),
                WRAPPER_ID);
        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), container));
        DesignerCommandSession colored = applied(added, new SetProperty(
                WRAPPER_ID, COLOR, new PropertyValue.ColorValue(0xFF102030L)));

        PropertyValue.BoxDecorationValue decoration = simpleDecoration();
        assertRejectedUnchanged(colored, new SetProperty(
                WRAPPER_ID, DECORATION, decoration),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);

        PatchProperties backgroundPatch = new PatchProperties(
                WRAPPER_ID,
                List.of(
                        new PatchProperties.ResetPatch(COLOR),
                        new PatchProperties.SetPatch(DECORATION, decoration)));
        DesignerCommandSession decorated = applied(colored, backgroundPatch);
        WidgetNode decoratedNode = find(
                decorated.current().document().root(), WRAPPER_ID);
        assertFalse(decoratedNode.properties().containsKey(COLOR));
        assertEquals(decoration, decoratedNode.properties().get(DECORATION));
        assertArrayEquals(colored.current().fdBytes(),
                decorated.undo().session().current().fdBytes());
        assertArrayEquals(decorated.current().fdBytes(),
                decorated.undo().session().redo().session().current().fdBytes());

        DesignerCommandSession clipNone = applied(added, new SetProperty(
                WRAPPER_ID, CLIP_BEHAVIOR,
                new PropertyValue.EnumValue("Clip", "none")));
        assertRejectedUnchanged(clipNone, new SetProperty(
                WRAPPER_ID, CLIP_BEHAVIOR,
                new PropertyValue.EnumValue("Clip", "hardEdge")),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);
        DesignerCommandSession clipped = applied(clipNone, new PatchProperties(
                WRAPPER_ID,
                List.of(
                        new PatchProperties.SetPatch(
                                CLIP_BEHAVIOR,
                                new PropertyValue.EnumValue("Clip", "hardEdge")),
                        new PatchProperties.SetPatch(DECORATION, decoration))));
        WidgetNode clippedNode = find(clipped.current().document().root(), WRAPPER_ID);
        assertEquals(new PropertyValue.EnumValue("Clip", "hardEdge"),
                clippedNode.properties().get(CLIP_BEHAVIOR));
        assertEquals(decoration, clippedNode.properties().get(DECORATION));
        String dart = new String(
                clipped.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("decoration: const BoxDecoration("), dart);
        assertTrue(dart.contains("clipBehavior: Clip.hardEdge"), dart);
    }

    @Test
    void patchPropertiesRejectsDuplicateEmptyOrAnyInvalidLeafBeforeMutation()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new PatchProperties(WRAPPER_ID, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new PatchProperties(
                        WRAPPER_ID,
                        List.of(
                                new PatchProperties.ResetPatch(COLOR),
                                new PatchProperties.SetPatch(
                                        COLOR,
                                        new PropertyValue.ColorValue(0xFF000000L)))));

        DesignerCommandSession initial = session(fixture());
        WidgetNode container = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.Container")).orElseThrow(),
                WRAPPER_ID);
        DesignerCommandSession added = applied(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0), container));
        assertRejectedUnchanged(added, new PatchProperties(
                WRAPPER_ID,
                List.of(
                        new PatchProperties.SetPatch(
                                COLOR, new PropertyValue.ColorValue(0xFF000000L)),
                        new PatchProperties.SetPatch(
                                WIDTH, new PropertyValue.StringValue("invalid")))),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
    }

    private static PropertyValue.BoxDecorationValue simpleDecoration() {
        return new PropertyValue.BoxDecorationValue(
                Optional.of(new ColorSource.Literal(0xFF405060L)),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE);
    }

    @Test
    void rejectsInvalidCommandsWithoutChangingSessionOrDiscardingRedo() throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second")));
        assertRejected(initial, new RemoveWidget(ROOT_ID),
                DesignerCommandDiagnosticCode.ROOT_MUTATION_FORBIDDEN);
        assertRejected(initial, new RemoveWidget(THIRD_ID),
                DesignerCommandDiagnosticCode.TARGET_NOT_FOUND);
        assertRejected(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 0),
                text(FIRST_ID, "duplicate")),
                DesignerCommandDiagnosticCode.WIDGET_ID_CONFLICT);
        assertRejected(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 99),
                text(THIRD_ID, "third")),
                DesignerCommandDiagnosticCode.SLOT_INDEX_OUT_OF_BOUNDS);
        assertRejected(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, slot("missing"), 0),
                text(THIRD_ID, "third")),
                DesignerCommandDiagnosticCode.SLOT_UNKNOWN);
        assertRejected(initial, new MoveWidget(
                ROOT_ID, new WidgetPlacement(FIRST_ID, CHILDREN, 0)),
                DesignerCommandDiagnosticCode.ROOT_MUTATION_FORBIDDEN);
        assertRejected(initial, new ResetProperty(FIRST_ID, DATA),
                DesignerCommandDiagnosticCode.PROPERTY_REQUIRED);
        assertRejected(initial, new SetProperty(
                FIRST_ID,
                MAX_LINES,
                new PropertyValue.IntegerValue(BigInteger.ZERO)),
                DesignerCommandDiagnosticCode.PROPERTY_VALUE_REJECTED);
        assertRejected(initial, new WrapWidget(
                FIRST_ID,
                WidgetNode.empty(SECOND_ID, type("flutter.widgets.Center")),
                CHILD,
                0),
                DesignerCommandDiagnosticCode.WIDGET_ID_CONFLICT);

        DesignerCommandSession applied = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("changed")));
        DesignerCommandSession undone = applied.undo().session();
        assertTrue(undone.canRedo());
        DesignerCommandSessionResult rejected = undone.apply(new ResetProperty(FIRST_ID, DATA));
        assertSame(undone, rejected.session());
        assertTrue(rejected.session().canRedo(),
                "a rejected command must retain the exact redo branch");
    }

    @Test
    void occupiedSingleSlotReplacementIsOnePersistentByteExactUndoRedoEdit()
            throws Exception {
        WidgetNode center = singleChild(
                WRAPPER_ID,
                "flutter.widgets.Center",
                CHILD,
                text(FIRST_ID, "obsolete"));
        Fixture fixture = fixture(center, text(SECOND_ID, "replacement"));
        DesignerCommandSession initial = session(fixture);
        ReplaceSlotChild command = new ReplaceSlotChild(
                WRAPPER_ID,
                CHILD,
                FIRST_ID,
                new ReplaceSlotChild.ExistingWidget(SECOND_ID));

        DesignerCommandSessionResult result = initial.apply(command);

        assertEquals(DesignerCommandStatus.APPLIED, result.status(),
                () -> result.diagnostics().toString());
        DesignerCommandSession changed = result.session();
        assertEquals(1, changed.cursor(), "replacement must publish one edit");
        assertSame(command, result.edit().orElseThrow().forward());
        assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                changed.current().persistenceKind());
        assertTrue(changed.current().preparedPair().isPresent());
        assertEquals(List.of(WRAPPER_ID), rootChildIds(changed),
                "the moved source must be removed from its former list slot");
        assertEquals(SECOND_ID, singleChild(
                find(changed.current().document().root(), WRAPPER_ID), CHILD).id());
        assertTrue(findOrNull(changed.current().document().root(), FIRST_ID) == null,
                "the exact old single-slot subtree must be removed");
        String dart = new String(
                changed.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("'replacement'"), dart);
        assertFalse(dart.contains("'obsolete'"), dart);

        DesignerCommandSession undone = changed.undo().session();
        assertArrayEquals(fixture.fd().copyBytes(), undone.current().fdBytes());
        assertArrayEquals(fixture.dart(), undone.current().dartCandidateBytes());
        assertEquals(initial.current().document(), undone.current().document());
        DesignerCommandSession redone = undone.redo().session();
        assertArrayEquals(changed.current().fdBytes(), redone.current().fdBytes());
        assertArrayEquals(changed.current().dartCandidateBytes(),
                redone.current().dartCandidateBytes());
        assertEquals(changed.current().document(), redone.current().document());
    }

    @Test
    void freshSingleSlotReplacementAndListClearAreEachOneAtomicEdit()
            throws Exception {
        WidgetNode center = singleChild(
                WRAPPER_ID,
                "flutter.widgets.Center",
                CHILD,
                text(FIRST_ID, "old"));
        DesignerCommandSession replaceInitial = session(fixture(center));
        DesignerCommandSessionResult replacedResult = replaceInitial.apply(
                new ReplaceSlotChild(
                        WRAPPER_ID,
                        CHILD,
                        FIRST_ID,
                        new ReplaceSlotChild.NewSubtree(
                                text(THIRD_ID, "fresh"))));
        assertEquals(DesignerCommandStatus.APPLIED, replacedResult.status(),
                () -> replacedResult.diagnostics().toString());
        DesignerCommandSession replaced = replacedResult.session();
        assertEquals(1, replaced.cursor());
        assertEquals(THIRD_ID, singleChild(
                find(replaced.current().document().root(), WRAPPER_ID), CHILD).id());
        assertEquals(replaceInitial.current().document(),
                replaced.undo().session().current().document());

        Fixture clearFixture = fixture(
                text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DesignerCommandSession clearInitial = session(clearFixture);
        ClearSlotChildren clear = new ClearSlotChildren(
                ROOT_ID, CHILDREN, List.of(FIRST_ID, SECOND_ID));
        DesignerCommandSessionResult clearedResult = clearInitial.apply(clear);
        assertEquals(DesignerCommandStatus.APPLIED, clearedResult.status(),
                () -> clearedResult.diagnostics().toString());
        DesignerCommandSession cleared = clearedResult.session();
        assertEquals(1, cleared.cursor(), "clear-all must publish one edit");
        assertSame(clear, clearedResult.edit().orElseThrow().forward());
        assertFalse(cleared.current().document().root().slots().containsKey(CHILDREN));
        assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                cleared.current().persistenceKind());
        assertTrue(cleared.current().preparedPair().isPresent());

        DesignerCommandSession clearUndone = cleared.undo().session();
        assertArrayEquals(clearFixture.fd().copyBytes(),
                clearUndone.current().fdBytes());
        assertArrayEquals(clearFixture.dart(),
                clearUndone.current().dartCandidateBytes());
        DesignerCommandSession clearRedone = clearUndone.redo().session();
        assertArrayEquals(cleared.current().fdBytes(),
                clearRedone.current().fdBytes());
        assertArrayEquals(cleared.current().dartCandidateBytes(),
                clearRedone.current().dartCandidateBytes());
    }

    @Test
    void compoundSlotCommandsRejectStaleRootCycleAndIdConflictsWithoutPartialMutation()
            throws Exception {
        WidgetNode center = singleChild(
                WRAPPER_ID,
                "flutter.widgets.Center",
                CHILD,
                text(FIRST_ID, "old"));
        Fixture fixture = fixture(center, text(SECOND_ID, "source"));
        DesignerCommandSession initial = session(fixture);

        assertRejectedUnchanged(initial, new ReplaceSlotChild(
                WRAPPER_ID,
                CHILD,
                THIRD_ID,
                new ReplaceSlotChild.ExistingWidget(SECOND_ID)),
                DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT);
        assertRejectedUnchanged(initial, new ReplaceSlotChild(
                WRAPPER_ID,
                CHILD,
                FIRST_ID,
                new ReplaceSlotChild.ExistingWidget(ROOT_ID)),
                DesignerCommandDiagnosticCode.ROOT_MUTATION_FORBIDDEN);
        assertRejectedUnchanged(initial, new ReplaceSlotChild(
                WRAPPER_ID,
                CHILD,
                FIRST_ID,
                new ReplaceSlotChild.ExistingWidget(THIRD_ID)),
                DesignerCommandDiagnosticCode.TARGET_NOT_FOUND);
        assertRejectedUnchanged(initial, new ReplaceSlotChild(
                WRAPPER_ID,
                CHILD,
                FIRST_ID,
                new ReplaceSlotChild.NewSubtree(text(SECOND_ID, "duplicate"))),
                DesignerCommandDiagnosticCode.WIDGET_ID_CONFLICT);
        assertRejectedUnchanged(initial, new ClearSlotChildren(
                ROOT_ID, CHILDREN, List.of(WRAPPER_ID, THIRD_ID)),
                DesignerCommandDiagnosticCode.STALE_SLOT_CONTENT);

        WidgetNode nestedOwner = singleChild(
                THIRD_ID,
                "flutter.widgets.Center",
                CHILD,
                text(FIRST_ID, "nested"));
        WidgetNode sourceAncestor = singleChild(
                WRAPPER_ID,
                "flutter.widgets.Center",
                CHILD,
                nestedOwner);
        DesignerCommandSession cyclic = session(fixture(sourceAncestor));
        assertRejectedUnchanged(cyclic, new ReplaceSlotChild(
                THIRD_ID,
                CHILD,
                FIRST_ID,
                new ReplaceSlotChild.ExistingWidget(WRAPPER_ID)),
                DesignerCommandDiagnosticCode.DESTINATION_INSIDE_SUBTREE);
    }

    @Test
    void compoundSlotCommandsUseCatalogAcceptanceAndMinimumInvariants() {
        WidgetDefinition constrainedOwner = new WidgetDefinition(
                type("test.ConstrainedOwner"),
                "ConstrainedOwner",
                Optional.empty(),
                true,
                "package:test/widgets.dart",
                List.of("package:test/widgets.dart"),
                Set.of(),
                new PaletteMetadata("test", 900, 0, "Constrained Owner"),
                List.of(),
                List.of(
                        new SlotDefinition(
                                CHILD,
                                DartParameter.named(0, false),
                                SlotCardinality.SINGLE,
                                0,
                                1,
                                new SlotAcceptance.ExactTypes(List.of(
                                        type("flutter.widgets.Center")))),
                        new SlotDefinition(
                                CHILDREN,
                                DartParameter.named(1, true),
                                SlotCardinality.LIST,
                                1,
                                10,
                                new SlotAcceptance.AnyWidget())));
        WidgetDefinition centerDefinition = CATALOG.find(
                type("flutter.widgets.Center")).orElseThrow();
        WidgetDefinition textDefinition = CATALOG.find(
                type("flutter.widgets.Text")).orElseThrow();
        WidgetCatalog catalog = WidgetCatalog.strict(List.of(
                constrainedOwner, centerDefinition, textDefinition));
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                constrainedOwner.typeId(),
                Map.of(),
                Map.of(
                        CHILD, WidgetSlot.SingleSlot.of(WidgetNode.empty(
                                WRAPPER_ID, centerDefinition.typeId())),
                        CHILDREN, new WidgetSlot.ListSlot(List.of(
                                text(FIRST_ID, "required")))),
                Extensions.empty());
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID,
                descriptor("0".repeat(64), "0".repeat(64)),
                root);
        DesignerCommandTransformer transformer = new DesignerCommandTransformer(
                catalog,
                DesignerCommandLimits.defaults().validationLimits());

        DesignerCommandTransformer.SemanticResult incompatible = transformer.apply(
                document,
                new ReplaceSlotChild(
                        ROOT_ID,
                        CHILD,
                        WRAPPER_ID,
                        new ReplaceSlotChild.NewSubtree(
                                text(SECOND_ID, "rejected"))));
        assertEquals(DesignerCommandStatus.REJECTED, incompatible.status());
        assertEquals(DesignerCommandDiagnosticCode.SLOT_REJECTS_WIDGET,
                incompatible.diagnostic().orElseThrow().code());
        assertTrue(incompatible.document().isEmpty());

        DesignerCommandTransformer.SemanticResult required = transformer.apply(
                document,
                new ClearSlotChildren(
                        ROOT_ID, CHILDREN, List.of(FIRST_ID)));
        assertEquals(DesignerCommandStatus.REJECTED, required.status());
        assertEquals(DesignerCommandDiagnosticCode.SLOT_REQUIRED,
                required.diagnostic().orElseThrow().code());
        assertTrue(required.document().isEmpty());
    }

    @Test
    void moveIntoOwnDescendantIsRejected() throws Exception {
        WidgetNode nested = new WidgetNode(
                WRAPPER_ID,
                type("flutter.widgets.Center"),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(text(FIRST_ID, "nested"))),
                Extensions.empty());
        DesignerCommandSession session = session(fixture(nested, text(SECOND_ID, "second")));

        assertRejected(session, new MoveWidget(
                WRAPPER_ID, new WidgetPlacement(FIRST_ID, CHILD, 0)),
                DesignerCommandDiagnosticCode.DESTINATION_INSIDE_SUBTREE);
    }

    @Test
    void wrapRootIsOneUndoableExactAction() throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second")));

        DesignerCommandSession wrapped = applied(initial, new WrapWidget(
                ROOT_ID,
                WidgetNode.empty(WRAPPER_ID, type("flutter.widgets.Center")),
                CHILD,
                0));

        assertEquals(WRAPPER_ID, wrapped.current().document().root().id());
        assertEquals(ROOT_ID,
                ((WidgetSlot.SingleSlot) wrapped.current().document().root()
                        .slots().get(CHILD)).child().orElseThrow().id());
        DesignerCommandSession undone = wrapped.undo().session();
        assertEquals(initial.current().document(), undone.current().document());
        assertArrayEquals(initial.current().fdBytes(), undone.current().fdBytes());
        assertArrayEquals(initial.current().dartCandidateBytes(),
                undone.current().dartCandidateBytes());
    }

    @Test
    void expandedUsesAtomicWrapCreationWhileEmptyAddAndInvalidWrapsFailClosed()
            throws Exception {
        DesignerCommandSession initial = session(fixture(text(FIRST_ID, "first")));
        WidgetDefinition expandedDefinition = CATALOG.find(
                type("flutter.widgets.Expanded")).orElseThrow();
        WidgetNode detachedWrapper = WidgetNodePrototypeFactory.create(
                expandedDefinition, WRAPPER_ID);

        DesignerCommandSession wrapped = applied(initial, new WrapWidget(
                FIRST_ID, detachedWrapper, CHILD, 0));
        WidgetNode expanded = rootChildren(wrapped).getFirst();
        assertEquals(WRAPPER_ID, expanded.id());
        assertTrue(expanded.properties().isEmpty());
        assertEquals(FIRST_ID, singleChild(expanded, CHILD).id());
        String dart = new String(
                wrapped.current().dartCandidateBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("Expanded("), dart);
        assertTrue(dart.contains("child: const Text('first')"), dart);

        assertRejectedUnchanged(initial, new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 1),
                WidgetNodePrototypeFactory.create(expandedDefinition, THIRD_ID)),
                DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID);

        DesignerCommandSessionResult rootWrap = initial.apply(new WrapWidget(
                ROOT_ID,
                WidgetNodePrototypeFactory.create(expandedDefinition, WRAPPER_ID),
                CHILD,
                0));
        assertEquals(DesignerCommandStatus.REJECTED, rootWrap.status());
        assertEquals(DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED,
                rootWrap.diagnostics().getFirst().code());
        assertTrue(rootWrap.diagnostics().getFirst().message()
                .contains("cannot be the Designer root"));

        DesignerCommandSessionResult nestedExpanded = wrapped.apply(new WrapWidget(
                WRAPPER_ID,
                WidgetNodePrototypeFactory.create(expandedDefinition, THIRD_ID),
                CHILD,
                0));
        assertEquals(DesignerCommandStatus.REJECTED, nestedExpanded.status());
        assertEquals(DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED,
                nestedExpanded.diagnostics().getFirst().code());
        assertTrue(nestedExpanded.diagnostics().getFirst().message()
                .contains("flutter.widgets.Expanded.child"));
    }

    @Test
    void expandedMoveAndReplacePathsUseTheSameDirectFlexPlacementGate()
            throws Exception {
        WidgetNode stack = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.Stack")).orElseThrow(),
                SECOND_ID);
        WidgetNode row = WidgetNodePrototypeFactory.create(
                CATALOG.find(type("flutter.widgets.Row")).orElseThrow(),
                THIRD_ID);
        DesignerCommandSession initial = session(fixture(
                text(FIRST_ID, "move"), stack, row));
        WidgetDefinition expandedDefinition = CATALOG.find(
                type("flutter.widgets.Expanded")).orElseThrow();
        DesignerCommandSession wrapped = applied(initial, new WrapWidget(
                FIRST_ID,
                WidgetNodePrototypeFactory.create(expandedDefinition, WRAPPER_ID),
                CHILD,
                0));

        DesignerCommandSessionResult wrongMove = wrapped.apply(new MoveWidget(
                WRAPPER_ID, new WidgetPlacement(SECOND_ID, CHILDREN, 0)));
        assertEquals(DesignerCommandStatus.REJECTED, wrongMove.status());
        assertEquals(DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED,
                wrongMove.diagnostics().getFirst().code());
        assertTrue(wrongMove.diagnostics().getFirst().message()
                .contains("flutter.widgets.Stack.children"));

        DesignerCommandSession moved = applied(wrapped, new MoveWidget(
                WRAPPER_ID, new WidgetPlacement(THIRD_ID, CHILDREN, 0)));
        WidgetNode movedRow = find(moved.current().document().root(), THIRD_ID);
        assertEquals(List.of(WRAPPER_ID),
                ((WidgetSlot.ListSlot) movedRow.slots().get(CHILDREN))
                        .children().stream().map(WidgetNode::id).toList());

        WidgetNode center = singleChild(
                WRAPPER_ID,
                "flutter.widgets.Center",
                CHILD,
                text(FIRST_ID, "replace"));
        DesignerCommandSession replaceSession = session(fixture(center));
        WidgetNode replacement = singleChild(
                SECOND_ID,
                "flutter.widgets.Expanded",
                CHILD,
                text(THIRD_ID, "replacement"));
        DesignerCommandSessionResult wrongReplace = replaceSession.apply(
                new ReplaceSlotChild(
                        WRAPPER_ID,
                        CHILD,
                        FIRST_ID,
                        new ReplaceSlotChild.NewSubtree(replacement)));
        assertEquals(DesignerCommandStatus.REJECTED, wrongReplace.status());
        assertEquals(DesignerCommandDiagnosticCode.WIDGET_PLACEMENT_REJECTED,
                wrongReplace.diagnostics().getFirst().code());
        assertTrue(wrongReplace.diagnostics().getFirst().message()
                .contains("flutter.widgets.Center.child"));
    }

    @Test
    void removeFromCatalogRequiredSlotHasSpecificStableDiagnostic() {
        WidgetDefinition requiredParent = new WidgetDefinition(
                type("test.RequiredParent"),
                "RequiredParent",
                Optional.empty(),
                true,
                "package:test/widgets.dart",
                List.of("package:test/widgets.dart"),
                Set.of(),
                new PaletteMetadata("test", 900, 0, "Required Parent"),
                List.of(),
                List.of(new SlotDefinition(
                        CHILDREN,
                        DartParameter.named(0, true),
                        SlotCardinality.LIST,
                        1,
                        10,
                        new SlotAcceptance.AnyWidget())));
        WidgetDefinition text = CATALOG.find(type("flutter.widgets.Text")).orElseThrow();
        WidgetCatalog catalog = WidgetCatalog.strict(List.of(requiredParent, text));
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                requiredParent.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(
                        List.of(text(FIRST_ID, "required")))),
                Extensions.empty());
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID,
                descriptor("0".repeat(64), "0".repeat(64)),
                root);

        DesignerCommandTransformer.SemanticResult result =
                new DesignerCommandTransformer(
                        catalog,
                        DesignerCommandLimits.defaults().validationLimits())
                        .apply(document, new RemoveWidget(FIRST_ID));

        assertEquals(DesignerCommandStatus.REJECTED, result.status());
        assertEquals(DesignerCommandDiagnosticCode.SLOT_REQUIRED,
                result.diagnostic().orElseThrow().code());
        assertTrue(result.document().isEmpty());
    }

    @Test
    void moveWithinSameRequiredListSlotIsValidatedAtomically() {
        WidgetDefinition requiredParent = new WidgetDefinition(
                type("test.RequiredParent"),
                "RequiredParent",
                Optional.empty(),
                true,
                "package:test/widgets.dart",
                List.of("package:test/widgets.dart"),
                Set.of(),
                new PaletteMetadata("test", 900, 0, "Required Parent"),
                List.of(),
                List.of(new SlotDefinition(
                        CHILDREN,
                        DartParameter.named(0, true),
                        SlotCardinality.LIST,
                        2,
                        10,
                        new SlotAcceptance.AnyWidget())));
        WidgetDefinition text = CATALOG.find(
                type("flutter.widgets.Text")).orElseThrow();
        WidgetCatalog catalog = WidgetCatalog.strict(List.of(requiredParent, text));
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                requiredParent.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(
                        text(FIRST_ID, "first"),
                        text(SECOND_ID, "second")))),
                Extensions.empty());
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID,
                descriptor("0".repeat(64), "0".repeat(64)),
                root);

        DesignerCommandTransformer.SemanticResult result =
                new DesignerCommandTransformer(
                        catalog,
                        DesignerCommandLimits.defaults().validationLimits())
                        .apply(document, new MoveWidget(
                                FIRST_ID,
                                new WidgetPlacement(ROOT_ID, CHILDREN, 1)));

        assertEquals(DesignerCommandStatus.APPLIED, result.status());
        WidgetSlot.ListSlot children = (WidgetSlot.ListSlot) result.document()
                .orElseThrow().root().slots().get(CHILDREN);
        assertEquals(List.of(SECOND_ID, FIRST_ID),
                children.children().stream().map(WidgetNode::id).toList());
    }

    @Test
    void moveAcrossSlotsStillEnforcesRequiredSourceMinimum() {
        SlotName destination = slot("destination");
        WidgetDefinition requiredParent = new WidgetDefinition(
                type("test.RequiredParent"),
                "RequiredParent",
                Optional.empty(),
                true,
                "package:test/widgets.dart",
                List.of("package:test/widgets.dart"),
                Set.of(),
                new PaletteMetadata("test", 900, 0, "Required Parent"),
                List.of(),
                List.of(
                        new SlotDefinition(
                                CHILDREN,
                                DartParameter.named(0, true),
                                SlotCardinality.LIST,
                                1,
                                10,
                                new SlotAcceptance.AnyWidget()),
                        new SlotDefinition(
                                destination,
                                DartParameter.named(1, false),
                                SlotCardinality.LIST,
                                0,
                                10,
                                new SlotAcceptance.AnyWidget())));
        WidgetDefinition text = CATALOG.find(
                type("flutter.widgets.Text")).orElseThrow();
        WidgetCatalog catalog = WidgetCatalog.strict(List.of(requiredParent, text));
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                requiredParent.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(
                        List.of(text(FIRST_ID, "required")))),
                Extensions.empty());
        DesignerDocument document = new DesignerDocument(
                DOCUMENT_ID,
                descriptor("0".repeat(64), "0".repeat(64)),
                root);

        DesignerCommandTransformer.SemanticResult result =
                new DesignerCommandTransformer(
                        catalog,
                        DesignerCommandLimits.defaults().validationLimits())
                        .apply(document, new MoveWidget(
                                FIRST_ID,
                                new WidgetPlacement(ROOT_ID, destination, 0)));

        assertEquals(DesignerCommandStatus.REJECTED, result.status());
        assertEquals(DesignerCommandDiagnosticCode.SLOT_REQUIRED,
                result.diagnostic().orElseThrow().code());
        assertTrue(result.document().isEmpty());
    }

    @Test
    void codeIdenticalMoveProducesDirtyFdOnlyRevisionNotNoOp() throws Exception {
        Fixture fixture = fixture(text(FIRST_ID, "same"), text(SECOND_ID, "same"));
        DesignerCommandSession initial = session(fixture);

        DesignerCommandSessionResult result = initial.apply(new MoveWidget(
                FIRST_ID, new WidgetPlacement(ROOT_ID, CHILDREN, 1)));

        assertEquals(DesignerCommandStatus.APPLIED, result.status());
        DesignerCommandSession moved = result.session();
        assertTrue(moved.dirty());
        assertEquals(List.of(SECOND_ID, FIRST_ID), rootChildIds(moved));
        assertEquals(DesignerRevisionPersistenceKind.FD_ONLY,
                moved.current().persistenceKind());
        assertTrue(moved.current().preparedPair().isEmpty());
        assertArrayEquals(fixture.dart(), moved.current().dartCandidateBytes());
        assertFalse(Arrays.equals(fixture.fd().copyBytes(), moved.current().fdBytes()));
        assertFalse(moved.undo().session().dirty());
    }

    @Test
    void markSavedReanchorsUndoToNewDurablePairAndRedoReturnsBaseline() throws Exception {
        DesignerCommandSession original = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession beforeSave = applied(original, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B")));
        long savedId = beforeSave.current().revisionId();

        DesignerCommandSession saved = beforeSave.markSaved();

        assertFalse(saved.dirty());
        assertEquals(savedId, saved.savedRevisionId());
        assertEquals(OptionalInt.of(1), saved.savedCursor());
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                saved.current().persistenceKind());
        assertArrayEquals(saved.current().fdBytes(),
                saved.durableFdAnchor().copyBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                saved.durableDartAnchorBytes());

        DesignerCommandSession undoA = saved.undo().session();
        assertTrue(undoA.dirty());
        assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                undoA.current().persistenceKind());
        assertArrayEquals(saved.durableFdAnchor().copyBytes(),
                undoA.current().preparedPair().orElseThrow().baselineFdBytes());
        assertArrayEquals(saved.durableDartAnchorBytes(),
                undoA.current().preparedPair().orElseThrow().baselineDartBytes());
        assertTrue(new String(
                undoA.current().dartCandidateBytes(), StandardCharsets.UTF_8).contains("'A'"));

        DesignerCommandSession redoB = undoA.redo().session();
        assertFalse(redoB.dirty());
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                redoB.current().persistenceKind());
        assertArrayEquals(saved.current().fdBytes(), redoB.current().fdBytes());
        assertArrayEquals(saved.current().dartCandidateBytes(),
                redoB.current().dartCandidateBytes());
    }

    @Test
    void markSavedAcceptsExactNativeOverlayPairAndRejectsDetachedAnchor()
            throws Exception {
        Fixture fixture = fixture(
                text(FIRST_ID, "A"), text(SECOND_ID, "second"));
        DesignerCommandSession initial = session(fixture);
        DesignerCommandSession dirty = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B")));
        PreparedDesignerPair exactPair = exactOverlayPair(
                dirty, "// exact native overlay\n");
        assertFalse(Arrays.equals(
                dirty.current().dartCandidateBytes(),
                exactPair.prospectiveDartBytes()));

        DesignerCommandSession saved = dirty.markSaved(exactPair);

        assertFalse(saved.dirty());
        assertEquals(dirty.current().revisionId(), saved.savedRevisionId());
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                saved.current().persistenceKind());
        assertArrayEquals(
                exactPair.prospectiveFdBytes(),
                saved.durableFdAnchor().copyBytes());
        assertArrayEquals(
                exactPair.prospectiveDartBytes(),
                saved.durableDartAnchorBytes());
        assertArrayEquals(
                exactPair.prospectiveDartBytes(),
                saved.current().dartCandidateBytes());
        DesignerCommandRevision undoA = saved.undo().session().current();
        assertEquals(initial.current().revisionId(), undoA.revisionId());
        assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                undoA.persistenceKind());
        assertTrue(new String(
                undoA.dartCandidateBytes(), StandardCharsets.UTF_8)
                .endsWith("// exact native overlay\n"));
        assertArrayEquals(
                exactPair.prospectiveDartBytes(),
                undoA.preparedPair().orElseThrow().baselineDartBytes());

        OriginalFdBytes detachedFd = OriginalFdBytes.copyOf(
                fixture.fd().copyBytes(), FdCodecLimits.defaults());
        DesignerCommandSession detached = applied(
                DesignerCommandSession.open(
                        detachedFd, fixture.dart(), CATALOG)
                        .session().orElseThrow(),
                new SetProperty(
                        FIRST_ID,
                        DATA,
                        new PropertyValue.StringValue("B")));
        PreparedDesignerPair detachedPair = exactOverlayPair(
                detached, "// detached overlay\n");
        assertThrows(IllegalArgumentException.class,
                () -> dirty.markSaved(detachedPair));
    }

    @Test
    void physicalRevisionProofAllowsTwoExactTemplatesForOneStableId()
            throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession dirty = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B")));
        PreparedDesignerPair exactPair = exactOverlayPair(
                dirty, "// durable native overlay\n");
        DesignerCommandSession saved = dirty.markSaved(exactPair);
        long historicalId = initial.current().revisionId();
        byte[] firstTemplate = exactPair.prospectiveDartBytes();
        byte[] secondTemplate = (new String(
                firstTemplate, StandardCharsets.UTF_8)
                + "// later unmanaged template\n")
                .getBytes(StandardCharsets.UTF_8);

        DesignerCommandRevision first = saved.rederiveRetainedRevision(
                historicalId, firstTemplate);
        DesignerCommandRevision second = saved.rederiveRetainedRevision(
                historicalId, secondTemplate);

        assertNotSame(first, second);
        assertEquals(historicalId, first.revisionId());
        assertEquals(historicalId, second.revisionId());
        assertEquals(first.document(), second.document());
        assertArrayEquals(first.fdBytes(), second.fdBytes());
        assertFalse(Arrays.equals(
                first.dartCandidateBytes(), second.dartCandidateBytes()));
        assertArrayEquals(firstTemplate,
                first.preparedPair().orElseThrow().liveDartBytes());
        assertArrayEquals(secondTemplate,
                second.preparedPair().orElseThrow().liveDartBytes());
        assertSame(saved.durableThreeWayIntegrity(),
                first.preparedPair().orElseThrow()
                        .dartTransition().baseline());
        assertSame(saved.durableThreeWayIntegrity(),
                second.preparedPair().orElseThrow()
                        .dartTransition().baseline());

        byte[] changedManaged = secondTemplate.clone();
        var region = saved.durableSourceIntegrity().region(
                DartSourceIntegrityScanner.BUILD_REGION).orElseThrow();
        changedManaged[region.payloadStartByte()] ^= 1;
        assertThrows(IllegalArgumentException.class,
                () -> saved.rederiveRetainedRevision(
                        historicalId, changedManaged));
        assertThrows(IllegalArgumentException.class,
                () -> saved.rederiveRetainedRevision(
                        Long.MAX_VALUE, firstTemplate));
    }

    @Test
    void projectedAnchorPhysicalRevisionKeepsHistoricalUnmanagedEnvelope()
            throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession c1 = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C1")));
        DesignerCommandRevision canonicalC1 = c1.current();
        DesignerCommandSession c2 = applied(c1, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C2")));
        DesignerCommandRevision canonicalC2 = c2.current();
        PreparedDesignerPair exactC2 = exactOverlayPair(
                c2, "// durable S2 envelope\n");
        DesignerCommandSession saved = c2.markSaved(exactC2);

        DesignerCommandRevision physicalC1 =
                saved.rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(),
                        canonicalC1.dartCandidateBytes());
        PreparedDesignerPair pair = physicalC1.preparedPair().orElseThrow();

        assertEquals(canonicalC1.revisionId(), physicalC1.revisionId());
        assertEquals(canonicalC1.document(), physicalC1.document());
        assertArrayEquals(canonicalC1.fdBytes(), physicalC1.fdBytes());
        assertArrayEquals(
                exactC2.prospectiveDartBytes(), pair.baselineDartBytes());
        assertArrayEquals(
                canonicalC2.dartCandidateBytes(), pair.liveDartBytes(),
                "the C2 anchor payload must be projected into the old S0 envelope");
        assertArrayEquals(
                canonicalC1.dartCandidateBytes(),
                pair.prospectiveDartBytes(),
                "re-derivation must restore C1 while retaining the S0 envelope");
        DesignerCommandRevision reanchoredC1 = saved.retainedRevision(
                canonicalC1.revisionId()).orElseThrow();
        assertSame(reanchoredC1.generation(), physicalC1.generation());
        assertSame(reanchoredC1.generation(),
                pair.dartTransition().generation());
        assertSame(saved.durableThreeWayIntegrity(),
                pair.dartTransition().baseline());
    }

    @Test
    void commandFromProjectedPhysicalEndpointKeepsItsExactDartEnvelope()
            throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "B"), text(SECOND_ID, "second")));
        DesignerCommandSession c1 = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C1")));
        DesignerCommandRevision canonicalC1 = c1.current();
        DesignerCommandSession c2 = applied(c1, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C2")));
        PreparedDesignerPair exactC2 = exactOverlayPair(
                c2, "// durable S2 envelope\n");
        DesignerCommandSession saved = c2.markSaved(exactC2);
        DesignerCommandSession atC1 = saved.undo().session();
        DesignerCommandRevision physicalC1 =
                saved.rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(),
                        canonicalC1.dartCandidateBytes());
        PreparedDesignerPair endpointPair =
                physicalC1.preparedPair().orElseThrow();
        int predecessorCursor = atC1.cursor();
        long predecessorRevisionId = atC1.current().revisionId();

        SetProperty command = new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C3"));
        DesignerCommandSessionResult physicalResult =
                atC1.applyFromPhysicalEndpoint(command, endpointPair);
        DesignerCommandSessionResult canonicalResult = atC1.apply(command);

        assertEquals(DesignerCommandStatus.APPLIED, physicalResult.status(),
                () -> physicalResult.diagnostics().toString());
        assertEquals(predecessorCursor, atC1.cursor(),
                "pure admission must not move the logical cursor");
        assertEquals(predecessorRevisionId,
                atC1.current().revisionId(),
                "pure admission must not replace the predecessor revision id");
        assertSame(atC1.current(),
                physicalResult.edit().orElseThrow().beforeRevision());
        DesignerCommandRevision candidate =
                physicalResult.session().current();
        PreparedDesignerPair candidatePair =
                candidate.preparedPair().orElseThrow();
        assertTrue(candidate.revisionId() > predecessorRevisionId);
        assertArrayEquals(
                endpointPair.liveDartBytes(),
                candidatePair.liveDartBytes(),
                "the command transition must start from the exact S0 envelope");
        assertSame(endpointPair.baselineFd(), candidatePair.baselineFd());
        assertSame(
                endpointPair.dartTransition().baseline(),
                candidatePair.dartTransition().baseline());
        assertSame(atC1.catalog(), physicalResult.session().catalog());
        assertSame(
                atC1.current().candidateCapacityBudget(),
                candidate.candidateCapacityBudget());
        assertFalse(new String(
                candidate.dartCandidateBytes(), StandardCharsets.UTF_8)
                .contains("durable S2 envelope"));
        assertTrue(new String(
                canonicalResult.session().current().dartCandidateBytes(),
                StandardCharsets.UTF_8)
                .contains("durable S2 envelope"),
                "canonical admission must remain observably distinct");
    }

    @Test
    void commandFromPhysicalEndpointRejectsAnotherSemanticOrAnchorIdentity()
            throws Exception {
        Fixture fixture = fixture(
                text(FIRST_ID, "B"), text(SECOND_ID, "second"));
        DesignerCommandSession initial = session(fixture);
        DesignerCommandSession c1 = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C1")));
        DesignerCommandRevision canonicalC1 = c1.current();
        DesignerCommandSession c2 = applied(c1, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C2")));
        DesignerCommandSession saved = c2.markSaved(exactOverlayPair(
                c2, "// durable S2 envelope\n"));
        DesignerCommandSession atC1 = saved.undo().session();
        PreparedDesignerPair physicalB = saved
                .rederiveRetainedRevisionByProjectingAnchor(
                        initial.current().revisionId(),
                        initial.current().dartCandidateBytes())
                .preparedPair().orElseThrow();

        DesignerCommandSession foreignInitial = session(fixture);
        DesignerCommandSession foreignC1 = applied(
                foreignInitial, new SetProperty(
                        FIRST_ID,
                        DATA,
                        new PropertyValue.StringValue("C1")));
        DesignerCommandSession foreignC2 = applied(
                foreignC1, new SetProperty(
                        FIRST_ID,
                        DATA,
                        new PropertyValue.StringValue("C2")));
        DesignerCommandSession foreignSaved = foreignC2.markSaved(
                exactOverlayPair(foreignC2, "// foreign S2 envelope\n"));
        PreparedDesignerPair foreignPhysicalC1 = foreignSaved
                .rederiveRetainedRevisionByProjectingAnchor(
                        foreignC1.current().revisionId(),
                        foreignC1.current().dartCandidateBytes())
                .preparedPair().orElseThrow();
        SetProperty command = new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C3"));

        assertThrows(IllegalArgumentException.class,
                () -> atC1.applyFromPhysicalEndpoint(command, physicalB));
        assertThrows(IllegalArgumentException.class,
                () -> atC1.applyFromPhysicalEndpoint(
                        command, foreignPhysicalC1));
        assertEquals(canonicalC1.revisionId(),
                atC1.current().revisionId());
        assertEquals(1, atC1.cursor());
    }

    @Test
    void projectedAnchorPhysicalRevisionRejectsMalformedAndHashOnlyTemplates()
            throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession c1 = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C1")));
        DesignerCommandRevision canonicalC1 = c1.current();
        DesignerCommandSession c2 = applied(c1, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C2")));
        DesignerCommandSession saved = c2.markSaved(exactOverlayPair(
                c2, "// durable S2 envelope\n"));

        byte[] unsupported = new String(
                canonicalC1.dartCandidateBytes(), StandardCharsets.UTF_8)
                .replace("region=\"build\"", "region=\"future\"")
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> saved.rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(), unsupported));

        byte[] normalizedEquivalent = withCrLfInsideBuildPayload(
                canonicalC1);
        DartSourceIntegrityResult normalizedScan =
                new DartSourceIntegrityScanner().scan(
                        normalizedEquivalent,
                        canonicalC1.document().source());
        assertTrue(normalizedScan.onDiskDeclaredMatch(),
                () -> normalizedScan.diagnostics().toString());
        assertThrows(IllegalArgumentException.class,
                () -> saved.rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(), normalizedEquivalent),
                "normalized-hash equality must not replace exact managed bytes");

        byte[] invalidUtf8 = canonicalC1.dartCandidateBytes();
        invalidUtf8[0] = (byte) 0xFF;
        assertThrows(IllegalArgumentException.class,
                () -> saved.rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(), invalidUtf8));
    }

    @Test
    void projectedAnchorPhysicalRevisionHonorsRetainedByteLimit()
            throws Exception {
        Fixture fixture = fixture(
                text(FIRST_ID, "A"), text(SECOND_ID, "second"));
        DesignerCommandLimits defaults = DesignerCommandLimits.defaults();
        DesignerCommandLimits bounded = new DesignerCommandLimits(
                defaults.fdCodecLimits(),
                defaults.generationLimits(),
                defaults.sourceLimits(),
                defaults.validationLimits(),
                defaults.maxHistoryEdits(),
                64 * 1024);
        DesignerCommandSession initial = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG, bounded)
                .session().orElseThrow();
        DesignerCommandSession c1 = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C1")));
        DesignerCommandRevision canonicalC1 = c1.current();
        DesignerCommandSession c2 = applied(c1, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C2")));
        DesignerCommandSession saved = c2.markSaved(exactOverlayPair(
                c2, "// durable S2 envelope\n"));
        byte[] oversizedEnvelope = (new String(
                canonicalC1.dartCandidateBytes(), StandardCharsets.UTF_8)
                + "// " + "x".repeat(70 * 1024) + "\n")
                .getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class,
                () -> saved.rederiveRetainedRevisionByProjectingAnchor(
                        canonicalC1.revisionId(), oversizedEnvelope));
    }

    @Test
    void sourceAnchorRebaseKeepsHistoricalCandidatesAndRebuildsTheirProofs()
            throws Exception {
        DesignerCommandSession original = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession savedB = applied(original, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B")))
                .markSaved();
        DesignerCommandRevision historicalA = savedB.retainedRevision(
                original.current().revisionId()).orElseThrow();
        DesignerCommandRevision priorSavedB = savedB.current();
        byte[] sourceOverlay = (new String(
                priorSavedB.dartCandidateBytes(), StandardCharsets.UTF_8)
                + "// saved unmanaged source overlay\n")
                .getBytes(StandardCharsets.UTF_8);

        DesignerCommandSession rebased =
                savedB.reanchorSavedSource(sourceOverlay);

        assertFalse(rebased.dirty());
        assertEquals(savedB.savedRevisionId(), rebased.savedRevisionId());
        assertEquals(savedB.cursor(), rebased.cursor());
        assertEquals(savedB.revisionCount(), rebased.revisionCount());
        assertNotSame(priorSavedB, rebased.current());
        assertEquals(priorSavedB.revisionId(),
                rebased.current().revisionId());
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                rebased.current().persistenceKind());
        assertArrayEquals(sourceOverlay,
                rebased.current().dartCandidateBytes());
        assertArrayEquals(sourceOverlay, rebased.durableDartAnchorBytes());
        assertArrayEquals(savedB.durableFdAnchor().copyBytes(),
                rebased.durableFdAnchor().copyBytes());
        DesignerCommandRevision reanchoredHistoricalA =
                rebased.retainedRevision(
                        historicalA.revisionId()).orElseThrow();
        assertNotSame(historicalA, reanchoredHistoricalA);
        assertArrayEquals(
                historicalA.dartCandidateBytes(),
                reanchoredHistoricalA.dartCandidateBytes(),
                "the older native semantic endpoint must retain its exact pre-overlay bytes");
        assertArrayEquals(
                historicalA.fdBytes(), reanchoredHistoricalA.fdBytes());
        assertArrayEquals(
                sourceOverlay,
                reanchoredHistoricalA.preparedPair().orElseThrow()
                        .baselineDartBytes(),
                "historical proof must use the new durable Source anchor");
        assertArrayEquals(
                priorSavedB.dartCandidateBytes(),
                reanchoredHistoricalA.preparedPair().orElseThrow()
                        .liveDartBytes(),
                "historical proof must retain the native pre-overlay template");
        assertFalse(Arrays.equals(
                sourceOverlay, historicalA.dartCandidateBytes()));

        DesignerCommandSession undoA = rebased.undo().session();
        assertSame(reanchoredHistoricalA, undoA.current());
        assertTrue(undoA.dirty());
        DesignerCommandSession redoSavedB = undoA.redo().session();
        assertSame(rebased.current(), redoSavedB.current());
        assertFalse(redoSavedB.dirty());

        DesignerCommandSession changedC = applied(rebased, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C")));
        assertTrue(new String(
                changedC.current().dartCandidateBytes(), StandardCharsets.UTF_8)
                .endsWith("// saved unmanaged source overlay\n"),
                "a new command from the clean Source anchor must preserve unmanaged bytes");
        assertArrayEquals(sourceOverlay,
                changedC.current().preparedPair().orElseThrow()
                        .baselineDartBytes());
    }

    @Test
    void sourceAnchorRebaseRejectsDirtyOrManagedSourceChanges() throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession dirty = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B")));

        assertThrows(IllegalStateException.class,
                () -> dirty.reanchorSavedSource(
                        dirty.current().dartCandidateBytes()));

        byte[] changedManaged = Arrays.copyOf(
                initial.current().dartCandidateBytes(),
                initial.current().dartCandidateBytes().length);
        String exactSource = new String(
                changedManaged, StandardCharsets.UTF_8);
        int buildMarker = exactSource.indexOf("region=\"build\"");
        int buildPayload = exactSource.indexOf('\n', buildMarker) + 1;
        assertTrue(buildMarker >= 0 && buildPayload > buildMarker);
        changedManaged[buildPayload] = changedManaged[buildPayload] == ' '
                ? (byte) '\t' : (byte) ' ';
        assertThrows(IllegalStateException.class,
                () -> initial.reanchorSavedSource(changedManaged));

        var buildRegion = initial.current().sourceIntegrity().region(
                DartSourceIntegrityScanner.BUILD_REGION).orElseThrow();
        byte[] normalizedEquivalentManaged = new byte[
                initial.current().dartCandidateBytes().length + 1];
        byte[] initialDart = initial.current().dartCandidateBytes();
        System.arraycopy(
                initialDart,
                0,
                normalizedEquivalentManaged,
                0,
                buildRegion.payloadEndByte());
        normalizedEquivalentManaged[buildRegion.payloadEndByte()] = '\n';
        System.arraycopy(
                initialDart,
                buildRegion.payloadEndByte(),
                normalizedEquivalentManaged,
                buildRegion.payloadEndByte() + 1,
                initialDart.length - buildRegion.payloadEndByte());
        assertThrows(IllegalStateException.class,
                () -> initial.reanchorSavedSource(
                        normalizedEquivalentManaged),
                "normalized hashes must not authorize an exact managed-payload byte change");
    }

    @Test
    void retainedRevisionLookupPreservesStableIdsAcrossSavedReanchor()
            throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandRevision revision0 = initial.current();
        DesignerCommandSession changedB = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B")));
        DesignerCommandRevision revision1 = changedB.current();
        DesignerCommandSession changedC = applied(changedB, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C")));
        DesignerCommandRevision revision2 = changedC.current();
        List<DesignerCommandRevision> retainedBefore = List.of(
                revision0, revision1, revision2);

        for (DesignerCommandRevision revision : retainedBefore) {
            assertSame(revision,
                    changedC.retainedRevision(revision.revisionId())
                            .orElseThrow(),
                    "lookup must return this session's exact retained identity");
        }
        assertTrue(changedC.retainedRevision(-1).isEmpty());
        assertTrue(changedC.retainedRevision(Long.MAX_VALUE).isEmpty());

        DesignerCommandSession saved = changedC.markSaved();

        assertEquals(changedC.revisionCount(), saved.revisionCount());
        for (DesignerCommandRevision previous : retainedBefore) {
            DesignerCommandRevision reanchored = saved.retainedRevision(
                    previous.revisionId()).orElseThrow();
            assertEquals(previous.revisionId(), reanchored.revisionId());
            assertNotSame(previous, reanchored,
                    "markSaved must derive a new revision identity for every retained id");
        }
        assertEquals(revision2.revisionId(), saved.savedRevisionId());
        DesignerCommandRevision savedRevision = saved.retainedRevision(
                saved.savedRevisionId()).orElseThrow();
        assertSame(saved.current(), savedRevision);
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                savedRevision.persistenceKind());
        assertTrue(saved.retainedRevision(-1).isEmpty());
        assertTrue(saved.retainedRevision(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void discardedSavedBranchNeverAliasesCleanRevisionIdentity() throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession savedB = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B"))).markSaved();
        long savedId = savedB.savedRevisionId();
        DesignerCommandSession undoA = savedB.undo().session();

        DesignerCommandSession branchC = applied(undoA, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("C")));

        assertTrue(branchC.dirty());
        assertFalse(branchC.canRedo());
        assertEquals(savedId, branchC.savedRevisionId());
        assertEquals(OptionalInt.empty(), branchC.savedCursor());
        assertNotEquals(savedId, branchC.current().revisionId());
        assertTrue(new String(
                branchC.current().dartCandidateBytes(), StandardCharsets.UTF_8).contains("'C'"));
    }

    @Test
    void aNewSemanticRevisionEqualToDurableAnchorRemainsDirtyUntilSaved() throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "A"), text(SECOND_ID, "second")));
        DesignerCommandSession changed = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("B")));

        DesignerCommandSession returned = applied(changed, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("A")));

        assertEquals(initial.current().document(), returned.current().document());
        assertEquals(DesignerRevisionPersistenceKind.BASELINE,
                returned.current().persistenceKind());
        assertTrue(returned.dirty(),
                "clean state is revision identity, not candidate byte equality");
        assertFalse(returned.markSaved().dirty());
    }

    @Test
    void byteSnapshotsAreCloneSafeAndEncodingIsDeterministic() throws Exception {
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DesignerCommand command = new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("changed"));
        DesignerCommandSession first = applied(session(fixture), command);
        DesignerCommandSession second = applied(session(fixture), command);

        assertArrayEquals(first.current().fdBytes(), second.current().fdBytes());
        assertArrayEquals(first.current().dartCandidateBytes(),
                second.current().dartCandidateBytes());
        assertEquals(first.current().document(), second.current().document());

        byte[] fdCopy = first.current().fdBytes();
        byte[] dartCopy = first.current().dartCandidateBytes();
        byte[] anchorCopy = first.durableDartAnchorBytes();
        fdCopy[0] ^= 0x7f;
        dartCopy[0] ^= 0x7f;
        anchorCopy[0] ^= 0x7f;
        assertFalse(fdCopy[0] == first.current().fdBytes()[0]);
        assertFalse(dartCopy[0] == first.current().dartCandidateBytes()[0]);
        assertFalse(anchorCopy[0] == first.durableDartAnchorBytes()[0]);
    }

    @Test
    void noChangeAndHistoryBoundsRetainExactSession() throws Exception {
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DesignerCommandLimits defaults = DesignerCommandLimits.defaults();
        DesignerCommandLimits oneEdit = new DesignerCommandLimits(
                defaults.fdCodecLimits(),
                defaults.generationLimits(),
                defaults.sourceLimits(),
                defaults.validationLimits(),
                1,
                defaults.maxRetainedPairBytes());
        DesignerCommandSession initial = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG, oneEdit).session().orElseThrow();

        DesignerCommandSessionResult noChange = initial.apply(new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("first")));
        assertEquals(DesignerCommandStatus.NO_CHANGE, noChange.status());
        assertSame(initial, noChange.session());
        assertEquals(DesignerCommandDiagnosticCode.NO_CHANGE,
                noChange.diagnostics().getFirst().code());

        DesignerCommandSession once = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("one")));
        DesignerCommandSessionResult limited = once.apply(new SetProperty(
                SECOND_ID, DATA, new PropertyValue.StringValue("two")));
        assertEquals(DesignerCommandStatus.LIMIT_EXCEEDED, limited.status());
        assertSame(once, limited.session());
        assertEquals(DesignerCommandDiagnosticCode.HISTORY_EDIT_LIMIT,
                limited.diagnostics().getFirst().code());
    }

    @Test
    void sharedProbeCapacityRejectsBeforePublishingCommandOrTruncatingRedo()
            throws Exception {
        DartCandidateCapacityBudget capacity = new DartCandidateCapacityBudget(
                "command-probe-boundary", 2 * 1024 * 1024, 6, 1);
        DesignerCommandLimits limits = commandLimits(capacity);
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DesignerCommandSession initial = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG, limits)
                .session().orElseThrow();
        assertSame(capacity, initial.current().candidateCapacityBudget());

        DesignerCommandSession changed = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("changed")));
        DesignerCommandSession undone = changed.undo().session();
        DesignerCommandEdit retainedRedo = undone.redoEdit().orElseThrow();
        DesignerCommandSessionResult rejected = undone.apply(new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 2),
                text(THIRD_ID, "third")));

        assertEquals(DesignerCommandStatus.LIMIT_EXCEEDED, rejected.status());
        assertSame(undone, rejected.session());
        assertSame(undone.current(), rejected.session().current());
        assertTrue(rejected.edit().isEmpty());
        assertEquals(0, rejected.session().cursor());
        assertEquals(2, rejected.session().revisionCount());
        assertSame(retainedRedo, rejected.session().redoEdit().orElseThrow());
        assertEquals(DesignerCommandDiagnosticCode.SYMBOL_PROBE_CAPACITY_LIMIT,
                rejected.diagnostics().getFirst().code());

        DesignerCommandSessionResult acceptedAfterRejection =
                rejected.session().apply(new SetProperty(
                        SECOND_ID,
                        DATA,
                        new PropertyValue.StringValue("accepted")));
        assertEquals(DesignerCommandStatus.APPLIED,
                acceptedAfterRejection.status());
        assertEquals(2,
                acceptedAfterRejection.session().current().revisionId(),
                "capacity rejection must not consume the next revision identity");
    }

    @Test
    void sharedProbeCapacityAcceptsTheExactCommandBoundaryAndRetainsIdentity()
            throws Exception {
        DartCandidateCapacityBudget capacity = new DartCandidateCapacityBudget(
                "command-probe-exact", 2 * 1024 * 1024, 7, 1);
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DesignerCommandSession initial = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG, commandLimits(capacity))
                .session().orElseThrow();

        DesignerCommandSessionResult result = initial.apply(new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, 2),
                text(THIRD_ID, "third")));

        assertEquals(DesignerCommandStatus.APPLIED, result.status(),
                () -> result.diagnostics().toString());
        DesignerCommandRevision revision = result.session().current();
        assertSame(capacity, revision.candidateCapacityBudget());
        assertEquals(6, revision.generation().generated().orElseThrow()
                .symbolOccurrences().size());
        assertSame(revision.generation(), revision.preparedPair().orElseThrow()
                .dartTransition().generation());
    }

    @Test
    void baselineOverSharedProbeCapacityFailsClosed() throws Exception {
        DartCandidateCapacityBudget capacity = new DartCandidateCapacityBudget(
                "command-baseline-over-capacity", 2 * 1024 * 1024, 5, 1);
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));

        DesignerCommandSessionOpenResult result = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG, commandLimits(capacity));

        assertEquals(DesignerCommandStatus.LIMIT_EXCEEDED, result.status());
        assertTrue(result.session().isEmpty());
        assertEquals(DesignerCommandDiagnosticCode.BASELINE_GENERATION_FAILED,
                result.diagnostics().getFirst().code());
    }

    @Test
    void commandGenerationUsesTheConfiguredValidationNodeBoundary()
            throws Exception {
        int baselineChildCount = ValidationLimits.DEFAULT_MAX_NODES - 2;
        int configuredMaxNodes = ValidationLimits.DEFAULT_MAX_NODES + 1;
        ValidationLimits defaults = ValidationLimits.defaults();
        ValidationLimits validation = new ValidationLimits(
                defaults.maxDepth(),
                configuredMaxNodes,
                defaults.maxPropertiesPerWidget(),
                defaults.maxSlotsPerWidget(),
                defaults.maxIssues());
        DartCandidateCapacityBudget capacity = new DartCandidateCapacityBudget(
                "command-validation-boundary",
                DartCandidateCapacityBudget.DEFAULT_MAX_CANDIDATE_UTF8_BYTES,
                configuredMaxNodes + 5,
                1);
        DartGenerationLimits generation = new DartGenerationLimits(
                DartGenerationLimits.DEFAULT_MAX_TOTAL_PAYLOAD_UTF8_BYTES,
                DartGenerationLimits.DEFAULT_MAX_IMPORTS,
                DartGenerationLimits.DEFAULT_MAX_VALUE_CODE_POINTS,
                capacity);
        FdCodecLimits fdLimits = withMaxWidgetNodes(configuredMaxNodes);
        DesignerCommandLimits commandLimits = commandLimits(
                fdLimits, generation, validation);
        WidgetNode[] baselineChildren = new WidgetNode[baselineChildCount];
        for (int index = 0; index < baselineChildren.length; index++) {
            baselineChildren[index] = WidgetNode.empty(
                    StableId.random(), type("flutter.widgets.SizedBox"));
        }
        Fixture fixture = fixture(
                fdLimits, generation, validation, baselineChildren);
        DesignerCommandSession initial = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG, commandLimits)
                .session().orElseThrow();

        WidgetNode twoNodeSubtree = new WidgetNode(
                WRAPPER_ID,
                type("flutter.widgets.Center"),
                Map.of(),
                Map.of(
                        CHILD,
                        WidgetSlot.SingleSlot.of(WidgetNode.empty(
                                THIRD_ID,
                                type("flutter.widgets.SizedBox")))));
        DesignerCommandSessionResult exact = initial.apply(new AddWidget(
                new WidgetPlacement(ROOT_ID, CHILDREN, baselineChildCount),
                twoNodeSubtree));

        assertEquals(DesignerCommandStatus.APPLIED, exact.status(),
                () -> exact.diagnostics().toString());
        assertEquals(configuredMaxNodes - 2,
                ((WidgetSlot.ListSlot) exact.session().current().document()
                        .root().slots().get(CHILDREN)).children().size());

        DesignerCommandSessionResult exceeded = exact.session().apply(
                new AddWidget(
                        new WidgetPlacement(
                                ROOT_ID, CHILDREN, baselineChildCount + 1),
                        WidgetNode.empty(
                                StableId.random(),
                                type("flutter.widgets.SizedBox"))));
        assertEquals(DesignerCommandStatus.LIMIT_EXCEEDED, exceeded.status());
        assertSame(exact.session(), exceeded.session());
        assertEquals(DesignerCommandDiagnosticCode.RESULT_MODEL_INVALID,
                exceeded.diagnostics().getFirst().code());
        assertTrue(exceeded.diagnostics().getFirst().message()
                .contains("designer.tree.nodes.limit"));
    }

    @Test
    void openFailsClosedForSubstitutedDartOrCatalog() throws Exception {
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        byte[] substituted = fixture.dart().clone();
        int text = new String(substituted, StandardCharsets.UTF_8).indexOf("'first'");
        substituted[text + 1] = 'x';

        DesignerCommandSessionOpenResult sourceConflict = DesignerCommandSession.open(
                fixture.fd(), substituted, CATALOG);
        assertEquals(DesignerCommandStatus.CONFLICT, sourceConflict.status());
        assertTrue(sourceConflict.session().isEmpty());
        assertEquals(DesignerCommandDiagnosticCode.BASELINE_SOURCE_CONFLICT,
                sourceConflict.diagnostics().getFirst().code());

        DesignerCommandSessionOpenResult catalogConflict = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), WidgetCatalog.strict(List.of()));
        assertEquals(DesignerCommandStatus.CONFLICT, catalogConflict.status());
        assertEquals(DesignerCommandDiagnosticCode.BASELINE_MODEL_INVALID,
                catalogConflict.diagnostics().getFirst().code());
    }

    @Test
    void openVerifiedRetainsExactEvidenceIdentityForDerivedPairedRevision()
            throws Exception {
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult source = scanner.scan(
                fixture.dart(), fixture.document().source());
        DartGenerationResult generation = new DartRegionGenerator().generate(
                fixture.document(), CATALOG);
        DartThreeWayIntegrityResult threeWay = new DartThreeWayIntegrityGate(scanner)
                .evaluate(source, fixture.document().source(), generation);

        DesignerCommandSessionOpenResult opened = DesignerCommandSession.openVerified(
                fixture.fd(), fixture.dart(), CATALOG, source, threeWay);

        assertTrue(opened.ready(), () -> opened.diagnostics().toString());
        DesignerCommandSession changed = applied(
                opened.session().orElseThrow(),
                new SetProperty(
                        FIRST_ID,
                        DATA,
                        new PropertyValue.StringValue("changed")));
        assertEquals(DesignerRevisionPersistenceKind.PAIRED,
                changed.current().persistenceKind());
        assertSame(threeWay,
                changed.current().sourceTransition().orElseThrow().baseline());
        assertSame(threeWay,
                changed.current().preparedPair().orElseThrow()
                        .dartTransition().baseline());
        assertSame(source, threeWay.source());
    }

    @Test
    void openVerifiedRejectsDetachedBytesSourceAndGenerationEvidence()
            throws Exception {
        Fixture fixture = fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second"));
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner();
        DartSourceIntegrityResult source = scanner.scan(
                fixture.dart(), fixture.document().source());
        DartGenerationResult generation = new DartRegionGenerator().generate(
                fixture.document(), CATALOG);
        DartThreeWayIntegrityResult threeWay = new DartThreeWayIntegrityGate(scanner)
                .evaluate(source, fixture.document().source(), generation);

        byte[] differentBytes = Arrays.copyOf(fixture.dart(), fixture.dart().length + 1);
        differentBytes[differentBytes.length - 1] = '\n';
        assertEvidenceMismatch(DesignerCommandSession.openVerified(
                fixture.fd(), differentBytes, CATALOG, source, threeWay));

        DartSourceIntegrityResult detachedEquivalent = scanner.scan(
                fixture.dart(), fixture.document().source());
        assertNotSame(source, detachedEquivalent);
        assertEvidenceMismatch(DesignerCommandSession.openVerified(
                fixture.fd(), fixture.dart(), CATALOG, detachedEquivalent, threeWay));

        DesignerCommandSession changed = applied(
                DesignerCommandSession.open(
                        fixture.fd(), fixture.dart(), CATALOG).session().orElseThrow(),
                new SetProperty(
                        FIRST_ID,
                        DATA,
                        new PropertyValue.StringValue("other")));
        DartThreeWayIntegrityResult wrongGeneration = new DartThreeWayIntegrityGate(scanner)
                .evaluate(
                        source,
                        changed.current().document().source(),
                        changed.current().generation());
        assertEvidenceMismatch(DesignerCommandSession.openVerified(
                fixture.fd(), fixture.dart(), CATALOG, source, wrongGeneration));
    }

    @Test
    void unavailableUndoRedoHaveStableDiagnostics() throws Exception {
        DesignerCommandSession initial = session(
                fixture(text(FIRST_ID, "first"), text(SECOND_ID, "second")));
        DesignerCommandSessionResult undo = initial.undo();
        assertEquals(DesignerCommandStatus.NOT_AVAILABLE, undo.status());
        assertSame(initial, undo.session());
        assertEquals(DesignerCommandDiagnosticCode.UNDO_NOT_AVAILABLE,
                undo.diagnostics().getFirst().code());

        DesignerCommandSession changed = applied(initial, new SetProperty(
                FIRST_ID, DATA, new PropertyValue.StringValue("changed")));
        DesignerCommandSessionResult redo = changed.redo();
        assertEquals(DesignerCommandStatus.NOT_AVAILABLE, redo.status());
        assertSame(changed, redo.session());
        assertEquals(DesignerCommandDiagnosticCode.REDO_NOT_AVAILABLE,
                redo.diagnostics().getFirst().code());
    }

    private static void assertRejected(
            DesignerCommandSession session,
            DesignerCommand command,
            DesignerCommandDiagnosticCode code) {
        DesignerCommandSessionResult result = session.apply(command);
        assertFalse(result.changed());
        assertSame(session, result.session());
        assertEquals(code, result.diagnostics().getFirst().code());
    }

    private static void assertRejectedUnchanged(
            DesignerCommandSession session,
            DesignerCommand command,
            DesignerCommandDiagnosticCode code) {
        byte[] fd = session.current().fdBytes();
        byte[] dart = session.current().dartCandidateBytes();
        DesignerDocument document = session.current().document();
        int cursor = session.cursor();
        boolean canUndo = session.canUndo();
        boolean canRedo = session.canRedo();

        DesignerCommandSessionResult result = session.apply(command);

        assertFalse(result.changed());
        assertSame(session, result.session());
        assertEquals(code, result.diagnostics().getFirst().code());
        assertEquals(cursor, result.session().cursor());
        assertEquals(canUndo, result.session().canUndo());
        assertEquals(canRedo, result.session().canRedo());
        assertSame(document, result.session().current().document());
        assertArrayEquals(fd, result.session().current().fdBytes());
        assertArrayEquals(dart, result.session().current().dartCandidateBytes());
    }

    private static void assertEvidenceMismatch(
            DesignerCommandSessionOpenResult result) {
        assertEquals(DesignerCommandStatus.CONFLICT, result.status());
        assertTrue(result.session().isEmpty());
        assertEquals(DesignerCommandDiagnosticCode.BASELINE_EVIDENCE_MISMATCH,
                result.diagnostics().getFirst().code());
    }

    private static DesignerCommandSession applied(
            DesignerCommandSession session,
            DesignerCommand command) {
        DesignerCommandSessionResult result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(),
                () -> result.diagnostics().toString());
        return result.session();
    }

    private static PreparedDesignerPair exactOverlayPair(
            DesignerCommandSession dirty,
            String unmanagedSuffix) {
        DesignerCommandRevision revision = dirty.current();
        PreparedDesignerPair canonical = revision.preparedPair().orElseThrow();
        byte[] liveOverlay = (new String(
                canonical.liveDartBytes(), StandardCharsets.UTF_8)
                + unmanagedSuffix).getBytes(StandardCharsets.UTF_8);
        var transition = new DartSourceTransitionPlanner(
                new DartSourceIntegrityScanner(dirty.limits().sourceLimits()))
                .plan(
                        dirty.durableThreeWayIntegrity(),
                        liveOverlay,
                        canonical.baselineDocument().source(),
                        revision.generation());
        assertTrue(transition.ready(), () -> transition.diagnostics().toString());
        var prepared = new DesignerPairPreparationPlanner(
                new FdDocumentCodec(dirty.limits().fdCodecLimits()))
                .prepare(
                        canonical.baselineFd(),
                        revision.document(),
                        transition.plan().orElseThrow());
        assertTrue(prepared.ready(), () -> prepared.diagnostics().toString());
        return prepared.preparedPair().orElseThrow();
    }

    private static byte[] withCrLfInsideBuildPayload(
            DesignerCommandRevision revision) {
        byte[] source = revision.dartCandidateBytes();
        var build = revision.sourceIntegrity().region(
                DartSourceIntegrityScanner.BUILD_REGION).orElseThrow();
        int newline = -1;
        for (int index = build.payloadStartByte();
                index < build.payloadEndByte(); index++) {
            if (source[index] == '\n'
                    && (index == 0 || source[index - 1] != '\r')) {
                newline = index;
                break;
            }
        }
        if (newline < 0) {
            throw new AssertionError("Expected LF inside generated build payload");
        }
        byte[] changed = new byte[source.length + 1];
        System.arraycopy(source, 0, changed, 0, newline);
        changed[newline] = '\r';
        System.arraycopy(
                source,
                newline,
                changed,
                newline + 1,
                source.length - newline);
        return changed;
    }

    private static DesignerCommandSession session(Fixture fixture) {
        DesignerCommandSessionOpenResult result = DesignerCommandSession.open(
                fixture.fd(), fixture.dart(), CATALOG);
        assertTrue(result.ready(), () -> result.diagnostics().toString());
        return result.session().orElseThrow();
    }

    private static DesignerCommandLimits commandLimits(
            DartCandidateCapacityBudget capacity) {
        DesignerCommandLimits defaults = DesignerCommandLimits.defaults();
        DartGenerationLimits generation = defaults.generationLimits();
        return commandLimits(
                defaults.fdCodecLimits(),
                new DartGenerationLimits(
                        generation.maxTotalPayloadUtf8Bytes(),
                        generation.maxImports(),
                        generation.maxValueCodePoints(),
                        capacity),
                defaults.validationLimits());
    }

    private static DesignerCommandLimits commandLimits(
            FdCodecLimits fdLimits,
            DartGenerationLimits generation,
            ValidationLimits validation) {
        DesignerCommandLimits defaults = DesignerCommandLimits.defaults();
        return new DesignerCommandLimits(
                fdLimits,
                generation,
                defaults.sourceLimits(),
                validation,
                defaults.maxHistoryEdits(),
                defaults.maxRetainedPairBytes());
    }

    private static Fixture fixture(WidgetNode... children) throws Exception {
        return fixture(
                FdCodecLimits.defaults(),
                DartGenerationLimits.defaults(),
                ValidationLimits.defaults(),
                children);
    }

    private static Fixture fixture(
            FdCodecLimits fdLimits,
            DartGenerationLimits generationLimits,
            ValidationLimits validationLimits,
            WidgetNode... children) throws Exception {
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                type("flutter.widgets.Column"),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(children))),
                Extensions.empty());
        DartSourceDescriptor placeholder = descriptor("0".repeat(64), "0".repeat(64));
        DesignerDocument provisional = new DesignerDocument(
                Optional.empty(),
                DOCUMENT_ID,
                placeholder,
                Optional.empty(),
                root,
                Extensions.empty());
        DartRegionGenerator generator = new DartRegionGenerator(
                generationLimits, validationLimits);
        GeneratedDartRegions generated = generator
                .generate(provisional, CATALOG).generated().orElseThrow();
        DartSourceDescriptor source = descriptor(
                generated.imports().normalizedSha256(),
                generated.build().normalizedSha256());
        DesignerDocument document = new DesignerDocument(
                Optional.empty(),
                DOCUMENT_ID,
                source,
                Optional.empty(),
                root,
                Extensions.empty());
        DartGenerationResult verified = generator.generate(document, CATALOG);
        GeneratedDartRegions regions = verified.generated().orElseThrow();
        byte[] dart = sourceBytes(
                regions.imports().payload(), regions.build().payload());
        OriginalFdBytes fd = new FdDocumentCodec(fdLimits).encode(document);
        return new Fixture(document, fd, dart);
    }

    private static FdCodecLimits withMaxWidgetNodes(int maxWidgetNodes) {
        FdCodecLimits defaults = FdCodecLimits.defaults();
        return new FdCodecLimits(
                defaults.maxDocumentBytes(),
                defaults.maxJsonNestingDepth(),
                defaults.maxJsonTokens(),
                defaults.maxFieldNameUtf16Units(),
                defaults.maxStringUtf16Units(),
                defaults.maxStringCodePoints(),
                defaults.maxNumberCharacters(),
                defaults.maxAbsoluteDecimalScale(),
                defaults.maxWidgetDepth(),
                maxWidgetNodes,
                defaults.maxPropertiesPerWidget(),
                defaults.maxSlotsPerWidget(),
                defaults.maxListChildren(),
                defaults.maxExtensionKeysPerBag(),
                defaults.maxExtensionNestingDepth(),
                defaults.maxExtensionValues(),
                defaults.maxJsonObjectFields(),
                defaults.maxJsonArrayElements(),
                defaults.maxDiagnostics());
    }

    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }

    private static byte[] sourceBytes(String imports, String build) {
        return ("// <netbeans-flutter-designer region=\"imports\">\n"
                + imports
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + build
                + "  // </netbeans-flutter-designer>\n"
                + "}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static List<WidgetNode> rootChildren(DesignerCommandSession session) {
        return ((WidgetSlot.ListSlot) session.current().document().root()
                .slots().get(CHILDREN)).children();
    }

    private static List<StableId> rootChildIds(DesignerCommandSession session) {
        return rootChildren(session).stream().map(WidgetNode::id).toList();
    }

    private static WidgetNode find(WidgetNode node, StableId id) {
        if (node.id().equals(id)) {
            return node;
        }
        for (WidgetSlot value : node.slots().values()) {
            List<WidgetNode> children = switch (value) {
                case WidgetSlot.SingleSlot single -> single.child().stream().toList();
                case WidgetSlot.ListSlot list -> list.children();
            };
            for (WidgetNode child : children) {
                WidgetNode found = findOrNull(child, id);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new IllegalArgumentException("missing widget " + id);
    }

    private static WidgetNode findOrNull(WidgetNode node, StableId id) {
        if (node.id().equals(id)) {
            return node;
        }
        for (WidgetSlot value : node.slots().values()) {
            List<WidgetNode> children = switch (value) {
                case WidgetSlot.SingleSlot single -> single.child().stream().toList();
                case WidgetSlot.ListSlot list -> list.children();
            };
            for (WidgetNode child : children) {
                WidgetNode found = findOrNull(child, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static WidgetNode text(StableId id, String value) {
        return new WidgetNode(
                id,
                type("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
    }

    private static WidgetNode singleChild(
            StableId id,
            String type,
            SlotName slot,
            WidgetNode child) {
        return new WidgetNode(
                id,
                type(type),
                Map.of(),
                Map.of(slot, WidgetSlot.SingleSlot.of(child)),
                Extensions.empty());
    }

    private static WidgetNode singleChild(WidgetNode owner, SlotName slot) {
        return ((WidgetSlot.SingleSlot) owner.slots().get(slot))
                .child().orElseThrow();
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }

    private static PropertyName property(String value) {
        return new PropertyName(value);
    }

    private static SlotName slot(String value) {
        return new SlotName(value);
    }

    private record Fixture(
            DesignerDocument document,
            OriginalFdBytes fd,
            byte[] dart) {
        Fixture {
            dart = dart.clone();
        }

        @Override
        public byte[] dart() {
            return dart.clone();
        }
    }
}
