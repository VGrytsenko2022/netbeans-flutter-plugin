package dev.flutter.netbeans.designer.command;

import dev.flutter.netbeans.designer.catalog.WidgetCatalog;
import dev.flutter.netbeans.designer.codec.FdCodecDiagnostic;
import dev.flutter.netbeans.designer.codec.FdDecodeResult;
import dev.flutter.netbeans.designer.codec.FdDocumentCodec;
import dev.flutter.netbeans.designer.codec.FdEncodeException;
import dev.flutter.netbeans.designer.codec.OriginalFdBytes;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnostic;
import dev.flutter.netbeans.designer.generation.DartGenerationDiagnosticCode;
import dev.flutter.netbeans.designer.generation.DartGenerationResult;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import dev.flutter.netbeans.designer.model.DesignerDocument;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationDiagnostic;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationPlanner;
import dev.flutter.netbeans.designer.pair.DesignerPairPreparationResult;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartManagedRegionSnapshot;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityDiagnostic;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityGate;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityResult;
import dev.flutter.netbeans.designer.source.DartThreeWayIntegrityStatus;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionDiagnostic;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlan;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionPlanner;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionResult;
import dev.flutter.netbeans.designer.transition.DartSourceTransitionStatus;
import dev.flutter.netbeans.designer.transition.DartUserSourceProjection;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.validation.ValidationIssue;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import dev.flutter.netbeans.designer.validation.WidgetTreeValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Immutable bounded command history over one exact durable Flutter Designer
 * pair.
 *
 * <p>Every semantic revision is regenerated from the single catalog instance
 * captured by {@link #open}; callers cannot substitute a later Lookup catalog.
 * Each prospective pair is planned directly from the current durable anchor,
 * while adjacent revisions still expose exact before/after candidate bytes.
 * This class performs no I/O and does not authorize persistence.</p>
 */
public final class DesignerCommandSession {
    private final WidgetCatalog catalog;
    private final DesignerCommandLimits limits;
    private final DurableAnchor anchor;
    private final List<DesignerCommandRevision> revisions;
    private final List<DesignerCommandEdit> edits;
    private final int cursor;
    private final long savedRevisionId;
    private final long nextRevisionId;
    private final List<AcceptedSourceVariant> acceptedSourceVariants;

    private DesignerCommandSession(
            WidgetCatalog catalog,
            DesignerCommandLimits limits,
            DurableAnchor anchor,
            List<DesignerCommandRevision> revisions,
            List<DesignerCommandEdit> edits,
            int cursor,
            long savedRevisionId,
            long nextRevisionId) {
        this(catalog, limits, anchor, revisions, edits, cursor, savedRevisionId, nextRevisionId, List.of());
    }

    private DesignerCommandSession(WidgetCatalog catalog, DesignerCommandLimits limits, DurableAnchor anchor,
            List<DesignerCommandRevision> revisions, List<DesignerCommandEdit> edits, int cursor,
            long savedRevisionId, long nextRevisionId, List<AcceptedSourceVariant> acceptedSourceVariants) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.revisions = List.copyOf(Objects.requireNonNull(revisions, "revisions"));
        this.edits = List.copyOf(Objects.requireNonNull(edits, "edits"));
        this.cursor = cursor;
        this.savedRevisionId = savedRevisionId;
        this.nextRevisionId = nextRevisionId;
        this.acceptedSourceVariants = List.copyOf(acceptedSourceVariants);
        validateState();
    }

    public static DesignerCommandSessionOpenResult open(
            OriginalFdBytes baselineFd,
            byte[] baselineDart,
            WidgetCatalog catalog) {
        return open(
                baselineFd,
                baselineDart,
                catalog,
                DesignerCommandLimits.defaults());
    }

    public static DesignerCommandSessionOpenResult open(
            OriginalFdBytes baselineFd,
            byte[] baselineDart,
            WidgetCatalog catalog,
            DesignerCommandLimits limits) {
        return openInternal(
                baselineFd,
                baselineDart,
                catalog,
                limits,
                Optional.empty());
    }

    /**
     * Opens a command session while retaining the exact source and three-way
     * evidence identities already published by the caller's loaded-pair gate.
     *
     * <p>The evidence is not trusted merely because it reports a match. This
     * method decodes and validates the exact .fd again, regenerates Dart with
     * the supplied catalog, and rejects any detached or incoherent evidence.
     * On success, every derived transition is anchored to the exact supplied
     * {@code threeWay} object.</p>
     */
    public static DesignerCommandSessionOpenResult openVerified(
            OriginalFdBytes baselineFd,
            byte[] baselineDart,
            WidgetCatalog catalog,
            DartSourceIntegrityResult source,
            DartThreeWayIntegrityResult threeWay) {
        return openVerified(
                baselineFd,
                baselineDart,
                catalog,
                source,
                threeWay,
                DesignerCommandLimits.defaults());
    }

    /** Same verified-evidence contract with explicit bounded session limits. */
    public static DesignerCommandSessionOpenResult openVerified(
            OriginalFdBytes baselineFd,
            byte[] baselineDart,
            WidgetCatalog catalog,
            DartSourceIntegrityResult source,
            DartThreeWayIntegrityResult threeWay,
            DesignerCommandLimits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(threeWay, "threeWay");
        return openInternal(
                baselineFd,
                baselineDart,
                catalog,
                limits,
                Optional.of(new VerifiedBaselineEvidence(source, threeWay)));
    }

    private static DesignerCommandSessionOpenResult openInternal(
            OriginalFdBytes baselineFd,
            byte[] baselineDart,
            WidgetCatalog catalog,
            DesignerCommandLimits limits,
            Optional<VerifiedBaselineEvidence> verifiedEvidence) {
        Objects.requireNonNull(baselineFd, "baselineFd");
        Objects.requireNonNull(baselineDart, "baselineDart");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(verifiedEvidence, "verifiedEvidence");

        if (baselineFd.size() > limits.fdCodecLimits().maxDocumentBytes()) {
            return openFailure(
                    DesignerCommandStatus.LIMIT_EXCEEDED,
                    DesignerCommandDiagnosticCode.BASELINE_FD_TOO_LARGE,
                    "",
                    "The exact baseline .fd contains " + baselineFd.size()
                    + " bytes, exceeding the command-session limit of "
                    + limits.fdCodecLimits().maxDocumentBytes() + ".");
        }
        long initialBytes = (long) baselineFd.size() + baselineDart.length;
        if (initialBytes > limits.maxRetainedPairBytes()) {
            return openFailure(
                    DesignerCommandStatus.LIMIT_EXCEEDED,
                    DesignerCommandDiagnosticCode.HISTORY_BYTE_LIMIT,
                    "",
                    "The exact baseline pair requires " + initialBytes
                    + " retained bytes, exceeding the session limit of "
                    + limits.maxRetainedPairBytes() + ".");
        }

        FdDocumentCodec codec = new FdDocumentCodec(limits.fdCodecLimits());
        FdDecodeResult decoded = codec.decode(baselineFd);
        if (decoded instanceof FdDecodeResult.UnsupportedNewer newer) {
            return openFailure(
                    DesignerCommandStatus.UNSUPPORTED,
                    DesignerCommandDiagnosticCode.BASELINE_FD_UNSUPPORTED,
                    "/schemaVersion",
                    "The exact baseline .fd uses unsupported schema version "
                    + newer.declaredSchemaVersion() + ".");
        }
        if (decoded instanceof FdDecodeResult.Invalid invalid) {
            FdCodecDiagnostic primary = invalid.diagnostics().getFirst();
            return openFailure(
                    DesignerCommandStatus.CONFLICT,
                    DesignerCommandDiagnosticCode.BASELINE_FD_INVALID,
                    primary.pointer(),
                    "The exact baseline .fd failed " + primary.code()
                    + ": " + primary.message());
        }
        DesignerDocument document = ((FdDecodeResult.Current) decoded).document();
        ValidationResult validation = new WidgetTreeValidator(
                limits.validationLimits()).validate(document, catalog);
        if (!validation.valid()) {
            ValidationIssue primary = validation.errors().getFirst();
            return openFailure(
                    validationLimit(primary)
                            ? DesignerCommandStatus.LIMIT_EXCEEDED
                            : DesignerCommandStatus.CONFLICT,
                    DesignerCommandDiagnosticCode.BASELINE_MODEL_INVALID,
                    primary.path(),
                    "The baseline model failed " + primary.code()
                    + ": " + primary.message());
        }

        DartGenerationResult generation = new DartRegionGenerator(
                limits.generationLimits(),
                limits.validationLimits()).generate(document, catalog);
        if (!generation.successful()) {
            DartGenerationDiagnostic primary = generation.diagnostics().getFirst();
            return openFailure(
                    generationStatus(primary.code()),
                    DesignerCommandDiagnosticCode.BASELINE_GENERATION_FAILED,
                    primary.path(),
                    "Baseline generation failed " + primary.code()
                    + ": " + primary.message());
        }
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(
                limits.sourceLimits());
        DartSourceIntegrityResult source;
        DartThreeWayIntegrityResult threeWay;
        if (verifiedEvidence.isPresent()) {
            VerifiedBaselineEvidence evidence = verifiedEvidence.orElseThrow();
            Optional<String> mismatch = verifyBaselineEvidence(
                    baselineDart,
                    document,
                    generation,
                    scanner,
                    evidence);
            if (mismatch.isPresent()) {
                return openFailure(
                        DesignerCommandStatus.CONFLICT,
                        DesignerCommandDiagnosticCode.BASELINE_EVIDENCE_MISMATCH,
                        "/source/dartFile",
                        mismatch.orElseThrow());
            }
            source = evidence.source();
            threeWay = evidence.threeWay();
            generation = threeWay.generation();
        } else {
            source = scanner.scan(baselineDart, document.source());
            threeWay = new DartThreeWayIntegrityGate(scanner).evaluate(
                    source, document.source(), generation);
        }
        if (!threeWay.onDiskThreeWayMatch()) {
            return threeWayOpenFailure(threeWay);
        }
        try {
            requireStateBindingFields(document, baselineDart, source);
        } catch (IllegalArgumentException invalid) {
            return openFailure(DesignerCommandStatus.CONFLICT,
                    DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED, "/source/dartFile", invalid.getMessage());
        }

        DurableAnchor anchor = new DurableAnchor(
                baselineFd, document, baselineDart, generation, source, threeWay);
        DesignerCommandRevision initial = new DesignerCommandRevision(
                0,
                document,
                baselineFd,
                generation,
                baselineDart,
                source,
                Optional.empty(),
                Optional.empty(),
                DesignerRevisionPersistenceKind.BASELINE,
                DartUserSourceProjection.identity(baselineDart, document.source()));
        if (initial.retainedPairBytes() > limits.maxRetainedPairBytes()) {
            return openFailure(DesignerCommandStatus.LIMIT_EXCEEDED,
                    DesignerCommandDiagnosticCode.HISTORY_BYTE_LIMIT, "",
                    "The baseline pair and user-source proof exceed the retained command-history limit.");
        }
        DesignerCommandSession session = new DesignerCommandSession(
                catalog,
                limits,
                anchor,
                List.of(initial),
                List.of(),
                0,
                0,
                1);
        return new DesignerCommandSessionOpenResult(
                DesignerCommandStatus.READY,
                Optional.of(session),
                List.of());
    }

    private static Optional<String> verifyBaselineEvidence(
            byte[] baselineDart,
            DesignerDocument document,
            DartGenerationResult regenerated,
            DartSourceIntegrityScanner scanner,
            VerifiedBaselineEvidence evidence) {
        DartSourceIntegrityResult source = evidence.source();
        DartThreeWayIntegrityResult threeWay = evidence.threeWay();
        if (threeWay.source() != source) {
            return Optional.of(
                    "Verified three-way evidence is detached from the exact supplied "
                    + "source-integrity identity.");
        }
        if (source.original().isEmpty()
                || !source.original().orElseThrow().contentEquals(baselineDart)) {
            return Optional.of(
                    "Verified source evidence does not describe the exact supplied "
                    + "Dart baseline bytes.");
        }
        DartSourceIntegrityResult rescanned = scanner.scan(
                baselineDart, document.source());
        if (!source.equals(rescanned)) {
            return Optional.of(
                    "Verified source evidence is not coherent with the decoded .fd "
                    + "source descriptor.");
        }
        if (!threeWay.generation().equals(regenerated)) {
            return Optional.of(
                    "Verified three-way generation does not match deterministic "
                    + "generation for the decoded .fd and supplied catalog.");
        }
        DartThreeWayIntegrityResult reevaluated = new DartThreeWayIntegrityGate(
                scanner).evaluate(source, document.source(), regenerated);
        if (!threeWay.onDiskThreeWayMatch()
                || !reevaluated.onDiskThreeWayMatch()
                || !threeWay.comparisons().equals(reevaluated.comparisons())
                || !threeWay.diagnostics().equals(reevaluated.diagnostics())) {
            return Optional.of(
                    "Verified three-way evidence is not coherent with the exact "
                    + "Dart baseline, decoded .fd, and supplied catalog.");
        }
        return Optional.empty();
    }

    /** Applies one semantic action, discarding a redo branch only on success. */
    public DesignerCommandSessionResult apply(DesignerCommand command) {
        return applyFromLiveEnvelope(
                Objects.requireNonNull(command, "command"),
                currentLiveEnvelopeBytes());
    }

    /**
     * The current paired revision retains the exact live envelope from which
     * it was derived. Reusing that template keeps a physical endpoint sticky
     * across subsequent ordinary commands after its first identity-bound
     * admission. Baseline and FD-only cursors continue to use the durable
     * anchor directly.
     */
    private byte[] currentLiveEnvelopeBytes() {
        return current().preparedPair()
                .map(PreparedDesignerPair::liveDartBytes)
                .orElseGet(anchor::dartBytes);
    }

    /**
     * Applies one semantic action from an exact physical form of the current
     * logical revision.
     *
     * <p>The endpoint pair must be attached to this session's exact durable
     * anchor and must describe the current semantic document and canonical
     * {@code .fd} bytes. Its candidate managed payloads must equal the current
     * logical revision byte-for-byte, while its live-source managed payloads
     * must equal the durable anchor byte-for-byte. These stronger checks make
     * the pair's live Dart bytes an exact unmanaged-envelope template; hash
     * normalization alone is never sufficient.</p>
     *
     * <p>The returned candidate is derived with this session's captured
     * catalog, limits and next revision id, but this immutable input session
     * remains at its existing logical cursor. The supplied pair is proof only:
     * this method performs no I/O and grants no persistence authority.</p>
     */
    public DesignerCommandSessionResult applyFromPhysicalEndpoint(
            DesignerCommand command,
            PreparedDesignerPair exactEndpointPair) {
        Objects.requireNonNull(command, "command");
        requireExactCurrentPhysicalEndpoint(
                Objects.requireNonNull(
                        exactEndpointPair, "exactEndpointPair"));
        return applyFromLiveEnvelope(command, exactEndpointPair.liveDartBytes(),
                exactEndpointPair.dartTransition().userSourceProjection());
    }

    private DesignerCommandSessionResult applyFromLiveEnvelope(
            DesignerCommand command,
            byte[] exactLiveEnvelope) {
        return applyFromLiveEnvelope(command, exactLiveEnvelope, current().userSourceProjection());
    }

    private DesignerCommandSessionResult applyFromLiveEnvelope(DesignerCommand command,
            byte[] exactLiveEnvelope, DartUserSourceProjection sourceProjection) {
        DesignerCommandTransformer.SemanticResult semantic =
                new DesignerCommandTransformer(catalog, limits.validationLimits())
                        .apply(current().document(), command);
        if (semantic.status() != DesignerCommandStatus.APPLIED) {
            return unchanged(
                    semantic.status(), semantic.diagnostic().orElseThrow());
        }

        int retainedEditCount = cursor + 1;
        if (retainedEditCount > limits.maxHistoryEdits()) {
            return unchanged(
                    DesignerCommandStatus.LIMIT_EXCEEDED,
                    diagnostic(
                            DesignerCommandDiagnosticCode.HISTORY_EDIT_LIMIT,
                            "",
                            "Applying another command would exceed the session limit of "
                            + limits.maxHistoryEdits() + " undoable edits."));
        }

        boolean insertMultiLayoutDelegate =
                !dev.flutter.netbeans.designer.catalog.CustomMultiChildLayoutWidgetPropertySchema.usesInitialDelegate(current().document().root())
                && dev.flutter.netbeans.designer.catalog.CustomMultiChildLayoutWidgetPropertySchema.usesInitialDelegate(semantic.document().orElseThrow().root());
        boolean insertLayoutDelegate =
                !dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.usesInitialDelegate(current().document().root())
                && dev.flutter.netbeans.designer.catalog.CustomSingleChildLayoutWidgetPropertySchema.usesInitialDelegate(semantic.document().orElseThrow().root());
        boolean insertHeaderDelegate =
                !dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.usesInitialDelegate(current().document().root())
                && dev.flutter.netbeans.designer.catalog.SliverPersistentHeaderWidgetPropertySchema.usesInitialDelegate(semantic.document().orElseThrow().root());
        DartUserSourceProjection userSource;
        try {
            userSource = sourceProjection.rebaseOnto(
                    exactLiveEnvelope, anchor.document().source());
            if (insertMultiLayoutDelegate) userSource = userSource.insertMultiChildLayoutDelegate();
            if (insertLayoutDelegate) userSource = userSource.insertSingleChildLayoutDelegate();
            if (insertHeaderDelegate) {
                userSource = userSource.insertPersistentHeaderDelegate();
            }
            if (command instanceof CreateMenuAnchorBuilder create) {
                userSource = userSource.insertHandler(eventMemberOwner(), create.methodName(),
                        create.declaration(), List.of("package:flutter/material.dart"));
            } else if (command instanceof CreateEventHandler create) {
                WidgetNode widget = EventCommandSupport.widget(current().document().root(), create.widgetId());
                WidgetEventDescriptor event = EventCommandSupport.event(catalog, widget, create.event());
                userSource = userSource.insertHandler(eventMemberOwner(),
                        create.handlerName(), event.createStub(create.handlerName()), event.signature().importUris());
            } else if (command instanceof RenameEventHandler rename) {
                WidgetNode widget = EventCommandSupport.widget(current().document().root(), rename.widgetId());
                String oldName = EventCommandSupport.localHandler(widget.properties().get(rename.event()))
                        .orElseThrow(() -> new IllegalArgumentException("The event has no local handler to rename."));
                userSource = userSource.renameHandler(eventMemberOwner(),
                        oldName, rename.newName());
            } else if (command instanceof RenameStateField rename) {
                WidgetNode widget = EventCommandSupport.widget(current().document().root(), rename.widgetId());
                var field = StateCommandSupport.field(widget, rename.fieldName());
                dev.flutter.netbeans.designer.events.DartEventHandlerSource.requireStateFieldRenameNamesAvailable(
                        current().dartCandidateBytes(), eventMemberOwner(), field, rename.newName());
                userSource = userSource.renameStateField(eventMemberOwner(), field, rename.newName());
            } else if (command instanceof CreateStateBinding create) {
                WidgetNode widget = EventCommandSupport.widget(
                        semantic.document().orElseThrow().root(), create.widgetId());
                // A retained user-source proof uses the durable managed template;
                // inspect the latest generated payload too, including prior unsaved commands.
                dev.flutter.netbeans.designer.events.DartEventHandlerSource.requireStateBindingNamesAvailable(
                        current().dartCandidateBytes(), eventMemberOwner(), widget.stateBinding().orElseThrow(), create.reusedField().isPresent());
                userSource = userSource.insertStateBinding(eventMemberOwner(), widget, create.initialText(), create.reusedField().isPresent());
            }
        } catch (IllegalArgumentException invalid) {
            return unchanged(DesignerCommandStatus.CONFLICT, diagnostic(
                    insertMultiLayoutDelegate ? DesignerCommandDiagnosticCode.MULTI_CHILD_LAYOUT_DELEGATE_REJECTED : insertLayoutDelegate ? DesignerCommandDiagnosticCode.SINGLE_CHILD_LAYOUT_DELEGATE_REJECTED : insertHeaderDelegate
                            ? DesignerCommandDiagnosticCode.PERSISTENT_HEADER_DELEGATE_REJECTED
                            : command instanceof CreateMenuAnchorBuilder
                            ? DesignerCommandDiagnosticCode.MENU_ANCHOR_BUILDER_REJECTED
                            : command instanceof CreateStateBinding || command instanceof RenameStateField
                            ? DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED
                            : DesignerCommandDiagnosticCode.EVENT_HANDLER_REJECTED,
                    "/source/dartFile", invalid.getMessage()));
        }
        DerivedRevision derived = deriveRevision(
                anchor,
                semantic.document().orElseThrow(),
                nextRevisionId,
                exactLiveEnvelope,
                userSource);
        if (derived.diagnostic().isPresent()) {
            return unchanged(
                    derived.status(), derived.diagnostic().orElseThrow());
        }
        DesignerCommandRevision after = derived.revision().orElseThrow();
        long retainedBytes = Math.addExact(retainedBytesThrough(cursor), acceptedSourceVariantBytes());
        if (wouldExceed(retainedBytes, after.retainedPairBytes(),
                limits.maxRetainedPairBytes())) {
            return unchanged(
                    DesignerCommandStatus.LIMIT_EXCEEDED,
                    diagnostic(
                            DesignerCommandDiagnosticCode.HISTORY_BYTE_LIMIT,
                            "",
                            "The prospective command history would exceed the retained-pair "
                            + "limit of " + limits.maxRetainedPairBytes() + " bytes."));
        }

        ArrayList<DesignerCommandRevision> nextRevisions = new ArrayList<>(
                revisions.subList(0, cursor + 1));
        nextRevisions.add(after);
        ArrayList<DesignerCommandEdit> nextEdits = new ArrayList<>(
                edits.subList(0, cursor));
        DesignerCommandEdit edit = new DesignerCommandEdit(
                command, current(), after);
        nextEdits.add(edit);
        DesignerCommandSession next = new DesignerCommandSession(
                catalog,
                limits,
                anchor,
                nextRevisions,
                nextEdits,
                cursor + 1,
                savedRevisionId,
                Math.incrementExact(nextRevisionId),
                acceptedSourceVariants);
        return changed(DesignerCommandStatus.APPLIED, next, edit);
    }

    private String eventMemberOwner() {
        return current().sourceIntegrity().verifiedMemberClassName().orElseThrow(() ->
                new IllegalArgumentException("The Designer event member owner is not verified in the current source."));
    }

    private static void requireStateBindingFields(DesignerDocument document, byte[] source,
            DartSourceIntegrityResult integrity) {
        var bindings = new ArrayList<dev.flutter.netbeans.designer.model.StateBinding>();
        var consumers = new ArrayList<dev.flutter.netbeans.designer.model.StatePropertyBinding>();
        var pending = new java.util.ArrayDeque<WidgetNode>();
        pending.push(document.root());
        while (!pending.isEmpty()) {
            WidgetNode node = pending.pop();
            node.stateBinding().ifPresent(bindings::add);
            node.propertyBindings().entrySet().stream()
                    .filter(entry -> dev.flutter.netbeans.designer.catalog.FocusWidgetPropertySchema.propertyAvailable(node, entry.getKey()))
                    .filter(entry -> dev.flutter.netbeans.designer.catalog.SwitchListTileWidgetPropertySchema.propertyAvailable(node, entry.getKey()))
                    .filter(entry -> dev.flutter.netbeans.designer.catalog.RadioListTileWidgetPropertySchema.propertyAvailable(node, entry.getKey()))
                    .map(java.util.Map.Entry::getValue).forEach(consumers::add);
            for (var slot : node.slots().values()) {
                switch (slot) {
                    case dev.flutter.netbeans.designer.model.WidgetSlot.SingleSlot single -> single.child().ifPresent(pending::push);
                    case dev.flutter.netbeans.designer.model.WidgetSlot.ListSlot list -> list.children().forEach(pending::push);
                }
            }
        }
        if (!bindings.isEmpty() || !consumers.isEmpty()) {
            if (document.source().widgetKind() != dev.flutter.netbeans.designer.model.WidgetClassKind.STATEFUL) {
                throw new IllegalArgumentException("State bindings require a verified Stateful source owner.");
            }
            dev.flutter.netbeans.designer.events.DartEventHandlerSource.requireStateBindingFields(source,
                    integrity.verifiedMemberClassName().orElseThrow(() -> new IllegalArgumentException(
                            "The State field owner is not verified in the current source.")), bindings);
            dev.flutter.netbeans.designer.events.DartEventHandlerSource.requirePropertyBindingFields(source,
                    integrity.verifiedMemberClassName().orElseThrow(), consumers);
        }
    }

    /**
     * Derives a same-logical-revision candidate from newly observed user Source.
     * The original exact physical pair is mandatory. The resulting capability
     * remains pure evidence and must pass the native owner's analyzer gate.
     */
    public DesignerSourceRestage prepareSourceRestage(PreparedDesignerPair predecessorPair, byte[] observedDart) {
        Objects.requireNonNull(observedDart, "observedDart");
        requireExactCurrentPhysicalEndpoint(Objects.requireNonNull(predecessorPair, "predecessorPair"));
        DartUserSourceProjection observed = DartUserSourceProjection.observedEdit(
                predecessorPair.prospectiveDartBytes(), observedDart, current().document().source());
        DartUserSourceProjection combined = predecessorPair.dartTransition().userSourceProjection().then(observed);
        DerivedRevision derived = deriveRevision(anchor, current().document(), current().revisionId(),
                predecessorPair.liveDartBytes(), current().generation(), combined);
        if (derived.revision().isEmpty()) {
            throw new IllegalArgumentException("Observed Source candidate cannot be derived: "
                    + derived.diagnostic().orElseThrow().message());
        }
        DesignerCommandRevision physical = derived.revision().orElseThrow();
        if (physical.persistenceKind() != DesignerRevisionPersistenceKind.PAIRED
                || physical.generation() != current().generation()
                || !physical.document().equals(current().document())
                || !physical.fdSnapshot().equals(current().fdSnapshot())
                || !Arrays.equals(physical.dartCandidateBytes(), observedDart)) {
            throw new IllegalArgumentException("Observed Source must preserve the exact logical model and generated payloads.");
        }
        if (acceptedSourceVariants.size() >= limits.maxHistoryEdits()) {
            throw new IllegalArgumentException("Observed Source variants exceed the bounded history entry limit.");
        }
        long bytes = Math.addExact(retainedBytesThrough(revisions.size() - 1), acceptedSourceVariantBytes());
        if (wouldExceed(bytes, physical.retainedPairBytes(), limits.maxRetainedPairBytes())) {
            throw new IllegalArgumentException("Observed Source variants exceed the retained command-history byte limit.");
        }
        ArrayList<AcceptedSourceVariant> variants = new ArrayList<>(acceptedSourceVariants);
        variants.add(new AcceptedSourceVariant(current(), physical));
        DesignerCommandSession accepted = new DesignerCommandSession(catalog, limits, anchor,
                revisions, edits, cursor, savedRevisionId, nextRevisionId, variants);
        return new DesignerSourceRestage(this, accepted, physical, predecessorPair);
    }

    /** Pure adoption only; the native owner must first accept exact analyzer/live-source evidence. */
    public DesignerCommandSession acceptSourceRestage(DesignerSourceRestage restage) {
        if (!Objects.requireNonNull(restage, "restage").belongsTo(this)) {
            throw new IllegalArgumentException("The Source restage belongs to another exact command session.");
        }
        return restage.acceptedSession();
    }

    private boolean acceptedSourceVariant(DesignerCommandRevision logical, PreparedDesignerPair physical) {
        return acceptedSourceVariants.stream().anyMatch(value -> value.logical() == logical
                && value.physical().preparedPair().orElseThrow() == physical);
    }

    private long acceptedSourceVariantBytes() {
        long bytes = 0;
        for (AcceptedSourceVariant value : acceptedSourceVariants) {
            bytes = Math.addExact(bytes, value.physical().retainedPairBytes());
        }
        return bytes;
    }

    private record AcceptedSourceVariant(DesignerCommandRevision logical, DesignerCommandRevision physical) { }

    private void requireExactCurrentPhysicalEndpoint(
            PreparedDesignerPair exactEndpointPair) {
        DesignerCommandRevision logical = current();
        DartSourceTransitionPlan transition =
                exactEndpointPair.dartTransition();
        if (logical.persistenceKind()
                    != DesignerRevisionPersistenceKind.PAIRED
                || exactEndpointPair.baselineFd() != anchor.fd()
                || transition.baseline() != anchor.threeWay()
                || transition.baseline().source() != anchor.source()
                || !exactEndpointPair.baselineDocument()
                        .equals(anchor.document())
                || !Arrays.equals(
                        exactEndpointPair.baselineDartBytes(),
                        anchor.dartBytes())
                || !exactEndpointPair.prospectiveDocument()
                        .equals(logical.document())
                || !exactEndpointPair.prospectiveFd()
                        .equals(logical.fdSnapshot())
                || !transition.generation().equals(logical.generation())
                || transition.generation().generated().orElseThrow()
                        .candidateCapacityBudget()
                    != logical.candidateCapacityBudget()
                || !exactManagedPayloadsMatch(
                        anchor.source(), transition.liveSource())
                || !exactManagedPayloadsMatch(
                        logical.sourceIntegrity(),
                        transition.candidateIntegrity())
                || !physicalUserEnvelopeMatches(logical, exactEndpointPair)) {
            throw new IllegalArgumentException(
                    "The physical endpoint pair is detached from the exact current logical revision or durable anchor");
        }
    }

    public DesignerCommandSessionResult undo() {
        Optional<DesignerCommandEdit> selected = undoEdit();
        if (selected.isEmpty()) {
            return unchanged(
                    DesignerCommandStatus.NOT_AVAILABLE,
                    diagnostic(
                            DesignerCommandDiagnosticCode.UNDO_NOT_AVAILABLE,
                            "",
                            "No designer command is available to undo."));
        }
        DesignerCommandEdit edit = selected.orElseThrow();
        if (!edit.inverse().canApplyTo(current().document())
                || !edit.inverse().applyTo(current().document())
                        .equals(revisions.get(cursor - 1).document())) {
            return unchanged(
                    DesignerCommandStatus.CONFLICT,
                    diagnostic(
                            DesignerCommandDiagnosticCode.EXACT_INVERSE_MISMATCH,
                            "",
                            "The selected Undo inverse does not match the exact current revision."));
        }
        DesignerCommandSession next = copyAtCursor(cursor - 1);
        return changed(DesignerCommandStatus.UNDONE, next, edit);
    }

    public DesignerCommandSessionResult redo() {
        Optional<DesignerCommandEdit> selected = redoEdit();
        if (selected.isEmpty()) {
            return unchanged(
                    DesignerCommandStatus.NOT_AVAILABLE,
                    diagnostic(
                            DesignerCommandDiagnosticCode.REDO_NOT_AVAILABLE,
                            "",
                            "No designer command is available to redo."));
        }
        DesignerCommandEdit edit = selected.orElseThrow();
        if (!current().document().equals(edit.beforeRevision().document())
                || !revisions.get(cursor + 1).document()
                        .equals(edit.afterRevision().document())) {
            return unchanged(
                    DesignerCommandStatus.CONFLICT,
                    diagnostic(
                            DesignerCommandDiagnosticCode.EXACT_INVERSE_MISMATCH,
                            "",
                            "The selected Redo command does not match the exact current revision."));
        }
        DesignerCommandSession next = copyAtCursor(cursor + 1);
        return changed(DesignerCommandStatus.REDONE, next, edit);
    }

    /**
     * Marks the exact current outputs as durably saved and re-anchors every
     * retained Undo/Redo revision to that pair.
     *
     * <p>The caller must invoke this only after its own durable transaction has
     * succeeded. This method performs no I/O and grants no write authority.</p>
     */
    public DesignerCommandSession markSaved() {
        DesignerCommandRevision saved = current();
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(
                limits.sourceLimits());
        DartSourceIntegrityResult source = scanner.scan(
                saved.dartCandidateBytes(), saved.document().source());
        DartThreeWayIntegrityResult threeWay = new DartThreeWayIntegrityGate(
                scanner).evaluate(source, saved.document().source(), saved.generation());
        if (!threeWay.onDiskThreeWayMatch()) {
            throw new IllegalStateException(
                    "The exact current revision cannot become a durable command anchor");
        }
        DurableAnchor nextAnchor = new DurableAnchor(
                saved.fdSnapshot(),
                saved.document(),
                saved.dartCandidateBytes(),
                saved.generation(),
                source,
                threeWay);

        return markSavedAtAnchor(
                saved, nextAnchor, "Saved command history re-anchor");
    }

    /**
     * Marks one exact physical paired serialization as the durable form of
     * the current semantic revision.
     *
     * <p>The supplied pair may preserve newer unmanaged/native-overlay Dart
     * bytes than the canonical candidate retained by {@link #current()}, but
     * it must retain the current durable anchor's exact baseline evidence,
     * the exact current semantic document and canonical {@code .fd} bytes,
     * and byte-identical managed payloads. This method performs no I/O and
     * grants no persistence authority.</p>
     */
    public DesignerCommandSession markSaved(PreparedDesignerPair exactPair) {
        Objects.requireNonNull(exactPair, "exactPair");
        DesignerCommandRevision saved = current();
        DartSourceTransitionPlan transition = exactPair.dartTransition();
        if (!dirty()
                || saved.persistenceKind()
                    != DesignerRevisionPersistenceKind.PAIRED
                || exactPair.baselineFd() != anchor.fd()
                || transition.baseline() != anchor.threeWay()
                || transition.baseline().source() != anchor.source()
                || transition.generation() != saved.generation()
                || !exactPair.baselineDocument().equals(anchor.document())
                || !Arrays.equals(
                        exactPair.baselineDartBytes(), anchor.dartBytes())
                || !exactPair.prospectiveDocument().equals(saved.document())
                || !exactPair.prospectiveFd().equals(saved.fdSnapshot())
                || !exactManagedPayloadsMatch(
                        anchor.source(), transition.liveSource())
                || !exactManagedPayloadsMatch(
                        saved.sourceIntegrity(),
                        transition.candidateIntegrity())
                || !physicalUserEnvelopeMatches(saved, exactPair)) {
            throw new IllegalArgumentException(
                    "The exact physical pair is detached from the current durable anchor or semantic revision");
        }

        DartSourceIntegrityResult source = transition.candidateIntegrity();
        DartThreeWayIntegrityResult threeWay = new DartThreeWayIntegrityGate(
                new DartSourceIntegrityScanner(limits.sourceLimits()))
                .evaluate(source, saved.document().source(), saved.generation());
        if (!threeWay.onDiskThreeWayMatch()) {
            throw new IllegalArgumentException(
                    "The exact physical pair cannot become a durable three-way anchor");
        }
        DurableAnchor nextAnchor = new DurableAnchor(
                exactPair.prospectiveFd(),
                exactPair.prospectiveDocument(),
                exactPair.prospectiveDartBytes(),
                saved.generation(),
                source,
                threeWay);

        return markSavedAtAnchor(
                saved, nextAnchor, "Exact-pair command history re-anchor", exactPair);
    }

    /**
     * Saves one retained metadata revision in an exact historical Source envelope.
     * Only unmanaged bytes may differ; no generated expression or analyzer proof
     * is invented. The caller owns persistence and the exact native history fence.
     */
    public DesignerCommandSession markSavedWithSourceEnvelope(byte[] exactDart) {
        Objects.requireNonNull(exactDart, "exactDart");
        DesignerCommandRevision saved = current();
        if (!dirty() || saved.persistenceKind() != DesignerRevisionPersistenceKind.FD_ONLY) {
            throw new IllegalArgumentException("A metadata Source envelope requires a dirty FD_ONLY revision");
        }
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(limits.sourceLimits());
        DartSourceIntegrityResult source = scanner.scan(exactDart, saved.document().source());
        DartThreeWayIntegrityResult threeWay = new DartThreeWayIntegrityGate(scanner)
                .evaluate(source, saved.document().source(), saved.generation());
        if (!threeWay.onDiskThreeWayMatch()
                || !exactManagedPayloadsMatch(saved.sourceIntegrity(), source)) {
            throw new IllegalArgumentException("The metadata Source envelope must retain exact managed payloads");
        }
        return markSavedAtAnchor(saved, new DurableAnchor(saved.fdSnapshot(), saved.document(),
                exactDart.clone(), saved.generation(), source, threeWay),
                "Metadata Source-envelope history re-anchor");
    }

    private DesignerCommandSession markSavedAtAnchor(
            DesignerCommandRevision saved,
            DurableAnchor nextAnchor,
            String failureContext) {
        return markSavedAtAnchor(saved, nextAnchor, failureContext, null);
    }

    private DesignerCommandSession markSavedAtAnchor(DesignerCommandRevision saved,
            DurableAnchor nextAnchor, String failureContext, PreparedDesignerPair exactPhysicalSaved) {

        ArrayList<DesignerCommandRevision> nextRevisions = new ArrayList<>(
                revisions.size());
        DesignerCommandRevision nextSaved = null;
        long retainedBytes = 0;
        byte[] sourceBasis = exactPhysicalSaved != null ? exactPhysicalSaved.liveDartBytes()
                : saved.preparedPair().map(PreparedDesignerPair::liveDartBytes)
                .orElseGet(anchor::dartBytes);
        DartUserSourceProjection savedProjection = (exactPhysicalSaved != null
                ? exactPhysicalSaved.dartTransition().userSourceProjection() : saved.userSourceProjection())
                .rebaseOnto(sourceBasis, anchor.document().source());
        for (DesignerCommandRevision revision : revisions) {
            boolean retainHistoricalEnvelope = revision != saved && revision.historicalUserEnvelope();
            byte[] liveTemplate;
            DartUserSourceProjection sourceProjection;
            if (revision == saved) {
                liveTemplate = nextAnchor.dartBytes();
                sourceProjection = DartUserSourceProjection.identity(liveTemplate, nextAnchor.document().source());
            } else if (retainHistoricalEnvelope) {
                byte[] historical = revision.preparedPair().map(PreparedDesignerPair::liveDartBytes)
                        .orElseGet(revision::dartCandidateBytes);
                DartSourceIntegrityResult historicalIntegrity = new DartSourceIntegrityScanner(limits.sourceLimits())
                        .scan(historical, anchor.document().source());
                liveTemplate = projectManagedPayloads(historical, historicalIntegrity, nextAnchor);
                sourceProjection = revision.userSourceProjection().rebaseOnto(liveTemplate, nextAnchor.document().source());
            } else {
                liveTemplate = nextAnchor.dartBytes();
                sourceProjection = savedProjection.inverse()
                        .then(revision.userSourceProjection().rebaseOnto(sourceBasis, anchor.document().source()))
                        .rebaseOnto(liveTemplate, nextAnchor.document().source());
            }
            DerivedRevision derived = deriveRevision(
                    nextAnchor, revision.document(), revision.revisionId(), liveTemplate, sourceProjection);
            if (derived.revision().isEmpty()) {
                DesignerCommandDiagnostic failure = derived.diagnostic().orElseThrow();
                throw new IllegalStateException(
                        failureContext + " failed " + failure.code()
                        + " at " + failure.path() + ": " + failure.message());
            }
            DesignerCommandRevision next = derived.revision().orElseThrow();
            if (retainHistoricalEnvelope) next = next.preservingHistoricalUserEnvelope();
            if (revision == saved) {
                nextSaved = next;
            }
            if (wouldExceed(
                    retainedBytes,
                    next.retainedPairBytes(),
                    limits.maxRetainedPairBytes())) {
                throw new IllegalStateException(
                        failureContext
                        + " exceeds the retained command-history limit");
            }
            retainedBytes += next.retainedPairBytes();
            nextRevisions.add(next);
        }
        if (nextSaved == null
                || nextSaved.revisionId() != saved.revisionId()
                || nextSaved.persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE
                || !nextSaved.document().equals(nextAnchor.document())
                || !nextSaved.fdSnapshot().equals(nextAnchor.fd())
                || !Arrays.equals(
                        nextSaved.dartCandidateBytes(), nextAnchor.dartBytes())
                || nextSaved.sourceIntegrity() != nextAnchor.source()) {
            throw new IllegalStateException(
                    failureContext
                    + " did not derive one exact saved baseline revision");
        }
        ArrayList<DesignerCommandEdit> nextEdits = new ArrayList<>(edits.size());
        for (int index = 0; index < edits.size(); index++) {
            nextEdits.add(new DesignerCommandEdit(
                    edits.get(index).forward(),
                    nextRevisions.get(index),
                    nextRevisions.get(index + 1)));
        }
        return new DesignerCommandSession(
                catalog,
                limits,
                nextAnchor,
                nextRevisions,
                nextEdits,
                cursor,
                saved.revisionId(),
                nextRevisionId);
    }

    /**
     * Re-derives one retained semantic revision against an exact physical
     * live-source template under this session's current durable anchor.
     *
     * <p>The result is an immutable proof variant only and is not installed in
     * the command graph. Multiple templates may therefore produce distinct
     * exact revision objects with the same stable revision id. A template is
     * accepted only when its managed payload bytes exactly equal the current
     * durable anchor; normalized-hash equivalence alone is insufficient.</p>
     */
    public DesignerCommandRevision rederiveRetainedRevision(
            long revisionId,
            byte[] exactLiveSourceTemplate) {
        Objects.requireNonNull(
                exactLiveSourceTemplate, "exactLiveSourceTemplate");
        DesignerCommandRevision retained = retainedRevision(revisionId)
                .orElseThrow(() -> new IllegalArgumentException(
                "The command session does not retain revision id "
                + revisionId));
        DartSourceIntegrityResult template = new DartSourceIntegrityScanner(
                limits.sourceLimits()).scan(
                        exactLiveSourceTemplate, anchor.document().source());
        if (!template.onDiskDeclaredMatch()
                || !exactManagedPayloadsMatch(anchor.source(), template)) {
            throw new IllegalArgumentException(
                    "The physical live-source template is detached from the exact durable anchor");
        }

        DerivedRevision derived = deriveRevision(
                anchor,
                retained.document(),
                retained.revisionId(),
                exactLiveSourceTemplate,
                retained.generation(),
                retained.userSourceProjection());
        if (derived.revision().isEmpty()) {
            DesignerCommandDiagnostic failure = derived.diagnostic().orElseThrow();
            throw new IllegalArgumentException(
                    "Physical revision derivation failed " + failure.code()
                    + " at " + failure.path() + ": " + failure.message());
        }
        DesignerCommandRevision physical = derived.revision().orElseThrow();
        if (physical.revisionId() != retained.revisionId()
                || !physical.document().equals(retained.document())
                || !physical.fdSnapshot().equals(retained.fdSnapshot())
                || physical.generation() != retained.generation()
                || physical.persistenceKind() != retained.persistenceKind()
                || !exactManagedPayloadsMatch(
                        retained.sourceIntegrity(),
                        physical.sourceIntegrity())) {
            throw new IllegalStateException(
                    "The physical template changed the retained semantic revision");
        }
        if (physical.preparedPair().isPresent()) {
            PreparedDesignerPair pair = physical.preparedPair().orElseThrow();
            if (pair.baselineFd() != anchor.fd()
                    || pair.dartTransition().baseline() != anchor.threeWay()
                    || !Arrays.equals(
                            pair.baselineDartBytes(), anchor.dartBytes())
                    || !Arrays.equals(
                            pair.liveDartBytes(), exactLiveSourceTemplate)) {
                throw new IllegalStateException(
                        "The physical revision lost its exact anchor/template proof");
            }
        } else if (!Arrays.equals(
                physical.dartCandidateBytes(), exactLiveSourceTemplate)) {
            throw new IllegalStateException(
                    "The physical revision did not retain its exact live-source template");
        }
        return physical;
    }

    /**
     * Re-derives one retained semantic revision while preserving the unmanaged
     * envelope of an exact historical physical endpoint.
     *
     * <p>The historical template must first be a byte-exact valid source for
     * the requested retained revision. This method then replaces only its two
     * scanner-proven managed payload ranges with the exact payload bytes from
     * the current durable anchor, then reverses only the retained bounded user
     * contribution to reconstruct its live basis. The projected source is scanned again as an
     * exact durable-anchor template before the strict
     * {@link #rederiveRetainedRevision(long, byte[])} path is used. Normalized
     * hash equivalence alone never authorizes the projection.</p>
     *
     * <p>The result is immutable proof only and is not installed in the
     * command graph. This method performs no I/O and grants no persistence
     * authority.</p>
     */
    public DesignerCommandRevision rederiveRetainedRevisionByProjectingAnchor(
            long revisionId,
            byte[] exactHistoricalTemplate) {
        Objects.requireNonNull(
                exactHistoricalTemplate, "exactHistoricalTemplate");
        DesignerCommandRevision retained = retainedRevision(revisionId)
                .orElseThrow(() -> new IllegalArgumentException(
                "The command session does not retain revision id "
                + revisionId));
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(
                limits.sourceLimits());
        DartSourceIntegrityResult historical = scanner.scan(
                exactHistoricalTemplate, retained.document().source());
        if (!historical.onDiskDeclaredMatch()
                || !exactManagedPayloadsMatch(
                        retained.sourceIntegrity(), historical)) {
            throw new IllegalArgumentException(
                    "The historical physical template is detached from the exact retained revision");
        }

        byte[] projected = projectDurableManagedPayloads(
                exactHistoricalTemplate, historical);
        DartSourceIntegrityResult projectedIntegrity = scanner.scan(
                projected, anchor.document().source());
        if (!projectedIntegrity.onDiskDeclaredMatch()
                || !exactManagedPayloadsMatch(
                        anchor.source(), projectedIntegrity)) {
            throw new IllegalArgumentException(
                    "The historical physical template cannot accept the exact durable managed payloads");
        }

        // Historical endpoint bytes already contain this revision's deliberate member edits.
        // Reconstruct their exact live basis before applying the forward proof again. Otherwise
        // a source-only paired revision would collapse to a false BASELINE no-op.
        byte[] liveBasis = retained.userSourceProjection().inverse().applyTo(
                projected, anchor.document().source());
        DesignerCommandRevision physical = rederiveRetainedRevision(
                revisionId, liveBasis);
        if (wouldExceed(
                0,
                physical.retainedPairBytes(),
                limits.maxRetainedPairBytes())) {
            throw new IllegalArgumentException(
                    "The projected physical revision exceeds the retained command-history limit");
        }
        return physical;
    }

    private byte[] projectDurableManagedPayloads(
            byte[] exactHistoricalTemplate,
            DartSourceIntegrityResult historical) {
        return projectManagedPayloads(exactHistoricalTemplate, historical, anchor);
    }

    private byte[] projectManagedPayloads(byte[] exactHistoricalTemplate,
            DartSourceIntegrityResult historical, DurableAnchor targetAnchor) {
        byte[] durableBytes = targetAnchor.dartBytes();
        List<String> ids = List.of(
                DartSourceIntegrityScanner.IMPORTS_REGION,
                DartSourceIntegrityScanner.BUILD_REGION);
        ArrayList<DartManagedRegionSnapshot> historicalRegions =
                new ArrayList<>(ids.size());
        ArrayList<DartManagedRegionSnapshot> durableRegions =
                new ArrayList<>(ids.size());
        long projectedLength = exactHistoricalTemplate.length;
        int precedingHistoricalEnd = 0;
        for (String id : ids) {
            DartManagedRegionSnapshot historicalRegion = historical.region(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                    "The historical physical template lacks managed region "
                    + id));
            DartManagedRegionSnapshot durableRegion = targetAnchor.source().region(id)
                    .orElseThrow(() -> new IllegalStateException(
                    "The durable anchor lacks managed region " + id));
            if (historicalRegion.payloadStartByte() < precedingHistoricalEnd
                    || historicalRegion.payloadEndByte()
                        > exactHistoricalTemplate.length
                    || durableRegion.payloadEndByte() > durableBytes.length) {
                throw new IllegalArgumentException(
                        "Managed-region byte offsets cannot be projected safely");
            }
            precedingHistoricalEnd = historicalRegion.payloadEndByte();
            projectedLength -= historicalRegion.payloadLengthBytes();
            projectedLength += durableRegion.payloadLengthBytes();
            if (projectedLength > limits.sourceLimits().maxSourceBytes()) {
                throw new IllegalArgumentException(
                        "The projected physical template exceeds the Dart source limit");
            }
            historicalRegions.add(historicalRegion);
            durableRegions.add(durableRegion);
        }
        if (projectedLength < 0
                || projectedLength > Integer.MAX_VALUE
                || projectedLength > limits.sourceLimits().maxSourceBytes()) {
            throw new IllegalArgumentException(
                    "The projected physical template size is unsupported");
        }

        byte[] projected = new byte[(int) projectedLength];
        int historicalCursor = 0;
        int projectedCursor = 0;
        for (int index = 0; index < historicalRegions.size(); index++) {
            DartManagedRegionSnapshot historicalRegion =
                    historicalRegions.get(index);
            DartManagedRegionSnapshot durableRegion = durableRegions.get(index);
            int unmanagedLength = historicalRegion.payloadStartByte()
                    - historicalCursor;
            System.arraycopy(
                    exactHistoricalTemplate,
                    historicalCursor,
                    projected,
                    projectedCursor,
                    unmanagedLength);
            projectedCursor += unmanagedLength;
            System.arraycopy(
                    durableBytes,
                    durableRegion.payloadStartByte(),
                    projected,
                    projectedCursor,
                    durableRegion.payloadLengthBytes());
            projectedCursor += durableRegion.payloadLengthBytes();
            historicalCursor = historicalRegion.payloadEndByte();
        }
        int suffixLength = exactHistoricalTemplate.length - historicalCursor;
        System.arraycopy(
                exactHistoricalTemplate,
                historicalCursor,
                projected,
                projectedCursor,
                suffixLength);
        projectedCursor += suffixLength;
        if (projectedCursor != projected.length) {
            throw new IllegalStateException(
                    "Managed-region projection produced an inconsistent byte length");
        }
        return projected;
    }

    /**
     * Re-anchors a clean saved model revision to newer exact Dart bytes whose
     * managed regions still match the same durable {@code .fd} model.
     *
     * <p>This is the pure command-side half of an ordinary Source Save above a
     * saved Designer entry. The saved revision keeps its stable logical id and
     * becomes a new exact {@link DesignerRevisionPersistenceKind#BASELINE}
     * identity for {@code durableDart}. Older semantic revisions deliberately
     * retain their exact historical Dart candidates: the native Source edit
     * which introduced the newer unmanaged bytes remains a separate
     * chronological entry above them.</p>
     *
     * <p>The method performs no I/O and grants no persistence authority. A
     * caller must adopt the returned session only after the exact Source bytes
     * have committed durably and all peer model/history identities have been
     * verified.</p>
     */
    public DesignerCommandSession reanchorSavedSource(byte[] durableDart) {
        Objects.requireNonNull(durableDart, "durableDart");
        DesignerCommandRevision saved = current();
        if (dirty()
                || saved.persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE
                || !saved.fdSnapshot().equals(anchor.fd())
                || !saved.document().equals(anchor.document())) {
            throw new IllegalStateException(
                    "Only the exact clean saved model revision can accept a Source anchor");
        }

        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(
                limits.sourceLimits());
        DartSourceIntegrityResult source = scanner.scan(
                durableDart, saved.document().source());
        DartThreeWayIntegrityResult threeWay = new DartThreeWayIntegrityGate(
                scanner).evaluate(
                        source, saved.document().source(), saved.generation());
        if (!threeWay.onDiskThreeWayMatch()
                || !exactManagedPayloadsMatch(
                        saved.sourceIntegrity(), source)) {
            throw new IllegalStateException(
                    "The new Source anchor changes or invalidates managed Designer regions");
        }
        requireStateBindingFields(saved.document(), durableDart, source);
        DurableAnchor nextAnchor = new DurableAnchor(
                anchor.fd(),
                saved.document(),
                durableDart,
                saved.generation(),
                source,
                threeWay);
        ArrayList<DesignerCommandRevision> nextRevisions = new ArrayList<>(
                revisions.size());
        DesignerCommandRevision nextSaved = null;
        for (DesignerCommandRevision revision : revisions) {
            byte[] liveTemplate = revision == saved
                    ? durableDart
                    : revision.preparedPair()
                            .map(pair -> pair.liveDartBytes())
                            .orElseGet(revision::dartCandidateBytes);
            DerivedRevision derived = deriveRevision(
                    nextAnchor,
                    revision.document(),
                    revision.revisionId(),
                    liveTemplate,
                    revision == saved ? DartUserSourceProjection.identity(durableDart, saved.document().source())
                            : revision.userSourceProjection());
            if (derived.revision().isEmpty()) {
                DesignerCommandDiagnostic failure =
                        derived.diagnostic().orElseThrow();
                throw new IllegalStateException(
                        "Source-anchor rebase failed " + failure.code()
                        + " at " + failure.path() + ": " + failure.message());
            }
            DesignerCommandRevision next = derived.revision().orElseThrow();
            if (revision != saved) next = next.preservingHistoricalUserEnvelope();
            if (revision == saved) {
                nextSaved = next;
            } else if (next.revisionId() != revision.revisionId()
                    || next.persistenceKind() != revision.persistenceKind()
                    || !next.fdSnapshot().equals(revision.fdSnapshot())
                    || !Arrays.equals(
                            next.dartCandidateBytes(),
                            revision.dartCandidateBytes())) {
                throw new IllegalStateException(
                        "The Source anchor changed exact historical command outputs");
            }
            nextRevisions.add(next);
        }
        if (nextSaved == null) {
            throw new IllegalStateException(
                    "The Source-anchor rebase lost its saved revision");
        }
        if (nextSaved.persistenceKind()
                    != DesignerRevisionPersistenceKind.BASELINE
                || !Arrays.equals(
                        nextSaved.dartCandidateBytes(), durableDart)
                || !nextSaved.fdSnapshot().equals(anchor.fd())) {
            throw new IllegalStateException(
                    "The new Source anchor did not derive one exact saved baseline");
        }

        long retainedBytes = 0;
        for (DesignerCommandRevision revision : nextRevisions) {
            if (wouldExceed(
                    retainedBytes,
                    revision.retainedPairBytes(),
                    limits.maxRetainedPairBytes())) {
                throw new IllegalStateException(
                        "The Source-anchor rebase exceeds the retained command-history limit");
            }
            retainedBytes += revision.retainedPairBytes();
        }
        ArrayList<DesignerCommandEdit> nextEdits = new ArrayList<>(edits.size());
        for (int index = 0; index < edits.size(); index++) {
            nextEdits.add(new DesignerCommandEdit(
                    edits.get(index).forward(),
                    nextRevisions.get(index),
                    nextRevisions.get(index + 1)));
        }
        return new DesignerCommandSession(
                catalog,
                limits,
                nextAnchor,
                nextRevisions,
                nextEdits,
                cursor,
                savedRevisionId,
                nextRevisionId);
    }

    public WidgetCatalog catalog() {
        return catalog;
    }

    public DesignerCommandLimits limits() {
        return limits;
    }

    public DesignerCommandRevision current() {
        return revisions.get(cursor);
    }

    public int cursor() {
        return cursor;
    }

    public int revisionCount() {
        return revisions.size();
    }

    /**
     * Returns the exact revision object retained by this immutable session for
     * one stable revision id. A re-anchored session preserves the ids while
     * returning its newly derived revision identities.
     */
    public Optional<DesignerCommandRevision> retainedRevision(
            long revisionId) {
        if (revisionId < 0) {
            return Optional.empty();
        }
        for (DesignerCommandRevision revision : revisions) {
            if (revision.revisionId() == revisionId) {
                return Optional.of(revision);
            }
        }
        return Optional.empty();
    }

    public long savedRevisionId() {
        return savedRevisionId;
    }

    /**
     * Current history position of the saved revision, or empty when a new
     * branch discarded that revision while retaining its unique savepoint id.
     */
    public OptionalInt savedCursor() {
        for (int index = 0; index < revisions.size(); index++) {
            if (revisions.get(index).revisionId() == savedRevisionId) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    public boolean dirty() {
        return current().revisionId() != savedRevisionId;
    }

    public boolean canUndo() {
        return cursor > 0;
    }

    public boolean canRedo() {
        return cursor < edits.size();
    }

    /** Exact edit selected by the next Undo, for stable presentation/binding. */
    public Optional<DesignerCommandEdit> undoEdit() {
        return canUndo() ? Optional.of(edits.get(cursor - 1)) : Optional.empty();
    }

    /** Exact edit selected by the next Redo, for stable presentation/binding. */
    public Optional<DesignerCommandEdit> redoEdit() {
        return canRedo() ? Optional.of(edits.get(cursor)) : Optional.empty();
    }

    /** Exact durable .fd anchor used by every prospective revision. */
    public OriginalFdBytes durableFdAnchor() {
        return anchor.fd();
    }

    /** Clone-safe exact durable Dart anchor used by every prospective revision. */
    public byte[] durableDartAnchorBytes() {
        return anchor.dartBytes();
    }

    /** Exact source-integrity identity owned by the durable command anchor. */
    public DartSourceIntegrityResult durableSourceIntegrity() {
        return anchor.source();
    }

    /** Exact three-way identity owned by the durable command anchor. */
    public DartThreeWayIntegrityResult durableThreeWayIntegrity() {
        return anchor.threeWay();
    }

    private DesignerCommandSession copyAtCursor(int nextCursor) {
        return new DesignerCommandSession(
                catalog,
                limits,
                anchor,
                revisions,
                edits,
                nextCursor,
                savedRevisionId,
                nextRevisionId,
                acceptedSourceVariants);
    }

    private DerivedRevision deriveRevision(
            DurableAnchor durable,
            DesignerDocument semanticDocument,
            long revisionId) {
        return deriveRevision(
                durable, semanticDocument, revisionId, durable.dartBytes(),
                DartUserSourceProjection.identity(durable.dartBytes(), durable.document().source()));
    }

    private DerivedRevision deriveRevision(
            DurableAnchor durable,
            DesignerDocument semanticDocument,
            long revisionId,
            byte[] liveSourceBytes,
            DartUserSourceProjection userSourceProjection) {
        Objects.requireNonNull(liveSourceBytes, "liveSourceBytes");
        DesignerDocument normalized = DesignerCommandTransformer.withSource(
                semanticDocument, durable.document().source());
        DartGenerationResult generation = new DartRegionGenerator(
                limits.generationLimits(),
                limits.validationLimits()).generate(normalized, catalog);
        return deriveRevision(
                durable,
                normalized,
                revisionId,
                liveSourceBytes,
                generation,
                userSourceProjection);
    }

    /**
     * Completes derivation with one caller-pinned generation identity. Normal
     * derivation supplies its freshly generated identity; physical-envelope
     * projection supplies the already-retained command revision identity.
     */
    private DerivedRevision deriveRevision(
            DurableAnchor durable,
            DesignerDocument semanticDocument,
            long revisionId,
            byte[] liveSourceBytes,
            DartGenerationResult generation,
            DartUserSourceProjection userSourceProjection) {
        Objects.requireNonNull(liveSourceBytes, "liveSourceBytes");
        Objects.requireNonNull(generation, "generation");
        DesignerDocument normalized = DesignerCommandTransformer.withSource(
                semanticDocument, durable.document().source());
        if (!generation.successful()) {
            DartGenerationDiagnostic primary = generation.diagnostics().getFirst();
            return DerivedRevision.failure(
                    generationStatus(primary.code()),
                    diagnostic(
                            primary.code()
                                    == DartGenerationDiagnosticCode.SYMBOL_PROBE_LIMIT
                            ? DesignerCommandDiagnosticCode
                                    .SYMBOL_PROBE_CAPACITY_LIMIT
                            : DesignerCommandDiagnosticCode.GENERATION_FAILED,
                            primary.path(),
                            "Command generation failed " + primary.code()
                            + ": " + primary.message()));
        }

        DartSourceTransitionResult transition = new DartSourceTransitionPlanner(
                new DartSourceIntegrityScanner(limits.sourceLimits())).plan(
                        durable.threeWay(),
                        liveSourceBytes,
                        durable.document().source(),
                        generation,
                        userSourceProjection);
        if (transition.status() == DartSourceTransitionStatus.READY) {
            DartSourceTransitionPlan plan = transition.plan().orElseThrow();
            DesignerDocument prospective = DesignerCommandTransformer.withSource(
                    normalized, plan.prospectiveDescriptor());
            try {
                requireStateBindingFields(prospective, plan.candidateBytes(), plan.candidateIntegrity());
            } catch (IllegalArgumentException invalid) {
                return DerivedRevision.failure(DesignerCommandStatus.CONFLICT, diagnostic(
                        DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED, "/source/dartFile", invalid.getMessage()));
            }
            DesignerPairPreparationResult prepared =
                    new DesignerPairPreparationPlanner(
                            new FdDocumentCodec(limits.fdCodecLimits()))
                            .prepare(durable.fd(), prospective, plan);
            if (!prepared.ready()) {
                DesignerPairPreparationDiagnostic primary =
                        prepared.diagnostics().getFirst();
                return DerivedRevision.failure(
                        pairStatus(prepared.status()),
                        diagnostic(
                                DesignerCommandDiagnosticCode.PAIR_PREPARATION_FAILED,
                                primary.path(),
                                "Pair preparation failed " + primary.code()
                                + ": " + primary.message()));
            }
            PreparedDesignerPair pair = prepared.preparedPair().orElseThrow();
            return DerivedRevision.success(new DesignerCommandRevision(
                    revisionId,
                    prospective,
                    pair.prospectiveFd(),
                    generation,
                    pair.prospectiveDartBytes(),
                    plan.candidateIntegrity(),
                    Optional.of(plan),
                    Optional.of(pair),
                    DesignerRevisionPersistenceKind.PAIRED,
                    plan.userSourceProjection()));
        }
        if (transition.status() != DartSourceTransitionStatus.NO_CHANGES) {
            DartSourceTransitionDiagnostic primary = transition.diagnostics().getFirst();
            return DerivedRevision.failure(
                    transitionStatus(transition.status()),
                    diagnostic(
                            DesignerCommandDiagnosticCode.SOURCE_TRANSITION_FAILED,
                            primary.path(),
                            "Source transition failed " + primary.code()
                            + ": " + primary.message()));
        }

        DesignerDocument prospective = DesignerCommandTransformer.withSource(
                normalized, durable.document().source());
        OriginalFdBytes fd;
        try {
            fd = prospective.equals(durable.document())
                    ? durable.fd()
                    : new FdDocumentCodec(limits.fdCodecLimits()).encode(prospective);
        } catch (FdEncodeException failure) {
            return DerivedRevision.failure(
                    DesignerCommandStatus.UNAVAILABLE,
                    diagnostic(
                            DesignerCommandDiagnosticCode.FD_ENCODING_FAILED,
                            failure.diagnostic().pointer(),
                            "Canonical .fd encoding failed "
                            + failure.diagnostic().code() + ": "
                            + failure.diagnostic().message()));
        }
        byte[] dart = liveSourceBytes.clone();
        DartSourceIntegrityResult integrity = Arrays.equals(
                    dart, durable.dartBytes())
                && prospective.source().equals(durable.document().source())
                ? durable.source()
                : new DartSourceIntegrityScanner(
                        limits.sourceLimits()).scan(dart, prospective.source());
        if (!integrity.onDiskDeclaredMatch()) {
            DartSourceIntegrityDiagnostic primary = integrity.primaryDiagnostic()
                    .orElse(null);
            return DerivedRevision.failure(
                    DesignerCommandStatus.CONFLICT,
                    diagnostic(
                            DesignerCommandDiagnosticCode.SOURCE_CANDIDATE_INVALID,
                            primary == null ? "/source/dartFile" : primary.path(),
                            primary == null
                                    ? "The unchanged Dart candidate lost source integrity."
                                    : "The unchanged Dart candidate failed "
                                    + primary.code() + ": " + primary.message()));
        }
        DesignerRevisionPersistenceKind kind = fd.equals(durable.fd())
                ? DesignerRevisionPersistenceKind.BASELINE
                : DesignerRevisionPersistenceKind.FD_ONLY;
        try {
            requireStateBindingFields(prospective, dart, integrity);
        } catch (IllegalArgumentException invalid) {
            return DerivedRevision.failure(DesignerCommandStatus.CONFLICT, diagnostic(
                    DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED, "/source/dartFile", invalid.getMessage()));
        }
        return DerivedRevision.success(new DesignerCommandRevision(
                revisionId,
                prospective,
                fd,
                generation,
                dart,
                integrity,
                Optional.empty(),
                Optional.empty(),
                kind,
                userSourceProjection.rebaseOnto(liveSourceBytes, durable.document().source())));
    }

    private boolean physicalUserEnvelopeMatches(DesignerCommandRevision logical, PreparedDesignerPair pair) {
        if (acceptedSourceVariant(logical, pair)) return true;
        try {
            byte[] expected = logical.userSourceProjection().applyTo(pair.liveDartBytes(), anchor.document().source());
            return DartUserSourceProjection.sameUserEnvelope(expected, anchor.document().source(),
                    pair.prospectiveDartBytes(), pair.prospectiveDocument().source());
        } catch (IllegalArgumentException conflict) {
            return false;
        }
    }

    /**
     * Three-way hashes deliberately normalize generated Dart.  A Source-only
     * anchor needs the stronger byte contract: both managed payloads must be
     * exactly unchanged, while bytes outside them may differ freely.
     */
    private static boolean exactManagedPayloadsMatch(
            DartSourceIntegrityResult before,
            DartSourceIntegrityResult after) {
        if (before.original().isEmpty() || after.original().isEmpty()
                || before.regions().size() != after.regions().size()) {
            return false;
        }
        byte[] beforeBytes = before.original().orElseThrow().copyBytes();
        byte[] afterBytes = after.original().orElseThrow().copyBytes();
        for (var beforeRegion : before.regions()) {
            var afterRegion = after.region(beforeRegion.id()).orElse(null);
            if (afterRegion == null
                    || beforeRegion.payloadLengthBytes()
                        != afterRegion.payloadLengthBytes()
                    || !Arrays.equals(
                            Arrays.copyOfRange(
                                    beforeBytes,
                                    beforeRegion.payloadStartByte(),
                                    beforeRegion.payloadEndByte()),
                            Arrays.copyOfRange(
                                    afterBytes,
                                    afterRegion.payloadStartByte(),
                                    afterRegion.payloadEndByte()))) {
                return false;
            }
        }
        return true;
    }

    private long retainedBytesThrough(int inclusiveCursor) {
        long total = 0;
        for (int index = 0; index <= inclusiveCursor; index++) {
            long value = revisions.get(index).retainedPairBytes();
            if (wouldExceed(total, value, Long.MAX_VALUE)) {
                return Long.MAX_VALUE;
            }
            total += value;
        }
        return total;
    }

    private static boolean wouldExceed(long current, long addition, long maximum) {
        return current > maximum || addition > maximum - current;
    }

    private DesignerCommandSessionResult changed(
            DesignerCommandStatus status,
            DesignerCommandSession next,
            DesignerCommandEdit edit) {
        return new DesignerCommandSessionResult(
                status, next, Optional.of(edit), List.of());
    }

    private DesignerCommandSessionResult unchanged(
            DesignerCommandStatus status,
            DesignerCommandDiagnostic diagnostic) {
        return new DesignerCommandSessionResult(
                status, this, Optional.empty(), List.of(diagnostic));
    }

    private void validateState() {
        if (revisions.isEmpty() || edits.size() + 1 != revisions.size()) {
            throw new IllegalArgumentException("command history shape is inconsistent");
        }
        if (cursor < 0 || cursor >= revisions.size()) {
            throw new IllegalArgumentException("cursor is outside command history");
        }
        if (nextRevisionId <= revisions.stream()
                .mapToLong(DesignerCommandRevision::revisionId).max().orElseThrow()) {
            throw new IllegalArgumentException(
                    "nextRevisionId must exceed every retained revision identity");
        }
        for (int index = 0; index < edits.size(); index++) {
            DesignerCommandEdit edit = edits.get(index);
            if (edit.beforeRevision() != revisions.get(index)
                    || edit.afterRevision() != revisions.get(index + 1)) {
                throw new IllegalArgumentException(
                        "command edit does not bind adjacent exact revisions");
            }
        }
    }

    private static DesignerCommandSessionOpenResult threeWayOpenFailure(
            DartThreeWayIntegrityResult threeWay) {
        DartThreeWayIntegrityDiagnostic diagnostic = threeWay.diagnostics().stream()
                .findFirst().orElse(null);
        String path = diagnostic == null ? "/source/dartFile" : diagnostic.path();
        String detail = diagnostic == null
                ? "The exact Dart/.fd/generated baseline is not a three-way match."
                : "The exact baseline failed " + diagnostic.code()
                + ": " + diagnostic.message();
        DesignerCommandStatus status = switch (threeWay.status()) {
            case ON_DISK_THREE_WAY_MATCH -> throw new IllegalArgumentException(
                    "matching three-way evidence cannot be an open failure");
            case CONFLICT -> DesignerCommandStatus.CONFLICT;
            case UNSUPPORTED -> DesignerCommandStatus.UNSUPPORTED;
            case UNAVAILABLE -> DesignerCommandStatus.UNAVAILABLE;
        };
        return openFailure(
                status,
                DesignerCommandDiagnosticCode.BASELINE_SOURCE_CONFLICT,
                path,
                detail);
    }

    private static DesignerCommandSessionOpenResult openFailure(
            DesignerCommandStatus status,
            DesignerCommandDiagnosticCode code,
            String path,
            String message) {
        return new DesignerCommandSessionOpenResult(
                status,
                Optional.empty(),
                List.of(diagnostic(code, path, message)));
    }

    private static DesignerCommandDiagnostic diagnostic(
            DesignerCommandDiagnosticCode code,
            String path,
            String message) {
        return new DesignerCommandDiagnostic(
                code, path, Optional.empty(), message);
    }

    private static DesignerCommandStatus generationStatus(
            DartGenerationDiagnosticCode code) {
        return switch (code) {
            case UNSUPPORTED_WIDGET_KIND, DART_EXPRESSION_UNSUPPORTED ->
                DesignerCommandStatus.UNSUPPORTED;
            case SYMBOL_PROBE_LIMIT -> DesignerCommandStatus.LIMIT_EXCEEDED;
            case MODEL_INVALID, INVALID_UNICODE,
                    IMPORT_LIMIT, OUTPUT_SIZE_LIMIT,
                    INTERNAL_CATALOG_INCONSISTENCY ->
                DesignerCommandStatus.UNAVAILABLE;
        };
    }

    private static DesignerCommandStatus transitionStatus(
            DartSourceTransitionStatus status) {
        return switch (status) {
            case READY, NO_CHANGES -> throw new IllegalArgumentException(
                    "successful transition status cannot be mapped as a failure");
            case CONFLICT -> DesignerCommandStatus.CONFLICT;
            case UNSUPPORTED -> DesignerCommandStatus.UNSUPPORTED;
            case UNAVAILABLE -> DesignerCommandStatus.UNAVAILABLE;
        };
    }

    private static DesignerCommandStatus pairStatus(
            dev.flutter.netbeans.designer.pair.DesignerPairPreparationStatus status) {
        return switch (status) {
            case READY -> throw new IllegalArgumentException(
                    "READY pair preparation cannot be mapped as a failure");
            case NO_TRANSITION, CONFLICT -> DesignerCommandStatus.CONFLICT;
            case UNSUPPORTED -> DesignerCommandStatus.UNSUPPORTED;
            case UNAVAILABLE -> DesignerCommandStatus.UNAVAILABLE;
        };
    }

    private static boolean validationLimit(ValidationIssue issue) {
        return switch (issue.code()) {
            case WidgetTreeValidator.DEPTH_LIMIT,
                    WidgetTreeValidator.NODE_LIMIT,
                    WidgetTreeValidator.PROPERTY_LIMIT,
                    WidgetTreeValidator.SLOT_LIMIT,
                    WidgetTreeValidator.ISSUES_TRUNCATED -> true;
            default -> false;
        };
    }

    private record VerifiedBaselineEvidence(
            DartSourceIntegrityResult source,
            DartThreeWayIntegrityResult threeWay) {
        VerifiedBaselineEvidence {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(threeWay, "threeWay");
        }
    }

    private record DurableAnchor(
            OriginalFdBytes fd,
            DesignerDocument document,
            byte[] dartBytes,
            DartGenerationResult generation,
            DartSourceIntegrityResult source,
            DartThreeWayIntegrityResult threeWay) {
        DurableAnchor {
            Objects.requireNonNull(fd, "fd");
            Objects.requireNonNull(document, "document");
            dartBytes = Objects.requireNonNull(dartBytes, "dartBytes").clone();
            Objects.requireNonNull(generation, "generation");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(threeWay, "threeWay");
            if (!threeWay.onDiskThreeWayMatch()
                    || !source.onDiskDeclaredMatch()
                    || source.original().isEmpty()
                    || !source.original().orElseThrow().contentEquals(dartBytes)) {
                throw new IllegalArgumentException(
                        "durable anchor must retain exact matching pair evidence");
            }
        }

        @Override
        public byte[] dartBytes() {
            return dartBytes.clone();
        }
    }

    private record DerivedRevision(
            DesignerCommandStatus status,
            Optional<DesignerCommandRevision> revision,
            Optional<DesignerCommandDiagnostic> diagnostic) {
        static DerivedRevision success(DesignerCommandRevision revision) {
            return new DerivedRevision(
                    DesignerCommandStatus.READY,
                    Optional.of(revision),
                    Optional.empty());
        }

        static DerivedRevision failure(
                DesignerCommandStatus status,
                DesignerCommandDiagnostic diagnostic) {
            return new DerivedRevision(
                    status, Optional.empty(), Optional.of(diagnostic));
        }
    }
}
