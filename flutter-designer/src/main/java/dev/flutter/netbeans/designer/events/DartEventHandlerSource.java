package dev.flutter.netbeans.designer.events;

import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StateBinding;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded, lexical source operations for user-owned event methods. This is not a
 * Dart resolver: the caller must analyze the complete candidate before applying
 * it. Ambiguous refactorings fail closed instead of guessing a symbol binding.
 * All recorded offsets are UTF-16 offsets (as used by Java/NetBeans documents).
 */
public final class DartEventHandlerSource {
    private static final int MAX_SOURCE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_NESTING = 256;
    private static final int MAX_TOKENS = 500_000;
    private static final String OPEN_MARKER = "// <netbeans-flutter-designer region=\"";
    private static final String CLOSE_MARKER = "// </netbeans-flutter-designer>";
    private static final Set<String> MODIFIERS = Set.of(
            "static", "external", "abstract", "covariant", "factory");
    private static final Set<String> RESERVED = Set.of(
            "assert", "break", "case", "catch", "class", "const", "continue", "default",
            "do", "else", "enum", "extends", "false", "final", "finally", "for", "if",
            "in", "is", "new", "null", "rethrow", "return", "super", "switch", "this",
            "throw", "true", "try", "var", "void", "while", "with", "await", "yield",
            "async", "sync", "get", "set", "operator", "static", "external", "factory");

    private DartEventHandlerSource() {
    }

    /** One direct method, including its precise source and parameter spans. */
    public record Method(String name, String signature, String returnType, String parameters,
            int declarationStart, int declarationEnd, int nameStart, int nameEnd,
            int parametersStart, int parametersEnd, int bodyStart, int bodyEnd,
            boolean isStatic, boolean generated) {
        public int headerStart() {
            return declarationStart;
        }

        public int headerEnd() {
            return bodyStart;
        }

        public boolean hasBody() {
            return bodyEnd > bodyStart;
        }
    }

    /** Returns direct methods of exactly one top-level class, never local functions. */
    public static List<Method> methods(byte[] source, String ownerClass) {
        return inspect(source, ownerClass).methods();
    }

