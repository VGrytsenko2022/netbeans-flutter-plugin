package dev.flutter.netbeans.designer.transition;

import dev.flutter.netbeans.designer.events.DartEventHandlerSource;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.source.DartManagedRegionSnapshot;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A bounded, immutable proof of deliberate changes to user-owned Dart members.
 * Generated contributions are lexical handler insertion and rename. Explicitly
 * observed native Source edits may also be retained after proving that every
 * complete managed section is byte-identical. History can compose, invert and
 * rebase these contributions; none of these operations authorizes a write.
 *
 * <p>This is not write authorization. The complete candidate still requires
 * source-integrity, Dart analyzer and exact live revision checks.</p>
 */
public final class DartUserSourceProjection {
    private final Envelope before;
    private final Envelope after;
    private final byte[] targetSource;
    private final DartSourceDescriptor targetDescriptor;
    private final List<Step> steps;

    private DartUserSourceProjection(Envelope before, Envelope after,
            byte[] targetSource, DartSourceDescriptor targetDescriptor) {
        this(before, after, targetSource, targetDescriptor,
                before.same(after) ? List.of() : List.of(new Step(before, after, null)));
    }

    private DartUserSourceProjection(Envelope before, Envelope after,
            byte[] targetSource, DartSourceDescriptor targetDescriptor, List<Step> steps) {
        this.before = before;
        this.after = after;
        this.targetSource = targetSource.clone();
        this.targetDescriptor = targetDescriptor;
        this.steps = List.copyOf(steps);
    }

    /** Establishes an unchanged, scanner-verified user-source envelope. */
    public static DartUserSourceProjection identity(
            byte[] source, DartSourceDescriptor descriptor) {
        Envelope envelope = envelope(scan(source, descriptor));
        return new DartUserSourceProjection(envelope, envelope, source, descriptor);
    }

    /**
     * Records an already observed native Source edit without touching any file
     * or editor. Both exact snapshots must retain the same verified owner and
     * byte-identical managed payloads and marker lines. This is deliberately
     * distinct from an authored handler insertion: the caller must establish
     * that {@code afterSource} is the exact currently accepted live document,
     * retain its revision identity, and analyze the complete prospective source
     * before publishing a command. Merely constructing this proof grants no
     * permission to replace user source or bypass the live-document gate.
     */
    public static DartUserSourceProjection observedEdit(
            byte[] beforeSource, byte[] afterSource, DartSourceDescriptor descriptor) {
        requireBoundedSize(Objects.requireNonNull(beforeSource, "beforeSource"));
        requireBoundedSize(Objects.requireNonNull(afterSource, "afterSource"));
        byte[] original = beforeSource.clone();
        byte[] edited = afterSource.clone();
        requireWritableFormat(original);
        requireWritableFormat(edited);
        DartSourceIntegrityResult beforeEvidence = scan(original, descriptor);
        DartSourceIntegrityResult afterEvidence = scan(edited, descriptor);
        requireSameMemberOwner(beforeEvidence, afterEvidence);
        for (int i = 0; i < beforeEvidence.regions().size(); i++) {
            if (!Arrays.equals(guardedBytes(original, beforeEvidence.regions().get(i)),
                    guardedBytes(edited, afterEvidence.regions().get(i)))) {
                throw new IllegalArgumentException(
                        "Observed Source edits must retain every managed payload and marker byte exactly.");
            }
        }
        return new DartUserSourceProjection(
                envelope(beforeEvidence), envelope(afterEvidence), edited, descriptor);
    }

    /** Adds exactly one concrete instance method outside generated regions. */
    public DartUserSourceProjection insertHandler(
            String ownerClass, String handlerName, String methodSource) {
        return insertHandler(ownerClass, handlerName, methodSource, List.of());
    }

    /** Adds signature imports as user-owned Dart directives in the same proof. */
    public DartUserSourceProjection insertHandler(
            String ownerClass, String handlerName, String methodSource, List<String> importUris) {
        requireOwner(ownerClass);
        byte[] updated = DartEventHandlerSource.insert(
                targetSource, ownerClass, handlerName, methodSource);
        updated = DartEventHandlerSource.addImports(updated, importUris);
        Envelope updatedEnvelope = envelope(scan(updated, targetDescriptor));
        return append(updatedEnvelope, updated, null);
    }

