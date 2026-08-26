package dev.flutter.netbeans.plugin.designer.guard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import org.junit.jupiter.api.Test;
import org.netbeans.api.editor.guards.GuardedSectionManager;
import org.netbeans.api.editor.guards.SimpleSection;

class DartGuardedSectionsProviderTest {

    @Test
    void exposesValidMarkersAsNamedSimpleSections() {
        String source = "before();\n"
                + "// <netbeans-flutter-designer region=\"screen.build\">\n"
                + "generated();\n"
                + "// </netbeans-flutter-designer>\n"
                + "after();\n";
        DartGuardedSectionsProvider provider = provider();

        var result = provider.readSections(source.toCharArray());

        assertEquals(1, result.getGuardedSections().size());
        assertEquals("screen.build", result.getGuardedSections().getFirst().getName());
        assertInstanceOf(SimpleSection.class, result.getGuardedSections().getFirst());
        assertEquals(source.length(), result.getContent().length);
    }

    @Test
    void exposesNoGuardAndNoMaskedTextWhenDocumentIsAmbiguous() {
        String source = "// <netbeans-flutter-designer region=\"same\">\n"
                + "first();\n"
                + "// </netbeans-flutter-designer>\n"
                + "// <netbeans-flutter-designer region=\"same\">\n"
                + "second();\n"
                + "// </netbeans-flutter-designer>\n";

        var result = provider().readSections(source.toCharArray());

        assertTrue(result.getGuardedSections().isEmpty());
        assertArrayEquals(source.toCharArray(), result.getContent());
    }

    @Test
    void factoryCreatesTheDartProvider() {
        var created = new DartGuardedSectionsFactory().create(
                DefaultStyledDocument::new);

        assertEquals(DartGuardedSectionsProvider.class, created.getClass());
    }

    @Test
    void guardedWriterRestoresMarkersAndKeepsLatestSafeSnapshot()
            throws IOException, BadLocationException {
        String source = sourceWithRegion();
        LoadedDocument loaded = load(source);
        loaded.document().insertString(loaded.document().getLength(), "manual();\n", null);

        ByteArrayOutputStream firstSave = save(loaded);
        String expected = source + "manual();\n";
        assertEquals(expected, firstSave.toString(StandardCharsets.UTF_8));

        SimpleSection section = manager(loaded).findSimpleSection("screen.build");
        assertNotNull(section);
        section.removeSection();

        ByteArrayOutputStream rejectedSave = new ByteArrayOutputStream();
        try (Writer writer = loaded.provider().createGuardedWriter(
                rejectedSave, StandardCharsets.UTF_8)) {
            writer.write(documentText(loaded.document()));
            IOException failure = assertThrows(IOException.class, writer::close);
            assertTrue(failure.getMessage().contains("expected [screen.build] but found []"));
        }
        assertEquals(expected, rejectedSave.toString(StandardCharsets.UTF_8));
    }

    @Test
    void explicitAttemptAbortsAfterFailedOutputAndRetainsOldFallback()
            throws IOException, BadLocationException {
        String source = sourceWithRegion();
        LoadedDocument loaded = load(source);
        loaded.document().insertString(
                loaded.document().getLength(), "unsaved-after-failure();\n", null);

        DartGuardedSectionsProvider.PersistenceAttempt attempt =
                loaded.provider().beginPersistenceAttempt();
        Writer writer = loaded.provider().createGuardedWriter(
                new AlwaysFailingOutputStream(), StandardCharsets.UTF_8);
        writer.write(documentText(loaded.document()));
        assertThrows(IOException.class, writer::close);
        attempt.abort();

        assertEquals(source, rejectedFallbackAfterRemovingGuard(loaded));
    }

    @Test
    void getInputStreamStyleSerializationCanAbortWithoutAdvancingFallback()
            throws IOException, BadLocationException {
        String source = sourceWithRegion();
        LoadedDocument loaded = load(source);
        String unsaved = "serialized-only();\n";
        loaded.document().insertString(
                loaded.document().getLength(), unsaved, null);
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();

        try (DartGuardedSectionsProvider.PersistenceAttempt attempt =
                loaded.provider().beginPersistenceAttempt()) {
            try (Writer writer = loaded.provider().createGuardedWriter(
                    serialized, StandardCharsets.UTF_8)) {
                writer.write(documentText(loaded.document()));
            }
            assertEquals(source + unsaved, serialized.toString(StandardCharsets.UTF_8));
            attempt.abort();
        }

        assertEquals(source, rejectedFallbackAfterRemovingGuard(loaded));
    }

