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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipRRectValidationTest {
    private static final PropertyName BORDER_RADIUS = new PropertyName("borderRadius");
    private static final PropertyName CLIPPER = new PropertyName("clipper");
    private static final PropertyName CLIP_BEHAVIOR = new PropertyName("clipBehavior");

    @Test
    void acceptsOmissionBothTypedRadiusBranchesEveryClipModeAndOptionalChild() {
        assertValid(clipRRect(Map.of(), WidgetSlot.SingleSlot.empty()));
        for (PropertyValue.BorderRadiusValue radius : List.of(
                physicalRadius("8", "12"), directionalRadius("4", "6"))) {
            for (String clip : List.of(
                    "none", "hardEdge", "antiAlias", "antiAliasWithSaveLayer")) {
                assertValid(clipRRect(
                        Map.of(
                                BORDER_RADIUS, radius,
                                CLIP_BEHAVIOR,
                                new PropertyValue.EnumValue("Clip", clip)),
                        WidgetSlot.SingleSlot.of(text("Child"))));
            }
        }
    }

    @Test
    void acceptsEveryClosedProjectClipperReferenceForm() {
        for (PropertyValue.DartObjectReferenceValue clipper : List.of(
                new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "_localClipper", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of("package:sample/clippers/rrect_clipper.dart"),
                        "RRectClippers", Optional.of("rounded"),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        Optional.empty()),
                new PropertyValue.DartObjectReferenceValue(
                        Optional.empty(), "LocalClipper", Optional.of("create"),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(false)),
                new PropertyValue.DartObjectReferenceValue(
                        Optional.of("package:sample/clippers/rrect_clipper.dart"),
                        "RoundedClipper", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access
                                .ZERO_ARGUMENT_INVOCATION,
                        Optional.of(true)))) {
            assertValid(clipRRect(
                    Map.of(CLIPPER, clipper), WidgetSlot.SingleSlot.empty()));
        }
    }

    @Test
    void rejectsWrongRadiusKindAndUnrepresentableGeometryFailClosed() {
        ValidationIssue wrongKind = onlyError(clipRRect(
                Map.of(BORDER_RADIUS,
                        new PropertyValue.StringValue("BorderRadius.circular(8)")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_KIND, wrongKind.code());
        assertEquals("/root/properties/borderRadius", wrongKind.path());

        ValidationIssue unrepresentable = onlyError(clipRRect(
                Map.of(BORDER_RADIUS, physicalRadius("1e10000", "2")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_CONSTRAINT, unrepresentable.code());
        assertEquals("/root/properties/borderRadius", unrepresentable.path());
    }

    @Test
    void rejectsWrongClipKindsAndValuesFailClosed() {
        ValidationIssue wrongKind = onlyError(clipRRect(
                Map.of(CLIP_BEHAVIOR,
                        new PropertyValue.StringValue("Clip.antiAlias")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_KIND, wrongKind.code());
        assertEquals("/root/properties/clipBehavior", wrongKind.path());

        ValidationIssue wrongValue = onlyError(clipRRect(
                Map.of(CLIP_BEHAVIOR,
                        new PropertyValue.EnumValue("Clip", "custom")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_CONSTRAINT, wrongValue.code());
        assertEquals("/root/properties/clipBehavior", wrongValue.path());
    }

    @Test
    void rejectsOpaqueClipperExpressionsAndNonSingleChildren() {
        ValidationIssue clipper = onlyError(clipRRect(
                Map.of(CLIPPER,
                        new PropertyValue.DartExpressionValue(
                                "const UserDefinedRRectClipper()")),
                WidgetSlot.SingleSlot.empty()));
        assertEquals(WidgetTreeValidator.PROPERTY_KIND, clipper.code());
        assertEquals("/root/properties/clipper", clipper.path());

        ValidationResult child = validate(clipRRect(
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

    private static WidgetNode clipRRect(
            Map<PropertyName, PropertyValue> properties,
            WidgetSlot child) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.ClipRRect"),
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

    private static PropertyValue.BorderRadiusValue physicalRadius(
            String x, String y) {
        PropertyValue.BoxDecorationValue.Radius radius = radius(x, y);
        return new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.PhysicalBorderRadius(
                        radius, radius, radius, radius));
    }

    private static PropertyValue.BorderRadiusValue directionalRadius(
            String x, String y) {
        PropertyValue.BoxDecorationValue.Radius radius = radius(x, y);
        return new PropertyValue.BorderRadiusValue(
                new PropertyValue.BoxDecorationValue.DirectionalBorderRadius(
                        radius, radius, radius, radius));
    }

    private static PropertyValue.BoxDecorationValue.Radius radius(
            String x, String y) {
        return new PropertyValue.BoxDecorationValue.Radius(
                new BigDecimal(x), new BigDecimal(y));
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
