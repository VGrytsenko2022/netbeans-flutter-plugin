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

@EnabledIfSystemProperty(named="flutter.events.sdk", matches=".+")
class ValueListenableBuilderRealSdkTest {
    @TempDir Path project;
    private Path sdk, flutter, dart, lib;
    private static final StableId BUILDER_ID=StableId.random(), GROUP_ID=StableId.random();
    @Test void boxSourceProofHistoryAndNativeSubscriptionLifecycle() throws Exception { verify(false); }
    @Test void sliverSourceProofHistoryAndNativeSubscriptionLifecycle() throws Exception { verify(true); }
    private void verify(boolean sliver) throws Exception {
        initialize();
        String fallback=sliver?"const SliverToBoxAdapter()":"const SizedBox.shrink()";
        String visible=sliver?"SliverToBoxAdapter(child:Text('value '+value.toString()))":"Text('value '+value.toString())";
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                import 'package:flutter/foundation.dart';
                class TrackedSource extends ValueNotifier<double> {
                  TrackedSource():super(0.0);
                  int adds=0, removes=0, disposals=0;
                  @override void addListener(VoidCallback listener) { adds++; super.addListener(listener); }
                  @override void removeListener(VoidCallback listener) { removes++; super.removeListener(listener); }
                  @override void dispose() { disposals++; super.dispose(); }
                  void change() { value++; }
                }
                class Sources {
                  static final instance=TrackedSource();
                  static ValueListenable<double> get source => instance;
                  static ValueListenable<double> create() => instance;
                }
                Widget externalBuilder(BuildContext context, double value, Widget? child) => %s;
                class Builders {
                  static ValueWidgetBuilder<double> get responsive => externalBuilder;
                  static ValueWidgetBuilder<double> create() => responsive;
                }
                """.formatted(visible));
        var sibling=new WidgetNode(StableId.random(),new WidgetTypeId(sliver?"flutter.widgets.SliverToBoxAdapter":"flutter.widgets.SizedBox"),Map.of(),Map.of());
        var children=new SlotName(sliver?"slivers":"children");
        var group=new WidgetNode(GROUP_ID,new WidgetTypeId(sliver?"flutter.widgets.CustomScrollView":"flutter.widgets.Column"),Map.of(),
                Map.of(children,new WidgetSlot.ListSlot(List.of(sibling))));
        var baseline=open(group,"probe.dart","""
                // User member is preserved.
                int _userValue() => 73;
                ValueListenable<double> get _source => const AlwaysStoppedAnimation<double>(0.0);
                ValueListenable<double> _sourceFactory() => _source;
                ValueWidgetBuilder<double> get _responsive => (context, value, child) => child ?? %s;
                ValueWidgetBuilder<double> _factory() => _responsive;
                Widget _direct(BuildContext context, double value, Object? child) => %s;
                WidgetBuilder get _wrongSignature => (context) => const SizedBox.shrink();
                Widget _nonNullableChild(BuildContext context, double value, Widget child) => child;
                ValueWidgetBuilder<double>? get _nullable => null;
                dynamic _dynamicResult(BuildContext context, double value, Widget? child) => %s;
                Widget? _nullableResult(BuildContext context, double value, Widget? child) => null;
                dynamic get _dynamicReference => _responsive;
                ValueListenable<double>? get _nullableSource => null;
                dynamic get _dynamicSource => _source;
                int get _wrongSource => 7;
                Widget get _wrongSourceObject => const SizedBox.shrink();
                ValueListenable<String> get _stringSource => ValueNotifier<String>('wrong');
                ValueListenable<double?> get _nullableValueSource => ValueNotifier<double?>(null);
                ValueListenable<dynamic> get _rawValueSource => ValueNotifier<dynamic>(0.0);
                Widget _wrongValueArgument(BuildContext context, String value, Widget? child) => const SizedBox.shrink();
                """.formatted(fallback,fallback,fallback));
        var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(sliver?
                ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE:ValueListenableBuilderWidgetPropertySchema.TYPE).orElseThrow(),BUILDER_ID);
        var current=apply(baseline,new AddWidget(new WidgetPlacement(GROUP_ID,children,1),prototype));
        assertAnalysis(baseline,current,"probe.dart",true);saveCase("empty",current);
        assertArrayEquals(baseline.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
        assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
        WidgetNode child=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),
                Map.of(new PropertyName("data"),new PropertyValue.StringValue("prebuilt child")),Map.of());
        if(sliver)child=new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),Map.of(),
                Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child)));
        var beforeChild=reopen(current,source(current));
        current=apply(beforeChild,new AddWidget(new WidgetPlacement(BUILDER_ID,new SlotName("child"),0),child));
        assertAnalysis(beforeChild,current,"probe.dart",true);saveCase("child",current);
        for(String property:List.of("valueListenable","builder"))for(DesignerCommand invalid:List.of(
                new ResetProperty(BUILDER_ID,new PropertyName(property)),
                new SetProperty(BUILDER_ID,new PropertyName(property),new PropertyValue.NullValue()))) {
            var rejected=current.apply(invalid);assertFalse(rejected.changed());
            assertEquals(current.current().fdSnapshot(),rejected.session().current().fdSnapshot());
            assertArrayEquals(current.current().dartCandidateBytes(),rejected.session().current().dartCandidateBytes());
        }
        for(String property:List.of("valueListenable","builder")) {
            for(String name:property.equals("valueListenable")?List.of("_source","_sourceFactory"):List.of("_responsive","_factory","_direct")) {
                var before=reopen(current,source(current));
                current=apply(before,new SetProperty(BUILDER_ID,new PropertyName(property),ref(name,name.endsWith("Factory")||name.equals("_factory"),null,false)));
                assertAnalysis(before,current,"probe.dart",true);saveCase(name.substring(1),current);
                assertArrayEquals(before.current().dartCandidateBytes(),current.undo().session().current().dartCandidateBytes());
                assertArrayEquals(current.current().dartCandidateBytes(),current.undo().session().redo().session().current().dartCandidateBytes());
            }
        }
        for(String bad:List.of("_wrongSignature","_wrongValueArgument","_nonNullableChild","_nullable","_dynamicResult","_dynamicReference","_nullableResult")) {
            var before=reopen(current,source(current));
            assertAnalysis(before,apply(before,new SetProperty(BUILDER_ID,new PropertyName("builder"),ref(bad,false,null,false))),"probe.dart",false);
        }
        for(String bad:List.of("_nullableSource","_dynamicSource","_wrongSource","_wrongSourceObject","_sourceFactory","_stringSource","_nullableValueSource","_rawValueSource")) {
            var before=reopen(current,source(current));
            assertAnalysis(before,apply(before,new SetProperty(BUILDER_ID,new PropertyName("valueListenable"),ref(bad,false,null,false))),"probe.dart",false);
        }
        for(boolean factory:List.of(false,true)) {
            var before=reopen(current,source(current));
            current=apply(before,new SetProperty(BUILDER_ID,new PropertyName("builder"),ref("Builders",factory,factory?"create":"responsive",true)));
            current=apply(current,new SetProperty(BUILDER_ID,new PropertyName("valueListenable"),ref("Sources",factory,factory?"create":"source",true)));
            assertAnalysis(before,current,"probe.dart",true);saveCase(factory?"external_factory":"external_getter",current);
        }
        var moved=apply(reopen(current,source(current)),new MoveWidget(BUILDER_ID,new WidgetPlacement(GROUP_ID,children,0)));
        assertAnalysis(current,moved,"probe.dart",true);saveCase("moved",moved);
        assertArrayEquals(current.current().dartCandidateBytes(),moved.undo().session().current().dartCandidateBytes());
        var reset=apply(reopen(moved,source(moved)),new SetProperty(BUILDER_ID,new PropertyName("builder"),new PropertyValue.StringValue("child")));
        reset=apply(reset,new SetProperty(BUILDER_ID,new PropertyName("valueListenable"),new PropertyValue.StringValue("constant")));
        assertAnalysis(moved,reset,"probe.dart",true);saveCase("reset",reset);
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("listenable_test.dart"), """
                import 'package:flutter/material.dart';
                import 'package:flutter/foundation.dart';
                import 'package:flutter_test/flutter_test.dart';
                import 'package:value_listenable_builder_contract/builders.dart';
                import '../lib/empty.dart' as empty;
                import '../lib/child.dart' as child_case;
                import '../lib/responsive.dart' as responsive;
                import '../lib/factory.dart' as factory_case;
                import '../lib/direct.dart' as direct;
                import '../lib/source.dart' as source_case;
                import '../lib/sourceFactory.dart' as source_factory;
                import '../lib/external_getter.dart' as external_getter;
                import '../lib/external_factory.dart' as external_factory;
                import '../lib/reset.dart' as reset;
                import '../lib/moved.dart' as moved;
                const sliver=%s;
                Finder native() => find.byWidgetPredicate((w) => w is ValueListenableBuilder<double> && (w.valueListenable is AlwaysStoppedAnimation<double> || w.valueListenable is TrackedSource),skipOffstage:false);
                Widget host(Widget widget,{bool rtl=false,bool dark=false}) => MaterialApp(
                  theme:ThemeData(brightness:dark?Brightness.dark:Brightness.light),
                  home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,
                    child:sliver?CustomScrollView(slivers:[widget]):Center(child:widget)));
                class CountChild extends StatelessWidget {
                  const CountChild(this.built);
                  final VoidCallback built;
                  @override Widget build(BuildContext context) { built(); return const SizedBox(width:30,height:20); }
                }
                class TrackedController extends AnimationController {
                  TrackedController(TickerProvider vsync):super(vsync:vsync,duration:const Duration(seconds:1));
                  int adds=0,removes=0,disposals=0;
                  @override void addListener(VoidCallback listener) { adds++;super.addListener(listener); }
                  @override void removeListener(VoidCallback listener) { removes++;super.removeListener(listener); }
                  @override void dispose() { disposals++;super.dispose(); }
                }
                void main() {
                  final cases=<String,Widget>{
                    'empty':const empty.Sample(),'child':const child_case.Sample(),'reset':const reset.Sample(),
                    'responsive':const responsive.Sample(),'factory':const factory_case.Sample(),
                    'direct':const direct.Sample(),'source':const source_case.Sample(),'sourceFactory':const source_factory.Sample(),
                    'external_getter':const external_getter.Sample(),'external_factory':const external_factory.Sample(),'moved':const moved.Sample()};
                  for(final entry in cases.entries)for(final rtl in [false,true])for(final dark in [false,true]) {
                    testWidgets(entry.key+' rtl=$rtl dark=$dark',(tester) async {
                      await tester.pumpWidget(MaterialApp(home:entry.value));
                      final generated=tester.widget<ValueListenableBuilder<double>>(native());
                      expect(generated.valueListenable.value,isA<double>());
                      await tester.pumpWidget(const SizedBox.shrink());
                      await tester.pumpWidget(host(generated,rtl:rtl,dark:dark));
                      expect(native(),findsOneWidget);expect(tester.takeException(),isNull);
                      final element=tester.element(native());
                      if(entry.key.startsWith('external_')||entry.key=='moved') {
                        expect(identical(generated.valueListenable,Sources.instance),isTrue);
                        final adds=Sources.instance.adds,removes=Sources.instance.removes;
                        final initial=Sources.instance.value;
                        expect(find.text('value $initial'),findsOneWidget);
                        Sources.instance.change();await tester.pump();
                        expect(find.text('value '+(initial+1).toString()),findsOneWidget);
                        expect(identical(element,tester.element(native())),isTrue);
                        expect(Sources.instance.adds,adds);expect(Sources.instance.removes,removes);
                        await tester.pumpWidget(const SizedBox.shrink());
                        expect(Sources.instance.removes,removes+1);expect(Sources.instance.hasListeners,isFalse);
                        expect(Sources.instance.disposals,0);Sources.instance.change();await tester.pump();
                      } else if(entry.key!='empty'&&entry.key!='direct') {
                        expect(find.text('prebuilt child'),findsOneWidget);
                      }
                      expect(tester.takeException(),isNull);
                    });
                  }
                  testWidgets('replace source, retain child, unsubscribe without disposing owner source',(tester) async {
                    final first=TrackedSource(),second=TrackedSource();
                    var builds=0,childBuilds=0;
                    final content=CountChild(()=>childBuilds++);
                    final Widget child=sliver?SliverToBoxAdapter(child:content):content;
                    final seenValues=<double>[];
                    Widget build(BuildContext context,double value,Widget? passed) { builds++;seenValues.add(value);expect(identical(child,passed),isTrue);return passed!; }
                    Widget make(ValueListenable<double> source) => ValueListenableBuilder(key:const ValueKey('retained'),valueListenable:source,builder:build,child:child);
                    await tester.pumpWidget(host(make(first)));
                    final element=tester.element(native()),initial=builds;
                    expect(first.adds,1);expect(childBuilds,1);
                    first.change();first.change();await tester.pump();
                    expect(builds,initial+1);expect(childBuilds,1);expect(seenValues.last,2.0);
                    first.value=first.value;await tester.pump();expect(builds,initial+1);
                    second.value=42.0;
                    await tester.pumpWidget(host(make(second)));final afterSwitch=builds;
                    expect(seenValues.last,42.0);
                    expect(identical(element,tester.element(native())),isTrue);
                    expect(first.removes,1);expect(first.hasListeners,isFalse);expect(second.adds,1);
                    first.change();await tester.pump();expect(builds,afterSwitch);
                    second.change();await tester.pump();expect(builds,afterSwitch+1);expect(childBuilds,1);
                    await tester.pumpWidget(const SizedBox.shrink());
                    expect(second.removes,1);expect(second.hasListeners,isFalse);
                    expect(first.disposals,0);expect(second.disposals,0);
                    second.change();await tester.pump();expect(tester.takeException(),isNull);
                    first.dispose();second.dispose();
                  });
                  testWidgets('generated Child builder supports real controller ticks, reverse, replacement and owner disposal',(tester) async {
                    await tester.pumpWidget(const MaterialApp(home:child_case.Sample()));
                    final generated=tester.widget<ValueListenableBuilder<double>>(native());
                    await tester.pumpWidget(const SizedBox.shrink());
                    final first=TrackedController(tester),second=TrackedController(tester);
                    var builds=0,childBuilds=0;
                    final values=<double>[];
                    final content=CountChild(()=>childBuilds++);
                    final Widget child=sliver?SliverToBoxAdapter(child:content):content;
                    final key=const ValueKey('controller-probe');
                    Widget make(TrackedController controller) => ValueListenableBuilder(key:key,valueListenable:controller,child:child,
                      builder:(context,value,passed) { builds++;values.add(controller.value);
                        return generated.builder(context,value,passed); });
                    final finder=find.byKey(key,skipOffstage:false);
                    await tester.pumpWidget(host(make(first)));
                    final element=tester.element(finder);expect(childBuilds,1);expect(first.adds,1);
                    first.forward();await tester.pump();await tester.pump(const Duration(milliseconds:250));
                    expect(first.value,closeTo(0.25,0.001));expect(values.last,first.value);
                    expect(builds,greaterThan(1));expect(childBuilds,1);
                    expect(identical(element,tester.element(finder)),isTrue);
                    first.reverse();await tester.pump();await tester.pump(const Duration(milliseconds:100));
                    expect(first.value,lessThan(0.25));expect(values.last,first.value);
                    await tester.pumpWidget(host(make(second)));final switched=builds;
                    expect(first.removes,1);expect(first.disposals,0);expect(second.adds,1);
                    first.stop();first.value=0.9;await tester.pump();expect(builds,switched);
                    second.forward();await tester.pump();await tester.pump(const Duration(milliseconds:250));
                    expect(values.last,closeTo(0.25,0.001));expect(childBuilds,1);
                    await tester.pumpWidget(const SizedBox.shrink());
                    expect(second.removes,1);expect(second.disposals,0);
                    first.dispose();second.dispose();await tester.pump();
                    expect(tester.takeException(),isNull);expect(tester.binding.transientCallbackCount,0);
                  });
                  testWidgets('nullable notifier forwards null and non-null values without recreating Child',(tester) async {
                    final notifier=ValueNotifier<int?>(null);
                    final seen=<int?>[];final child=sliver?const SliverToBoxAdapter():const SizedBox.shrink();
                    Widget build() => ValueListenableBuilder<int?>(key:const ValueKey('nullable-values'),valueListenable:notifier,
                      child:child,builder:(c,value,passed){seen.add(value);expect(identical(passed,child),isTrue);return passed!;});
                    await tester.pumpWidget(host(build()));final element=tester.element(find.byKey(const ValueKey('nullable-values'),skipOffstage:false));
                    expect(seen.last,isNull);notifier.value=7;await tester.pump();expect(seen.last,7);
                    notifier.value=null;await tester.pump();expect(seen.last,isNull);
                    expect(identical(element,tester.element(find.byKey(const ValueKey('nullable-values'),skipOffstage:false))),isTrue);
                    await tester.pumpWidget(const SizedBox.shrink());expect(notifier.hasListeners,isFalse);
                    notifier.dispose();expect(tester.takeException(),isNull);
                  });
                  testWidgets('null Child and inherited updates retain native element',(tester) async {
                    final calls=<Brightness>[];
                    final widget=ValueListenableBuilder(valueListenable:const AlwaysStoppedAnimation<double>(0),
                      builder:(context,value,child) { expect(child,isNull);calls.add(Theme.of(context).brightness);
                        return sliver?const SliverToBoxAdapter():const SizedBox.shrink(); });
                    await tester.pumpWidget(host(widget));
                    final element=tester.element(native());expect(calls.last,Brightness.light);
                    await tester.pumpWidget(host(widget,dark:true));await tester.pumpAndSettle();
                    expect(calls.last,Brightness.dark);expect(identical(element,tester.element(native())),isTrue);
                    expect(tester.takeException(),isNull);
                  });
                }
                """.formatted(sliver));
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
    }
    @Test void genericBuiltinsNullableValuesAndProjectTypeAliasesUseOneProvenT() throws Exception {
        initialize();
        Files.writeString(lib.resolve("builders.dart"), """
                import 'package:flutter/widgets.dart';
                import 'package:flutter/foundation.dart';
                enum Mode { one, two }
                class Payload { const Payload(); }
                typedef Rows = List<String>;
                typedef NullablePayload = Payload?;
                typedef DynamicAlias = dynamic;
                class Bindings {
                  static final ValueNotifier<Payload> payload = ValueNotifier(const Payload());
                  static final ValueNotifier<Mode> mode = ValueNotifier(Mode.one);
                  static final ValueNotifier<Rows> rows = ValueNotifier(<String>['one']);
                  static final ValueNotifier<NullablePayload> nullablePayload = ValueNotifier(null);
                  static final ValueNotifier<DynamicAlias> dynamicAlias = ValueNotifier(1);
                  static Widget payloadBuilder(BuildContext c, Payload value, Widget? child) => child ?? const SizedBox.shrink();
                  static Widget modeBuilder(BuildContext c, Mode value, Widget? child) => child ?? const SizedBox.shrink();
                  static Widget rowsBuilder(BuildContext c, Rows value, Widget? child) => child ?? const SizedBox.shrink();
                  static Widget nullablePayloadBuilder(BuildContext c, NullablePayload value, Widget? child) => child ?? const SizedBox.shrink();
                  static Widget dynamicAliasBuilder(BuildContext c, DynamicAlias value, Widget? child) => child ?? const SizedBox.shrink();
                }
                """);
        var imports = new StringBuilder("import 'package:flutter/material.dart';\nimport 'package:flutter_test/flutter_test.dart';\n");
        var cases = new ArrayList<String>();
        int index = 0;
        for (boolean sliver : List.of(false, true)) for (boolean nullable : List.of(false, true))
            for (String type : RadioWidgetPropertySchema.valueTypes()) {
                var prototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(sliver
                        ? ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE : ValueListenableBuilderWidgetPropertySchema.TYPE).orElseThrow(), BUILDER_ID);
                var group = new WidgetNode(GROUP_ID, new WidgetTypeId(sliver ? "flutter.widgets.CustomScrollView" : "flutter.widgets.Column"),
                        Map.of(), Map.of(new SlotName(sliver ? "slivers" : "children"), new WidgetSlot.ListSlot(List.of(prototype))));
                var baselineProps = new HashMap<>(prototype.properties());
                baselineProps.put(new PropertyName("builder"), ref("_baselineBuilder",false,null,false));
                prototype = new WidgetNode(prototype.id(),prototype.type(),baselineProps,prototype.slots());
                group = new WidgetNode(group.id(),group.type(),group.properties(),Map.of(
                        new SlotName(sliver?"slivers":"children"),new WidgetSlot.ListSlot(List.of(prototype))));
                var before = open(group, "generic.dart", "// User member is preserved.\nint _userValue() => 73;\nWidget _baselineBuilder(BuildContext c,double value,Widget? child) => "
                        + (sliver?"const SliverToBoxAdapter();":"const SizedBox.shrink();"));
                var candidate = apply(before, new PatchProperties(BUILDER_ID, List.of(
                        new PatchProperties.SetPatch(new PropertyName("builder"),new PropertyValue.StringValue("child")),
                        new PatchProperties.SetPatch(new PropertyName("valueType"), new PropertyValue.StringValue(type)),
                        new PatchProperties.SetPatch(new PropertyName("nullableValueType"), new PropertyValue.BooleanValue(nullable)))));
                assertAnalysis(before, candidate, "generic.dart", true);
                String name = "generic" + index++; saveCase(name, candidate);
                imports.append("import '../lib/").append(name).append(".dart' as ").append(name).append(";\n");
                cases.add("testWidgets('" + name + "',(tester) async { await tester.pumpWidget(const MaterialApp(home:" + name
                        + ".Sample())); final w=tester.widget<ValueListenableBuilder<" + type + (nullable ? "?" : "")
                        + ">>(find.byWidgetPredicate((w)=>w is ValueListenableBuilder<" + type + (nullable ? "?" : "")
                        + "> && w.valueListenable is AlwaysStoppedAnimation<" + type + (nullable ? "?" : "") + ">,skipOffstage:false)); expect(w.valueListenable.value," + (nullable ? "isNull" : "isA<" + type + ">()")
                        + ");expect(tester.takeException(),isNull);});");
            }
        for (boolean sliver : List.of(false, true)) for (String type : List.of("Payload", "Mode", "Rows", "NullablePayload", "DynamicAlias")) {
            var prototype = WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(sliver
                    ? ValueListenableBuilderWidgetPropertySchema.SLIVER_TYPE : ValueListenableBuilderWidgetPropertySchema.TYPE).orElseThrow(), BUILDER_ID);
            var child = new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.Text"),
                    Map.of(new PropertyName("data"),new PropertyValue.StringValue("typed child")),Map.of());
            if (sliver) child = new WidgetNode(StableId.random(),new WidgetTypeId("flutter.widgets.SliverToBoxAdapter"),
                    Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child)));
            prototype = new WidgetNode(prototype.id(),prototype.type(),prototype.properties(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.of(child)));
            var group = new WidgetNode(GROUP_ID,new WidgetTypeId(sliver?"flutter.widgets.CustomScrollView":"flutter.widgets.Column"),
                    Map.of(),Map.of(new SlotName(sliver?"slivers":"children"),new WidgetSlot.ListSlot(List.of(prototype))));
            var before = open(group,"generic.dart","// User member is preserved.\nint _userValue() => 73;");
            String field = Character.toLowerCase(type.charAt(0))+type.substring(1);
            var selected = ref(type,false,null,true);
            var candidate = apply(before,new PatchProperties(BUILDER_ID,List.of(
                    new PatchProperties.SetPatch(new PropertyName("valueType"),selected),
                    new PatchProperties.SetPatch(new PropertyName("valueListenable"),ref("Bindings",false,field,true)),
                    new PatchProperties.SetPatch(new PropertyName("builder"),ref("Bindings",false,field+"Builder",true)))));
            boolean valid = !type.equals("NullablePayload") && !type.equals("DynamicAlias");
            assertAnalysis(before,candidate,"generic.dart",valid);
            if (type.equals("NullablePayload")) {
                candidate = apply(candidate,new SetProperty(BUILDER_ID,new PropertyName("nullableValueType"),new PropertyValue.BooleanValue(true)));
                assertAnalysis(before,candidate,"generic.dart",true); valid=true;
                var constant = apply(candidate,new SetProperty(BUILDER_ID,new PropertyName("valueListenable"),new PropertyValue.StringValue("constant")));
                assertAnalysis(before,constant,"generic.dart",true);
            }
            if (!valid) continue;
            String name="generic"+index++;saveCase(name,candidate);
            imports.append("import '../lib/").append(name).append(".dart' as ").append(name).append(";\n");
            cases.add("testWidgets('"+name+"',(tester) async { await tester.pumpWidget(const MaterialApp(home:"+name+".Sample()));expect(find.text('typed child'),findsOneWidget);expect(tester.takeException(),isNull);});");
        }
        Files.writeString(Files.createDirectories(project.resolve("test")).resolve("generic_test.dart"),imports+"\nvoid main() {\n"+String.join("\n",cases)+"\n}\n");
        run(List.of(flutter.toString(),"test","--reporter","expanded"),"generic-test.log");
    }

    private static PropertyValue.DartObjectReferenceValue ref(String name,boolean factory,String member,boolean imported) {
        return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:value_listenable_builder_contract/builders.dart"):Optional.empty(),
                name,Optional.ofNullable(member),factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,
                factory?Optional.of(false):Optional.empty());
    }
    private void saveCase(String name,DesignerCommandSession session) throws Exception {
        var reopened=reopen(session,source(session));
        assertEquals(session.current().fdSnapshot(),reopened.current().fdSnapshot());
        assertArrayEquals(session.current().dartCandidateBytes(),reopened.current().dartCandidateBytes());
        assertTrue(source(reopened).contains("// User member is preserved."));
        assertTrue(source(reopened).contains("int _userValue() => 73;"));
        Files.write(lib.resolve(name+".dart"),reopened.current().dartCandidateBytes());
    }
    private void initialize() throws Exception {
        sdk = Path.of(System.getProperty("flutter.events.sdk")).toRealPath();
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        flutter = sdk.resolve(windows ? "bin/flutter.bat" : "bin/flutter");
        dart = sdk.resolve(windows ? "bin/cache/dart-sdk/bin/dart.exe" : "bin/cache/dart-sdk/bin/dart");
        Files.writeString(project.resolve("pubspec.yaml"), """
                name: value_listenable_builder_contract
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
        var session = StateBindingRealSdkTest.openRoot(root, file, members);
        return reopen(session, "import 'package:flutter/foundation.dart';\n" + source(session));
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
