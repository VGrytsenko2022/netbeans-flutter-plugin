package io.github.vgrytsenko2022.designer.source;

import io.github.vgrytsenko2022.designer.model.DartSourceDescriptor;
import io.github.vgrytsenko2022.designer.model.ManagedRegion;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * NetBeans-independent, fail-closed verifier for version 1 managed Dart source.
 * It never parses or evaluates expressions inside generated payloads.
 */
public final class DartSourceIntegrityScanner {

    public static final String IMPORTS_REGION = "imports";
    public static final String BUILD_REGION = "build";
    public static final String OPEN_PREFIX =
            "// <netbeans-flutter-designer region=\"";
    public static final String OPEN_SUFFIX = "\">";
    public static final String CLOSE_MARKER = "// </netbeans-flutter-designer>";

    private static final String MARKER_TOKEN = "netbeans-flutter-designer";
    private static final String MANAGED_REGIONS_PATH = "/source/managedRegions";
    private static final List<String> EXPECTED_REGION_ORDER =
            List.of(IMPORTS_REGION, BUILD_REGION);
    private static final int MAX_INTERPOLATION_NESTING = 128;

    private final DartSourceIntegrityLimits limits;

    public DartSourceIntegrityScanner() {
        this(DartSourceIntegrityLimits.defaults());
    }

    public DartSourceIntegrityScanner(DartSourceIntegrityLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public DartSourceIntegrityLimits limits() {
        return limits;
    }

    public DartSourceIntegrityResult scan(byte[] bytes, DartSourceDescriptor descriptor) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(descriptor, "descriptor");
        if (bytes.length > limits.maxSourceBytes()) {
            return DartSourceIntegrityResult.sourceTooLarge(
                    bytes.length, limits.maxSourceBytes());
        }

        OriginalDartBytes original = OriginalDartBytes.copyOf(bytes, limits);
        DecodedSource decoded;
        try {
            decoded = decode(bytes);
        } catch (CharacterCodingException invalidUtf8) {
            return new DartSourceIntegrityResult(
                    Optional.of(original),
                    List.of(),
                    List.of(DartSourceIntegrityDiagnostic.source(
                            DartSourceIntegrityDiagnosticCode.INVALID_UTF8,
                            "/source/dartFile",
                            "The paired Dart source is not strict UTF-8.")));
        }

        DiagnosticCollector diagnostics = new DiagnosticCollector(limits.maxDiagnostics());
        Structure structure = scanStructure(
                decoded.text(), descriptor.className(), limits.maxMarkers(), diagnostics,
                descriptor.widgetKind() == WidgetClassKind.STATEFUL);
        ClassSummary memberClass = structure.classSummary();
        String memberClassName = descriptor.className();
        if (descriptor.widgetKind() == WidgetClassKind.STATEFUL) {
            Optional<String> stateOwner = verifiedStateOwner(structure, descriptor.className());
            if (stateOwner.isEmpty()) {
                memberClass = null;
                diagnostics.add(DartSourceIntegrityDiagnostic.source(
                        DartSourceIntegrityDiagnosticCode.STATEFUL_SOURCE_BINDING_UNSUPPORTED,
                        "/source/widgetKind", "Stateful source requires one unambiguous top-level State<"
                        + descriptor.className() + "> owner returned directly by its zero-argument createState method."));
            } else {
                memberClassName = stateOwner.orElseThrow();
                memberClass = scanStructure(decoded.text(), memberClassName, limits.maxMarkers(),
                        diagnostics, false).classSummary();
            }
        }
        List<DartManagedRegionSnapshot> regions = verifyRegions(
                decoded,
                descriptor,
                structure.markers(),
                structure.classSummary(),
                memberClass,
                diagnostics);
        verifyClass(descriptor, structure.classSummary(), diagnostics);
        List<DartSourceIntegrityDiagnostic> finalDiagnostics = diagnostics.snapshot();
        Optional<DartDesignerSuperclassOccurrence> superclassOccurrence =
                verifiedSuperclassOccurrence(
                        original,
                        decoded,
                        descriptor.className(),
                        descriptor.widgetKind() == WidgetClassKind.STATEFUL ? "StatefulWidget" : "StatelessWidget",
                        structure.classSummary(),
                        finalDiagnostics);
        Optional<DartDesignerSuperclassOccurrence> stateSuperclass = descriptor.widgetKind() == WidgetClassKind.STATEFUL
                && memberClass != null ? verifiedSuperclassOccurrence(original, decoded, memberClassName,
                        "State", memberClass, finalDiagnostics) : Optional.empty();
        Optional<String> verifiedOwner = finalDiagnostics.isEmpty() && superclassOccurrence.isPresent()
                && (descriptor.widgetKind() == WidgetClassKind.STATELESS || stateSuperclass.isPresent())
                ? Optional.of(memberClassName) : Optional.empty();
        return new DartSourceIntegrityResult(
                Optional.of(original), regions, finalDiagnostics,
                superclassOccurrence, stateSuperclass, verifiedOwner);
    }