    /**
     * Inserts one instance method immediately before the owning class closes.
     * Every pre-existing source byte, including BOM and mixed line endings, is
     * retained. The inserted member follows the source's line-ending convention.
     */
    public static byte[] insert(byte[] source, String ownerClass,
            String handlerName, String methodSource) {
        requireIdentifier(handlerName);
        Objects.requireNonNull(methodSource, "methodSource");
        Inspection target = inspect(source, ownerClass);
        requireFreeName(target, handlerName);
        String wrapper = "class " + ownerClass + " {\n" + methodSource + "\n}";
        Inspection addition = inspect(encode(wrapper), ownerClass);
        if (addition.members().size() != 1 || addition.methods().size() != 1
                || !addition.methods().getFirst().name().equals(handlerName)
                || addition.methods().getFirst().isStatic()
                || !addition.methods().getFirst().hasBody()
                || !addition.lex().managed().isEmpty()) {
            throw unsafe("Insertion requires exactly one concrete instance method named '"
                    + handlerName + "'.");
        }
        String text = target.lex().text();
        int insertion = target.lex().tokens().get(target.close()).start();
        String lineEnding = text.contains("\r\n") ? "\r\n" : "\n";
        int lineStart = lineStart(text, insertion);
        String indentation = text.substring(lineStart, insertion);
        if (indentation.isBlank()) {
            insertion = lineStart;
        } else {
            indentation = "";
        }
        if (target.lex().managed(insertion)
                || target.lex().managed(target.lex().tokens().get(target.close()).start())) {
            throw unsafe("The owning class closes inside a generated region.");
        }
        String member = methodSource.replace("\r\n", "\n").replace('\r', '\n')
                .strip().stripIndent();
        StringBuilder indented = new StringBuilder(lineEnding);
        for (String line : member.split("\n", -1)) {
            indented.append(indentation).append("  ").append(line).append(lineEnding);
        }
        return (text.substring(0, insertion) + indented + text.substring(insertion))
                .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Scaffolds exactly one reviewed top-level delegate; never overwrites an existing user class.
     * Imports and source bytes belong to the same reversible source projection as insertion.
     */
    public static byte[] insertPersistentHeaderDelegate(byte[] source) {
        String declaration = """
                // Editable SliverPersistentHeader delegate. Designer does not regenerate this class.
                class _FlutterDesignerPersistentHeaderDelegate extends SliverPersistentHeaderDelegate {
                  const _FlutterDesignerPersistentHeaderDelegate();
                  @override
                  double get minExtent => 56;
                  @override
                  double get maxExtent => 112;
                  @override
                  Widget build(BuildContext context, double shrinkOffset, bool overlapsContent) =>
                      const SizedBox.expand();
                  @override
                  bool shouldRebuild(covariant _FlutterDesignerPersistentHeaderDelegate oldDelegate) => false;
                }
                """;
        return insertReviewedDelegate(source,
                dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.DELEGATE_CLASS, declaration);
    }

    /** Inserts the finite-size starter, once; every delegate method remains user-owned. */
    public static byte[] insertSingleChildLayoutDelegate(byte[] source) {
        return insertReviewedDelegate(source,
                dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.DELEGATE_CLASS, """
                // Editable CustomSingleChildLayout delegate. Designer does not regenerate this class.
                class _FlutterDesignerSingleChildLayoutDelegate extends SingleChildLayoutDelegate {
                  const _FlutterDesignerSingleChildLayoutDelegate({super.relayout});
                  @override
                  Size getSize(BoxConstraints constraints) => constraints.constrain(const Size(128, 96));
                  @override
                  BoxConstraints getConstraintsForChild(BoxConstraints constraints) =>
                      BoxConstraints.loose(getSize(constraints));
                  @override
                  Offset getPositionForChild(Size size, Size childSize) =>
                      Offset((size.width - childSize.width) / 2, (size.height - childSize.height) / 2);
                  @override
                  bool shouldRelayout(covariant _FlutterDesignerSingleChildLayoutDelegate oldDelegate) => false;
                }
                """);
    }

    private static byte[] insertReviewedDelegate(byte[] source, String name, String declaration) {
        Lexed lex = new Lexer(decode(source)).scan();
        for (int i = 0; i < lex.tokens().size(); i++) {
            if (value(lex, i).equals("class") && value(lex, i + 1).equals(name)) {
                inspect(source, name); // rejects duplicate or non-concrete/ambiguous declarations
                if (lex.managed(lex.tokens().get(i).start())) throw unsafe("Layout delegate must be outside managed regions.");
                return source.clone(); // edits to this class are user-owned; the Dart analyzer proves its type
            }
            if (value(lex, i).equals(name) && !lex.managed(lex.tokens().get(i).start())) {
                throw unsafe("Cannot create layout delegate: top-level name '" + name + "' is already in use.");
            }
            String token = value(lex, i);
            if (token.equals("{") || token.equals("(") || token.equals("[")) i = lex.pairs()[i];
        }
        String text = decode(addImports(source, List.of("package:flutter/widgets.dart")));
        String nl = text.contains("\r\n") ? "\r\n" : "\n";
        return encode(text + nl + declaration.replace("\n", nl));
    }

    /** Inserts a reviewed typed mutable field and update method, never arbitrary member source. */
    public static byte[] insertStateBinding(byte[] source, String ownerClass, WidgetNode widget) {
        return insertStateBinding(source, ownerClass, widget, "", false);
    }

    public static byte[] insertStateBinding(byte[] source, String ownerClass, WidgetNode widget,
            String initialText, boolean reuseExistingField) {
        StateBinding binding = widget.stateBinding().orElseThrow(() -> unsafe("The widget has no typed state binding."));
        var descriptor = WidgetStateBindingCatalog.find(widget)
                .orElseThrow(() -> unsafe("The widget has no reviewed state-binding contract."));
        WidgetStateBindingCatalog.validationError(widget).ifPresent(error -> { throw unsafe(error); });
        Inspection target = inspect(source, ownerClass);
        requireStateBindingNamesAvailable(source, ownerClass, binding, reuseExistingField);
        if (reuseExistingField) requireStateBindingFields(source, ownerClass, List.of(binding));
        List<PropertyValue> values = descriptor.previewProperties().stream()
                .map(name -> widget.properties().getOrDefault(name, new PropertyValue.NullValue())).toList();
        List<String> imports = new ArrayList<>();
        binding.referenceType().flatMap(PropertyValue.DartObjectReferenceValue::libraryUri).ifPresent(imports::add);
        String initializer;
        if (binding.type() == StateBinding.Type.TEXT_CONTROLLER) {
            imports.add("package:flutter/widgets.dart");
            initializer = "TextEditingController(text: " + stateLiteral(new PropertyValue.StringValue(initialText), imports) + ")";
        } else if (binding.action() == StateBinding.Action.SELECT) {
            boolean selected = new PropertyValue.BooleanValue(true).equals(
                    widget.properties().get(new dev.flutter.netbeans.designer.model.PropertyName("selected")));
            initializer = selected ? stateLiteral(binding.selectedValue().orElseThrow(), imports) : "null";
        } else if (binding.type() == StateBinding.Type.RANGE_VALUES) {
            imports.add("package:flutter/material.dart");
            if (values.size() != 2) throw unsafe("Range state requires both reviewed endpoint literals.");
            initializer = "RangeValues(" + stateLiteral(values.get(0), imports) + ", "
                    + stateLiteral(values.get(1), imports) + ")";
        } else {
            if (values.size() != 1) throw unsafe("Scalar state requires one reviewed preview literal.");
            PropertyValue initial = values.getFirst();
            if (initial instanceof PropertyValue.NullValue && binding.type() == StateBinding.Type.BOOL
                    && binding.action() == StateBinding.Action.TOGGLE) initial = new PropertyValue.BooleanValue(false);
            initializer = stateLiteral(initial, imports);
        }
        String field = (binding.type() == StateBinding.Type.TEXT_CONTROLLER ? "final " : "")
                + stateFieldType(binding) + " " + binding.fieldName() + " = " + initializer + ";";
        byte[] withField = reuseExistingField ? source.clone() : insertMember(target, field);
        if (binding.type() == StateBinding.Type.TEXT_CONTROLLER && !reuseExistingField) {
            withField = insertControllerLifecycle(withField, ownerClass, binding.fieldName());
        }
        String guard = binding.type() == StateBinding.Type.BOOL && descriptor.callbackParameterType().equals("bool?")
                ? "  if (value == null) return;\n" : "";
        String parameters = descriptor.callbackParameterType().isEmpty() ? "" : descriptor.callbackParameterType() + " value";
        String assignment = switch (binding.action()) {
            case CHANGE -> "value";
            case TOGGLE -> "!" + binding.fieldName();
            case SELECT -> stateLiteral(binding.selectedValue().orElseThrow(), imports);
        };
        String body = binding.type() == StateBinding.Type.TEXT_CONTROLLER
                ? "  // Handle user text changes here; the controller listener rebuilds dependent values.\n"
                : guard + "  setState(() {\n    " + binding.fieldName() + " = " + assignment + ";\n  });\n";
        String method = "void " + binding.handlerName() + "(" + parameters + ") {\n" + body + "}";
        byte[] result = addImports(insert(withField, ownerClass, binding.handlerName(), method), imports.stream().distinct().toList());
        requireStateBindingFields(result, ownerClass, List.of(binding));
        return result;
    }

    /** Checks current generated references as well as user members before new State names are introduced. */
    public static void requireStateBindingNamesAvailable(byte[] source, String ownerClass, StateBinding binding) {
        requireStateBindingNamesAvailable(source, ownerClass, binding, false);
    }

    public static void requireStateBindingNamesAvailable(byte[] source, String ownerClass, StateBinding binding,
            boolean reuseExistingField) {
        Objects.requireNonNull(binding, "binding");
        Inspection target = inspect(source, ownerClass);
        if (!reuseExistingField) requireUnusedStateName(target, binding.fieldName());
        requireUnusedStateName(target, binding.handlerName());
        if (!reuseExistingField && binding.type() == StateBinding.Type.TEXT_CONTROLLER) {
            requireUnusedStateName(target, controllerListenerName(binding.fieldName()));
            if (binding.handlerName().equals(controllerListenerName(binding.fieldName()))) {
                throw unsafe("The event handler name is reserved for this controller's State listener.");
            }
        }
    }

    /** Verifies direct initialized mutable State fields; getters, inherited/static/final fields are not admitted. */
    public static void requireStateBindingFields(byte[] source, String ownerClass, List<StateBinding> bindings) {
        if (bindings.isEmpty()) return;
        Inspection target = inspect(source, ownerClass);
        for (StateBinding binding : bindings) {
            requireField(target, binding.fieldName(), binding.type(), binding.referenceType());
        }
    }

    /** Consumer fields obey the same exact ownership/type/lifecycle proof as controlled widgets. */
    public static void requirePropertyBindingFields(byte[] source, String ownerClass, List<StatePropertyBinding> bindings) {
        if (bindings.isEmpty()) return;
        Inspection target = inspect(source, ownerClass);
        for (StatePropertyBinding binding : bindings) requireField(target, binding.fieldName(), binding.type(), binding.referenceType());
    }

    private static Member requireField(Inspection target, String fieldName, StateBinding.Type fieldType,
            java.util.Optional<PropertyValue.DartObjectReferenceValue> referenceType) {
            String type = stateFieldType(fieldType, referenceType);
            List<String> expected = new ArrayList<>();
            if (fieldType == StateBinding.Type.TEXT_CONTROLLER) expected.add("final");
            expected.add(type.replace("?", ""));
            if (type.endsWith("?")) expected.add("?");
            expected.add(fieldName);
            expected.add("=");
            int matches = 0;
            Member found = null;
            for (Member member : target.members()) {
                if (member.end() - member.start() <= expected.size()
                        || !value(target.lex(), member.end() - 1).equals(";")) continue;
                boolean header = true;
                for (int index = 0; index < expected.size(); index++) {
                    header &= value(target.lex(), member.start() + index).equals(expected.get(index));
                }
                if (!header) continue;
                if (fieldType == StateBinding.Type.TEXT_CONTROLLER
                        && !ownsControllerConstruction(target.lex(), member.start() + expected.size(), member.end() - 1)) continue;
                int start = target.lex().tokens().get(member.start()).start();
                int end = target.lex().tokens().get(member.end() - 1).end();
                if (target.lex().managed().stream().anyMatch(span -> span.overlaps(start, end))) continue;
                boolean multiple = false;
                for (int index = member.start() + expected.size(); index < member.end(); index++) {
                    String token = value(target.lex(), index);
                    if (token.equals(",")) multiple = true;
                    if (Set.of("(", "[", "{").contains(token)) index = target.lex().pairs()[index];
                }
                if (!multiple) { matches++; found = member; }
            }
            if (matches != 1) {
                throw unsafe("State field '" + fieldName + "' must be one direct initialized "
                        + (fieldType == StateBinding.Type.TEXT_CONTROLLER ? "final " : "mutable ") + type + " field in the verified owner.");
            }
            if (fieldType == StateBinding.Type.TEXT_CONTROLLER) requireControllerLifecycle(target, fieldName);
            return found;
    }

    /** Exact UTF-16 declaration-name location, after ownership/type/lifecycle validation. */
    public static int stateFieldNameOffset(byte[] source, String ownerClass, StatePropertyBinding field) {
        Objects.requireNonNull(field, "field");
        Inspection target = inspect(source, ownerClass);
        Member member = requireField(target, field.fieldName(), field.type(), field.referenceType());
        return target.lex().tokens().get(fieldNameToken(member, field)).start();
    }

    /** Checks the complete current candidate, including current generated references, before source projection. */
    public static void requireStateFieldRenameNamesAvailable(byte[] source, String ownerClass,
            StatePropertyBinding field, String newName) {
        requireStateFieldRenameNamesAvailable(source, ownerClass, field, newName, false);
    }

    private static void requireStateFieldRenameNamesAvailable(byte[] source, String ownerClass,
            StatePropertyBinding field, String newName, boolean replay) {
        renamedField(field, newName);
        Inspection target = inspect(source, ownerClass);
        requireSingleFileFieldOwner(target.lex());
        requireField(target, field.fieldName(), field.type(), field.referenceType());
        if (field.fieldName().equals(newName)) return;
        requireUnusedFieldRenameName(target, newName, replay);
        if (field.type() == StateBinding.Type.TEXT_CONTROLLER) {
            requireUnusedFieldRenameName(target, controllerListenerName(newName), replay);
        }
    }

    private static void requireUnusedFieldRenameName(Inspection target, String name, boolean replay) {
        if (target.lex().interpolatedNames().contains(name)) {
            throw unsafe("State rename name '" + name + "' already occurs in a source interpolation.");
        }
        for (Token token : target.lex().tokens()) {
            if (token.identifier() && token.value().equals(name) && !(replay && target.lex().managed(token.start()))) {
                throw unsafe("State rename name '" + name + "' already occurs in this library and could capture an extension or private reference.");
            }
        }
    }

    /**
     * Renames one proved State field and its owned controller listener, if any.
     * Managed references are regenerated from metadata by the same analyzed command.
     * Ambiguous references are refused; this is not a guessed project-wide refactoring.
     */
    public static byte[] renameStateField(byte[] source, String ownerClass, StatePropertyBinding field, String newName) {
        return renameStateField(source, ownerClass, field, newName, false);
    }

    /**
     * Pure helper only for replay of an already authored typed source-projection
     * step. This grants no write/admission authority. Its caller must retain that
     * exact proof and owner; an observed edit alone cannot authorize this path.
     * Only destination tokens in managed payloads are ignored, because historical
     * payloads are regenerated separately. All unmanaged collision, ownership,
     * lifecycle, part-library and ambiguous-reference guards remain in force.
     */
    public static byte[] replayStateFieldRename(byte[] source, String ownerClass, StatePropertyBinding field, String newName) {
        return renameStateField(source, ownerClass, field, newName, true);
    }

    private static byte[] renameStateField(byte[] source, String ownerClass, StatePropertyBinding field, String newName, boolean replay) {
        StatePropertyBinding renamed = renamedField(field, newName);
        requireStateFieldRenameNamesAvailable(source, ownerClass, field, newName, replay);
        if (field.fieldName().equals(newName)) return source.clone();
        Inspection target = inspect(source, ownerClass);
        Lexed lex = target.lex();
        Member member = requireField(target, field.fieldName(), field.type(), field.referenceType());
        int declaration = lex.tokens().get(fieldNameToken(member, field)).start();
        String oldListener = field.type() == StateBinding.Type.TEXT_CONTROLLER ? controllerListenerName(field.fieldName()) : null;
        Method listener = oldListener == null ? null : target.methods().stream()
                .filter(method -> method.name().equals(oldListener)).findFirst().orElseThrow();
        if (lex.interpolatedNames().contains(field.fieldName())
                || oldListener != null && lex.interpolatedNames().contains(oldListener)) {
            throw unsafe("The State field or listener is referenced by string interpolation; rename it in Source first.");
        }
        boolean[] bindings = fieldBindingPositions(lex);
        List<Token> replacements = new ArrayList<>();
        for (int index = 0; index < lex.tokens().size(); index++) {
            Token token = lex.tokens().get(index);
            if (!token.identifier() || !(token.value().equals(field.fieldName()) || token.value().equals(oldListener))
                    || lex.managed(token.start())) continue;
            if (isSymbolIdentifier(lex, index)) {
                throw unsafe("State member '" + token.value() + "' occurs in a Symbol literal; its value cannot be changed by field rename.");
            }
            if (token.start() == declaration || listener != null && token.start() == listener.nameStart()) {
                replacements.add(token);
                continue;
            }
            if (index <= target.open() || index >= target.close()) {
                throw unsafe("State member '" + token.value() + "' occurs outside its verified owner; use Dart source refactoring.");
            }
            String previous = value(lex, index - 1);
            if (previous.equals(".")) {
                if (!value(lex, index - 2).equals("this")) {
                    throw unsafe("State member '" + token.value() + "' uses an unresolved receiver; use Dart source refactoring.");
                }
            } else {
                boolean inHeader = target.methods().stream().anyMatch(method -> token.start() >= method.declarationStart()
                        && token.start() < method.bodyStart());
                if (inHeader || bindings[index] || value(lex, index + 1).equals(":") || ambiguousTypedLocal(lex, index)
                        || localFunctionDeclarationAt(lex, index)
                        || token.value().equals(field.fieldName()) && value(lex, index + 1).equals("(")
                        || lex.tokens().get(index - 1).identifier()
                                && !Set.of("return", "await", "throw", "yield", "case", "else", "do").contains(previous)) {
                    throw unsafe("State member '" + token.value() + "' has an ambiguous local declaration or pattern; rename it in Source first.");
                }
            }
            replacements.add(token);
        }
        StringBuilder text = new StringBuilder(lex.text());
        for (int index = replacements.size() - 1; index >= 0; index--) {
            Token token = replacements.get(index);
            text.replace(token.start(), token.end(), token.value().equals(field.fieldName()) ? newName : controllerListenerName(newName));
        }
        byte[] result = encode(text.toString());
        requirePropertyBindingFields(result, ownerClass, List.of(renamed));
        return result;
    }

    private static boolean isSymbolIdentifier(Lexed lex, int index) {
        int start = index;
        while (start >= 2 && value(lex, start - 1).equals(".") && lex.tokens().get(start - 2).identifier()) start -= 2;
        return value(lex, start - 1).equals("#");
    }

    private static boolean localFunctionDeclarationAt(Lexed lex, int name) {
        int parameters = name + 1;
        if (value(lex, parameters).equals("<")) {
            int depth = 1;
            parameters++;
            while (parameters < lex.tokens().size() && depth > 0) {
                String token = value(lex, parameters++);
                if (Set.of(";", "{", "}", "(").contains(token)) return false;
                if (token.equals("<")) depth++;
                if (token.equals(">")) depth--;
            }
            if (depth != 0) return false;
        }
        return value(lex, parameters).equals("(")
                && Set.of("=>", "{").contains(value(lex, afterAsync(lex, lex.pairs()[parameters] + 1)));
    }

    private static boolean ambiguousTypedLocal(Lexed lex, int name) {
        if (!Set.of("=", ";", ",", "in").contains(value(lex, name + 1))) return false;
        String previous = value(lex, name - 1);
        if (previous.equals(",") && commaMayDeclare(lex, name)) return true;
        if (previous.equals("?") || previous.equals(">")) return true;
        if (!previous.equals(")")) return false;
        int open = lex.pairs()[name - 1];
        // A parenthesized record/function type can declare a local. An ordinary
        // unbraced control body instead writes the existing field.
        return !Set.of("if", "while", "for").contains(value(lex, open - 1));
    }

    private static boolean commaMayDeclare(Lexed lex, int name) {
        for (int index = name - 1; index >= 0; index--) {
            String token = value(lex, index);
            if (Set.of(")", "]", "}").contains(token)) { index = lex.pairs()[index]; continue; }
            if (token.equals("(")) return value(lex, index - 1).equals("for");
            if (token.equals("[")) return false;
            if (token.equals("{") || token.equals(";")) return true;
        }
        return true;
    }

    private static StatePropertyBinding renamedField(StatePropertyBinding field, String newName) {
        Objects.requireNonNull(field, "field");
        return new StatePropertyBinding(newName, field.type(), field.referenceType(), field.transform(), field.comparisonValue());
    }

    private static void requireSingleFileFieldOwner(Lexed lex) {
        for (int index = 0; index < lex.tokens().size(); index++) {
            if (value(lex, index).equals("part") && Set.of("<string>", "of").contains(value(lex, index + 1))) {
                throw unsafe("State field rename requires a single-file library: part directives may hide private references in another source file.");
            }
            if (Set.of("(", "[", "{").contains(value(lex, index))) index = lex.pairs()[index];
        }
    }

    private static int fieldNameToken(Member member, StatePropertyBinding field) {
        return member.start() + (field.type() == StateBinding.Type.TEXT_CONTROLLER ? 1 : 0)
                + 1 + (stateFieldType(field.type(), field.referenceType()).endsWith("?") ? 1 : 0);
    }

    private static boolean[] fieldBindingPositions(Lexed lex) {
        int[] changes = new int[lex.tokens().size() + 1];
        for (int index = 0; index < lex.tokens().size(); index++) {
            String opening = value(lex, index);
            int parameters = index + 1;
            if (opening.equals(">") && value(lex, parameters).equals("(")
                    && Set.of("=>", "{").contains(value(lex, afterAsync(lex, lex.pairs()[parameters] + 1)))) {
                int depth = 1;
                int start = index - 1;
                while (start >= 0 && depth > 0) {
                    String value = value(lex, start);
                    if (Set.of(";", "{", "}").contains(value)) break;
                    if (value.equals(">")) depth++;
                    if (value.equals("<")) depth--;
                    if (depth > 0) start--;
                }
                if (depth == 0) {
                    changes[start + 1]++;
                    changes[index]--;
                }
            }
            if (!Set.of("(", "[", "{").contains(opening)) continue;
            int after = afterAsync(lex, lex.pairs()[index] + 1);
            boolean controlCondition = Set.of("if", "while", "for", "switch").contains(value(lex, index - 1));
            if (Set.of("=", "in", "when").contains(value(lex, after))
                    || opening.equals("(") && !controlCondition && Set.of("=>", "{").contains(value(lex, after))) {
                changes[index + 1]++;
                changes[lex.pairs()[index]]--;
            }
        }
        boolean[] result = new boolean[lex.tokens().size()];
        int active = 0;
        for (int index = 0; index < result.length; index++) {
            active += changes[index];
            result[index] = active > 0;
        }
        return result;
    }

    private static boolean ownsControllerConstruction(Lexed lex, int start, int end) {
        if (!value(lex, start).equals("TextEditingController")) return false;
        int arguments = start + 1;
        if (tokensMatch(lex, arguments, List.of(".", "fromValue"))) arguments += 2;
        return value(lex, arguments).equals("(") && lex.pairs()[arguments] == end - 1;
    }

    private static String stateFieldType(StateBinding binding) {
        return stateFieldType(binding.type(), binding.referenceType());
    }

    private static String stateFieldType(StateBinding.Type type,
            java.util.Optional<PropertyValue.DartObjectReferenceValue> referenceType) {
        return switch (type) {
            case STRING -> "String";
            case INT -> "int";
            case NUM -> "num";
            case TEXT_CONTROLLER -> "TextEditingController";
            case BOOL -> "bool";
            case NULLABLE_BOOL -> "bool?";
            case DOUBLE -> "double";
            case RANGE_VALUES -> "RangeValues";
            case NULLABLE_STRING -> "String?";
            case NULLABLE_INT -> "int?";
            case NULLABLE_DOUBLE -> "double?";
            case NULLABLE_NUM -> "num?";
            case NULLABLE_OBJECT -> "Object?";
            case NULLABLE_REFERENCE -> referenceType.orElseThrow().rootSymbol() + "?";
        };
    }

    /** Source candidates only: exact direct private initialized fields; the analyzer still decides admission. */
    public static List<StatePropertyBinding> discoverStateFields(byte[] source, String ownerClass) {
        Inspection target = inspect(source, ownerClass);
        List<StatePropertyBinding> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Member member : target.members()) {
            int cursor = member.start();
            if (value(target.lex(), cursor).equals("final")) cursor++;
            String type = value(target.lex(), cursor++);
            if (value(target.lex(), cursor).equals("?")) { type += "?"; cursor++; }
            String name = value(target.lex(), cursor++);
            if (!name.matches("_[A-Za-z][A-Za-z0-9_]*") || !value(target.lex(), cursor).equals("=")) continue;
            StateBinding.Type closed = switch (type) {
                case "bool" -> StateBinding.Type.BOOL;
                case "bool?" -> StateBinding.Type.NULLABLE_BOOL;
                case "String" -> StateBinding.Type.STRING;
                case "String?" -> StateBinding.Type.NULLABLE_STRING;
                case "int" -> StateBinding.Type.INT;
                case "int?" -> StateBinding.Type.NULLABLE_INT;
                case "double" -> StateBinding.Type.DOUBLE;
                case "double?" -> StateBinding.Type.NULLABLE_DOUBLE;
                case "num" -> StateBinding.Type.NUM;
                case "num?" -> StateBinding.Type.NULLABLE_NUM;
                case "Object?" -> StateBinding.Type.NULLABLE_OBJECT;
                case "RangeValues" -> StateBinding.Type.RANGE_VALUES;
                case "TextEditingController" -> StateBinding.Type.TEXT_CONTROLLER;
                default -> null;
            };
            if (closed == null || !names.add(name)) continue;
            try {
                requireField(target, name, closed, java.util.Optional.empty());
                result.add(new StatePropertyBinding(name, closed, java.util.Optional.empty(),
                        StatePropertyBinding.Transform.DIRECT, java.util.Optional.empty()));
            } catch (IllegalArgumentException unavailable) {
                // Non-mutable, complex or unproved lifecycle fields are not offered as owned candidates.
            }
            if (result.size() > 256) throw unsafe("The State field discovery limit of 256 was exceeded.");
        }
        return List.copyOf(result);
    }

