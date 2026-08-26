package dev.flutter.netbeans.plugin.designer.guard;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.guards.GuardedSection;
import org.netbeans.api.editor.guards.GuardedSectionManager;
import org.netbeans.api.editor.guards.SimpleSection;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.netbeans.spi.editor.guards.support.AbstractGuardedSectionsProvider;

/**
 * Persists guarded regions in Dart comments without changing source offsets.
 *
 * <p>The marker text is replaced with equal-length spaces while the document is
 * loaded. The exact marker text is restored on save. Keeping both the character
 * count and line separators stable is important for Dart LSP source offsets.</p>
 */
public final class DartGuardedSectionsProvider extends AbstractGuardedSectionsProvider {

    private final GuardedEditorSupport editor;
    private final ThreadLocal<SaveContext> activeSave = new ThreadLocal<>();
    private final ThreadLocal<PersistenceAttempt> explicitAttempt = new ThreadLocal<>();
    private final Object persistenceLock = new Object();
    private boolean initialReadObserved;
    private PersistenceAttempt openAttempt;
    private volatile PersistenceSnapshot persistenceSnapshot;

    public DartGuardedSectionsProvider(GuardedEditorSupport editor) {
        super(editor, true);
        this.editor = editor;
    }

    /** Whether a marker-bearing baseline exists for an explicit two-phase save. */
    public boolean supportsExplicitPersistenceAttempt() {
        return persistenceSnapshot != null;
    }

    /**
     * Starts a same-thread, two-phase persistence attempt.
     *
     * <p>A guarded writer created on this thread stages its reconstructed
     * marker-bearing snapshot instead of advancing the provider fallback. The
     * caller must invoke {@link PersistenceAttempt#commit()} only after the
     * actual output operation has succeeded. Closing or aborting the attempt
     * retains the old fallback. Writers created without an explicit attempt
     * keep the standard one-phase behavior used by generic NetBeans clients.</p>
     *
     * @return the only active attempt for this provider
     * @throws IllegalStateException before the initial guarded read, or when
     *         another explicit attempt is active
     */
    public PersistenceAttempt beginPersistenceAttempt() {
        if (explicitAttempt.get() != null) {
            throw new IllegalStateException(
                    "A Dart guarded-section persistence attempt is already active "
                    + "on this thread");
        }
        synchronized (persistenceLock) {
            if (openAttempt != null) {
                throw new IllegalStateException(
                        "A Dart guarded-section persistence attempt is already active");
            }
            PersistenceSnapshot fallback = persistenceSnapshot;
            if (fallback == null) {
                throw new IllegalStateException(
                        "The initial marker-bearing persistence snapshot is unavailable");
            }
            PersistenceAttempt attempt = new PersistenceAttempt(
                    Thread.currentThread(), fallback);
            openAttempt = attempt;
            explicitAttempt.set(attempt);
            return attempt;
        }
    }

    @Override
    public Result readSections(char[] content) {
        boolean initialRead = claimInitialRead();
        DartGuardedSectionMarkers.ParseResult parsed = DartGuardedSectionMarkers.parse(content);
        if (!parsed.valid() || parsed.regions().isEmpty()) {
            return new Result(parsed.content(), List.of());
        }

        List<GuardedSection> sections = new ArrayList<>(parsed.regions().size());
        try {
            for (DartGuardedSectionMarkers.Region region : parsed.regions()) {
                sections.add(createSimpleSection(
                        region.id(),
                        region.startOffset(),
                        region.endOffset()));
            }
        } catch (BadLocationException | IllegalArgumentException ex) {
            // Never expose masked marker text without a matching guard.
            return new Result(content.clone(), List.of());
        }
        if (initialRead) {
            captureInitialSnapshot(content, parsed.regions());
        }
        return new Result(parsed.content(), List.copyOf(sections));
    }

