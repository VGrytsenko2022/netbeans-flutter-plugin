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
class DataTableRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();

    @Test void dataTableCompleteDescriptorsEventsSourcesPairSaveAndNativeLayout() throws Exception {
        initialize();
        String members="// User member is preserved.\nint _userValue() => 73;\nObject get wrong=>Object();\ndynamic get unsafe=>null;\n";
        members += "bool _chosen=false; bool _ascending=true; int? _sort=0;\n"
                + "void _choose(bool? value){setState((){_chosen=value??false;});}\n"
                + "void _sortData(int column,bool ascending){setState((){_sort=column;_ascending=ascending;});}\n";
        var sources=new LinkedHashMap<String,String>();
        sources.put("width","TableColumnWidth?");sources.put("style","TextStyle?");sources.put("colors","WidgetStateProperty<Color?>?");
        sources.put("cursors","WidgetStateProperty<MouseCursor?>?");sources.put("border","TableBorder?");
        sources.put("decoration","Decoration?");sources.put("rowKey","LocalKey?");sources.put("tableKey","Key?");
        var defaults=Map.of("width","const FixedColumnWidth(150)","style","const TextStyle(fontSize:16)",
                "colors","const WidgetStatePropertyAll<Color?>(Color(0xffeeddcc))","cursors","const WidgetStatePropertyAll<MouseCursor?>(SystemMouseCursors.click)",
                "border","TableBorder.all()","decoration","const BoxDecoration(color:Color(0xffeeddcc))",
                "rowKey","const ValueKey('source-row')","tableKey","const ValueKey('source-table')");
        var values=new StringBuilder("import 'package:flutter/material.dart';\n");
        var statics=new StringBuilder("class Values {\n");
        for(var e:sources.entrySet()){
            String method="make"+Character.toUpperCase(e.getKey().charAt(0))+e.getKey().substring(1);
            values.append(e.getValue()+" get "+e.getKey()+"=>"+defaults.get(e.getKey())+";\n"+e.getValue()+" "+method+"()=>"+e.getKey()+";\n");
            statics.append("static "+e.getValue()+" get "+e.getKey()+"=>"+defaults.get(e.getKey())+";\nstatic "+e.getValue()+" "+method+"()=>"+e.getKey()+";\n");
            members+=e.getValue()+" get "+e.getKey()+"=>refs."+e.getKey()+";\n"+e.getValue()+" "+method+"()=>refs."+e.getKey()+";\n";
        }
        Files.writeString(lib.resolve("values.dart"),values.append(statics).append("}\n"));
        var catalog=BuiltInWidgetCatalog.getDefault();
        var prototype=WidgetNodePrototypeFactory.create(catalog.find(DataTableWidgetPropertySchema.TYPE).orElseThrow(),StableId.random());
        var baseline=open(group(StableId.random(),List.of()),"probe.dart",members);
        baseline=reopen(baseline,"import 'package:data_table_contract/values.dart' as refs;\n"+source(baseline));
        var current=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),0),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);save("starter",current);
        baseline=reopen(current,source(current));
        var tableId=prototype.id();var column=DataTableGrid.children(prototype,DataTableGrid.COLUMNS).getFirst();
        var row=DataTableGrid.children(prototype,DataTableGrid.ROWS).getFirst();var cell=DataTableGrid.children(row,DataTableGrid.CELLS).getFirst();
        var indexed=WidgetNodePrototypeFactory.create(catalog.find(DataTableWidgetPropertySchema.ROW_INDEX).orElseThrow(),StableId.random());
        current=apply(baseline,new AddWidget(new WidgetPlacement(tableId,DataTableGrid.ROWS,2),indexed));
        assertAnalysis(baseline,current,"probe.dart",true);save("indexed",current);baseline=reopen(current,source(current));
        var empty=WidgetNodePrototypeFactory.create(catalog.find(DataTableWidgetPropertySchema.EMPTY).orElseThrow(),StableId.random());
        var before=reopen(current,source(current));
        current=apply(before,new AddWidget(new WidgetPlacement(row.id(),DataTableGrid.CELLS,0),empty));
        assertAnalysis(before,current,"probe.dart",true);save("empty",current);
        // Native event signatures, owned member preservation, stable reopen and two-argument sorting.
        current=baseline;
        for(var target:List.of(prototype,column,row,indexed,cell))for(String callback:DataTableWidgetPropertySchema.callbacks(target.type()).keySet()){
            before=reopen(current,source(current));
            current=apply(before,new CreateEventHandler(target.id(),new PropertyName(callback),"_handle"+cases.size()));
            assertAnalysis(before,current,"probe.dart",true);save("event_"+cases.size(),current);
        }
        before=reopen(current,source(current));
        var sortEvent=column.id();
        current=apply(before,new RenameEventHandler(sortEvent,new PropertyName("onSort"),"_sortRows"));
        assertAnalysis(before,current,"probe.dart",true);save("renamed_sort",current);
        for(var target:List.of(new Object[]{tableId,"dataTextStyle","style"},new Object[]{tableId,"headingTextStyle","style"},
                new Object[]{tableId,"dataRowColor","colors"},new Object[]{tableId,"headingRowColor","colors"},
                new Object[]{tableId,"border","border"},new Object[]{tableId,"decoration","decoration"},new Object[]{tableId,"key","tableKey"},
                new Object[]{column.id(),"columnWidth","width"},new Object[]{column.id(),"mouseCursor","cursors"},
                new Object[]{row.id(),"color","colors"},new Object[]{row.id(),"mouseCursor","cursors"},new Object[]{row.id(),"key","rowKey"})) {
            for(int sourceKind:List.of(0,1,2))for(boolean factory:List.of(false,true)){
                boolean member=sourceKind==1;String field=(String)target[2],method="make"+Character.toUpperCase(field.charAt(0))+field.substring(1);
                var reference=new PropertyValue.DartObjectReferenceValue(sourceKind==2?Optional.empty():Optional.of("package:data_table_contract/values.dart"),
                        member?"Values":factory?method:field,member?Optional.of(factory?method:field):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                before=reopen(baseline,source(baseline));current=apply(before,new SetProperty((StableId)target[0],new PropertyName((String)target[1]),reference));
                assertAnalysis(before,current,"probe.dart",true);save("source_"+cases.size(),current);
            }
            for(String bad:List.of("wrong","unsafe")){
                before=reopen(baseline,source(baseline));
                current=apply(before,new SetProperty((StableId)target[0],new PropertyName((String)target[1]),
                        new PropertyValue.DartObjectReferenceValue(Optional.empty(),bad,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty())));
                assertAnalysis(before,current,"probe.dart",false);
            }
        }
        current=baseline;
        for(var op:List.of(TableGrid.Operation.ADD_COLUMN,TableGrid.Operation.MOVE_COLUMN,TableGrid.Operation.REMOVE_COLUMN,TableGrid.Operation.MOVE_ROW)){
            before=reopen(current,source(current));
            current=apply(before,new EditDataTableGrid(find(before.current().document().root(),tableId),op,op==TableGrid.Operation.MOVE_COLUMN?2:1,0,StableId.random()));
            assertAnalysis(before,current,"probe.dart",true);save("grid_"+op.name().toLowerCase(Locale.ROOT),current);
            assertArrayEquals(before.current().fdBytes(),current.undo().session().current().fdBytes());
        }
        before=reopen(baseline,source(baseline));current=before;
        for(var binding:List.of(new Object[]{tableId,"sortColumnIndex","_sort",StateBinding.Type.NULLABLE_INT},
                new Object[]{tableId,"sortAscending","_ascending",StateBinding.Type.BOOL},
                new Object[]{row.id(),"selected","_chosen",StateBinding.Type.BOOL},
                new Object[]{indexed.id(),"selected","_chosen",StateBinding.Type.BOOL})) {
            current=apply(current,new BindPropertyToState((StableId)binding[0],new PropertyName((String)binding[1]),
                    new StatePropertyBinding((String)binding[2],(StateBinding.Type)binding[3],Optional.empty(),StatePropertyBinding.Transform.DIRECT)));
        }
        current=apply(current,new SetProperty(column.id(),new PropertyName("onSort"),localReference("_sortData")));
        current=apply(current,new SetProperty(row.id(),new PropertyName("onSelectChanged"),localReference("_choose")));
        assertAnalysis(before,current,"probe.dart",true);save("controlled",current);
        for(String width:List.of("fixed(180)","flex(1)","fraction(0.4)","intrinsic(1)","min(fixed(160),intrinsic())","max(fixed(100),intrinsic())")) {
            before=reopen(baseline,source(baseline));current=apply(before,new SetProperty(column.id(),new PropertyName("columnWidth"),new PropertyValue.StringValue(width)));
            assertAnalysis(before,current,"probe.dart",true);save("width_"+cases.size(),current);
        }
        before=reopen(baseline,source(baseline));
        var local=new LinkedHashMap<String,PropertyValue>();
        for(String name:List.of("dataTextStyle","headingTextStyle","dataRowColor","headingRowColor"))local.put(name,new PropertyValue.StringValue("local"));
        local.put("border",new PropertyValue.StringValue("all"));
        local.put("dataTextStyleFontSize",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(15)));
        local.put("headingTextStyleFontSize",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(16)));
        local.put("dataRowColorSelected",new PropertyValue.NullValue());
        local.put("headingRowColorDefault",new PropertyValue.ColorValue(0xffeeddccL));
        local.put("dataRowMinHeight",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(48)));
        local.put("dataRowMaxHeight",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(70)));
        local.put("headingRowHeight",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(56)));
        local.put("horizontalMargin",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(8)));
        local.put("columnSpacing",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(12)));
        local.put("checkboxHorizontalMargin",new PropertyValue.DoubleValue(java.math.BigDecimal.valueOf(8)));
        local.put("dividerThickness",new PropertyValue.DoubleValue(java.math.BigDecimal.ONE));
        local.put("clipBehavior",new PropertyValue.EnumValue("Clip","hardEdge"));
        current=apply(before,new PatchProperties(tableId,local.entrySet().stream().map(e->(PatchProperties.Patch)new PatchProperties.SetPatch(new PropertyName(e.getKey()),e.getValue())).toList()));
        assertAnalysis(before,current,"probe.dart",true);save("local",current);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("data_table_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
        String log=Files.readString(project.resolve("flutter-test.log"));
        int expected=cases.size()*2;assertTrue(log.contains("+"+expected+": All tests passed!"),log);
        System.out.println("DataTable: "+expected+" generated/native SDK cases passed.");
    }
    private static PropertyValue.DartObjectReferenceValue localReference(String name) {
        return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
    }
    private static WidgetNode find(WidgetNode node,StableId id) {
        if(node.id().equals(id))return node;
        for(var slot:node.slots().values()) {
            var children=slot instanceof WidgetSlot.ListSlot l?l.children():((WidgetSlot.SingleSlot)slot).child().stream().toList();
            for(var child:children){var found=find(child,id);if(found!=null)return found;}
        }
        return null;
    }
    private void save(String name,DesignerCommandSession session)throws Exception {saveCase(name,session);cases.add(name);}
    private String nativeTests() {
        var out=new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        for(String name:cases)out.append("import 'package:data_table_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String direction:List.of("ltr","rtl"))
            out.append("testWidgets('").append(name).append(" ").append(direction).append("',(tester)async{")
                .append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:TextDirection.").append(direction)
                .append(",child:Scaffold(body:").append(name).append(".Sample()))));")
                .append("expect(find.byType(DataTable),findsOneWidget);expect(tester.takeException(),isNull);")
                .append(name.equals("controlled") ?
                        "final first=tester.widget<DataTable>(find.byType(DataTable));"
                        + "expect(first.rows.first.selected,isFalse);first.rows.first.onSelectChanged!(true);first.columns.first.onSort!(1,false);"
                        + "await tester.pump();final updated=tester.widget<DataTable>(find.byType(DataTable));"
                        + "expect(updated.rows.first.selected,isTrue);expect(updated.rows.last.selected,isTrue);"
                        + "expect(updated.sortColumnIndex,1);expect(updated.sortAscending,isFalse);expect(tester.takeException(),isNull);" : "")
                .append("});\n");
        return out.append("}").toString();
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
                name: data_table_contract
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
