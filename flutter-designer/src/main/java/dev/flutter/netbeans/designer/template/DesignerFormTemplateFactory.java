package dev.flutter.netbeans.designer.template;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdEncodeException;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.SlotName;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetSlot;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/** Builds the canonical, internally consistent starter pair for a new form. */
public final class DesignerFormTemplateFactory {
    private static final String EMPTY_SHA256 = "0".repeat(64);
    private static final String IMPORTS = "imports";
    private static final String BUILD = "build";

    private final DartRegionGenerator generator;
    private final FdDocumentCodec codec;

    public DesignerFormTemplateFactory() {
        this(new DartRegionGenerator(), new FdDocumentCodec());
    }

    DesignerFormTemplateFactory(
            DartRegionGenerator generator,
            FdDocumentCodec codec) {
        this.generator = generator;
        this.codec = codec;
    }

    /**
     * Creates a stateless {@code Scaffold -> Center -> Text} starter form.
     * The Dart filename is the basename stored in schema-v1 source metadata;
     * the NetBeans integration owns its physical {@code lib} mapping.
     */
    public DesignerFormTemplate create(String dartFile, String className)
            throws FdEncodeException {
        WidgetNode root = starterTree();
        DartSourceDescriptor provisionalSource = source(
                dartFile,
                className,
                EMPTY_SHA256,
                EMPTY_SHA256);
        DesignerDocument provisional = new DesignerDocument(
                StableId.random(), provisionalSource, root);

        DartGenerationResult generatedResult = generator.generate(
                provisional,
                BuiltInWidgetCatalog.getDefault());
        GeneratedDartRegions generated = generatedResult.generated().orElseThrow(() ->
                new IllegalStateException(
                        "The built-in Flutter Designer starter form cannot be generated: "
                        + generatedResult.diagnostics()));

        DartSourceDescriptor finalSource = source(
                dartFile,
                className,
                generated.imports().normalizedSha256(),
                generated.build().normalizedSha256());
        DesignerDocument document = new DesignerDocument(
                provisional.documentId(), finalSource, root);
        byte[] dartBytes = renderDart(className, generated)
                .getBytes(StandardCharsets.UTF_8);
        DartSourceIntegrityResult integrity = new DartSourceIntegrityScanner()
                .scan(dartBytes, finalSource);
        if (!integrity.onDiskDeclaredMatch()) {
            throw new IllegalStateException(
                    "The built-in Flutter Designer starter source failed integrity validation: "
                    + integrity.diagnostics());
        }
        byte[] fdBytes = codec.encode(document).copyBytes();
        return new DesignerFormTemplate(document, dartBytes, fdBytes);
    }

    private static DartSourceDescriptor source(
            String dartFile,
            String className,
            String importsSha256,
            String buildSha256) {
        return new DartSourceDescriptor(
                dartFile,
                className,
                WidgetClassKind.STATELESS,
                Optional.empty(),
                new ManagedRegions(
                        new ManagedRegion(importsSha256),
                        new ManagedRegion(buildSha256)));
    }

    private static WidgetNode starterTree() {
        WidgetNode text = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),
                        new PropertyValue.StringValue("Hello from NetBeans")),
                Map.of());
        WidgetNode center = new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.widgets.Center"),
                Map.of(),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
        return new WidgetNode(
                StableId.random(),
                new WidgetTypeId("flutter.material.Scaffold"),
                Map.of(),
                Map.of(new SlotName("body"), WidgetSlot.SingleSlot.of(center)));
    }

    private static String renderDart(
            String className,
            GeneratedDartRegions generated) {
        return openingMarker(IMPORTS, "")
                + generated.imports().payload()
                + closingMarker("")
                + "\nclass " + className + " extends StatelessWidget {\n"
                + "  const " + className + "({super.key});\n\n"
                + openingMarker(BUILD, "  ")
                + generated.build().payload()
                + closingMarker("  ")
                + "}\n";
    }

    private static String openingMarker(String region, String indentation) {
        return indentation + "// <netbeans-flutter-designer region=\""
                + region + "\">\n";
    }

    private static String closingMarker(String indentation) {
        return indentation + "// </netbeans-flutter-designer>\n";
    }
}