    @Override
    public char[] writeSections(List<GuardedSection> sections, char[] content) {
        SaveContext context = activeSave.get();
        if (context != null && context.recovery()) {
            // The supplied content is the last known marker-bearing source.
            // Returning it unchanged lets NetBeans apply its normal charset and
            // newline conversion without asking the invalid live guard state to
            // reconstruct anything.
            return content.clone();
        }

        String structuralFailure = validateSections(sections);
        if (structuralFailure == null && context != null) {
            structuralFailure = validateExpectedIds(context.fallback(), sections);
        }
        if (structuralFailure != null) {
            return failPersistence(context, structuralFailure);
        }

        char[] restored;
        try {
            restored = DartGuardedSectionMarkers.write(sections, content);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return failPersistence(context, "guarded-section positions changed during save");
        }
        if (restored == null) {
            return failPersistence(context, "guard offsets cannot be restored safely");
        }
        if (context != null) {
            List<String> persistedIds;
            try {
                persistedIds = orderedSectionIds(sections);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return failPersistence(context, "guarded-section positions changed during save");
            }
            context.persisted(new PersistenceSnapshot(
                    restored.clone(),
                    persistedIds));
        }
        return restored;
    }

    /**
     * NetBeans normally bypasses {@link #writeSections} after the last guarded
     * section is removed. Wrap the standard writer so that this bypass cannot
     * persist the in-document marker placeholders as spaces.
     */
    @Override
    public Writer createGuardedWriter(OutputStream stream, Charset charset) {
        PersistenceAttempt attempt = explicitAttempt.get();
        if (attempt != null) {
            attempt.claimWriter();
        }
        PersistenceSnapshot fallback = attempt == null
                ? persistenceSnapshot
                : attempt.baseline();
        if (fallback == null) {
            return super.createGuardedWriter(stream, charset);
        }

        String stateFailure = validateLiveState(fallback);
        Writer delegate = super.createGuardedWriter(stream, charset);
        GuardedPersistenceRejectionSink rejectionSink =
                stream instanceof GuardedPersistenceRejectionSink sink
                        ? sink : null;
        SaveContext context = new SaveContext(
                fallback, stateFailure != null, rejectionSink);
        return new FailClosedWriter(delegate, context, stateFailure, attempt);
    }

