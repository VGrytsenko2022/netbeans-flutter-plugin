package io.github.vgrytsenko2022.plugin.designer;

import io.github.vgrytsenko2022.dart.*;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.codec.*;
import io.github.vgrytsenko2022.designer.command.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.designer.validation.ValidationResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class TimePickerDialogRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();



    @Test void exactTimeAndCallbackProofsGeneratedCodeAndRealFlutterRendering() throws Exception {
        initialize();
        Files.writeString(lib.resolve("sources.dart"), """
                import 'package:flutter/material.dart';
                const picked = TimeOfDay(hour: 23, minute: 59);
                TimeOfDay get time => picked;
                TimeOfDay makeTime() => picked;
                void changed(TimePickerEntryMode mode) {}
                EntryModeChangeCallback makeChanged() => changed;
                class Times {
                  static TimeOfDay get time => picked;
                  static TimeOfDay makeTime() => picked;
                  static void changed(TimePickerEntryMode mode) {}
                  static EntryModeChangeCallback makeChanged() => changed;
                }
                """);
        String members="""
                // User member is preserved.
                int _userValue() => 73;
                TimeOfDay get time => refs.picked;
                TimeOfDay makeTime() => refs.picked;
                static TimeOfDay get staticTime => refs.picked;
                static TimeOfDay makeStaticTime() => refs.picked;
                void changed(TimePickerEntryMode mode) {}
                EntryModeChangeCallback makeChanged() => changed;
                static void staticChanged(TimePickerEntryMode mode) {}
                static EntryModeChangeCallback makeStaticChanged() => staticChanged;
                Object get wrong => Object();
                dynamic get unsafe => refs.picked;
                TimeOfDay? get nullable => null;
                EntryModeChangeCallback? get nullableCallback => null;
                void wrongCallback(DateTime date) {}
                Key get timeKey => const ValueKey('project-time');
                """;
        var root=group(StableId.random(),List.of());
        var baseline=open(root,"probe.dart",members);
        baseline=reopen(baseline,source(baseline).replace("class Sample extends","import 'package:time_picker_contract/sources.dart' as refs;\nclass Sample extends"));
        var picker=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(TimePickerDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var current=apply(baseline,new AddWidget(place(root.id(),0),picker));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter",current);
        baseline=reopen(current,source(current));
        for(String field:List.of("initialTime","onEntryModeChanged")){
            String noun=field.equals("initialTime")?"Time":"Changed",lower=Character.toLowerCase(noun.charAt(0))+noun.substring(1);
            for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
                String name=member?(imported?"Times":"Storage"):(factory?"make"+noun:lower);
                var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:time_picker_contract/sources.dart"):Optional.empty(),name,
                        member?Optional.of(imported?(factory?"make"+noun:lower):(factory?"makeStatic"+noun:"static"+noun)):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),value));
                assertAnalysis(before,current,"probe.dart",true);save("source_"+cases.size(),current);
            }
            for(String invalid:List.of("wrong","unsafe",field.equals("initialTime")?"nullable":"nullableCallback")){
                var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref(invalid)));
                assertAnalysis(before,current,"probe.dart",false);
            }
        }
        {
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName("onEntryModeChanged"),ref("wrongCallback")));
            assertAnalysis(before,current,"probe.dart",false);
        }
        current=baseline;
        for(String mode:List.of("dial","input","dialOnly","inputOnly")){
            var before=reopen(current,source(current));current=before;var configured=apply(before,new SetProperty(picker.id(),new PropertyName("initialEntryMode"),new PropertyValue.EnumValue("TimePickerEntryMode",mode)));
            configured=apply(configured,new SetProperty(picker.id(),new PropertyName("emptyInitialInput"),new PropertyValue.BooleanValue(mode.startsWith("input"))));
            configured=apply(configured,new SetProperty(picker.id(),new PropertyName("orientation"),new PropertyValue.EnumValue("Orientation","portrait")));
            assertAnalysis(before,configured,"probe.dart",true);save("mode_"+mode,configured);
        }
        for(String time:List.of("00:00","12:00","23:59")){
            var before=reopen(current,source(current));current=before;var candidate=apply(before,new SetProperty(picker.id(),new PropertyName("initialTime"),new PropertyValue.StringValue(time)));
            assertAnalysis(before,candidate,"probe.dart",true);save("time_"+cases.size(),candidate);
        }
        var before=reopen(current,source(current));current=before;
        for(String field:List.of("cancelText","confirmText","helpText","errorInvalidText","hourLabelText","minuteLabelText","restorationId"))
            current=apply(current,new SetProperty(picker.id(),new PropertyName(field),new PropertyValue.StringValue("Custom "+field)));
        current=apply(current,new SetProperty(picker.id(),new PropertyName("orientation"),new PropertyValue.EnumValue("Orientation","landscape")));
        current=apply(current,new SetProperty(picker.id(),new PropertyName("key"),ref("timeKey")));
        for(String slot:List.of("switchToInputEntryModeIcon","switchToTimerEntryModeIcon")){
            var icon=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow(),StableId.random());
            current=apply(current,new AddWidget(new WidgetPlacement(picker.id(),new SlotName(slot),0),icon));
        }
        assertAnalysis(before,current,"probe.dart",true);save("all_arguments",current);
        before=reopen(current,source(current));current=before;current=apply(before,new CreateEventHandler(picker.id(),new PropertyName("onEntryModeChanged"),"_modeChanged"));
        assertAnalysis(before,current,"probe.dart",true);save("created_event",current);
        before=reopen(current,source(current).replace("// TODO: Handle onEntryModeChanged.","_userValue(); // Real user logic."));
        current=apply(before,new RenameEventHandler(picker.id(),new PropertyName("onEntryModeChanged"),"_renamedMode"));
        assertAnalysis(before,current,"probe.dart",true);save("renamed_event",current);
        assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        before=reopen(current,source(current));current=before;
        for(String field:List.of("key","orientation","onEntryModeChanged","cancelText","confirmText","helpText","errorInvalidText","hourLabelText","minuteLabelText","restorationId"))
            current=apply(current,new SetProperty(picker.id(),new PropertyName(field),new PropertyValue.NullValue()));
        assertAnalysis(before,current,"probe.dart",true);save("explicit_nulls",current);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("TimePickerDialog: "+(cases.size()*2)+" generated SDK rendering cases passed; 7 unsafe source shapes rejected.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession session)throws Exception{saveCase(name,session);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(String name:cases)out.append("import 'package:time_picker_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(boolean rtl:List.of(false,true))out.append("""
              testWidgets('NAME RTL', (tester) async {
                await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                await tester.pumpAndSettle();
                expect(find.byType(TimePickerDialog),findsOneWidget);
                final widget=tester.widget<TimePickerDialog>(find.byType(TimePickerDialog));
                expect(widget.initialTime.hour,inInclusiveRange(0,23));
                expect(widget.initialTime.minute,inInclusiveRange(0,59));
                expect(tester.takeException(),isNull);
                await tester.pumpWidget(const SizedBox());await tester.pump();
              });
                """.replace("NAME",name).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr"));
        return out.append("}\n").toString();
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

    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: time_picker_contract
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
        if(candidate.current().preparedPair().isEmpty()) {
            assertTrue(pass,"Only unchanged Dart can omit a candidate analysis pair");
            assertArrayEquals(baseline.current().dartCandidateBytes(),candidate.current().dartCandidateBytes());
            return;
        }
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