    /** Adds the closed starter header class; its future edits remain user-owned. */
    public DartUserSourceProjection insertPersistentHeaderDelegate() {
        byte[] updated = DartEventHandlerSource.insertPersistentHeaderDelegate(targetSource);
        Envelope updatedEnvelope = envelope(scan(updated, targetDescriptor));
        return append(updatedEnvelope, updated, null);
    }

    /** Adds only a reviewed typed State field and its update handler as one source contribution. */
    public DartUserSourceProjection insertStateBinding(
            String ownerClass, dev.flutter.netbeans.designer.model.WidgetNode widget) {
        return insertStateBinding(ownerClass, widget, "", false);
    }

    public DartUserSourceProjection insertStateBinding(String ownerClass,
            dev.flutter.netbeans.designer.model.WidgetNode widget, String initialText, boolean reuseExistingField) {
        requireOwner(ownerClass);
        if (targetDescriptor.widgetKind() != dev.flutter.netbeans.designer.model.WidgetClassKind.STATEFUL) {
            throw new IllegalArgumentException("State binding requires a verified Stateful source owner.");
        }
        byte[] updated = DartEventHandlerSource.insertStateBinding(targetSource, ownerClass, widget, initialText, reuseExistingField);
        Envelope updatedEnvelope = envelope(scan(updated, targetDescriptor));
        return append(updatedEnvelope, updated, null);
    }

    /** Renames only a lexically unambiguous user method and its references. */
    public DartUserSourceProjection renameHandler(
            String ownerClass, String oldName, String newName) {
        requireOwner(ownerClass);
        byte[] updated = DartEventHandlerSource.rename(
                targetSource, ownerClass, oldName, newName);
        Envelope updatedEnvelope = envelope(scan(updated, targetDescriptor));
        return append(updatedEnvelope, updated, null);
    }

    /** Renames only one verified directly declared State field and its owned references. */
    public DartUserSourceProjection renameStateField(String ownerClass,
            dev.flutter.netbeans.designer.model.StatePropertyBinding field, String newName) {
        requireOwner(ownerClass);
        if (targetDescriptor.widgetKind() != dev.flutter.netbeans.designer.model.WidgetClassKind.STATEFUL) {
            throw new IllegalArgumentException("State field rename requires a verified Stateful source owner.");
        }
        byte[] updated = DartEventHandlerSource.renameStateField(targetSource, ownerClass, field, newName);
        Envelope updatedEnvelope = envelope(scan(updated, targetDescriptor));
        return append(updatedEnvelope, updated, new FieldRename(ownerClass, field, newName));
    }

    private DartUserSourceProjection append(Envelope next, byte[] source, FieldRename rename) {
        var sequence = new ArrayList<>(steps);
        if (!after.same(next)) sequence.add(new Step(after, next, rename));
        return new DartUserSourceProjection(before, next, source, targetDescriptor, sequence);
    }

    /** Reverses precisely this contribution; unrelated user edits are not lost. */
    public DartUserSourceProjection inverse() {
        byte[] reversed = replaceEnvelope(scan(targetSource, targetDescriptor), before);
        var sequence = new ArrayList<Step>(steps.size());
        for (int index = steps.size() - 1; index >= 0; index--) sequence.add(steps.get(index).inverse());
        return new DartUserSourceProjection(after, before, reversed, targetDescriptor, sequence);
    }

    /** Composes proofs only when their exact user-envelope endpoints agree. */
    public DartUserSourceProjection then(DartUserSourceProjection next) {
        Objects.requireNonNull(next, "next");
        requireSameOwner(next.targetDescriptor);
        if (!after.same(next.before)) {
            throw new IllegalArgumentException(
                    "User-source projection endpoints do not match exactly.");
        }
        var sequence = new ArrayList<>(steps);
        sequence.addAll(next.steps);
        return new DartUserSourceProjection(
                before, next.after, next.targetSource, next.targetDescriptor, sequence);
    }

    /**
     * Retains this authorized delta against a newly observed source envelope.
     * Disjoint external edits survive. An overlap fails closed, including an
     * attempt to undo creation after the user edited the created method body.
     */
    public DartUserSourceProjection rebaseOnto(
            byte[] liveSource, DartSourceDescriptor descriptor) {
        requireSameOwner(descriptor);
        DartSourceIntegrityResult live = scan(liveSource, descriptor);
        requireSameMemberOwner(scan(targetSource, targetDescriptor), live);
        Envelope actual = envelope(live);
        Execution execution = execute(live, descriptor);
        Envelope projected = envelope(scan(execution.source, descriptor));
        return new DartUserSourceProjection(actual, projected, execution.source, descriptor, execution.steps);
    }

