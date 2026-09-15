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
class PaginatedDataTableRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();

    @Test void paginatedSourceLifetimeEventsStateAndNativePages() throws Exception {
        initialize();
        Files.writeString(lib.resolve("sources.dart"), """
                import 'package:flutter/material.dart';
                class ProjectSource extends DataTableSource {
                  int count=24, adds=0, removes=0;
                  bool approximate=false, loading=false, selected=false, descending=false;
                  @override void addListener(VoidCallback value){adds++;super.addListener(value);}
                  @override void removeListener(VoidCallback value){removes++;super.removeListener(value);}
                  void change({int? rows,bool? wait,bool? estimate,bool? choose,bool? reverse}) {
                    count=rows??count;loading=wait??loading;approximate=estimate??approximate;
                    selected=choose??selected;descending=reverse??descending;notifyListeners();
                  }
                  @override DataRow? getRow(int index) => loading ? null : index>=count ? null : DataRow.byIndex(
                    index:index,selected:selected,onSelectChanged:(value)=>change(choose:value??false),
                    cells:[DataCell(Text('Row ${descending?count-index-1:index}')),DataCell(Text('Value $index'))]);
                  @override int get rowCount=>count;
                  @override bool get isRowCountApproximate=>approximate;
                  @override int get selectedRowCount=>selected?count:0;
                }
                final shared=ProjectSource();
                DataTableSource get source=>shared;
                DataTableSource createSource()=>shared;
                class Sources {
                  static DataTableSource get source=>shared;
                  static DataTableSource createSource()=>shared;
                }
                """);
        String members="""
                // User member is preserved.
                int _userValue() => 73;
                Object get wrong=>Object();
                dynamic get unsafe=>refs.shared;
                DataTableSource? get nullable=>null;
                DataTableSource get local=>refs.shared;
                DataTableSource createSource()=>refs.shared;
                static DataTableSource get source=>refs.shared;
                static DataTableSource make()=>refs.shared;
                int _pageSize=10; int? _sort=0; bool _ascending=true;
                void _pageSizeChanged(int? value){if(value!=null)setState(()=>_pageSize=value);}
                void _sortChanged(int column,bool ascending){refs.shared.change(reverse:!ascending);setState((){_sort=column;_ascending=ascending;});}
                """;
        var root=group(StableId.random(),List.of());
        var baseline=open(root,"probe.dart",members);
        baseline=reopen(baseline,source(baseline).replace("class Sample extends","import 'package:paginated_data_table_contract/sources.dart' as refs;\nclass Sample extends"));
        var catalog=BuiltInWidgetCatalog.getDefault();
        var table=WidgetNodePrototypeFactory.create(catalog.find(PaginatedDataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var current=apply(baseline,new AddWidget(place(root.id(),0),table));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        baseline=reopen(current,source(current));
        for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)) {
            var ref=new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:paginated_data_table_contract/sources.dart"):Optional.empty(),
                    member?(imported?"Sources":"Storage"):(imported?(factory?"createSource":"source"):(factory?"createSource":"local")),
                    member?Optional.of(factory?(imported?"createSource":"make"):"source"):Optional.empty(),
                    factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                    factory?Optional.of(false):Optional.empty());
            var before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(table.id(),new PropertyName("source"),ref));
            assertAnalysis(before,current,"probe.dart",true);save("source_"+cases.size(),current);
        }
        for(String name:List.of("wrong","unsafe","nullable")) {
            var before=reopen(baseline,source(baseline));
            var bad=apply(before,new SetProperty(table.id(),new PropertyName("source"),ref(name)));
            assertAnalysis(before,bad,"probe.dart",false);
        }
        current=baseline;
        for(String event:List.of("onPageChanged","onRowsPerPageChanged","onSelectAll")) {
            var before=reopen(current,source(current));current=apply(before,new CreateEventHandler(table.id(),new PropertyName(event),"_handle"+cases.size()));
            assertAnalysis(before,current,"probe.dart",true);save("event_"+cases.size(),current);
        }
        var before=reopen(baseline,source(baseline));
        current=apply(before,new SetProperty(table.id(),new PropertyName("source"),ref("local")));
        var values=new LinkedHashMap<String,PropertyValue>();
        values.put("rowsPerPage",new PropertyValue.IntegerValue(java.math.BigInteger.valueOf(5)));
        values.put("availableRowsPerPage",new PropertyValue.StringValue("5, 10, 20"));
        values.put("initialFirstRowIndex",new PropertyValue.IntegerValue(java.math.BigInteger.TEN));
        values.put("showFirstLastButtons",new PropertyValue.BooleanValue(true));
        values.put("showEmptyRows",new PropertyValue.BooleanValue(false));
        values.put("headingRowColor",new PropertyValue.StringValue("local"));
        values.put("headingRowColorDefault",new PropertyValue.ColorValue(0xffeeddccL));
        values.put("headingRowHeight",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(44)));
        values.put("dataRowMinHeight",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(30)));
        values.put("dataRowMaxHeight",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(40)));
        values.put("horizontalMargin",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(8)));
        values.put("columnSpacing",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(12)));
        values.put("checkboxHorizontalMargin",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(8)));
        values.put("dividerThickness",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(2)));
        values.put("dragStartBehavior",new PropertyValue.EnumValue("DragStartBehavior","down"));
        values.put("arrowHeadColor",new PropertyValue.ColorValue(0xff123456L));
        values.put("primary",new PropertyValue.BooleanValue(false));
        values.put("showCheckboxColumn",new PropertyValue.BooleanValue(true));
        values.put("onSelectAll",new PropertyValue.StringValue("noop"));
        current=apply(current,new PatchProperties(table.id(),values.entrySet().stream().map(e->(PatchProperties.Patch)new PatchProperties.SetPatch(new PropertyName(e.getKey()),e.getValue())).toList()));
        current=apply(current,new AddWidget(new WidgetPlacement(table.id(),new SlotName("header"),0),adapter("Header",40)));
        current=apply(current,new AddWidget(new WidgetPlacement(table.id(),new SlotName("actions"),0),adapter("Action",30)));
        assertAnalysis(before,current,"probe.dart",true);save("local",current);
        before=reopen(baseline,source(baseline));
        current=apply(before,new SetProperty(table.id(),new PropertyName("source"),ref("local")));
        for(Object[] binding:List.of(new Object[]{"rowsPerPage","_pageSize",StateBinding.Type.INT},
                new Object[]{"sortColumnIndex","_sort",StateBinding.Type.NULLABLE_INT},
                new Object[]{"sortAscending","_ascending",StateBinding.Type.BOOL})) {
            current=apply(current,new BindPropertyToState(table.id(),new PropertyName((String)binding[0]),
                new StatePropertyBinding((String)binding[1],(StateBinding.Type)binding[2],Optional.empty(),StatePropertyBinding.Transform.DIRECT)));
        }
        current=apply(current,new SetProperty(table.id(),new PropertyName("onRowsPerPageChanged"),ref("_pageSizeChanged")));
        var column=DataTableGrid.children(table,DataTableGrid.COLUMNS).getFirst();
        current=apply(current,new SetProperty(column.id(),new PropertyName("onSort"),ref("_sortChanged")));
        assertAnalysis(before,current,"probe.dart",true);save("controlled",current);
        for(var operation:List.of(TableGrid.Operation.ADD_COLUMN,TableGrid.Operation.MOVE_COLUMN,TableGrid.Operation.REMOVE_COLUMN)) {
            before=reopen(baseline,source(baseline));current=apply(before,new EditDataTableGrid(table,operation,operation==TableGrid.Operation.ADD_COLUMN?2:0,1,StableId.random()));
            assertAnalysis(before,current,"probe.dart",true);save("grid_"+cases.size(),current);
        }
        before=reopen(baseline,source(baseline));
        current=apply(before,new SetProperty(table.id(),new PropertyName("dataRowHeight"),new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(32))));
        assertAnalysis(before,current,"probe.dart",true);save("legacy_height",current);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("paginated_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
        var log=Files.readString(project.resolve("flutter-test.log"));int expected=cases.size()*2+1;
        assertTrue(log.contains("+"+expected+": All tests passed!"),log);
        System.out.println("PaginatedDataTable: "+expected+" generated/native SDK cases passed.");
    }
    private static PropertyValue.DartObjectReferenceValue ref(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
    }
    private void save(String name,DesignerCommandSession session)throws Exception {saveCase(name,session);cases.add(name);}
    private String nativeTests() {
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\nimport 'package:paginated_data_table_contract/sources.dart' as refs;\n");
        for(String name:cases)out.append("import 'package:paginated_data_table_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String direction:List.of("ltr","rtl")) {
            out.append("testWidgets('").append(name).append(" ").append(direction).append("',(tester)async{")
                .append("refs.shared.change(rows:24,wait:false,estimate:false,choose:false,reverse:false);")
                .append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:TextDirection.").append(direction)
                .append(",child:Scaffold(body:SingleChildScrollView(child:").append(name).append(".Sample())))));")
                .append("expect(find.byType(PaginatedDataTable),findsOneWidget);expect(tester.takeException(),isNull);")
                .append("final native=tester.widget<PaginatedDataTable>(find.byType(PaginatedDataTable));final source=native.source;");
            if(name.equals("controlled"))out.append("""
                    expect(native.rowsPerPage,10);native.onRowsPerPageChanged!(20);
                    await tester.pump();final updated=tester.widget<PaginatedDataTable>(find.byType(PaginatedDataTable));
                    expect(updated.rowsPerPage,20);expect(updated.source,same(source));
                    updated.columns.first.onSort!(1,false);await tester.pump();
                    expect(tester.widget<PaginatedDataTable>(find.byType(PaginatedDataTable)).sortColumnIndex,1);
                    expect(tester.widget<PaginatedDataTable>(find.byType(PaginatedDataTable)).sortAscending,isFalse);
                    expect(refs.shared.descending,isTrue);
                    """);
            if(name.equals("local"))out.append("expect(native.rowsPerPage,5);expect(native.header,isNotNull);expect(native.actions!.length,1);expect(native.headingRowHeight,44);expect(native.headingRowColor!.resolve({}),const Color(0xffeeddcc));");
            out.append("expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());expect(source.hasListeners,isFalse);});\n");
        }
        out.append("""
                testWidgets('lazy paging, notifications, approximate loading, selection and listener release',(tester)async{
                  refs.shared.change(rows:24,wait:false,estimate:false,choose:false,reverse:false);
                  await tester.pumpWidget(const MaterialApp(home:Scaffold(body:SingleChildScrollView(child:controlled.Sample()))));
                  var state=tester.state<PaginatedDataTableState>(find.byType(PaginatedDataTable));
                  state.pageTo(10);await tester.pump();
                  expect(find.text('Row 10'),findsOneWidget);
                  refs.shared.change(rows:32,wait:true,estimate:true);await tester.pump();
                  // Flutter 3.44.8 puts one progress indicator in each non-numeric column.
                  expect(find.byType(CircularProgressIndicator),findsNWidgets(2));
                  expect(tester.widget<PaginatedDataTable>(find.byType(PaginatedDataTable)).source.isRowCountApproximate,isTrue);
                  refs.shared.change(wait:false,estimate:false,choose:true);await tester.pump();
                  expect(tester.widget<PaginatedDataTable>(find.byType(PaginatedDataTable)).source.selectedRowCount,32);
                  expect(find.text('Row 10'),findsOneWidget);
                  expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());
                  expect(refs.shared.hasListeners,isFalse);expect(refs.shared.adds,refs.shared.removes);
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
                name: paginated_data_table_contract
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