    @Test
    void explicitAttemptCommitsOnlyAfterSuccessfulOutput()
            throws IOException, BadLocationException {
        String source = sourceWithRegion();
        LoadedDocument loaded = load(source);
        String persisted = "committed();\n";
        loaded.document().insertString(
                loaded.document().getLength(), persisted, null);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (DartGuardedSectionsProvider.PersistenceAttempt attempt =
                loaded.provider().beginPersistenceAttempt()) {
            try (Writer writer = loaded.provider().createGuardedWriter(
                    output, StandardCharsets.UTF_8)) {
                writer.write(documentText(loaded.document()));
            }
            assertEquals(source + persisted, output.toString(StandardCharsets.UTF_8));
            attempt.commit();
        }

        assertEquals(source + persisted, rejectedFallbackAfterRemovingGuard(loaded));
    }

    @Test
    void removedLastGuardRestoresKnownGoodMarkersAndFailsSave()
            throws IOException, BadLocationException {
        String source = sourceWithRegion().replace("\n", "\r\n");
        LoadedDocument loaded = load(source);
        SimpleSection section = manager(loaded).findSimpleSection("screen.build");
        assertNotNull(section);
        section.removeSection();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Writer writer = loaded.provider().createGuardedWriter(
                output, StandardCharsets.UTF_8);
        writer.write(documentText(loaded.document()));

        IOException failure = assertThrows(IOException.class, writer::close);

        assertTrue(failure.getMessage().contains("expected [screen.build] but found []"));
        assertEquals(source, output.toString(StandardCharsets.UTF_8));
        assertTrue(output.toString(StandardCharsets.UTF_8)
                .contains("// <netbeans-flutter-designer region=\"screen.build\">"));
    }

    @Test
    void failClosedFallbackRejectsExactOutputBeforeDelegateClose()
            throws IOException, BadLocationException {
        LoadedDocument loaded = load(sourceWithRegion());
        SimpleSection section = manager(loaded).findSimpleSection("screen.build");
        assertNotNull(section);
        section.removeSection();
        RejectionRecordingOutput output = new RejectionRecordingOutput();
        Writer writer = loaded.provider().createGuardedWriter(
                output, StandardCharsets.UTF_8);
        writer.write(documentText(loaded.document()));

        assertThrows(IOException.class, writer::close);

        assertNotNull(output.rejectionReason);
        assertTrue(output.rejectionReason.contains(
                "expected [screen.build] but found []"));
        assertTrue(output.rejectedBeforeClose,
                "guard failure must reach the transaction output before close");
    }

    @Test
    void unexpectedlyRenamedGuardRestoresKnownGoodSourceAndFailsSave()
            throws IOException, BadLocationException, PropertyVetoException {
        String source = sourceWithRegion();
        LoadedDocument loaded = load(source);
        SimpleSection section = manager(loaded).findSimpleSection("screen.build");
        assertNotNull(section);
        section.setName("screen.renamed");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Writer writer = loaded.provider().createGuardedWriter(
                output, StandardCharsets.UTF_8);
        writer.write(documentText(loaded.document()));

        IOException failure = assertThrows(IOException.class, writer::close);

        assertTrue(failure.getMessage()
                .contains("expected [screen.build] but found [screen.renamed]"));
        assertEquals(source, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void directInvalidWriteCannotReturnMaskedContentAsSuccessfulPersistence()
            throws IOException, BadLocationException {
        LoadedDocument loaded = load(sourceWithRegion());
        SimpleSection section = manager(loaded).findSimpleSection("screen.build");
        assertNotNull(section);
        section.removeSection();

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> loaded.provider().writeSections(
                        java.util.List.of(section),
                        documentText(loaded.document()).toCharArray()));

        assertTrue(failure.getMessage().contains("Cannot persist Dart guarded sections"));
    }

    @Test
    void reconstructsMarkersWithoutAdvancingThePersistenceFallback()
            throws IOException, BadLocationException {
        String source = sourceWithRegion();
        LoadedDocument loaded = load(source);
        String unsaved = "manual();\n";
        loaded.document().insertString(loaded.document().getLength(), unsaved, null);

        DartGuardedSectionsProvider.MarkerBearingSnapshot snapshot =
                loaded.provider().reconstructMarkerBearingSnapshot(loaded.document());

        assertEquals(source + unsaved, new String(snapshot.markerBearingContent()));
        assertEquals(documentText(loaded.document()), new String(snapshot.maskedContent()));
        assertEquals(1, snapshot.sections().size());
        DartGuardedSectionsProvider.MarkerSectionSnapshot section =
                snapshot.sections().getFirst();
        assertEquals("screen.build", section.id());
        assertSame(manager(loaded).findSimpleSection("screen.build"),
                section.sectionIdentity());
        assertEquals(source.indexOf("// <netbeans-flutter-designer"),
                section.sectionStartChar());
        assertEquals(source.indexOf("generated();"), section.payloadStartChar());
        assertEquals(source.indexOf("// </netbeans-flutter-designer>"),
                section.payloadEndChar());

        section.sectionIdentity().removeSection();
        ByteArrayOutputStream rejected = new ByteArrayOutputStream();
        Writer writer = loaded.provider().createGuardedWriter(
                rejected, StandardCharsets.UTF_8);
        writer.write(documentText(loaded.document()));
        assertThrows(IOException.class, writer::close);
        assertEquals(source, rejected.toString(StandardCharsets.UTF_8),
                "a read-only reconstruction must not replace the persisted fallback");
    }

    @Test
    void reconstructionRejectsADifferentDocument() {
        DartGuardedSectionsProvider provider = provider();

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> provider.reconstructMarkerBearingSnapshot(
                        new DefaultStyledDocument()));

        assertTrue(failure.getMessage().contains("not bound"));
    }

