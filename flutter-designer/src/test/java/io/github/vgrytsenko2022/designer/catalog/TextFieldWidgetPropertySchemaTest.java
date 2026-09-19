package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFieldWidgetPropertySchemaTest {

    private static final List<String> PROPERTY_NAMES = List.of(
            "keyboardType", "textInputAction", "textCapitalization", "textAlign",
            "textAlignVertical", "textDirection", "readOnly", "showCursor",
            "autofocus", "obscuringCharacter", "obscureText", "autocorrect",
            "smartDashesType", "smartQuotesType", "enableSuggestions", "maxLines",
            "minLines", "expands", "maxLength", "maxLengthEnforcement", "onChanged",
            "onEditingComplete", "onSubmitted", "onAppPrivateCommand", "enabled",
            "ignorePointers", "cursorWidth", "cursorHeight", "cursorRadiusX",
            "cursorRadiusY", "cursorOpacityAnimates", "cursorColor", "cursorErrorColor",
            "selectionHeightStyle", "selectionWidthStyle", "keyboardAppearance",
            "scrollPaddingLeft", "scrollPaddingTop", "scrollPaddingRight",
            "scrollPaddingBottom", "dragStartBehavior", "enableInteractiveSelection",
            "selectAllOnFocus", "onTap", "onTapAlwaysCalled", "onTapOutside",
            "onTapUpOutside", "mouseCursor", "clipBehavior", "restorationId",
            "stylusHandwritingEnabled", "enableIMEPersonalizedLearning",
            "enableInlinePrediction", "canRequestFocus", "buildCounter", "contextMenuBuilder");

    @Test
    void exposesExactlyFiftySixReviewedLeavesRetainingTheOriginalConstructorOrder() {
        assertEquals(56, TextFieldWidgetPropertySchema.CONSTRUCTOR_PROPERTY_COUNT);
        assertEquals(PROPERTY_NAMES,
                List.copyOf(TextFieldWidgetPropertySchema.definitions().keySet()));
        assertEquals(java.util.stream.IntStream.range(0, 56).boxed().toList(),
                TextFieldWidgetPropertySchema.definitions().values().stream()
                        .map(TextFieldWidgetPropertySchema.Definition::dartOrder)
                        .toList());
        assertTrue(TextFieldWidgetPropertySchema.definitions().values().stream()
                .allMatch(value -> !value.displayName().isBlank()
                        && !value.description().isBlank()
                        && !value.dartName().isBlank()));
    }

    @Test
    void identifiesOnlyTheReviewedSynthesizedConstructorValues() {
        for (String name : List.of(
                "keyboardType", "textAlignVertical", "maxLength",
                "cursorRadiusX", "cursorRadiusY", "scrollPaddingLeft",
                "scrollPaddingTop", "scrollPaddingRight", "scrollPaddingBottom",
                "mouseCursor")) {
            assertTrue(TextFieldWidgetPropertySchema.isSynthesized(
                    new PropertyName(name)), name);
        }
        assertFalse(TextFieldWidgetPropertySchema.isSynthesized(
                new PropertyName("expands")));
        assertFalse(TextFieldWidgetPropertySchema.find(
                new PropertyName("controller")).isPresent());
        assertFalse(TextFieldWidgetPropertySchema.find(
                new PropertyName("child")).isPresent());
    }

    @Test
    void obscuringCharacterPatternAcceptsOneBmpScalarAndRejectsEmojiAndSurrogates() {
        PropertyDefinition obscurer = BuiltInWidgetCatalog.getDefault()
                .find(TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE)
                .orElseThrow()
                .property(new PropertyName("obscuringCharacter"))
                .orElseThrow();
        PropertyValueConstraint.StringPattern pattern = assertInstanceOf(
                PropertyValueConstraint.StringPattern.class,
                obscurer.constraints().getFirst());
        assertEquals("[\\u0000-\\uD7FF\\uE000-\\uFFFF]",
                pattern.regularExpression());

        for (String accepted : List.of(
                "A", Character.toString(0x2022), Character.toString(0x0000),
                Character.toString(0xD7FF), Character.toString(0xE000),
                Character.toString(0xFFFF))) {
            assertTrue(pattern.accepts(new PropertyValue.StringValue(accepted)),
                    Integer.toHexString(accepted.charAt(0)));
        }
        for (String rejected : List.of(
                "", "ab", new String(Character.toChars(0x1F600)),
                Character.toString(0xD800), Character.toString(0xDBFF),
                Character.toString(0xDC00), Character.toString(0xDFFF))) {
            assertFalse(pattern.accepts(new PropertyValue.StringValue(rejected)),
                    rejected.codePoints()
                            .mapToObj(Integer::toHexString)
                            .toList().toString());
        }
    }
}