    private static Optional<DartDesignerSuperclassOccurrence>
            verifiedSuperclassOccurrence(
                    OriginalDartBytes original,
                    DecodedSource source,
                    String className,
                    String symbolName,
                    ClassSummary summary,
                    List<DartSourceIntegrityDiagnostic> diagnostics) {
        if (!diagnostics.isEmpty()
                || summary.matches() != 1
                || summary.declarationBraceDepth() != 0
                || summary.firstBaseQualified()
                || !symbolName.equals(
                        summary.firstBaseClass())
                || summary.topLevelTypeNames().contains(
                        symbolName)
                || summary.firstBaseStartUtf16() < 0
                || summary.firstBaseEndUtf16()
                        <= summary.firstBaseStartUtf16()) {
            return Optional.empty();
        }
        int startUtf16 = summary.firstBaseStartUtf16();
        int endUtf16 = summary.firstBaseEndUtf16();
        if (endUtf16 > source.text().length()
                || !source.text().substring(startUtf16, endUtf16)
                        .equals(symbolName)) {
            return Optional.empty();
        }
        return Optional.of(new DartDesignerSuperclassOccurrence(
                original,
                className,
                symbolName,
                startUtf16,
                endUtf16,
                source.byteOffsets()[startUtf16],
                source.byteOffsets()[endUtf16]));
    }

    private List<DartManagedRegionSnapshot> verifyRegions(
            DecodedSource source,
            DartSourceDescriptor descriptor,
            List<MarkerEvent> markers,
            ClassSummary classSummary,
            ClassSummary memberClass,
            DiagnosticCollector diagnostics) {
        List<FoundRegion> found = new ArrayList<>();
        Set<String> openedIds = new HashSet<>();
        OpenMarker active = null;
        int markerCount = 0;
        boolean markerLimitReported = false;

        for (MarkerEvent marker : markers) {
            markerCount++;
            if (markerCount > limits.maxMarkers()) {
                if (!markerLimitReported) {
                    diagnostics.add(DartSourceIntegrityDiagnostic.source(
                            DartSourceIntegrityDiagnosticCode.TOO_MANY_MARKERS,
                            MANAGED_REGIONS_PATH,
                            "The Dart source contains more than "
                            + limits.maxMarkers() + " structural designer markers."));
                    markerLimitReported = true;
                }
                continue;
            }

            if (marker.kind() == MarkerKind.MALFORMED) {
                diagnostics.add(DartSourceIntegrityDiagnostic.source(
                        DartSourceIntegrityDiagnosticCode.MALFORMED_MARKER,
                        MANAGED_REGIONS_PATH,
                        "A designer marker line is not in canonical form."));
                continue;
            }
            if (marker.kind() == MarkerKind.OPEN) {
                String id = marker.regionId();
                if (!EXPECTED_REGION_ORDER.contains(id)) {
                    diagnostics.add(DartSourceIntegrityDiagnostic.region(
                            DartSourceIntegrityDiagnosticCode.UNKNOWN_REGION,
                            MANAGED_REGIONS_PATH,
                            id,
                            "Managed region '" + id
                            + "' is not supported by schema version 1."));
                }
                if (!openedIds.add(id)) {
                    diagnostics.add(DartSourceIntegrityDiagnostic.region(
                            DartSourceIntegrityDiagnosticCode.DUPLICATE_REGION,
                            regionPath(id),
                            id,
                            "Managed region '" + id + "' occurs more than once."));
                }
                if (active != null) {
                    diagnostics.add(DartSourceIntegrityDiagnostic.region(
                            DartSourceIntegrityDiagnosticCode.NESTED_MARKER,
                            regionPath(id),
                            id,
                            "Managed region '" + id + "' is nested inside region '"
                            + active.id() + "'."));
                } else {
                    active = new OpenMarker(
                            id,
                            marker.afterLine(),
                            marker.lineStart(),
                            marker.scope());
                }
            } else if (active == null) {
                diagnostics.add(DartSourceIntegrityDiagnostic.source(
                        DartSourceIntegrityDiagnosticCode.UNMATCHED_CLOSE_MARKER,
                        MANAGED_REGIONS_PATH,
                        "A closing designer marker has no matching opening marker."));
            } else {
                found.add(new FoundRegion(
                        active.id(),
                        active.payloadStart(),
                        marker.lineStart(),
                        active.markerStart(),
                        marker.lineStart(),
                        active.scope(),
                        marker.scope()));
                active = null;
            }
        }

        if (active != null) {
            diagnostics.add(DartSourceIntegrityDiagnostic.region(
                    DartSourceIntegrityDiagnosticCode.UNCLOSED_MARKER,
                    regionPath(active.id()),
                    active.id(),
                    "Managed region '" + active.id() + "' has no closing marker."));
        }

        Map<String, List<FoundRegion>> byId = new HashMap<>();
        for (FoundRegion region : found) {
            byId.computeIfAbsent(region.id(), ignored -> new ArrayList<>()).add(region);
        }
        for (String expected : EXPECTED_REGION_ORDER) {
            if (!byId.containsKey(expected)) {
                diagnostics.add(DartSourceIntegrityDiagnostic.region(
                        DartSourceIntegrityDiagnosticCode.MISSING_REGION,
                        regionPath(expected),
                        expected,
                        "Required managed region '" + expected + "' is missing."));
            }
        }

        List<String> recognizedOrder = found.stream()
                .map(FoundRegion::id)
                .filter(EXPECTED_REGION_ORDER::contains)
                .toList();
        if (EXPECTED_REGION_ORDER.stream().allMatch(
                id -> byId.getOrDefault(id, List.of()).size() == 1)
                && !recognizedOrder.equals(EXPECTED_REGION_ORDER)) {
            diagnostics.add(DartSourceIntegrityDiagnostic.source(
                    DartSourceIntegrityDiagnosticCode.REORDERED_REGIONS,
                    MANAGED_REGIONS_PATH,
                    "Schema version 1 requires managed regions in order: imports, build."));
        }

        verifyRegionScopes(descriptor, classSummary, memberClass, byId, diagnostics);

        List<DartManagedRegionSnapshot> snapshots = new ArrayList<>(found.size());
        for (FoundRegion region : found) {
            if (region.payloadEnd() < region.payloadStart()) {
                diagnostics.add(DartSourceIntegrityDiagnostic.region(
                        DartSourceIntegrityDiagnosticCode.MALFORMED_MARKER,
                        regionPath(region.id()),
                        region.id(),
                        "Managed region '" + region.id()
                        + "' has invalid payload boundaries."));
                continue;
            }
            String payload = source.text().substring(
                    region.payloadStart(), region.payloadEnd());
            String hash = DartManagedRegionHashing.normalizedSha256(payload);
            snapshots.add(new DartManagedRegionSnapshot(
                    region.id(),
                    source.byteOffsets()[region.payloadStart()],
                    source.byteOffsets()[region.payloadEnd()],
                    hash));

            ManagedRegion expected = expectedRegion(descriptor, region.id());
            if (expected != null && !expected.sha256().equals(hash)) {
                diagnostics.add(DartSourceIntegrityDiagnostic.region(
                        DartSourceIntegrityDiagnosticCode.REGION_HASH_MISMATCH,
                        regionPath(region.id()) + "/sha256",
                        region.id(),
                        "Managed region '" + region.id() + "' has SHA-256 " + hash
                        + ", but the .fd model records " + expected.sha256() + "."));
            }
        }
        return List.copyOf(snapshots);
    }

