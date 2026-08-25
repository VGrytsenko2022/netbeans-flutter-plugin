package dev.flutter.netbeans.plugin.project;

import java.io.File;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.MoveOrRenameOperationImplementation;
import org.openide.filesystems.FileObject;

/** Preserves private Flutter state while NetBeans moves a project directory. */
final class FlutterProjectMoveOperation implements MoveOrRenameOperationImplementation {
    enum RecoveryResult {
        RECOVERED,
        BLOCKED
    }

    private static final Logger LOGGER = Logger.getLogger(
            FlutterProjectMoveOperation.class.getName());
    private static final ActiveMoveRegistry ACTIVE_MOVES = new ActiveMoveRegistry();

    private final FlutterProject project;
    private final FlutterProjectInformation information;
    private final PrivateStateWriter privateStateWriter;
    private final PrivatePreferencesFlusher privatePreferencesFlusher;
    private final FlutterMoveHandoff handoff;
    private Map<String, Object> privateStateSnapshot = Collections.emptyMap();
    private String pendingTransactionId;
    private String pendingSourceUri;
    private boolean movePending;
    private FlutterProjectMoveOperation pendingSource;
    private String pendingTargetDisplayName;
    private volatile boolean recoveryBlocked;
    private volatile boolean recoveryRenameAllowed;
    private volatile String recoveryBlockReason;

    FlutterProjectMoveOperation(
            FlutterProject project,
            FlutterProjectInformation information) {
        this(
                project,
                information,
                FlutterProjectMoveOperation::writePrivateState,
                FlutterProjectMoveOperation::flushPrivatePreferences);
    }

    FlutterProjectMoveOperation(
            FlutterProject project,
            FlutterProjectInformation information,
            PrivateStateWriter privateStateWriter) {
        this(
                project,
                information,
                privateStateWriter,
                FlutterProjectMoveOperation::flushPrivatePreferences);
    }

    FlutterProjectMoveOperation(
            FlutterProject project,
            FlutterProjectInformation information,
            PrivateStateWriter privateStateWriter,
            PrivatePreferencesFlusher privatePreferencesFlusher) {
        this.project = Objects.requireNonNull(project, "project");
        this.information = Objects.requireNonNull(information, "information");
        this.privateStateWriter = Objects.requireNonNull(
                privateStateWriter,
                "privateStateWriter");
        this.privatePreferencesFlusher = Objects.requireNonNull(
                privatePreferencesFlusher,
                "privatePreferencesFlusher");
        this.handoff = new FlutterMoveHandoff(project.getProjectDirectory());
    }

    @Override
    public List<FileObject> getMetadataFiles() {
        return List.of();
    }

    @Override
    public List<FileObject> getDataFiles() {
        return List.of(project.getProjectDirectory());
    }

    @Override
    public synchronized void notifyMoving() throws IOException {
        privatePreferencesFlusher.flush(project);
        Map<String, Object> snapshot = capturePrivateState();
        String sourceUri = projectUri();
        FlutterMoveHandoff.Transaction prepared = handoff.writePrepared(
                snapshot,
                sourceUri);
        FlutterMoveHandoff.Transaction persisted = handoff.read();
        if (persisted.phase() != FlutterMoveHandoff.Phase.PREPARED
                || !sourceUri.equals(persisted.sourceUri())
                || persisted.targetDisplayName() != null
                || !FlutterMoveHandoff.snapshotsEqual(
                        prepared.snapshot(),
                        persisted.snapshot())) {
            throw new IOException(
                    "Flutter move handoff changed during read-back verification");
        }
        privateStateSnapshot = persisted.snapshot();
        pendingTransactionId = persisted.transactionId();
        pendingSourceUri = persisted.sourceUri();
        movePending = true;
        ACTIVE_MOVES.register(persisted.transactionId(), this);
    }

    @Override
    public void notifyMoved(Project original, File originalPath, String newName)
            throws IOException {
        if (original == null) {
            return;
        }

        FlutterProjectMoveOperation source = original.getLookup()
                .lookup(FlutterProjectMoveOperation.class);
        if (source == null) {
            throw new IOException(
                    "Could not restore private Flutter project state after moving "
                            + project.getProjectDirectory().getPath()
                            + ": the source move service is unavailable");
        }
        rememberPendingMove(source, newName);
        FlutterMoveHandoff.Transaction transaction = handoff.read();
        source.verifyPending(transaction);
        if (transaction.phase() == FlutterMoveHandoff.Phase.COMMITTED) {
            requireTargetDisplayName(transaction, newName);
            information.reloadDisplayName();
        } else {
            if (transaction.phase() == FlutterMoveHandoff.Phase.PREPARED) {
                transaction = handoff.writeTargetIntent(transaction, newName);
            } else {
                requireTargetDisplayName(transaction, newName);
            }
            restoreTransaction(transaction);
            transaction = handoff.writeCommitted(transaction);
        }
        source.verifyPending(transaction);
        source.completeMove(transaction);
        clearPendingSource(source);
        handoff.delete();
        clearRecoveryBlock();
    }

