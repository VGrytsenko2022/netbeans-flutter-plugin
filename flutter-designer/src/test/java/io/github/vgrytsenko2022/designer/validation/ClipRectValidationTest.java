package io.github.vgrytsenko2022.designer.validation;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.Extensions;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRectValidationTest {
    private static final PropertyName CLIPPER = new PropertyName("clipper");
    private static final PropertyName CLIP_BEHAVIOR =
            new PropertyName("clipBehavior");

    @Test
    void acceptsOmissionEveryClosedClipModeAndAnOptionalChild() {
        assertValid(clipRect(Map.of(), WidgetSlot.SingleSlot.empty()));
        assertValid(clipRect(
                Map.of(CLIPPER, new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "RectClipper", Optional.of("instance"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty())),
                WidgetSlot.SingleSlot.empty()));
        for (String value : List.of(
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
            assertValid(clipRect(
                    Map.of(CLIP_BEHAVIOR,
                            new PropertyValue.EnumValue("Clip", value)),
                    WidgetSlot.SingleSlot.of(text("Child"))));
        }
    }

    @Test
    void rejectsWrongClipKindsAndValuesFailClosed() {
        ValidationIssue wrongKind = onlyError(clipRect(
                Map.of(CLIP_BEHAVIOR,
                        new PropertyValue.StringValue("Clip.hardEdge")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_KIND, wrongKind.code());
        assertEquals("/root/properties/clipBehavior", wrongKind.path());

        ValidationIssue wrongValue = onlyError(clipRect(
                Map.of(CLIP_BEHAVIOR,
                        new PropertyValue.EnumValue("Clip", "custom")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_CONSTRAINT, wrongValue.code());
        assertEquals("/root/properties/clipBehavior", wrongValue.path());
    }

    @Test
    void rejectsRawCustomClipperExpressionsAndNonSingleChildren() {
        ValidationIssue clipper = onlyError(clipRect(
                Map.of(CLIPPER,
                        new PropertyValue.DartExpressionValue(
                                "const UserDefinedRectClipper()")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_KIND, clipper.code());
        assertEquals("/root/properties/clipper", clipper.path());

        ValidationResult child = validate(clipRect(
                Map.of(), new WidgetSlot.ListSlot(List.of(text("A"), text("B")))));
        assertFalse(child.valid());
        assertEquals(List.of(
                        WidgetTreeValidator.SLOT_KIND,
                        WidgetTreeValidator.SLOT_CARDINALITY),
                child.errors().stream().map(ValidationIssue::code).toList());
        assertTrue(child.errors().stream()
                .allMatch(issue -> issue.path().equals("/root/slots/child")));
    }

    private static void assertValid(WidgetNode root) {
        ValidationResult result = validate(root);
        assertTrue(result.valid(), () -> result.issues().toString());
    }

    private static ValidationIssue onlyError(WidgetNode root) {
        ValidationResult result = validate(root);
        assertFalse(result.valid());
        assertEquals(1, result.errors().size(), result.issues().toString());
        return result.errors().getFirst();
    }

    private static ValidationResult validate(WidgetNode root) {
        return new WidgetTreeValidator().validate(
                document(root), BuiltInWidgetCatalog.getDefault());
    }

    private static WidgetNode clipRect(
            Map<PropertyName, PropertyValue> properties,
            WidgetSlot child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ClipRect"),
                properties,
                Map.of(new SlotName("child"), child),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue(value)),
                Map.of(),
                Extensions.empty());
    }

    private static DesignerDocument document(WidgetNode root) {
        ManagedRegion region = new ManagedRegion("0".repeat(64));
        return new DesignerDocument(
                Optional.empty(),
                StableId.random(),
                new DartSourceDescriptor(
                        "sample.dart", "Sample", WidgetClassKind.STATELESS,
                        Optional.empty(), new ManagedRegions(region, region)),
                Optional.empty(), root, Extensions.empty());
    }
}
