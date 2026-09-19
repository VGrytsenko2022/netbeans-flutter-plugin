package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.DartParameter;
import io.github.vgrytsenko2022.designer.catalog.PropertyDefinition;
import io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.PropertyValueKind;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.FeatureDescriptor;
import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.util.Optional;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openide.explorer.propertysheet.PropertyEnv;

/** Exact local-value/reference union; nested editors never publish into the owning cell. */
final class FlutterLocalDartReferenceEditorComponent {
    static final String MODE_NAME = "flutter.localReference.mode";
    static final String OMIT = "Use Flutter default (omit argument)";
    static final String COLOR = "Literal or theme color";
    static final String INSETS = "Physical or directional insets";
    static final String ALIGNMENT = "Physical or directional alignment";
    static final String OFFSET = "Signed Offset coordinates";
    static final String NUMBER = "Local number";
    static final String MATRIX = "Structured Matrix4";
    static final String SIZE = "Width and height";
    static final String GRADIENT = "Structured gradient shader";
    static final String PHYSICAL_ALIGNMENT = "Physical Alignment";
    static final String CONSTRAINTS = "Structured BoxConstraints";
    static final String DECORATION = "Structured BoxDecoration";
    static final String RADIUS = "Physical elliptical corners";
    static final String IMAGE = "Declared asset image";
    static final String ICON = "Material Icon or empty Icon";
    static final String NULL = "Explicit null";
    static final String PROJECT = "Project reference";
    private FlutterLocalDartReferenceEditorComponent() { }

    private static PropertyValue.BorderRadiusValue zeroRadius() {
        var zero=new PropertyValue.BoxDecorationValue.Radius(BigDecimal.ZERO,BigDecimal.ZERO);
        return new PropertyValue.BorderRadiusValue(new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(zero,zero,zero,zero));
    }

    static Component customEditor(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
        return new LocalReferencePanel(editor, binding, environment);
    }

    /** Valid inactive draft only; opening an omitted/null/reference value must not commit it. */
    static PropertyValue initialNumber(PropertyDefinition definition) {
        var zero = new PropertyValue.DoubleValue(BigDecimal.ZERO);
        if (definition.constraints().stream().anyMatch(value -> value.accepts(zero))) return zero;
        for (var constraint : definition.constraints()) {
            if (constraint instanceof PropertyValueConstraint.IntegerRange range) {
                var candidate = java.math.BigInteger.ZERO;
                if (range.minimum() != null) candidate = candidate.max(range.minimum());
                if (range.maximum() != null) candidate = candidate.min(range.maximum());
                var value = new PropertyValue.IntegerValue(candidate);
                if (range.accepts(value)) return value;
            } else if (constraint instanceof PropertyValueConstraint.DoubleRange range) {
                var candidate = range.minimum() != null && range.maximum() != null
                        ? range.minimum().add(range.maximum()).divide(BigDecimal.valueOf(2))
                        : range.minimum() != null ? range.minimum().add(BigDecimal.ONE)
                        : range.maximum() != null ? range.maximum().subtract(BigDecimal.ONE) : BigDecimal.ZERO;
                var value = new PropertyValue.DoubleValue(candidate);
                if (range.accepts(value)) return value;
            } else if (constraint.kind() == PropertyValueKind.INTEGER) {
                var value = new PropertyValue.IntegerValue(java.math.BigInteger.ZERO);
                if (constraint.accepts(value)) return value;
            }
        }
        throw new IllegalArgumentException("No valid local numeric draft for " + definition.name().value());
    }

    private static final class LocalReferencePanel extends FlutterPropertyEditorComponents.CommitOnValidPanel {
        private final JComboBox<String> mode;
        private final String localMode;
        private final CardLayout layout = new CardLayout();
        private final JPanel cards = new JPanel(layout);
        private final JTextArea note = new JTextArea(3, 52);
        private final FlutterPropertyEditorComponents.CommitOnValidPanel localPanel;
        private final FlutterPropertyEditorComponents.CommitOnValidPanel referencePanel;
        private boolean refreshing;