    @Override
    public void notifyRenaming() {
    }

    @Override
    public void notifyRenamed(String newName) throws IOException {
        if (!handoff.exists()) {
            information.renameTo(newName);
            clearRecoveryBlock();
            return;
        }
        FlutterMoveHandoff.Transaction transaction = handoff.read();
        if (transaction.phase() != FlutterMoveHandoff.Phase.PREPARED) {
            throw new IOException(
                    "Cannot rename the Flutter project while move recovery phase "
                            + transaction.phase().xmlValue()
                            + " is pending");
        }
        if (transaction.sourceUri().equals(projectUri())) {
            verifyCurrentPrivateState(transaction.snapshot());
            information.reloadDisplayName();
            cancelPendingMove(transaction);
            handoff.delete();
            information.renameTo(newName);
            clearRecoveryBlock();
            return;
        }

        FlutterProjectMoveOperation source = pendingSource();
        if (source == null) {
            source = ACTIVE_MOVES.find(transaction.transactionId());
            if (source != null) {
                source.verifyPending(transaction);
                rememberRecoveredSource(source);
            }
        }
        if (source != null) {
            source.verifyPending(transaction);
        }
        transaction = handoff.writeTargetIntent(transaction, newName);
        restoreTransaction(transaction);
        transaction = handoff.writeCommitted(transaction);
        if (source != null) {
            source.completeMove(transaction);
            clearPendingSource(source);
        }
        handoff.delete();
        clearRecoveryBlock();
        notifyRecoveryResolved();
    }