    /**
     * Reconstructs the exact marker-bearing live document without advancing the
     * provider's persistence fallback. The returned section objects are the
     * exact identities installed in the supplied document.
     *
     * <p>This is a read-only bridge for the Flutter Designer live-document
     * gate. It deliberately does not call {@link #writeSections}, create a save
     * context or update {@link #persistenceSnapshot}.</p>
     *
     * @param document the exact live document owned by this provider
     * @return one coherent masked/marker-bearing character snapshot
     * @throws IllegalStateException when the live guards cannot be
     *         reconstructed reversibly
     */
    public MarkerBearingSnapshot reconstructMarkerBearingSnapshot(
            StyledDocument document) {
        Objects.requireNonNull(document, "document");
        if (editor.getDocument() != document) {
            throw new IllegalStateException(
                    "The guarded provider is not bound to the supplied live document");
        }

        AtomicReference<MarkerBearingSnapshot> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        document.render(() -> {
            try {
                result.set(reconstructMarkerBearingSnapshotLocked(document));
            } catch (RuntimeException ex) {
                failure.set(ex);
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
        MarkerBearingSnapshot reconstructed = result.get();
        if (reconstructed == null) {
            throw new IllegalStateException(
                    "The guarded document snapshot could not be reconstructed");
        }
        return reconstructed;
    }

    private MarkerBearingSnapshot reconstructMarkerBearingSnapshotLocked(
            StyledDocument document) {
        PersistenceSnapshot fallback = persistenceSnapshot;
        if (fallback == null) {
            throw new IllegalStateException(
                    "The initial marker-bearing persistence snapshot is unavailable");
        }

        GuardedSectionManager manager = GuardedSectionManager.getInstance(document);
        if (manager == null) {
            throw new IllegalStateException("The guarded-section manager is unavailable");
        }
        List<GuardedSection> sections = new ArrayList<>();
        manager.getGuardedSections().forEach(sections::add);
        sections.sort(Comparator.comparingInt(
                section -> section.getStartPosition().getOffset()));

        String structuralFailure = validateSections(sections);
        if (structuralFailure == null) {
            structuralFailure = validateExpectedIds(fallback, sections);
        }
        if (structuralFailure != null) {
            throw new IllegalStateException(
                    "Cannot reconstruct Dart guarded sections: " + structuralFailure);
        }

        char[] masked;
        try {
            masked = document.getText(0, document.getLength()).toCharArray();
        } catch (BadLocationException impossible) {
            throw new IllegalStateException(
                    "The guarded document text is unavailable", impossible);
        }
        char[] markerBearing = DartGuardedSectionMarkers.write(sections, masked);
        if (markerBearing == null) {
            throw new IllegalStateException(
                    "Cannot reconstruct Dart guarded-section marker positions");
        }

        DartGuardedSectionMarkers.ParseResult reparsed =
                DartGuardedSectionMarkers.parse(markerBearing);
        if (!reparsed.valid()
                || !Arrays.equals(masked, reparsed.content())
                || reparsed.regions().size() != sections.size()) {
            throw new IllegalStateException(
                    "The marker-bearing reconstruction is not reversible");
        }

        List<MarkerSectionSnapshot> reconstructedSections = new ArrayList<>(
                sections.size());
        for (int index = 0; index < sections.size(); index++) {
            GuardedSection guarded = sections.get(index);
            if (!(guarded instanceof SimpleSection simple)) {
                throw new IllegalStateException(
                        "Only simple Dart guarded sections can be reconstructed");
            }
            DartGuardedSectionMarkers.Region region = reparsed.regions().get(index);
            int actualStart = simple.getStartPosition().getOffset();
            int actualEnd = simple.getEndPosition().getOffset();
            if (!simple.getName().equals(region.id())
                    || actualStart != region.startOffset()
                    || actualEnd != region.endOffset()) {
                throw new IllegalStateException(
                        "The live guarded-section identities and reconstructed ranges disagree");
            }
            int payloadStart = DartGuardedSectionMarkers.payloadStartOffset(
                    markerBearing, region);
            int payloadEnd = DartGuardedSectionMarkers.payloadEndOffset(
                    markerBearing, region);
            int closingMarkerStart = actualEnd + 1
                    - DartGuardedSectionMarkers.CLOSE_MARKER.length();
            if (payloadStart < actualStart
                    || payloadEnd < payloadStart
                    || closingMarkerStart < payloadEnd
                    || closingMarkerStart > actualEnd + 1) {
                throw new IllegalStateException(
                        "The reconstructed guarded payload range is invalid");
            }
            reconstructedSections.add(new MarkerSectionSnapshot(
                    region.id(),
                    simple,
                    actualStart,
                    actualEnd + 1,
                    payloadStart,
                    payloadEnd,
                    closingMarkerStart));
        }
        return new MarkerBearingSnapshot(
                masked,
                markerBearing,
                reconstructedSections);
    }

    private synchronized boolean claimInitialRead() {
        if (initialReadObserved) {
            return false;
        }
        initialReadObserved = true;
        return true;
    }

    private synchronized void captureInitialSnapshot(
            char[] content,
            List<DartGuardedSectionMarkers.Region> regions) {
        if (persistenceSnapshot == null && !regions.isEmpty()) {
            persistenceSnapshot = new PersistenceSnapshot(
                    content.clone(),
                    regions.stream().map(DartGuardedSectionMarkers.Region::id).toList());
        }
    }

    private String validateLiveState(PersistenceSnapshot expected) {
        try {
            StyledDocument document = editor.getDocument();
            if (document == null) {
                return "guarded document is unavailable";
            }
            GuardedSectionManager manager = GuardedSectionManager.getInstance(document);
            if (manager == null) {
                return "guarded-section manager is unavailable";
            }

            List<GuardedSection> sections = new ArrayList<>();
            manager.getGuardedSections().forEach(sections::add);
            String structuralFailure = validateSections(sections);
            if (structuralFailure != null) {
                return structuralFailure;
            }
            return validateExpectedIds(expected, sections);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return "guarded-section state is unavailable";
        }
    }

    private static String validateSections(List<GuardedSection> sections) {
        if (sections == null) {
            return "guarded-section list is unavailable";
        }
        List<DartGuardedSectionMarkers.Region> regions = new ArrayList<>(sections.size());
        try {
            for (GuardedSection section : sections) {
                if (!(section instanceof SimpleSection)) {
                    return "only simple Dart guarded sections can be persisted";
                }
                if (!section.isValid()) {
                    return "guarded section " + section.getName() + " is invalid";
                }
                int start = section.getStartPosition().getOffset();
                int end = section.getEndPosition().getOffset();
                regions.add(new DartGuardedSectionMarkers.Region(
                        section.getName(), start, end));
            }
            regions.sort(Comparator.comparingInt(
                    DartGuardedSectionMarkers.Region::startOffset));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return "guarded-section positions are unavailable";
        }
        return DartGuardedSectionMarkers.validateRegions(regions);
    }

    private static String validateExpectedIds(
            PersistenceSnapshot expected,
            List<GuardedSection> sections) {
        List<String> actualIds;
        try {
            actualIds = orderedSectionIds(sections);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return "guarded-section positions are unavailable";
        }
        if (!actualIds.equals(expected.regionIds())) {
            return "guarded sections changed unexpectedly: expected "
                    + expected.regionIds() + " but found " + actualIds;
        }
        return null;
    }

    private static List<String> orderedSectionIds(List<GuardedSection> sections) {
        return sections.stream()
                .sorted(Comparator.comparingInt(section ->
                    section.getStartPosition().getOffset()))
                .map(GuardedSection::getName)
                .toList();
    }

    private char[] failPersistence(SaveContext context, String reason) {
        if (context == null) {
            throw new IllegalStateException("Cannot persist Dart guarded sections: " + reason);
        }
        context.failure(reason);
        return context.fallback().content();
    }

    private final class FailClosedWriter extends Writer {
        private final Writer delegate;
        private final SaveContext context;
        private final String preflightFailure;
        private final PersistenceAttempt attempt;
        private boolean closed;

        FailClosedWriter(
                Writer delegate,
                SaveContext context,
                String preflightFailure,
                PersistenceAttempt attempt) {
            this.delegate = delegate;
            this.context = context;
            this.preflightFailure = preflightFailure;
            this.attempt = attempt;
        }

        @Override
        public void write(char[] characters, int offset, int length) throws IOException {
            ensureOpen();
            if (preflightFailure == null) {
                delegate.write(characters, offset, length);
            }
        }

        @Override
        public void flush() throws IOException {
            ensureOpen();
            if (preflightFailure == null) {
                delegate.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;

            SaveContext previous = activeSave.get();
            if (previous != null) {
                throw new IOException("Nested Dart guarded-section save is not supported");
            }
            activeSave.set(context);
            try {
                if (preflightFailure != null) {
                    context.failure(preflightFailure);
                    delegate.write(context.fallback().content());
                }
                delegate.close();
            } catch (RuntimeException ex) {
                throw new IOException("Cannot persist Dart guarded sections safely", ex);
            } finally {
                activeSave.remove();
            }

            String failure = preflightFailure != null
                    ? preflightFailure
                    : context.failure();
            if (failure != null) {
                throw new IOException("Cannot persist Dart guarded sections: " + failure);
            }
            PersistenceSnapshot persisted = context.persisted();
            if (persisted != null) {
                if (attempt == null) {
                    synchronized (persistenceLock) {
                        persistenceSnapshot = persisted;
                    }
                } else {
                    attempt.stage(persisted);
                }
            }
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Dart guarded-section writer is closed");
            }
        }
    }

    private record PersistenceSnapshot(char[] content, List<String> regionIds) {
        PersistenceSnapshot {
            content = content.clone();
            regionIds = List.copyOf(regionIds);
        }

        @Override
        public char[] content() {
            return content.clone();
        }
    }

    /**
     * One same-thread two-phase guarded persistence scope. A completed writer
     * only stages a fallback; {@link #commit()} publishes it.
     */
    public final class PersistenceAttempt implements AutoCloseable {
        private final Thread owner;
        private final PersistenceSnapshot baseline;
        private PersistenceSnapshot staged;
        private boolean writerClaimed;
        private boolean finished;

        private PersistenceAttempt(Thread owner, PersistenceSnapshot baseline) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.baseline = Objects.requireNonNull(baseline, "baseline");
        }

        /**
         * Publishes the staged fallback after the caller has durably completed
         * its actual output operation.
         */
        public void commit() {
            requireOwner();
            synchronized (persistenceLock) {
                requireOpen();
                if (staged == null) {
                    throw new IllegalStateException(
                            "The explicit persistence writer has not staged a "
                            + "marker-bearing snapshot");
                }
                if (persistenceSnapshot != baseline) {
                    finishLocked();
                    throw new IllegalStateException(
                            "The guarded persistence fallback changed during the "
                            + "explicit attempt");
                }
                persistenceSnapshot = staged;
                finishLocked();
            }
        }

        /** Retains the old fallback and releases this attempt. */
        public void abort() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException(
                        "The Dart guarded-section persistence attempt belongs to "
                        + "its creating thread");
            }
            synchronized (persistenceLock) {
                if (finished) {
                    return;
                }
                requireOpen();
                finishLocked();
            }
        }

        /** Equivalent to {@link #abort()} unless already committed or aborted. */
        @Override
        public void close() {
            abort();
        }

        private PersistenceSnapshot baseline() {
            requireOwner();
            synchronized (persistenceLock) {
                requireOpen();
                return baseline;
            }
        }

        private void claimWriter() {
            requireOwner();
            synchronized (persistenceLock) {
                requireOpen();
                if (writerClaimed) {
                    throw new IllegalStateException(
                            "An explicit persistence attempt supports one writer");
                }
                writerClaimed = true;
            }
        }

        private void stage(PersistenceSnapshot candidate) throws IOException {
            requireOwner();
            synchronized (persistenceLock) {
                if (finished || openAttempt != this) {
                    throw new IOException(
                            "The explicit Dart persistence attempt is no longer active");
                }
                if (staged != null) {
                    throw new IOException(
                            "The explicit Dart persistence attempt is already staged");
                }
                staged = Objects.requireNonNull(candidate, "candidate");
            }
        }

        private void requireOwner() {
            if (Thread.currentThread() != owner
                    || explicitAttempt.get() != this) {
                throw new IllegalStateException(
                        "The Dart guarded-section persistence attempt belongs to "
                        + "its creating thread");
            }
        }

        private void requireOpen() {
            if (finished || openAttempt != this) {
                throw new IllegalStateException(
                        "The Dart guarded-section persistence attempt is closed");
            }
        }

        private void finishLocked() {
            finished = true;
            staged = null;
            openAttempt = null;
            explicitAttempt.remove();
        }
    }

