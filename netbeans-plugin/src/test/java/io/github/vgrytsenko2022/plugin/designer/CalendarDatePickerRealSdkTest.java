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
class CalendarDatePickerRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();


    @Test void datesEventsPredicateKeysAndNativeCalendarLifecycle() throws Exception {
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
                class Dates {
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
                Key get calendarKey => const ValueKey('project-calendar');
                """;
        var root=group(StableId.random(),List.of());
        var baseline=open(root,"probe.dart",members);
        baseline=reopen(baseline,source(baseline).replace("class Sample extends","import 'package:calendar_date_picker_contract/sources.dart' as refs;\nclass Sample extends"));
        var picker=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(CalendarDatePickerWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var current=apply(baseline,new AddWidget(place(root.id(),0),picker));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        baseline=reopen(current,source(current));
        for(String field:List.of("initialDate","calendarDelegate","onDateChanged","onDisplayedMonthChanged")){
            String noun=field.equals("calendarDelegate")?"Calendar":field.startsWith("on")?"Changed":"Date";
            String lower=Character.toLowerCase(noun.charAt(0))+noun.substring(1);
            for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
                String name=member?(imported?"Dates":"Storage"):(factory?"make"+noun:lower);
                var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:calendar_date_picker_contract/sources.dart"):Optional.empty(),name,
                        member?Optional.of(imported?(factory?"make"+noun:lower):(factory?"makeStatic"+noun:"static"+noun)):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),value));
                assertAnalysis(before,current,"probe.dart",true);save("source_"+cases.size(),current);
            }
        }
        for(String field:List.of("firstDate","lastDate","currentDate")){
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref("date")));
            assertAnalysis(before,current,"probe.dart",true);save(field,current);
        }
        for(String field:List.of("initialDate","currentDate")){
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref("nullable")));
            assertAnalysis(before,current,"probe.dart",true);save("nullable_"+field,current);
        }
        for(String field:List.of("firstDate","initialDate","calendarDelegate","onDateChanged","onDisplayedMonthChanged","selectableDayPredicate")){
            for(String name:List.of("wrong","unsafe")){
                var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(field),ref(name)));
                assertAnalysis(before,current,"probe.dart",false);
            }
        }
        for(var entry:Map.of("firstDate","nullable","calendarDelegate","nullableCalendar","onDateChanged","nullableCallback","onDisplayedMonthChanged","wrongCallback").entrySet()){
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName(entry.getKey()),ref(entry.getValue())));
            assertAnalysis(before,current,"probe.dart",false);
        }
        for(boolean imported:List.of(false,true))for(boolean factory:List.of(false,true)){
            var value=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:calendar_date_picker_contract/sources.dart"):Optional.empty(),
                    factory?"makeAllowed":"allowed",Optional.empty(),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory?Optional.of(false):Optional.empty());
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(picker.id(),new PropertyName("selectableDayPredicate"),value));
            assertAnalysis(before,current,"probe.dart",true);save("predicate_"+cases.size(),current);
        }
        current=baseline;
        for(String event:List.of("onDateChanged","onDisplayedMonthChanged","selectableDayPredicate")){
            var before=reopen(current,source(current));current=apply(before,new CreateEventHandler(picker.id(),new PropertyName(event),"_"+event));
            assertAnalysis(before,current,"probe.dart",true);save("event_"+cases.size(),current);
            before=reopen(current,source(current));current=apply(before,new RenameEventHandler(picker.id(),new PropertyName(event),"_renamed"+event));
            assertAnalysis(before,current,"probe.dart",true);save("renamed_"+cases.size(),current);
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        }
        for(String mode:List.of("day","year")){
            var before=reopen(current,source(current));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName("initialCalendarMode"),en("DatePickerMode",mode)));
            assertAnalysis(before,candidate,"probe.dart",true);save("mode_"+mode,candidate);
        }
        for(String field:List.of("initialDate","currentDate","onDisplayedMonthChanged","selectableDayPredicate","key")){
            var input=field.equals("initialDate")?apply(current,new SetProperty(picker.id(),new PropertyName(field),new PropertyValue.StringValue("2024-02-29"))):current;
            var before=reopen(input,source(input));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName(field),new PropertyValue.NullValue()));
            assertAnalysis(before,candidate,"probe.dart",true);save("null_"+field,candidate);
        }
        for(PropertyValue key:List.of(new PropertyValue.StringValue("reset-calendar"),ref("calendarKey"))){
            var before=reopen(current,source(current));var candidate=apply(before,new SetProperty(picker.id(),new PropertyName("key"),key));
            assertAnalysis(before,candidate,"probe.dart",true);save("key_"+cases.size(),candidate);
        }
        var dated=apply(current,new SetProperty(picker.id(),new PropertyName("initialDate"),new PropertyValue.StringValue("2024-02-29")));
        var before=reopen(dated,source(dated));dated=apply(before,new SetProperty(picker.id(),new PropertyName("currentDate"),new PropertyValue.StringValue("2024-02-15")));
        assertAnalysis(before,dated,"probe.dart",true);save("dated",dated);
        Files.writeString(project.resolve("native_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","native_test.dart","--reporter","expanded"),"native.log");
        System.out.println("CalendarDatePicker: "+(cases.size()*2+4)+" generated/native SDK cases passed; 16 unsafe source shapes rejected.");
    }
    private static PropertyValue.EnumValue en(String type,String value){return new PropertyValue.EnumValue(type,value);}
    private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
    private void save(String name,DesignerCommandSession session)throws Exception{saveCase(name,session);cases.add(name);}
    private String nativeTests(){
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\nimport 'package:calendar_date_picker_contract/sources.dart' as refs;\n");
        for(String name:cases)out.append("import 'package:calendar_date_picker_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(boolean rtl:List.of(false,true))out.append("""
              testWidgets('NAME RTL', (tester) async {
                await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:DIR,child:Scaffold(body:NAME.Sample()))));
                await tester.pumpAndSettle();
                expect(find.byType(CalendarDatePicker),findsOneWidget);
                final widget=tester.widget<CalendarDatePicker>(find.byType(CalendarDatePicker));
                expect(widget.firstDate.isAfter(widget.lastDate),isFalse);
                expect(tester.takeException(),isNull);
                await tester.pumpWidget(const SizedBox());await tester.pump();
              });
                """.replace("NAME",name).replace("RTL",Boolean.toString(rtl)).replace("DIR",rtl?"TextDirection.rtl":"TextDirection.ltr"));
        out.append("""
              testWidgets('selection and month navigation have distinct native Events', (tester) async {
                final dates=<DateTime>[],months=<DateTime>[];
                await tester.pumpWidget(MaterialApp(home:Scaffold(body:CalendarDatePicker(
                  initialDate:DateTime(2024,2,15),currentDate:DateTime(2024,2,15),firstDate:DateTime(2024),lastDate:DateTime(2025,12,31),
                  onDateChanged:dates.add,onDisplayedMonthChanged:months.add))));
                await tester.pumpAndSettle();expect(dates,isEmpty);expect(months,isEmpty);
                await tester.tap(find.text('20'));await tester.pumpAndSettle();
                expect(dates,[DateTime(2024,2,20)]);expect(months,isEmpty);
                await tester.tap(find.byTooltip('Next month'));await tester.pumpAndSettle();
                expect(months,[DateTime(2024,3,1)]);expect(dates,[DateTime(2024,2,20)]);
                expect(find.byType(DatePickerDialog),findsNothing);expect(tester.takeException(),isNull);
              });
              testWidgets('initial values are ignored for the same key and reset for a new key', (tester) async {
                Widget app(Key key,DateTime? date,DatePickerMode mode)=>MaterialApp(home:Scaffold(body:CalendarDatePicker(
                  key:key,initialDate:date,currentDate:DateTime(2024,2,15),firstDate:DateTime(2024),lastDate:DateTime(2025,12,31),
                  initialCalendarMode:mode,onDateChanged:(_){})));
                await tester.pumpWidget(app(const ValueKey('same'),DateTime(2024,2,15),DatePickerMode.day));await tester.pumpAndSettle();
                await tester.pumpWidget(app(const ValueKey('same'),DateTime(2025,1,2),DatePickerMode.year));await tester.pumpAndSettle();
                expect(find.text('February 2024'),findsOneWidget);expect(find.byType(YearPicker),findsNothing);
                await tester.pumpWidget(app(const ValueKey('new'),DateTime(2025,1,2),DatePickerMode.year));await tester.pumpAndSettle();
                expect(find.byType(YearPicker),findsOneWidget);expect(tester.takeException(),isNull);
              });
              testWidgets('nullable initial selection, predicate and project calendar run in application', (tester) async {
                refs.calendarCalls=0;int predicates=0;DateTime? selected;
                await tester.pumpWidget(MaterialApp(home:Scaffold(body:CalendarDatePicker(
                  initialDate:null,firstDate:DateTime(2024),lastDate:DateTime(2025,12,31),calendarDelegate:refs.calendar,
                  selectableDayPredicate:(date){predicates++;return date.day!=20;},onDateChanged:(date)=>selected=date))));
                await tester.pumpAndSettle();expect(refs.calendarCalls,greaterThan(0));expect(predicates,greaterThan(0));
                expect(find.text('February 2024'),findsOneWidget);
                await tester.tap(find.text('20'),warnIfMissed:false);await tester.pumpAndSettle();expect(selected,isNull);
                await tester.tap(find.text('21'));await tester.pumpAndSettle();expect(selected,DateTime(2024,2,21));
                expect(tester.takeException(),isNull);
              });
              testWidgets('year navigation reports delegate month and native selection', (tester) async {
                final months=<DateTime>[],dates=<DateTime>[];
                await tester.pumpWidget(MaterialApp(home:Scaffold(body:CalendarDatePicker(
                  initialDate:DateTime(2024,2,15),firstDate:DateTime(2024),lastDate:DateTime(2025,12,31),
                  initialCalendarMode:DatePickerMode.year,onDateChanged:dates.add,onDisplayedMonthChanged:months.add))));
                await tester.pumpAndSettle();await tester.tap(find.text('2025'));await tester.pumpAndSettle();
                expect(months,[DateTime(2025,2,1)]);expect(dates,[DateTime(2025,2,15)]);
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
                name: calendar_date_picker_contract
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