    private static DartGuardedSectionsProvider provider() {
        DefaultStyledDocument document = new DefaultStyledDocument();
        return new DartGuardedSectionsProvider(() -> document);
    }

    private static LoadedDocument load(String source)
            throws IOException, BadLocationException {
        DefaultStyledDocument document = new DefaultStyledDocument();
        DartGuardedSectionsProvider provider = new DartGuardedSectionsProvider(() -> document);
        try (Reader reader = provider.createGuardedReader(
                new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8)) {
            String displayed = readAll(reader);
            document.insertString(0, displayed, null);
        }
        return new LoadedDocument(document, provider);
    }

    private static ByteArrayOutputStream save(LoadedDocument loaded) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Writer writer = loaded.provider().createGuardedWriter(
                output, StandardCharsets.UTF_8)) {
            writer.write(documentText(loaded.document()));
        }
        return output;
    }

    private static String rejectedFallbackAfterRemovingGuard(LoadedDocument loaded)
            throws IOException {
        SimpleSection section = manager(loaded).findSimpleSection("screen.build");
        assertNotNull(section);
        section.removeSection();

        ByteArrayOutputStream rejected = new ByteArrayOutputStream();
        Writer writer = loaded.provider().createGuardedWriter(
                rejected, StandardCharsets.UTF_8);
        writer.write(documentText(loaded.document()));
        assertThrows(IOException.class, writer::close);
        return rejected.toString(StandardCharsets.UTF_8);
    }

    private static GuardedSectionManager manager(LoadedDocument loaded) {
        GuardedSectionManager manager = GuardedSectionManager.getInstance(loaded.document());
        assertNotNull(manager);
        return manager;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[256];
        int count;
        while ((count = reader.read(buffer)) >= 0) {
            result.append(buffer, 0, count);
        }
        return result.toString();
    }

    private static String documentText(DefaultStyledDocument document) {
        try {
            return document.getText(0, document.getLength());
        } catch (BadLocationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static String sourceWithRegion() {
        return "before();\n"
                + "// <netbeans-flutter-designer region=\"screen.build\">\n"
                + "generated();\n"
                + "// </netbeans-flutter-designer>\n"
                + "after();\n";
    }

    private record LoadedDocument(
            DefaultStyledDocument document,
            DartGuardedSectionsProvider provider) {
    }

    private static final class AlwaysFailingOutputStream extends OutputStream {
        @Override
        public void write(int value) throws IOException {
            throw new IOException("injected output failure");
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            throw new IOException("injected output failure");
        }
    }

    private static final class RejectionRecordingOutput
            extends ByteArrayOutputStream
            implements GuardedPersistenceRejectionSink {
        private String rejectionReason;
        private boolean rejectedBeforeClose;

        @Override
        public void rejectGuardedPersistence(String reason) {
            rejectionReason = reason;
        }

        @Override
        public void close() throws IOException {
            rejectedBeforeClose = rejectionReason != null;
            super.close();
        }
    }
}
