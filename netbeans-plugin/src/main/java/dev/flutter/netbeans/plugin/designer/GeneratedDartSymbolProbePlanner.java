package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.api.DartCandidateCapacityBudget;
import dev.flutter.netbeans.dart.DartSymbolProbe;
import dev.flutter.netbeans.designer.generation.DartManagedRegionId;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegion;
import dev.flutter.netbeans.designer.generation.GeneratedDartRegions;
import dev.flutter.netbeans.designer.generation.GeneratedDartSymbolOccurrence;
import dev.flutter.netbeans.designer.pair.PreparedDesignerPair;
import dev.flutter.netbeans.designer.source.DartDesignerSuperclassOccurrence;
import dev.flutter.netbeans.designer.source.DartManagedRegionSnapshot;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Maps generator-owned payload occurrences to one exact Dart candidate. */
final class GeneratedDartSymbolProbePlanner {
    static final String DESIGNER_SUPERCLASS_PROBE_ID =
            "source:designer-superclass";

    private static final String FLUTTER_LIBRARY_PREFIX = "package:flutter/";
    private static final String WIDGETS_LIBRARY_URI =
            "package:flutter/widgets.dart";
    private static final String CLASS_TARGET_KIND = "CLASS";

    private GeneratedDartSymbolProbePlanner() {
    }

    static List<DartSymbolProbe> plan(
            PreparedDesignerPair prepared,
            Path trustedFlutterSdkRoot) {
        Objects.requireNonNull(prepared, "prepared");
        Objects.requireNonNull(trustedFlutterSdkRoot,
                "trustedFlutterSdkRoot");
        if (!trustedFlutterSdkRoot.isAbsolute()) {
            throw new IllegalArgumentException(
                    "trustedFlutterSdkRoot must be absolute");
        }

        GeneratedDartRegions generated = prepared.dartTransition()
                .generation()
                .generated()
                .orElseThrow(() -> new IllegalArgumentException(
                "The prepared transition has no generated Dart payloads"));
        DartCandidateCapacityBudget capacity =
                generated.candidateCapacityBudget();
        if (capacity.reservedSourceSymbolProbes() != 1) {
            throw new IllegalArgumentException(
                    "The generated capacity profile does not reserve the exact scanner probe");
        }
        if (generated.symbolOccurrences().isEmpty()) {
            throw new IllegalArgumentException(
                    "The generated Dart payload has no symbol-occurrence manifest");
        }
        byte[] candidateBytes = prepared.prospectiveDartBytes();
        if (hasUtf8Bom(candidateBytes)) {
            throw new IllegalArgumentException(
                    "The exact prospective Dart candidate must be BOM-free");
        }
        String candidate = decodeStrict(candidateBytes);
        int importsStart = payloadStartUtf16(
                candidateBytes,
                generated.imports(),
                region(prepared, DartManagedRegionId.IMPORTS));
        int buildStart = payloadStartUtf16(
                candidateBytes,
                generated.build(),
                region(prepared, DartManagedRegionId.BUILD));

        ArrayList<DartSymbolProbe> probes = new ArrayList<>();
        Path normalizedRoot = trustedFlutterSdkRoot.normalize();
        probes.add(superclassProbe(
                prepared,
                candidateBytes,
                candidate,
                superclassTargetRoot(normalizedRoot)));
        for (GeneratedDartSymbolOccurrence occurrence
                : generated.symbolOccurrences()) {
            if (!occurrence.libraryUri().startsWith(FLUTTER_LIBRARY_PREFIX)) {
                continue;
            }
            int payloadStart = switch (occurrence.region()) {
                case IMPORTS -> importsStart;
                case BUILD -> buildStart;
            };
            int candidateOffset = Math.addExact(payloadStart, occurrence.offset());
            int candidateEnd = Math.addExact(candidateOffset, occurrence.length());
            if (candidateEnd > candidate.length()
                    || !candidate.substring(candidateOffset, candidateEnd)
                            .equals(occurrence.symbolName())) {
                throw new IllegalArgumentException(
                        "Generated symbol occurrence does not identify the exact candidate: "
                        + occurrence.id());
            }
            probes.add(new DartSymbolProbe(
                    occurrence.id(),
                    candidateOffset,
                    occurrence.length(),
                    occurrence.symbolName(),
                    occurrence.libraryUri(),
                     normalizedRoot,
                     Optional.empty()));
        }
        if (probes.stream().noneMatch(probe -> probe.expectedSymbolName()
                        .equals("Widget"))
                || probes.stream().noneMatch(probe -> probe.expectedSymbolName()
                        .equals("BuildContext"))) {
            throw new IllegalArgumentException(
                    "Generated Dart symbol probes must include Widget and BuildContext");
        }
        probes.sort(Comparator.comparingInt(DartSymbolProbe::offset)
                .thenComparing(DartSymbolProbe::id));
        validateCombinedManifest(probes, capacity);
        return List.copyOf(probes);
    }