        LocalReferencePanel(PropertyEditor editor, FlutterTypedPropertyEditors.Binding binding, PropertyEnv environment) {
            super(editor, binding, environment);
            boolean time = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.TIME_REFERENCE;
            boolean dateRange = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.DATE_RANGE_REFERENCE;
            boolean date = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.DATE_REFERENCE;
            boolean tableWidth = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.TABLE_WIDTH_REFERENCE;
            boolean key = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.KEY_REFERENCE;
            boolean string = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.STRING_REFERENCE;
            boolean image = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.IMAGE_PROVIDER_REFERENCE;
            boolean radius = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.BORDER_RADIUS_REFERENCE;
            boolean color = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.COLOR_REFERENCE;
            boolean alignment = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.ALIGNMENT_REFERENCE;
            boolean offset = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.OFFSET_REFERENCE;
            boolean size = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.SIZE_REFERENCE;
            boolean gradient = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.GRADIENT_REFERENCE;
            boolean matrix = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.MATRIX4_REFERENCE;
            boolean number = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.NUMBER_REFERENCE;
            boolean physicalAlignment = binding.definition().constraints().stream().anyMatch(io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.AlignmentValues.class::isInstance);
            boolean constraints = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.BOX_CONSTRAINTS_REFERENCE;
            boolean decoration = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.BOX_DECORATION_REFERENCE;
            boolean icon = binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.ICON_WIDGET_REFERENCE;
            localMode = time ? "Time of day (HH:mm)" : dateRange ? "Gregorian date range" : date ? "Gregorian date (YYYY-MM-DD)" : tableWidth ? "Structured column widths" : key ? "String ValueKey" : string ? "Literal string" : size ? SIZE : gradient ? GRADIENT : image ? IMAGE : radius ? RADIUS : matrix ? MATRIX : color ? COLOR : alignment ? physicalAlignment ? PHYSICAL_ALIGNMENT : ALIGNMENT : number ? NUMBER : offset ? OFFSET : constraints ? CONSTRAINTS : decoration ? DECORATION : icon ? ICON : INSETS;
            var modes = new java.util.ArrayList<String>(); if (binding.optional()) modes.add(OMIT);
            if (binding.definition().acceptedKinds().contains(PropertyValueKind.NULL)) modes.add(NULL);
            modes.add(localMode); modes.add(PROJECT); mode = new JComboBox<>(modes.toArray(String[]::new));
            setLayout(new BorderLayout(0, 8)); setPreferredSize(new Dimension(720, 590));
            setName("flutter.localReference.editor");
            getAccessibleContext().setAccessibleName(binding.definition().name().value() + " local value or project reference editor");
            getAccessibleContext().setAccessibleDescription("Choose an omitted argument, a typed local value, or a strictly verified project reference. Drafts stay local until OK; Cancel preserves the original value.");
            mode.setName(MODE_NAME); mode.getAccessibleContext().setAccessibleName("Value source");
            mode.getAccessibleContext().setAccessibleDescription("Local value and project reference are exclusive; changing source does not commit either draft.");
            var heading = new JPanel(new BorderLayout(8, 0)); var label = new JLabel("Source:"); label.setLabelFor(mode);
            heading.add(label, BorderLayout.WEST); heading.add(mode, BorderLayout.CENTER); add(heading, BorderLayout.NORTH);
            var initial = initialValue().explicitValue().orElse(null);
            var localDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    binding.definition().constraints().stream().filter(value -> value.kind() != PropertyValueKind.DART_OBJECT_REFERENCE && value.kind() != PropertyValueKind.NULL).toList(), Optional.empty());
            var localBinding = FlutterTypedPropertyEditors.binding(localDefinition).orElseThrow();
            var localEditor = localBinding.createEditor();
            PropertyValue initialLocal = initial != null && !(initial instanceof PropertyValue.DartObjectReferenceValue) && !(initial instanceof PropertyValue.NullValue) ? initial
                    : time ? binding.definition().creationDefault().orElse(new PropertyValue.StringValue("09:00"))
                    : dateRange ? new PropertyValue.StringValue("2000-01-01/2000-01-01")
                    : date ? binding.definition().creationDefault().filter(PropertyValue.StringValue.class::isInstance).orElse(new PropertyValue.StringValue("2000-01-01"))
                    : tableWidth ? new PropertyValue.StringValue(binding.definition().name().value().equals("columnWidths") ? "" : "flex(1)")
                    : key || string ? new PropertyValue.StringValue("")
                    : size ? new PropertyValue.SizeValue(BigDecimal.ZERO, BigDecimal.ZERO)
                    : gradient ? io.github.vgrytsenko2022.designer.catalog.ShaderMaskWidgetPropertySchema.neutral()
                    : image ? PropertyValue.ImageProviderValue.unresolved()
                    : radius ? zeroRadius()
                    : matrix ? new PropertyValue.Matrix4Value(java.util.stream.IntStream.range(0, 16).mapToObj(i -> i % 5 == 0 ? BigDecimal.ONE : BigDecimal.ZERO).toList())
                    : color ? new PropertyValue.ColorValue(0xff000000L)
                    : alignment ? new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.PHYSICAL, BigDecimal.ZERO, BigDecimal.ZERO)
                    : number ? binding.definition().creationDefault().filter(value ->
                        value instanceof PropertyValue.IntegerValue || value instanceof PropertyValue.DoubleValue)
                        .orElseGet(() -> initialNumber(localDefinition))
                    : offset ? new PropertyValue.OffsetValue(BigDecimal.ZERO, BigDecimal.ZERO)
                    : constraints ? new PropertyValue.BoxConstraintsValue(BigDecimal.ZERO, Optional.empty(), BigDecimal.ZERO, Optional.empty())
                    : decoration ? new PropertyValue.BoxDecorationValue(Optional.empty(), Optional.empty(), Optional.empty(), java.util.List.of(), Optional.empty(), Optional.empty(), PropertyValue.BoxDecorationValue.BoxShape.RECTANGLE)
                    : icon ? PropertyValue.IconDataValue.none()
                    : new PropertyValue.EdgeInsetsValue(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            localEditor.setValue(FlutterPropertyCellValue.explicit(initialLocal));
            var localDescriptor = new FeatureDescriptor();
            if (offset && binding.definition().constraints().stream().anyMatch(value ->
                    value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                            && reference.expectedDartType().equals("Animation<Offset>"))) {
                localDescriptor.setValue(FlutterOffsetPropertyEditorComponents.FRACTIONAL_COORDINATES_ATTRIBUTE, true);
            }
            Object imageChoices = environment.getFeatureDescriptor() == null ? null : environment.getFeatureDescriptor().getValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE);
            if (imageChoices != null) localDescriptor.setValue(FlutterImageAssetChoices.FEATURE_ATTRIBUTE, imageChoices);
            var localEnvironment = PropertyEnv.create(localDescriptor);
            localPanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    (time ? new FlutterTimeOfDayEditorComponent(localEditor,localBinding,localEnvironment)
                    : dateRange ? new FlutterDateRangeEditorComponent(localEditor,localBinding,localEnvironment)
                    : tableWidth ? new FlutterTableWidthEditorComponent(localEditor,localBinding,localEnvironment)
                    : number ? FlutterNullableNumberEditorComponent.customEditor(localEditor, localBinding, localEnvironment)
                    : FlutterPropertyEditorComponents.customEditor(localEditor, localBinding, localEnvironment));
            cards.add(localPanel, localMode);
            var referenceDefinition = new PropertyDefinition(binding.definition().name(), DartParameter.named(0, true),
                    binding.definition().constraints().stream().filter(value -> value.kind() == PropertyValueKind.DART_OBJECT_REFERENCE).toList(), Optional.empty());
            var referenceBinding = FlutterTypedPropertyEditors.binding(referenceDefinition).orElseThrow();
            var referenceEditor = referenceBinding.createEditor();
            referenceEditor.setValue(FlutterPropertyCellValue.explicit(initial instanceof PropertyValue.DartObjectReferenceValue ? initial
                    : new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_reference", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
            var referenceEnvironment = PropertyEnv.create(new FeatureDescriptor());
            referencePanel = (FlutterPropertyEditorComponents.CommitOnValidPanel)
                    FlutterDartObjectReferenceEditorComponent.customEditor(referenceEditor, referenceBinding, referenceEnvironment);
            if (!(initial instanceof PropertyValue.DartObjectReferenceValue)) clearRoot(referencePanel);
            cards.add(referencePanel, PROJECT); cards.add(new JPanel(), OMIT); cards.add(new JPanel(), NULL); add(cards, BorderLayout.CENTER);
            note.setEditable(false); note.setOpaque(false); note.setLineWrap(true); note.setWrapStyleWord(true);
            note.getAccessibleContext().setAccessibleName("Local value and reference behavior"); add(note, BorderLayout.SOUTH);
            mode.setSelectedItem(initial == null ? binding.optional() ? OMIT : localMode
                    : initial instanceof PropertyValue.NullValue ? NULL : initial instanceof PropertyValue.DartObjectReferenceValue ? PROJECT : localMode);
            mode.addActionListener(ignored -> refresh(true));
            localEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            referenceEnvironment.addPropertyChangeListener(ignored -> refresh(true));
            activate(); refresh(true);
        }

        @Override boolean prepareCommit() { return refresh(false); }

        private boolean refresh(boolean requestValidation) {
            if (refreshing) return true;
            refreshing = true;
            try {
                String selected = (String) mode.getSelectedItem(); layout.show(cards, selected);
                String text = PROJECT.equals(selected)
                        ? "Stores a strict typed project reference or factory. The analyzer verifies the exact required type; isolated Canvas never executes project code."
                        : OMIT.equals(selected) ? "Omits this argument and preserves Flutter theme/default resolution. No local value is synthesized."
                        : NULL.equals(selected) ? "Stores explicit null and preserves this property's SDK/theme fallback. It is distinct from omission; no local value or project reference is synthesized."
                        : "Stores the exact local typed value. It is exclusive with a project reference; drafts in the inactive source do not change this choice.";
                if (binding.definition().constraints().stream().anyMatch(value ->
                        value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<AlignmentGeometry>"))) text +=
                        " Local physical/directional alignment creates AlwaysStoppedAnimation<AlignmentGeometry>; edits are immediate. "
                        + "Project animation owns timing and lifetime; Canvas previews it at center. Directional alignment follows inherited LTR/RTL.";
                if (binding.definition().constraints().stream().anyMatch(value ->
                        value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("TransformCallback"))) text +=
                        " Local Matrix4 creates a callback that returns a fresh fixed matrix and ignores animationValue. "
                        + "Project mode requires Matrix4 Function(double animationValue), not a Matrix4 object or nullable callback. "
                        + "Project code owns value-dependent 2D/3D transforms; Canvas substitutes identity without executing it.";
                if (binding.definition().name().value().equals("animation") && binding.definition().constraints().stream().anyMatch(value ->
                        value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<double>"))) text +=
                        " Local signed values create AlwaysStoppedAnimation<double>; the On transform callback defines their units and range. "
                        + "Project animation owns timing and lifetime; Canvas substitutes 0. A fixed local matrix does not change with animationValue.";
                if (binding.editorKind() == FlutterTypedPropertyEditors.EditorKind.ICON_WIDGET_REFERENCE) text +=
                        " For SubmenuButton, configured states resolve Disabled, Hovered, Focused, then Default; an omitted bucket skips to the next active configured bucket or Default. Explicit null is terminal and returns to MenuTheme/native arrow fallback. "
                        + "Material icon None creates Icon(null), an empty glyph, and is not null fallback. A strict Widget reference supports custom size, color or arbitrary widget content in Source. "
                        + "Only Default, Disabled, Hovered and Focused states are resolved; project widget factories are never executed in Canvas.";
                if (binding.definition().constraints().stream().anyMatch(value ->
                        value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<double>")) && binding.definition().name().value().equals("opacity")) text +=
                        " Local opacity creates AlwaysStoppedAnimation<double> and does not animate edits. "
                        + "Project mode requires a non-null Animation<double>; its controller and lifetime remain source-owned. Canvas previews it at opacity 1.";
                if (binding.definition().constraints().stream().anyMatch(value ->
                        value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<Offset>"))) text +=
                        " Local X/Y fractions create AlwaysStoppedAnimation<Offset> and do not animate edits. "
                        + "Project mode requires a non-null Animation<Offset>; its controller and lifetime remain source-owned. Canvas previews it at zero.";
                if (binding.definition().name().value().equals("turns")
                        && binding.definition().constraints().stream().anyMatch(value ->
                            value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<double>"))) text +=
                        " Local signed turns creates AlwaysStoppedAnimation<double>; edits are immediate and 1 is 360 degrees clockwise, negative values rotate counterclockwise, and zero is identity. "
                        + "Project mode requires a non-null Animation<double> and owns its lifetime. Canvas previews it at turns 0. AlwaysStoppedAnimation reports forward, so the configured filter still applies.";
                if (binding.definition().name().value().equals("sizeFactor")
                        && binding.definition().constraints().stream().anyMatch(value ->
                            value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<double>"))) text +=
                        " Local signed size factor creates AlwaysStoppedAnimation<double>; edits are immediate and Flutter renders negative values as zero without changing the stored value. "
                        + "Project mode requires a non-null Animation<double> and owns its lifetime. Canvas previews it at size factor 1; parent constraints and clipping still apply.";
                if (binding.definition().name().value().equals("scale")
                        && binding.definition().constraints().stream().anyMatch(value ->
                            value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<double>"))) text +=
                        " Local signed scale creates AlwaysStoppedAnimation<double>; edits are immediate and zero/negative scales are allowed. "
                        + "Project mode requires a non-null Animation<double> and owns its lifetime. Canvas previews it at scale 1. AlwaysStoppedAnimation reports forward, so the configured filter still applies.";
                if (binding.definition().constraints().stream().anyMatch(value ->
                        value instanceof io.github.vgrytsenko2022.designer.catalog.PropertyValueConstraint.DartObjectReferenceValues reference
                                && reference.expectedDartType().equals("Animation<Decoration>"))) text +=
                        " Local BoxDecoration creates AlwaysStoppedAnimation<Decoration>; edits are immediate. "
                        + "Project animations may return BoxDecoration, ShapeDecoration or custom Decoration. Controller and tween lifetime remain in Dart. "
                        + "Canvas previews a project animation as an empty BoxDecoration without executing it. Position is independent; no padding or clipping is added.";
                note.setText(text); note.getAccessibleContext().setAccessibleDescription(text);
                var candidate = PROJECT.equals(selected) ? requestValidation ? referencePanel.stagedDraftValue() : referencePanel.validatedDraftValue()
                        : localMode.equals(selected) ? requestValidation ? localPanel.stagedDraftValue() : localPanel.validatedDraftValue()
                        : NULL.equals(selected) ? FlutterPropertyCellValue.explicit(new PropertyValue.NullValue())
                        : FlutterPropertyCellValue.unset();
                clearInvalid(note, text);
                if (requestValidation) markValid(candidate); else stageValid(candidate);
                return true;
            } catch (IllegalArgumentException failure) { markInvalid(failure.getMessage(), note); return false; }
            finally { refreshing = false; }
        }

        private static void clearRoot(Container parent) {
            for (var component : parent.getComponents()) {
                if (component instanceof JTextField field && FlutterDartObjectReferenceEditorComponent.ROOT_SYMBOL_NAME.equals(field.getName())) field.setText("");
                else if (component instanceof Container child) clearRoot(child);
            }
        }
    }
}
