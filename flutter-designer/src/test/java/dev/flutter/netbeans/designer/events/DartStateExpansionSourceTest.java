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

class DartStateExpansionSourceTest {
    private static final String OWNER = "FormLogic";

    @Test void createsOwnedControllerAndNotificationListenerWithEscapedInitialText() {
        WidgetNode bound = bound("flutter.material.TextField", Map.of(), "_text", "_changed",
                StateBinding.Action.CHANGE, Optional.empty(), Optional.empty());
        byte[] source = insert("class FormLogic extends State<Sample> {}\n", bound, "quoted '$value'\nline", false);
        String text = text(source);
        assertTrue(text.contains("final TextEditingController _text = TextEditingController(text: 'quoted \\'\\$value\\'\\u000aline');"), text);
        assertTrue(text.contains("void _textStateListener() {\n    setState(() {});"), text);
        assertTrue(text.contains("super.initState();\n    _text.addListener(_textStateListener);"), text);
        assertTrue(text.contains("_text.removeListener(_textStateListener);\n    _text.dispose();\n    super.dispose();"), text);
        assertTrue(text.contains("void _changed(String value)"));
        assertFalse(text.contains("_text = value;"));
        assertEquals(List.of(field("_text", StateBinding.Type.TEXT_CONTROLLER)),
                DartEventHandlerSource.discoverStateFields(source, OWNER));
    }

    @Test void mergesLifecycleWithoutRewritingCustomStatementsOrComments() {
        String original = "class FormLogic extends State<Sample> {\n"
                + "  @override\n  void initState() {\n    super.initState();\n    // retain init\n    initializeResources();\n  }\n"
                + "  @override\n  void dispose() {\n    // retain cleanup\n    releaseResources();\n    super.dispose();\n  }\n}\n";
        byte[] result = insert(original, controller("_text", "_changed"), "", false);
        String text = text(result);
        assertTrue(text.contains("    // retain init\n    initializeResources();\n  }"));
        assertTrue(text.contains("    // retain cleanup\n    releaseResources();\n    super.dispose();\n  }"));
        assertTrue(text.indexOf("super.initState();") < text.indexOf("_text.addListener"));
        assertTrue(text.indexOf("_text.addListener") < text.indexOf("initializeResources();"));
        assertTrue(text.indexOf("_text.dispose();") < text.indexOf("releaseResources();"));
        assertEquals(1, occurrences(text, "void initState()"));
        assertEquals(1, occurrences(text, "void dispose()"));
    }

    @Test void controllerReuseAddsOnlyHandlerAndRetainsOriginalLifecycle() {
        byte[] first = insert("class FormLogic extends State<Sample> {}\n", controller("_text", "_first"), "initial", false);
        var binding = bound("flutter.material.TextField", Map.of(), "_text", "_second", StateBinding.Action.CHANGE,
                Optional.empty(), Optional.of(field("_text", StateBinding.Type.TEXT_CONTROLLER)));
        byte[] reused = DartEventHandlerSource.insertStateBinding(first, OWNER, binding, "ignored", true);
        String text = text(reused);
        assertEquals(1, occurrences(text, "final TextEditingController _text"));
        assertEquals(1, occurrences(text, "_text.addListener"));
        assertEquals(1, occurrences(text, "_text.dispose();"));
        assertTrue(text.contains("text: 'initial'"));
        assertFalse(text.contains("ignored"));
        assertTrue(text.contains("void _first(String value)"));
        assertTrue(text.contains("void _second(String value)"));
    }

    @Test void multipleOwnedControllersKeepOneLifecyclePairAndIndependentListeners() {
        byte[] first = insert("class FormLogic extends State<Sample> {}\n", controller("_first", "_firstChanged"), "", false);
        byte[] second = DartEventHandlerSource.insertStateBinding(first, OWNER, controller("_second", "_secondChanged"), "", false);
        String text = text(second);
        assertEquals(1, occurrences(text, "void initState()"));
        assertEquals(1, occurrences(text, "void dispose()"));
        assertEquals(2, DartEventHandlerSource.discoverStateFields(second, OWNER).size());
        assertTrue(text.contains("_first.removeListener(_firstStateListener);"));
        assertTrue(text.contains("_second.removeListener(_secondStateListener);"));
    }

    @Test void ambiguousLifecycleAndReservedListenerNamesFailWithoutChangingInput() {
        for (String members : List.of("void initState() => super.initState();", "void initState() async { super.initState(); }",
                "void initState() { initializeResources(); super.initState(); }", "void dispose() { super.dispose(); releaseResources(); }",
                "void dispose() { if (true) { super.dispose(); } }", "void _textStateListener() {}")) {
            byte[] original = bytes("class FormLogic extends State<Sample> {" + members + "}\n");
            byte[] snapshot = original.clone();
            assertThrows(IllegalArgumentException.class,
                    () -> DartEventHandlerSource.insertStateBinding(original, OWNER, controller("_text", "_changed"), "", false), members);
            assertArrayEquals(snapshot, original);
        }
    }

