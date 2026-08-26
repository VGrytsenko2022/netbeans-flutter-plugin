package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.designer.source.DartSourceIntegrityLimits;
import dev.flutter.netbeans.plugin.designer.guard.DartGuardedSectionsProvider;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.guards.SimpleSection;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;

/**
 * Immutable evidence for one exact guarded Dart {@link StyledDocument}
 * revision. Character offsets belong to the live Swing document; persisted
 * source evidence is retained separately as strict UTF-8 bytes.
 */
final class LiveDartDocumentSnapshot {
    static final String IMPORTS_REGION = "imports";
    static final String BUILD_REGION = "build";

    private final StyledDocument documentIdentity;
    private final long documentVersion;
    private final String maskedContent;
    private final byte[] markerBearingUtf8;
    private final String markerBearingSha256;
    private final ManagedSection imports;
    private final ManagedSection build;

    private LiveDartDocumentSnapshot(
            StyledDocument documentIdentity,
            long documentVersion,
            String maskedContent,
            byte[] markerBearingUtf8,
            String markerBearingSha256,
            ManagedSection imports,
            ManagedSection build) {
        this.documentIdentity = documentIdentity;
        this.documentVersion = documentVersion;
        this.maskedContent = maskedContent;
        this.markerBearingUtf8 = markerBearingUtf8.clone();
        this.markerBearingSha256 = markerBearingSha256;
        this.imports = imports;
        this.build = build;
    }

