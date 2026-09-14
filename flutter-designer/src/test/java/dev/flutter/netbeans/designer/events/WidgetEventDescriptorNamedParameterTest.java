package dev.flutter.netbeans.designer.events;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor.Parameter;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor.Signature;

class WidgetEventDescriptorNamedParameterTest {
    @Test void retainsAllExistingRequiredPositionalRendering() {
        var positional = new Parameter("String?", "value");
        assertFalse(positional.named());
        assertTrue(positional.required());
        assertEquals("String? value", positional.declaration());
        var signature = new Signature("void", List.of(positional, new Parameter("int", "count")), List.of());
        assertEquals("void _handle(String? value, int count)", signature.declaration("_handle"));
        assertEquals("void Function(String?, int)", signature.dartFunctionType());
        assertEquals("void Function()", new Signature("void", List.of(), List.of()).dartFunctionType());
    }

    @Test void requiredNamedAndNullableTypeAreIndependentAndRetainNamesInFunctionTypes() {
        var nullableRequired = Parameter.requiredNamed("int?", "maxLength");
        assertTrue(nullableRequired.named());
        assertTrue(nullableRequired.required());
        var signature = new Signature("Widget?", List.of(new Parameter("BuildContext", "context"),
                Parameter.requiredNamed("int", "currentLength"), nullableRequired,
                Parameter.requiredNamed("bool", "isFocused")), List.of("package:flutter/material.dart"));
        assertEquals("Widget? _counter(BuildContext context, {required int currentLength, required int? maxLength, required bool isFocused})",
                signature.declaration("_counter"));
        assertEquals("Widget? Function(BuildContext, {required int currentLength, required int? maxLength, required bool isFocused})",
                signature.dartFunctionType());
    }

    @Test void optionalNamedParametersHaveSafeImplicitNullDefaultsAndNoPositionalComma() {
        var optional = Parameter.optionalNamed("String?", "label");
        assertTrue(optional.named());
        assertFalse(optional.required());
        var signature = new Signature("void", List.of(optional, Parameter.requiredNamed("bool", "enabled")), List.of());
        assertEquals("void _named({String? label, required bool enabled})", signature.declaration("_named"));
        assertEquals("void Function({String? label, required bool enabled})", signature.dartFunctionType());
        assertThrows(IllegalArgumentException.class, () -> Parameter.optionalNamed("String", "label"));
        assertThrows(IllegalArgumentException.class, () -> Parameter.optionalNamed("dynamic", "label"));
        assertThrows(IllegalArgumentException.class, () -> new Parameter("int?", "value", false, false));
    }

    @Test void rejectsDuplicateNamesInvalidOrderingAndInjectedParameterMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new Signature("void", List.of(
                new Parameter("int", "value"), Parameter.requiredNamed("int?", "value")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Signature("void", List.of(
                Parameter.requiredNamed("int", "count"), new Parameter("String", "label")), List.of()));
        for (String type : List.of("int = injected()", "{int}", "int); injected(", "List<int", "List<int>>")) {
            assertThrows(IllegalArgumentException.class, () -> Parameter.requiredNamed(type, "value"), type);
        }
        for (String name : List.of("class", "value = 0", "value, other", "value}", "")) {
            assertThrows(IllegalArgumentException.class, () -> Parameter.requiredNamed("int?", name), name);
        }
    }
}
