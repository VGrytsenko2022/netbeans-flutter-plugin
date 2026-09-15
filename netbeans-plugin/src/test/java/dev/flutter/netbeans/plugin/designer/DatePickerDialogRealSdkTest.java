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
class DatePickerDialogRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();


    @Test void dateSourcesModesPredicatesAndNavigatorResults() throws Exception {
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
                class Dates {
                  static DateTime get date => picked;
                  static DateTime makeDate() => picked;
                  static CalendarDelegate<DateTime> get calendar => const ProjectCalendar();
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
                Object get wrong => Object();
                dynamic get unsafe => refs.picked;
                DateTime? get nullable => null;
                Object get wrongCalendar => Object();
                dynamic get unsafeCalendar => refs.calendar;
                CalendarDelegate<DateTime>? get nullableCalendar => null;
                EdgeInsets get padding => const EdgeInsets.all(8);
                TextInputType get keyboard => TextInputType.datetime;
                """;
        var root=group(StableId.random(),List.of());
        var baseline=open(root,"probe.dart",members);
        baseline=reopen(baseline,source(baseline).replace("class Sample extends","import 'package:date_picker_contract/sources.dart' as refs;\nclass Sample extends"));
        var c=BuiltInWidgetCatalog.getDefault();
        var picker=WidgetNodePrototypeFactory.create(c.find(DatePickerDialogWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var current=apply(baseline,new AddWidget(place(root.id(),0),picker));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        baseline=reopen(current,source(current));
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)) {
            String name=member?(imported?"Dates":"Storage"):(factory?"makeDate":"date");
            var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:date_picker_contract/sources.dart"):Optional.empty(),name,
                    member?Optional.of(imported?(factory?"makeDate":"date"):(factory?"makeStaticDate":"staticDate")):Optional.empty(),
                    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory?Optional.of(false):Optional.empty());
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName("initialDate"),value));
            assertAnalysis(before,current,"probe.dart",true);save("date_"+cases.size(),current);
        }
        for(String field:List.of("firstDate","lastDate","currentDate")) {
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref("date")));
            assertAnalysis(before,current,"probe.dart",true);save(field,current);
        }
        for(boolean imported:List.of(false,true))for(boolean factory:List.of(false,true)) {
            var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:date_picker_contract/sources.dart"):Optional.empty(),
                    factory?"makeCalendar":"calendar",Optional.empty(),
                    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName("calendarDelegate"),value));
            assertAnalysis(before,current,"probe.dart",true);save("calendar_"+cases.size(),current);
        }
        for(String name:List.of("wrong","unsafe","nullable","wrongCalendar","unsafeCalendar","nullableCalendar")) {
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(name.endsWith("Calendar")?"calendarDelegate":"firstDate"),ref(name)));
            assertAnalysis(before,current,"probe.dart",false);
        }
        current=baseline;
        for(String event:List.of("onDatePickerModeChange","selectableDayPredicate")) {
            var before=reopen(current,source(current));current=apply(before,new CreateEventHandler(picker.id(),new PropertyName(event),event.equals("onDatePickerModeChange")?"_modeChanged":"_allowedDate"));
            assertAnalysis(before,current,"probe.dart",true);save("event_"+cases.size(),current);
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        }
        // Every source remains editable, including the predicate body.
        assertTrue(source(current).contains("bool _allowedDate(DateTime date)"));
        assertTrue(source(current).contains("return true;"));
        var configured=apply(current,new SetProperty(picker.id(),new PropertyName("initialDate"),new PropertyValue.StringValue("2024-02-29")));
        for(String name:List.of("cancelText","confirmText","helpText","errorFormatText","errorInvalidText","fieldHintText","fieldLabelText","restorationId"))
            configured=apply(configured,new SetProperty(picker.id(),new PropertyName(name),new PropertyValue.StringValue(name)));
        configured=apply(configured,new SetProperty(picker.id(),new PropertyName("currentDate"),new PropertyValue.StringValue("2024-02-15")));
        configured=apply(configured,new SetProperty(picker.id(),new PropertyName("insetPadding"),ref("padding")));
        configured=apply(configured,new SetProperty(picker.id(),new PropertyName("keyboardType"),ref("keyboard")));
        DesignerCommandSession lastBefore=reopen(configured,source(configured));
        for(String slot:List.of("switchToInputEntryModeIcon","switchToCalendarEntryModeIcon")){
            lastBefore=reopen(configured,source(configured));
            var icon=WidgetNodePrototypeFactory.create(c.find(new WidgetTypeId("flutter.widgets.Icon")).orElseThrow(),StableId.random());
            configured=apply(lastBefore,new AddWidget(new WidgetPlacement(picker.id(),new SlotName(slot),0),icon));
        }
        assertAnalysis(lastBefore,configured,"probe.dart",true);save("configured",configured);
        for(String entry:List.of("calendar","input","calendarOnly","inputOnly"))for(String mode:List.of("day","year")) {
            var candidate=apply(baseline,new SetProperty(picker.id(),new PropertyName("initialEntryMode"),en("DatePickerEntryMode",entry)));
            var before=reopen(candidate,source(candidate));candidate=apply(before,new SetProperty(picker.id(),new PropertyName("initialCalendarMode"),en("DatePickerMode",mode)));
            assertAnalysis(before,candidate,"probe.dart",true);save("mode_"+cases.size(),candidate);
        }
        for(String keyboard:DatePickerDialogWidgetPropertySchema.KEYBOARDS){
            var candidate=apply(baseline,new SetProperty(picker.id(),new PropertyName("keyboardType"),new PropertyValue.StringValue(keyboard)));
            assertAnalysis(baseline,candidate,"probe.dart",true);save("keyboard_"+cases.size(),candidate);
        }
        var route=apply(baseline,new SetProperty(picker.id(),new PropertyName("initialDate"),new PropertyValue.StringValue("2024-02-29")));
        var routeBefore=reopen(route,source(route));route=apply(routeBefore,new SetProperty(picker.id(),new PropertyName("currentDate"),new PropertyValue.StringValue("2024-02-15")));
        assertAnalysis(routeBefore,route,"probe.dart",true);save("route",route);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("DatePickerDialog: "+(cases.size()*2+3)+" generated/native SDK cases passed.");
    }
    private static PropertyValue.EnumValue en(String type,String value){return new PropertyValue.EnumValue(type,value);}
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession session)throws Exception{saveCase(name,session);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\nimport 'package:date_picker_contract/sources.dart' as refs;\n");
        for(String name:cases)out.append("import 'package:date_picker_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(boolean rtl:List.of(false,true))out.append("""
              testWidgets('NAME RTL', (tester) async {
                await tester.pumpWidget(MaterialApp(restorationScopeId:'app',home:Directionality(textDirection:DIR,
                  child:Scaffold(body:NAME.Sample()))));
                await tester.pumpAndSettle();
                expect(find.byType(DatePickerDialog),findsOneWidget);
                final widget=tester.widget<DatePickerDialog>(find.byType(DatePickerDialog));
                expect(widget.firstDate.isAfter(widget.lastDate),isFalse);
                expect(tester.takeException(),isNull);
                await tester.pumpWidget(const SizedBox());await tester.pump();
              });
                """.replace("NAME",name).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr"));
        out.append("""
              testWidgets('confirm and cancel return the native Navigator results', (tester) async {
                DateTime? result; bool completed=false;
                await tester.pumpWidget(MaterialApp(home:Builder(builder:(context)=>Scaffold(body:TextButton(
                  onPressed:() async {result=await showDialog<DateTime>(context:context,builder:(_)=>route.Sample());completed=true;},
                  child:const Text('Open'))))));
                await tester.tap(find.text('Open'));await tester.pumpAndSettle();
                await tester.tap(find.text('OK'));await tester.pumpAndSettle();
                expect(completed,isTrue);expect(result,DateTime(2024,2,29));
                completed=false;await tester.tap(find.text('Open'));await tester.pumpAndSettle();
                await tester.tap(find.text('Cancel'));await tester.pumpAndSettle();
                expect(completed,isTrue);expect(result,isNull);
              });
              testWidgets('custom calendar methods are invoked only by the application', (tester) async {
                refs.calendarCalls=0;
                await tester.pumpWidget(MaterialApp(home:DatePickerDialog(firstDate:DateTime(2000),lastDate:DateTime(2030),
                  initialDate:DateTime(2024,2,29),calendarDelegate:refs.calendar)));
                await tester.pumpAndSettle();expect(refs.calendarCalls,greaterThan(0));expect(tester.takeException(),isNull);
              });
              testWidgets('mode callback and editable predicate drive native behavior', (tester) async {
                DatePickerEntryMode? mode;int allowedCalls=0;
                await tester.pumpWidget(MaterialApp(home:DatePickerDialog(firstDate:DateTime(2024),lastDate:DateTime(2025),
                  initialDate:DateTime(2024,2,29),onDatePickerModeChange:(value)=>mode=value,
                  selectableDayPredicate:(date){allowedCalls++;return date.weekday!=DateTime.sunday;})));
                await tester.pumpAndSettle();
                await tester.tap(find.byTooltip('Switch to input'));await tester.pumpAndSettle();
                expect(mode,DatePickerEntryMode.input);expect(allowedCalls,greaterThan(0));expect(tester.takeException(),isNull);
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
    private static WidgetNode adapter(String label, int extent) {
        var text = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"), new PropertyValue.StringValue(label)), Map.of());
        var size = new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(extent));
        var box = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.SizedBox"),
                Map.of(new PropertyName("width"), size, new PropertyName("height"), size),
                Map.of(new SlotName("child"), WidgetSlot.SingleSlot.of(text)));
        return box;
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: date_picker_contract
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
