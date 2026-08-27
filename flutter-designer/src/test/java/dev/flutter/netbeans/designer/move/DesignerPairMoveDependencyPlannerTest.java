package dev.flutter.netbeans.designer.move;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DesignerPairMoveDependencyPlannerTest {
    private static final String PACKAGE = "sample_app";
    private static final String OLD = "screens/order/order_screen.dart";
    private static final String TARGET = "features/orders/order_screen.dart";

    private final DesignerPairMoveDependencyPlanner planner =
            new DesignerPairMoveDependencyPlanner();

    @Test
    void acceptsOnlyLocationStableUrisAndReturnsDeterministicProofSummary() {
        DesignerPairMoveDependencyResult.Safe safe = assertInstanceOf(
                DesignerPairMoveDependencyResult.Safe.class,
                planner.prepare(
                        PACKAGE,
                        OLD,
                        TARGET,
                        List.of(
                                source("test/widget_test.dart", ""),
                                source("lib/shared/theme.dart", ""),
                                source("lib/" + OLD, """
                                        import 'dart:async';
                                        import 'package:http/http.dart';
                                        import 'package:sample_app/shared/theme.dart';
                                        export r'package:sample_app/shared/theme.dart';
                                        part of sample.order_screen;
                                        """))));

        DesignerPairMoveDependencyPlan plan = safe.plan();
        assertAll(
                () -> assertEquals(PACKAGE, plan.packageName()),
                () -> assertEquals(OLD, plan.originalLibRelativePath()),
                () -> assertEquals(TARGET, plan.targetLibRelativePath()),
                () -> assertEquals(
                        List.of(
                                "lib/screens/order/order_screen.dart",
                                "lib/shared/theme.dart",
                                "test/widget_test.dart"),
                        plan.inspectedProjectRelativePaths()),
                () -> assertEquals(4, plan.inspectedDirectiveUris()),
                () -> assertTrue(plan.inspectedSourceBytes() > 0));
    }

    @ParameterizedTest
    @MethodSource("outgoingRelativeDirectives")
    void rejectsEveryRelativeUriBranchInMovedSource(String directive, String uri) {
        DesignerPairMoveDependencyResult.Rejected rejected = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(source("lib/" + OLD, directive))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.OUTGOING_RELATIVE_DIRECTIVE,
                        rejected.diagnostic().code()),
                () -> assertEquals("lib/" + OLD,
                        rejected.diagnostic().sourceProjectRelativePath()),
                () -> assertEquals(uri, rejected.diagnostic().uri()),
                () -> assertTrue(rejected.diagnostic().message().contains(TARGET)));
    }

    private static Stream<Arguments> outgoingRelativeDirectives() {
        return Stream.of(
                Arguments.of("import '../shared.dart';", "../shared.dart"),
                Arguments.of("export 'model.dart';", "model.dart"),
                Arguments.of("part 'order_screen.g.dart';", "order_screen.g.dart"),
                Arguments.of("part of '../order.dart';", "../order.dart"),
                Arguments.of(
                        "import 'package:http/http.dart' "
                        + "if (dart.library.io) 'native.dart';",
                        "native.dart"));
    }

    @ParameterizedTest
    @MethodSource("incomingReferences")
    void rejectsIncomingRelativeAndSelfPackageReferences(
            String sourcePath,
            String sourceText,
            String expectedUri) {
        DesignerPairMoveDependencyResult.Rejected rejected = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(
                        source("lib/" + OLD, "import 'dart:async';"),
                        source(sourcePath, sourceText))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.INCOMING_REFERENCE,
                        rejected.diagnostic().code()),
                () -> assertEquals(sourcePath,
                        rejected.diagnostic().sourceProjectRelativePath()),
                () -> assertEquals(expectedUri, rejected.diagnostic().uri()),
                () -> assertTrue(rejected.diagnostic().message()
                        .contains("lib/" + OLD)));
    }

    private static Stream<Arguments> incomingReferences() {
        return Stream.of(
                Arguments.of(
                        "test/order_test.dart",
                        "import '../lib/screens/order/order_screen.dart';",
                        "../lib/screens/order/order_screen.dart"),
                Arguments.of(
                        "lib/screens/consumer.dart",
                        "export 'order/order_screen.dart';",
                        "order/order_screen.dart"),
                Arguments.of(
                        "lib/consumer.dart",
                        "import 'package:sample_app/screens/order/order_screen.dart';",
                        "package:sample_app/screens/order/order_screen.dart"),
                Arguments.of(
                        "lib/screens/order/order_part.dart",
                        "part of 'order_screen.dart';",
                        "order_screen.dart"),
                Arguments.of(
                        "lib/conditional.dart",
                        "import 'package:http/http.dart' if (dart.library.io) "
                        + "'package:sample_app/screens/order/order_screen.dart';",
                        "package:sample_app/screens/order/order_screen.dart"));
    }

    @ParameterizedTest
    @MethodSource("targetBindingReferences")
    void rejectsEveryDirectiveBranchThatWouldBindToMoveTarget(
            String sourcePath,
            String sourceText,
            String expectedUri) {
        List<DartMoveSourceSnapshot> sources = sourcePath.equals("lib/" + OLD)
                ? List.of(source(sourcePath, sourceText))
                : List.of(
                        source("lib/" + OLD, "import 'dart:async';"),
                        source(sourcePath, sourceText));

        DesignerPairMoveDependencyResult.Rejected rejected = rejected(planner.prepare(
                PACKAGE, OLD, TARGET, sources));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                        rejected.diagnostic().code()),
                () -> assertEquals(sourcePath,
                        rejected.diagnostic().sourceProjectRelativePath()),
                () -> assertEquals(expectedUri, rejected.diagnostic().uri()),
                () -> assertTrue(rejected.diagnostic().message()
                        .contains("lib/" + TARGET)));
    }

    private static Stream<Arguments> targetBindingReferences() {
        String packageTarget = "package:sample_app/" + TARGET;
        return Stream.of(
                Arguments.of(
                        "lib/features/consumer.dart",
                        "import 'orders/order_screen.dart';",
                        "orders/order_screen.dart"),
                Arguments.of(
                        "test/order_test.dart",
                        "import '" + packageTarget + "';",
                        packageTarget),
                Arguments.of(
                        "lib/" + OLD,
                        "import '" + packageTarget + "';",
                        packageTarget),
                Arguments.of(
                        "lib/" + OLD,
                        "import '../../features/orders/order_screen.dart';",
                        "../../features/orders/order_screen.dart"),
                Arguments.of(
                        "lib/conditional.dart",
                        "import 'package:http/http.dart' if (dart.library.io) '"
                        + packageTarget + "';",
                        packageTarget),
                Arguments.of(
                        "lib/features/api.dart",
                        "export 'orders/order_screen.dart';",
                        "orders/order_screen.dart"),
                Arguments.of(
                        "lib/features/library.dart",
                        "part 'orders/order_screen.dart';",
                        "orders/order_screen.dart"),
                Arguments.of(
                        "lib/features/library_part.dart",
                        "part of 'orders/order_screen.dart';",
                        "orders/order_screen.dart"));
    }

    @Test
    void rejectsExactAndCaseAliasedTargetOccupancyBeforeScanning() {
        String targetProjectPath = "lib/" + TARGET;
        DesignerPairMoveDependencyResult.Rejected exact = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(
                        source("lib/" + OLD, "import 'dart:async';"),
                        source(targetProjectPath, "/* deliberately malformed"))));
        String caseAlias = "lib/FEATURES/orders/ORDER_SCREEN.dart";
        DesignerPairMoveDependencyResult.Rejected aliased = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(
                        source("lib/" + OLD, "import 'dart:async';"),
                        source(caseAlias, "class Occupied {}"))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                        exact.diagnostic().code()),
                () -> assertEquals(targetProjectPath,
                        exact.diagnostic().sourceProjectRelativePath()),
                () -> assertEquals(-1, exact.diagnostic().utf16Offset()),
                () -> assertEquals("", exact.diagnostic().uri()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                        aliased.diagnostic().code()),
                () -> assertEquals(caseAlias,
                        aliased.diagnostic().sourceProjectRelativePath()));
    }

    @Test
    void targetBindingDiagnosticIsIndependentOfInventoryIterationOrder() {
        DartMoveSourceSnapshot moved = source(
                "lib/" + OLD, "import 'dart:async';");
        DartMoveSourceSnapshot first = source(
                "lib/a_consumer.dart",
                "import 'package:sample_app/" + TARGET + "';");
        DartMoveSourceSnapshot last = source(
                "lib/features/z_consumer.dart",
                "export 'orders/order_screen.dart';");

        DesignerPairMoveDependencyResult.Rejected forward = rejected(
                planner.prepare(
                        PACKAGE, OLD, TARGET, List.of(moved, first, last)));
        DesignerPairMoveDependencyResult.Rejected reverse = rejected(
                planner.prepare(
                        PACKAGE, OLD, TARGET, List.of(last, first, moved)));

        assertAll(
                () -> assertEquals(forward.diagnostic(), reverse.diagnostic()),
                () -> assertEquals("lib/a_consumer.dart",
                        forward.diagnostic().sourceProjectRelativePath()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                        forward.diagnostic().code()));
    }

    @Test
    void rejectsCaseAndUnicodeAliasesOfOriginalAndTarget() {
        DesignerPairMoveDependencyResult.Rejected originalCase = rejected(
                planner.prepare(
                        PACKAGE,
                        OLD,
                        TARGET,
                        List.of(
                                source("lib/" + OLD, ""),
                                source("lib/consumer.dart",
                                        "import 'Screens/Order/Order_Screen.dart';"))));
        DesignerPairMoveDependencyResult.Rejected targetCase = rejected(
                planner.prepare(
                        PACKAGE,
                        OLD,
                        TARGET,
                        List.of(
                                source("lib/" + OLD, ""),
                                source("lib/consumer.dart",
                                        "import 'package:sample_app/FEATURES/orders/"
                                        + "ORDER_SCREEN.dart';"))));

        String composed = "caf\u00e9";
        String decomposed = "cafe\u0301";
        String unicodeOld = "screens/" + composed + "/order_screen.dart";
        String unicodeTarget = "features/" + composed + "/order_screen.dart";
        DesignerPairMoveDependencyResult.Rejected originalUnicode = rejected(
                planner.prepare(
                        PACKAGE,
                        unicodeOld,
                        unicodeTarget,
                        List.of(
                                source("lib/" + unicodeOld, ""),
                                source("lib/consumer.dart",
                                        "import 'screens/" + decomposed
                                        + "/order_screen.dart';"))));
        DesignerPairMoveDependencyResult.Rejected targetUnicode = rejected(
                planner.prepare(
                        PACKAGE,
                        unicodeOld,
                        unicodeTarget,
                        List.of(
                                source("lib/" + unicodeOld, ""),
                                source("lib/consumer.dart",
                                        "import 'package:sample_app/features/"
                                        + decomposed + "/order_screen.dart';"))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.INCOMING_REFERENCE,
                        originalCase.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                        targetCase.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.INCOMING_REFERENCE,
                        originalUnicode.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TARGET_BINDING_CHANGE,
                        targetUnicode.diagnostic().code()));
    }

    @Test
    void doesNotConfuseExternalPackageWithMovedProjectFile() {
        DesignerPairMoveDependencyResult.Safe safe = assertInstanceOf(
                DesignerPairMoveDependencyResult.Safe.class,
                planner.prepare(
                        PACKAGE,
                        OLD,
                        TARGET,
                        List.of(
                                source("lib/" + OLD, "import 'dart:async';"),
                                source("lib/consumer.dart", """
                                        import 'package:other/screens/order/order_screen.dart';
                                        export 'package:sample_app/features/another.dart';
                                        """))));

        assertEquals(3, safe.plan().inspectedDirectiveUris());
    }

    @Test
    void rejectsUnsupportedSchemesAndAmbiguousUriPaths() {
        DesignerPairMoveDependencyResult.Rejected fileUri = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(
                        source("lib/" + OLD, "import 'dart:async';"),
                        source("lib/consumer.dart", "import 'file:///tmp/x.dart';"))));
        DesignerPairMoveDependencyResult.Rejected encoded = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(
                        source("lib/" + OLD, "import 'dart:async';"),
                        source("lib/consumer.dart", "import 'screens/%6frder.dart';"))));
        DesignerPairMoveDependencyResult.Rejected opaqueQuery = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(
                        source("lib/" + OLD, "import 'dart:async';"),
                        source("lib/consumer.dart", "import 'dart:async?mode=test';"))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI,
                        fileUri.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI,
                        encoded.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI,
                        opaqueQuery.diagnostic().code()));
    }

    @Test
    void validatesExactInventoryAndMovePathsBeforeScanning() {
        DesignerPairMoveDependencyResult.Rejected packageName = rejected(planner.prepare(
                "Sample-App", OLD, TARGET, List.of()));
        DesignerPairMoveDependencyResult.Rejected oldPath = rejected(planner.prepare(
                PACKAGE, "lib/" + OLD, TARGET, List.of()));
        DesignerPairMoveDependencyResult.Rejected same = rejected(planner.prepare(
                PACKAGE, OLD, OLD, List.of()));
        DesignerPairMoveDependencyResult.Rejected caseAlias = rejected(planner.prepare(
                PACKAGE, OLD, "Screens/order/order_screen.dart", List.of()));
        DesignerPairMoveDependencyResult.Rejected unicodeAlias = rejected(planner.prepare(
                PACKAGE,
                "screens/caf\u00e9/order_screen.dart",
                "screens/cafe\u0301/order_screen.dart",
                List.of()));
        DesignerPairMoveDependencyResult.Rejected renamed = rejected(planner.prepare(
                PACKAGE, OLD, "features/orders/renamed.dart", List.of()));
        DesignerPairMoveDependencyResult.Rejected missing = rejected(planner.prepare(
                PACKAGE, OLD, TARGET, List.of(source("lib/other.dart", ""))));
        DesignerPairMoveDependencyResult.Rejected duplicate = rejected(planner.prepare(
                PACKAGE,
                OLD,
                TARGET,
                List.of(
                        source("lib/" + OLD, ""),
                        source("lib/" + OLD, ""))));
        DesignerPairMoveDependencyResult.Rejected aliasedDuplicate = rejected(
                planner.prepare(
                        PACKAGE,
                        OLD,
                        TARGET,
                        List.of(
                                source("lib/" + OLD, ""),
                                source("lib/Screens/Order/order_screen.dart", ""))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.INVALID_PACKAGE_NAME,
                        packageName.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.INVALID_ORIGINAL_PATH,
                        oldPath.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.SAME_PATH,
                        same.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.SAME_PATH,
                        caseAlias.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.SAME_PATH,
                        unicodeAlias.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TARGET_FILENAME_CHANGED,
                        renamed.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.MOVED_SOURCE_MISSING,
                        missing.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.DUPLICATE_SOURCE_PATH,
                        duplicate.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.DUPLICATE_SOURCE_PATH,
                        aliasedDuplicate.diagnostic().code()));
    }

    @Test
    void enforcesProjectWideFileAndByteBounds() {
        DartMoveDependencyLimits bounded = new DartMoveDependencyLimits(
                1, 32, 16, 8, 32, 8);
        DesignerPairMoveDependencyPlanner boundedPlanner =
                new DesignerPairMoveDependencyPlanner(bounded);

        DesignerPairMoveDependencyResult.Rejected tooMany = rejected(
                boundedPlanner.prepare(
                        PACKAGE,
                        OLD,
                        TARGET,
                        List.of(
                                source("lib/" + OLD, ""),
                                source("lib/other.dart", ""))));
        DesignerPairMoveDependencyResult.Rejected totalBytes = rejected(
                boundedPlanner.prepare(
                        PACKAGE,
                        OLD,
                        TARGET,
                        List.of(source("lib/" + OLD, "x".repeat(17)))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TOO_MANY_SOURCES,
                        tooMany.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.PROJECT_TOO_LARGE,
                        totalBytes.diagnostic().code()));
    }

    private static DesignerPairMoveDependencyResult.Rejected rejected(
            DesignerPairMoveDependencyResult result) {
        return assertInstanceOf(
                DesignerPairMoveDependencyResult.Rejected.class, result);
    }

    private static DartMoveSourceSnapshot source(String path, String text) {
        return new DartMoveSourceSnapshot(
                path, text.getBytes(StandardCharsets.UTF_8));
    }
}