    private static String controllerListenerName(String fieldName) { return fieldName + "StateListener"; }

    private static byte[] insertControllerLifecycle(byte[] source, String ownerClass, String fieldName) {
        String listener = controllerListenerName(fieldName);
        byte[] result = insert(source, ownerClass, listener,
                "void " + listener + "() {\n  setState(() {});\n}");
        result = insertLifecycleStatement(result, ownerClass, "initState", fieldName + ".addListener(" + listener + ");");
        return insertLifecycleStatement(result, ownerClass, "dispose",
                fieldName + ".removeListener(" + listener + ");\n" + fieldName + ".dispose();");
    }

    /** Inserts into one proven synchronous lifecycle block without replacing any existing body byte. */
    private static byte[] insertLifecycleStatement(byte[] source, String ownerClass, String name, String statements) {
        Inspection target = inspect(source, ownerClass);
        Method method = target.methods().stream().filter(candidate -> candidate.name().equals(name)).findFirst().orElse(null);
        if (method == null) {
            String body = name.equals("initState") ? "  super.initState();\n  " + statements.replace("\n", "\n  ")
                    : "  " + statements.replace("\n", "\n  ") + "\n  super.dispose();";
            return insert(source, ownerClass, name, "@override\nvoid " + name + "() {\n" + body + "\n}");
        }
        int[] body = lifecycleBody(target, method, name);
        int insertion = name.equals("initState") ? target.lex().tokens().get(body[0] + 6).end()
                : target.lex().tokens().get(body[0]).end();
        String ending = target.lex().text().contains("\r\n") ? "\r\n" : "\n";
        String added = ending + "    " + statements.replace("\n", ending + "    ") + ending;
        return encode(target.lex().text().substring(0, insertion) + added + target.lex().text().substring(insertion));
    }