    /** Applies only the proved user delta, retaining actual managed payloads. */
    public byte[] applyTo(byte[] liveSource, DartSourceDescriptor descriptor) {
        requireSameOwner(descriptor);
        DartSourceIntegrityResult live = scan(liveSource, descriptor);
        requireSameMemberOwner(scan(targetSource, targetDescriptor), live);
        byte[] result = execute(live, descriptor).source;
        requireSameMemberOwner(live, scan(result, descriptor));
        return result;
    }

    /**
     * Preserve operation boundaries: an exact observed body edit is not part of
     * a later field rename. Only a rename authored by renameStateField may be
     * replayed lexically on an older/newer body variant; generic byte edits keep
     * their conservative overlap fence. Each replay re-proves the same owner,
     * field type, controller lifecycle, names and references.
     */
    private Execution execute(DartSourceIntegrityResult live, DartSourceDescriptor descriptor) {
        Envelope actual = envelope(live);
        byte[] source = live.original().orElseThrow().copyBytes();
        if (before.same(after) || after.same(actual)) return new Execution(source, List.of());
        if (before.same(actual)) return new Execution(replaceEnvelope(live, after), steps);
        var retained = new ArrayList<Step>();
        for (Step step : steps) {
            Envelope endpoint;
            byte[] updated;
            if (step.before.same(step.after) || step.after.same(actual)) continue;
            if (step.before.same(actual)) {
                endpoint = step.after;
                updated = replaceEnvelope(live, endpoint);
            } else if (step.rename != null) {
                updated = step.rename.replay(source);
                endpoint = envelope(scan(updated, descriptor));
            } else {
                endpoint = merge(step.before, step.after, actual);
                updated = replaceEnvelope(live, endpoint);
            }
            DartSourceIntegrityResult next = scan(updated, descriptor);
            requireSameMemberOwner(live, next);
            if (!actual.same(endpoint)) retained.add(new Step(actual, endpoint, step.rename));
            source = updated;
            actual = endpoint;
            live = next;
        }
        return new Execution(source, retained);
    }

    public boolean isIdentity() {
        return before.same(after);
    }

    /** Exact unmanaged endpoint comparison, independent of generated hashes. */
    public boolean targetMatches(byte[] source, DartSourceDescriptor descriptor) {
        requireSameOwner(descriptor);
        return after.same(envelope(scan(source, descriptor)));
    }

    /** Retained byte arrays included in bounded history accounting. */
    public long retainedBytes() {
        return targetSource.length + before.parts.stream().mapToLong(part -> part.length).sum()
                + after.parts.stream().mapToLong(part -> part.length).sum()
                + steps.stream().mapToLong(step -> step.before.bytes() + step.after.bytes()).sum();
    }

    public static boolean sameUserEnvelope(byte[] first, DartSourceDescriptor firstDescriptor,
            byte[] second, DartSourceDescriptor secondDescriptor) {
        if (!firstDescriptor.dartFile().equals(secondDescriptor.dartFile())
                || !firstDescriptor.className().equals(secondDescriptor.className())
                || firstDescriptor.widgetKind() != secondDescriptor.widgetKind()) {
            return false;
        }
        return envelope(scan(first, firstDescriptor)).same(envelope(scan(second, secondDescriptor)));
    }

    private void requireOwner(String ownerClass) {
        if (!scan(targetSource, targetDescriptor).verifiedMemberClassName().orElseThrow()
                .equals(ownerClass)) {
            throw new IllegalArgumentException(
                    "Handler owner must be the verified Designer build-member class.");
        }
    }

    private static void requireSameMemberOwner(DartSourceIntegrityResult before,
            DartSourceIntegrityResult after) {
        if (!before.verifiedMemberClassName().equals(after.verifiedMemberClassName())) {
            throw new IllegalArgumentException("User-source projection cannot change the verified Designer member owner.");
        }
    }

    private void requireSameOwner(DartSourceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        if (!targetDescriptor.dartFile().equals(descriptor.dartFile())
                || !targetDescriptor.className().equals(descriptor.className())
                || targetDescriptor.widgetKind() != descriptor.widgetKind()) {
            throw new IllegalArgumentException(
                    "User-source projection belongs to a different Designer source.");
        }
    }

