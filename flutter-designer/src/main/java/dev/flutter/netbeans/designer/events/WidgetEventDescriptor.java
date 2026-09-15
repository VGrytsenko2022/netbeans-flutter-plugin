package dev.flutter.netbeans.designer.events;

import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Reviewed callable metadata; builders and predicates are deliberately not events. */
public record WidgetEventDescriptor(
        PropertyName propertyName,
        String callbackType,
        Kind kind,
        Signature signature,
        boolean required,
        boolean allowsExplicitNull,
        boolean sdkRequired,
        boolean nullableCallback,
        boolean defaultEvent,
        Optional<PropertyValue> creationDefault,
        String unsetBehavior,
        List<String> availableVariants,
        List<PropertyName> requiredCompanionProperties) {

    public WidgetEventDescriptor {
        Objects.requireNonNull(propertyName, "propertyName");
        callbackType = requireType(callbackType);
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(creationDefault, "creationDefault");
        Objects.requireNonNull(unsetBehavior, "unsetBehavior");
        availableVariants = List.copyOf(availableVariants);
        requiredCompanionProperties = List.copyOf(requiredCompanionProperties);
        if (defaultEvent && kind != Kind.EVENT) {
            throw new IllegalArgumentException("Only an event can be the default event");
        }
    }

    /** Reviewed source lifecycle actions, separate from native Events classification. */
    public boolean supportsHandlerActions() {
        return kind == Kind.EVENT || (kind == Kind.PREDICATE && callbackType.equals("SelectableDayPredicate"))
                || (kind == Kind.DELEGATE && java.util.Set.of("TransformCallback", "ShaderCallback").contains(callbackType))
                || (kind == Kind.BUILDER && callbackType.equals("ImageErrorWidgetBuilder?"));
    }

    public enum Kind { EVENT, BUILDER, PREDICATE, FORMATTER, DELEGATE }

    public record Parameter(String type, String name, boolean named, boolean required) {
        /** Backward-compatible required positional parameter. */
        public Parameter(String type, String name) {
            this(type, name, false, true);
        }

        public Parameter {
            type = requireType(type);
            name = requireHandlerName(name);
            if (!named && !required) {
                throw new IllegalArgumentException("Optional positional callback parameters are not admitted");
            }
            if (named && !required && !type.endsWith("?")) {
                throw new IllegalArgumentException("An optional named callback parameter without a default must have a nullable type");
            }
        }

        public static Parameter requiredNamed(String type, String name) {
            return new Parameter(type, name, true, true);
        }

        public static Parameter optionalNamed(String type, String name) {
            return new Parameter(type, name, true, false);
        }

        public String declaration() {
            return (named && required ? "required " : "") + type + " " + name;
        }
    }

    /** Concrete types suitable for a member of a non-generic generated form. */
    public record Signature(String returnType, List<Parameter> parameters, List<String> importUris) {
        public Signature {
            returnType = requireType(returnType);
            parameters = List.copyOf(parameters);
            importUris = List.copyOf(importUris);
            HashSet<String> names = new HashSet<>();
            boolean namedSeen = false;
            for (Parameter parameter : parameters) {
                if (!names.add(parameter.name())) {
                    throw new IllegalArgumentException("Duplicate callback parameter: " + parameter.name());
                }
                if (!parameter.named() && namedSeen) {
                    throw new IllegalArgumentException("Positional callback parameters must precede named parameters");
                }
                namedSeen |= parameter.named();
            }
            for (String uri : importUris) {
                if (!uri.matches("(?:dart:[a-z_]+|package:flutter/[a-z_]+\\.dart)")) {
                    throw new IllegalArgumentException("Unreviewed callback import: " + uri);
                }
            }
        }

        public String declaration(String handlerName) {
            return returnType + " " + requireHandlerName(handlerName) + "("
                    + parameterList(false) + ")";
        }

        public String dartFunctionType() {
            return returnType + " Function("
                    + parameterList(true) + ")";
        }

        private String parameterList(boolean typesOnly) {
            String positional = parameters.stream().filter(parameter -> !parameter.named())
                    .map(parameter -> typesOnly ? parameter.type() : parameter.declaration()).collect(Collectors.joining(", "));
            String named = parameters.stream().filter(Parameter::named)
                    .map(Parameter::declaration).collect(Collectors.joining(", "));
            return named.isEmpty() ? positional : positional + (positional.isEmpty() ? "" : ", ") + "{" + named + "}";
        }

        @Override
        public String toString() {
            return dartFunctionType();
        }
    }

    /** The body is user-owned once inserted; non-void callbacks need an explicit implementation. */
    public String createStub(String handlerName) {
        String declaration = signature.declaration(handlerName);
        if (callbackType.equals("SelectableDayPredicate")) {
            return declaration + " {\n  // Customize the allowed dates; the initial date must satisfy this predicate.\n  return true;\n}";
        }
        if (callbackType.equals("ImageErrorWidgetBuilder?")) {
            return declaration + " {\n  // TODO: Display a fallback for " + propertyName.value()
                    + ".\n  return const SizedBox.shrink();\n}";
        }
        if (callbackType.equals("ShaderCallback")) {
            return declaration + " {\n  // Return a shader for the current paint bounds.\n"
                    + "  return (const LinearGradient(colors: <Color>[Color(0xFFFFFFFF), Color(0xFFFFFFFF)])).createShader(bounds);\n}";
        }
        if (callbackType.equals("TransformCallback")) {
            return declaration + " {\n  // Compute a fresh transform from animationValue.\n  return Matrix4.identity();\n}";
        }
        if (callbackType.equals("AnimatedSwitcherTransitionBuilder")) {
            return declaration + " {\n  // Customize the transition while retaining the child and its animation.\n"
                    + "  return AnimatedSwitcher.defaultTransitionBuilder(child, animation);\n}";
        }
        if (callbackType.equals("AnimatedSwitcherLayoutBuilder")) {
            return declaration + " {\n  // Preserve every outgoing child and allow currentChild to be null.\n"
                    + "  return AnimatedSwitcher.defaultLayoutBuilder(currentChild, previousChildren);\n}";
        }
        if (callbackType.equals("AnimatedCrossFadeBuilder")) {
            return declaration + " {\n  // Customize the layout while preserving both children and their keys.\n"
                    + "  return AnimatedCrossFade.defaultLayoutBuilder(topChild, topChildKey, bottomChild, bottomChildKey);\n}";
        }
        if (signature.returnType().equals("Future<void>")) {
            return declaration + " async {\n  // TODO: Handle " + propertyName.value() + ".\n}";
        }
        if (signature.returnType().equals("void")) {
            return declaration + " {\n  // TODO: Handle " + propertyName.value() + ".\n}";
        }
        return declaration + " {\n  // TODO: Implement " + propertyName.value()
                + ".\n  throw UnimplementedError('Implement " + requireHandlerName(handlerName) + "');\n}";
    }

    /** Preserve the existing persisted representation instead of migrating callbacks on selection. */
    public PropertyValue bindingValue(String handlerName, PropertyDefinition property) {
        Objects.requireNonNull(property, "property");
        if (!propertyName.equals(property.name())) {
            throw new IllegalArgumentException("Descriptor/property mismatch: " + property.name());
        }
        String acceptedName = requireHandlerName(handlerName);
        PropertyValue value;
        if (property.acceptedKinds().contains(PropertyValueKind.CALLBACK)) {
            value = new PropertyValue.CallbackValue(acceptedName);
        } else if (property.acceptedKinds().contains(PropertyValueKind.DART_OBJECT_REFERENCE)) {
            value = new PropertyValue.DartObjectReferenceValue(Optional.empty(), acceptedName, Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        } else {
            throw new IllegalArgumentException("Property has no callable binding representation: " + propertyName);
        }
        if (property.constraints().stream().noneMatch(constraint -> constraint.accepts(value))) {
            throw new IllegalArgumentException("Property rejects callback binding: " + propertyName);
        }
        return value;
    }

    private static String requireHandlerName(String value) {
        // Reuse the model's identifier and reserved-keyword validation, including private names.
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), value, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()).rootSymbol();
    }

    private static String requireType(String value) {
        Objects.requireNonNull(value, "Dart type");
        if (dev.flutter.netbeans.designer.catalog.ScaffoldWidgetPropertySchema.BOTTOM_SHEET_SCRIM_BUILDER_TYPE.equals(value)) return value;
        if (value.length() > 256 || !value.matches("[A-Za-z_][A-Za-z0-9_]*(?:<[A-Za-z0-9_?<>, ]+>)?\\??")) {
            throw new IllegalArgumentException("Invalid callback Dart type: " + value);
        }
        int depth = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '<') depth++;
            if (character == '>' && --depth < 0) {
                throw new IllegalArgumentException("Unbalanced callback Dart type: " + value);
            }
        }
        if (depth != 0) throw new IllegalArgumentException("Unbalanced callback Dart type: " + value);
        return value;
    }
}
