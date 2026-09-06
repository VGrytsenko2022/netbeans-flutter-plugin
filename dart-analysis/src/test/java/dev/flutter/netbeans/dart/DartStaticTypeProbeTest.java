package dev.flutter.netbeans.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class DartStaticTypeProbeTest {
    @Test
    void acceptsOnlyClosedNonNullOuterTypesWithOneOptionalNullableArgument() {
        for (String type : List.of("ShapeBorder", "CustomClipper<RRect>",
                "Animation<Color>", "Animation<Color?>", "AnimationController")) {
            assertEquals(type, probe(type).expectedDartType());
        }
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