    private static DartSymbolProbe superclassProbe(
            PreparedDesignerPair prepared,
            byte[] candidateBytes,
            String candidate,
            Path expectedTargetRoot) {
        DartSourceIntegrityResult integrity = prepared.dartTransition()
                .candidateIntegrity();
        if (!integrity.onDiskDeclaredMatch()
                || integrity.original().isEmpty()
                || !integrity.original().orElseThrow()
                        .contentEquals(candidateBytes)) {
            throw new IllegalArgumentException(
                    "The exact candidate does not retain matching scanner evidence");
        }
        DartDesignerSuperclassOccurrence occurrence = integrity
                .superclassOccurrence()
                .orElseThrow(() -> new IllegalArgumentException(
                "The exact candidate has no scanner-owned Designer superclass occurrence"));
        if (!occurrence.className().equals(
                        prepared.dartTransition().prospectiveDescriptor().className())
                || !DartDesignerSuperclassOccurrence.SYMBOL_NAME.equals(
                        occurrence.symbolName())
                || occurrence.endByte() > candidateBytes.length) {
            throw new IllegalArgumentException(
                    "Scanner-owned superclass evidence does not describe the prospective class");
        }

        int startUtf16 = decodeStrict(
                candidateBytes, 0, occurrence.startByte()).length();
        int endUtf16 = decodeStrict(
                candidateBytes, 0, occurrence.endByte()).length();
        if (startUtf16 != occurrence.startUtf16()
                || endUtf16 != occurrence.endUtf16()
                || endUtf16 > candidate.length()
                || !candidate.substring(startUtf16, endUtf16)
                        .equals(occurrence.symbolName())) {
            throw new IllegalArgumentException(
                    "Scanner-owned superclass coordinates do not identify the exact candidate");
        }
        return new DartSymbolProbe(
                DESIGNER_SUPERCLASS_PROBE_ID,
                startUtf16,
                occurrence.lengthUtf16(),
                occurrence.symbolName(),
                WIDGETS_LIBRARY_URI,
                expectedTargetRoot,
                Optional.of(CLASS_TARGET_KIND));
    }

    private static Path superclassTargetRoot(Path trustedRoot) {
        Path frameworkLibrary = trustedRoot.resolve("packages")
                .resolve("flutter")
                .resolve("lib");
        if (!Files.isDirectory(frameworkLibrary)) {
            return trustedRoot;
        }
        try {
            Path trustedReal = trustedRoot.toRealPath();
            Path frameworkReal = frameworkLibrary.toRealPath();
            return frameworkReal.startsWith(trustedReal)
                    ? frameworkReal : trustedRoot;
        } catch (java.io.IOException | SecurityException unavailable) {
            return trustedRoot;
        }
    }

    private static void validateCombinedManifest(
            List<DartSymbolProbe> probes,
            DartCandidateCapacityBudget capacity) {
        if (probes.size() > capacity.maxSymbolProbes()) {
            throw new IllegalArgumentException(
                    "Dart candidate requires " + probes.size()
                    + " distinct Flutter symbol probes, exceeding shared capacity profile "
                    + capacity.profileId() + " with maxSymbolProbes="
                    + capacity.maxSymbolProbes());
        }
        Set<String> ids = new HashSet<>();
        int previousEnd = -1;
        for (DartSymbolProbe probe : probes) {
            if (!ids.add(probe.id())) {
                throw new IllegalArgumentException(
                        "Duplicate Dart symbol probe id: " + probe.id());
            }
            if (probe.offset() < previousEnd) {
                throw new IllegalArgumentException(
                        "Dart symbol probe occurrences overlap at " + probe.id());
            }
            previousEnd = Math.addExact(probe.offset(), probe.length());
        }
    }

    private static DartManagedRegionSnapshot region(
            PreparedDesignerPair prepared,
            DartManagedRegionId id) {
        return prepared.dartTransition()
                .candidateIntegrity()
                .region(id.wireName())
                .orElseThrow(() -> new IllegalArgumentException(
                "The exact candidate has no managed region " + id.wireName()));
    }

    private static int payloadStartUtf16(
            byte[] candidate,
            GeneratedDartRegion generated,
            DartManagedRegionSnapshot snapshot) {
        if (!snapshot.id().equals(generated.id().wireName())
                || snapshot.payloadEndByte() > candidate.length
                || !Arrays.equals(
                        Arrays.copyOfRange(
                                candidate,
                                snapshot.payloadStartByte(),
                                snapshot.payloadEndByte()),
                        generated.utf8Bytes())) {
            throw new IllegalArgumentException(
                    "Generated payload does not equal the exact candidate region "
                    + generated.id().wireName());
        }
        return decodeStrict(candidate, 0, snapshot.payloadStartByte()).length();
    }

    private static String decodeStrict(byte[] bytes) {
        return decodeStrict(bytes, 0, bytes.length);
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
    }

    private static String decodeStrict(byte[] bytes, int offset, int length) {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, length));
            return decoded.toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw new IllegalArgumentException(
                    "The exact Dart candidate is not strict UTF-8", invalidUtf8);
        }
    }
}