    RecoveryResult recoverPendingHandoff() {
        try {
            if (!handoff.exists()) {
                return recovered();
            }
            FlutterMoveHandoff.Transaction transaction = handoff.read();
            FlutterProjectMoveOperation source = pendingSource();
            boolean preparedOnSource = transaction.phase()
                    == FlutterMoveHandoff.Phase.PREPARED
                    && transaction.sourceUri().equals(projectUri());
            if (source == null && !preparedOnSource) {
                source = ACTIVE_MOVES.find(transaction.transactionId());
                if (source != null) {
                    source.verifyPending(transaction);
                    rememberRecoveredSource(source);
                }
            }
            if (source != null) {
                source.verifyPending(transaction);
            }
            if (transaction.phase() == FlutterMoveHandoff.Phase.COMMITTED) {
                if (source != null) {
                    source.completeMove(transaction);
                    clearPendingSource(source);
                }
                information.reloadDisplayName();
                handoff.delete();
                return recovered();
            }
            if (preparedOnSource) {
                verifyCurrentPrivateState(transaction.snapshot());
                information.reloadDisplayName();
                cancelPendingMove(transaction);
                handoff.delete();
                return recovered();
            }

            String rememberedTargetName = pendingTargetDisplayName();
            if (transaction.phase() == FlutterMoveHandoff.Phase.PREPARED) {
                if (rememberedTargetName == null) {
                    restoreTransaction(transaction);
                    LOGGER.log(
                            Level.WARNING,
                            "Recovered private Flutter project state for {0}, but the "
                                    + "target display-name intent was unavailable. The "
                                    + "PREPARED move handoff was preserved; retry the move "
                                    + "completion or explicitly rename the target project.",
                            project.getProjectDirectory().getPath());
                    return blocked(
                            "the target project name was not durably recorded; "
                                    + "use Rename to finish the interrupted move",
                            true);
                }
                transaction = handoff.writeTargetIntent(
                        transaction,
                        rememberedTargetName);
            }
            restoreTransaction(transaction);
            if (transaction.phase() != FlutterMoveHandoff.Phase.COMMITTED) {
                transaction = handoff.writeCommitted(transaction);
            }
            if (source != null) {
                source.verifyPending(transaction);
                source.completeMove(transaction);
                clearPendingSource(source);
            }
            handoff.delete();
            return recovered();
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not recover pending private Flutter project state for "
                            + project.getProjectDirectory().getPath()
                            + "; the latest durable handoff phase was preserved and no unverified recovery "
                            + "was accepted: " + ex.getMessage(),
                    ex);
            return blocked(ex.getMessage(), false);
        }
    }

    boolean isRecoveryBlocked() {
        return recoveryBlocked;
    }

    String recoveryBlockReason() {
        String reason = recoveryBlockReason;
        return reason == null || reason.isBlank()
                ? "private Flutter project state recovery is incomplete"
                : reason;
    }

    boolean canResolveRecoveryByRename() {
        return recoveryBlocked && recoveryRenameAllowed;
    }

    private RecoveryResult recovered() {
        clearRecoveryBlock();
        return RecoveryResult.RECOVERED;
    }

    private RecoveryResult blocked(String reason, boolean renameAllowed) {
        recoveryBlockReason = reason;
        recoveryRenameAllowed = renameAllowed;
        recoveryBlocked = true;
        return RecoveryResult.BLOCKED;
    }

    private void clearRecoveryBlock() {
        recoveryBlocked = false;
        recoveryRenameAllowed = false;
        recoveryBlockReason = null;
    }

    private void notifyRecoveryResolved() {
        FlutterProjectLifecycle lifecycle = project.getLookup()
                .lookup(FlutterProjectLifecycle.class);
        if (lifecycle != null) {
            lifecycle.recoveryResolved();
        }
    }

    private synchronized void verifyPending(
            FlutterMoveHandoff.Transaction transaction)
            throws IOException {
        if (!movePending
                || !Objects.equals(
                        pendingTransactionId,
                        transaction.transactionId())
                || !Objects.equals(pendingSourceUri, transaction.sourceUri())
                || !FlutterMoveHandoff.snapshotsEqual(
                        privateStateSnapshot,
                        transaction.snapshot())) {
            throw new IOException(
                    "Persistent Flutter move handoff does not match the pending source state");
        }
    }

    private synchronized void completeMove(
            FlutterMoveHandoff.Transaction transaction)
            throws IOException {
        if (!movePending
                || !Objects.equals(
                        pendingTransactionId,
                        transaction.transactionId())
                || !Objects.equals(pendingSourceUri, transaction.sourceUri())
                || !FlutterMoveHandoff.snapshotsEqual(
                        privateStateSnapshot,
                        transaction.snapshot())) {
            throw new IOException(
                    "Private Flutter project state changed during the move; "
                            + "the source project was preserved");
        }
        project.state().notifyDeleted();
        ACTIVE_MOVES.unregister(pendingTransactionId, this);
        privateStateSnapshot = Collections.emptyMap();
        pendingTransactionId = null;
        pendingSourceUri = null;
        movePending = false;
    }

    private synchronized void cancelPendingMove(
            FlutterMoveHandoff.Transaction transaction)
            throws IOException {
        if (movePending
                && (!Objects.equals(
                        pendingTransactionId,
                        transaction.transactionId())
                        || !Objects.equals(pendingSourceUri, transaction.sourceUri())
                        || !FlutterMoveHandoff.snapshotsEqual(
                                privateStateSnapshot,
                                transaction.snapshot()))) {
            throw new IOException(
                    "Private Flutter project state changed while cancelling a move");
        }
        ACTIVE_MOVES.unregister(pendingTransactionId, this);
        privateStateSnapshot = Collections.emptyMap();
        pendingTransactionId = null;
        pendingSourceUri = null;
        movePending = false;
    }

    private synchronized void rememberPendingMove(
            FlutterProjectMoveOperation source,
            String targetDisplayName) {
        pendingSource = source;
        pendingTargetDisplayName = targetDisplayName;
    }

    private synchronized FlutterProjectMoveOperation pendingSource() {
        return pendingSource;
    }

    private synchronized void rememberRecoveredSource(
            FlutterProjectMoveOperation source) {
        if (pendingSource == null) {
            pendingSource = source;
        }
    }

    private synchronized String pendingTargetDisplayName() {
        return pendingTargetDisplayName;
    }

    private synchronized void clearPendingSource(
            FlutterProjectMoveOperation completedSource) {
        if (pendingSource == completedSource) {
            pendingSource = null;
            pendingTargetDisplayName = null;
        }
    }

    private void restoreTransaction(FlutterMoveHandoff.Transaction transaction)
            throws IOException {
        restoreAndVerify(transaction.snapshot());
        String targetDisplayName = transaction.targetDisplayName();
        if (targetDisplayName == null) {
            information.reloadDisplayName();
            return;
        }
        information.renameTo(targetDisplayName);
        if (!information.isDisplayNamePersisted(targetDisplayName)) {
            throw new IOException(
                    "Could not verify the Flutter project display name after moving "
                            + project.getProjectDirectory().getPath());
        }
    }

    private static void requireTargetDisplayName(
            FlutterMoveHandoff.Transaction transaction,
            String newName) throws IOException {
        String normalized = newName == null ? "" : newName.strip();
        if (transaction.targetDisplayName() == null
                || !transaction.targetDisplayName().equals(normalized)) {
            throw new IOException(
                    "The pending Flutter move target does not match " + newName);
        }
    }

    private void verifyCurrentPrivateState(Map<String, Object> expected)
            throws IOException {
        Map<String, Object> current = capturePrivateState();
        if (!FlutterMoveHandoff.snapshotsEqual(expected, current)) {
            throw new IOException(
                    "The source Flutter project state changed after move preparation");
        }
    }

    private Map<String, Object> capturePrivateState() {
        FileObject directory = project.getProjectDirectory();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Object primaryMetadata = directory.getAttribute(
                FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE);
        if (primaryMetadata != null) {
            snapshot.put(
                    FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE,
                    primaryMetadata);
        }
        Enumeration<String> attributes = directory.getAttributes();
        while (attributes.hasMoreElements()) {
            String attribute = attributes.nextElement();
            if (isPrivateStateAttribute(attribute)) {
                Object value = directory.getAttribute(attribute);
                if (value != null) {
                    snapshot.put(attribute, value);
                }
            }
        }
        return snapshot;
    }

    private String projectUri() {
        return project.getProjectDirectory().toURI().normalize().toASCIIString();
    }

    private void restoreAndVerify(Map<String, Object> snapshot)
            throws IOException {
        FileObject destination = project.getProjectDirectory();
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            // Reapply the modifier even when a provider copied the same logical value:
            // the target may otherwise have persisted it as a regular attribute.
            privateStateWriter.write(destination, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            if (!FlutterMoveHandoff.valuesEqual(
                    entry.getValue(),
                    destination.getAttribute(entry.getKey()))) {
                throw new IOException(
                        "Could not verify restored private Flutter project state attribute "
                                + entry.getKey() + " after moving "
                                + destination.getPath());
            }
        }
    }

    private static boolean isPrivateStateAttribute(String attribute) {
        return FlutterProjectMetadata.PRIVATE_METADATA_ATTRIBUTE.equals(attribute)
                || attribute.startsWith(
                        FlutterProjectMetadata.LEGACY_VALUE_QUARANTINE_PREFIX)
                || attribute.startsWith(
                        FlutterProjectMetadata.EXTERNAL_PAYLOAD_QUARANTINE_PREFIX);
    }

    private static void writePrivateState(
            FileObject destination,
            String attribute,
            Object value) throws IOException {
        destination.setAttribute(
                FlutterProjectMetadata.TRANSIENT_ATTRIBUTE_PREFIX + attribute,
                value);
    }

    private static void flushPrivatePreferences(FlutterProject project)
            throws IOException {
        try {
            ProjectUtils.getPreferences(
                    project,
                    FlutterRunController.class,
                    false).flush();
        } catch (BackingStoreException ex) {
            throw new IOException(
                    "Could not flush private Flutter project settings before moving "
                            + project.getProjectDirectory().getPath(),
                    ex);
        }
    }

    private static final class ActiveMoveRegistry {
        private final ConcurrentMap<String, SourceReference> sources =
                new ConcurrentHashMap<>();
        private final ReferenceQueue<FlutterProjectMoveOperation> collected =
                new ReferenceQueue<>();

        void register(String transactionId, FlutterProjectMoveOperation source) {
            cleanCollected();
            sources.put(
                    transactionId,
                    new SourceReference(transactionId, source, collected));
        }

        FlutterProjectMoveOperation find(String transactionId) {
            cleanCollected();
            SourceReference reference = sources.get(transactionId);
            if (reference == null) {
                return null;
            }
            FlutterProjectMoveOperation source = reference.get();
            if (source == null) {
                sources.remove(transactionId, reference);
            }
            return source;
        }

        void unregister(String transactionId, FlutterProjectMoveOperation source) {
            if (transactionId == null) {
                return;
            }
            SourceReference reference = sources.get(transactionId);
            if (reference != null && reference.get() == source) {
                sources.remove(transactionId, reference);
            }
            cleanCollected();
        }

        private void cleanCollected() {
            SourceReference reference;
            while ((reference = (SourceReference) collected.poll()) != null) {
                sources.remove(reference.transactionId, reference);
            }
        }
    }

    private static final class SourceReference
            extends WeakReference<FlutterProjectMoveOperation> {
        private final String transactionId;

        SourceReference(
                String transactionId,
                FlutterProjectMoveOperation source,
                ReferenceQueue<FlutterProjectMoveOperation> collected) {
            super(source, collected);
            this.transactionId = transactionId;
        }
    }

    @FunctionalInterface
    interface PrivateStateWriter {
        void write(FileObject destination, String attribute, Object value)
                throws IOException;
    }

    @FunctionalInterface
    interface PrivatePreferencesFlusher {
        void flush(FlutterProject project) throws IOException;
    }
}
