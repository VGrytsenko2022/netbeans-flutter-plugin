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

class ClipPathValidationTest {
    private static final PropertyName CLIPPER = new PropertyName("clipper");
    private static final PropertyName CLIP_BEHAVIOR =
            new PropertyName("clipBehavior");

    @Test
    void acceptsOmissionEveryClosedClipModeAndAnOptionalChild() {
        assertValid(clipPath(Map.of(), WidgetSlot.SingleSlot.empty()));
        assertValid(clipPath(
                Map.of(CLIPPER, new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "PathClipper", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(true))),
                WidgetSlot.SingleSlot.empty()));
        for (String value : List.of(
                "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
            assertValid(clipPath(
                    Map.of(CLIP_BEHAVIOR,
                            new PropertyValue.EnumValue("Clip", value)),
                    WidgetSlot.SingleSlot.of(text("Child"))));
        }
    }

    @Test
    void rejectsWrongClipKindsAndValuesFailClosed() {
        ValidationIssue wrongKind = onlyError(clipPath(
                Map.of(CLIP_BEHAVIOR,
                        new PropertyValue.StringValue("Clip.antiAlias")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_KIND, wrongKind.code());
        assertEquals("/root/properties/clipBehavior", wrongKind.path());

        ValidationIssue wrongValue = onlyError(clipPath(
                Map.of(CLIP_BEHAVIOR,
                        new PropertyValue.EnumValue("Clip", "custom")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_CONSTRAINT, wrongValue.code());
        assertEquals("/root/properties/clipBehavior", wrongValue.path());
    }

    @Test
    void rejectsRawCustomClipperExpressionsAndNonSingleChildren() {
        ValidationIssue clipper = onlyError(clipPath(
                Map.of(CLIPPER,
                        new PropertyValue.DartExpressionValue(
                                "const UserDefinedPathClipper()")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_KIND, clipper.code());
        assertEquals("/root/properties/clipper", clipper.path());

        ValidationResult child = validate(clipPath(
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

    @Test
    void acceptsEveryReferenceFormForEitherBranchWithoutConflatingTheirStaticTypes() {
        for (String property : List.of("clipper", "shape")) {
            for (Optional<String> uri : List.of(Optional.<String>empty(),
                    Optional.of("package:sample/objects.dart"))) {
                for (Optional<String> member : List.of(Optional.<String>empty(), Optional.of("selected"))) {
                    for (Optional<Boolean> constant : List.of(Optional.<Boolean>empty(),
                            Optional.of(false), Optional.of(true))) {
                        var reference = new PropertyValue.DartObjectReferenceValue(uri, "Objects", member,
                                constant.isEmpty() ? PropertyValue.DartObjectReferenceValue.Access.REFERENCE
                                        : PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                                constant);
                        assertValid(clipPath(Map.of(new PropertyName(property), reference),
                                WidgetSlot.SingleSlot.of(text("Child"))));
                    }
                }
            }
        }
    }

    @Test
    void rejectsSimultaneousBranchesWithPreciseRemediation() {
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "value", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        ValidationIssue conflict = onlyError(clipPath(Map.of(CLIPPER, reference,
                new PropertyName("shape"), reference), WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_CONFLICT, conflict.code());
        assertEquals("/root/properties/shape", conflict.path());
        assertTrue(conflict.message().contains("unset 'clipper'"), conflict.message());
        assertTrue(conflict.message().contains("CustomClipper<Path>"), conflict.message());
        assertTrue(conflict.message().contains("ShapeBorder"), conflict.message());
    }

    @Test
    void rejectsOpaqueShapesWrongKindsAndUnknownConstructorFields() {
        for (PropertyValue wrong : List.of(new PropertyValue.StringValue("CircleBorder()"),
                new PropertyValue.DartExpressionValue("const CircleBorder()"),
                new PropertyValue.BooleanValue(true))) {
            ValidationIssue issue = onlyError(clipPath(Map.of(new PropertyName("shape"), wrong),
                    WidgetSlot.SingleSlot.empty()));
            assertEquals(WidgetTreeValidator.PROPERTY_KIND, issue.code());
            assertEquals("/root/properties/shape", issue.path());
        }
        var result = validate(clipPath(Map.of(new PropertyName("borderRadius"),
                new PropertyValue.StringValue("8")), WidgetSlot.SingleSlot.empty()));
        assertFalse(result.valid());
        assertEquals("/root/properties/borderRadius", result.errors().getFirst().path());
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

    private static WidgetNode clipPath(
            Map<PropertyName, PropertyValue> properties,
            WidgetSlot child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ClipPath"),
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
