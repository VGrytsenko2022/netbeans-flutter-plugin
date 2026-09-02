package dev.flutter.netbeans.plugin.designer.properties;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.awt.Component;
import java.awt.Container;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlutterWidgetSlotPropertyEditorTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final SlotName BODY = new SlotName("body");
    private static final SlotName APP_BAR_SLOT = new SlotName("appBar");
    private static final SlotName BOTTOM = new SlotName("bottom");
    private static final SlotName CHILD = new SlotName("child");
    private static final SlotName CHILDREN = new SlotName("children");

    @Test
    void addIsATransactionalDraftAndDispatchesOneExactIntentAfterOk()
            throws Exception {
        WidgetDefinition scaffoldDefinition = definition("flutter.material.Scaffold");
        WidgetNode scaffold = new WidgetNode(
                id("082e80e3-06ed-418f-9478-18d8fcb68333"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        DesignerDocument document = document(scaffold);
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document,
                CATALOG,
                List.of(type("flutter.widgets.Center"), type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                scaffold,
                scaffoldDefinition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> body = slotProperty(node, "body");
        PropertyEditor editor = body.getPropertyEditor();
        editor.setValue(body.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Body"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            assertNotNull(action);
            assertNotNull(addType);
            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Center");

            assertEquals("Empty", editor.getAsText(),
                    "dialog controls must keep an uncommitted local draft");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotCellValue staged = assertInstanceOf(
                    FlutterWidgetSlotCellValue.class, editor.getValue());
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    staged.mutation().orElseThrow());
            assertEquals(scaffold.id(), add.ownerId());
            assertEquals(BODY, add.slotName());
            assertEquals(type("flutter.widgets.Center"), add.widgetType());
            assertEquals(0, add.index());

            body.setValue(staged);
            body.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted slot dialog may consume its revision lease once");
            return null;
        });
    }

    @Test
    void sizedBoxChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition sizedBoxDefinition = definition("flutter.widgets.SizedBox");
        WidgetNode sizedBox = new WidgetNode(
                id("c10d24fa-0186-4866-8c86-8f004e4fcb42"),
                sizedBoxDefinition.typeId(),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(sizedBox),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                sizedBox,
                sizedBoxDefinition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "SizedBox.child must remain unchanged until dialog validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotCellValue staged = assertInstanceOf(
                    FlutterWidgetSlotCellValue.class, editor.getValue());
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    staged.mutation().orElseThrow());
            assertEquals(sizedBox.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted SizedBox child dialog may consume its revision lease once");
            return null;
        });
    }

    @Test
    void listSlotOffersTextFieldAsAnImmediateLeafInsertion() throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetNode column = WidgetNodePrototypeFactory.create(
                columnDefinition,
                id("c66a2a48-d31e-4c81-bdb8-8c379ee48436"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(column),
                CATALOG,
                List.of(type("flutter.material.TextField")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                column,
                columnDefinition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> children =
                slotProperty(node, "children");
        PropertyEditor editor = children.getPropertyEditor();
        editor.setValue(children.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Children"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            assertTrue(labels(action).contains("Add new widget"));
            assertEquals(List.of("Text Field"), labels(addType));
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text Field");
            environment.setState(PropertyEnv.STATE_VALID);

            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(column.id(), add.ownerId());
            assertEquals(CHILDREN, add.slotName());
            assertEquals(type("flutter.material.TextField"), add.widgetType());
            assertEquals(0, add.index());
            children.setValue((FlutterWidgetSlotCellValue) editor.getValue());
            assertEquals(List.of(add), submitted);
            return null;
        });
    }

    @Test
    void stableSlotPropertyRearmsItsLocalGateForEachRefreshedHandler()
            throws Exception {
        WidgetDefinition scaffoldDefinition = definition(
                "flutter.material.Scaffold");
        WidgetNode scaffold = new WidgetNode(
                id("9ed02795-dd94-4277-91fc-b643d5260d7b"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        FlutterWidgetSlotEditorContext firstContext =
                new FlutterWidgetSlotEditorContext(
                        document(scaffold),
                        CATALOG,
                        List.of(type("flutter.widgets.Center")));
        FlutterWidgetSlotEditorContext secondContext =
                new FlutterWidgetSlotEditorContext(
                        document(scaffold),
                        CATALOG,
                        List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> firstRevision = new ArrayList<>();
        List<FlutterWidgetSlotMutation> secondRevision = new ArrayList<>();
        FlutterWidgetPropertiesNode.PropertyMutationHandler propertyHandler =
                ignored -> { };
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                scaffold,
                scaffoldDefinition,
                propertyHandler,
                firstContext,
                firstRevision::add);
        Node.Property<FlutterWidgetSlotCellValue> stableBody =
                slotProperty(node, "body");
        FlutterWidgetSlotMutation.Add addCenter =
                new FlutterWidgetSlotMutation.Add(
                        scaffold.id(), BODY,
                        type("flutter.widgets.Center"), 0);
        FlutterWidgetSlotCellValue firstIntent =
                FlutterWidgetSlotCellValue.staged("Add Center", addCenter);

        stableBody.setValue(firstIntent);
        stableBody.setValue(firstIntent);
        assertEquals(List.of(addCenter), firstRevision,
                "one handler/revision may consume the staged intent once");

        node.refreshPresentation(
                scaffold,
                scaffoldDefinition,
                propertyHandler,
                secondContext,
                secondRevision::add,
                FlutterImageAssetChoices.empty());

        assertSame(stableBody, slotProperty(node, "body"));
        FlutterWidgetSlotMutation.Add addText =
                new FlutterWidgetSlotMutation.Add(
                        scaffold.id(), BODY,
                        type("flutter.widgets.Text"), 0);
        FlutterWidgetSlotCellValue secondIntent =
                FlutterWidgetSlotCellValue.staged("Add Text", addText);
        stableBody.setValue(secondIntent);
        stableBody.setValue(secondIntent);

        assertEquals(List.of(addCenter), firstRevision,
                "refresh must never redispatch through the prior revision handler");
        assertEquals(List.of(addText), secondRevision,
                "a new handler must rearm the stable Property for exactly one intent");
    }

    @Test
    void aspectRatioChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.AspectRatio");
        WidgetNode aspectRatio = WidgetNodePrototypeFactory.create(
                definition,
                id("4b1f7b08-642b-46da-969d-019b20aa8edf"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(aspectRatio),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                aspectRatio,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "AspectRatio.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(aspectRatio.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted AspectRatio child edit consumes one lease");
            return null;
        });
    }

    @Test
    void opacityChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Opacity");
        WidgetNode opacity = WidgetNodePrototypeFactory.create(
                definition,
                id("7e8cb67e-c4b4-40f5-9244-9840348283d2"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(opacity),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                opacity,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "Opacity.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(opacity.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted Opacity child edit consumes one lease");
            return null;
        });
    }

    @Test
    void alignChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.Align");
        WidgetNode align = WidgetNodePrototypeFactory.create(
                definition,
                id("141ba5c4-9065-4598-975b-7e33f2f517bd"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(align),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                align,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "Align.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(align.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted Align child edit consumes one lease");
            return null;
        });
    }

    @Test
    void fractionallySizedBoxChildAddsTextAsOneExactTransactionalIntent()
            throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.FractionallySizedBox");
        WidgetNode fractionallySizedBox = WidgetNodePrototypeFactory.create(
                definition,
                id("e33804b6-ed2a-491f-a8ea-a87cb9430c8a"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(fractionallySizedBox),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                fractionallySizedBox,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "FractionallySizedBox.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(fractionallySizedBox.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted FractionallySizedBox child edit consumes one lease");
            return null;
        });
    }

    @Test
    void fittedBoxChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.FittedBox");
        WidgetNode fittedBox = WidgetNodePrototypeFactory.create(
                definition,
                id("b587a092-9a65-420a-9d1c-e127cc752f8d"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(fittedBox),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                fittedBox,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "FittedBox.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(fittedBox.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted FittedBox child edit consumes one lease");
            return null;
        });
    }

    @Test
    void constrainedBoxChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.ConstrainedBox");
        WidgetNode constrainedBox = WidgetNodePrototypeFactory.create(
                definition,
                id("dc8305ac-41e7-431b-91d4-14addcaac591"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(constrainedBox),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                constrainedBox,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "ConstrainedBox.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(constrainedBox.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted ConstrainedBox child edit consumes one lease");
            return null;
        });
    }

    @Test
    void unconstrainedBoxChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.UnconstrainedBox");
        WidgetNode unconstrainedBox = WidgetNodePrototypeFactory.create(
                definition,
                id("88db0707-d034-40ae-98ac-69a4e1002b35"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(unconstrainedBox),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                unconstrainedBox,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "UnconstrainedBox.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(unconstrainedBox.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted UnconstrainedBox child edit consumes one lease");
            return null;
        });
    }

    @Test
    void limitedBoxChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.LimitedBox");
        WidgetNode limitedBox = WidgetNodePrototypeFactory.create(
                definition,
                id("8d958c9c-fc08-45b0-b4a3-63236c71cf27"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(limitedBox),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                limitedBox,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "LimitedBox.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(limitedBox.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted LimitedBox child edit consumes one lease");
            return null;
        });
    }

    @Test
    void overflowBoxChildAddsTextAsOneExactTransactionalIntent() throws Exception {
        WidgetDefinition definition = definition("flutter.widgets.OverflowBox");
        WidgetNode overflowBox = WidgetNodePrototypeFactory.create(
                definition,
                id("b9d91c9d-81c1-47ce-8d14-110e31ef75ae"));
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(overflowBox),
                CATALOG,
                List.of(type("flutter.widgets.Text")));
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                overflowBox,
                definition,
                ignored -> { },
                context,
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> child = slotProperty(node, "child");
        PropertyEditor editor = child.getPropertyEditor();
        editor.setValue(child.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Child"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            assertEquals("Empty", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals("Empty", editor.getAsText(),
                    "OverflowBox.child remains unchanged until validation");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION, environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(overflowBox.id(), add.ownerId());
            assertEquals(CHILD, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(0, add.index());

            FlutterWidgetSlotCellValue staged =
                    (FlutterWidgetSlotCellValue) editor.getValue();
            child.setValue(staged);
            child.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted OverflowBox child edit consumes one lease");
            return null;
        });
    }

    @Test
    void stackChildrenAddsNewFrontLayerAtExactTerminalPaintOrderIndex()
            throws Exception {
        WidgetDefinition stackDefinition = definition("flutter.widgets.Stack");
        WidgetNode back = text(
                id("42bb3e86-daab-4a97-9c1d-9515f34f7a8b"), "back");
        WidgetNode stackPrototype = WidgetNodePrototypeFactory.create(
                stackDefinition,
                id("8a51ab51-dd2e-40b4-8dba-b74ef9fc332e"));
        WidgetNode stack = new WidgetNode(
                stackPrototype.id(),
                stackPrototype.type(),
                stackPrototype.properties(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(back))),
                Extensions.empty());
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                stack,
                stackDefinition,
                ignored -> { },
                new FlutterWidgetSlotEditorContext(
                        document(stack),
                        CATALOG,
                        List.of(type("flutter.widgets.Text"))),
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> children =
                slotProperty(node, "children");
        PropertyEditor editor = children.getPropertyEditor();
        editor.setValue(children.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Children"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            JComboBox<?> position = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.POSITION_NAME,
                    JComboBox.class);
            JList<?> current = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.CURRENT_LIST_NAME,
                    JList.class);

            assertEquals(1, current.getModel().getSize());
            assertEquals("1 widget", editor.getAsText());
            selectLabel(action, "Add new widget");
            selectLabel(addType, "Text");
            assertEquals(List.of(), labels(position),
                    "fresh list children are terminal-only and expose no arbitrary index");
            assertEquals(PropertyEnv.STATE_NEEDS_VALIDATION,
                    environment.getState());
            assertEquals(List.of(), submitted);

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotCellValue staged = assertInstanceOf(
                    FlutterWidgetSlotCellValue.class, editor.getValue());
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    staged.mutation().orElseThrow());
            assertEquals(stack.id(), add.ownerId());
            assertEquals(CHILDREN, add.slotName());
            assertEquals(type("flutter.widgets.Text"), add.widgetType());
            assertEquals(1, add.index(),
                    "terminal Stack insertion paints the new layer in front");

            children.setValue(staged);
            children.setValue(staged);
            assertEquals(List.of(add), submitted,
                    "one accepted Stack children dialog consumes one revision lease");
            return null;
        });
    }

    @Test
    void preferredSizeSlotsOfferOnlyAppBarAndProduceExactAddIntent()
            throws Exception {
        WidgetDefinition scaffoldDefinition = definition("flutter.material.Scaffold");
        WidgetNode scaffold = new WidgetNode(
                id("fa19924b-c3b1-4112-b236-f33e4c343750"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(APP_BAR_SLOT, WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(scaffold),
                CATALOG,
                List.of(
                        type("flutter.widgets.Text"),
                        type("flutter.material.AppBar")));
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                scaffold,
                scaffoldDefinition,
                scaffoldDefinition.slot(APP_BAR_SLOT).orElseThrow(),
                context);
        PropertyEnv environment = PropertyEnv.create(descriptor("App bar"));
        editor.attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);

            selectLabel(action, "Add new widget");
            assertEquals(List.of("AppBar"), labels(addType),
                    "Text must not enter a PreferredSizeWidget slot");
            selectLabel(addType, "AppBar");
            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Add add = assertInstanceOf(
                    FlutterWidgetSlotMutation.Add.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(scaffold.id(), add.ownerId());
            assertEquals(APP_BAR_SLOT, add.slotName());
            assertEquals(type("flutter.material.AppBar"), add.widgetType());
            assertEquals(0, add.index());
            return null;
        });

        WidgetDefinition appBarDefinition = definition("flutter.material.AppBar");
        WidgetNode appBar = new WidgetNode(
                id("96d57566-d518-429d-8226-198189d40aec"),
                appBarDefinition.typeId(),
                Map.of(),
                Map.of(BOTTOM, WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        FlutterWidgetSlotPropertyEditor bottomEditor =
                new FlutterWidgetSlotPropertyEditor(
                        appBar,
                        appBarDefinition,
                        appBarDefinition.slot(BOTTOM).orElseThrow(),
                        new FlutterWidgetSlotEditorContext(
                                document(appBar),
                                CATALOG,
                                List.of(
                                        type("flutter.widgets.Text"),
                                        type("flutter.material.AppBar"))));
        PropertyEnv bottomEnvironment = PropertyEnv.create(descriptor("Bottom"));
        bottomEditor.attachEnv(bottomEnvironment);
        onEdt(() -> {
            Component custom = bottomEditor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            selectLabel(action, "Add new widget");
            assertEquals(List.of("AppBar"), labels(addType));
            return null;
        });
    }

    @Test
    void listSlotOffersExactReorderPositionsAndDirectChildRemoval()
            throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetNode first = text(
                id("a5bf370b-4189-497a-b529-ed4464ae6203"), "first");
        WidgetNode second = text(
                id("18adca4f-9d75-4455-9ecf-da7964b0ba9f"), "second");
        WidgetNode third = text(
                id("554e32b4-bc66-42d4-946c-d847be359054"), "third");
        WidgetNode column = new WidgetNode(
                id("c1289524-b4f4-47db-9245-4dfb0cc044fa"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(first, second, third))),
                Extensions.empty());
        DesignerDocument document = document(column);
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document, CATALOG, List.of(type("flutter.widgets.Text")));
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                column,
                columnDefinition,
                columnDefinition.slot(CHILDREN).orElseThrow(),
                context);
        PropertyEnv environment = PropertyEnv.create(descriptor("Children"));
        editor.attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> moveSource = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.MOVE_SOURCE_NAME,
                    JComboBox.class);
            JComboBox<?> position = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.POSITION_NAME,
                    JComboBox.class);
            JList<?> current = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.CURRENT_LIST_NAME,
                    JList.class);

            assertEquals(3, current.getModel().getSize());
            assertTrue(labels(action).contains("Move existing widget here"));
            assertTrue(labels(action).contains("Remove selected widget"));
            selectLabel(action, "Move existing widget here");
            selectContains(moveSource, first.id().toString());
            assertEquals(2, position.getItemCount(),
                    "same-slot no-op and post-removal out-of-range positions are absent");
            assertTrue(labels(position).contains("Position 2 (index 1)"));
            assertTrue(labels(position).contains("Last (index 2)"));
            selectLabel(position, "Last (index 2)");
            environment.setState(PropertyEnv.STATE_VALID);

            FlutterWidgetSlotMutation.Move move = assertInstanceOf(
                    FlutterWidgetSlotMutation.Move.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(first.id(), move.sourceId());
            assertEquals(column.id(), move.ownerId());
            assertEquals(CHILDREN, move.slotName());
            assertEquals(2, move.postRemovalIndex());
            assertEquals("Children slot editor",
                    ((JComponent) custom).getAccessibleContext().getAccessibleName());
            return null;
        });
    }

    @Test
    void movingCurrentLastChildDoesNotMislabelMiddlePositionAsLast()
            throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetNode first = text(
                id("249df88b-8a5b-4710-b253-1f6f5aba8f50"), "first");
        WidgetNode second = text(
                id("f154f77f-95b2-433b-bd41-4c7b408d4279"), "second");
        WidgetNode third = text(
                id("263c7021-35f1-4698-bd03-6c0d7937f5dd"), "third");
        WidgetNode column = new WidgetNode(
                id("e3428420-d69a-4545-9f57-dff4adb5bad3"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(first, second, third))),
                Extensions.empty());
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                column,
                columnDefinition,
                columnDefinition.slot(CHILDREN).orElseThrow(),
                new FlutterWidgetSlotEditorContext(
                        document(column),
                        CATALOG,
                        List.of(type("flutter.widgets.Text"))));
        PropertyEnv environment = PropertyEnv.create(descriptor("Children"));
        editor.attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> moveSource = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.MOVE_SOURCE_NAME,
                    JComboBox.class);
            JComboBox<?> position = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.POSITION_NAME,
                    JComboBox.class);

            selectLabel(action, "Move existing widget here");
            selectContains(moveSource, third.id().toString());

            assertEquals(List.of(
                    "First (index 0)",
                    "Position 2 (index 1)"), labels(position));
            assertFalse(labels(position).stream()
                    .anyMatch(label -> label.startsWith("Last")),
                    "the actual last position is a no-op and must not be offered");
            return null;
        });
    }

    @Test
    void occupiedSingleSlotOffersExplicitAtomicFreshReplacement() throws Exception {
        WidgetDefinition scaffoldDefinition = definition("flutter.material.Scaffold");
        WidgetNode center = new WidgetNode(
                id("e3a3ed63-209c-49fc-9bb7-d61a967522bd"),
                type("flutter.widgets.Center"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        WidgetNode scaffold = new WidgetNode(
                id("e00a3e9a-abf4-4a26-8af4-8fb1192f9d3b"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.of(center)),
                Extensions.empty());
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                scaffold,
                scaffoldDefinition,
                scaffoldDefinition.slot(BODY).orElseThrow(),
                new FlutterWidgetSlotEditorContext(
                        document(scaffold),
                        CATALOG,
                        List.of(type("flutter.widgets.Text"))));
        PropertyEnv environment = PropertyEnv.create(descriptor("Body"));
        editor.attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            JComboBox<?> moveSource = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.MOVE_SOURCE_NAME,
                    JComboBox.class);
            JComboBox<?> position = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.POSITION_NAME,
                    JComboBox.class);
            JList<?> current = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.CURRENT_LIST_NAME,
                    JList.class);
            assertFalse(labels(action).contains("Add new widget"));
            assertTrue(labels(action).contains("Replace with new widget"));
            assertTrue(labels(action).contains("Clear single child"));
            selectLabel(action, "Replace with new widget");
            selectLabel(addType, "Text");
            assertTrue(addType.isEnabled());
            assertFalse(moveSource.isEnabled());
            assertFalse(position.isEnabled());
            assertFalse(current.isEnabled());

            environment.setState(PropertyEnv.STATE_VALID);
            FlutterWidgetSlotMutation.Replace replace = assertInstanceOf(
                    FlutterWidgetSlotMutation.Replace.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(scaffold.id(), replace.ownerId());
            assertEquals(BODY, replace.slotName());
            assertEquals(center.id(), replace.expectedChildId());
            FlutterWidgetSlotMutation.Replace.NewWidget fresh = assertInstanceOf(
                    FlutterWidgetSlotMutation.Replace.NewWidget.class,
                    replace.replacement());
            assertEquals(type("flutter.widgets.Text"), fresh.widgetType());
            return null;
        });
    }

    @Test
    void occupiedSingleSlotOffersCompatibleExistingReplacementAndRejectsStaleDraft()
            throws Exception {
        WidgetDefinition scaffoldDefinition = definition("flutter.material.Scaffold");
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetNode center = new WidgetNode(
                id("347a80eb-b14c-4fa7-b4cc-bdc7565931ca"),
                type("flutter.widgets.Center"),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.empty()),
                Extensions.empty());
        WidgetNode scaffold = new WidgetNode(
                id("419485a8-40fc-4ef7-a5da-f4c5c651beaf"),
                scaffoldDefinition.typeId(),
                Map.of(),
                Map.of(BODY, WidgetSlot.SingleSlot.of(center)),
                Extensions.empty());
        WidgetNode source = text(
                id("a0c87acf-63bd-485d-b3ec-e91e21fca137"), "source");
        WidgetNode column = new WidgetNode(
                id("265c7fa8-d436-4fba-a87c-d93431507c20"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(scaffold, source))),
                Extensions.empty());
        FlutterWidgetSlotPropertyEditor editor = new FlutterWidgetSlotPropertyEditor(
                scaffold,
                scaffoldDefinition,
                scaffoldDefinition.slot(BODY).orElseThrow(),
                new FlutterWidgetSlotEditorContext(
                        document(column),
                        CATALOG,
                        List.of(type("flutter.widgets.Text"))));
        PropertyEnv environment = PropertyEnv.create(descriptor("Body"));
        editor.attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JComboBox<?> moveSource = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.MOVE_SOURCE_NAME,
                    JComboBox.class);
            JComboBox<?> position = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.POSITION_NAME,
                    JComboBox.class);
            assertTrue(labels(action).contains("Replace with existing widget"));
            selectLabel(action, "Replace with existing widget");
            assertEquals(1, moveSource.getItemCount(),
                    "root, current child and cyclic/incompatible sources stay unavailable");
            selectContains(moveSource, source.id().toString());
            assertTrue(moveSource.isEnabled());
            assertFalse(position.isEnabled());
            environment.setState(PropertyEnv.STATE_VALID);

            FlutterWidgetSlotMutation.Replace replace = assertInstanceOf(
                    FlutterWidgetSlotMutation.Replace.class,
                    ((FlutterWidgetSlotCellValue) editor.getValue())
                            .mutation().orElseThrow());
            assertEquals(center.id(), replace.expectedChildId());
            FlutterWidgetSlotMutation.Replace.ExistingWidget existing =
                    assertInstanceOf(
                            FlutterWidgetSlotMutation.Replace.ExistingWidget.class,
                            replace.replacement());
            assertEquals(source.id(), existing.sourceId());
            return null;
        });

        FlutterWidgetSlotCellValue stale = FlutterWidgetSlotCellValue.staged(
                "stale replacement",
                new FlutterWidgetSlotMutation.Replace(
                        scaffold.id(),
                        BODY,
                        source.id(),
                        new FlutterWidgetSlotMutation.Replace.NewWidget(
                                type("flutter.widgets.Text"))));
        assertThrows(IllegalArgumentException.class, () -> editor.setValue(stale));
    }

    @Test
    void expandedChildIsReplacementOnlyAndDirectFlexSlotsNeverOfferAddExpanded()
            throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetDefinition expandedDefinition = definition("flutter.widgets.Expanded");
        WidgetNode current = text(
                id("9b1b7481-02a1-45aa-a326-8e5669820aae"), "current");
        WidgetNode replacement = text(
                id("7ad5fe4d-ff0d-46ae-b52e-5a69d41081bd"), "replacement");
        WidgetNode expanded = new WidgetNode(
                id("ba78e2c7-2b67-4769-9b52-843585d73d74"),
                expandedDefinition.typeId(),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(current)),
                Extensions.empty());
        WidgetNode column = new WidgetNode(
                id("6d4d53ee-780d-4d38-a12a-1934494350d2"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(expanded, replacement))),
                Extensions.empty());
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(column),
                CATALOG,
                List.of(type("flutter.widgets.Text"),
                        type("flutter.widgets.Expanded")));

        FlutterWidgetSlotPropertyEditor columnEditor =
                new FlutterWidgetSlotPropertyEditor(
                        column,
                        columnDefinition,
                        columnDefinition.slot(CHILDREN).orElseThrow(),
                        context);
        PropertyEnv columnEnvironment = PropertyEnv.create(descriptor("Children"));
        columnEditor.attachEnv(columnEnvironment);
        onEdt(() -> {
            Component custom = columnEditor.getCustomEditor();
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            assertEquals(List.of("Text"), labels(addType),
                    "Expanded is a wrapper affordance and must never appear as a "
                    + "terminal Add new widget choice.");
            return null;
        });

        FlutterWidgetSlotPropertyEditor expandedEditor =
                new FlutterWidgetSlotPropertyEditor(
                        expanded,
                        expandedDefinition,
                        expandedDefinition.slot(CHILD).orElseThrow(),
                        context);
        PropertyEnv expandedEnvironment = PropertyEnv.create(descriptor("Child"));
        expandedEditor.attachEnv(expandedEnvironment);
        onEdt(() -> {
            Component custom = expandedEditor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JLabel status = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.STATUS_NAME,
                    JLabel.class);
            assertEquals(List.of(
                    "No change",
                    "Replace with new widget",
                    "Replace with existing widget"), labels(action));
            assertFalse(labels(action).contains("Add new widget"));
            assertFalse(labels(action).contains("Clear single child"));
            assertFalse(labels(action).contains("Remove selected widget"));
            assertTrue(status.getText().contains(
                    "Expanded.child is required and cannot be removed or cleared"));
            return null;
        });

        assertThrows(IllegalArgumentException.class, () -> expandedEditor.setValue(
                FlutterWidgetSlotCellValue.staged(
                        "illegal removal",
                        new FlutterWidgetSlotMutation.Remove(
                                expanded.id(), CHILD, current.id()))));
    }

    @Test
    void flexibleChildIsReplacementOnlyAndDirectFlexSlotsNeverOfferAddFlexible()
            throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetDefinition flexibleDefinition = definition("flutter.widgets.Flexible");
        WidgetNode current = text(
                id("827983f4-76a6-4ef2-a7da-366acee3e093"), "current");
        WidgetNode replacement = text(
                id("37da84ab-0ef0-4410-a6db-e9d7b171cd48"), "replacement");
        WidgetNode flexible = new WidgetNode(
                id("b2745b7e-3585-4d12-80f0-e85e3fa9ae3d"),
                flexibleDefinition.typeId(),
                Map.of(),
                Map.of(CHILD, WidgetSlot.SingleSlot.of(current)),
                Extensions.empty());
        WidgetNode column = new WidgetNode(
                id("479f56bf-86d7-4379-b080-f458291e3bb5"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN,
                        new WidgetSlot.ListSlot(List.of(flexible, replacement))),
                Extensions.empty());
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(column),
                CATALOG,
                List.of(type("flutter.widgets.Text"),
                        type("flutter.widgets.Expanded"),
                        type("flutter.widgets.Flexible"),
                        type("flutter.widgets.Spacer")));

        FlutterWidgetSlotPropertyEditor columnEditor =
                new FlutterWidgetSlotPropertyEditor(
                        column,
                        columnDefinition,
                        columnDefinition.slot(CHILDREN).orElseThrow(),
                        context);
        PropertyEnv columnEnvironment = PropertyEnv.create(descriptor("Children"));
        columnEditor.attachEnv(columnEnvironment);
        onEdt(() -> {
            Component custom = columnEditor.getCustomEditor();
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            assertEquals(List.of("Text", "Spacer"), labels(addType),
                    "Expanded and Flexible are wrapping affordances, while Spacer is "
                    + "a terminal direct-Flex child.");
            return null;
        });

        FlutterWidgetSlotPropertyEditor flexibleEditor =
                new FlutterWidgetSlotPropertyEditor(
                        flexible,
                        flexibleDefinition,
                        flexibleDefinition.slot(CHILD).orElseThrow(),
                        context);
        PropertyEnv flexibleEnvironment = PropertyEnv.create(descriptor("Child"));
        flexibleEditor.attachEnv(flexibleEnvironment);
        onEdt(() -> {
            Component custom = flexibleEditor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JLabel status = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.STATUS_NAME,
                    JLabel.class);
            assertEquals(List.of(
                    "No change",
                    "Replace with new widget",
                    "Replace with existing widget"), labels(action));
            assertFalse(labels(action).contains("Add new widget"));
            assertFalse(labels(action).contains("Clear single child"));
            assertFalse(labels(action).contains("Remove selected widget"));
            assertTrue(status.getText().contains(
                    "Flexible.child is required and cannot be removed or cleared"));
            return null;
        });

        assertThrows(IllegalArgumentException.class, () -> flexibleEditor.setValue(
                FlutterWidgetSlotCellValue.staged(
                        "illegal removal",
                        new FlutterWidgetSlotMutation.Remove(
                                flexible.id(), CHILD, current.id()))));
    }

    @Test
    void spacerIsAnAddChoiceOnlyForDirectRowOrColumnChildren() throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetDefinition rowDefinition = definition("flutter.widgets.Row");
        WidgetDefinition stackDefinition = definition("flutter.widgets.Stack");
        WidgetNode row = new WidgetNode(
                id("7bdabf33-2e62-4548-b884-8a39c24be5e2"),
                rowDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())),
                Extensions.empty());
        WidgetNode stack = new WidgetNode(
                id("01fb3e4d-3911-4dd3-988a-ec2a8e5c7e0b"),
                stackDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of())),
                Extensions.empty());
        WidgetNode column = new WidgetNode(
                id("6869194a-729d-4a84-aa0d-2b853c0c77d2"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(row, stack))),
                Extensions.empty());
        FlutterWidgetSlotEditorContext context = new FlutterWidgetSlotEditorContext(
                document(column), CATALOG, List.of(type("flutter.widgets.Spacer")));

        FlutterWidgetSlotPropertyEditor rowEditor = new FlutterWidgetSlotPropertyEditor(
                row,
                rowDefinition,
                rowDefinition.slot(CHILDREN).orElseThrow(),
                context);
        rowEditor.attachEnv(PropertyEnv.create(descriptor("Children")));
        FlutterWidgetSlotPropertyEditor stackEditor = new FlutterWidgetSlotPropertyEditor(
                stack,
                stackDefinition,
                stackDefinition.slot(CHILDREN).orElseThrow(),
                context);
        stackEditor.attachEnv(PropertyEnv.create(descriptor("Children")));

        onEdt(() -> {
            JComboBox<?> rowAddType = component(
                    rowEditor.getCustomEditor(),
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            JComboBox<?> stackAddType = component(
                    stackEditor.getCustomEditor(),
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            assertEquals(List.of("Spacer"), labels(rowAddType));
            assertEquals(List.of(), labels(stackAddType));
            return null;
        });
    }

    @Test
    void listClearAllStagesExactOrderedIdsAndConsumesPropertyLeaseOnce()
            throws Exception {
        WidgetDefinition columnDefinition = definition("flutter.widgets.Column");
        WidgetNode first = text(
                id("9c773413-b75c-4808-94a3-f23233bc6e8f"), "first");
        WidgetNode second = text(
                id("2ea510e8-9ba0-43ba-821b-c50e3c312229"), "second");
        WidgetNode column = new WidgetNode(
                id("42f4a4e3-714c-41bf-b16c-4c5a2ca7e788"),
                columnDefinition.typeId(),
                Map.of(),
                Map.of(CHILDREN, new WidgetSlot.ListSlot(List.of(first, second))),
                Extensions.empty());
        List<FlutterWidgetSlotMutation> submitted = new ArrayList<>();
        FlutterWidgetPropertiesNode node = new FlutterWidgetPropertiesNode(
                Children.LEAF,
                column,
                columnDefinition,
                ignored -> { },
                new FlutterWidgetSlotEditorContext(
                        document(column),
                        CATALOG,
                        List.of(type("flutter.widgets.Text"))),
                submitted::add);
        Node.Property<FlutterWidgetSlotCellValue> children =
                slotProperty(node, "children");
        PropertyEditor editor = children.getPropertyEditor();
        editor.setValue(children.getValue());
        PropertyEnv environment = PropertyEnv.create(descriptor("Children"));
        ((ExPropertyEditor) editor).attachEnv(environment);

        onEdt(() -> {
            Component custom = editor.getCustomEditor();
            JComboBox<?> action = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ACTION_NAME,
                    JComboBox.class);
            JList<?> current = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.CURRENT_LIST_NAME,
                    JList.class);
            JComboBox<?> addType = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.ADD_TYPE_NAME,
                    JComboBox.class);
            JComboBox<?> moveSource = component(
                    custom,
                    FlutterWidgetSlotPropertyEditor.MOVE_SOURCE_NAME,
                    JComboBox.class);
            assertTrue(labels(action).contains("Clear all widgets"));
            selectLabel(action, "Clear all widgets");
            assertFalse(current.isEnabled());
            assertFalse(addType.isEnabled());
            assertFalse(moveSource.isEnabled());
            environment.setState(PropertyEnv.STATE_VALID);

            FlutterWidgetSlotCellValue staged = assertInstanceOf(
                    FlutterWidgetSlotCellValue.class, editor.getValue());
            FlutterWidgetSlotMutation.ClearAll clear = assertInstanceOf(
                    FlutterWidgetSlotMutation.ClearAll.class,
                    staged.mutation().orElseThrow());
            assertEquals(column.id(), clear.ownerId());
            assertEquals(CHILDREN, clear.slotName());
            assertEquals(List.of(first.id(), second.id()), clear.expectedChildIds());
            assertTrue(staged.summary().contains("Clear all 2 widgets"));

            children.setValue(staged);
            children.setValue(staged);
            assertEquals(List.of(clear), submitted,
                    "Clear All must consume one Properties revision lease once");
            return null;
        });
    }

    private static WidgetDefinition definition(String value) {
        return CATALOG.find(type(value)).orElseThrow();
    }

    private static WidgetTypeId type(String value) {
        return new WidgetTypeId(value);
    }

    private static StableId id(String value) {
        return StableId.parse(value);
    }

    private static WidgetNode text(StableId id, String data) {
        return new WidgetNode(
                id,
                type("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(data)),
                Map.of(),
                Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegion checksum = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(
                id("0954b8d3-c41b-4796-9b22-977181523ec5"),
                new DartSourceDescriptor(
                        "slot_screen.dart",
                        "SlotScreen",
                        WidgetClassKind.STATELESS,
                        Optional.empty(),
                        new ManagedRegions(checksum, checksum)),
                root);
    }

    private static FeatureDescriptor descriptor(String displayName) {
        FeatureDescriptor descriptor = new FeatureDescriptor();
        descriptor.setName(displayName);
        descriptor.setDisplayName(displayName);
        return descriptor;
    }

    @SuppressWarnings("unchecked")
    private static Node.Property<FlutterWidgetSlotCellValue> slotProperty(
            FlutterWidgetPropertiesNode node,
            String name) {
        Node.Property<?> property = Arrays.stream(node.getPropertySets())
                .filter(set -> FlutterWidgetPropertiesNode.SLOTS_SET_NAME
                        .equals(set.getName()))
                .flatMap(set -> Arrays.stream(set.getProperties()))
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(FlutterWidgetSlotCellValue.class, property.getValueType());
        return (Node.Property<FlutterWidgetSlotCellValue>) property;
    }

    private static List<String> labels(JComboBox<?> combo) {
        ArrayList<String> values = new ArrayList<>();
        for (int index = 0; index < combo.getItemCount(); index++) {
            values.add(combo.getItemAt(index).toString());
        }
        return values;
    }

    private static void selectLabel(JComboBox<?> combo, String label) {
        for (int index = 0; index < combo.getItemCount(); index++) {
            if (label.equals(combo.getItemAt(index).toString())) {
                combo.setSelectedIndex(index);
                return;
            }
        }
        throw new AssertionError("Missing combo item " + label + " in " + labels(combo));
    }

    private static void selectContains(JComboBox<?> combo, String fragment) {
        for (int index = 0; index < combo.getItemCount(); index++) {
            if (combo.getItemAt(index).toString().contains(fragment)) {
                combo.setSelectedIndex(index);
                return;
            }
        }
        throw new AssertionError(
                "Missing combo item containing " + fragment + " in " + labels(combo));
    }

    private static <T extends Component> T component(
            Component root,
            String name,
            Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = component(child, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(ThrowingSupplier<T> operation) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return operation.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(operation.get());
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        return result.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
