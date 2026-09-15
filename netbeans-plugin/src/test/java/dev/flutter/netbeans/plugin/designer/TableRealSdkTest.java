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
class TableRealSdkTest {
    @TempDir(cleanup=CleanupMode.ON_SUCCESS) Path project;
    private Path sdk,flutter,dart,lib;
    private static final StableId NESTED_ID=StableId.random();
    private static final SlotName CHILDREN=new SlotName("children");
    private final List<String> cases=new ArrayList<>();


    @Test void completeTableConstructorsSourcesSaveReopenAndNativeLayout() throws Exception {
        initialize();
        Files.writeString(lib.resolve("values.dart"),"""
                import 'package:flutter/widgets.dart';
                final TableColumnWidth width = const FixedColumnWidth(70);
                final Map<int,TableColumnWidth> widths = {0:const FixedColumnWidth(70)};
                final TableBorder border = TableBorder.all(color:const Color(0xff112233),width:2);
                const Decoration decoration = BoxDecoration(color:Color(0xffeeddaa));
                const LocalKey rowKey = ValueKey('source-row');
                const Key cellKey = ValueKey('source-cell');
                TableColumnWidth makeWidth()=>width;
                Map<int,TableColumnWidth> makeWidths()=>widths;
                TableBorder makeBorder()=>border;
                Decoration makeDecoration()=>decoration;
                LocalKey makeRowKey()=>rowKey;
                Key makeCellKey()=>cellKey;
                class Values {
                  static TableColumnWidth get width=>const FixedColumnWidth(70);
                  static Map<int,TableColumnWidth> get widths=>{0:const FixedColumnWidth(70)};
                  static TableBorder get border=>TableBorder.all();
                  static Decoration get decoration=>const BoxDecoration();
                  static LocalKey get rowKey=>const ValueKey('member-row');
                  static Key get cellKey=>const ValueKey('member-cell');
                  static TableColumnWidth makeWidth()=>width;
                  static Map<int,TableColumnWidth> makeWidths()=>widths;
                  static TableBorder makeBorder()=>border;
                  static Decoration makeDecoration()=>decoration;
                  static LocalKey makeRowKey()=>rowKey;
                  static Key makeCellKey()=>cellKey;
                }
                """);
        String members="""
                // User member is preserved.
                int _userValue() => 73;
                TableColumnWidth get width=>refs.width;
                Map<int,TableColumnWidth> get widths=>refs.widths;
                TableBorder? get border=>refs.border;
                Decoration? get decoration=>refs.decoration;
                LocalKey? get rowKey=>refs.rowKey;
                Key? get cellKey=>refs.cellKey;
                TableColumnWidth makeWidth()=>refs.width;
                Map<int,TableColumnWidth>? makeWidths()=>refs.widths;
                TableBorder? makeBorder()=>refs.border;
                Decoration? makeDecoration()=>refs.decoration;
                LocalKey? makeRowKey()=>refs.rowKey;
                Key? makeCellKey()=>refs.cellKey;
                Object get wrong=>Object();
                dynamic get unsafe=>refs.width;
                TableColumnWidth? get nullableWidth=>null;
                """;
        var baseline=open(group(StableId.random(),List.of()),"probe.dart",members);
        baseline=reopen(baseline,"import 'package:table_contract/values.dart' as refs;\n"+source(baseline));
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(TableWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
        var current=apply(baseline,new AddWidget(place(baseline.current().document().root().id(),0),prototype));
        assertAnalysis(baseline,current,"probe.dart",true); save("starter",current);
        var row=TableGrid.children(prototype).getFirst();
        var cell=TableGrid.children(row).getFirst();
        var cellId=StableId.random();
        var wrapper=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(TableWidgetPropertySchema.CELL).orElseThrow(),cellId);
        var before=reopen(current,source(current));
        current=apply(before,new WrapWidget(cell.id(),wrapper,new SlotName("child"),0));
        assertAnalysis(before,current,"probe.dart",true); save("cell",current);
        for(String width:List.of("fixed(0)","flex(2)","fraction(0.4)","intrinsic()","intrinsic(1)","min(fixed(30),max(fixed(10),intrinsic()))")) {
            before=reopen(current,source(current));
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName("defaultColumnWidth"),new PropertyValue.StringValue(width)));
            assertAnalysis(before,current,"probe.dart",true); save("width_"+cases.size(),current);
        }
        before=reopen(current,source(current));
        current=apply(before,new ResetProperty(NESTED_ID,new PropertyName("defaultColumnWidth")));
        for(String mode:List.of("all","symmetric","custom")) {
            before=reopen(current,source(current));
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName("border"),new PropertyValue.StringValue(mode)));
            assertAnalysis(before,current,"probe.dart",true);save("border_"+mode,current);
        }
        for(String alignment:TableWidgetPropertySchema.ALIGNMENTS) {
            before=reopen(current,source(current));
            current=apply(before,new PatchProperties(NESTED_ID,List.of(
                    new PatchProperties.SetPatch(new PropertyName("textBaseline"),new PropertyValue.EnumValue("TextBaseline","alphabetic")),
                    new PatchProperties.SetPatch(new PropertyName("defaultVerticalAlignment"),new PropertyValue.EnumValue("TableCellVerticalAlignment",alignment)))));
            assertAnalysis(before,current,"probe.dart",true);save("align_"+alignment,current);
        }
        var tableNode=TableGrid.withChildren(prototype,List.of()); // fresh fence below comes from the session, not this prototype.
        for(var op:List.of(TableGrid.Operation.ADD_COLUMN,TableGrid.Operation.MOVE_COLUMN,TableGrid.Operation.REMOVE_COLUMN,TableGrid.Operation.ADD_ROW)) {
            before=reopen(current,source(current));
            tableNode=find(before.current().document().root(),NESTED_ID);
            current=apply(before,new EditTableGrid(tableNode,op,op==TableGrid.Operation.MOVE_COLUMN?2:1,0,StableId.random()));
            assertAnalysis(before,current,"probe.dart",true);
            assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
            assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
            save("grid_"+op.name().toLowerCase(Locale.ROOT),current);
        }
        // Use a fresh starter for source checks so deleted columns do not remove the TableCell target.
        baseline=open(group(StableId.random(),List.of(prototype)),"probe.dart",members);
        baseline=reopen(baseline,"import 'package:table_contract/values.dart' as refs;\n"+source(baseline));
        baseline=apply(baseline,new WrapWidget(cell.id(),wrapper,new SlotName("child"),0));
        for(var target:List.of(new Object[]{NESTED_ID,"defaultColumnWidth","width"},
                new Object[]{NESTED_ID,"columnWidths","widths"},new Object[]{NESTED_ID,"border","border"},
                new Object[]{row.id(),"decoration","decoration"},new Object[]{row.id(),"key","rowKey"},new Object[]{cellId,"key","cellKey"})) {
            for(int sourceKind:List.of(0,1,2))for(boolean factory:List.of(false,true)) {
                boolean member=sourceKind==1;
                String field=(String)target[2],method="make"+Character.toUpperCase(field.charAt(0))+field.substring(1);
                var reference=new PropertyValue.DartObjectReferenceValue(sourceKind==2?Optional.empty():Optional.of("package:table_contract/values.dart"),
                        member?"Values":factory?method:field,member?Optional.of(factory?method:field):Optional.empty(),
                        factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                        factory?Optional.of(false):Optional.empty());
                before=reopen(baseline,source(baseline));
                current=apply(before,new SetProperty((StableId)target[0],new PropertyName((String)target[1]),reference));
                assertAnalysis(before,current,"probe.dart",true);save("source_"+cases.size(),current);
            }
        }
        for(String field:List.of("defaultColumnWidth","columnWidths","border")) for(String bad:List.of("wrong","unsafe","nullableWidth")) {
            before=reopen(baseline,source(baseline));
            var reference=new PropertyValue.DartObjectReferenceValue(Optional.empty(),bad,Optional.empty(),
                    PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
            current=apply(before,new SetProperty(NESTED_ID,new PropertyName(field),reference));
            assertAnalysis(before,current,"probe.dart",false);
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("table_test.dart"),nativeTests());
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
        String log=Files.readString(project.resolve("flutter-test.log"));
        int expected=cases.size()*2;
        assertTrue(log.contains("+"+expected+": All tests passed!"),log);
        System.out.println("Table: "+expected+" generated/native SDK cases passed.");
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
        for(String name:cases)out.append("import 'package:table_contract/").append(name).append(".dart' as ").append(name).append(";\n");
        out.append("void main(){\n");
        for(String name:cases)for(String direction:List.of("ltr","rtl"))
            out.append("testWidgets('").append(name).append(" ").append(direction).append("',(tester)async{")
                .append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:TextDirection.").append(direction)
                .append(",child:Scaffold(body:").append(name).append(".Sample()))));")
                .append("expect(find.byType(Table),findsOneWidget);expect(tester.takeException(),isNull);});\n");
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
                name: table_contract
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
