package dev.flutter.netbeans.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DesignerDocumentTest {
    @Test
    void representsCurrentDocumentAndPreservesSchemaReference() {
        StableId documentId = StableId.parse("2f04ce87-876a-4f35-8a7c-2fba3e135c7e");
        WidgetNode root = widget("35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce");
        DesignerDocument document = new DesignerDocument(
                Optional.of("../fd-v6.schema.json"),
                documentId,
                source(),
                Optional.of(CanvasPreferences.empty()),
                root,
                Extensions.empty());

        assertEquals("netbeans-flutter-designer", document.format());
        assertEquals(6, document.schemaVersion());
        assertEquals(Optional.of("../fd-v6.schema.json"), document.schemaReference());
        assertSame(root, document.root());
        assertEquals(Optional.of(CanvasPreferences.empty()), document.canvas());
    }

    @Test
    void convenienceConstructorUsesAbsentOptionalSections() {
        DesignerDocument document = new DesignerDocument(
                StableId.random(), source(), widget("35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce"));

        assertEquals(Optional.empty(), document.schemaReference());
        assertEquals(Optional.empty(), document.canvas());
        assertEquals(Extensions.empty(), document.extensions());
    }

    @Test
    void leavesDuplicateWidgetIdsForPathAwareStructuralValidation() {
        StableId repeated = StableId.parse("51ea38d4-19f3-46be-894d-0c8a9f70a3f6");
        WidgetNode first = WidgetNode.empty(repeated, new WidgetTypeId("flutter.widgets.Text"));
        WidgetNode second = WidgetNode.empty(repeated, new WidgetTypeId("flutter.widgets.Icon"));
        WidgetNode root = new WidgetNode(
                StableId.parse("35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce"),
                new WidgetTypeId("flutter.widgets.Column"),
                Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(List.of(first, second))));

        DesignerDocument document = new DesignerDocument(StableId.random(), source(), root);

        assertSame(root, document.root());
    }

    @Test
    void rejectsNullComponentsInsteadOfCreatingPartiallyDefinedDocuments() {
        StableId id = StableId.random();
        DartSourceDescriptor source = source();
        WidgetNode root = widget("35ca8ca5-c5ec-4fe1-8982-dfc036e3c6ce");

        assertThrows(NullPointerException.class,
                () -> new DesignerDocument(null, id, source, Optional.empty(), root, Extensions.empty()));
        assertThrows(NullPointerException.class,
                () -> new DesignerDocument(Optional.empty(), null, source, Optional.empty(), root, Extensions.empty()));
        assertThrows(NullPointerException.class,
                () -> new DesignerDocument(Optional.empty(), id, null, Optional.empty(), root, Extensions.empty()));
        assertThrows(NullPointerException.class,
                () -> new DesignerDocument(Optional.empty(), id, source, null, root, Extensions.empty()));
        assertThrows(NullPointerException.class,
                () -> new DesignerDocument(Optional.empty(), id, source, Optional.empty(), null, Extensions.empty()));
        assertThrows(NullPointerException.class,
                () -> new DesignerDocument(Optional.empty(), id, source, Optional.empty(), root, null));
    }

    private static WidgetNode widget(String id) {
        return WidgetNode.empty(StableId.parse(id), new WidgetTypeId("flutter.material.Scaffold"));
    }

    private static DartSourceDescriptor source() {
        ManagedRegion region = new ManagedRegion("A".repeat(64));
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.of("0.1.3-SNAPSHOT"),
                new ManagedRegions(region, region));
    }
}
