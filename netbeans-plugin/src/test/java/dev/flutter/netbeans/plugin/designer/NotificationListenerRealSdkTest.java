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

/** Generated forms, exact analyzer bounds, and real SDK bubbling; no handwritten replacement. */
@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class NotificationListenerRealSdkTest {
    @TempDir Path project;
    private static final WidgetTypeId TYPE = new WidgetTypeId("flutter.widgets.NotificationListener");
    private static final PropertyName FILTER = new PropertyName("notificationType");
    private static final PropertyName EVENT = new PropertyName("onNotification");
    private static final List<String> TYPES = List.of("Notification", "LayoutChangedNotification", "ScrollNotification",
            "ScrollStartNotification", "ScrollUpdateNotification", "OverscrollNotification", "ScrollEndNotification",
            "UserScrollNotification", "SizeChangedLayoutNotification", "ScrollMetricsNotification",
            "OverscrollIndicatorNotification", "DraggableScrollableNotification", "KeepAliveNotification", "NavigationNotification");
    private Path sdk, flutter, dart, lib;

    @Test
    void allSdkFiltersGenerateExactGenericTypesAndRealEventsPreservePropagationAndBodies() throws Exception {
        initialize();
        var nodes = TYPES.stream().map(type -> listener(new PropertyValue.StringValue(type))).toList();
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), Map.of(),
                Map.of(new SlotName("children"), new WidgetSlot.ListSlot(nodes)));
        var baseline = open(root, "all_types.dart", "");
        var candidate = baseline;
        for (var node : nodes) candidate = apply(candidate, new SetProperty(node.id(), EVENT, new PropertyValue.NullValue()));
        assertAnalysis(baseline, candidate, "all_types.dart", true);
        Files.write(lib.resolve("all_types.dart"), candidate.current().dartCandidateBytes());

        var scroll = listener(new PropertyValue.StringValue("ScrollNotification"));
        var empty = open(scroll, "sample.dart", "final seen = <Notification>[]; bool stop = false;");
        var created = apply(empty, new CreateEventHandler(scroll.id(), EVENT, "_notification"));
        assertAnalysis(empty, created, "sample.dart", true);
        String implemented = source(created).replace("throw UnimplementedError('Implement _notification');",
                "seen.add(notification); return stop;");
        int ownerEnd = implemented.lastIndexOf('}');
        implemented = implemented.substring(0, ownerEnd)
                + "  bool _reference(Notification event) => _notification(event);\n" + implemented.substring(ownerEnd);
        assertTrue(implemented.contains("bool _notification(Notification notification)"), implemented);
        var reopened = reopen(created, implemented);
        var typed = apply(reopened, new SetProperty(scroll.id(), EVENT, reference("_reference")));
        assertAnalysis(reopened, typed, "sample.dart", true);
        var changed = apply(typed, new SetProperty(scroll.id(), FILTER, new PropertyValue.StringValue("ScrollStartNotification")));
        assertTrue(source(changed).contains("seen.add(notification); return stop;"));
        assertEquals(scroll.id(), changed.current().document().root().id());
        var undone = changed.undo();
        assertTrue(undone.changed(), undone.diagnostics().toString());
        assertArrayEquals(typed.current().dartCandidateBytes(), undone.session().current().dartCandidateBytes());
        Files.write(lib.resolve("sample.dart"), typed.current().dartCandidateBytes());

        StringBuilder tests = new StringBuilder("""
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:notification_contract/all_types.dart' as allTypes;
                import 'package:notification_contract/sample.dart' as sample;
                class CustomNotification extends Notification {}
                void main() {
                """);
        for (String type : TYPES) tests.append("  testWidgets('generated filter ").append(type).append("', (tester) async {\n")
                .append("    await tester.pumpWidget(const MaterialApp(home: allTypes.Sample()));\n")
                .append("    final generated = tester.widget<NotificationListener<").append(type)
                .append(">>(find.descendant(of: find.byType(allTypes.Sample), matching: find.byType(NotificationListener<")
                .append(type).append(">)));\n")
                .append("    expect(generated.onNotification, isNull);\n    expect(tester.takeException(), isNull);\n  });\n");
        tests.append("""
                  testWidgets('subtype filter, true stop, false bubble and null dispatch', (tester) async {
                    final parentEvents = <Notification>[];
                    final key = GlobalKey<sample.Storage>();
                    await tester.pumpWidget(MaterialApp(home: NotificationListener<Notification>(
                      onNotification: (event) { parentEvents.add(event); return false; },
                      child: sample.Sample(key: key))));
                    final generated = tester.widget<NotificationListener<ScrollNotification>>(
                      find.byType(NotificationListener<ScrollNotification>));
                    final target = tester.element(find.byWidget(generated.child));
                    final metrics = FixedScrollMetrics(minScrollExtent: 0, maxScrollExtent: 100,
                      pixels: 0, viewportDimension: 50, axisDirection: AxisDirection.down, devicePixelRatio: 1);
                    final start = ScrollStartNotification(metrics: metrics, context: target);
                    key.currentState!.seen.clear(); parentEvents.clear();
                    start.dispatch(target);
                    expect(key.currentState!.seen, [start]); expect(parentEvents, [start]);
                    key.currentState!.stop = true;
                    final end = ScrollEndNotification(metrics: metrics, context: target);
                    end.dispatch(target);
                    expect(key.currentState!.seen, [start, end]); expect(parentEvents, [start]);
                    final other = CustomNotification();
                    other.dispatch(target);
                    expect(key.currentState!.seen, [start, end]); expect(parentEvents, [start, other]);
                    start.dispatch(null);
                    expect(key.currentState!.seen, [start, end]);
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("notification_test.dart"), tests);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    @Test
    void customTypesAliasesAndCallbackVarianceRequireIndependentUnspoofableProofs() throws Exception {
        initialize();
        Files.writeString(lib.resolve("notifications.dart"), """
                import 'package:flutter/widgets.dart';
                class CustomNotification extends Notification {
                  const CustomNotification(this.value);
                  final int value;
                }
                class PayloadNotification<T> extends Notification {
                  const PayloadNotification(this.payload);
                  final T payload;
                }
                typedef CustomAlias = CustomNotification;
                typedef ClosedGenericAlias = PayloadNotification<int>;
                typedef DynamicAlias = dynamic;
                typedef NullableAlias = CustomNotification?;
                class WrongType {}
                final Type typeVariable = CustomNotification;
                final seen = <int>[];
                bool typed(CustomNotification value) { seen.add(value.value); return value.value == 7; }
                bool broad(Notification value) => false;
                bool incompatible(ScrollNotification value) => false;
                int wrongReturn(CustomNotification value) => 1;
                bool? nullableReturn(CustomNotification value) => null;
                dynamic get dynamicCallback => typed;
                bool Function(CustomNotification)? get nullableCallback => typed;
                """);
        var root = listener(new PropertyValue.StringValue("Notification"));
        var baseline = open(root, "custom.dart", "");
        // Type bounds remain required even when the callback is absent or explicitly null.
        for (String name : List.of("CustomNotification", "CustomAlias", "ClosedGenericAlias")) {
            var selected = apply(baseline, new SetProperty(root.id(), FILTER, imported(name)));
            assertAnalysis(baseline, selected, "custom.dart", true);
            var explicitNull = apply(selected, new SetProperty(root.id(), EVENT, new PropertyValue.NullValue()));
            assertAnalysis(baseline, explicitNull, "custom.dart", true);
        }
        var selected = apply(baseline, new SetProperty(root.id(), FILTER, imported("CustomNotification")));
        var typed = apply(selected, new SetProperty(root.id(), EVENT, imported("typed")));
        assertAnalysis(baseline, typed, "custom.dart", true);
        Files.write(lib.resolve("custom.dart"), typed.current().dartCandidateBytes());
        assertAnalysis(baseline, apply(selected, new SetProperty(root.id(), EVENT, imported("broad"))), "custom.dart", true);

        // Suppression cannot turn a wrong/dynamic/nullable alias or callback into accepted evidence.
        String ignored = "// ignore_for_file: invalid_assignment, type_argument_not_matching_bounds, argument_type_not_assignable, non_type_as_type_argument\n";
        var adversarial = reopen(baseline, ignored + source(baseline));
        for (String name : List.of("WrongType", "DynamicAlias", "NullableAlias", "typeVariable")) {
            assertAnalysis(adversarial, apply(adversarial, new SetProperty(root.id(), FILTER, imported(name))), "custom.dart", false);
        }
        var adversarialType = apply(adversarial, new SetProperty(root.id(), FILTER, imported("CustomNotification")));
        for (String name : List.of("incompatible", "wrongReturn", "nullableReturn", "dynamicCallback", "nullableCallback")) {
            assertAnalysis(adversarial, apply(adversarialType, new SetProperty(root.id(), EVENT, imported(name))), "custom.dart", false);
        }
        // A specialized handler is valid only while the selected notification type stays compatible.
        var saved = reopen(typed, source(typed));
        assertAnalysis(saved, apply(saved, new SetProperty(root.id(), FILTER, new PropertyValue.StringValue("Notification"))), "custom.dart", false);
        Files.write(lib.resolve("custom.dart"), typed.current().dartCandidateBytes());
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("custom_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:notification_contract/custom.dart' as generated;
                import 'package:notification_contract/notifications.dart' as events;
                void main() {
                  testWidgets('generated custom T filters and callback controls ancestor dispatch', (tester) async {
                    final ancestor = <Notification>[];
                    await tester.pumpWidget(MaterialApp(home: NotificationListener<Notification>(
                      onNotification: (event) { ancestor.add(event); return false; },
                      child: const generated.Sample())));
                    final wrapper = tester.widget<NotificationListener<events.CustomNotification>>(
                      find.byType(NotificationListener<events.CustomNotification>));
                    final target = tester.element(find.byWidget(wrapper.child));
                    events.seen.clear(); ancestor.clear();
                    const events.CustomNotification(3).dispatch(target);
                    const events.CustomNotification(7).dispatch(target);
                    final other = LayoutChangedNotification();
                    other.dispatch(target);
                    expect(events.seen, [3, 7]); expect(ancestor.length, 2);
                    expect((ancestor.first as events.CustomNotification).value, 3);
                    expect(ancestor.last, same(other));
                    expect(tester.takeException(), isNull);
                  });
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }

    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: notification_contract
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
                .filter(type -> type.sourceTypeBound().equals(Optional.of("Notification"))).isPresent()), ticket.request().toString());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var result = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, result);
            assertEquals(pass, accepted.ready(), () -> result + "\n" + accepted.diagnostics());
        } finally { operation.cancel(); }
        assertArrayEquals(baseline.current().dartCandidateBytes(), Files.readAllBytes(source));
    }

    private static WidgetNode listener(PropertyValue type) {
        var child = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("height"), new PropertyValue.DoubleValue(java.math.BigDecimal.ONE)), Map.of());
        return new WidgetNode(StableId.random(), TYPE, Map.of(FILTER, type),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(child)));
    }
    private static DesignerCommandSession open(WidgetNode root, String file, String members) throws Exception {
        return StateBindingRealSdkTest.openRoot(root, file, members);
    }
    private static DesignerCommandSession reopen(DesignerCommandSession session, String source) {
        var result = DesignerCommandSession.open(session.current().fdSnapshot(), source.getBytes(StandardCharsets.UTF_8), BuiltInWidgetCatalog.getDefault());
        assertTrue(result.ready(), result.diagnostics().toString());
        return result.session().orElseThrow();
    }
    private static PropertyValue reference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(), name, Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
    }
    private static PropertyValue imported(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.of("package:notification_contract/notifications.dart"), name,
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
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
