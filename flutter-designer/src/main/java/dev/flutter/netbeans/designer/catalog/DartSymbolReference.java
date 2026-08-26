package dev.flutter.netbeans.designer.catalog;

/** Immutable reference to a public top-level Dart symbol and an import library that exports it. */
public record DartSymbolReference(String libraryUri, String name) {
    public DartSymbolReference {
        libraryUri = DartImportUris.requireValid(libraryUri, "Dart symbol library URI");
        name = DartIdentifiers.requirePublicIdentifier(name, "Dart symbol name");
    }
}
