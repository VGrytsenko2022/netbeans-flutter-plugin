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
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexedStackValidationTest {
    private static final PropertyName INDEX = new PropertyName("index");

    @Test
    void acceptsOmittedZeroAndExplicitNullForAnEmptyChildrenList() {
        assertValid(indexedStack(Map.of(), List.of()));
        assertValid(indexedStack(Map.of(
                INDEX, new PropertyValue.IntegerValue(BigInteger.ZERO)), List.of()));
        assertValid(indexedStack(Map.of(
                INDEX, new PropertyValue.NullValue()), List.of()));
    }

    @Test
    void enforcesTheEffectiveIndexAgainstTheOrderedChildren() {
        List<WidgetNode> children = List.of(text("A"), text("B"));
        assertValid(indexedStack(Map.of(
                INDEX, new PropertyValue.IntegerValue(BigInteger.ONE)), children));
        assertValid(indexedStack(Map.of(
                INDEX, new PropertyValue.NullValue()), children));

        ValidationIssue emptyOutOfRange = onlyError(indexedStack(Map.of(
                INDEX, new PropertyValue.IntegerValue(BigInteger.ONE)), List.of()));
        assertEquals(WidgetTreeValidator.PROPERTY_CONSTRAINT, emptyOutOfRange.code());
        assertEquals("/root/properties/index", emptyOutOfRange.path());
        assertTrue(emptyOutOfRange.message().contains("0 children"));

        ValidationIssue nonEmptyOutOfRange = onlyError(indexedStack(Map.of(
                INDEX, new PropertyValue.IntegerValue(BigInteger.TWO)), children));
        assertEquals(WidgetTreeValidator.PROPERTY_CONSTRAINT, nonEmptyOutOfRange.code());
        assertTrue(nonEmptyOutOfRange.message().contains("2 children"));
        assertTrue(nonEmptyOutOfRange.message().contains("0 through 1"));
    }

    @Test
    void rejectsNegativeAndWrongKindValuesFailClosed() {
        ValidationResult negative = validate(indexedStack(Map.of(
                INDEX, new PropertyValue.IntegerValue(BigInteger.ONE.negate())), List.of()));
        assertFalse(negative.valid());
        assertTrue(negative.errors().stream().anyMatch(issue ->
                issue.code().equals(WidgetTreeValidator.PROPERTY_CONSTRAINT)
                && issue.path().equals("/root/properties/index")));

        ValidationResult wrongKind = validate(indexedStack(Map.of(
                INDEX, new PropertyValue.DoubleValue(BigDecimal.ZERO)), List.of()));
        assertFalse(wrongKind.valid());
        assertTrue(wrongKind.errors().stream().anyMatch(issue ->
                issue.code().equals(WidgetTreeValidator.PROPERTY_KIND)
                && issue.path().equals("/root/properties/index")));
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

    private static WidgetNode indexedStack(
            Map<PropertyName, PropertyValue> properties,
            List<WidgetNode> children) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.IndexedStack"),
                properties,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)),
                Extensions.empty());
    }

    private static WidgetNode text(String value) {
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(value)),
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