    private static int[] lifecycleBody(Inspection target, Method method, String name) {
        Lexed lex = target.lex();
        if (method.generated() || method.isStatic() || !method.returnType().equals("void")
                || !method.parameters().isBlank() || !method.hasBody()) {
            throw unsafe("Controller ownership needs one direct synchronous void " + name + "() lifecycle method.");
        }
        int open = tokenAt(lex, method.bodyStart());
        if (!value(lex, open).equals("{") || !value(lex, open - 1).equals(")")) {
            throw unsafe("Merge controller lifecycle into " + name + " in Source first: a synchronous block body is required.");
        }
        int close = lex.pairs()[open];
        int call = name.equals("initState") ? open + 1 : close - 6;
        if (!tokensMatch(lex, call, List.of("super", ".", name, "(", ")", ";"))) {
            throw unsafe("Controller lifecycle requires super." + name + "(); "
                    + (name.equals("initState") ? "first" : "last") + " in its block. Preserve custom logic and adjust this in Source first.");
        }
        int calls = 0;
        for (int index = open + 1; index < close; index++) {
            if (tokensMatch(lex, index, List.of("super", ".", name, "("))) calls++;
        }
        if (calls != 1) throw unsafe("Controller lifecycle requires exactly one inherited " + name + " call.");
        return new int[] {open, close};
    }

