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
import java.math.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="flutter.events.sdk",matches=".+")
class BackdropFilterRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 @Test void ordinaryConstructorConfigsSourcesAndHistory()throws Exception{verifyConstructor(BackdropFilterWidgetPropertySchema.TYPE);}
 @Test void groupedConstructorKeysSourcesAndHistory()throws Exception{verifyConstructor(BackdropFilterWidgetPropertySchema.GROUPED);}
 private void verifyConstructor(WidgetTypeId requestedType)throws Exception{
  initialize();Files.createDirectories(project.resolve("test"));
  Files.writeString(lib.resolve("references.dart"),"""
import 'dart:ui' as ui;
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
final _sharedBackdropKey=BackdropKey();
ui.ImageFilter get filter => const ColorFilter.mode(Color(0xFF225588), BlendMode.src);
ui.ImageFilter filterFactory() => filter;
ui.Rect? get bounds => const ui.Rect.fromLTWH(1,2,20,30);
ui.Rect? boundsFactory() => bounds;
Float64List get matrix4 => Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]);
Float64List matrix4Factory() => matrix4;
ui.ImageFilter get inner => const ColorFilter.mode(Color(0xFFFF0000),BlendMode.src);
ui.ImageFilter innerFactory() => inner;
ui.ImageFilter get outer => const ColorFilter.mode(Color(0xFF00FF00),BlendMode.src);
ui.ImageFilter outerFactory() => outer;
ui.FragmentShader get shader => throw StateError('Source-owned shader');
ui.FragmentShader shaderFactory() => shader;
ImageFilterConfig get filterConfig => const ImageFilterConfig.blur(sigmaX:3,bounded:true);
ImageFilterConfig filterConfigFactory() => filterConfig;
ImageFilterConfig get configInner => const ImageFilterConfig.blur(sigmaX:1);
ImageFilterConfig configInnerFactory() => configInner;
ImageFilterConfig get configOuter => const ImageFilterConfig.blur(sigmaX:2);
ImageFilterConfig configOuterFactory() => configOuter;
BackdropKey? get backdropGroupKey => _sharedBackdropKey;
BackdropKey? backdropGroupKeyFactory() => backdropGroupKey;
BackdropKey? get backdropKey => _sharedBackdropKey;
BackdropKey? backdropKeyFactory() => backdropKey;
class Values {
static final _sharedBackdropKey=BackdropKey();
static ui.ImageFilter get filter => const ColorFilter.mode(Color(0xFF225588), BlendMode.src);
static ui.ImageFilter filterFactory() => filter;
static ui.Rect? get bounds => const ui.Rect.fromLTWH(1,2,20,30);
static ui.Rect? boundsFactory() => bounds;
static Float64List get matrix4 => Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]);
static Float64List matrix4Factory() => matrix4;
static ui.ImageFilter get inner => const ColorFilter.mode(Color(0xFFFF0000),BlendMode.src);
static ui.ImageFilter innerFactory() => inner;
static ui.ImageFilter get outer => const ColorFilter.mode(Color(0xFF00FF00),BlendMode.src);
static ui.ImageFilter outerFactory() => outer;
static ui.FragmentShader get shader => throw StateError('Source-owned shader');
static ui.FragmentShader shaderFactory() => shader;
static ImageFilterConfig get filterConfig => const ImageFilterConfig.blur(sigmaX:3,bounded:true);
static ImageFilterConfig filterConfigFactory() => filterConfig;
static ImageFilterConfig get configInner => const ImageFilterConfig.blur(sigmaX:1);
static ImageFilterConfig configInnerFactory() => configInner;
static ImageFilterConfig get configOuter => const ImageFilterConfig.blur(sigmaX:2);
static ImageFilterConfig configOuterFactory() => configOuter;
static BackdropKey? get backdropGroupKey => _sharedBackdropKey;
static BackdropKey? backdropGroupKeyFactory() => backdropGroupKey;
static BackdropKey? get backdropKey => _sharedBackdropKey;
static BackdropKey? backdropKeyFactory() => backdropKey;
}

""");
  int forms=0;
  for(var type:List.of(requestedType)){
   String tag=type.equals(BackdropFilterWidgetPropertySchema.TYPE)?"plain":"grouped";
   var frameId=StableId.random();var frame=new WidgetNode(frameId,new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()));
   var baseline=open(group(GROUP_ID,List.of(frame,adapter("Sibling",20))),"probe.dart","""
// User member is preserved.
int _userValue() => 73;
static final _sharedBackdropKey=BackdropKey();
static ui.ImageFilter get filter => const ColorFilter.mode(Color(0xFF225588), BlendMode.src);
static ui.ImageFilter filterFactory() => filter;
static ui.Rect? get bounds => const ui.Rect.fromLTWH(1,2,20,30);
static ui.Rect? boundsFactory() => bounds;
static Float64List get matrix4 => Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]);
static Float64List matrix4Factory() => matrix4;
static ui.ImageFilter get inner => const ColorFilter.mode(Color(0xFFFF0000),BlendMode.src);
static ui.ImageFilter innerFactory() => inner;
static ui.ImageFilter get outer => const ColorFilter.mode(Color(0xFF00FF00),BlendMode.src);
static ui.ImageFilter outerFactory() => outer;
static ui.FragmentShader get shader => throw StateError('Source-owned shader');
static ui.FragmentShader shaderFactory() => shader;
static ImageFilterConfig get filterConfig => const ImageFilterConfig.blur(sigmaX:3,bounded:true);
static ImageFilterConfig filterConfigFactory() => filterConfig;
static ImageFilterConfig get configInner => const ImageFilterConfig.blur(sigmaX:1);
static ImageFilterConfig configInnerFactory() => configInner;
static ImageFilterConfig get configOuter => const ImageFilterConfig.blur(sigmaX:2);
static ImageFilterConfig configOuterFactory() => configOuter;
static BackdropKey? get backdropGroupKey => _sharedBackdropKey;
static BackdropKey? backdropGroupKeyFactory() => backdropGroupKey;
static BackdropKey? get backdropKey => _sharedBackdropKey;
static BackdropKey? backdropKeyFactory() => backdropKey;
ui.ImageFilter get _filter => filter;
ui.ImageFilter _filterFactory() => filter;
ui.Rect? get _bounds => bounds;
ui.Rect? _boundsFactory() => bounds;
Float64List get _matrix4 => matrix4;
Float64List _matrix4Factory() => matrix4;
ui.ImageFilter get _inner => inner;
ui.ImageFilter _innerFactory() => inner;
ui.ImageFilter get _outer => outer;
ui.ImageFilter _outerFactory() => outer;
ui.FragmentShader get _shader => shader;
ui.FragmentShader _shaderFactory() => shader;
ImageFilterConfig get _filterConfig => filterConfig;
ImageFilterConfig _filterConfigFactory() => filterConfig;
ImageFilterConfig get _configInner => configInner;
ImageFilterConfig _configInnerFactory() => configInner;
ImageFilterConfig get _configOuter => configOuter;
ImageFilterConfig _configOuterFactory() => configOuter;
BackdropKey? get _backdropGroupKey => backdropGroupKey;
BackdropKey? _backdropGroupKeyFactory() => backdropGroupKey;
BackdropKey? get _backdropKey => backdropKey;
BackdropKey? _backdropKeyFactory() => backdropKey;
Object get _wrong=>Object();
dynamic get _dynamic=>null;
ImageFilterConfig? get _nullableConfig=>null;
ui.ImageFilter? get _nullableFilter=>null;
BackdropKey? get _nullKey=>null;

""");
   baseline=reopen(baseline,"import 'dart:ui' as ui;\nimport 'dart:typed_data';\nimport 'package:flutter/material.dart';\nimport 'package:flutter/rendering.dart';\n"+source(baseline));
   var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(type).orElseThrow(),NESTED_ID);
   var base=apply(baseline,new AddWidget(new WidgetPlacement(frameId,new SlotName("child"),0),prototype));assertAnalysis(baseline,base,"probe.dart",true);record(tag+"_creation",base,"blur");
   assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
   base=reopen(base,source(base));
   var child=apply(base,new AddWidget(new WidgetPlacement(NESTED_ID,new SlotName("child"),0),adapter("Filtered child",32)));assertAnalysis(base,child,"probe.dart",true);base=reopen(child,source(child));
   for(String preset:List.of("blur","dilate","erode","matrix","compose")){
    var changed=preset.equals("blur")?base:apply(base,new SetProperty(NESTED_ID,new PropertyName("filter"),new PropertyValue.StringValue(preset)));
    if(changed!=base)assertAnalysis(base,changed,"probe.dart",true);record(tag+"_local_"+preset,changed,preset);
   }
   for(String config:BackdropFilterWidgetPropertySchema.CONFIGS){
    var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName("filterConfig"),new PropertyValue.StringValue(config)));assertAnalysis(base,changed,"probe.dart",true);record(tag+"_config_"+config,changed,"config_"+config);
   }
   for(String field:List.of("filter","filterConfig","configInner","configOuter","bounds","matrix4","inner","outer","shader","backdropGroupKey")){
    if(type.equals(BackdropFilterWidgetPropertySchema.GROUPED)&&field.equals("backdropGroupKey"))continue;
    for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
     var changes=active(field,reference(field,imported,member,factory));var changed=apply(base,new PatchProperties(NESTED_ID,changes));assertAnalysis(base,changed,"probe.dart",true);forms++;
     if(!field.equals("shader"))record(tag+"_source_"+field.toLowerCase()+"_"+imported+"_"+member+"_"+factory,changed,field+"Source");
    }
    for(String bad:List.of("_wrong","_dynamic"))assertAnalysis(base,apply(base,new PatchProperties(NESTED_ID,active(field,ref(bad)))),"probe.dart",false);
   }
   for(String field:List.of("filterConfig","configInner","configOuter"))assertAnalysis(base,apply(base,new PatchProperties(NESTED_ID,active(field,ref("_nullableConfig")))),"probe.dart",false);
   assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName("filter"),ref("_nullableFilter"))),"probe.dart",false);
   var configured=apply(base,new PatchProperties(NESTED_ID,List.of(new PatchProperties.SetPatch(new PropertyName("filterConfig"),new PropertyValue.StringValue("blur")),
      new PatchProperties.SetPatch(new PropertyName("configBounded"),new PropertyValue.BooleanValue(true)),new PatchProperties.SetPatch(new PropertyName("configSigmaX"),new PropertyValue.DoubleValue(BigDecimal.valueOf(3))))));
   assertAnalysis(base,configured,"probe.dart",true);record(tag+"_bounded",configured,"filterConfigSource");configured=reopen(configured,source(configured));
   var absent=new PropertyValue.DartObjectReferenceValue(Optional.of("package:absent/never.dart"),"AbsentFilter",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
   var inactive=apply(configured,new SetProperty(NESTED_ID,new PropertyName("filter"),absent));assertAnalysis(configured,inactive,"probe.dart",true);record(tag+"_inactive",inactive,"filterConfigSource");inactive=reopen(inactive,source(inactive));
   assertFalse(source(inactive).contains("package:absent"));
   final var storedInactive=inactive;
   var activated=apply(storedInactive,new ResetProperty(NESTED_ID,new PropertyName("filterConfig")));
   var failure=assertThrows(java.io.IOException.class,()->assertAnalysis(storedInactive,activated,"probe.dart",false));
   assertTrue(failure.getMessage().contains("package:absent/never.dart"),failure.getMessage());
   assertArrayEquals(storedInactive.current().dartCandidateBytes(),Files.readAllBytes(lib.resolve("probe.dart")));
   var noFilter=apply(configured,new ResetProperty(NESTED_ID,new PropertyName("filter")));assertAnalysis(configured,noFilter,"probe.dart",true);record(tag+"_no_filter",noFilter,"filterConfigSource");
   var disabled=apply(base,new SetProperty(NESTED_ID,new PropertyName("enabled"),new PropertyValue.BooleanValue(false)));assertAnalysis(base,disabled,"probe.dart",true);record(tag+"_disabled",disabled,"disabled");
   assertFalse(base.apply(new ResetProperty(NESTED_ID,new PropertyName("filter"))).changed());
   assertFalse(base.apply(new SetProperty(NESTED_ID,new PropertyName("filter"),new PropertyValue.StringValue("shader"))).changed());
   if(type.equals(BackdropFilterWidgetPropertySchema.TYPE)){
    var key=apply(base,new SetProperty(NESTED_ID,new PropertyName("backdropGroupKey"),ref("_nullKey")));assertAnalysis(base,key,"probe.dart",true);record("nullable_key",key,"blur");
   }else{
    var wrapper=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(BackdropFilterWidgetPropertySchema.GROUP).orElseThrow(),StableId.random());
    var grouped=apply(base,new WrapWidget(GROUP_ID,wrapper,new SlotName("child"),0));assertAnalysis(base,grouped,"probe.dart",true);record("backdrop_group",grouped,"group");grouped=reopen(grouped,source(grouped));
    for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
      var key=apply(grouped,new SetProperty(wrapper.id(),new PropertyName("backdropKey"),reference("backdropKey",imported,member,factory)));assertAnalysis(grouped,key,"probe.dart",true);forms++;record("group_key_"+imported+"_"+member+"_"+factory,key,"group");
    }
    for(String bad:List.of("_wrong","_dynamic"))assertAnalysis(grouped,apply(grouped,new SetProperty(wrapper.id(),new PropertyName("backdropKey"),ref(bad))),"probe.dart",false);
    var key=apply(grouped,new SetProperty(wrapper.id(),new PropertyName("backdropKey"),ref("_nullKey")));assertAnalysis(grouped,key,"probe.dart",true);record("group_null_key",key,"group");
   }
   var removed=apply(base,new RemoveWidget(NESTED_ID));assertAnalysis(base,removed,"probe.dart",true);assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  }
  assertEquals(80,forms);
  Files.writeString(project.resolve("test/backdrop_test.dart"),runtime());runtimeCaseCount=cases.size()*2+4;run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private static List<PatchProperties.Patch> active(String field,PropertyValue value){
  var changes=new ArrayList<PatchProperties.Patch>();
  if(!field.equals("filter"))changes.add(new PatchProperties.SetPatch(new PropertyName("filter"),new PropertyValue.StringValue(mode(field))));
  if(field.equals("configInner")||field.equals("configOuter"))changes.add(new PatchProperties.SetPatch(new PropertyName("filterConfig"),new PropertyValue.StringValue("compose")));
  changes.add(new PatchProperties.SetPatch(new PropertyName(field),value));return changes;
 }
 private static String mode(String field){return switch(field){case "matrix4"->"matrix";case "inner","outer"->"compose";case "shader"->"shader";default->"blur";};}
 private void record(String name,DesignerCommandSession session,String mode)throws Exception{saveCase(name,session);cases.put(name,mode);}
 private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:backdrop_filter_contract/references.dart"):Optional.empty(),
   member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
   member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
   factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
 }

 private static String nativeRuntime(){return """
void nativeBackdropTests() {
  testWidgets(
    'backdrop filters existing pixels, preserves foreground and clip',
    (tester) async {
      final key = GlobalKey();
      Widget view(bool enabled) => Directionality(
        textDirection: TextDirection.ltr,
        child: Center(
          child: RepaintBoundary(
            key: key,
            child: SizedBox(
              width: 64,
              height: 64,
              child: Stack(
                children: [
                  const Positioned.fill(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Expanded(child: ColoredBox(color: Color(0xFFFF0000))),
                        Expanded(child: ColoredBox(color: Color(0xFF0000FF))),
                      ],
                    ),
                  ),
                  Positioned(
                    left: 16,
                    top: 16,
                    width: 32,
                    height: 32,
                    child: ClipRect(
                      child: BackdropFilter(
                        filter: ui.ImageFilter.blur(sigmaX: 4, sigmaY: 4),
                        enabled: enabled,
                        child: const Center(
                          child: SizedBox(
                            width: 8,
                            height: 8,
                            child: ColoredBox(color: Color(0xFF00FF00)),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
      Future<List<int>> pixel(int x, int y) async {
        final render =
            key.currentContext!.findRenderObject()! as RenderRepaintBoundary;
        final bytes = await tester.runAsync(() async {
          final image = await render.toImage();
          try {
            return await image.toByteData(format: ui.ImageByteFormat.rawRgba);
          } finally {
            image.dispose();
          }
        });
        final index = (y * 64 + x) * 4;
        return [for (int n = 0; n < 4; n++) bytes!.getUint8(index + n)];
      }

      await tester.pumpWidget(view(true));
      final blurred = await pixel(30, 20);
      expect(blurred[0], inExclusiveRange(0, 255));
      expect(blurred[2], inExclusiveRange(0, 255));
      expect(await pixel(30, 8), [255, 0, 0, 255]);
      expect(await pixel(32, 32), [0, 255, 0, 255]);
      final render = tester.renderObject<RenderBackdropFilter>(
        find.byType(BackdropFilter),
      );
      expect(render.needsCompositing, isTrue);
      await tester.pumpWidget(view(false));
      expect(tester.renderObject(find.byType(BackdropFilter)), same(render));
      expect(render.enabled, isFalse);
      // Pinned RenderBackdropFilter retains compositing when it has a child.
      expect(render.needsCompositing, isTrue);
      expect(await pixel(30, 20), [255, 0, 0, 255]);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'bounded config resolves current layout bounds and preserves composition order',
    (tester) async {
      for (final bounds in [
        const Rect.fromLTWH(1, 2, 20, 30),
        const Rect.fromLTWH(8, 9, 40, 60),
      ]) {
        final context = ImageFilterContext(bounds: bounds);
        const config = ImageFilterConfig.blur(
          sigmaX: 2,
          sigmaY: 3,
          bounded: true,
          tileMode: TileMode.decal,
        );
        expect(
          config.resolve(context),
          ui.ImageFilter.blur(
            sigmaX: 2,
            sigmaY: 3,
            bounds: bounds,
            tileMode: TileMode.decal,
          ),
        );
        const outer = ImageFilterConfig(
          ColorFilter.mode(Color(0xFF00FF00), BlendMode.src),
        );
        expect(
          const ImageFilterConfig.compose(
            inner: config,
            outer: outer,
          ).resolve(context),
          ui.ImageFilter.compose(
            inner: config.resolve(context),
            outer: outer.resolve(context),
          ),
        );
      }
      final key = GlobalKey();
      Widget view(double width) => Directionality(
        textDirection: TextDirection.ltr,
        child: Center(
          child: SizedBox(
            width: width,
            height: 20,
            child: ClipRect(
              child: BackdropFilter(
                key: key,
                filterConfig: const ImageFilterConfig.blur(
                  sigmaX: 2,
                  bounded: true,
                ),
                child: const SizedBox.expand(),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(view(30));
      final render =
          key.currentContext!.findRenderObject()! as RenderBackdropFilter;
      expect(render.size, const Size(30, 20));
      await tester.pumpWidget(view(60));
      expect(key.currentContext!.findRenderObject(), same(render));
      expect(render.size, const Size(60, 20));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets(
    'grouped nearest key, explicit key, null and no ancestor match native semantics',
    (tester) async {
      final shared = BackdropKey(),
          nested = BackdropKey(),
          explicit = BackdropKey();
      Widget filter(String id, {bool grouped = true, BackdropKey? key}) =>
          grouped
          ? BackdropFilter.grouped(
              key: ValueKey(id),
              filter: ui.ImageFilter.blur(),
              child: const SizedBox(width: 12, height: 12),
            )
          : BackdropFilter(
              key: ValueKey(id),
              filter: ui.ImageFilter.blur(),
              backdropGroupKey: key,
              child: const SizedBox(width: 12, height: 12),
            );
      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: Column(
            children: [
              BackdropGroup(
                backdropKey: shared,
                child: Column(
                  children: [
                    filter('a'),
                    filter('b'),
                    filter('explicit', grouped: false, key: explicit),
                    filter('null', grouped: false),
                    BackdropGroup(backdropKey: nested, child: filter('nested')),
                  ],
                ),
              ),
              filter('outside'),
            ],
          ),
        ),
      );
      BackdropKey? keyOf(String id) => tester
          .renderObject<RenderBackdropFilter>(find.byKey(ValueKey(id)))
          .backdropKey;
      expect(keyOf('a'), same(shared));
      expect(keyOf('b'), same(shared));
      expect(keyOf('nested'), same(nested));
      expect(keyOf('explicit'), same(explicit));
      expect(keyOf('null'), isNull);
      expect(keyOf('outside'), isNull);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox());
    },
  );
  testWidgets('enabled leaves child hit testing semantics and layout intact', (
    tester,
  ) async {
    int taps = 0;
    final semantics = tester.ensureSemantics();
    Widget view(bool enabled) => Directionality(
      textDirection: TextDirection.rtl,
      child: Center(
        child: BackdropFilter(
          filterConfig: const ImageFilterConfig.blur(sigmaX: 2, bounded: true),
          enabled: enabled,
          child: Semantics(
            label: 'Backdrop child',
            button: true,
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () {
                taps++;
              },
              child: const SizedBox(width: 40, height: 20),
            ),
          ),
        ),
      ),
    );
    for (final enabled in [true, false]) {
      await tester.pumpWidget(view(enabled));
      expect(tester.getSize(find.byType(BackdropFilter)), const Size(40, 20));
      await tester.tap(find.byType(BackdropFilter));
      expect(find.bySemanticsLabel('Backdrop child'), findsOneWidget);
    }
    expect(taps, 2);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    semantics.dispose();
  });
}

""";}
 private String runtime(){
  var imports=new StringBuilder();var tests=new StringBuilder();
  cases.forEach((name,mode)->{
   imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
   tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{")
    .append("await tester.pumpWidget(MaterialApp(home:Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:const case_").append(name).append(".Sample())));await tester.pump();")
    .append("final n=tester.widget<BackdropFilter>(find.byType(BackdropFilter));expect(find.text('Sibling'),findsOneWidget);");
   String expected=switch(mode){
    case "filterSource"->"const ColorFilter.mode(Color(0xFF225588),BlendMode.src)";
    case "boundsSource"->"ui.ImageFilter.blur(sigmaX:0,sigmaY:0,bounds:const ui.Rect.fromLTWH(1,2,20,30))";
    case "matrix4Source"->"ui.ImageFilter.matrix(Float64List.fromList([1,0,0,0,0,1,0,0,0,0,1,0,2,3,0,1]))";
    case "innerSource"->"ui.ImageFilter.compose(inner:const ColorFilter.mode(Color(0xFFFF0000),BlendMode.src),outer:ui.ImageFilter.blur())";
    case "outerSource"->"ui.ImageFilter.compose(inner:ui.ImageFilter.blur(),outer:const ColorFilter.mode(Color(0xFF00FF00),BlendMode.src))";
    case "dilate"->"ui.ImageFilter.dilate()";case "erode"->"ui.ImageFilter.erode()";
    case "matrix"->"ui.ImageFilter.matrix(Matrix4.identity().storage)";
    case "compose"->"ui.ImageFilter.compose(inner:ui.ImageFilter.blur(),outer:ui.ImageFilter.blur())";
    case "config_wrap"->"ImageFilterConfig(ui.ImageFilter.blur())";
    case "config_blur"->"const ImageFilterConfig.blur()";
    case "config_compose"->"const ImageFilterConfig.compose(inner:ImageFilterConfig.blur(),outer:ImageFilterConfig.blur())";
    case "filterConfigSource"->"const ImageFilterConfig.blur(sigmaX:3,bounded:true)";
    case "configInnerSource"->"const ImageFilterConfig.compose(inner:ImageFilterConfig.blur(sigmaX:1),outer:ImageFilterConfig.blur())";
    case "configOuterSource"->"const ImageFilterConfig.compose(inner:ImageFilterConfig.blur(),outer:ImageFilterConfig.blur(sigmaX:2))";
    default->"ui.ImageFilter.blur()";
   };
   boolean config=mode.startsWith("config")||mode.equals("filterConfigSource");
   tests.append("expect(n.").append(config?"filterConfig":"filter").append(",").append(expected).append(");expect(n.enabled,").append(!mode.equals("disabled")).append(");");
   if(mode.equals("group"))tests.append("final g=tester.widget<BackdropGroup>(find.byType(BackdropGroup));expect(tester.renderObject<RenderBackdropFilter>(find.byType(BackdropFilter)).backdropKey,same(g.backdropKey));");
   if(mode.equals("backdropGroupKeySource"))tests.append("expect(tester.renderObject<RenderBackdropFilter>(find.byType(BackdropFilter)).backdropKey,isNotNull);");
   tests.append("expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());});}\n");
  });
  return "import 'dart:ui' as ui;\nimport 'dart:typed_data';\nimport 'package:flutter/material.dart';\nimport 'package:flutter/rendering.dart';\nimport 'package:flutter_test/flutter_test.dart';\n"+imports+nativeRuntime()+"\nvoid main(){nativeBackdropTests();"+tests+"}\n";
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
                name: backdrop_filter_contract
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
        // Inactive local drafts and structurally identical replacements can be FD-only.
        // No analyzer ticket is necessary when the exact generated Dart is unchanged.
        if (candidate.current().preparedPair().isEmpty()) {
            assertTrue(pass);
            assertArrayEquals(baseline.current().dartCandidateBytes(), candidate.current().dartCandidateBytes());
            assertNotEquals(baseline.current().fdSnapshot(), candidate.current().fdSnapshot());
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
        if (logName.equals("flutter-test.log")) {
            assertTrue(output.contains("+"+runtimeCaseCount+": All tests passed!"), output);
            System.out.println("BackdropFilter pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
