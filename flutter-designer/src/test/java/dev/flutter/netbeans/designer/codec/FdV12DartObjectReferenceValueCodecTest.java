package dev.flutter.netbeans.designer.codec;

import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.model.Extensions;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdV12DartObjectReferenceValueCodecTest {
    private static final PropertyName CURRENT_REFERENCE =
            new PropertyName("currentReference");
    private static final PropertyName IMPORTED_REFERENCE =
            new PropertyName("importedReference");
    private static final PropertyName CURRENT_INVOCATION =
            new PropertyName("currentInvocation");
    private static final PropertyName IMPORTED_INVOCATION =
            new PropertyName("importedInvocation");
    private static final PropertyName CLIPPER = new PropertyName("clipper");

    private final FdDocumentCodec codec = new FdDocumentCodec();

    @Test
    void roundTripsEveryClosedReferenceFormInCanonicalOrder() throws Exception {
        PropertyValue.DartObjectReferenceValue currentReference = reference(
                Optional.empty(), "_localClipper", Optional.empty());
        PropertyValue.DartObjectReferenceValue importedReference = reference(
                Optional.of("package:sample/clippers/rrect_clipper.dart"),
                "RRectClippers", Optional.of("rounded"));
        PropertyValue.DartObjectReferenceValue currentInvocation = invocation(
                Optional.empty(), "LocalClipper", Optional.of("create"), false);
        PropertyValue.DartObjectReferenceValue importedInvocation = invocation(
                Optional.of("package:sample/clippers/rrect_clipper.dart"),
                "RoundedClipper", Optional.empty(), true);
        OriginalFdBytes encoded = codec.encode(document(Map.of(
                CURRENT_REFERENCE, currentReference,
                IMPORTED_REFERENCE, importedReference,
                CURRENT_INVOCATION, currentInvocation,
                IMPORTED_INVOCATION, importedInvocation)));
        String json = new String(encoded.copyBytes(), StandardCharsets.UTF_8);
        String compact = json.replaceAll("\\s+", "");

        assertTrue(json.contains("\"schemaVersion\": 12"), json);
        assertTrue(json.contains("\"$schema\": \"../fd-v12.schema.json\""), json);
        assertTrue(compact.contains(
                "\"currentReference\":{\"kind\":\"dartObjectReference\","
                + "\"libraryUri\":null,\"rootSymbol\":\"_localClipper\","
                + "\"member\":null,\"access\":\"reference\"}"), json);
        assertTrue(compact.contains(
                "\"importedReference\":{\"kind\":\"dartObjectReference\","
                + "\"libraryUri\":\"package:sample/clippers/rrect_clipper.dart\","
                + "\"rootSymbol\":\"RRectClippers\",\"member\":\"rounded\","
                + "\"access\":\"reference\"}"), json);
        assertTrue(compact.contains(
                "\"currentInvocation\":{\"kind\":\"dartObjectReference\","
                + "\"libraryUri\":null,\"rootSymbol\":\"LocalClipper\","
                + "\"member\":\"create\",\"access\":\"zeroArgumentInvocation\","
                + "\"constant\":false}"), json);
        assertTrue(compact.contains(
                "\"importedInvocation\":{\"kind\":\"dartObjectReference\","
                + "\"libraryUri\":\"package:sample/clippers/rrect_clipper.dart\","
                + "\"rootSymbol\":\"RoundedClipper\",\"member\":null,"
                + "\"access\":\"zeroArgumentInvocation\",\"constant\":true}"),
                json);

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class, codec.decode(encoded));
        assertEquals(12, decoded.sourceSchemaVersion());
        assertFalse(decoded.migrated());
        assertEquals(currentReference,
                decoded.document().root().properties().get(CURRENT_REFERENCE));
        assertEquals(importedReference,
                decoded.document().root().properties().get(IMPORTED_REFERENCE));
        assertEquals(currentInvocation,
                decoded.document().root().properties().get(CURRENT_INVOCATION));
        assertEquals(importedInvocation,
                decoded.document().root().properties().get(IMPORTED_INVOCATION));
        assertArrayEquals(encoded.copyBytes(),
                codec.encode(decoded.document()).copyBytes());
    }

    @Test
    void rejectsTheNewKindBeforeV12AndMigratesV11Omission() throws Exception {
        String currentWithValue = new String(codec.encode(document(Map.of(
                CLIPPER, reference(Optional.empty(), "clipper", Optional.empty()))))
                .copyBytes(), StandardCharsets.UTF_8);
        String v11WithValue = currentWithValue
                .replace("\"schemaVersion\": 12", "\"schemaVersion\": 11")
                .replace("../fd-v12.schema.json", "../fd-v11.schema.json");
        assertInvalid(v11WithValue, FdCodecDiagnosticCode.INVALID_VALUE,
                "/root/properties/clipper/kind");

        String currentOmission = new String(codec.encode(document(Map.of())).copyBytes(),
                StandardCharsets.UTF_8);
        String v11Omission = currentOmission
                .replace("\"schemaVersion\": 12", "\"schemaVersion\": 11")
                .replace("../fd-v12.schema.json", "../fd-v11.schema.json");
        FdDecodeResult.Current migrated = assertInstanceOf(
                FdDecodeResult.Current.class,
                codec.decode(v11Omission.getBytes(StandardCharsets.UTF_8)));
        assertEquals(11, migrated.sourceSchemaVersion());
        assertTrue(migrated.migrated());
        assertEquals(Optional.of("../fd-v12.schema.json"),
                migrated.document().schemaReference());
        assertFalse(migrated.document().root().properties().containsKey(CLIPPER));
    }

    @Test
    void rejectsMissingUnknownAndAccessSpecificFields() throws Exception {
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference","rootSymbol":"Clipper",
                 "member":null,"access":"reference"}
                """), FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                "/root/properties/clipper/libraryUri");
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference","libraryUri":null,
                 "rootSymbol":"Clipper","access":"reference"}
                """), FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                "/root/properties/clipper/member");
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference","libraryUri":null,
                 "rootSymbol":"Clipper","member":null,"access":"reference",
                 "constant":false}
                """), FdCodecDiagnosticCode.UNKNOWN_FIELD,
                "/root/properties/clipper/constant");
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference","libraryUri":null,
                 "rootSymbol":"Clipper","member":null,
                 "access":"zeroArgumentInvocation"}
                """), FdCodecDiagnosticCode.MISSING_REQUIRED_FIELD,
                "/root/properties/clipper/constant");
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference","libraryUri":null,
                 "rootSymbol":"Clipper","member":null,
                 "access":"zeroArgumentInvocation","constant":"yes"}
                """), FdCodecDiagnosticCode.WRONG_VALUE_TYPE,
                "/root/properties/clipper/constant");
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference","libraryUri":null,
                 "rootSymbol":"Clipper","member":null,"access":"call"}
                """), FdCodecDiagnosticCode.INVALID_VALUE,
                "/root/properties/clipper/access");
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference","libraryUri":null,
                 "rootSymbol":"Clipper","member":null,"access":"reference",
                 "future":true}
                """), FdCodecDiagnosticCode.UNKNOWN_FIELD,
                "/root/properties/clipper/future");
    }

    @Test
    void rejectsUnsafeUrisAndIdentifiersFailClosed() throws Exception {
        for (String uri : new String[] {
                "dart:ui", "../clipper.dart", "package:Sample/clipper.dart",
                "package:sample/../clipper.dart", "package:sample//clipper.dart"}) {
            assertInvalid(propertyDocument("""
                    {"kind":"dartObjectReference","libraryUri":"%s",
                     "rootSymbol":"Clipper","member":null,"access":"reference"}
                    """.formatted(uri)), FdCodecDiagnosticCode.INVALID_VALUE,
                    "/root/properties/clipper");
        }
        for (String root : new String[] {"9Clipper", "Clipper.bad", "class"}) {
            assertInvalid(propertyDocument("""
                    {"kind":"dartObjectReference","libraryUri":null,
                     "rootSymbol":"%s","member":null,"access":"reference"}
                    """.formatted(root)), FdCodecDiagnosticCode.INVALID_VALUE,
                    "/root/properties/clipper");
        }
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference",
                 "libraryUri":"package:sample/clipper.dart",
                 "rootSymbol":"_PrivateClipper","member":null,
                 "access":"reference"}
                """), FdCodecDiagnosticCode.INVALID_VALUE,
                "/root/properties/clipper");
        assertInvalid(propertyDocument("""
                {"kind":"dartObjectReference",
                 "libraryUri":"package:sample/clipper.dart",
                 "rootSymbol":"Clipper","member":"_private",
                 "access":"reference"}
                """), FdCodecDiagnosticCode.INVALID_VALUE,
                "/root/properties/clipper");
    }

    private void assertInvalid(
            String json,
            FdCodecDiagnosticCode code,
            String pointer) throws Exception {
        FdDecodeResult.Invalid invalid = assertInstanceOf(
                FdDecodeResult.Invalid.class,
                codec.decode(json.getBytes(StandardCharsets.UTF_8)), json);
        assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == code && diagnostic.pointer().equals(pointer)),
                () -> invalid.diagnostics().toString());
    }

    private static PropertyValue.DartObjectReferenceValue reference(
            Optional<String> libraryUri,
            String rootSymbol,
            Optional<String> member) {
        return new PropertyValue.DartObjectReferenceValue(
                libraryUri, rootSymbol, member,
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                Optional.empty());
    }

    private static PropertyValue.DartObjectReferenceValue invocation(
            Optional<String> libraryUri,
            String rootSymbol,
            Optional<String> member,
            boolean constant) {
        return new PropertyValue.DartObjectReferenceValue(
                libraryUri, rootSymbol, member,
                PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION,
                Optional.of(constant));
    }

    private static DesignerDocument document(
            Map<PropertyName, PropertyValue> properties) {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DesignerDocument(
                Optional.of("../fd-v12.schema.json"),
                StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                new DartSourceDescriptor(
                        "clip_rrect_page.dart", "ClipRRectPage",
                        WidgetClassKind.STATELESS, Optional.of("test"),
                        new ManagedRegions(region, region)),
                Optional.empty(),
                new WidgetNode(
                        StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                        new WidgetTypeId("flutter.widgets.ClipRRect"),
                        properties, Map.of(), Extensions.empty()),
                Extensions.empty());
    }

    private static String propertyDocument(String value) {
        return """
                {
                  "$schema": "../fd-v12.schema.json",
                  "format": "netbeans-flutter-designer",
                  "schemaVersion": 12,
                  "documentId": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                  "source": {
                    "dartFile": "clip_rrect_page.dart",
                    "className": "ClipRRectPage",
                    "widgetKind": "stateless",
                    "managedRegions": {
                      "imports": {
                        "sha256": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                      },
                      "build": {
                        "sha256": "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
                      }
                    }
                  },
                  "root": {
                    "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                    "type": "flutter.widgets.ClipRRect",
                    "properties": {"clipper": %s},
                    "slots": {}
                  }
                }
                """.formatted(value);
    }
}