    private static void verifyRegionScopes(
            DartSourceDescriptor descriptor,
            ClassSummary classSummary,
            ClassSummary memberClass,
            Map<String, List<FoundRegion>> byId,
            DiagnosticCollector diagnostics) {
        if (classSummary.matches() != 1) {
            return;
        }

        List<FoundRegion> imports = byId.getOrDefault(IMPORTS_REGION, List.of());
        if (imports.size() == 1) {
            FoundRegion region = imports.getFirst();
            if (!region.openScope().libraryLevel()
                    || !region.closeScope().libraryLevel()
                    || classSummary.declarationStart() < 0
                    || region.closeMarkerStart() >= classSummary.declarationStart()) {
                diagnostics.add(DartSourceIntegrityDiagnostic.region(
                        DartSourceIntegrityDiagnosticCode.REGION_SCOPE_MISMATCH,
                        regionPath(IMPORTS_REGION),
                        IMPORTS_REGION,
                        "Managed region 'imports' must be top-level and precede "
                        + "the declared designer class."));
            }
        }

        if (memberClass == null) {
            return;
        }

        List<FoundRegion> builds = byId.getOrDefault(BUILD_REGION, List.of());
        if (builds.size() == 1) {
            FoundRegion region = builds.getFirst();
            if (memberClass.bodyDepth() < 0
                    || memberClass.bodyEnd() < 0
                    || !region.openScope().directClassMember(memberClass.bodyDepth())
                    || !region.closeScope().directClassMember(memberClass.bodyDepth())
                    || region.openMarkerStart() < memberClass.bodyStart()
                    || region.closeMarkerStart() > memberClass.bodyEnd()) {
                diagnostics.add(DartSourceIntegrityDiagnostic.region(
                        DartSourceIntegrityDiagnosticCode.REGION_SCOPE_MISMATCH,
                        regionPath(BUILD_REGION),
                        BUILD_REGION,
                        "Managed region 'build' must wrap a direct member of the verified "
                        + (descriptor.widgetKind() == WidgetClassKind.STATEFUL ? "State owner for '" : "Dart class '")
                        + descriptor.className() + "'."));
            }
        }
    }

    private static ManagedRegion expectedRegion(
            DartSourceDescriptor descriptor,
            String id) {
        return switch (id) {
            case IMPORTS_REGION -> descriptor.managedRegions().imports();
            case BUILD_REGION -> descriptor.managedRegions().build();
            default -> null;
        };
    }

    private static void verifyClass(
            DartSourceDescriptor descriptor,
            ClassSummary summary,
            DiagnosticCollector diagnostics) {
        if (summary.matches() == 0) {
            diagnostics.add(DartSourceIntegrityDiagnostic.source(
                    DartSourceIntegrityDiagnosticCode.CLASS_MISSING,
                    "/source/className",
                    "Dart class '" + descriptor.className() + "' was not found."));
            return;
        }
        if (summary.matches() > 1) {
            diagnostics.add(DartSourceIntegrityDiagnostic.source(
                    DartSourceIntegrityDiagnosticCode.CLASS_DUPLICATE,
                    "/source/className",
                    "Dart class '" + descriptor.className()
                    + "' is declared more than once."));
            return;
        }

        if (summary.declarationBraceDepth() != 0) {
            diagnostics.add(DartSourceIntegrityDiagnostic.source(
                    DartSourceIntegrityDiagnosticCode.CLASS_SCOPE_MISMATCH,
                    "/source/className",
                    "Dart class '" + descriptor.className()
                    + "' must be declared at lexical top level."));
        }

        String expectedBase = descriptor.widgetKind() == WidgetClassKind.STATEFUL
                ? "StatefulWidget" : "StatelessWidget";
        if (summary.topLevelTypeNames().contains(expectedBase)) {
            diagnostics.add(DartSourceIntegrityDiagnostic.source(
                    DartSourceIntegrityDiagnosticCode.LOCAL_WIDGET_BASE_SHADOWED,
                    "/source/widgetKind",
                    "A top-level Dart declaration named '" + expectedBase
                    + "' shadows the Flutter widget base required by class '"
                    + descriptor.className() + "'."));
        } else if (summary.firstBaseQualified()) {
            diagnostics.add(DartSourceIntegrityDiagnostic.source(
                    DartSourceIntegrityDiagnosticCode.QUALIFIED_WIDGET_BASE_UNSUPPORTED,
                    "/source/widgetKind",
                    "Dart class '" + descriptor.className()
                    + "' uses a qualified widget base. Import-prefix binding is "
                    + "not supported by this source-integrity contract."));
        } else if (!expectedBase.equals(summary.firstBaseClass())) {
            diagnostics.add(DartSourceIntegrityDiagnostic.source(
                    DartSourceIntegrityDiagnosticCode.CLASS_KIND_MISMATCH,
                    "/source/widgetKind",
                    "Dart class '" + descriptor.className() + "' must extend "
                    + expectedBase + ", but extends "
                    + (summary.firstBaseClass() == null
                            ? "no supported widget base class"
                            : summary.firstBaseClass()) + "."));
        }
    }

