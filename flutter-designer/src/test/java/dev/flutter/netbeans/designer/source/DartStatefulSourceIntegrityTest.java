package dev.flutter.netbeans.designer.source;

import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DartStatefulSourceIntegrityTest {
    private static final String IMPORTS = "import 'package:flutter/widgets.dart';\n";
    private static final String BUILD = "  @override\n  Widget build(BuildContext context) {\n"
            + "    return const SizedBox();\n  }\n";
    private static final DartSourceDescriptor DESCRIPTOR = new DartSourceDescriptor("home_page.dart", "HomePage",
            WidgetClassKind.STATEFUL, Optional.empty(), new ManagedRegions(
                    new ManagedRegion(DartManagedRegionHashing.normalizedSha256(IMPORTS)),
                    new ManagedRegion(DartManagedRegionHashing.normalizedSha256(BUILD))));

    @Test
    void canonicalArrowPublishesActualStateOwnerAndBothExactSuperclassOccurrences() {
        String source = source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState");
        var result = scan(source);
        assertTrue(result.onDiskDeclaredMatch(), result.diagnostics().toString());
        assertEquals(Optional.of("CustomHomeState"), result.verifiedMemberClassName());
        var widget = result.superclassOccurrence().orElseThrow();
        var state = result.stateSuperclassOccurrence().orElseThrow();
        assertEquals("HomePage", widget.className());
        assertEquals("StatefulWidget", widget.symbolName());
        assertEquals("CustomHomeState", state.className());
        assertEquals("State", state.symbolName());
        assertExactOccurrence(source, widget);
        assertExactOccurrence(source, state);
        assertTrue(widget.belongsTo(result.original().orElseThrow()));
        assertTrue(state.belongsTo(result.original().orElseThrow()));
    }

    @Test
    void supportsConcreteReturnAndSynchronousReturnBlockWithComments() {
        for (String factory : List.of(
                "CustomHomeState createState() => CustomHomeState();",
                "State<HomePage> createState() { return CustomHomeState(); }",
                "State /* type */ <HomePage> createState() { /* body */ return CustomHomeState(); }")) {
            var result = scan(source(factory, "CustomHomeState"));
            assertTrue(result.onDiskDeclaredMatch(), factory + "\n" + result.diagnostics());
            assertEquals(Optional.of("CustomHomeState"), result.verifiedMemberClassName());
        }
    }

    @Test
    void unicodeBomAndCrLfCoordinatesRemainSourceExactForReadOnlyScan() {
        String source = "\uFEFF// Привіт 😀\n" + source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState");
        source = source.replace("\n", "\r\n");
        var result = scan(source);
        assertTrue(result.onDiskDeclaredMatch(), result.diagnostics().toString());
        assertExactOccurrence(source, result.superclassOccurrence().orElseThrow());
        assertExactOccurrence(source, result.stateSuperclassOccurrence().orElseThrow());
    }

    @Test
    void rejectsAmbiguousAsyncGenericAndIndirectFactories() {
        for (String factory : List.of(
                "State<HomePage> createState() async => CustomHomeState();",
                "Future<State<HomePage>> createState() async => CustomHomeState();",
                "State<OtherPage> createState() => CustomHomeState();",
                "State<HomePage>? createState() => CustomHomeState();",
                "State<HomePage> createState(int ignored) => CustomHomeState();",
                "State<HomePage> createState<T>() => CustomHomeState();",
                "State<HomePage> createState() => otherFactory();",
                "State<HomePage> createState() => CustomHomeState.named();",
                "State<HomePage> createState() => CustomHomeState(7);",
                "State<HomePage> createState() { if (true) return CustomHomeState(); return CustomHomeState(); }",
                "static State<HomePage> createState() => CustomHomeState();")) {
            assertUnsupported(scan(source(factory, "CustomHomeState")), factory);
        }
    }

    @Test
    void rejectsWrongStateArgumentAndMultipleOrNestedStateOwners() {
        String valid = source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState");
        for (String invalid : List.of(
                valid.replace("extends State<HomePage>", "extends State<OtherPage>"),
                valid.replace("extends State<HomePage>", "extends State<HomePage<int>>"),
                valid.replace("class HomePage extends", "class HomePage<T> extends"),
                valid.replace("class CustomHomeState extends", "class CustomHomeState<T> extends"),
                valid + "\nclass OtherState extends State<HomePage> {}\n",
                valid + "\nclass CustomHomeState extends State<HomePage> {}\n",
                valid.replace("class CustomHomeState", "void enclosing() { class CustomHomeState") + "}\n",
                valid.replace("createState() => CustomHomeState();", "createState() => CustomHomeState();\n"
                        + "  State<HomePage> createState() => CustomHomeState();"))) {
            assertUnsupported(scan(invalid), invalid);
        }
    }

    @Test
    void rejectsShadowedOrQualifiedFrameworkBasesAndConstructorTarget() {
        String valid = source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState");
        for (String invalid : List.of(
                "class State<T> {}\n" + valid,
                "typedef State<T> = Object;\n" + valid,
                "mixin State<T> {}\n" + valid,
                "class StatefulWidget {}\n" + valid,
                valid.replace("extends StatefulWidget", "extends widgets.StatefulWidget"),
                valid.replace("extends State<HomePage>", "extends widgets.State<HomePage>"),
                valid.replace("  const HomePage({super.key});", "  const HomePage({super.key});\n"
                        + "  State<HomePage> CustomHomeState() => throw UnimplementedError();"))) {
            assertUnsupported(scan(invalid), invalid);
        }
    }

    @Test
    void neverAcceptsBuildGuardInOuterOrNestedScopeOrPublishesPartialOwnerEvidence() {
        String valid = source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState");
        String buildRegion = "  // <netbeans-flutter-designer region=\"build\">\n" + BUILD
                + "  // </netbeans-flutter-designer>\n";
        for (String invalid : List.of(
                valid.replace(buildRegion, "").replace("  const HomePage({super.key});", "  const HomePage({super.key});\n" + buildRegion),
                valid.replace(buildRegion, "  void nested() {\n" + buildRegion + "  }\n"),
                valid.replace("return const SizedBox();", "return const Text('changed');"))) {
            var result = scan(invalid);
            assertFalse(result.onDiskDeclaredMatch(), invalid);
            assertTrue(result.verifiedMemberClassName().isEmpty());
            assertTrue(result.superclassOccurrence().isEmpty());
            assertTrue(result.stateSuperclassOccurrence().isEmpty());
        }
    }

    @Test
    void commentsStringsAndUnrelatedHelpersCannotSpoofOwnership() {
        String valid = source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState");
        var withDecoys = scan("// class State<T> {}\n" + valid.replace("  int count = 0;",
                "  int count = 0;\n  final text = 'class State<T> {} createState() => Other();';\n"
                + "  void helper() { final createState = () => 7; }"));
        assertTrue(withDecoys.onDiskDeclaredMatch(), withDecoys.diagnostics().toString());
        assertEquals(Optional.of("CustomHomeState"), withDecoys.verifiedMemberClassName());
        assertUnsupported(scan(valid.replace("  @override\n  State<HomePage> createState() => CustomHomeState();",
                "  final text = 'State<HomePage> createState() => CustomHomeState();';")), "string-only factory");
    }

    @Test
    void compatibilityResultsCannotInventVerifiedOwnersOrTransferStateEvidence() {
        var valid = scan(source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState"));
        var oldConstructor = new DartSourceIntegrityResult(valid.original(), valid.regions(), valid.diagnostics(),
                valid.superclassOccurrence());
        assertTrue(oldConstructor.stateSuperclassOccurrence().isEmpty());
        assertTrue(oldConstructor.verifiedMemberClassName().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new DartSourceIntegrityResult(valid.original(),
                valid.regions(), valid.diagnostics(), valid.superclassOccurrence(), valid.stateSuperclassOccurrence(),
                Optional.of("InventedState")));
        var another = scan(source("State<HomePage> createState() => CustomHomeState();", "CustomHomeState"));
        assertThrows(IllegalArgumentException.class, () -> new DartSourceIntegrityResult(another.original(),
                another.regions(), another.diagnostics(), another.superclassOccurrence(), valid.stateSuperclassOccurrence(),
                Optional.of("CustomHomeState")));
    }

    private static DartSourceIntegrityResult scan(String source) {
        return new DartSourceIntegrityScanner().scan(source.getBytes(StandardCharsets.UTF_8), DESCRIPTOR);
    }

    private static void assertUnsupported(DartSourceIntegrityResult result, String reason) {
        assertEquals(DartSourceIntegrityStatus.UNSUPPORTED, result.status(), reason + "\n" + result.diagnostics());
        assertTrue(result.verifiedMemberClassName().isEmpty(), reason);
        assertTrue(result.superclassOccurrence().isEmpty(), reason);
        assertTrue(result.stateSuperclassOccurrence().isEmpty(), reason);
    }

    private static void assertExactOccurrence(String source, DartDesignerSuperclassOccurrence occurrence) {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        String decoded = source.startsWith("\uFEFF") ? source.substring(1) : source;
        assertEquals(occurrence.symbolName(), decoded.substring(occurrence.startUtf16(), occurrence.endUtf16()));
        assertEquals(occurrence.symbolName(), new String(bytes, occurrence.startByte(), occurrence.lengthBytes(), StandardCharsets.UTF_8));
    }

    private static String source(String factory, String stateName) {
        return "// <netbeans-flutter-designer region=\"imports\">\n" + IMPORTS
                + "// </netbeans-flutter-designer>\n\nclass HomePage extends StatefulWidget {\n"
                + "  const HomePage({super.key});\n  @override\n  " + factory + "\n}\n\n"
                + "class " + stateName + " extends State<HomePage> {\n  int count = 0;\n"
                + "  void changed() { setState(() { count++; }); }\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + BUILD
                + "  // </netbeans-flutter-designer>\n}\n";
    }
}
