package dev.flutter.netbeans.designer.events;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DartStateFieldRenameSourceTest {
    private static final String OWNER = "Storage";

    @Test void renamesUnambiguousReadsWritesCompoundAssignmentsAndThisReferences() {
        String source = "class Storage {\n  int _count = 0;\n"
                + "  int update() { _count = 1; _count += 2; ++_count; _count--; this._count *= 3;\n"
                + "    if (_count > 0) { _count++; } while (_count < 9) { _count++; }\n"
                + "    if (_count > 0) _count = 1; else _count = 2; while (_count < 2) _count = 2;\n"
                + "    do _count++; while (_count < 3);\n"
                + "    printValues(1, _count, 3); printValues(1, _count);\n"
                + "    for (; _count < 10; _count++) {} return _count; }\n}\n";
        String renamed = rename(source, field("_count", StateBinding.Type.INT), "_total");
        assertEquals(source.replace("_count", "_total"), renamed);
    }

    @Test void preservesCommentsPlainStringsAndManagedPayloadsByteForByte() {
        String source = "class Storage {\n  bool _flag = false;\n"
                + "  // _flag is user documentation\n  String label = '_flag';\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + "  Widget build(BuildContext context) => Checkbox(value: _flag, onChanged: null);\n"
                + "  // </netbeans-flutter-designer>\n"
                + "  void update() { /* _flag */ _flag = !_flag; }\n}\n";
        String result = rename(source, field("_flag", StateBinding.Type.BOOL), "_enabled");
        assertTrue(result.contains("bool _enabled = false;"));
        assertTrue(result.contains("/* _flag */ _enabled = !_enabled;"));
        assertTrue(result.contains("// _flag is user documentation"));
        assertTrue(result.contains("String label = '_flag';"));
        assertTrue(result.contains("Checkbox(value: _flag, onChanged: null)"));
    }

    @Test void controllerRenamesDerivedListenerButLeavesActionHandlerAndUserBodiesIntact() {
        var widget = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(new WidgetTypeId("flutter.material.TextField")).orElseThrow(), StableId.random());
        var binding = WidgetStateBindingCatalog.createBinding(widget, "_text", "_changed");
        var bound = new WidgetNode(widget.id(), widget.type(), widget.properties(), widget.slots(), widget.extensions(), Optional.of(binding));
        byte[] created = DartEventHandlerSource.insertStateBinding(bytes("class Storage {\n  void custom() { print('user'); }\n}\n"),
                OWNER, bound, "initial", false);
        String result = text(DartEventHandlerSource.renameStateField(created, OWNER,
                field("_text", StateBinding.Type.TEXT_CONTROLLER), "_query"));
        assertTrue(result.contains("final TextEditingController _query = TextEditingController(text: 'initial');"));
        assertTrue(result.contains("void _queryStateListener()"));
        assertTrue(result.contains("_query.addListener(_queryStateListener);"));
        assertTrue(result.contains("_query.removeListener(_queryStateListener);"));
        assertTrue(result.contains("_query.dispose();"));
        assertTrue(result.contains("void _changed(String value)"));
        assertTrue(result.contains("void custom() { print('user'); }"));
        assertFalse(result.contains("_text"));
        DartEventHandlerSource.requirePropertyBindingFields(bytes(result), OWNER,
                List.of(field("_query", StateBinding.Type.TEXT_CONTROLLER)));
        String longestField = "_" + "q".repeat(127);
        String longest = text(DartEventHandlerSource.renameStateField(created, OWNER,
                field("_text", StateBinding.Type.TEXT_CONTROLLER), longestField));
        assertTrue(longest.contains("void " + longestField + "StateListener()"));
        DartEventHandlerSource.requirePropertyBindingFields(bytes(longest), OWNER,
                List.of(field(longestField, StateBinding.Type.TEXT_CONTROLLER)));
        String shadowedListener = text(created).replace("void custom() { print('user'); }",
                "void custom() { _textStateListener() {} _textStateListener(); }");
        assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.renameStateField(bytes(shadowedListener), OWNER,
                field("_text", StateBinding.Type.TEXT_CONTROLLER), "_query"));
    }

    @Test void rejectsLocalShadowingParametersPatternsUnknownReceiversAndInterpolation() {
        for (String member : List.of(
                "void update() { var _count = 2; print(_count); }",
                "void update() { int? _count = 2; print(_count); }",
                "void update() { List<int> _count = []; print(_count); }",
                "void update() { void Function() _count = () {}; _count(); }",
                "void update() { (int, int) _count = (1, 2); print(_count); }",
                "void update() { int other = 1, _count = 2; print(_count); }",
                "void update() { int? other = 1, _count, third; print(_count); }",
                "void update() { for (var other = 0, _count = 1; other < 2; other++) { print(_count); } }",
                "void update() { _count() => 42; print(_count()); }",
                "void update() { _count<T>() => 42; print(_count<int>()); }",
                "void update() { void local<_count>() {} local<int>(); }",
                "void update(int _count) { print(_count); }",
                "void update() { final (_count, other) = (1, 2); print(other); }",
                "void update() { [1].forEach((_count) { print(_count); }); }",
                "void update() { other._count = 3; }",
                "String read() => '$_count';",
                "String read() => '${this._count}';")) {
            byte[] original = bytes("class Storage { int _count = 0; " + member + " }\n");
            byte[] snapshot = original.clone();
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.renameStateField(original, OWNER,
                    field("_count", StateBinding.Type.INT), "_total"), member);
            assertArrayEquals(snapshot, original);
        }
    }

    @Test void rejectsSymbolLiteralValuesIncludingThisQualifiedSymbols() {
        for (String symbol : List.of("#_count", "#this._count", "#Storage._count", "#_count.member")) {
            String source = "class Storage { int _count = 0; Object name() => " + symbol + "; }";
            assertThrows(IllegalArgumentException.class, () -> rename(source, field("_count", StateBinding.Type.INT), "_total"), symbol);
        }
    }

    @Test void allClosedStateFieldTypesRetainTheirExactTypeAndInitializer() {
        for (StateBinding.Type type : StateBinding.Type.values()) {
            if (type == StateBinding.Type.TEXT_CONTROLLER) continue; // Complete owned lifecycle is exercised above.
            String dartType = switch (type) {
                case BOOL -> "bool"; case NULLABLE_BOOL -> "bool?";
                case STRING -> "String"; case NULLABLE_STRING -> "String?";
                case INT -> "int"; case NULLABLE_INT -> "int?";
                case DOUBLE -> "double"; case NULLABLE_DOUBLE -> "double?";
                case NUM -> "num"; case NULLABLE_NUM -> "num?";
                case NULLABLE_OBJECT -> "Object?"; case NULLABLE_REFERENCE -> "Choice?";
                case RANGE_VALUES -> "RangeValues";
                case TEXT_CONTROLLER -> throw new AssertionError();
            };
            String initializer = type.name().startsWith("NULLABLE_") ? "null" : switch (type) {
                case BOOL -> "false"; case STRING -> "'unchanged'"; case INT, NUM -> "3";
                case DOUBLE -> "3.5"; case RANGE_VALUES -> "RangeValues(0.2, 0.8)";
                default -> throw new AssertionError(type);
            };
            Optional<PropertyValue.DartObjectReferenceValue> reference = type == StateBinding.Type.NULLABLE_REFERENCE
                    ? Optional.of(new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/choice.dart"), "Choice", Optional.empty(),
                            PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())) : Optional.empty();
            var field = new StatePropertyBinding("_value", type, reference, StatePropertyBinding.Transform.DIRECT);
            String source = "class Storage { " + dartType + " _value = " + initializer + "; Object? read() => _value; }";
            assertEquals(source.indexOf("_value"), DartEventHandlerSource.stateFieldNameOffset(bytes(source), OWNER, field), type.toString());
            assertEquals(source.replace("_value", "_renamed"), rename(source, field, "_renamed"), type.toString());
        }
    }

    @Test void rejectsOutsideOwnerPrivateReferencesAndLibraryParts() {
        for (String source : List.of(
                "class Storage { int _count = 0; } class Peer { int read(Storage state) => state._count; }",
                "class Storage { int _count = 0; } int read(Storage state) => state._count;",
                "part 'hidden.dart'; class Storage { int _count = 0; }",
                "part of 'owner.dart'; class Storage { int _count = 0; }")) {
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.renameStateField(bytes(source), OWNER,
                    field("_count", StateBinding.Type.INT), "_total"), source);
        }
        String part = "part 'hidden.dart'; class Storage { int _count = 0; }";
        assertEquals(part.indexOf("_count"), DartEventHandlerSource.stateFieldNameOffset(bytes(part), OWNER,
                field("_count", StateBinding.Type.INT)), "Read-only Go to Field need not reject library parts");
    }

    @Test void rejectsNewNameCollisionsInMembersUserReadsAndCurrentManagedReferences() {
        for (String extra : List.of("int _total = 1;", "void _total() {}", "int read() => _total;",
                "// <netbeans-flutter-designer region=\"build\">\n Widget build(BuildContext context) => Text(_total);\n"
                        + "// </netbeans-flutter-designer>\n")) {
            String source = "class Storage { int _count = 0;\n " + extra + "}\n";
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.requireStateFieldRenameNamesAvailable(
                    bytes(source), OWNER, field("_count", StateBinding.Type.INT), "_total"), extra);
        }
        String extension = "class Storage { int _count = 0; } extension Extra on Storage { int get _total => 42; } "
                + "int read(Storage state) => state._total;";
        assertThrows(IllegalArgumentException.class, () -> rename(extension, field("_count", StateBinding.Type.INT), "_total"),
                "A new instance field must not replace an extension getter's dispatch outside its owner");
    }

    @Test void exactNameOffsetUsesUtf16AndPreservesBomCrLfAndNameBounds() {
        String source = "\uFEFF// 😀 header\r\nclass Storage {\r\n  String? _label = null;\r\n  String? read() => _label;\r\n}\r\n";
        var field = field("_label", StateBinding.Type.NULLABLE_STRING);
        assertEquals(source.indexOf("_label"), DartEventHandlerSource.stateFieldNameOffset(bytes(source), OWNER, field));
        assertEquals(source.replace("_label", "_caption"), rename(source, field, "_caption"));
        assertArrayEquals(bytes(source), DartEventHandlerSource.renameStateField(bytes(source), OWNER, field, "_label"));
        assertThrows(IllegalArgumentException.class, () -> rename(source, field, "publicField"));
        assertThrows(IllegalArgumentException.class, () -> rename(source, field, "_" + "x".repeat(128)));
    }

    @Test void nameOffsetAndRenameRejectIncorrectTypeOrMissingField() {
        String source = "class Storage { final int _count = 0; bool _flag = false; }";
        for (StatePropertyBinding field : List.of(field("_count", StateBinding.Type.INT),
                field("_missing", StateBinding.Type.BOOL), field("_flag", StateBinding.Type.INT))) {
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.stateFieldNameOffset(bytes(source), OWNER, field));
            assertThrows(IllegalArgumentException.class, () -> rename(source, field, "_renamed"));
        }
    }

    @Test void provedReplayIgnoresOnlyManagedDestinationTokensAndPreservesAllOtherGuards() {
        String source = "class Storage { int _count = 0;\n"
                + "// <netbeans-flutter-designer region=\"build\">\n"
                + "Widget build(BuildContext context) => Text(_total.toString());\n"
                + "// </netbeans-flutter-designer>\n"
                + "void update() { _count++; }\n}\n";
        var field = field("_count", StateBinding.Type.INT);
        assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.renameStateField(bytes(source), OWNER, field, "_total"));
        String replayed = text(DartEventHandlerSource.replayStateFieldRename(bytes(source), OWNER, field, "_total"));
        assertEquals(source.replace("int _count", "int _total").replace("_count++;", "_total++;"), replayed);
        for (String unsafe : List.of(source.replace("void update()", "int _total = 1; void update()"),
                source.replace("_count++;", "print(#_count); _count++;"),
                source.replace("_count++;", "print('$_total'); _count++;"),
                "part 'other.dart';\n" + source)) {
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.replayStateFieldRename(bytes(unsafe), OWNER, field, "_total"));
        }
    }

    private static StatePropertyBinding field(String name, StateBinding.Type type) {
        return new StatePropertyBinding(name, type, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
    }
    private static String rename(String source, StatePropertyBinding field, String newName) {
        return text(DartEventHandlerSource.renameStateField(bytes(source), OWNER, field, newName));
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static String text(byte[] value) { return new String(value, StandardCharsets.UTF_8); }
}