    private static void requireControllerLifecycle(Inspection target, String fieldName) {
        String listener = controllerListenerName(fieldName);
        Method init = target.methods().stream().filter(method -> method.name().equals("initState")).findFirst()
                .orElseThrow(() -> unsafe("Controller '" + fieldName + "' has no owned initState listener registration."));
        Method dispose = target.methods().stream().filter(method -> method.name().equals("dispose")).findFirst()
                .orElseThrow(() -> unsafe("Controller '" + fieldName + "' has no owned dispose cleanup."));
        int[] initBody = lifecycleBody(target, init, "initState");
        int[] disposeBody = lifecycleBody(target, dispose, "dispose");
        requireUnshadowedLifecycleNames(target.lex(), initBody, Set.of(fieldName, listener));
        requireUnshadowedLifecycleNames(target.lex(), disposeBody, Set.of(fieldName, listener));
        requireDirectCall(target.lex(), initBody, List.of(fieldName, ".", "addListener", "(", listener, ")", ";"));
        requireDirectCall(target.lex(), disposeBody, List.of(fieldName, ".", "removeListener", "(", listener, ")", ";"));
        requireDirectCall(target.lex(), disposeBody, List.of(fieldName, ".", "dispose", "(", ")", ";"));
        Method callback = target.methods().stream().filter(method -> method.name().equals(listener)).findFirst()
                .orElseThrow(() -> unsafe("Controller '" + fieldName + "' has no owned State listener."));
        if (callback.isStatic() || callback.generated() || !callback.returnType().equals("void")
                || !callback.parameters().isBlank() || !callback.hasBody()
                || !value(target.lex(), tokenAt(target.lex(), callback.bodyStart()) - 1).equals(")")) {
            throw unsafe("Controller State listener must be one direct concrete void instance method.");
        }
    }

    private static void requireUnshadowedLifecycleNames(Lexed lex, int[] body, Set<String> names) {
        boolean[] bindings = bindingPositions(lex);
        for (int index = body[0] + 1; index < body[1]; index++) {
            if (!names.contains(value(lex, index)) || value(lex, index - 1).equals(".")) continue;
            String previous = value(lex, index - 1);
            if (bindings[index] || Set.of("=", "=>", ":").contains(value(lex, index + 1))
                    || lex.tokens().get(index - 1).identifier()
                            && !Set.of("return", "await", "throw", "yield").contains(previous)) {
                throw unsafe("Controller lifecycle has an ambiguous local declaration of '" + value(lex, index)
                        + "'. Rename that local in Source before using owned State bindings.");
            }
        }
    }

    private static void requireDirectCall(Lexed lex, int[] body, List<String> call) {
        int direct = 0;
        int all = 0;
        int firstCall = -1;
        for (int index = body[0] + 1; index < body[1]; index++) if (tokensMatch(lex, index, call)) all++;
        for (int index = body[0] + 1; index < body[1]; index++) {
            if (tokensMatch(lex, index, call) && (index == body[0] + 1
                    || Set.of(";", "}").contains(value(lex, index - 1)))) {
                direct++;
                firstCall = index;
            }
            if (Set.of("(", "[", "{").contains(value(lex, index))) index = lex.pairs()[index];
        }
        if (direct != 1 || all != 1) throw unsafe("Controller lifecycle needs exactly one unconditional " + String.join("", call) + " call.");
        for (int index = body[0] + 1; index < firstCall; index++) {
            if (Set.of("return", "throw").contains(value(lex, index))) {
                throw unsafe("Controller lifecycle cannot exit before " + String.join("", call) + ".");
            }
        }
    }

    private static int tokenAt(Lexed lex, int start) {
        for (int index = 0; index < lex.tokens().size(); index++) if (lex.tokens().get(index).start() == start) return index;
        throw unsafe("The exact lifecycle token span is unavailable.");
    }

    private static boolean tokensMatch(Lexed lex, int start, List<String> expected) {
        if (start < 0 || start + expected.size() > lex.tokens().size()) return false;
        for (int index = 0; index < expected.size(); index++) if (!value(lex, start + index).equals(expected.get(index))) return false;
        return true;
    }

    private static String stateLiteral(PropertyValue value, List<String> imports) {
        if (value instanceof PropertyValue.NullValue) return "null";
        if (value instanceof PropertyValue.BooleanValue bool) return Boolean.toString(bool.value());
        if (value instanceof PropertyValue.IntegerValue integer) return integer.value().toString();
        if (value instanceof PropertyValue.DoubleValue number) {
            String literal = number.value().toPlainString();
            return literal.contains(".") ? literal : literal + ".0";
        }
        if (value instanceof PropertyValue.EnumValue number && number.type().equals("double")) {
            return switch (number.value()) {
                case "infinity" -> "(1.0 / 0.0)";
                case "negativeInfinity" -> "(-1.0 / 0.0)";
                case "nan" -> "(0.0 / 0.0)";
                default -> throw unsafe("Unsupported nonfinite State initializer: " + number.value());
            };
        }
        if (value instanceof PropertyValue.StringValue string) {
            StringBuilder literal = new StringBuilder("'");
            for (int index = 0; index < string.value().length(); index++) {
                char character = string.value().charAt(index);
                if (character == '\\' || character == '\'' || character == '$') literal.append('\\').append(character);
                else if (character < 32 || character == 127 || character == '\u2028' || character == '\u2029')
                    literal.append("\\u").append(String.format(java.util.Locale.ROOT, "%04x", (int) character));
                else literal.append(character);
            }
            return literal.append('\'').toString();
        }
        if (value instanceof PropertyValue.DartObjectReferenceValue reference) {
            reference.libraryUri().ifPresent(imports::add);
            return (reference.constant().orElse(false) ? "const " : "") + reference.rootSymbol()
                    + reference.member().map(member -> "." + member).orElse("")
                    + (reference.access() == PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION ? "()" : "");
        }
        throw unsafe("State initialization requires a reviewed scalar or object-reference preview value.");
    }

    private static void requireUnusedStateName(Inspection target, String name) {
        requireFreeName(target, name);
        if (target.lex().interpolatedNames().contains(name)) {
            throw unsafe("State member name '" + name + "' already occurs in a source interpolation.");
        }
        for (int index = target.open() + 1; index < target.close(); index++) {
            Token token = target.lex().tokens().get(index);
            if (token.identifier() && token.value().equals(name)) {
                throw unsafe("State member name '" + name + "' already occurs in the owning class and could shadow user code.");
            }
        }
    }

    private static byte[] insertMember(Inspection target, String member) {
        String text = target.lex().text();
        int close = target.lex().tokens().get(target.close()).start();
        if (target.lex().managed(close)) throw unsafe("The owning class closes inside a generated region.");
        int insertion = lineStart(text, close);
        if (!text.substring(insertion, close).isBlank()) insertion = close;
        String ending = text.contains("\r\n") ? "\r\n" : "\n";
        return encode(text.substring(0, insertion) + ending + "  " + member + ending + text.substring(insertion));
    }

