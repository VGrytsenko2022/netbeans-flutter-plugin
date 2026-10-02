package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.events.WidgetEventCatalog;
import io.github.vgrytsenko2022.designer.generation.*;
import io.github.vgrytsenko2022.designer.validation.*;
import io.github.vgrytsenko2022.designer.codec.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultTextStyleTransitionContractTest {
    public static final WidgetTypeId TYPE = DefaultTextStyleTransitionWidgetPropertySchema.TYPE;
    private static final WidgetCatalog C = BuiltInWidgetCatalog.getDefault();
    public static WidgetNode node() { return DefaultTextStyleContractTest.node(TYPE); }
    public static WidgetNode with(WidgetNode node, Map<PropertyName,PropertyValue> props) { return DefaultTextStyleContractTest.with(node, props); }
    private static GeneratedDartRegions generated(WidgetNode node) {
        var result = new DartRegionGenerator().generate(DefaultTextStyleContractTest.document(node), C);
        assertTrue(result.successful(), result.diagnostics().toString());
        return result.generated().orElseThrow();
    }
    @Test void completeConstructorAndAllLocalFieldsHaveStoppedAnimationAndSymbolEvidence() {
        var definition = C.find(TYPE).orElseThrow();
        assertEquals(36, definition.properties().size());
        assertEquals(31, definition.properties().stream().filter(p -> AnimatedDefaultTextStyleWidgetPropertySchema.styleLeaf(p.name())).count());
        assertEquals(1, definition.slots().getFirst().minChildren());
        assertEquals(36, definition.slots().getFirst().parameter().order());
        assertTrue(definition.constConstructor());
        assertTrue(WidgetEventCatalog.eventsFor(definition).isEmpty());
        for (String absent : List.of("textWidthBasis", "textHeightBehavior", "textHeightApplyFirstAscent", "durationUs", "curve", "onEnd", "listenable"))
            assertTrue(definition.property(new PropertyName(absent)).isEmpty(), absent);
        var result = generated(node());
        var code = result.build().payload();
        assertTrue(code.contains("const DefaultTextStyleTransition("), code);
        assertTrue(code.contains("const AlwaysStoppedAnimation<TextStyle>(const TextStyle("), code);
        for (String suffix : List.of(":stopped-text-style-animation", ":stopped-text-style-type"))
            assertTrue(result.symbolOccurrences().stream().anyMatch(o -> o.id().endsWith(suffix) && Set.of("package:flutter/widgets.dart", "package:flutter/material.dart").contains(o.libraryUri())));
        for (String absent : List.of("duration:", "curve:", "onEnd:", "textWidthBasis:", "textHeightBehavior:"))
            assertFalse(code.contains(absent), code);
    }
    @Test void strictAnimationAndNullableIntegerReferencesRoundTripWithExactType() throws Exception {
        for (String name : List.of("style", "maxLines")) for (boolean imported : List.of(false,true))
            for (boolean member : List.of(false,true)) for (boolean factory : List.of(false,true)) {
                var value = new PropertyValue.DartObjectReferenceValue(imported ? Optional.of("package:sample/animation.dart") : Optional.empty(),
                    member ? "Styles" : "animation", member ? Optional.of("current") : Optional.empty(),
                    factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION : PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory ? Optional.of(false) : Optional.empty());
                var n = with(node(), Map.of(new PropertyName(name),value));
                var document = DefaultTextStyleContractTest.document(n);
                var codec = new FdDocumentCodec();
                assertEquals(document, ((FdDecodeResult.Current)codec.decode(codec.encode(document))).document());
                var result = generated(n);
                assertTrue(result.symbolOccurrences().stream().flatMap(o -> o.staticTypeRequirement().stream())
                    .anyMatch(t -> t.expectedDartType().equals(name.equals("style") ? "Animation<TextStyle>" : "int?")));
                if (name.equals("style")) assertFalse(result.build().payload().contains("AlwaysStoppedAnimation"), result.build().payload());
            }
    }
    @Test void fullStyleCompositionThemeFallbackAndDomainConflictsAreClosed() {
        var styled = with(node(), Map.of(
            new PropertyName("styleFontSize"),new PropertyValue.DoubleValue(BigDecimal.valueOf(24)),
            new PropertyName("styleColor"),new PropertyValue.ColorValue(0xff112233L),
            new PropertyName("styleFontWeight"),new PropertyValue.EnumValue("FontWeight","w700"),
            new PropertyName("styleLocaleLanguageCode"),new PropertyValue.StringValue("uk"),
            new PropertyName("styleDecorationUnderline"),new PropertyValue.BooleanValue(true)));
        var code = generated(styled).build().payload();
        for (String token : List.of("AlwaysStoppedAnimation<TextStyle>(", "fontSize: 24.0", "FontWeight.w700", "languageCode: 'uk'", "TextDecoration.underline"))
            assertTrue(code.contains(token), code);
        var themed = generated(with(node(), Map.of(new PropertyName("styleThemeTextStyle"),
            new PropertyValue.ThemeTokenValue(new ThemeToken("material.textTheme.bodyLarge"))))).build().payload();
        assertTrue(themed.contains("AlwaysStoppedAnimation<TextStyle>("), themed);
        assertTrue(themed.contains("?? const TextStyle()"), themed);
        assertFalse(themed.contains("const AlwaysStoppedAnimation"), themed);
        for (var bad : List.of(new PropertyValue.NullValue(), AnimatedDefaultTextStyleContractTest.ref("_animation")))
            assertFalse(new WidgetTreeValidator().validate(DefaultTextStyleContractTest.document(with(styled,Map.of(new PropertyName("style"),bad))),C).valid());
        var missing = new LinkedHashMap<>(node().properties());missing.clear();
        var n = node();
        assertFalse(new WidgetTreeValidator().validate(DefaultTextStyleContractTest.document(new WidgetNode(n.id(),TYPE,missing,n.slots())),C).valid());
        for (String bad : List.of("0","-1","9007199254740992"))
            assertFalse(new WidgetTreeValidator().validate(DefaultTextStyleContractTest.document(with(node(),Map.of(new PropertyName("maxLines"),new PropertyValue.IntegerValue(new BigInteger(bad))))),C).valid());
        for (String allowed : List.of("textAlign","maxLines"))
            assertTrue(generated(with(node(),Map.of(new PropertyName(allowed),new PropertyValue.NullValue()))).build().payload().contains(allowed+": null"));
        for (String bad : List.of("style","softWrap","overflow","textWidthBasis","textHeightBehavior","durationUs"))
            assertFalse(new WidgetTreeValidator().validate(DefaultTextStyleContractTest.document(with(node(),Map.of(new PropertyName(bad),new PropertyValue.NullValue()))),C).valid());
    }
}
