package dev.flutter.netbeans.designer.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.command.DesignerCommandSession;
import dev.flutter.netbeans.designer.command.DesignerCommandSessionOpenResult;
import dev.flutter.netbeans.designer.command.DesignerCommandSessionResult;
import dev.flutter.netbeans.designer.command.DesignerCommandStatus;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ValidatedCanvasRevisionSnapshotTest {
    private static final WidgetCatalog CATALOG = BuiltInWidgetCatalog.getDefault();
    private static final StableId DOCUMENT_ID = StableId.parse(
            "63a5a225-718e-47c0-a16a-2524cddf2e46");
    private static final StableId ROOT_ID = StableId.parse(
            "dcb0d82f-faa4-4d17-a503-ed8ace10c4f0");
    private static final PropertyName DATA = new PropertyName("data");

    @Test
    void capturesTheExactCurrentCommandSessionRevision() throws Exception {
        DesignerCommandSession initial = openSession();
        DesignerCommandSessionResult changed = initial.apply(new SetProperty(
                ROOT_ID,
                DATA,
                new PropertyValue.StringValue("current")));
        assertEquals(DesignerCommandStatus.APPLIED, changed.status());
        DesignerCommandSession session = changed.session();

        ValidatedCanvasRevisionSnapshot snapshot =
                ValidatedCanvasRevisionSnapshot.capture(session);

        assertEquals(session.current().revisionId(), snapshot.logicalRevisionId());
        assertSame(session.current().document(), snapshot.document());
        assertSame(session.catalog(), snapshot.catalog());
        assertEquals(
                new WidgetTreeValidator(session.limits().validationLimits())
                        .validate(session.current().document(), session.catalog()),
                snapshot.validation());
        assertTrue(snapshot.validation().valid());
    }

    @Test
    void packagePrivateValidationRejectsAnInvalidDetachedDocument()
            throws Exception {
        DesignerCommandSession session = openSession();
        DesignerDocument current = session.current().document();
        DesignerDocument detached = new DesignerDocument(
                current.schemaReference(),
                current.documentId(),
                current.source(),
                current.canvas(),
                WidgetNode.empty(
                        ROOT_ID,
                        new WidgetTypeId("flutter.widgets.DetachedUnknown")),
                current.extensions());

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> ValidatedCanvasRevisionSnapshot.validate(
                        session.current().revisionId(),
                        detached,
                        session.catalog(),
                        session.limits().validationLimits()));

        assertTrue(failure.getMessage().contains(
                WidgetTreeValidator.UNKNOWN_WIDGET_TYPE));
    }

    private static DesignerCommandSession openSession() throws Exception {
        DartSourceDescriptor placeholder = sourceDescriptor(
                "0".repeat(64), "0".repeat(64));
        WidgetNode root = new WidgetNode(
                ROOT_ID,
                new WidgetTypeId("flutter.widgets.Text"),
                Map.of(DATA, new PropertyValue.StringValue("initial")),
                Map.of());
        DesignerDocument provisional = new DesignerDocument(
                DOCUMENT_ID, placeholder, root);
        DartRegionGenerator generator = new DartRegionGenerator();
        GeneratedDartRegions generated = generator.generate(provisional, CATALOG)
                .generated().orElseThrow();
        DartSourceDescriptor source = sourceDescriptor(
                generated.imports().normalizedSha256(),
                generated.build().normalizedSha256());
        DesignerDocument document = new DesignerDocument(DOCUMENT_ID, source, root);
        GeneratedDartRegions verified = generator.generate(document, CATALOG)
                .generated().orElseThrow();
        OriginalFdBytes fd = new FdDocumentCodec().encode(document);
        DesignerCommandSessionOpenResult opened = DesignerCommandSession.open(
                fd,
                sourceBytes(verified.imports().payload(), verified.build().payload()),
                CATALOG);

        assertTrue(opened.ready(), () -> opened.diagnostics().toString());
        return opened.session().orElseThrow();
    }

    private static DartSourceDescriptor sourceDescriptor(
            String importsHash,
            String buildHash) {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(
                        new ManagedRegion(importsHash),
                        new ManagedRegion(buildHash)));
    }

    private static byte[] sourceBytes(String imports, String build) {
        return ("// <netbeans-flutter-designer region=\"imports\">\n"
                + imports
                + "// </netbeans-flutter-designer>\n\n"
                + "class HomePage extends StatelessWidget {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + build
                + "  // </netbeans-flutter-designer>\n"
                + "}\n").getBytes(StandardCharsets.UTF_8);
    }
}
