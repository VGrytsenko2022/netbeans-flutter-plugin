package dev.flutter.netbeans.designer.validation;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRectValidationTest {
    private static final PropertyName CLIP_BEHAVIOR =
            new PropertyName("clipBehavior");

    @Test
    void acceptsOmissionEveryClosedClipModeAndAnOptionalChild() {
        assertValid(clipRect(Map.of(), WidgetSlot.SingleSlot.empty()));
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
    void rejectsCustomClipperExpressionsAndNonSingleChildren() {
        ValidationIssue clipper = onlyError(clipRect(
                Map.of(new PropertyName("clipper"),
                        new PropertyValue.DartExpressionValue(
                                "const UserDefinedRectClipper()")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.UNKNOWN_PROPERTY, clipper.code());
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
