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
class FadeInImageRealSdkTest {
 @TempDir Path project;
 private Path sdk,flutter,dart,lib;
 private static final StableId GROUP_ID=StableId.random(),NESTED_ID=StableId.random();
 private static final SlotName CHILDREN=new SlotName("children");
 private final Map<String,String> cases=new LinkedHashMap<>();
 private int runtimeCaseCount;
 private static final List<String> FIELDS=List.of("placeholder","image","placeholderErrorBuilder","imageErrorBuilder","fadeOutDurationUs","fadeInDurationUs","fadeOutCurve","fadeInCurve","color","placeholderColor","width","height","alignment");
 @Test void fullConstructorSourceEvidencePersistenceAndNativeImages()throws Exception {
  initialize();
  // Compile and run native fixture BEFORE the expensive analyzer matrix.
  Files.writeString(Files.createDirectories(project.resolve("test")).resolve("native_test.dart"),nativeRuntime()+"\nvoid main()=>nativeTests();\n");
  runtimeCaseCount=4;run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
  Files.delete(project.resolve("test/native_test.dart"));
  Files.writeString(lib.resolve("references.dart"),"""
import 'dart:convert';
import 'package:flutter/material.dart';
const png='iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=';
ImageProvider<Object> get placeholder=>MemoryImage(base64Decode(png));
ImageProvider<Object> placeholderFactory()=>placeholder;
ImageProvider<Object> get image=>MemoryImage(base64Decode(png));
ImageProvider<Object> imageFactory()=>image;
ImageErrorWidgetBuilder? get placeholderErrorBuilder=>(context,error,stack)=>const SizedBox();
ImageErrorWidgetBuilder? placeholderErrorBuilderFactory()=>placeholderErrorBuilder;
ImageErrorWidgetBuilder? get imageErrorBuilder=>(context,error,stack)=>const SizedBox();
ImageErrorWidgetBuilder? imageErrorBuilderFactory()=>imageErrorBuilder;
Duration get fadeOutDurationUs=>const Duration(milliseconds:10);
Duration fadeOutDurationUsFactory()=>fadeOutDurationUs;
Duration get fadeInDurationUs=>const Duration(milliseconds:20);
Duration fadeInDurationUsFactory()=>fadeInDurationUs;
Curve get fadeOutCurve=>Curves.linear;
Curve fadeOutCurveFactory()=>fadeOutCurve;
Curve get fadeInCurve=>Curves.easeIn;
Curve fadeInCurveFactory()=>fadeInCurve;
Color? get color=>const Color(0xFF112233);
Color? colorFactory()=>color;
Color? get placeholderColor=>const Color(0xFF445566);
Color? placeholderColorFactory()=>placeholderColor;
double? get width=>48;
double? widthFactory()=>width;
double? get height=>32;
double? heightFactory()=>height;
AlignmentGeometry get alignment=>AlignmentDirectional.topEnd;
AlignmentGeometry alignmentFactory()=>alignment;
class Values {
static ImageProvider<Object> get placeholder=>MemoryImage(base64Decode(png));
static ImageProvider<Object> placeholderFactory()=>placeholder;
static ImageProvider<Object> get image=>MemoryImage(base64Decode(png));
static ImageProvider<Object> imageFactory()=>image;
static ImageErrorWidgetBuilder? get placeholderErrorBuilder=>(context,error,stack)=>const SizedBox();
static ImageErrorWidgetBuilder? placeholderErrorBuilderFactory()=>placeholderErrorBuilder;
static ImageErrorWidgetBuilder? get imageErrorBuilder=>(context,error,stack)=>const SizedBox();
static ImageErrorWidgetBuilder? imageErrorBuilderFactory()=>imageErrorBuilder;
static Duration get fadeOutDurationUs=>const Duration(milliseconds:10);
static Duration fadeOutDurationUsFactory()=>fadeOutDurationUs;
static Duration get fadeInDurationUs=>const Duration(milliseconds:20);
static Duration fadeInDurationUsFactory()=>fadeInDurationUs;
static Curve get fadeOutCurve=>Curves.linear;
static Curve fadeOutCurveFactory()=>fadeOutCurve;
static Curve get fadeInCurve=>Curves.easeIn;
static Curve fadeInCurveFactory()=>fadeInCurve;
static Color? get color=>const Color(0xFF112233);
static Color? colorFactory()=>color;
static Color? get placeholderColor=>const Color(0xFF445566);
static Color? placeholderColorFactory()=>placeholderColor;
static double? get width=>48;
static double? widthFactory()=>width;
static double? get height=>32;
static double? heightFactory()=>height;
static AlignmentGeometry get alignment=>AlignmentDirectional.topEnd;
static AlignmentGeometry alignmentFactory()=>alignment;
}
""");
  var frameId=StableId.random();var frame=new WidgetNode(frameId,new WidgetTypeId("flutter.widgets.Center"),Map.of(),Map.of(new SlotName("child"),WidgetSlot.SingleSlot.empty()));
  var baseline=open(group(GROUP_ID,List.of(frame,adapter("Sibling",20))),"probe.dart","""
// User member is preserved.
int _userValue() => 73;
static const png='iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=';
static ImageProvider<Object> get placeholder=>MemoryImage(base64Decode(png));
static ImageProvider<Object> placeholderFactory()=>placeholder;
static ImageProvider<Object> get image=>MemoryImage(base64Decode(png));
static ImageProvider<Object> imageFactory()=>image;
static ImageErrorWidgetBuilder? get placeholderErrorBuilder=>(context,error,stack)=>const SizedBox();
static ImageErrorWidgetBuilder? placeholderErrorBuilderFactory()=>placeholderErrorBuilder;
static ImageErrorWidgetBuilder? get imageErrorBuilder=>(context,error,stack)=>const SizedBox();
static ImageErrorWidgetBuilder? imageErrorBuilderFactory()=>imageErrorBuilder;
static Duration get fadeOutDurationUs=>const Duration(milliseconds:10);
static Duration fadeOutDurationUsFactory()=>fadeOutDurationUs;
static Duration get fadeInDurationUs=>const Duration(milliseconds:20);
static Duration fadeInDurationUsFactory()=>fadeInDurationUs;
static Curve get fadeOutCurve=>Curves.linear;
static Curve fadeOutCurveFactory()=>fadeOutCurve;
static Curve get fadeInCurve=>Curves.easeIn;
static Curve fadeInCurveFactory()=>fadeInCurve;
static Color? get color=>const Color(0xFF112233);
static Color? colorFactory()=>color;
static Color? get placeholderColor=>const Color(0xFF445566);
static Color? placeholderColorFactory()=>placeholderColor;
static double? get width=>48;
static double? widthFactory()=>width;
static double? get height=>32;
static double? heightFactory()=>height;
static AlignmentGeometry get alignment=>AlignmentDirectional.topEnd;
static AlignmentGeometry alignmentFactory()=>alignment;
ImageProvider<Object> get _placeholder=>placeholder;
ImageProvider<Object> _placeholderFactory()=>placeholder;
ImageProvider<Object> get _image=>image;
ImageProvider<Object> _imageFactory()=>image;
ImageErrorWidgetBuilder? get _placeholderErrorBuilder=>placeholderErrorBuilder;
ImageErrorWidgetBuilder? _placeholderErrorBuilderFactory()=>placeholderErrorBuilder;
ImageErrorWidgetBuilder? get _imageErrorBuilder=>imageErrorBuilder;
ImageErrorWidgetBuilder? _imageErrorBuilderFactory()=>imageErrorBuilder;
Duration get _fadeOutDurationUs=>fadeOutDurationUs;
Duration _fadeOutDurationUsFactory()=>fadeOutDurationUs;
Duration get _fadeInDurationUs=>fadeInDurationUs;
Duration _fadeInDurationUsFactory()=>fadeInDurationUs;
Curve get _fadeOutCurve=>fadeOutCurve;
Curve _fadeOutCurveFactory()=>fadeOutCurve;
Curve get _fadeInCurve=>fadeInCurve;
Curve _fadeInCurveFactory()=>fadeInCurve;
Color? get _color=>color;
Color? _colorFactory()=>color;
Color? get _placeholderColor=>placeholderColor;
Color? _placeholderColorFactory()=>placeholderColor;
double? get _width=>width;
double? _widthFactory()=>width;
double? get _height=>height;
double? _heightFactory()=>height;
AlignmentGeometry get _alignment=>alignment;
AlignmentGeometry _alignmentFactory()=>alignment;
ImageProvider<Object>? get _nullableProvider=>null;
ImageProvider<dynamic> get _rawProvider=>MemoryImage(base64Decode(png));
ImageErrorWidgetBuilder? get _nullBuilder=>null;
dynamic _dynamicBuilder(BuildContext context,Object error,StackTrace? stack)=>null;
String get _wrong=>'wrong';
dynamic get _dynamic=>null;
""");
  baseline=reopen(baseline,"import 'dart:convert';\nimport 'package:flutter/material.dart';\n"+source(baseline));
  var prototype=WidgetNodePrototypeFactory.create(BuiltInWidgetCatalog.getDefault().find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow(),NESTED_ID);
  var base=apply(baseline,new AddWidget(new WidgetPlacement(frameId,new SlotName("child"),0),prototype));
  assertAnalysis(baseline,base,"probe.dart",true);record("creation",base,"creation");
  assertArrayEquals(baseline.current().dartCandidateBytes(),base.undo().session().current().dartCandidateBytes());
  assertArrayEquals(base.current().dartCandidateBytes(),base.undo().session().redo().session().current().dartCandidateBytes());
  base=reopen(base,source(base));
  for(boolean imported:List.of(false,true))for(boolean member:List.of(false,true))for(boolean factory:List.of(false,true)){
   var patches=new ArrayList<PatchProperties.Patch>();
   for(String name:FIELDS)patches.add(new PatchProperties.SetPatch(new PropertyName(name),reference(name,imported,member,factory)));
   var changed=apply(base,new PatchProperties(NESTED_ID,patches));assertAnalysis(base,changed,"probe.dart",true);record("sources_"+imported+"_"+member+"_"+factory,changed,"source");
  }
  var changes=new ArrayList<PatchProperties.Patch>();
  for(String name:List.of("fadeOutDurationUs","fadeInDurationUs"))changes.add(new PatchProperties.SetPatch(new PropertyName(name),new PropertyValue.IntegerValue(BigInteger.valueOf(1000))));
  for(String name:List.of("fadeOutCurve","fadeInCurve"))changes.add(new PatchProperties.SetPatch(new PropertyName(name),new PropertyValue.StringValue("easeIn")));
  changes.add(new PatchProperties.SetPatch(new PropertyName("width"),new PropertyValue.DoubleValue(BigDecimal.valueOf(48))));
  changes.add(new PatchProperties.SetPatch(new PropertyName("height"),new PropertyValue.DoubleValue(BigDecimal.valueOf(32))));
  changes.add(new PatchProperties.SetPatch(new PropertyName("color"),new PropertyValue.ColorValue(0xFF112233L)));
  changes.add(new PatchProperties.SetPatch(new PropertyName("placeholderColor"),new PropertyValue.ColorValue(0xFF445566L)));
  for(var entry:Map.of("fit","cover","placeholderFit","contain","filterQuality","high","placeholderFilterQuality","low","repeat","repeatX","colorBlendMode","multiply","placeholderColorBlendMode","srcIn").entrySet()){
   String type=entry.getKey().contains("BlendMode")?"BlendMode":entry.getKey().contains("Fit")||entry.getKey().equals("fit")?"BoxFit":entry.getKey().equals("repeat")?"ImageRepeat":"FilterQuality";
   changes.add(new PatchProperties.SetPatch(new PropertyName(entry.getKey()),new PropertyValue.EnumValue(type,entry.getValue())));
  }
  changes.add(new PatchProperties.SetPatch(new PropertyName("alignment"),new PropertyValue.AlignmentGeometryValue(PropertyValue.AlignmentGeometryValue.HorizontalBasis.DIRECTIONAL,BigDecimal.ONE,BigDecimal.ONE.negate())));
  changes.add(new PatchProperties.SetPatch(new PropertyName("imageSemanticLabel"),new PropertyValue.StringValue("Photo")));
  for(String name:List.of("excludeFromSemantics","matchTextDirection"))changes.add(new PatchProperties.SetPatch(new PropertyName(name),new PropertyValue.BooleanValue(true)));
  var local=apply(base,new PatchProperties(NESTED_ID,changes));assertAnalysis(base,local,"probe.dart",true);record("local",local,"local");
  var nullable=new ArrayList<PatchProperties.Patch>();
  for(var p:FadeInImageWidgetPropertySchema.properties())if(p.acceptedKinds().contains(PropertyValueKind.NULL))nullable.add(new PatchProperties.SetPatch(p.name(),new PropertyValue.NullValue()));
  var nulls=apply(base,new PatchProperties(NESTED_ID,nullable));assertAnalysis(base,nulls,"probe.dart",true);record("nulls",nulls,"creation");
  for(String name:List.of("placeholderErrorBuilder","imageErrorBuilder")){
   var changed=apply(base,new SetProperty(NESTED_ID,new PropertyName(name),ref("_nullBuilder")));assertAnalysis(base,changed,"probe.dart",true);record("nullable_"+name.toLowerCase(),changed,"creation");
  }
  var created=apply(base,new CreateEventHandler(NESTED_ID,new PropertyName("placeholderErrorBuilder"),"_placeholderFailure"));
  assertAnalysis(base,created,"probe.dart",true);created=reopen(created,source(created));
  var handlers=apply(created,new CreateEventHandler(NESTED_ID,new PropertyName("imageErrorBuilder"),"_targetFailure"));
  assertAnalysis(created,handlers,"probe.dart",true);handlers=reopen(handlers,source(handlers));
  var renamed=apply(handlers,new RenameEventHandler(NESTED_ID,new PropertyName("placeholderErrorBuilder"),"_newPlaceholderFailure"));assertAnalysis(handlers,renamed,"probe.dart",true);renamed=reopen(renamed,source(renamed));
  var disconnected=apply(renamed,new ResetProperty(NESTED_ID,new PropertyName("placeholderErrorBuilder")));assertAnalysis(renamed,disconnected,"probe.dart",true);
  assertTrue(source(disconnected).contains("Widget _newPlaceholderFailure("));saveCase("handler_lifecycle",disconnected);
  assertArrayEquals(renamed.current().dartCandidateBytes(),disconnected.undo().session().current().dartCandidateBytes());
  for(String name:FIELDS)for(String bad:List.of("_wrong","_dynamic"))assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName(name),ref(bad))),"probe.dart",false);
  for(String name:List.of("placeholder","image"))for(String bad:List.of("_nullableProvider","_rawProvider"))assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName(name),ref(bad))),"probe.dart",false);
  for(String name:List.of("placeholderErrorBuilder","imageErrorBuilder"))assertAnalysis(base,apply(base,new SetProperty(NESTED_ID,new PropertyName(name),ref("_dynamicBuilder"))),"probe.dart",false);
  for(String name:List.of("placeholder","image"))assertFalse(base.apply(new ResetProperty(NESTED_ID,new PropertyName(name))).changed());
  var removed=apply(base,new RemoveWidget(NESTED_ID));assertAnalysis(base,removed,"probe.dart",true);assertArrayEquals(base.current().dartCandidateBytes(),removed.undo().session().current().dartCandidateBytes());
  Files.writeString(project.resolve("test/fade_in_image_test.dart"),runtime());run(List.of(flutter.toString(),"test","--reporter","expanded"),"flutter-test.log");
 }
 private void record(String name,DesignerCommandSession session,String mode)throws Exception{saveCase(name,session);cases.put(name,mode);}
 private static PropertyValue.DartObjectReferenceValue ref(String name){return new PropertyValue.DartObjectReferenceValue(Optional.empty(),name,Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());}
 private static PropertyValue.DartObjectReferenceValue reference(String field,boolean imported,boolean member,boolean factory){
  return new PropertyValue.DartObjectReferenceValue(imported?Optional.of("package:fade_in_image_contract/references.dart"):Optional.empty(),
   member?(imported?"Values":"Storage"):(imported?"":"_")+field+(factory?"Factory":""),
   member?Optional.of(field+(factory?"Factory":"")):Optional.empty(),
   factory?PropertyValue.DartObjectReferenceValue.Access.ZERO_ARGUMENT_INVOCATION:PropertyValue.DartObjectReferenceValue.Access.REFERENCE,factory?Optional.of(false):Optional.empty());
 }
 private String nativeRuntime(){return """
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class ControlledStream extends ImageStreamCompleter {
  void completeImage(ImageInfo image) => setImage(image);
}

class ControlledProvider extends ImageProvider<ControlledProvider> {
  ControlledProvider();
  final stream = ControlledStream();
  @override
  Future<ControlledProvider> obtainKey(ImageConfiguration config) =>
      SynchronousFuture(this);
  @override
  ImageStreamCompleter loadImage(
    ControlledProvider key,
    ImageDecoderCallback decode,
  ) => stream;
}

Future<ui.Image> makeImage(WidgetTester tester, Color color) async =>
    (await tester.runAsync(() async {
      final recorder = ui.PictureRecorder();
      Canvas(
        recorder,
      ).drawRect(const Rect.fromLTWH(0, 0, 8, 8), Paint()..color = color);
      final picture = recorder.endRecording();
      try {
        return await picture.toImage(8, 8);
      } finally {
        picture.dispose();
      }
    }))!;
Widget host(Widget child) => MaterialApp(home: Center(child: child));
Finder images() =>
    find.descendant(of: find.byType(FadeInImage), matching: find.byType(Image));
void nativeTests() {
  testWidgets(
    'native pending placeholder then sequential fades and gapless replacement',
    (tester) async {
      final p = ControlledProvider(),
          i = ControlledProvider(),
          next = ControlledProvider();
      final pImage = await makeImage(tester, Colors.red),
          iImage = await makeImage(tester, Colors.blue);
      p.stream.completeImage(ImageInfo(image: pImage));
      Widget view(ImageProvider<Object> image) => host(
        FadeInImage(
          key: const ValueKey('stable'),
          placeholder: p,
          image: image,
          width: 48,
          height: 32,
          fadeOutDuration: const Duration(milliseconds: 100),
          fadeInDuration: const Duration(milliseconds: 200),
          fadeOutCurve: Curves.linear,
          fadeInCurve: Curves.linear,
        ),
      );
      await tester.pumpWidget(view(i));
      final state = tester.state(find.byType(FadeInImage));
      List<Image> current() => tester.widgetList<Image>(images()).toList();
      expect(current(), hasLength(2));
      expect(current().singleWhere((n) => n.image == p).opacity!.value, 1);
      i.stream.completeImage(ImageInfo(image: iImage));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 50));
      expect(
        current().singleWhere((n) => n.image == p).opacity!.value,
        closeTo(.5, .03),
      );
      expect(current().singleWhere((n) => n.image == i).opacity!.value, 0);
      await tester.pump(const Duration(milliseconds: 100));
      expect(
        current().singleWhere((n) => n.image == i).opacity!.value,
        closeTo(.25, .03),
      );
      await tester.pump(const Duration(milliseconds: 151));
      await tester.pump();
      expect(images(), findsOneWidget);
      await tester.pumpWidget(view(next));
      expect(tester.state(find.byType(FadeInImage)), same(state));
      expect(current().single.gaplessPlayback, true);
      expect(current().single.excludeFromSemantics, true);
      expect(
        tester
            .widget<RawImage>(
              find.descendant(
                of: find.byType(FadeInImage),
                matching: find.byType(RawImage),
              ),
            )
            .image,
        isNotNull,
      );
      await tester.pumpWidget(const SizedBox());
      PaintingBinding.instance.imageCache.clear();
      PaintingBinding.instance.imageCache.clearLiveImages();
      expect(p.stream.hasListeners, false);
      expect(i.stream.hasListeners, false);
      expect(next.stream.hasListeners, false);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'synchronous cached target skips placeholder and preserves one image semantic label',
    (tester) async {
      final p = ControlledProvider(), i = ControlledProvider();
      i.stream.completeImage(
        ImageInfo(image: await makeImage(tester, Colors.blue)),
      );
      final handle = tester.ensureSemantics();
      try {
        await tester.pumpWidget(
          host(
            FadeInImage(
              placeholder: p,
              image: i,
              imageSemanticLabel: 'Target',
              width: 32,
              height: 32,
            ),
          ),
        );
        expect(images(), findsOneWidget);
        expect(p.stream.hasListeners, false);
        expect(find.bySemanticsLabel('Target'), findsOneWidget);
        final sem = tester
            .getSemantics(find.bySemanticsLabel('Target'))
            .getSemanticsData();
        expect(sem.flagsCollection.isImage, true);
        expect(sem.hasAction(ui.SemanticsAction.tap), false);
        await tester.pumpWidget(
          host(
            FadeInImage(
              placeholder: p,
              image: i,
              imageSemanticLabel: 'Target',
              excludeFromSemantics: true,
              width: 32,
              height: 32,
            ),
          ),
        );
        expect(find.bySemanticsLabel('Target'), findsNothing);
        await tester.pumpWidget(const SizedBox());
      } finally {
        handle.dispose();
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
      }
    },
  );
  testWidgets('placeholder and target error builders are independent', (
    tester,
  ) async {
    final p = ControlledProvider(), i = ControlledProvider();
    int pCalls = 0, iCalls = 0;
    await tester.pumpWidget(
      host(
        FadeInImage(
          placeholder: p,
          image: i,
          width: 32,
          height: 32,
          placeholderErrorBuilder: (c, e, s) {
            pCalls++;
            return const Text('placeholder failed');
          },
          imageErrorBuilder: (c, e, s) {
            iCalls++;
            return const Text('target failed');
          },
        ),
      ),
    );
    p.stream.reportError(
      context: ErrorDescription('placeholder'),
      exception: StateError('p'),
      silent: true,
    );
    await tester.pump();
    expect(pCalls, greaterThan(0));
    expect(iCalls, 0);
    expect(find.text('placeholder failed'), findsOneWidget);
    i.stream.reportError(
      context: ErrorDescription('target'),
      exception: StateError('i'),
      silent: true,
    );
    await tester.pump();
    expect(iCalls, greaterThan(0));
    expect(find.text('target failed'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
  });
  testWidgets(
    'named factory composition is representable by the general constructor',
    (tester) async {
      final asset = FadeInImage.assetNetwork(
        placeholder: 'assets/a.png',
        image: 'https://example.invalid/image.png',
        placeholderScale: 2,
        imageScale: 3,
        placeholderCacheWidth: 10,
        imageCacheHeight: 20,
      );
      expect(asset.placeholder, isA<ResizeImage>());
      expect(
        (asset.placeholder as ResizeImage).imageProvider,
        isA<ExactAssetImage>(),
      );
      expect(
        ((asset.placeholder as ResizeImage).imageProvider as ExactAssetImage)
            .scale,
        2,
      );
      expect((asset.image as ResizeImage).imageProvider, isA<NetworkImage>());
      expect(
        ((asset.image as ResizeImage).imageProvider as NetworkImage).scale,
        3,
      );
      final memory = FadeInImage.memoryNetwork(
        placeholder: Uint8List.fromList([1, 2, 3]),
        image: 'https://example.invalid/image.png',
        placeholderScale: 2,
      );
      expect(memory.placeholder, isA<MemoryImage>());
      expect((memory.placeholder as MemoryImage).scale, 2);
    },
  );
}

""";}
 private String runtime(){
  runtimeCaseCount=cases.size()*2+4;
  var imports=new StringBuilder();var tests=new StringBuilder();
  cases.forEach((name,mode)->{
   imports.append("import '../lib/").append(name).append(".dart' as case_").append(name).append(";\n");
   tests.append("for(final rtl in [false,true]){testWidgets('generated ").append(name).append(" rtl=$rtl',(tester)async{")
    .append("await tester.pumpWidget(host(Directionality(textDirection:rtl?TextDirection.rtl:TextDirection.ltr,child:const case_").append(name).append(".Sample())));await tester.pump();")
    .append("final n=tester.widget<FadeInImage>(find.byType(FadeInImage));expect(n.placeholder,isA<MemoryImage>());expect(n.image,isA<MemoryImage>());expect(find.text('Sibling'),findsOneWidget);");
   if(mode.equals("source"))tests.append("expect(n.fadeOutDuration,const Duration(milliseconds:10));expect(n.fadeInDuration,const Duration(milliseconds:20));expect(n.fadeOutCurve,Curves.linear);expect(n.fadeInCurve,Curves.easeIn);expect(n.width,48);expect(n.height,32);expect(n.color,const Color(0xFF112233));expect(n.placeholderColor,const Color(0xFF445566));expect(n.alignment,AlignmentDirectional.topEnd);expect(n.placeholderErrorBuilder,isNotNull);expect(n.imageErrorBuilder,isNotNull);");
   else if(mode.equals("local"))tests.append("expect(n.fadeOutDuration,const Duration(milliseconds:1));expect(n.fadeInDuration,const Duration(milliseconds:1));expect(n.fadeOutCurve,Curves.easeIn);expect(n.fadeInCurve,Curves.easeIn);expect(n.width,48);expect(n.height,32);expect(n.color,const Color(0xFF112233));expect(n.placeholderColor,const Color(0xFF445566));expect(n.fit,BoxFit.cover);expect(n.placeholderFit,BoxFit.contain);expect(n.filterQuality,FilterQuality.high);expect(n.placeholderFilterQuality,FilterQuality.low);expect(n.repeat,ImageRepeat.repeatX);expect(n.alignment,AlignmentDirectional.topEnd);expect(n.colorBlendMode,BlendMode.multiply);expect(n.placeholderColorBlendMode,BlendMode.srcIn);expect(n.excludeFromSemantics,true);expect(n.matchTextDirection,true);expect(n.imageSemanticLabel,'Photo');");
   else tests.append("expect(n.fadeOutDuration,const Duration(milliseconds:300));expect(n.fadeInDuration,const Duration(milliseconds:700));expect(n.fadeOutCurve,Curves.easeOut);expect(n.fadeInCurve,Curves.easeIn);expect(n.placeholderErrorBuilder,isNull);expect(n.imageErrorBuilder,isNull);");
   tests.append("expect(tester.takeException(),isNull);await tester.pumpWidget(const SizedBox());});}\n");
  });
  return imports+nativeRuntime()+"\nvoid main(){nativeTests();"+tests+"}\n";
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
                name: fade_in_image_contract
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
            System.out.println("FadeInImage pinned SDK: "+runtimeCaseCount+" generated/native runtime cases passed.");
        }
    }
}