    private static Structure scanStructure(
            String source,
            String className,
            int maximumMarkers,
            DiagnosticCollector diagnostics,
            boolean retainTokens) {
        List<MarkerEvent> markers = new ArrayList<>();
        List<SourceToken> tokens = new ArrayList<>();
        ClassTracker classes = new ClassTracker(className);
        DelimiterTracker delimiters = new DelimiterTracker();
        int index = 0;
        while (index < source.length()) {
            if (retainTokens && tokens.size() >= 500_000) {
                diagnostics.add(DartSourceIntegrityDiagnostic.source(
                        DartSourceIntegrityDiagnosticCode.STATEFUL_SOURCE_BINDING_UNSUPPORTED,
                        "/source/widgetKind", "Stateful source exceeds the bounded lexical token limit."));
                break;
            }
            if (index == 0 && source.startsWith("#!")) {
                index = lineContentEnd(source, index);
                continue;
            }
            char current = source.charAt(index);
            if (current == '/' && index + 1 < source.length()) {
                char next = source.charAt(index + 1);
                if (next == '/') {
                    int lineEnd = lineContentEnd(source, index);
                    if (onlyIndentationBefore(source, index)) {
                        String comment = source.substring(index, lineEnd);
                        MarkerEvent marker = marker(
                                comment,
                                lineStart(source, index),
                                nextLineOffset(source, lineEnd),
                                delimiters.scope());
                        if (marker != null && markers.size() <= maximumMarkers) {
                            // Retain at most one sentinel beyond the limit so
                            // verifyRegions can emit TOO_MANY_MARKERS without
                            // accumulating attacker-controlled marker events.
                            markers.add(marker);
                        }
                    }
                    index = lineEnd;
                    continue;
                }
                if (next == '*') {
                    SkipResult skipped = skipBlockComment(source, index);
                    if (!skipped.terminated()) {
                        diagnostics.add(DartSourceIntegrityDiagnostic.source(
                                DartSourceIntegrityDiagnosticCode.UNTERMINATED_BLOCK_COMMENT,
                                "/source/dartFile",
                                "The Dart source contains an unterminated block comment."));
                        break;
                    }
                    index = skipped.nextIndex();
                    continue;
                }
            }
            if (current == '\'' || current == '"') {
                if (retainTokens) tokens.add(new SourceToken("<string>", index, delimiters.scope()));
                SkipResult skipped = skipString(source, index, 0);
                if (!skipped.terminated()) {
                    DartSourceIntegrityDiagnosticCode code = skipped.nestingTooDeep()
                            ? DartSourceIntegrityDiagnosticCode.LEXICAL_NESTING_TOO_DEEP
                            : DartSourceIntegrityDiagnosticCode.UNTERMINATED_STRING;
                    String message = skipped.nestingTooDeep()
                            ? "Dart string interpolation exceeds the bounded lexical nesting limit."
                            : "The Dart source contains an unterminated string literal.";
                    diagnostics.add(DartSourceIntegrityDiagnostic.source(
                            code,
                            "/source/dartFile",
                            message));
                    break;
                }
                index = skipped.nextIndex();
                continue;
            }
            if (isIdentifierStart(current)) {
                int end = index + 1;
                while (end < source.length() && isIdentifierPart(source.charAt(end))) {
                    end++;
                }
                classes.identifier(
                        source.substring(index, end), index, delimiters.braceDepth());
                if (retainTokens) tokens.add(new SourceToken(source.substring(index, end), index, delimiters.scope()));
                index = end;
                continue;
            }
            classes.symbol(current, index, delimiters.braceDepth());
            if (retainTokens && !Character.isWhitespace(current)) {
                tokens.add(new SourceToken(String.valueOf(current), index, delimiters.scope()));
            }
            delimiters.symbol(current, diagnostics);
            index++;
        }
        delimiters.finish(diagnostics);
        return new Structure(List.copyOf(markers), classes.summary(), List.copyOf(tokens));
    }