    private static DartSourceIntegrityResult scan(
            byte[] source, DartSourceDescriptor descriptor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(descriptor, "descriptor");
        DartSourceIntegrityResult result = new DartSourceIntegrityScanner().scan(source, descriptor);
        if (!result.onDiskDeclaredMatch() || result.original().isEmpty() || result.verifiedMemberClassName().isEmpty()
                || result.regions().size() != 2
                || !result.regions().get(0).id().equals(DartSourceIntegrityScanner.IMPORTS_REGION)
                || !result.regions().get(1).id().equals(DartSourceIntegrityScanner.BUILD_REGION)) {
            throw new IllegalArgumentException(
                    "User-source projection requires an exact verified Dart source envelope.");
        }
        return result;
    }

    private static void requireWritableFormat(byte[] source) {
        requireBoundedSize(source);
        if (source.length >= 3 && (source[0] & 0xFF) == 0xEF
                && (source[1] & 0xFF) == 0xBB && (source[2] & 0xFF) == 0xBF) {
            throw new IllegalArgumentException("Observed Source edits require UTF-8 without a byte-order mark.");
        }
        for (byte value : source) {
            if (value == '\r') {
                throw new IllegalArgumentException("Observed Source edits require LF-only line endings.");
            }
        }
        // scan() subsequently performs strict UTF-8 decoding and owner validation.
    }

    private static void requireBoundedSize(byte[] source) {
        if (source.length > new DartSourceIntegrityScanner().limits().maxSourceBytes()) {
            throw new IllegalArgumentException("Observed Source edit exceeds the writable source size limit.");
        }
    }

    /** Includes opening indentation/marker, exact payload and closing marker line. */
    private static byte[] guardedBytes(byte[] source, DartManagedRegionSnapshot region) {
        int start = region.payloadStartByte();
        if (start <= 0 || source[start - 1] != '\n') {
            throw new IllegalArgumentException("Observed managed opening marker has no canonical line boundary.");
        }
        start--;
        while (start > 0 && source[start - 1] != '\n') {
            start--;
        }
        int end = region.payloadEndByte();
        while (end < source.length && source[end] != '\n') {
            end++;
        }
        if (end < source.length) {
            end++;
        }
        return Arrays.copyOfRange(source, start, end);
    }

    private static Envelope envelope(DartSourceIntegrityResult source) {
        byte[] bytes = source.original().orElseThrow().copyBytes();
        DartManagedRegionSnapshot imports = source.regions().get(0);
        DartManagedRegionSnapshot build = source.regions().get(1);
        return new Envelope(List.of(
                Arrays.copyOfRange(bytes, 0, imports.payloadStartByte()),
                Arrays.copyOfRange(bytes, imports.payloadEndByte(), build.payloadStartByte()),
                Arrays.copyOfRange(bytes, build.payloadEndByte(), bytes.length)));
    }

