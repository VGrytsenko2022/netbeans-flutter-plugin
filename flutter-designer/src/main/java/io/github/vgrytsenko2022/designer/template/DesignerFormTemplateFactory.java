package io.github.vgrytsenko2022.designer.template;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.codec.FdEncodeException;
import io.github.vgrytsenko2022.designer.generation.DartGenerationResult;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.generation.GeneratedDartRegions;
import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.DesignerDocument;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.ManagedRegions;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.StableId;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.model.WidgetNode;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityResult;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityScanner;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
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
     * The Dart filename is the basename stored in versioned .fd source metadata;
     * the NetBeans integration owns its physical {@code lib} mapping.
     */
    public DesignerFormTemplate create(String dartFile, String className)
            throws FdEncodeException {
        return create(dartFile, className, WidgetClassKind.STATELESS);
    }

    /** Creates an explicitly selected class shape without changing existing forms. */
    public DesignerFormTemplate create(String dartFile, String className, WidgetClassKind widgetKind)
            throws FdEncodeException {
        Objects.requireNonNull(widgetKind, "widgetKind");
        WidgetNode root = starterTree();
        DartSourceDescriptor provisionalSource = source(
                dartFile,
                className,
                widgetKind,
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
                widgetKind,
                generated.imports().normalizedSha256(),
                generated.build().normalizedSha256());
        DesignerDocument document = new DesignerDocument(
                provisional.documentId(), finalSource, root);
        byte[] dartBytes = renderDart(className, widgetKind, generated)
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
            WidgetClassKind widgetKind,
            String importsSha256,
            String buildSha256) {
        return new DartSourceDescriptor(
                dartFile,
                className,
                widgetKind,
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
            WidgetClassKind widgetKind,
            GeneratedDartRegions generated) {
        String classOpening;
        if (widgetKind == WidgetClassKind.STATEFUL) {
            String stateClassName = (className.startsWith("_") ? className : "_" + className) + "State";
            classOpening = "\nclass " + className + " extends StatefulWidget {\n"
                    + "  const " + className + "({super.key});\n\n"
                    + "  @override\n"
                    + "  State<" + className + "> createState() => " + stateClassName + "();\n"
                    + "}\n\n"
                    + "class " + stateClassName + " extends State<" + className + "> {\n";
        } else {
            classOpening = "\nclass " + className + " extends StatelessWidget {\n"
                    + "  const " + className + "({super.key});\n\n";
        }
        return openingMarker(IMPORTS, "")
                + generated.imports().payload()
                + closingMarker("")
                + classOpening
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