    @Test void missingConditionalDuplicateAndBorrowedControllerOwnershipIsRejected() {
        WidgetNode bound = controller("_text", "_changed");
        String valid = text(insert("class FormLogic extends State<Sample> {}\n", bound, "", false));
        for (String invalid : List.of(
                valid.replace("_text.addListener(_textStateListener);", ""),
                valid.replace("_text.dispose();", "if (mounted) _text.dispose();"),
                valid.replace("_text.dispose();", "if (mounted) { _text.dispose(); }"),
                valid.replace("_text.dispose();", "return; _text.dispose();"),
                valid.replace("_text.dispose();", "_text.dispose(); _text.dispose();"),
                valid.replace("_text.removeListener(_textStateListener);", "final _text = TextEditingController(); _text.removeListener(_textStateListener);"),
                valid.replace("_text.removeListener(_textStateListener);", "void _textStateListener() {} _text.removeListener(_textStateListener);"),
                valid.replace("TextEditingController(text: '')", "TextEditingController.shared"),
                valid.replace("final TextEditingController", "TextEditingController"))) {
            assertThrows(IllegalArgumentException.class, () -> DartEventHandlerSource.requireStateBindingFields(
                    bytes(invalid), OWNER, List.of(bound.stateBinding().orElseThrow())), invalid);
            assertTrue(DartEventHandlerSource.discoverStateFields(bytes(invalid), OWNER).isEmpty());
        }
    }

    @Test void discoversOnlyDirectInitializedMutableFieldsOrOwnedControllers() {
        String source = "class FormLogic extends State<Sample> { bool _flag = false; String _label = ''; int _count = 0; "
                + "double _amount = 0.0; num _number = 0; int? _choice = null; RangeValues _range = RangeValues(0, 1); "
                + "final bool _constant = false; static bool _static = false; late bool _late = false; "
                + "bool get _getter => false; bool _uninitialized; final TextEditingController _unowned = TextEditingController(); }";
        var fields = DartEventHandlerSource.discoverStateFields(bytes(source), OWNER);
        assertEquals(List.of("_flag", "_label", "_count", "_amount", "_number", "_choice", "_range"),
                fields.stream().map(StatePropertyBinding::fieldName).toList());
        DartEventHandlerSource.requirePropertyBindingFields(bytes(source), OWNER, fields);
    }

    @Test void toggleAndSelectAreTypedAndReuseDoesNotChangeFieldInitializer() {
        WidgetNode toggle = bound("flutter.material.IconButton", Map.of(new PropertyName("isSelected"), new PropertyValue.BooleanValue(false)),
                "_selected", "_pressed", StateBinding.Action.TOGGLE, Optional.empty(), Optional.empty());
        String text = text(insert("class FormLogic extends State<Sample> {}", toggle, "", false));
        assertTrue(text.contains("bool _selected = false;"));
        assertTrue(text.contains("void _pressed()"));
        assertTrue(text.contains("_selected = !_selected;"));
        for (boolean selected : List.of(false, true)) {
            WidgetNode select = bound("flutter.material.ListTile", Map.of(new PropertyName("selected"), new PropertyValue.BooleanValue(selected)),
                    "_choice", "_tap", StateBinding.Action.SELECT, Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.TWO)), Optional.empty());
            String created = text(insert("class FormLogic extends State<Sample> {}", select, "", false));
            assertTrue(created.contains("int? _choice = " + (selected ? "2" : "null") + ";"), created);
            assertTrue(created.contains("void _tap()"));
            assertTrue(created.contains("_choice = 2;"));
        }
        WidgetNode select = bound("flutter.material.ListTile", Map.of(), "_choice", "_tap", StateBinding.Action.SELECT,
                Optional.of(new PropertyValue.IntegerValue(java.math.BigInteger.TWO)), Optional.of(field("_choice", StateBinding.Type.INT)));
        String reused = text(insert("class FormLogic extends State<Sample> { int _choice = 0; }", select, "", true));
        assertEquals(1, occurrences(reused, "int _choice = 0;"));
        assertFalse(reused.contains("int? _choice"));
        assertTrue(reused.contains("_choice = 2;"));
    }

    private static WidgetNode controller(String field, String handler) {
        return bound("flutter.material.TextField", Map.of(), field, handler, StateBinding.Action.CHANGE,
                Optional.empty(), Optional.empty());
    }

    private static WidgetNode bound(String type, Map<PropertyName, PropertyValue> properties, String field, String handler,
            StateBinding.Action action, Optional<PropertyValue> selected, Optional<StatePropertyBinding> reused) {
        var widget = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow(),
                StableId.random(), properties);
        var binding = WidgetStateBindingCatalog.createBinding(widget, field, handler, action, selected, reused);
        return new WidgetNode(widget.id(), widget.type(), widget.properties(), widget.slots(), widget.extensions(), Optional.of(binding));
    }
    private static StatePropertyBinding field(String name, StateBinding.Type type) {
        return new StatePropertyBinding(name, type, Optional.empty(), StatePropertyBinding.Transform.DIRECT);
    }
    private static byte[] insert(String source, WidgetNode bound, String initial, boolean reuse) {
        return DartEventHandlerSource.insertStateBinding(bytes(source), OWNER, bound, initial, reuse);
    }
    private static String text(byte[] value) { return new String(value, StandardCharsets.UTF_8); }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static int occurrences(String text, String part) { return (text.length() - text.replace(part, "").length()) / part.length(); }
}