    /** Adds bounded plain Dart/package imports without touching managed payloads. */
    public static byte[] addImports(byte[] source, List<String> importUris) {
        Objects.requireNonNull(importUris, "importUris");
        if (importUris.size() > 32) {
            throw unsafe("A handler signature may require at most 32 imports.");
        }
        Lexed lex = new Lexer(decode(source)).scan();
        if (value(lex, 0).equals("part") && value(lex, 1).equals("of")) {
            throw unsafe("A Dart part cannot declare handler signature imports.");
        }
        Set<String> additions = new java.util.LinkedHashSet<>();
        for (String uri : importUris) {
            if (uri == null || uri.length() > 512 || !uri.matches(
                    "(?:dart:[A-Za-z][A-Za-z0-9_]*|package:[A-Za-z_][A-Za-z0-9_]*(?:/[A-Za-z0-9_.-]+)+)")) {
                throw unsafe("Handler imports require a plain Dart or package library URI.");
            }
            boolean present = false;
            for (int i = 0; i + 2 < lex.tokens().size(); i++) {
                if (value(lex, i).equals("import") && value(lex, i + 1).equals("<string>")
                        && value(lex, i + 2).equals(";")
                        && !lex.managed(lex.tokens().get(i).start())) {
                    Token literal = lex.tokens().get(i + 1);
                    String written = lex.text().substring(literal.start(), literal.end());
                    if (written.equals("'" + uri + "'") || written.equals("\"" + uri + "\"")) {
                        present = true;
                        break;
                    }
                }
            }
            if (!present) {
                additions.add(uri);
            }
        }
        if (additions.isEmpty()) {
            return source.clone();
        }
        int insertion = lex.text().startsWith("\uFEFF") ? 1 : 0;
        if (value(lex, 0).equals("library")) {
            int end = 1;
            while (end < lex.tokens().size() && !value(lex, end).equals(";")) {
                end++;
            }
            if (end == lex.tokens().size()) {
                throw unsafe("Unterminated Dart library directive.");
            }
            insertion = lex.tokens().get(end).end();
        } else if (value(lex, 0).equals("@")) {
            // Annotated library directives need a resolver; do not insert before metadata.
            for (Token token : lex.tokens()) {
                if (token.value().equals("library")) {
                    throw unsafe("Annotated Dart library imports require a Dart source edit.");
                }
            }
        }
        // In a form without a leading user comment, offset zero is also the
        // native imports guard's start. Keep retained imports outside that
        // guard instead of asking the live editor to insert at its boundary.
        boolean afterInitialImportsGuard = false;
        for (Span span : lex.managed()) {
            if (span.start() == insertion && lex.text().startsWith(OPEN_MARKER + "imports\">", span.start())) {
                insertion = span.end();
                if (insertion < lex.text().length() && lex.text().charAt(insertion) == '\r') insertion++;
                if (insertion < lex.text().length() && lex.text().charAt(insertion) == '\n') insertion++;
                afterInitialImportsGuard = true;
                break;
            }
        }
        String lineEnding = lex.text().contains("\r\n") ? "\r\n" : "\n";
        StringBuilder imports = new StringBuilder();
        if (insertion > 0 && !afterInitialImportsGuard) {
            imports.append(lineEnding);
        }
        additions.forEach(uri -> imports.append("import '").append(uri).append("';").append(lineEnding));
        return encode(lex.text().substring(0, insertion) + imports + lex.text().substring(insertion));
    }