    /** Resolves only direct, synchronous constructor-return createState shapes. */
    private static Optional<String> verifiedStateOwner(Structure structure, String owner) {
        List<SourceToken> tokens = structure.tokens();
        if (structure.classSummary().matches() != 1
                || structure.classSummary().topLevelTypeNames().contains("State")) {
            return Optional.empty();
        }
        List<SourceClass> classes = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!value(tokens, i).equals("class") || !tokens.get(i).scope().libraryLevel()) continue;
            int open = i + 2;
            while (open < tokens.size() && !value(tokens, open).equals("{")
                    && !value(tokens, open).equals(";")) open++;
            if (!value(tokens, open).equals("{") || !tokens.get(open).scope().libraryLevel()) continue;
            int close = open + 1;
            while (close < tokens.size() && !(value(tokens, close).equals("}")
                    && tokens.get(close).scope().braceDepth() == 1
                    && tokens.get(close).scope().parenthesisDepth() == 0
                    && tokens.get(close).scope().bracketDepth() == 0)) close++;
            if (close == tokens.size()) return Optional.empty();
            classes.add(new SourceClass(value(tokens, i + 1), i, open, close));
        }
        List<SourceClass> roots = classes.stream().filter(type -> type.name().equals(owner)).toList();
        if (roots.size() != 1) return Optional.empty();
        SourceClass root = roots.getFirst();
        if (!values(tokens, root.start(), root.open()).equals(List.of("class", owner, "extends", "StatefulWidget"))) {
            return Optional.empty();
        }
        List<SourceClass> states = classes.stream().filter(type -> {
            List<String> header = values(tokens, type.start(), type.open());
            return header.equals(List.of("class", type.name(), "extends", "State", "<", owner, ">"));
        }).toList();
        if (states.size() != 1) return Optional.empty();
        SourceClass state = states.getFirst();
        if (classes.stream().filter(type -> type.name().equals(state.name())).count() != 1) return Optional.empty();

        List<Integer> declarations = new ArrayList<>();
        for (int i = root.open() + 1; i < root.close(); i++) {
            if (value(tokens, i).equals("createState") && tokens.get(i).scope().directClassMember(1)) {
                declarations.add(i);
            }
        }
        if (declarations.size() != 1) return Optional.empty();
        int method = declarations.getFirst();
        int returnStart;
        if (method >= 4 && values(tokens, method - 4, method).equals(List.of("State", "<", owner, ">"))) {
            returnStart = method - 4;
        } else if (value(tokens, method - 1).equals(state.name())) {
            returnStart = method - 1;
        } else {
            return Optional.empty();
        }
        int prefix = returnStart;
        if (value(tokens, prefix - 2).equals("@") && value(tokens, prefix - 1).equals("override")) prefix -= 2;
        int previous = prefix - 1;
        boolean memberBoundary = previous == root.open()
                || previous >= 0 && value(tokens, previous).equals(";")
                        && tokens.get(previous).scope().directClassMember(1)
                || previous >= 0 && value(tokens, previous).equals("}")
                        && tokens.get(previous).scope().braceDepth() == 2
                        && tokens.get(previous).scope().parenthesisDepth() == 0
                        && tokens.get(previous).scope().bracketDepth() == 0;
        if (!memberBoundary || !values(tokens, method + 1, Math.min(method + 3, tokens.size())).equals(List.of("(", ")"))) {
            return Optional.empty();
        }
        int body = method + 3;
        List<String> arrow = List.of("=", ">", state.name(), "(", ")", ";");
        List<String> block = List.of("{", "return", state.name(), "(", ")", ";", "}");
        if (!(matches(tokens, body, arrow) || matches(tokens, body, block))) return Optional.empty();
        for (int i = root.open() + 1; i < root.close(); i++) {
            if (value(tokens, i).equals(state.name()) && i != body + 2 && i != returnStart) {
                // An identically named field/helper can turn the apparent constructor
                // invocation into an instance call. A resolver is required for that shape.
                return Optional.empty();
            }
        }
        return Optional.of(state.name());
    }

    private static String value(List<SourceToken> tokens, int index) {
        return index < 0 || index >= tokens.size() ? "" : tokens.get(index).value();
    }

    private static List<String> values(List<SourceToken> tokens, int start, int end) {
        if (start < 0 || end > tokens.size() || end < start) return List.of();
        return tokens.subList(start, end).stream().map(SourceToken::value).toList();
    }

    private static boolean matches(List<SourceToken> tokens, int start, List<String> expected) {
        return values(tokens, start, start + expected.size()).equals(expected);
    }

    private static MarkerEvent marker(
            String comment,
            int lineStart,
            int afterLine,
            DelimiterScope scope) {
        if (comment.equals(CLOSE_MARKER)) {
            return new MarkerEvent(
                    MarkerKind.CLOSE, "", lineStart, afterLine, scope);
        }
        if (comment.startsWith(OPEN_PREFIX) && comment.endsWith(OPEN_SUFFIX)) {
            String id = comment.substring(
                    OPEN_PREFIX.length(), comment.length() - OPEN_SUFFIX.length());
            if (validRegionId(id) && comment.equals(openingMarker(id))) {
                return new MarkerEvent(
                        MarkerKind.OPEN, id, lineStart, afterLine, scope);
            }
            return new MarkerEvent(
                    MarkerKind.MALFORMED, "", lineStart, afterLine, scope);
        }
        String body = comment.length() >= 2 ? comment.substring(2).stripLeading() : "";
        if (body.startsWith("<" + MARKER_TOKEN)
                || body.startsWith("</" + MARKER_TOKEN)) {
            return new MarkerEvent(
                    MarkerKind.MALFORMED, "", lineStart, afterLine, scope);
        }
        return null;
    }

    private static SkipResult skipString(
            String source,
            int quoteStart,
            int interpolationNesting) {
        char quote = source.charAt(quoteStart);
        boolean triple = quoteStart + 2 < source.length()
                && source.charAt(quoteStart + 1) == quote
                && source.charAt(quoteStart + 2) == quote;
        boolean raw = quoteStart > 0
                && (source.charAt(quoteStart - 1) == 'r'
                || source.charAt(quoteStart - 1) == 'R')
                && (quoteStart < 2 || !isIdentifierPart(source.charAt(quoteStart - 2)));
        int delimiterLength = triple ? 3 : 1;
        int index = quoteStart + delimiterLength;
        while (index < source.length()) {
            if (matchesDelimiter(source, index, quote, delimiterLength)) {
                return new SkipResult(index + delimiterLength, true, false);
            }
            char current = source.charAt(index);
            if (!triple && (current == '\r' || current == '\n')) {
                return new SkipResult(source.length(), false, false);
            }
            if (!raw && current == '\\') {
                index = Math.min(source.length(), index + 2);
                continue;
            }
            if (!raw && current == '$' && index + 1 < source.length()
                    && source.charAt(index + 1) == '{') {
                if (interpolationNesting >= MAX_INTERPOLATION_NESTING) {
                    return new SkipResult(source.length(), false, true);
                }
                SkipResult interpolation = skipInterpolation(
                        source, index + 2, interpolationNesting + 1);
                if (!interpolation.terminated()) {
                    return interpolation;
                }
                index = interpolation.nextIndex();
                continue;
            }
            index++;
        }
        return new SkipResult(source.length(), false, false);
    }

    private static SkipResult skipInterpolation(
            String source,
            int start,
            int interpolationNesting) {
        int depth = 1;
        int index = start;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\'' || current == '"') {
                SkipResult string = skipString(source, index, interpolationNesting);
                if (!string.terminated()) {
                    return string;
                }
                index = string.nextIndex();
                continue;
            }
            if (current == '/' && index + 1 < source.length()) {
                char next = source.charAt(index + 1);
                if (next == '/') {
                    index = lineContentEnd(source, index);
                    continue;
                }
                if (next == '*') {
                    SkipResult comment = skipBlockComment(source, index);
                    if (!comment.terminated()) {
                        return comment;
                    }
                    index = comment.nextIndex();
                    continue;
                }
            }
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return new SkipResult(index + 1, true, false);
            }
            index++;
        }
        return new SkipResult(source.length(), false, false);
    }

    private static SkipResult skipBlockComment(String source, int start) {
        int depth = 1;
        int index = start + 2;
        while (index < source.length()) {
            if (index + 1 < source.length()
                    && source.charAt(index) == '/'
                    && source.charAt(index + 1) == '*') {
                depth++;
                index += 2;
            } else if (index + 1 < source.length()
                    && source.charAt(index) == '*'
                    && source.charAt(index + 1) == '/') {
                depth--;
                index += 2;
                if (depth == 0) {
                    return new SkipResult(index, true, false);
                }
            } else {
                index++;
            }
        }
        return new SkipResult(source.length(), false, false);
    }

    private static DecodedSource decode(byte[] bytes) throws CharacterCodingException {
        int bomBytes = bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF ? 3 : 0;
        String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, bomBytes, bytes.length - bomBytes))
                .toString();
        int[] byteOffsets = new int[text.length() + 1];
        int byteOffset = bomBytes;
        int index = 0;
        while (index < text.length()) {
            byteOffsets[index] = byteOffset;
            int codePoint = text.codePointAt(index);
            int charCount = Character.charCount(codePoint);
            if (charCount == 2) {
                byteOffsets[index + 1] = byteOffset;
            }
            byteOffset += utf8Length(codePoint);
            index += charCount;
        }
        byteOffsets[text.length()] = byteOffset;
        if (byteOffset != bytes.length) {
            throw new IllegalStateException("UTF-8 offset mapping did not consume the source");
        }
        return new DecodedSource(text, byteOffsets);
    }

    private static int utf8Length(int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        }
        if (codePoint <= 0x7FF) {
            return 2;
        }
        if (codePoint <= 0xFFFF) {
            return 3;
        }
        return 4;
    }

    private static boolean onlyIndentationBefore(String source, int offset) {
        int start = lineStart(source, offset);
        for (int index = start; index < offset; index++) {
            char value = source.charAt(index);
            if (value != ' ' && value != '\t') {
                return false;
            }
        }
        return true;
    }

    private static int lineStart(String source, int offset) {
        int start = offset;
        while (start > 0) {
            char previous = source.charAt(start - 1);
            if (previous == '\n' || previous == '\r') {
                break;
            }
            start--;
        }
        return start;
    }

    private static int lineContentEnd(String source, int start) {
        int end = start;
        while (end < source.length()) {
            char current = source.charAt(end);
            if (current == '\r' || current == '\n') {
                break;
            }
            end++;
        }
        return end;
    }

    private static int nextLineOffset(String source, int contentEnd) {
        if (contentEnd >= source.length()) {
            return source.length();
        }
        if (source.charAt(contentEnd) == '\r'
                && contentEnd + 1 < source.length()
                && source.charAt(contentEnd + 1) == '\n') {
            return contentEnd + 2;
        }
        return contentEnd + 1;
    }

    private static boolean matchesDelimiter(
            String source,
            int index,
            char quote,
            int length) {
        if (index + length > source.length()) {
            return false;
        }
        for (int offset = 0; offset < length; offset++) {
            if (source.charAt(index + offset) != quote) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifierStart(char value) {
        return value == '_' || value == '$' || Character.isUnicodeIdentifierStart(value);
    }

    private static boolean isIdentifierPart(char value) {
        return value == '_' || value == '$' || Character.isUnicodeIdentifierPart(value);
    }

    private static boolean validRegionId(String id) {
        if (id.isEmpty() || id.length() > 128) {
            return false;
        }
        for (int index = 0; index < id.length(); index++) {
            char value = id.charAt(index);
            if (!((value >= 'a' && value <= 'z')
                    || (value >= 'A' && value <= 'Z')
                    || (value >= '0' && value <= '9')
                    || value == '.' || value == '_' || value == ':' || value == '-')) {
                return false;
            }
        }
        return true;
    }

    private static String openingMarker(String id) {
        return OPEN_PREFIX + id + OPEN_SUFFIX;
    }

    private static String regionPath(String id) {
        return EXPECTED_REGION_ORDER.contains(id)
                ? MANAGED_REGIONS_PATH + "/" + id
                : MANAGED_REGIONS_PATH;
    }

    private static final class DiagnosticCollector {
        private final int maximum;
        private final List<DartSourceIntegrityDiagnostic> diagnostics = new ArrayList<>();

        DiagnosticCollector(int maximum) {
            this.maximum = maximum;
        }

        void add(DartSourceIntegrityDiagnostic diagnostic) {
            if (diagnostics.size() < maximum) {
                diagnostics.add(diagnostic);
            } else if (diagnostic.code().isUnsupportedSourceShape()
                    && diagnostics.stream().noneMatch(existing ->
                            existing.code().isUnsupportedSourceShape())) {
                // Aggregate status must not depend on diagnostic truncation.
                // Preserve at least one capability/source-shape explanation.
                diagnostics.set(diagnostics.size() - 1, diagnostic);
            }
        }

        List<DartSourceIntegrityDiagnostic> snapshot() {
            return List.copyOf(diagnostics);
        }
    }

    private static final class DelimiterTracker {
        private final ArrayDeque<Character> stack = new ArrayDeque<>();
        private int braceDepth;
        private int parenthesisDepth;
        private int bracketDepth;

        int braceDepth() {
            return braceDepth;
        }

        DelimiterScope scope() {
            return new DelimiterScope(braceDepth, parenthesisDepth, bracketDepth);
        }

        void symbol(char symbol, DiagnosticCollector diagnostics) {
            switch (symbol) {
                case '{', '(', '[' -> push(symbol);
                case '}', ')', ']' -> close(symbol, diagnostics);
                default -> {
                    // Not a structural Dart delimiter.
                }
            }
        }

        void finish(DiagnosticCollector diagnostics) {
            if (!stack.isEmpty()) {
                diagnostics.add(DartSourceIntegrityDiagnostic.source(
                        DartSourceIntegrityDiagnosticCode.UNCLOSED_DELIMITER,
                        "/source/dartFile",
                        "The Dart source contains " + stack.size()
                        + " unclosed structural delimiter(s)."));
            }
        }

        private void push(char symbol) {
            stack.push(symbol);
            adjust(symbol, 1);
        }

        private void close(char symbol, DiagnosticCollector diagnostics) {
            char expectedOpen = switch (symbol) {
                case '}' -> '{';
                case ')' -> '(';
                case ']' -> '[';
                default -> throw new IllegalArgumentException("not a closing delimiter");
            };
            if (stack.isEmpty() || stack.peek() != expectedOpen) {
                diagnostics.add(DartSourceIntegrityDiagnostic.source(
                        DartSourceIntegrityDiagnosticCode.UNMATCHED_DELIMITER,
                        "/source/dartFile",
                        "Closing delimiter '" + symbol
                        + "' does not match the current Dart lexical scope."));
                return;
            }
            stack.pop();
            adjust(expectedOpen, -1);
        }

        private void adjust(char symbol, int delta) {
            switch (symbol) {
                case '{' -> braceDepth += delta;
                case '(' -> parenthesisDepth += delta;
                case '[' -> bracketDepth += delta;
                default -> throw new IllegalArgumentException("not an opening delimiter");
            }
        }
    }

    private static final class ClassTracker {
        private final String targetName;
        private boolean awaitingClassName;
        private boolean targetHeader;
        private boolean readingBase;
        private boolean expectingQualifiedBase;
        private boolean awaitingTopLevelTypeName;
        private boolean awaitingExtensionTypeKeyword;
        private int typeParameterDepth;
        private int matches;
        private int pendingClassStart = -1;
        private int pendingClassBraceDepth = -1;
        private int declarationStart = -1;
        private int declarationBraceDepth = -1;
        private int bodyStart = -1;
        private int bodyEnd = -1;
        private int bodyDepth = -1;
        private boolean insideTargetBody;
        private String currentBase;
        private int currentBaseStartUtf16 = -1;
        private int currentBaseEndUtf16 = -1;
        private boolean currentBaseQualified;
        private String firstBase;
        private int firstBaseStartUtf16 = -1;
        private int firstBaseEndUtf16 = -1;
        private boolean firstBaseQualified;
        private final Set<String> topLevelTypeNames = new HashSet<>();

        ClassTracker(String targetName) {
            this.targetName = targetName;
        }

        void identifier(String identifier, int offset, int currentBraceDepth) {
            if (awaitingTopLevelTypeName) {
                if (identifier.equals("class")) {
                    // `mixin class Name` keeps Name as the declaration.
                    return;
                }
                awaitingTopLevelTypeName = false;
                topLevelTypeNames.add(identifier);
                return;
            }
            if (awaitingExtensionTypeKeyword) {
                awaitingExtensionTypeKeyword = false;
                if (identifier.equals("type")) {
                    awaitingTopLevelTypeName = true;
                }
                return;
            }
            if (awaitingClassName) {
                awaitingClassName = false;
                if (pendingClassBraceDepth == 0) {
                    topLevelTypeNames.add(identifier);
                }
                if (identifier.equals(targetName)) {
                    matches++;
                    targetHeader = true;
                    if (matches == 1) {
                        declarationStart = pendingClassStart;
                        declarationBraceDepth = pendingClassBraceDepth;
                    }
                    currentBase = null;
                    currentBaseStartUtf16 = -1;
                    currentBaseEndUtf16 = -1;
                    currentBaseQualified = false;
                    readingBase = false;
                    expectingQualifiedBase = false;
                    typeParameterDepth = 0;
                }
                return;
            }
            if (currentBraceDepth == 0) {
                if (identifier.equals("typedef")
                        || identifier.equals("mixin")
                        || identifier.equals("enum")) {
                    awaitingTopLevelTypeName = true;
                    return;
                }
                if (identifier.equals("extension")) {
                    awaitingExtensionTypeKeyword = true;
                    return;
                }
            }
            if (targetHeader) {
                if (identifier.equals("extends") && typeParameterDepth == 0) {
                    readingBase = true;
                    expectingQualifiedBase = true;
                    return;
                }
                if (readingBase) {
                    if (expectingQualifiedBase) {
                        currentBase = identifier;
                        currentBaseStartUtf16 = offset;
                        currentBaseEndUtf16 = Math.addExact(
                                offset, identifier.length());
                        expectingQualifiedBase = false;
                        return;
                    }
                    readingBase = false;
                }
            }
            if (identifier.equals("class")) {
                awaitingClassName = true;
                pendingClassStart = offset;
                pendingClassBraceDepth = currentBraceDepth;
            }
        }

        void symbol(char symbol, int offset, int currentBraceDepth) {
            if (targetHeader && !readingBase) {
                if (symbol == '<') {
                    typeParameterDepth++;
                } else if (symbol == '>' && typeParameterDepth > 0) {
                    typeParameterDepth--;
                }
            }
            if (targetHeader && readingBase) {
                if (symbol == '.' && currentBase != null && !expectingQualifiedBase) {
                    expectingQualifiedBase = true;
                    currentBaseQualified = true;
                } else if (!Character.isWhitespace(symbol)) {
                    readingBase = false;
                }
            }
            if (targetHeader && (symbol == '{' || symbol == ';')) {
                if (matches == 1) {
                    firstBase = currentBase;
                    firstBaseStartUtf16 = currentBaseStartUtf16;
                    firstBaseEndUtf16 = currentBaseEndUtf16;
                    firstBaseQualified = currentBaseQualified;
                }
                if (symbol == '{' && matches == 1) {
                    bodyStart = offset + 1;
                    bodyDepth = currentBraceDepth + 1;
                    insideTargetBody = true;
                }
                targetHeader = false;
                readingBase = false;
                expectingQualifiedBase = false;
            }
            if (awaitingClassName && !Character.isWhitespace(symbol)
                    && symbol != '@') {
                awaitingClassName = false;
            }
            if (insideTargetBody && symbol == '}'
                    && currentBraceDepth == bodyDepth) {
                bodyEnd = offset;
                insideTargetBody = false;
            }
        }

        ClassSummary summary() {
            if (targetHeader && matches == 1) {
                firstBase = currentBase;
                firstBaseStartUtf16 = currentBaseStartUtf16;
                firstBaseEndUtf16 = currentBaseEndUtf16;
                firstBaseQualified = currentBaseQualified;
            }
            return new ClassSummary(
                    matches,
                    firstBase,
                    firstBaseStartUtf16,
                    firstBaseEndUtf16,
                    firstBaseQualified,
                    declarationStart,
                    declarationBraceDepth,
                    bodyStart,
                    bodyEnd,
                    bodyDepth,
                    Set.copyOf(topLevelTypeNames));
        }
    }

    private record DecodedSource(String text, int[] byteOffsets) {
    }

    private record Structure(List<MarkerEvent> markers, ClassSummary classSummary, List<SourceToken> tokens) {
    }

    private record SourceToken(String value, int offset, DelimiterScope scope) { }

    private record SourceClass(String name, int start, int open, int close) { }

    private record MarkerEvent(
            MarkerKind kind,
            String regionId,
            int lineStart,
            int afterLine,
            DelimiterScope scope) {
    }

    private record OpenMarker(
            String id,
            int payloadStart,
            int markerStart,
            DelimiterScope scope) {
    }

    private record FoundRegion(
            String id,
            int payloadStart,
            int payloadEnd,
            int openMarkerStart,
            int closeMarkerStart,
            DelimiterScope openScope,
            DelimiterScope closeScope) {
    }

    private record DelimiterScope(
            int braceDepth,
            int parenthesisDepth,
            int bracketDepth) {

        boolean libraryLevel() {
            return braceDepth == 0 && parenthesisDepth == 0 && bracketDepth == 0;
        }

        boolean directClassMember(int expectedBraceDepth) {
            return braceDepth == expectedBraceDepth
                    && parenthesisDepth == 0
                    && bracketDepth == 0;
        }
    }

    private record ClassSummary(
            int matches,
            String firstBaseClass,
            int firstBaseStartUtf16,
            int firstBaseEndUtf16,
            boolean firstBaseQualified,
            int declarationStart,
            int declarationBraceDepth,
            int bodyStart,
            int bodyEnd,
            int bodyDepth,
            Set<String> topLevelTypeNames) {
    }

    private record SkipResult(
            int nextIndex,
            boolean terminated,
            boolean nestingTooDeep) {
    }

    private enum MarkerKind {
        OPEN,
        CLOSE,
        MALFORMED
    }
}
