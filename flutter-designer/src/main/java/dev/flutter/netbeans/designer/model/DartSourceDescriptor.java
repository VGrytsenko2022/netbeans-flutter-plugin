package dev.flutter.netbeans.designer.model;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Immutable source-pair metadata stored in a Flutter Designer document. */
public record DartSourceDescriptor(
        String dartFile,
        String className,
        WidgetClassKind widgetKind,
        Optional<String> generatorVersion,
        ManagedRegions managedRegions) {

    private static final Pattern DART_FILE = Pattern.compile("_?[a-z][a-z0-9_]*\\.dart");
    private static final Pattern CLASS_NAME = Pattern.compile("_?[A-Z][A-Za-z0-9]*");

    public DartSourceDescriptor {
        dartFile = ModelConstraints.matching(dartFile, "dartFile", DART_FILE);
        className = ModelConstraints.matching(className, "className", CLASS_NAME);
        if (className.equals("Function")) {
            throw new IllegalArgumentException("className is a reserved Dart identifier: " + className);
        }
        Objects.requireNonNull(widgetKind, "widgetKind");
        Objects.requireNonNull(generatorVersion, "generatorVersion");
        generatorVersion = generatorVersion.map(value ->
                ModelConstraints.codePointLength(value, "generatorVersion", 1, 64));
        Objects.requireNonNull(managedRegions, "managedRegions");
    }
}