    private static byte[] replaceEnvelope(DartSourceIntegrityResult source, Envelope replacement) {
        byte[] bytes = source.original().orElseThrow().copyBytes();
        DartManagedRegionSnapshot imports = source.regions().get(0);
        DartManagedRegionSnapshot build = source.regions().get(1);
        long size = (long) imports.payloadLengthBytes() + build.payloadLengthBytes();
        for (byte[] part : replacement.parts) {
            size += part.length;
        }
        if (size > new DartSourceIntegrityScanner().limits().maxSourceBytes()) {
            throw new IllegalArgumentException("Projected user source exceeds the source size limit.");
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream((int) size);
        result.writeBytes(replacement.parts.get(0));
        result.write(bytes, imports.payloadStartByte(), imports.payloadLengthBytes());
        result.writeBytes(replacement.parts.get(1));
        result.write(bytes, build.payloadStartByte(), build.payloadLengthBytes());
        result.writeBytes(replacement.parts.get(2));
        return result.toByteArray();
    }

    private static Envelope merge(Envelope base, Envelope target, Envelope actual) {
        return new Envelope(List.of(
                mergePart(base.parts.get(0), target.parts.get(0), actual.parts.get(0)),
                mergePart(base.parts.get(1), target.parts.get(1), actual.parts.get(1)),
                mergePart(base.parts.get(2), target.parts.get(2), actual.parts.get(2))));
    }

    /** Conservative three-way merge: at most one bounded changed hunk per side. */
    private static byte[] mergePart(byte[] base, byte[] target, byte[] actual) {
        if (Arrays.equals(base, target) || Arrays.equals(target, actual)) {
            return actual;
        }
        if (Arrays.equals(base, actual)) {
            return target;
        }
        Change intended = change(base, target);
        Change external = change(base, actual);
        // Insertions at the same boundary have no unambiguous ordering.
        boolean ambiguousInsertion = (intended.start == intended.end
                && intended.start >= external.start && intended.start <= external.end)
                || (external.start == external.end
                && external.start >= intended.start && external.start <= intended.end);
        if (ambiguousInsertion || (intended.start < external.end && external.start < intended.end)) {
            throw new IllegalArgumentException(
                    "User-source change overlaps the event-handler contribution; no source was changed. "
                    + "Intended byte range " + intended.start + ".." + intended.end
                    + " (replacement " + intended.replacement.length + " bytes); observed byte range "
                    + external.start + ".." + external.end + " (replacement " + external.replacement.length
                    + " bytes); source part " + base.length + " bytes.");
        }
        if (intended.start < external.start) {
            return replace(replace(base, external), intended);
        }
        return replace(replace(base, intended), external);
    }

    private static Change change(byte[] base, byte[] updated) {
        int start = 0;
        while (start < base.length && start < updated.length && base[start] == updated[start]) {
            start++;
        }
        int end = base.length;
        int updatedEnd = updated.length;
        while (end > start && updatedEnd > start && base[end - 1] == updated[updatedEnd - 1]) {
            end--;
            updatedEnd--;
        }
        return new Change(start, end, Arrays.copyOfRange(updated, start, updatedEnd));
    }

    private static byte[] replace(byte[] source, Change change) {
        byte[] result = new byte[Math.addExact(source.length - (change.end - change.start),
                change.replacement.length)];
        System.arraycopy(source, 0, result, 0, change.start);
        System.arraycopy(change.replacement, 0, result, change.start, change.replacement.length);
        System.arraycopy(source, change.end, result, change.start + change.replacement.length,
                source.length - change.end);
        return result;
    }

    private record Change(int start, int end, byte[] replacement) {}

    private record Execution(byte[] source, List<Step> steps) {}

    private record Step(Envelope before, Envelope after, FieldRename rename) {
        private Step inverse() {
            return new Step(after, before, rename == null ? null : rename.inverse());
        }
    }

    private record FieldRename(String owner, dev.flutter.netbeans.designer.model.StatePropertyBinding field,
            String newName) {
        private FieldRename inverse() {
            return new FieldRename(owner, new dev.flutter.netbeans.designer.model.StatePropertyBinding(newName,
                    field.type(), field.referenceType(), dev.flutter.netbeans.designer.model.StatePropertyBinding.Transform.DIRECT), field.fieldName());
        }

        private byte[] replay(byte[] source) {
            try {
                return DartEventHandlerSource.replayStateFieldRename(source, owner, field, newName);
            } catch (IllegalArgumentException failure) {
                // An older physical endpoint may already contain this exact
                // rename but have a different body. Prove the typed inverse
                // and an exact round trip; never accept a missing/wrong field
                // or a conflicting destination merely because its name exists.
                FieldRename reverse = inverse();
                try {
                    byte[] prior = DartEventHandlerSource.replayStateFieldRename(source, owner, reverse.field, reverse.newName);
                    byte[] roundTrip = DartEventHandlerSource.replayStateFieldRename(prior, owner, field, newName);
                    if (Arrays.equals(source, roundTrip)) return source;
                } catch (IllegalArgumentException rejected) {
                    // Retain the original directional rejection.
                }
                throw failure;
            }
        }
    }

    private static final class Envelope {
        private final List<byte[]> parts;

        private Envelope(List<byte[]> parts) {
            this.parts = parts.stream().map(byte[]::clone).toList();
        }

        private boolean same(Envelope other) {
            for (int i = 0; i < parts.size(); i++) {
                if (!Arrays.equals(parts.get(i), other.parts.get(i))) {
                    return false;
                }
            }
            return true;
        }

        private long bytes() {
            return parts.stream().mapToLong(part -> part.length).sum();
        }
    }
}
