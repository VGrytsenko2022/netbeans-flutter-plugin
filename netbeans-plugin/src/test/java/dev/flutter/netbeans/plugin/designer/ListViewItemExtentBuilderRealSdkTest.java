package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.*;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real nullable Rendering typedef proof and generated static-list layout on the pinned SDK. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class ListViewItemExtentBuilderRealSdkTest {
    @TempDir Path project;
    private static final StableId LIST_ID = StableId.random();
    private static final PropertyName BUILDER = new PropertyName("itemExtentBuilder");
    private static final String BUILDER_TYPE = "ItemExtentBuilder?";
    private Path sdk, flutter, dart, lib;

    @Test
    void exactNullableRenderingProofsPreserveUserSourceAndRejectDynamicResults() throws Exception {
        initialize(); writeBuilders();
        var baseline = open(list(Map.of()), "probe.dart", "");
        for (String name : List.of("extent", "constantExtent", "broadExtent", "configured", "absent")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(LIST_ID, BUILDER, imported(name))), "probe.dart", true);
        }
        for (String name : List.of("makeExtent", "makeAbsent")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(LIST_ID, BUILDER,
                    imported(name, Optional.empty(), true))), "probe.dart", true);
        }
        for (String root : List.of("Builders", "holder")) {
            assertAnalysis(baseline, apply(baseline, new SetProperty(LIST_ID, BUILDER,
                    imported(root, Optional.of("extent"), false))), "probe.dart", true);
        }
        var adversarial = reopen(baseline, "// ignore_for_file: argument_type_not_assignable, invalid_assignment\n" + source(baseline));
        for (String name : List.of("dynamicExtent", "integerExtent", "numberExtent", "wrongIndex",
                "wrongDimensions", "missingDimension", "extraRequired", "asyncExtent", "dynamicGetter")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(LIST_ID, BUILDER, imported(name))), "probe.dart", false);
        }
        assertAnalysis(adversarial, apply(adversarial, new SetProperty(LIST_ID, BUILDER,
                imported("dynamicFactory", Optional.empty(), true))), "probe.dart", false);
        var localBaseline = open(list(Map.of()), "local.dart", """
                  double? _localExtent(int index, SliverLayoutDimensions dimensions) {
                    // User-owned Привіт.
                    return index < 8 ? 40.0 : null;
                  }
                """);
        localBaseline = reopen(localBaseline, "import 'package:flutter/rendering.dart';\n" + source(localBaseline));
        var local = apply(localBaseline, new SetProperty(LIST_ID, BUILDER,
                new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_localExtent", Optional.empty(),
                        PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty())));
        assertAnalysis(localBaseline, local, "local.dart", true);
        var reset = apply(reopen(local, source(local)), new ResetProperty(LIST_ID, BUILDER));
        assertFalse(source(reset).contains("itemExtentBuilder:")); assertTrue(source(reset).contains("// User-owned Привіт."));
        assertArrayEquals(local.current().dartCandidateBytes(), reset.undo().session().current().dartCandidateBytes());

        // A single candidate exercises Material, Rendering and Services proof imports together.
        var field = new WidgetNode(StableId.random(), TextFieldWidgetPropertySchema.TEXT_FIELD_TYPE,
                Map.of(p("buildCounter"), imported("counter")), Map.of());
        var pointer = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Listener"),
                Map.of(p("onPointerDown"), imported("pointerDown")),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(box(0))));
        var mixed = open(list(Map.of(), List.of(field, pointer)), "mixed.dart", "");
        assertAnalysis(mixed, apply(mixed, new SetProperty(LIST_ID, BUILDER, imported("extent"))), "mixed.dart", true);
    }

    @Test
    void generatedListsKeepActualExtentsAcrossAxisScrollResizeAndNullableFallbacks() throws Exception {
        initialize(); writeBuilders();
        save("vertical.dart", Map.of(BUILDER, imported("extent")));
        save("horizontal.dart", Map.of(BUILDER, imported("extent"), p("scrollDirection"), new PropertyValue.EnumValue("Axis", "horizontal")));
        save("reverse.dart", Map.of(BUILDER, imported("extent"), p("reverse"), new PropertyValue.BooleanValue(true)));
        save("shrink.dart", Map.of(BUILDER, imported("extent"), p("shrinkWrap"), new PropertyValue.BooleanValue(true)));
        save("natural.dart", Map.of());
        save("explicit_null.dart", Map.of(BUILDER, new PropertyValue.NullValue()));
        save("nullable.dart", Map.of(BUILDER, imported("makeAbsent", Optional.empty(), true)));
        save("fixed.dart", Map.of(BUILDER, new PropertyValue.NullValue(), p("itemExtent"), number(45)));
        Files.write(lib.resolve("empty.dart"), open(list(Map.of(BUILDER, imported("emptyExtent")), List.of()),
                "empty.dart", "").current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("extent_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:list_view_extent_contract/builders.dart' as builders;
                import 'package:list_view_extent_contract/vertical.dart' as vertical;
                import 'package:list_view_extent_contract/horizontal.dart' as horizontal;
                import 'package:list_view_extent_contract/reverse.dart' as reversed;
                import 'package:list_view_extent_contract/shrink.dart' as shrink;
                import 'package:list_view_extent_contract/natural.dart' as natural;
                import 'package:list_view_extent_contract/explicit_null.dart' as explicitNull;
                import 'package:list_view_extent_contract/nullable.dart' as nullable;
                import 'package:list_view_extent_contract/fixed.dart' as fixed;
                import 'package:list_view_extent_contract/empty.dart' as empty;
                Future<void> mount(WidgetTester tester, Widget sample, {double width = 300, double height = 120, bool loose = false}) async {
                  await tester.pumpWidget(MaterialApp(home: Scaffold(body: Align(alignment: Alignment.topLeft,
                    child: loose ? SizedBox(width: width, child: sample) : SizedBox(width: width, height: height, child: sample)))));
                  await tester.pumpAndSettle();
                }
                Finder item(int index) => find.text('Item $index');
                Finder box(int index) => find.ancestor(of: item(index), matching: find.byType(SizedBox)).first;
                void main() {
                  testWidgets('variable extents and child order are real generated layout', (tester) async {
                    await mount(tester, const vertical.Sample());
                    expect(find.byType(SliverVariedExtentList), findsOneWidget);
                    expect(tester.getSize(box(0)).height, 30);
                    expect(tester.getSize(box(1)).height, 35);
                    expect(tester.getTopLeft(box(1)).dy - tester.getTopLeft(box(0)).dy, 30);
                    expect(builders.dimensions!.viewportMainAxisExtent, 120);
                    expect(builders.dimensions!.crossAxisExtent, 300);
                    expect(builders.dimensions!.precedingScrollExtent, 0);
                    expect(tester.widget<ListView>(find.byType(ListView)).semanticChildCount, 8);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('horizontal and reverse honor main-axis extents', (tester) async {
                    await mount(tester, const horizontal.Sample(), width: 120, height: 210);
                    expect(tester.getSize(box(0)).width, 30);
                    expect(tester.getSize(box(1)).width, 35);
                    expect(tester.getTopLeft(box(1)).dx - tester.getTopLeft(box(0)).dx, 30);
                    expect(builders.dimensions!.viewportMainAxisExtent, 120);
                    expect(builders.dimensions!.crossAxisExtent, 210);
                    await mount(tester, const reversed.Sample());
                    expect(tester.getSize(box(0)).height, 30);
                    expect(tester.getTopLeft(box(0)).dy, greaterThan(tester.getTopLeft(box(1)).dy));
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('scroll offset and resized viewport reach real layout dimensions', (tester) async {
                    await mount(tester, const vertical.Sample());
                    final position = tester.state<ScrollableState>(find.byType(Scrollable)).position;
                    position.jumpTo(85); await tester.pumpAndSettle();
                    expect(position.pixels, 85);
                    expect(builders.dimensions!.scrollOffset, 85);
                    await mount(tester, const vertical.Sample(), width: 250, height: 160);
                    expect(builders.dimensions!.viewportMainAxisExtent, 160);
                    expect(builders.dimensions!.crossAxisExtent, 250);
                    expect(position.pixels, 85);
                    position.jumpTo(position.maxScrollExtent); await tester.pumpAndSettle();
                    expect(item(7), findsOneWidget); expect(tester.takeException(), isNull);
                  });
                  testWidgets('shrinkWrap sums extents without losing children', (tester) async {
                    await mount(tester, const shrink.Sample(), loose: true);
                    expect(tester.getSize(find.byType(ListView)).height, 380);
                    for (var i = 0; i < 8; i++) {
                      expect(item(i), findsOneWidget);
                      expect(tester.getSize(box(i)).height, 30 + 5 * i);
                    }
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('omission null and null-returning factory keep natural child sizes', (tester) async {
                    for (final sample in [const natural.Sample(), const explicitNull.Sample(), const nullable.Sample()]) {
                      await mount(tester, sample);
                      expect(tester.widget<ListView>(find.byType(ListView)).itemExtentBuilder, isNull);
                      expect(find.byType(SliverList), findsOneWidget);
                      expect(tester.getSize(box(0)).height, 24);
                      expect(tester.getSize(box(1)).height, 24);
                    }
                    await mount(tester, const fixed.Sample());
                    expect(find.byType(SliverFixedExtentList), findsOneWidget);
                    expect(tester.getSize(box(0)).height, 45);
                    expect(tester.takeException(), isNull);
                  });
                  testWidgets('null is out-of-range only; empty child list stays empty', (tester) async {
                    await mount(tester, const vertical.Sample());
                    expect(builders.extent(8, builders.dimensions!), isNull);
                    for (var i = 0; i < 8; i++) {
                      expect(builders.extent(i, builders.dimensions!), 30 + 5 * i);
                    }
                    await mount(tester, const empty.Sample());
                    // An empty sliver has no visible geometry and is skipped by onstage finders.
                    expect(find.byType(SliverVariedExtentList, skipOffstage: false), findsOneWidget);
                    expect(tester.widget<ListView>(find.byType(ListView)).childrenDelegate.estimatedChildCount, 0);
                    expect(find.textContaining('Item '), findsNothing);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private void writeBuilders() throws Exception {
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                SliverLayoutDimensions? dimensions;
                double? extent(int index, SliverLayoutDimensions value) {
                  dimensions = value;
                  return index < 8 ? 30.0 + index * 5 : null;
                }
                double constantExtent(int index, SliverLayoutDimensions value) => 40.0;
                double? broadExtent(num index, Object value) => 40.0;
                double? emptyExtent(int index, SliverLayoutDimensions value) => null;
                ItemExtentBuilder? get configured => extent;
                ItemExtentBuilder? get absent => null;
                ItemExtentBuilder? makeExtent() => extent;
                ItemExtentBuilder? makeAbsent() => null;
                class Builders { static double? extent(int i, SliverLayoutDimensions d) => 40.0; }
                class Holder { double? extent(int i, SliverLayoutDimensions d) => 40.0; }
                final holder = Holder();
                dynamic dynamicExtent(int i, SliverLayoutDimensions d) => 40.0;
                int? integerExtent(int i, SliverLayoutDimensions d) => 40;
                num? numberExtent(int i, SliverLayoutDimensions d) => 40.0;
                double? wrongIndex(String i, SliverLayoutDimensions d) => 40.0;
                double? wrongDimensions(int i, String d) => 40.0;
                double? missingDimension(int i) => 40.0;
                double? extraRequired(int i, SliverLayoutDimensions d, bool extra) => 40.0;
                Future<double?> asyncExtent(int i, SliverLayoutDimensions d) async => 40.0;
                dynamic get dynamicGetter => extent;
                dynamic dynamicFactory() => extent;
                Widget? counter(BuildContext c, {required int currentLength, required int? maxLength, required bool isFocused}) => null;
                void pointerDown(PointerDownEvent event) {}
                """);
    }
    private void save(String file, Map<PropertyName, PropertyValue> properties) throws Exception {
        Files.write(lib.resolve(file), open(list(properties), file, "").current().dartCandidateBytes());
    }
    private static WidgetNode list(Map<PropertyName, PropertyValue> properties) {
        return list(properties, java.util.stream.IntStream.range(0, 8).mapToObj(ListViewItemExtentBuilderRealSdkTest::box).toList());
    }
    private static WidgetNode list(Map<PropertyName, PropertyValue> properties, List<WidgetNode> children) {
        var values = new java.util.LinkedHashMap<>(properties);
        values.put(p("primary"), new PropertyValue.BooleanValue(false));
        return new WidgetNode(LIST_ID, ListViewWidgetPropertySchema.LIST_VIEW_TYPE, values,
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(children)));
    }
    private static WidgetNode box(int index) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(p("data"), new PropertyValue.StringValue("Item " + index)), Map.of());
        return new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(p("height"), number(24), p("width"), number(24)),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
    }
    private static PropertyName p(String name) { return new PropertyName(name); }
    private static PropertyValue number(int value) { return new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(value)); }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: list_view_extent_contract
                publish_to: none
                environment:
                  sdk: '>=3.11.0 <4.0.0'
                dependencies:
                  flutter:
                    sdk: flutter
                dev_dependencies:
                  flutter_test:
                    sdk: flutter
                """);
        lib = Files.createDirectories(project.resolve("lib"));
        run(List.of(flutter.toString(), "pub", "get", "--offline"), "pub-get.log");
    }
    private void assertAnalysis(DesignerCommandSession baseline, DesignerCommandSession candidate, String file, boolean pass) throws Exception {
        Path source = lib.resolve(file);
        Files.write(source, baseline.current().dartCandidateBytes());
        var pair = candidate.current().preparedPair().orElseThrow();
        var current = new FlutterDesignerDocumentState.Current((FdDecodeResult.Current) new FdDocumentCodec().decode(baseline.current().fdSnapshot()),
                new ValidationResult(List.of()), BuiltInWidgetCatalog.getDefault(), List.of(), List.of(),
                Optional.of(pair.dartTransition().baseline().source()), Optional.of(pair.dartTransition().baseline()));
        var ticket = PairSaveEvidenceGate.prepareAnalysis(current, pair, project, source, DartCandidateWarningPolicy.ALLOW, sdk);
        assertTrue(ticket.request().symbolProbes().stream().anyMatch(probe -> probe.staticTypeProbe()
                .filter(type -> type.expectedDartType().equals(BUILDER_TYPE)).isPresent()), ticket.request().toString());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var result = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, result);
            assertEquals(pass, accepted.ready(), () -> result + "\n" + accepted.diagnostics());
            if (!pass) assertTrue(result.symbolEvidence().stream().anyMatch(evidence -> evidence.staticTypeEvidence()
                    .filter(proof -> !proof.accepted()).isPresent()), result.toString());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline.current().dartCandidateBytes(), Files.readAllBytes(source));
    }
    private static DesignerCommandSession open(WidgetNode root, String file, String members) throws Exception {
        return StateBindingRealSdkTest.openRoot(root, file, members);
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session, String source) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static PropertyValue imported(String name) { return imported(name, Optional.empty(), false); }
    private static PropertyValue imported(String name, Optional<String> member, boolean factory) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:list_view_extent_contract/builders.dart"), name, member,
                factory ? PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION
                        : PropertyValue.DartObjectReferenceValue.Access.REFERENCE, factory ? Optional.of(false) : Optional.empty());
    }
    private static DesignerCommandSession apply(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertTrue(result.changed(), result.diagnostics().toString());
        return result.session();
    }
    private static String source(DesignerCommandSession session) {
        return new String(session.current().dartCandidateBytes(), StandardCharsets.UTF_8);
    }
    private void run(List<String> command, String logName) throws Exception {
        Path log = project.resolve(logName);
        Process process = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = process.waitFor(180, TimeUnit.SECONDS);
        if (!finished) { process.destroyForcibly(); process.waitFor(10, TimeUnit.SECONDS); }
        String output = Files.readString(log);
        assertTrue(finished, command + " timed out\n" + output);
        assertEquals(0, process.exitValue(), command + "\n" + output);
    }
}



