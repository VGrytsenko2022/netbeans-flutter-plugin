package dev.flutter.netbeans.plugin.designer;

import dev.flutter.netbeans.dart.*;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.codec.*;
import dev.flutter.netbeans.designer.command.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "flutter.events.sdk", matches = ".+")
class AnimatedOpacityRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId GROUP_ID = StableId.random(), NESTED_ID = StableId.random();
    private static final SlotName CHILDREN = new SlotName("children");

    @Test void insertionPropertiesMoveUndoRedoAndReopenPassAnalyzerAndNativeSdk() throws Exception {
        initialize();
        var baseline = open(root(group(GROUP_ID, List.of(adapter("Sibling", 60)))),
                "probe.dart", "// User member is preserved.\nint _userValue() => 73;\nDuration _customDuration() => const Duration(milliseconds: 25);\nCurve get _customCurve => Curves.easeIn;\nvoid _handleEnd() {}");
        var prototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault()
                .find(AnimatedOpacityWidgetPropertySchema.TYPE).orElseThrow(), NESTED_ID);
        var empty = apply(baseline, new AddWidget(place(baseline.current().document().root().id(), 1), prototype));
        assertAnalysis(baseline, empty, "probe.dart", true); saveCase("empty", empty);
        assertArrayEquals(baseline.current().dartCandidateBytes(), empty.undo().session().current().dartCandidateBytes());
        assertArrayEquals(empty.current().dartCandidateBytes(), empty.undo().session().redo().session().current().dartCandidateBytes());
        empty = reopen(empty, source(empty));
        var child = adapter("Opacity body", 40);
        var current = apply(empty, new AddWidget(new WidgetPlacement(NESTED_ID, new SlotName("child"), 0), child));
        assertAnalysis(empty, current, "probe.dart", true); saveCase("opaque", current); current = reopen(current, source(current));
        for (String opacity : List.of("0.5", "0")) {
            var changed = apply(current, new SetProperty(NESTED_ID, new PropertyName("opacity"),
                    new PropertyValue.DoubleValue(new java.math.BigDecimal(opacity))));
            assertAnalysis(current, changed, "probe.dart", true);
            saveCase(opacity.equals("0") ? "zero" : "fractional", changed);
            current = reopen(changed, source(changed));
        }
        var semantics = apply(current, new SetProperty(NESTED_ID, new PropertyName("alwaysIncludeSemantics"), new PropertyValue.BooleanValue(true)));
        assertAnalysis(current, semantics, "probe.dart", true); saveCase("semantics", semantics); semantics = reopen(semantics, source(semantics));
        var reset = apply(semantics, new ResetProperty(NESTED_ID, new PropertyName("alwaysIncludeSemantics")));
        assertAnalysis(semantics, reset, "probe.dart", true); reset = reopen(reset, source(reset));
        assertFalse(reset.apply(new ResetProperty(NESTED_ID, new PropertyName("opacity"))).changed());
        var moved = apply(reset, new MoveWidget(child.id(), place(reset.current().document().root().id(), 2)));
        assertAnalysis(reset, moved, "probe.dart", true);
        assertArrayEquals(reset.current().dartCandidateBytes(), moved.undo().session().current().dartCandidateBytes());
        saveCase("moved", moved);
        var typedBase=reopen(current,source(current));
        var typed=apply(typedBase,new PatchProperties(NESTED_ID,List.of(
            new PatchProperties.SetPatch(new PropertyName("durationUs"),ref("_customDuration",true)),
            new PatchProperties.SetPatch(new PropertyName("curve"),ref("_customCurve",false)),
            new PatchProperties.SetPatch(new PropertyName("onEnd"),ref("_handleEnd",false)))));
        assertAnalysis(typedBase,typed,"probe.dart",true);saveCase("typed",typed);
        assertArrayEquals(typedBase.current().dartCandidateBytes(),typed.undo().session().current().dartCandidateBytes());
        typed=reopen(typed,source(typed));
        var wrong=apply(typed,new SetProperty(NESTED_ID,new PropertyName("durationUs"),ref("_customCurve",false)));
        assertAnalysis(typed,wrong,"probe.dart",false);
        var cleared=apply(typed,new SetProperty(NESTED_ID,new PropertyName("onEnd"),new PropertyValue.NullValue()));
        assertAnalysis(typed,cleared,"probe.dart",true);saveCase("cleared",cleared);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("opacity_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/rendering.dart';
                import 'package:flutter_test/flutter_test.dart';
                import '../lib/empty.dart' as empty;
                import '../lib/opaque.dart' as opaque;
                import '../lib/zero.dart' as zero;
                import '../lib/fractional.dart' as fractional;
                import '../lib/semantics.dart' as semantics;
                import '../lib/moved.dart' as moved;
                import '../lib/typed.dart' as typed;
                import '../lib/cleared.dart' as cleared;
                void main() {
                  testWidgets('generated custom values and completion callback lifecycle',(tester) async {
                    await tester.pumpWidget(const MaterialApp(home:typed.Sample()));
                    await tester.pumpAndSettle();
                    final native=tester.widget<AnimatedOpacity>(find.byType(AnimatedOpacity,skipOffstage:false));
                    expect(native.curve,Curves.easeIn);expect(native.duration,const Duration(milliseconds:25));expect(native.onEnd,isNotNull);
                    native.onEnd!();
                    await tester.pumpWidget(const MaterialApp(home:cleared.Sample()));
                    expect(tester.widget<AnimatedOpacity>(find.byType(AnimatedOpacity,skipOffstage:false)).onEnd,isNull);
                    await tester.pumpWidget(const SizedBox.shrink());
                    var ends=0;var taps=0;
                    Future<void> show(double opacity,int ms) async {
                      await tester.pumpWidget(MaterialApp(home:Center(child:
                        AnimatedOpacity(opacity:opacity,duration:Duration(milliseconds:ms),onEnd:()=>ends++,
                          child:GestureDetector(behavior:HitTestBehavior.opaque,
                            onTap:()=>taps++,child:const SizedBox(width:40,height:40))))));
                      await tester.pump();
                    }
                    await show(1,100);expect(ends,0);
                    await show(0,100);await tester.pump(const Duration(milliseconds:50));expect(ends,0);
                    await show(1,100);await tester.pump(const Duration(milliseconds:101));expect(ends,1);
                    await show(1,100);await tester.pump(const Duration(milliseconds:200));expect(ends,1);
                    await show(0,0);await tester.pump();expect(ends,2);
                    await tester.tap(find.byType(GestureDetector).last);expect(taps,1);
                    expect(tester.takeException(),isNull);
                  });
                  final cases = <String, (Widget, double, bool, bool)>{
                    'empty': (const empty.Sample(), 1, false, true),
                    'opaque': (const opaque.Sample(), 1, false, false),
                    'zero': (const zero.Sample(), 0, false, false),
                    'fractional': (const fractional.Sample(), 0.5, false, false),
                    'semantics': (const semantics.Sample(), 0, true, false),
                    'moved': (const moved.Sample(), 0, false, true),
                  };
                  for (final entry in cases.entries) {
                    for (final rtl in [false,true]) {
                      testWidgets('${entry.key} rtl=$rtl', (tester) async {
                        await tester.pumpWidget(MaterialApp(home: Directionality(
                          textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:entry.value.$1)));
                        await tester.pumpAndSettle();
                        final native=tester.widget<AnimatedOpacity>(find.byType(AnimatedOpacity,skipOffstage:false));
                        expect(native.opacity,entry.value.$2);
                        expect(native.alwaysIncludeSemantics,entry.value.$3);
                        if(entry.value.$4) expect(native.child,isNull);
                        final render=tester.renderObject<RenderAnimatedOpacity>(find.byType(AnimatedOpacity,skipOffstage:false));
                        expect(render.size.height,entry.value.$4?0:40);
                        final children=<RenderObject>[];
                        render.visitChildrenForSemantics(children.add);
                        expect(children.isNotEmpty,!entry.value.$4&&(entry.value.$2>0||entry.value.$3));
                        expect(tester.takeException(),isNull);
                      });
                    }
                  }
                }
                """);
        run(List.of(flutter.toString(), "test", "--reporter", "expanded"), "flutter-test.log");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean factory){
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),
            factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
            factory?Optional.of(false):Optional.empty());
    }
    private void saveCase(String name, DesignerCommandSession session) throws Exception {
        var reopened = reopen(session, source(session));
        assertEquals(session.current().fdSnapshot(), reopened.current().fdSnapshot());
        assertArrayEquals(session.current().dartCandidateBytes(), reopened.current().dartCandidateBytes());
        assertTrue(source(reopened).contains("// User member is preserved."));
        assertTrue(source(reopened).contains("int _userValue() => 73;"));
        Files.write(lib.resolve(name + ".dart"), reopened.current().dartCandidateBytes());
    }
    private static WidgetPlacement place(StableId id, int index) { return new WidgetPlacement(id, CHILDREN, index); }
    private static WidgetNode group(StableId id, List<WidgetNode> children) {
        return new WidgetNode(id, new WidgetTypeId("flutter.widgets.Column"), Map.of(), Map.of(CHILDREN, new WidgetSlot.ListSlot(children)));
    }
    private static WidgetNode adapter(String label, int extent) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(label)), Map.of());
        var size = new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(extent));
        var box = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), size, new PropertyName("height"), size),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
        return box;
    }
    private static WidgetNode root(WidgetNode group) { return group; }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: animated_opacity_contract
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
        assertFalse(ticket.request().symbolProbes().isEmpty());
        var operation = new DartCandidateAnalyzer(dart, ignored -> { }).analyze(ticket.request());
        try {
            var result = operation.result().toCompletableFuture().get(90, TimeUnit.SECONDS);
            var accepted = PairSaveEvidenceGate.evaluateAnalysis(ticket, result);
            assertEquals(pass, accepted.ready(), () -> result + "\n" + accepted.diagnostics());
            if (!pass) assertTrue(result.status() == DartCandidateAnalysisStatus.REJECTED || result.symbolEvidence().stream().anyMatch(evidence -> evidence.staticTypeEvidence()
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
