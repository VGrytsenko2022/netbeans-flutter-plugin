package dev.flutter.netbeans.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DartStaticTypeProbeTest {
    @Test
    void acceptsOnlyClosedNonNullOuterTypesWithOneOptionalNullableArgument() {
        for (String type : List.of("ShapeBorder", "CustomClipper<RRect>",
                "Animation<Color>", "Animation<Color?>", "AnimationController", "Object?")) {
            assertEquals(type, probe(type).expectedDartType());
        }
    }

    @Test
    void sourceTypeOverridesAreClosedRadioIdentitiesAndPreserveLegacyConstructor() {
        assertEquals(Optional.empty(), probe("Object").sourceTypeOverride());
        for (String expected : List.of("Type", "Object", "Object?", "ValueChanged<Object?>", "RadioGroupRegistry<Object>")) {
            for (String type : List.of("String", "Object?", "_LocalEnum", "project_alias.Choice?")) {
                assertEquals(Optional.of(type), new DartStaticTypeProbe(10, 5, 0, 5, expected,
                        "package:flutter/widgets.dart", Optional.of(type)).sourceTypeOverride());
            }
        }
        for (String type : List.of("", "Choice??", "a.b.c", "List<Choice>", "void Function()",
                "Choice;exit()", "Choice /* comment */", " Choice", "Choice\n")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "Object", "package:flutter/widgets.dart", Optional.of(type)), type);
        }
        assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                "FocusNode", "package:flutter/widgets.dart", Optional.of("Other")));
    }

    @Test
    void rejectsNullableOuterTypesAndTypeExpressionEscapeHatches() {
        for (String type : List.of("", "Animation?", "Animation<Color>?",
                "Animation<Color?>?", "Animation<Color??>", "Animation< Color?>",
                "Animation<Color? >", "Animation<other.Color?>", "other.Animation<Color?>",
                "Animation<List<Color?>>", "Animation<Color?, Color>", "Animation<Color?>;exit()",
                "Animation<Color/*comment*/?>", "Animation<Color>\n", "Animation<>")) {
            assertThrows(IllegalArgumentException.class, () -> probe(type), type);
        }
    }

    private static DartStaticTypeProbe probe(String type) {
        return new DartStaticTypeProbe(10, 5, 0, 5, type, "package:flutter/widgets.dart");
    }
}