    /** Immutable character snapshot produced without entering the save path. */
    public static final class MarkerBearingSnapshot {
        private final char[] maskedContent;
        private final char[] markerBearingContent;
        private final List<MarkerSectionSnapshot> sections;

        MarkerBearingSnapshot(
                char[] maskedContent,
                char[] markerBearingContent,
                List<MarkerSectionSnapshot> sections) {
            this.maskedContent = maskedContent.clone();
            this.markerBearingContent = markerBearingContent.clone();
            this.sections = List.copyOf(sections);
        }

        public char[] maskedContent() {
            return maskedContent.clone();
        }

        public char[] markerBearingContent() {
            return markerBearingContent.clone();
        }

        public List<MarkerSectionSnapshot> sections() {
            return sections;
        }
    }

    /** One verified live simple-section identity and its character ranges. */
    public record MarkerSectionSnapshot(
            String id,
            SimpleSection sectionIdentity,
            int sectionStartChar,
            int sectionEndChar,
            int payloadStartChar,
            int payloadEndChar,
            int closingMarkerStartChar) {
        public MarkerSectionSnapshot {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(sectionIdentity, "sectionIdentity");
            if (sectionStartChar < 0
                    || sectionEndChar <= sectionStartChar
                    || payloadStartChar < sectionStartChar
                    || payloadEndChar < payloadStartChar
                    || closingMarkerStartChar < payloadEndChar
                    || closingMarkerStartChar > sectionEndChar) {
                throw new IllegalArgumentException(
                        "The guarded-section character ranges are invalid");
            }
        }
    }

    private static final class SaveContext {
        private final PersistenceSnapshot fallback;
        private final boolean recovery;
        private final GuardedPersistenceRejectionSink rejectionSink;
        private String failure;
        private PersistenceSnapshot persisted;

        SaveContext(
                PersistenceSnapshot fallback,
                boolean recovery,
                GuardedPersistenceRejectionSink rejectionSink) {
            this.fallback = fallback;
            this.recovery = recovery;
            this.rejectionSink = rejectionSink;
        }

        PersistenceSnapshot fallback() {
            return fallback;
        }

        boolean recovery() {
            return recovery;
        }

        String failure() {
            return failure;
        }

        void failure(String failure) {
            this.failure = failure;
            if (rejectionSink != null) {
                rejectionSink.rejectGuardedPersistence(failure);
            }
        }

        PersistenceSnapshot persisted() {
            return persisted;
        }

        void persisted(PersistenceSnapshot persisted) {
            this.persisted = persisted;
        }
    }
}
