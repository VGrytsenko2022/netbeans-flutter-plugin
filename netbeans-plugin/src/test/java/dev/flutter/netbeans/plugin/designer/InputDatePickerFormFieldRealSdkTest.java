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
import org.junit.jupiter.api.io.CleanupMode;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class InputDatePickerFormFieldRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();


    @Test void datesEventsFormValidationFocusAndNativeInputLifecycle() throws Exception {
        initialize();
        Files.writeString(lib.resolve("sources.dart"), """
                import 'package:flutter/material.dart';
                final picked = DateTime(2024, 2, 29);
                DateTime get date => picked;
                DateTime makeDate() => picked;
                int calendarCalls = 0;
                class ProjectCalendar extends GregorianCalendarDelegate {
                  const ProjectCalendar();
                  @override DateTime dateOnly(DateTime date) { calendarCalls++; return super.dateOnly(date); }
                  @override DateTime now() => DateTime(2024,2,15);
                }
                const CalendarDelegate<DateTime> calendar = ProjectCalendar();
                CalendarDelegate<DateTime> makeCalendar() => calendar;
                void changed(DateTime date) {}
                ValueChanged<DateTime> makeChanged() => changed;
                bool allowed(DateTime date) => date.weekday != DateTime.sunday;
                SelectableDayPredicate makeAllowed() => allowed;
                final focus = FocusNode(debugLabel: 'imported-focus');
                FocusNode makeFocus() => focus;
                const keyboard = TextInputType.datetime;
                TextInputType makeKeyboard() => keyboard;
                class Dates {
                  static FocusNode get focus => _sharedFocus;
                  static final _sharedFocus = FocusNode(debugLabel: 'static-imported-focus');
                  static FocusNode makeFocus() => focus;
                  static TextInputType get keyboard => TextInputType.datetime;
                  static TextInputType makeKeyboard() => keyboard;
                  static DateTime get date => picked;
                  static DateTime makeDate() => picked;
                  static CalendarDelegate<DateTime> get calendar => const ProjectCalendar();
                  static CalendarDelegate<DateTime> makeCalendar() => calendar;
                  static void changed(DateTime date) {}
                  static ValueChanged<DateTime> makeChanged() => changed;
                }
                """);
        String members="""
                // User member is preserved.
                int _userValue() => 73;
                DateTime get date => refs.picked;
                DateTime makeDate() => refs.picked;
                static DateTime get staticDate => refs.picked;
                static DateTime makeStaticDate() => refs.picked;
                CalendarDelegate<DateTime> get calendar => refs.calendar;
                CalendarDelegate<DateTime> makeCalendar() => refs.calendar;
                static CalendarDelegate<DateTime> get staticCalendar => refs.calendar;
                static CalendarDelegate<DateTime> makeStaticCalendar() => refs.calendar;
                void changed(DateTime date) {}
                ValueChanged<DateTime> makeChanged() => changed;
                static void staticChanged(DateTime date) {}
                static ValueChanged<DateTime> makeStaticChanged() => staticChanged;
                bool allowed(DateTime date) => date.weekday != DateTime.sunday;
                SelectableDayPredicate makeAllowed() => allowed;
                Object get wrong => Object();
                dynamic get unsafe => refs.picked;
                DateTime? get nullable => null;
                CalendarDelegate<DateTime>? get nullableCalendar => null;
                void wrongCallback(int date) {}
                ValueChanged<DateTime>? get nullableCallback => null;
                FocusNode? get nullableFocus => null;
                final ownedFocus = FocusNode(debugLabel: 'input-date-owned');
                FocusNode get focus => ownedFocus;
                FocusNode makeFocus() => ownedFocus;
                static final sharedFocus = FocusNode(debugLabel: 'input-date-shared');
                static FocusNode get staticFocus => sharedFocus;
                static FocusNode makeStaticFocus() => sharedFocus;
                TextInputType get keyboard => TextInputType.datetime;
                TextInputType makeKeyboard() => keyboard;
                static TextInputType get staticKeyboard => TextInputType.datetime;
                static TextInputType makeStaticKeyboard() => staticKeyboard;
                Key get calendarKey => const ValueKey('project-calendar');
                """;
        var root=group(StableId.random(),List.of());
        var baseline=open(root,"probe.dart",members);
        baseline=reopen(baseline,source(baseline).replace("class Sample extends","import 'package:input_date_picker_contract/sources.dart' as refs;\nclass Sample extends"));
        var picker=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(InputDatePickerFormFieldWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var current=apply(baseline,new AddWidget(place(root.id(),0),picker));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        baseline=reopen(current,source(current));
        for(String field:List.of("initialDate","calendarDelegate","onDateSubmitted","onDateSaved","focusNode","keyboardType")){
            String noun=field.equals("calendarDelegate")?"Calendar":field.equals("focusNode")?"Focus":field.equals("keyboardType")?"Keyboard":field.startsWith("on")?"Changed":"Date";
            String lower=Character.toLowerCase(noun.charAt(0))+noun.substring(1);
            for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
                String name=member?(imported?"Dates":"Storage"):(factory?"make"+noun:lower);
                var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:input_date_picker_contract/sources.dart"):Optional.empty(),name,
                        member?Optional.of(imported?(factory?"make"+noun:lower):(factory?"makeStatic"+noun:"static"+noun)):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),value));
                assertAnalysis(before,current,"probe.dart",true);save("source_"+cases.size(),current);
            }
        }
        for(String field:List.of("firstDate","lastDate")){
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref("date")));
            assertAnalysis(before,current,"probe.dart",true);save(field,current);
        }
        for(String field:List.of("initialDate")){
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref("nullable")));
            assertAnalysis(before,current,"probe.dart",true);save("nullable_"+field,current);
        }
        for(String field:List.of("firstDate","initialDate","calendarDelegate","onDateSubmitted","onDateSaved","selectableDayPredicate","focusNode","keyboardType")){
            for(String name:List.of("wrong","unsafe")){
                var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref(name)));
                assertAnalysis(before,current,"probe.dart",false);
            }
        }
        for(var entry:Map.of("firstDate","nullable","calendarDelegate","nullableCalendar","onDateSubmitted","nullableCallback","onDateSaved","wrongCallback").entrySet()){
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(entry.getKey()),ref(entry.getValue())));
            assertAnalysis(before,current,"probe.dart",false);
        }
        for(boolean imported:List.of(false,true))for(boolean factory:List.of(false,true)){
            var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:input_date_picker_contract/sources.dart"):Optional.empty(),
                    factory?"makeAllowed":"allowed",Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory?Optional.of(false):Optional.empty());
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName("selectableDayPredicate"),value));
            assertAnalysis(before,current,"probe.dart",true);save("predicate_"+cases.size(),current);
        }
        current=baseline;
        for(String event:List.of("onDateSubmitted","onDateSaved","selectableDayPredicate")){
            var before=reopen(current,source(current));current=apply(before,new CreateEventHandler(picker.id(),new PropertyName(event),"_"+event));
            assertAnalysis(before,current,"probe.dart",true);save("event_"+cases.size(),current);
            before=reopen(current,source(current));current=apply(before,new RenameEventHandler(picker.id(),new PropertyName(event),"_renamed"+event));
            assertAnalysis(before,current,"probe.dart",true);save("renamed_"+cases.size(),current);
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        }
        for(String keyboard:DatePickerDialogWidgetPropertySchema.KEYBOARDS){
            var before=reopen(current,source(current));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName("keyboardType"),new PropertyValue.StringValue(keyboard)));
            assertAnalysis(before,candidate,"probe.dart",true);save("keyboard_"+keyboard,candidate);
        }
        for(String flag:List.of("autofocus","acceptEmptyDate"))for(boolean value:List.of(false,true)){
            var before=reopen(current,source(current));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName(flag),new PropertyValue.BooleanValue(value)));
            assertAnalysis(before,candidate,"probe.dart",true);save(flag+"_"+value,candidate);
        }
        for(String field:List.of("fieldHintText","fieldLabelText","errorFormatText","errorInvalidText"))for(String value:List.of("","Custom "+field)){
            var before=reopen(current,source(current));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName(field),new PropertyValue.StringValue(value)));
            assertAnalysis(before,candidate,"probe.dart",true);save("text_"+cases.size(),candidate);
        }
        {
            var before=reopen(current,source(current));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName("focusNode"),ref("nullableFocus")));
            assertAnalysis(before,candidate,"probe.dart",true);save("nullable_focus",candidate);
        }
        for(String field:List.of("initialDate","onDateSubmitted","onDateSaved","selectableDayPredicate","key","focusNode","keyboardType","errorFormatText","errorInvalidText","fieldHintText","fieldLabelText")){
            var input=field.equals("initialDate")?apply(current,new SetProperty(picker.id(),new PropertyName(field),new PropertyValue.StringValue("2024-02-29"))):current;
            var before=reopen(input,source(input));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName(field),new PropertyValue.NullValue()));
            assertAnalysis(before,candidate,"probe.dart",true);save("null_"+field,candidate);
        }
        for(PropertyValue key:List.of(new PropertyValue.StringValue("reset-calendar"),ref("calendarKey"))){
            var before=reopen(current,source(current));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName("key"),key));
            assertAnalysis(before,candidate,"probe.dart",true);save("key_"+cases.size(),candidate);
        }
        var before=reopen(current,source(current));var dated=apply(before,new SetProperty(picker.id(),new PropertyName("initialDate"),new PropertyValue.StringValue("2024-02-29")));
        assertAnalysis(before,dated,"probe.dart",true);save("dated",dated);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("InputDatePickerFormField: "+(cases.size()*2+5)+" generated/native SDK cases passed; 20 unsafe source shapes rejected.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession session)throws Exception{saveCase(name,session);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\nimport 'package:input_date_picker_contract/sources.dart' as refs;\n");
        for(String name:cases)out.append("import 'package:input_date_picker_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(boolean rtl:List.of(false,true))out.append("""
              testWidgets('NAME RTL', (tester) async {
                await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                await tester.pumpAndSettle();
                expect(find.byType(InputDatePickerFormField),findsOneWidget);
                final widget=tester.widget<InputDatePickerFormField>(find.byType(InputDatePickerFormField));
                expect(widget.firstDate.isAfter(widget.lastDate),isFalse);
                expect(tester.takeException(),isNull);
                await tester.pumpWidget(const SizedBox());await tester.pump();
              });
                """.replace("NAME",name).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr"));
        out.append("""
              testWidgets('validation submission and form save are separate and accept only valid dates', (tester) async {
                final key=GlobalKey<FormState>(),submitted=<DateTime>[],saved=<DateTime>[];
                await tester.pumpWidget(MaterialApp(home:Scaffold(body:Form(key:key,child:InputDatePickerFormField(
                  firstDate:DateTime(2024),lastDate:DateTime(2024,12,31),onDateSubmitted:submitted.add,onDateSaved:saved.add,
                  errorFormatText:'Bad format',errorInvalidText:'Bad date',selectableDayPredicate:(date)=>date.day!=20)))));
                await tester.pumpAndSettle();
                for(final text in ['nonsense','02/30/2024','01/01/2023','02/20/2024']){
                  await tester.enterText(find.byType(TextFormField),text);
                  expect(key.currentState!.validate(),isFalse);key.currentState!.save();
                  await tester.testTextInput.receiveAction(TextInputAction.done);await tester.pumpAndSettle();
                  expect(submitted,isEmpty);expect(saved,isEmpty);
                  expect(find.text(text=='nonsense'||text=='02/30/2024'?'Bad format':'Bad date'),findsOneWidget);
                }
                await tester.enterText(find.byType(TextFormField),'02/29/2024');
                expect(key.currentState!.validate(),isTrue);
                await tester.testTextInput.receiveAction(TextInputAction.done);await tester.pumpAndSettle();
                expect(submitted,[DateTime(2024,2,29)]);expect(saved,isEmpty);
                key.currentState!.save();expect(saved,[DateTime(2024,2,29)]);
                expect(tester.takeException(),isNull);
              });
              testWidgets('acceptEmptyDate validates empty without emitting null or stale date', (tester) async {
                final key=GlobalKey<FormState>(),events=<DateTime>[];
                await tester.pumpWidget(MaterialApp(home:Scaffold(body:Form(key:key,child:InputDatePickerFormField(
                  firstDate:DateTime(2024),lastDate:DateTime(2024,12,31),initialDate:DateTime(2024,2,29),
                  acceptEmptyDate:true,onDateSubmitted:events.add,onDateSaved:events.add)))));
                await tester.pumpAndSettle();await tester.enterText(find.byType(TextFormField),'');
                expect(key.currentState!.validate(),isTrue);key.currentState!.save();
                await tester.testTextInput.receiveAction(TextInputAction.done);await tester.pumpAndSettle();
                expect(events,isEmpty);expect(tester.takeException(),isNull);
              });
              testWidgets('same-key changed initialDate updates next frame; unchanged retains typing; null clears', (tester) async {
                Widget app(DateTime? initial)=>MaterialApp(home:Scaffold(body:InputDatePickerFormField(key:const ValueKey('same'),
                  initialDate:initial,firstDate:DateTime(2024),lastDate:DateTime(2025,12,31))));
                String text()=>tester.widget<EditableText>(find.byType(EditableText)).controller.text;
                await tester.pumpWidget(app(DateTime(2024,2,29)));await tester.pumpAndSettle();expect(text(),'02/29/2024');
                await tester.enterText(find.byType(TextFormField),'03/15/2024');
                await tester.pumpWidget(app(DateTime(2024,2,29)));await tester.pumpAndSettle();expect(text(),'03/15/2024');
                await tester.pumpWidget(app(DateTime(2025,1,2)));await tester.pumpAndSettle();expect(text(),'01/02/2025');
                await tester.pumpWidget(app(null));await tester.pumpAndSettle();expect(text(),isEmpty);
                expect(tester.takeException(),isNull);
              });
              testWidgets('autofocus initial selection and caller-owned focus node lifetime', (tester) async {
                final focus=FocusNode(debugLabel:'test-owned');
                await tester.pumpWidget(MaterialApp(home:Scaffold(body:InputDatePickerFormField(
                  initialDate:DateTime(2024,2,29),firstDate:DateTime(2024),lastDate:DateTime(2025),focusNode:focus,autofocus:true))));
                await tester.pumpAndSettle();expect(focus.hasFocus,isTrue);
                final controller=tester.widget<EditableText>(find.byType(EditableText)).controller;
                expect(controller.selection,TextSelection(baseOffset:0,extentOffset:10));
                await tester.pumpWidget(const SizedBox());await tester.pumpAndSettle();
                expect(()=>focus.addListener((){}),returnsNormally);focus.dispose();
                expect(tester.takeException(),isNull);
              });
              testWidgets('project calendar owns conversion formatting parsing and native predicate', (tester) async {
                refs.calendarCalls=0;int predicates=0;
                await tester.pumpWidget(MaterialApp(home:Scaffold(body:InputDatePickerFormField(
                  initialDate:DateTime(2024,2,29),firstDate:DateTime(2024),lastDate:DateTime(2025),
                  calendarDelegate:refs.calendar,selectableDayPredicate:(date){predicates++;return true;}))));
                await tester.pumpAndSettle();
                expect(refs.calendarCalls,greaterThan(0));expect(predicates,greaterThan(0));
                expect(tester.widget<EditableText>(find.byType(EditableText)).controller.text,'02/29/2024');
                expect(tester.takeException(),isNull);
              });
            }
            """);
        return out.toString();
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
                name: input_date_picker_contract
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
