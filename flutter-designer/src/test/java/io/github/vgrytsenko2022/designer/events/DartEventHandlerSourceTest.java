package io.github.vgrytsenko2022.designer.events;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DartEventHandlerSourceTest {
    @Test
    void retainedImportsStayOutsideInitialImportsGuardWithOrWithoutBomAndCrLf() {
        for (String ending : List.of("\n", "\r\n")) {
            for (String prefix : List.of("", "\ufeff")) {
                String guard = "// <netbeans-flutter-designer region=\"imports\">" + ending
                        + "import 'package:flutter/material.dart';" + ending
                        + "// </netbeans-flutter-designer>" + ending;
                String original = prefix + guard + ending + "class Screen {}" + ending;
                String retained = "import 'package:flutter/widgets.dart';" + ending;
                byte[] result = DartEventHandlerSource.addImports(bytes(original), List.of("package:flutter/widgets.dart"));
                assertEquals(prefix + guard + retained + ending + "class Screen {}" + ending, text(result));
                assertEquals(original, text(result).replace(retained, ""), "Existing bytes and both native guard boundaries stay untouched.");
                assertArrayEquals(result, DartEventHandlerSource.addImports(result, List.of("package:flutter/widgets.dart")),
                        "Repeated requests reuse the retained unguarded import.");
            }
        }
    }

    @Test
    void bomAllowanceDoesNotWeakenGuardLineBoundariesOrPermitNestedMarkers() {
        String guard = "// <netbeans-flutter-designer region=\"imports\">\n"
                + "import 'package:flutter/material.dart';\n// </netbeans-flutter-designer>\n";
        for (String prefix : List.of("\n\ufeff", "\ufefftoken ", "\ufeff\ufeff")) {
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.addImports(
                    bytes(prefix + guard + "class Screen {}\n"), List.of("dart:async")),
                    "Only one BOM at the file start may precede a guard marker.");
        }
        String nested = "\ufeff// <netbeans-flutter-designer region=\"imports\">\n" + guard;
        assertThrows(IllegalArgumentException.class,
                () -> DartEventHandlerSource.addImports(bytes(nested), List.of("dart:async")));
    }

    @Test
    void inventoriesOnlyDirectMethodsOfExactOwner() {
        String source = """
                // class Screen { void fake() {} }
                class ScreenExtra { void notOurs() {} }
                class Screen extends StatelessWidget {
                  const Screen({super.key});
                  final String text = 'class Screen { void hidden() {} }';
                  final Object value = () { void local() {} return local; };
                  @override
                  Widget build(BuildContext context) => const Text('hello');
                  Future<void> _changed(String value, {bool enabled = true}) async {
                    void nested() {}
                    final callback = (String s) { nested(); };
                  }
                  T _identity<T extends Object>(T value) => value;
                  static void shared() {}
                  int get count => 1;
                  set count(int value) {}
                }
                class Other { void extra() {} }
                """;
        List<DartEventHandlerSource.Method> methods = DartEventHandlerSource.methods(bytes(source), "Screen");
        assertEquals(List.of("build", "_changed", "_identity", "shared"),
                methods.stream().map(DartEventHandlerSource.Method::name).toList());
        assertEquals("Future<void>", methods.get(1).returnType());
        assertEquals("String value, {bool enabled = true}", methods.get(1).parameters());
        assertEquals("T", methods.get(2).returnType());
        assertTrue(methods.get(3).isStatic());
        for (DartEventHandlerSource.Method method : methods) {
            assertEquals(method.name(), source.substring(method.nameStart(), method.nameEnd()));
            assertEquals(method.parameters(), source.substring(method.parametersStart(), method.parametersEnd()));
            assertTrue(method.hasBody());
        }
    }

    @Test
    void handlesFunctionReturnTypesAndAnnotatedDefaultsWithoutMistakingCallsForMethods() {
        String source = """
                class Screen {
                  @annotation('text', {'x': 1})
                  void Function(String) callbackFactory() => (value) {};
                  final provider = Widget.named(onTap: () {});
                  final mapped = <String, Object>{'x': 1}.map((key, value) => MapEntry(key, value));
                  void event(void Function(int) callback, [String text = '(){}']) {}
                }
                """;
        List<DartEventHandlerSource.Method> methods = DartEventHandlerSource.methods(bytes(source), "Screen");
        assertEquals(List.of("callbackFactory", "event"),
                methods.stream().map(DartEventHandlerSource.Method::name).toList());
        assertEquals("void Function(String)", methods.getFirst().returnType());
    }

    @Test
    void insertsIntoStateOwnerAndPreservesEveryOriginalByte() {
        String source = "\ufeff// Україна 😀\r\nclass Screen extends StatefulWidget {}\r\n"
                + "class _ScreenState extends State<Screen> {\r\n"
                + "  final String label = 'не змінювати';\r\n}\r\n// tail\n";
        byte[] result = DartEventHandlerSource.insert(bytes(source), "_ScreenState", "_changed",
                "void _changed(bool value) {\n  print(value);\n}");
        String updated = text(result);
        String insertion = "\r\n  void _changed(bool value) {\r\n    print(value);\r\n  }\r\n";
        assertTrue(updated.contains(insertion));
        assertEquals(source, updated.replace(insertion, ""));
        DartEventHandlerSource.Method method = DartEventHandlerSource.methods(result, "_ScreenState").getFirst();
        int byteOffset = DartEventHandlerSource.byteOffset(result, method.nameStart());
        assertEquals("_changed", new String(result, byteOffset, 8, StandardCharsets.UTF_8));
        int emoji = source.indexOf("😀");
        assertThrows(IllegalArgumentException.class,
                () -> DartEventHandlerSource.byteOffset(bytes(source), emoji + 1));
    }

    @Test
    void rejectsDuplicateClassesMissingOwnerMalformedSourceAndDuplicateMethodNames() {
        for (String source : List.of(
                "class Screen {} class Screen {}",
                "class ScreenExtra {}",
                "class Screen { void f() {} void f() {} }",
                "class Screen { void f() { ] } }",
                "class Screen { final text = 'unterminated; }",
                "class Screen { /* unterminated }",
                "void wrapper() { class Screen {} }")) {
            assertThrows(IllegalArgumentException.class,
                    () -> DartEventHandlerSource.methods(bytes(source), "Screen"), source);
        }
        assertThrows(IllegalArgumentException.class,
                () -> DartEventHandlerSource.methods(new byte[]{(byte) 0xc3, 0x28}, "Screen"));
    }

    @Test
    void insertionRejectsCollisionStaticAbstractOrAdditionalSource() {
        for (String source : List.of(
                "class Screen { void _changed() {} }",
                "class Screen { final _changed = 1; }",
                "class Screen { Object get _changed => 1; }")) {
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.insert(
                    bytes(source), "Screen", "_changed", "void _changed() {}"));
        }
        for (String addition : List.of(
                "static void _changed() {}", "void _changed();", "void different() {}",
                "void _changed() {} int field = 1;", "} class Other {} class Injected { void _changed() {}")) {
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.insert(
                    bytes("class Screen {}"), "Screen", "_changed", addition), addition);
        }
        assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.insert(
                bytes("class Screen {}"), "Screen", "_changed", "void _changed() { print('\ud800'); }"));
    }

    @Test
    void renamePreservesStringsCommentsOtherNamesAndManagedPayload() {
        String source = """
                class Screen {
                  // <netbeans-flutter-designer region="build">
                  Widget build(BuildContext context) => Button(onPressed: _tap);
                  // </netbeans-flutter-designer>
                  void _tap() {
                    // _tap();
                    final text = '_tap';
                    this._tap();
                  }
                  void other() { _tap(); final callback = _tap; }
                  void _tapExtra() {}
                }
                """;
        byte[] renamed = DartEventHandlerSource.rename(bytes(source), "Screen", "_tap", "_pressed");
        String result = text(renamed);
        assertTrue(result.contains("onPressed: _tap"));
        assertTrue(result.contains("// _tap();"));
        assertTrue(result.contains("final text = '_tap';"));
        assertTrue(result.contains("void _pressed()"));
        assertTrue(result.contains("this._pressed();"));
        assertTrue(result.contains("_pressed(); final callback = _pressed;"));
        assertTrue(result.contains("void _tapExtra()"));
        assertTrue(DartEventHandlerSource.methods(renamed, "Screen").getFirst().generated());
    }

    @Test
    void renameRejectsShadowedOrUnresolvedBindingsWithoutPartialChanges() {
        for (String body : List.of(
                "void other(int _tap) { print(_tap); }",
                "void other() { final _tap = () {}; _tap(); }",
                "void other() { void _tap() {} _tap(); }",
                "void other() { var (_tap, other) = record; _tap(); }",
                "void other() { final [_tap] = values; _tap(); }",
                "void other() { values.forEach((_tap) { _tap(); }); }",
                "void other() { otherObject._tap(); }",
                "void other() { print('value $_tap'); }",
                "void other() { print('value ${this._tap}'); }")) {
            byte[] source = bytes("class Screen { void _tap() {} " + body + " }");
            byte[] original = source.clone();
            assertThrows(IllegalArgumentException.class,
                    () -> DartEventHandlerSource.rename(source, "Screen", "_tap", "_pressed"), body);
            assertArrayEquals(original, source);
        }
        assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.rename(
                bytes("class Screen { void _tap() {} } void other() { screen._tap(); }"),
                "Screen", "_tap", "_pressed"));
        assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.rename(
                bytes("class Screen { void _tap() {} void other() { final _pressed = () {}; _tap(); } }"),
                "Screen", "_tap", "_pressed"));
    }

    @Test
    void recognizesRawAndNestedInterpolatedStringsAndNestedComments() {
        String source = """
                class Screen {
                  /* comment /* nested } */ class Screen {} */
                  final raw = r'''class Screen { void fake() {} } $literal''';
                  final text = 'value ${(() { return "nested ${1}"; })()}';
                  void _tap() { print(r'$_tap'); }
                }
                """;
        assertEquals(List.of("_tap"), DartEventHandlerSource.methods(bytes(source), "Screen")
                .stream().map(DartEventHandlerSource.Method::name).toList());
        assertTrue(text(DartEventHandlerSource.rename(bytes(source), "Screen", "_tap", "_pressed"))
                .contains("print(r'$_tap')"));
    }

    @Test
    void rejectsRenamingGeneratedMethodAndBadIdentifiers() {
        String source = """
                class Screen {
                  // <netbeans-flutter-designer region="build">
                  void build() {}
                  // </netbeans-flutter-designer>
                }
                """;
        assertThrows(IllegalArgumentException.class,
                () -> DartEventHandlerSource.rename(bytes(source), "Screen", "build", "other"));
        assertThrows(IllegalArgumentException.class,
                () -> DartEventHandlerSource.rename(bytes("class Screen { @override void build() {} }"),
                        "Screen", "build", "other"));
        for (String name : List.of("a.b", "void", "a()", "", "1event")) {
            assertThrows(IllegalArgumentException.class,
                    () -> DartEventHandlerSource.insert(bytes(source), "Screen", name, "void f() {}"));
        }
    }

    @Test
    void refusesInsertionWithinManagedClassAndAmbiguousConstructorInitializers() {
        assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.insert(bytes("""
                // <netbeans-flutter-designer region="build">
                class Screen {}
                // </netbeans-flutter-designer>
                """), "Screen", "_tap", "void _tap() {}"));
        assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.methods(
                bytes("class Screen { Screen() : callback = () {} {} void event() {} }"), "Screen"));
        assertEquals(List.of("event"), DartEventHandlerSource.methods(
                bytes("class Screen { const Screen() : super(); void event() {} }"), "Screen")
                .stream().map(DartEventHandlerSource.Method::name).toList());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