    /**
     * Renames a user method and lexically unambiguous references within its
     * owner. Generated regions are untouched; regenerate their model bindings
     * in the same transaction. References through another receiver, outside the
     * owner, in interpolation, or shadowed by a local declaration are rejected.
     */
    public static byte[] rename(byte[] source, String ownerClass,
            String oldName, String newName) {
        requireIdentifier(oldName);
        requireIdentifier(newName);
        Inspection target = inspect(source, ownerClass);
        List<Method> matches = target.methods().stream()
                .filter(method -> method.name().equals(oldName)).toList();
        if (matches.size() != 1 || matches.getFirst().generated()) {
            throw unsafe("Rename requires one user-owned method named '" + oldName + "'.");
        }
        if (oldName.equals(newName)) {
            return source.clone();
        }
        requireFreeName(target, newName);
        Lexed lex = target.lex();
        for (int i = target.open() + 1; i < target.close(); i++) {
            if (value(lex, i).equals(newName) && !lex.managed(lex.tokens().get(i).start())) {
                throw unsafe("New handler name occurs in the owning class and could shadow a binding.");
            }
        }
        if (lex.interpolatedNames().contains(oldName)) {
            throw unsafe("Handler is referenced by string interpolation; use Dart symbol rename.");
        }
        Method method = matches.getFirst();
        if (lex.tokens().stream().anyMatch(token -> token.start() >= method.declarationStart()
                && token.start() < method.nameStart() && token.value().equals("override"))) {
            throw unsafe("An overriding method requires Dart symbol rename.");
        }
        boolean[] bindingPositions = bindingPositions(lex);
        List<Token> replacements = new ArrayList<>();
        for (int i = 0; i < lex.tokens().size(); i++) {
            Token token = lex.tokens().get(i);
            if (!token.identifier() || !token.value().equals(oldName) || lex.managed(token.start())) {
                continue;
            }
            if (token.start() == method.nameStart()) {
                replacements.add(token);
                continue;
            }
            if (i <= target.open() || i >= target.close()) {
                throw unsafe("Handler name occurs outside its owning class; use Dart symbol rename.");
            }
            String previous = value(lex, i - 1);
            String next = value(lex, i + 1);
            if (previous.equals(".")) {
                if (!value(lex, i - 2).equals("this")) {
                    throw unsafe("Handler name uses an unresolved receiver; use Dart symbol rename.");
                }
            } else {
                boolean inHeader = target.methods().stream().anyMatch(candidate ->
                        token.start() >= candidate.declarationStart()
                                && token.start() < candidate.bodyStart());
                if (inHeader || bindingPositions[i] || next.equals(":")
                        || next.equals("=") || next.equals("=>")
                        || (i > 0 && lex.tokens().get(i - 1).identifier()
                        && !Set.of("return", "await", "throw", "yield").contains(previous))
                        || !Set.of("(", "<", ";", ",", ")", "]", "}", "?", ".").contains(next)) {
                    throw unsafe("Handler name has an ambiguous local binding; use Dart symbol rename.");
                }
            }
            replacements.add(token);
        }
        StringBuilder result = new StringBuilder(lex.text());
        for (int i = replacements.size() - 1; i >= 0; i--) {
            Token replacement = replacements.get(i);
            result.replace(replacement.start(), replacement.end(), newName);
        }
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Converts a UTF-16 offset to a UTF-8 byte offset; split surrogates are rejected. */
    public static int byteOffset(byte[] source, int utf16Offset) {
        String text = decode(source);
        if (utf16Offset < 0 || utf16Offset > text.length()
                || (utf16Offset > 0 && utf16Offset < text.length()
                && Character.isHighSurrogate(text.charAt(utf16Offset - 1))
                && Character.isLowSurrogate(text.charAt(utf16Offset)))) {
            throw unsafe("Invalid UTF-16 source offset.");
        }
        return text.substring(0, utf16Offset).getBytes(StandardCharsets.UTF_8).length;
    }

    private static Inspection inspect(byte[] source, String ownerClass) {
        requireIdentifier(ownerClass);
        Lexed lex = new Lexer(decode(source)).scan();
        int open = -1;
        int close = -1;
        for (int i = 0; i < lex.tokens().size(); i++) {
            if (value(lex, i).equals("class") && value(lex, i + 1).equals(ownerClass)) {
                if (open != -1) {
                    throw unsafe("Owning class '" + ownerClass + "' is declared more than once.");
                }
                int cursor = i + 2;
                while (cursor < lex.tokens().size() && !value(lex, cursor).equals("{")
                        && !value(lex, cursor).equals(";") && !value(lex, cursor).equals("=")) {
                    cursor++;
                }
                if (!value(lex, cursor).equals("{")) {
                    throw unsafe("Owning class must have a concrete class body.");
                }
                open = cursor;
                close = lex.pairs()[open];
            }
            String token = value(lex, i);
            if (token.equals("{") || token.equals("(") || token.equals("[")) {
                i = lex.pairs()[i];
            }
        }
        if (open == -1) {
            throw unsafe("Top-level owning class '" + ownerClass + "' was not found.");
        }
        List<Member> members = splitMembers(lex, open, close);
        List<Method> methods = new ArrayList<>();
        for (Member member : members) {
            Method method = method(lex, member, ownerClass);
            if (method != null) {
                methods.add(method);
            }
        }
        Set<String> names = new HashSet<>();
        for (Method method : methods) {
            if (!names.add(method.name())) {
                throw unsafe("Ambiguous duplicate method '" + method.name() + "'.");
            }
        }
        return new Inspection(lex, open, close, members, List.copyOf(methods));
    }

    private static List<Member> splitMembers(Lexed lex, int open, int close) {
        List<Member> members = new ArrayList<>();
        int start = open + 1;
        while (start < close) {
            int cursor = start;
            boolean expression = false;
            boolean constructorInitializers = false;
            while (cursor < close) {
                String token = value(lex, cursor);
                if (token.equals(";") && !expression) {
                    cursor++;
                    break;
                }
                if (token.equals("=") || token.equals("=>")) {
                    expression = true;
                }
                if (token.equals(":") && !expression) {
                    constructorInitializers = true;
                }
                if (token.equals("{") && constructorInitializers && expression) {
                    // A closure/map initializer and a constructor body can have
                    // the same lexical shape. Never consume following members
                    // while trying to guess the initializer expression boundary.
                    throw unsafe("Constructor initializer with a block requires Dart syntax analysis.");
                }
                if (token.equals(";") || (token.equals("{") && !expression)) {
                    cursor = token.equals("{") ? lex.pairs()[cursor] + 1 : cursor + 1;
                    break;
                }
                if (token.equals("(") || token.equals("[") || token.equals("{")) {
                    cursor = lex.pairs()[cursor] + 1;
                } else {
                    cursor++;
                }
            }
            if (cursor == close && !Set.of(";", "}").contains(value(lex, cursor - 1))) {
                throw unsafe("Unterminated class member.");
            }
            members.add(new Member(start, cursor));
            start = cursor;
        }
        return List.copyOf(members);
    }

    private static Method method(Lexed lex, Member member, String ownerClass) {
        int header = skipAnnotations(lex, member.start(), member.end());
        boolean isStatic = false;
        while (header < member.end() && MODIFIERS.contains(value(lex, header))) {
            isStatic |= value(lex, header).equals("static");
            header++;
        }
        int constructorName = value(lex, header).equals("const") ? header + 1 : header;
        if (value(lex, constructorName).equals(ownerClass)
                && Set.of("(", ".").contains(value(lex, constructorName + 1))) {
            return null;
        }
        for (int i = header; i < member.end(); i++) {
            String token = value(lex, i);
            if (Set.of("=", "=>", "{", ";").contains(token)) {
                return null;
            }
            if (!token.equals("(")) {
                continue;
            }
            int parametersEnd = lex.pairs()[i];
            int body = afterAsync(lex, parametersEnd + 1);
            int nameIndex = i - 1;
            if (value(lex, nameIndex).equals(">")) {
                int depth = 1;
                while (--nameIndex >= header && depth > 0) {
                    depth += value(lex, nameIndex).equals(">") ? 1
                            : value(lex, nameIndex).equals("<") ? -1 : 0;
                }
            }
            if (nameIndex < header || !lex.tokens().get(nameIndex).identifier()
                    || !Set.of("{", "=>", ";").contains(value(lex, body))) {
                i = parametersEnd;
                continue;
            }
            String name = value(lex, nameIndex);
            if (RESERVED.contains(name) || name.equals(ownerClass) || value(lex, nameIndex - 1).equals(".")
                    || value(lex, nameIndex - 1).equals("set")
                    || value(lex, nameIndex - 1).equals("operator")) {
                return null;
            }
            Token nameToken = lex.tokens().get(nameIndex);
            int declarationStart = lex.tokens().get(member.start()).start();
            int declarationEnd = lex.tokens().get(member.end() - 1).end();
            int bodyStart = lex.tokens().get(body).start();
            int bodyEnd = value(lex, body).equals(";") ? bodyStart : declarationEnd;
            String returnType = lex.text().substring(lex.tokens().get(header).start(), nameToken.start()).strip();
            return new Method(name, lex.text().substring(lex.tokens().get(header).start(), bodyStart).strip(),
                    returnType.isEmpty() ? "dynamic" : returnType,
                    lex.text().substring(lex.tokens().get(i).end(), lex.tokens().get(parametersEnd).start()),
                    declarationStart, declarationEnd, nameToken.start(), nameToken.end(),
                    lex.tokens().get(i).end(), lex.tokens().get(parametersEnd).start(),
                    bodyStart, bodyEnd, isStatic,
                    lex.managed().stream().anyMatch(span -> span.overlaps(declarationStart, declarationEnd)));
        }
        return null;
    }

    private static int skipAnnotations(Lexed lex, int start, int end) {
        int cursor = start;
        while (cursor < end && value(lex, cursor).equals("@")) {
            cursor += 2;
            while (value(lex, cursor).equals(".")) {
                cursor += 2;
            }
            if (value(lex, cursor).equals("(")) {
                cursor = lex.pairs()[cursor] + 1;
            }
        }
        return cursor;
    }

    private static int afterAsync(Lexed lex, int start) {
        if (value(lex, start).equals("async") || value(lex, start).equals("sync")) {
            start++;
            if (value(lex, start).equals("*")) {
                start++;
            }
        }
        return start;
    }

    private static boolean[] bindingPositions(Lexed lex) {
        int[] changes = new int[lex.tokens().size() + 1];
        for (int i = 0; i < lex.tokens().size(); i++) {
            String opening = value(lex, i);
            if (Set.of("(", "[", "{").contains(opening)) {
                int after = afterAsync(lex, lex.pairs()[i] + 1);
                if (Set.of("=", "in", "when").contains(value(lex, after))
                        || (opening.equals("(") && Set.of("=>", "{").contains(value(lex, after)))) {
                    changes[i + 1]++;
                    changes[lex.pairs()[i]]--;
                }
            }
        }
        boolean[] result = new boolean[lex.tokens().size()];
        int active = 0;
        for (int i = 0; i < result.length; i++) {
            active += changes[i];
            result[i] = active > 0;
        }
        return result;
    }

    private static void requireFreeName(Inspection inspection, String name) {
        for (Member member : inspection.members()) {
            for (int i = member.start(); i < member.end(); i++) {
                String token = value(inspection.lex(), i);
                if (Set.of("=", "=>", "{", ";").contains(token)) {
                    break;
                }
                if (token.equals(name)) {
                    throw unsafe("Class member name '" + name + "' is already in use.");
                }
                if (token.equals("(") || token.equals("[")) {
                    i = inspection.lex().pairs()[i];
                }
            }
        }
    }

    private static String decode(byte[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length > MAX_SOURCE_BYTES) {
            throw unsafe("Dart source exceeds the event editor's 16 MiB limit.");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(source)).toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw unsafe("Dart source must be valid UTF-8.");
        }
    }