    static LiveDartDocumentSnapshot capture(
            StyledDocument document,
            DartGuardedSectionsProvider provider) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(provider, "provider");
        AtomicReference<LiveDartDocumentSnapshot> result = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        document.render(() -> {
            try {
                long before = DocumentUtilities.getDocumentVersion(document);
                DartGuardedSectionsProvider.MarkerBearingSnapshot reconstructed =
                        provider.reconstructMarkerBearingSnapshot(document);
                long after = DocumentUtilities.getDocumentVersion(document);
                if (before != after) {
                    throw new IOException(
                            "The live Dart document changed while it was being snapshotted");
                }
                result.set(fromReconstruction(document, after, reconstructed));
            } catch (IOException ex) {
                failure.set(ex);
            } catch (RuntimeException ex) {
                failure.set(new IOException(
                        "Cannot reconstruct the live Dart guarded document: "
                        + reason(ex), ex));
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        LiveDartDocumentSnapshot snapshot = result.get();
        if (snapshot == null) {
            throw new IOException("The live Dart document snapshot is unavailable");
        }
        return snapshot;
    }

    private static LiveDartDocumentSnapshot fromReconstruction(
            StyledDocument document,
            long version,
            DartGuardedSectionsProvider.MarkerBearingSnapshot reconstructed)
            throws IOException {
        char[] maskedChars = reconstructed.maskedContent();
        char[] markerChars = reconstructed.markerBearingContent();
        String masked = new String(maskedChars);
        byte[] markerUtf8 = strictWritableUtf8(markerChars);
        if (markerUtf8.length > DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES) {
            throw new IOException("The live Dart source exceeds the "
                    + DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES
                    + " byte writable safety limit");
        }

        List<DartGuardedSectionsProvider.MarkerSectionSnapshot> sections =
                reconstructed.sections();
        if (sections.size() != 2
                || !IMPORTS_REGION.equals(sections.get(0).id())
                || !BUILD_REGION.equals(sections.get(1).id())) {
            throw new IOException(
                    "Writable Flutter Designer source requires exactly imports then build guards");
        }
        ManagedSection imports = managedSection(masked, sections.get(0));
        ManagedSection build = managedSection(masked, sections.get(1));
        if (imports.sectionEndChar() > build.sectionStartChar()) {
            throw new IOException("The imports and build live guard ranges overlap");
        }
        return new LiveDartDocumentSnapshot(
                document,
                version,
                masked,
                markerUtf8,
                sha256(markerUtf8),
                imports,
                build);
    }

    private static ManagedSection managedSection(
            String masked,
            DartGuardedSectionsProvider.MarkerSectionSnapshot source)
            throws IOException {
        int start = source.sectionStartChar();
        int end = source.sectionEndChar();
        int payloadStart = source.payloadStartChar();
        int payloadEnd = source.payloadEndChar();
        int closingMarkerStart = source.closingMarkerStartChar();
        if (end > masked.length()) {
            throw new IOException("The live guard range exceeds the Dart document");
        }
        String sectionText = masked.substring(start, end);
        String openingPlaceholder = masked.substring(start, payloadStart);
        String closingPlaceholder = masked.substring(payloadEnd, end);
        String closingIndentation = masked.substring(payloadEnd, closingMarkerStart);
        String closingMarkerPlaceholder = masked.substring(closingMarkerStart, end);
        if (!openingPlaceholder.endsWith("\n")
                || closingPlaceholder.indexOf('\n') >= 0
                || openingPlaceholder.indexOf('\r') >= 0
                || closingPlaceholder.indexOf('\r') >= 0
                || closingMarkerPlaceholder.isEmpty()
                || !closingMarkerPlaceholder.isBlank()) {
            throw new IOException(
                    "Writable live Dart guards require LF-only marker lines");
        }
        return new ManagedSection(
                source.id(),
                source.sectionIdentity(),
                start,
                end,
                payloadStart,
                payloadEnd,
                openingPlaceholder,
                closingIndentation,
                closingPlaceholder,
                sectionText);
    }

    StyledDocument documentIdentity() {
        return documentIdentity;
    }

    long documentVersion() {
        return documentVersion;
    }

    byte[] markerBearingUtf8() {
        return markerBearingUtf8.clone();
    }

    String markerBearingSha256() {
        return markerBearingSha256;
    }

    ManagedSection imports() {
        return imports;
    }

    ManagedSection build() {
        return build;
    }

    boolean sameEvidence(LiveDartDocumentSnapshot other) {
        return other != null
                && documentIdentity == other.documentIdentity
                && documentVersion == other.documentVersion
                && markerBearingSha256.equals(other.markerBearingSha256)
                && Arrays.equals(markerBearingUtf8, other.markerBearingUtf8)
                && imports.sameIdentityAndRanges(other.imports)
                && build.sameIdentityAndRanges(other.build);
    }

    /**
     * Whether the two generated regions still contain the exact staged text.
     * Unmanaged Source edits may legitimately advance the document version;
     * they do not invalidate the .fd relationship as long as both managed
     * regions and their live section identities remain unchanged.
     */
    boolean sameManagedContent(LiveDartDocumentSnapshot other) {
        return other != null
                && documentIdentity == other.documentIdentity
                && imports.sameIdentityAndContent(other.imports)
                && build.sameIdentityAndContent(other.build);
    }

    String maskedContent() {
        return maskedContent;
    }

    static byte[] strictWritableUtf8(byte[] candidate) throws IOException {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.length > DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES) {
            throw new IOException("The Dart candidate exceeds the "
                    + DartSourceIntegrityLimits.DEFAULT_MAX_SOURCE_BYTES
                    + " byte writable safety limit");
        }
        if (candidate.length >= 3
                && (candidate[0] & 0xFF) == 0xEF
                && (candidate[1] & 0xFF) == 0xBB
                && (candidate[2] & 0xFF) == 0xBF) {
            throw new IOException("Writable Dart source must not contain a UTF-8 BOM");
        }
        String decoded;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(candidate))
                    .toString();
        } catch (CharacterCodingException ex) {
            throw new IOException("Writable Dart source must be strict UTF-8", ex);
        }
        if (decoded.indexOf('\r') >= 0) {
            throw new IOException("Writable Dart source must use LF line separators");
        }
        return candidate.clone();
    }

    private static byte[] strictWritableUtf8(char[] value) throws IOException {
        if (value.length > 0 && value[0] == '\uFEFF') {
            throw new IOException("Writable Dart source must not contain a UTF-8 BOM");
        }
        for (char character : value) {
            if (character == '\r') {
                throw new IOException("Writable Dart source must use LF line separators");
            }
        }
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        } catch (CharacterCodingException ex) {
            throw new IOException("Writable Dart source must be strict UTF-8", ex);
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "The Java runtime does not provide SHA-256", impossible);
        }
    }

    private static String reason(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.strip();
    }

    /** Exact live guard identity, ranges and masked marker lines. */
    static final class ManagedSection {
        private final String id;
        private final SimpleSection sectionIdentity;
        private final int sectionStartChar;
        private final int sectionEndChar;
        private final int payloadStartChar;
        private final int payloadEndChar;
        private final String openingPlaceholder;
        private final String closingIndentation;
        private final String closingPlaceholder;
        private final String originalSectionText;

        ManagedSection(
                String id,
                SimpleSection sectionIdentity,
                int sectionStartChar,
                int sectionEndChar,
                int payloadStartChar,
                int payloadEndChar,
                String openingPlaceholder,
                String closingIndentation,
                String closingPlaceholder,
                String originalSectionText) {
            this.id = Objects.requireNonNull(id, "id");
            this.sectionIdentity = Objects.requireNonNull(
                    sectionIdentity, "sectionIdentity");
            this.sectionStartChar = sectionStartChar;
            this.sectionEndChar = sectionEndChar;
            this.payloadStartChar = payloadStartChar;
            this.payloadEndChar = payloadEndChar;
            this.openingPlaceholder = Objects.requireNonNull(
                    openingPlaceholder, "openingPlaceholder");
            this.closingIndentation = Objects.requireNonNull(
                    closingIndentation, "closingIndentation");
            this.closingPlaceholder = Objects.requireNonNull(
                    closingPlaceholder, "closingPlaceholder");
            this.originalSectionText = Objects.requireNonNull(
                    originalSectionText, "originalSectionText");
        }

        String id() {
            return id;
        }

        SimpleSection sectionIdentity() {
            return sectionIdentity;
        }

        int sectionStartChar() {
            return sectionStartChar;
        }

        int sectionEndChar() {
            return sectionEndChar;
        }

        int payloadStartChar() {
            return payloadStartChar;
        }

        int payloadEndChar() {
            return payloadEndChar;
        }

        String originalSectionText() {
            return originalSectionText;
        }

        String replacementText(String generatedPayload) {
            Objects.requireNonNull(generatedPayload, "generatedPayload");
            // SimpleSection#setText retains the closing marker through the
            // guarded reader/writer. Its indentation is supplied as the final
            // whitespace-only line so reconstruction can preserve it exactly.
            return openingPlaceholder + generatedPayload + setTextClosingPrefix();
        }

        String originalReplacementText() {
            return originalSectionText.substring(
                    0,
                    originalSectionText.length()
                    - (closingPlaceholder.length() - closingIndentation.length()));
        }

        private String setTextClosingPrefix() {
            return closingIndentation;
        }

        private boolean sameIdentityAndRanges(ManagedSection other) {
            return other != null
                    && id.equals(other.id)
                    && sectionIdentity == other.sectionIdentity
                    && sectionStartChar == other.sectionStartChar
                    && sectionEndChar == other.sectionEndChar
                    && payloadStartChar == other.payloadStartChar
                    && payloadEndChar == other.payloadEndChar
                    && openingPlaceholder.equals(other.openingPlaceholder)
                    && closingIndentation.equals(other.closingIndentation)
                    && closingPlaceholder.equals(other.closingPlaceholder);
        }

        private boolean sameIdentityAndContent(ManagedSection other) {
            return other != null
                    && id.equals(other.id)
                    && sectionIdentity == other.sectionIdentity
                    && openingPlaceholder.equals(other.openingPlaceholder)
                    && closingIndentation.equals(other.closingIndentation)
                    && closingPlaceholder.equals(other.closingPlaceholder)
                    && originalSectionText.equals(other.originalSectionText);
        }
    }
}