    private static byte[] encode(String source) {
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(source));
            byte[] encoded = new byte[buffer.remaining()];
            buffer.get(encoded);
            return encoded;
        } catch (CharacterCodingException invalidUnicode) {
            throw unsafe("Handler source contains an unpaired Unicode surrogate.");
        }
    }

    private static void requireIdentifier(String name) {
        if (name == null || name.isEmpty() || RESERVED.contains(name)
                || !identifierStart(name.charAt(0))) {
            throw unsafe("Invalid Dart identifier: " + name);
        }
        for (int i = 1; i < name.length(); i++) {
            if (!identifierPart(name.charAt(i))) {
                throw unsafe("Invalid Dart identifier: " + name);
            }
        }
    }

    private static boolean identifierStart(char value) {
        return value == '_' || value == '$' || (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z');
    }

    private static boolean identifierPart(char value) {
        return identifierStart(value) || (value >= '0' && value <= '9');
    }

    private static String value(Lexed lex, int index) {
        return index < 0 || index >= lex.tokens().size() ? "" : lex.tokens().get(index).value();
    }

    private static int lineStart(String source, int offset) {
        int start = offset;
        while (start > 0 && source.charAt(start - 1) != '\n' && source.charAt(start - 1) != '\r') {
            start--;
        }
        return start;
    }

    private static IllegalArgumentException unsafe(String message) {
        return new IllegalArgumentException(message);
    }

    private record Token(String value, int start, int end, boolean identifier) {
    }

    private record Span(int start, int end) {
        boolean overlaps(int from, int to) {
            return start < to && end > from;
        }
    }

    private record Member(int start, int end) {
    }

    private record Lexed(String text, List<Token> tokens, int[] pairs,
            List<Span> managed, Set<String> interpolatedNames) {
        boolean managed(int offset) {
            return managed.stream().anyMatch(span -> offset >= span.start() && offset < span.end());
        }
    }

    private record Inspection(Lexed lex, int open, int close,
            List<Member> members, List<Method> methods) {
    }

    private static final class Lexer {
        private final String text;
        private final List<Token> tokens = new ArrayList<>();
        private final List<Span> managed = new ArrayList<>();
        private final Set<String> interpolatedNames = new HashSet<>();
        private int managedStart = -1;

        Lexer(String text) {
            this.text = text;
        }

        Lexed scan() {
            int index = 0;
            while (index < text.length()) {
                if (tokens.size() >= MAX_TOKENS) {
                    throw unsafe("Dart source exceeds the event editor's lexical token limit.");
                }
                char current = text.charAt(index);
                if (Character.isWhitespace(current) || (index == 0 && current == '\ufeff')) {
                    index++;
                } else if (index == 0 && text.startsWith("#!")) {
                    index = lineEnd(index);
                } else if (text.startsWith("//", index)) {
                    int end = lineEnd(index);
                    marker(index, end);
                    index = end;
                } else if (text.startsWith("/*", index)) {
                    index = comment(index);
                } else if (current == '\'' || current == '"'
                        || ((current == 'r' || current == 'R') && index + 1 < text.length()
                        && (text.charAt(index + 1) == '\'' || text.charAt(index + 1) == '"'))) {
                    int end = string(index, 0);
                    tokens.add(new Token("<string>", index, end, false));
                    index = end;
                } else if (identifierStart(current)) {
                    int end = identifierEnd(index);
                    tokens.add(new Token(text.substring(index, end), index, end, true));
                    index = end;
                } else {
                    int end = text.startsWith("=>", index) ? index + 2 : index + 1;
                    tokens.add(new Token(text.substring(index, end), index, end, false));
                    index = end;
                }
            }
            if (managedStart >= 0) {
                throw unsafe("Unterminated generated region marker.");
            }
            int[] pairs = new int[tokens.size()];
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            for (int i = 0; i < tokens.size(); i++) {
                String value = tokens.get(i).value();
                if (Set.of("(", "[", "{").contains(value)) {
                    if (stack.size() >= MAX_NESTING) {
                        throw unsafe("Dart source exceeds the lexical nesting limit.");
                    }
                    stack.push(i);
                } else if (Set.of(")", "]", "}").contains(value)) {
                    if (stack.isEmpty()) {
                        throw unsafe("Unbalanced Dart source delimiters.");
                    }
                    int opening = stack.pop();
                    if (!"() [] {}".contains(tokens.get(opening).value() + value)) {
                        throw unsafe("Mismatched Dart source delimiters.");
                    }
                    pairs[opening] = i;
                    pairs[i] = opening;
                }
            }
            if (!stack.isEmpty()) {
                throw unsafe("Unbalanced Dart source delimiters.");
            }
            return new Lexed(text, List.copyOf(tokens), pairs,
                    List.copyOf(managed), Set.copyOf(interpolatedNames));
        }

        private void marker(int start, int end) {
            int prefixStart = lineStart(text, start);
            // A BOM is a file prefix, not source preceding the first marker.
            // Do not accept it on later lines or elsewhere inside a prefix.
            if (prefixStart == 0 && !text.isEmpty() && text.charAt(0) == '\ufeff') prefixStart = 1;
            if (!text.substring(prefixStart, start).isBlank()) {
                return;
            }
            String comment = text.substring(start, end).stripTrailing();
            if (comment.startsWith(OPEN_MARKER) && comment.endsWith("\">")) {
                if (managedStart >= 0) {
                    throw unsafe("Nested generated region markers.");
                }
                managedStart = start;
            } else if (comment.equals(CLOSE_MARKER)) {
                if (managedStart < 0) {
                    throw unsafe("Unmatched generated region closing marker.");
                }
                managed.add(new Span(managedStart, end));
                managedStart = -1;
            } else if (comment.startsWith("// <netbeans-flutter-designer")
                    || comment.startsWith("// </netbeans-flutter-designer")) {
                throw unsafe("Malformed generated region marker.");
            }
        }

        private int lineEnd(int start) {
            int end = start;
            while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
                end++;
            }
            return end;
        }

        private int comment(int start) {
            int depth = 1;
            int index = start + 2;
            while (index < text.length()) {
                if (text.startsWith("/*", index)) {
                    if (++depth > MAX_NESTING) {
                        throw unsafe("Dart block comment exceeds the nesting limit.");
                    }
                    index += 2;
                } else if (text.startsWith("*/", index)) {
                    index += 2;
                    if (--depth == 0) {
                        return index;
                    }
                } else {
                    index++;
                }
            }
            throw unsafe("Unterminated Dart block comment.");
        }

        private int string(int start, int depth) {
            if (depth > MAX_NESTING) {
                throw unsafe("Dart string interpolation exceeds the nesting limit.");
            }
            boolean raw = text.charAt(start) == 'r' || text.charAt(start) == 'R';
            int quoteStart = raw ? start + 1 : start;
            char quote = text.charAt(quoteStart);
            boolean triple = quoteStart + 2 < text.length()
                    && text.charAt(quoteStart + 1) == quote && text.charAt(quoteStart + 2) == quote;
            int width = triple ? 3 : 1;
            String delimiter = String.valueOf(quote).repeat(width);
            int index = quoteStart + width;
            while (index < text.length()) {
                if (text.startsWith(delimiter, index)) {
                    return index + width;
                }
                char current = text.charAt(index);
                if (!triple && (current == '\r' || current == '\n')) {
                    throw unsafe("Unterminated single-line Dart string.");
                }
                if (!raw && current == '\\') {
                    index += 2;
                } else if (!raw && text.startsWith("${", index)) {
                    index = interpolation(index + 2, depth + 1);
                } else if (!raw && current == '$' && index + 1 < text.length()
                        && identifierStart(text.charAt(index + 1))) {
                    int end = identifierEnd(index + 1);
                    interpolatedNames.add(text.substring(index + 1, end));
                    index = end;
                } else {
                    index++;
                }
            }
            throw unsafe("Unterminated Dart string literal.");
        }

        private int interpolation(int start, int recursion) {
            int braces = 1;
            int index = start;
            while (index < text.length()) {
                char current = text.charAt(index);
                if (text.startsWith("//", index)) {
                    index = lineEnd(index);
                } else if (text.startsWith("/*", index)) {
                    index = comment(index);
                } else if (current == '\'' || current == '"'
                        || ((current == 'r' || current == 'R') && index + 1 < text.length()
                        && (text.charAt(index + 1) == '\'' || text.charAt(index + 1) == '"'))) {
                    index = string(index, recursion);
                } else if (current == '{') {
                    if (++braces > MAX_NESTING) {
                        throw unsafe("Dart interpolation exceeds the nesting limit.");
                    }
                    index++;
                } else if (current == '}') {
                    if (--braces == 0) {
                        return index + 1;
                    }
                    index++;
                } else if (identifierStart(current)) {
                    int end = identifierEnd(index);
                    interpolatedNames.add(text.substring(index, end));
                    index = end;
                } else {
                    index++;
                }
            }
            throw unsafe("Unterminated Dart string interpolation.");
        }

        private int identifierEnd(int start) {
            int end = start + 1;
            while (end < text.length() && identifierPart(text.charAt(end))) {
                end++;
            }
            return end;
        }
    }
}
