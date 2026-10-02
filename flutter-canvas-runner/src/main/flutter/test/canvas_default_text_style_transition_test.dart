import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_default_text_style_test.dart' as s;
import 'canvas_default_text_style_test.dart' as d;
import 'canvas_model_test.dart' as m;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type='flutter.widgets.DefaultTextStyleTransition';
Map<String,Object?> data({Map<String,Object?> properties=const{},bool rtl=false,bool outer=false}) {
  final raw=d.data(properties:properties,rtl:rtl,outer:outer);
  a.builder(raw)['type']=type;
  return raw;
}
Finder native()=>find.byType(DefaultTextStyleTransition);
DefaultTextStyleTransition target(WidgetTester t)=>t.widget<DefaultTextStyleTransition>(native());
DefaultTextStyle inherited(WidgetTester t)=>DefaultTextStyle.of(t.element(find.text('Styled')));
void main(){
  test('complete domains admit animation presence and reject unsupported fields',(){
    final missing=data();(a.builder(missing)['properties'] as Map).remove('style');
    expect(()=>f.decode(missing),throwsFormatException);
    final empty=data();a.builder(empty)['slots']={'child':f.single(null)};
    expect(()=>f.decode(empty),throwsFormatException);
    for(final field in ['style','softWrap','overflow']){
      expect(()=>f.decode(data(properties:{field:{'kind':'null'}})),throwsFormatException);
    }
    for(final field in ['textAlign','maxLines']){
      expect(()=>f.decode(data(properties:{field:{'kind':'null'}})),returnsNormally);
    }
    for(final field in ['textWidthBasis','textHeightBehavior','textHeightApplyFirstAscent','durationUs','curve','onEnd','listenable']){
      expect(()=>f.decode(data(properties:{field:{'kind':'null'}})),throwsFormatException);
    }
    for(final bad in [0,-1,1.5,9007199254740992]){
      expect(()=>f.decode(data(properties:{'maxLines':{'kind':'integer','value':bad}})),throwsFormatException);
    }
    for(final p in [
      {'style':{'kind':'dartObjectReferencePresence'},'styleFontSize':f.number(20)},
      {'styleColor':{'kind':'color','argb':'0xFF123456'},'styleForeground':s.paint()},
      {'styleBackgroundColor':{'kind':'color','argb':'0xFF123456'},'styleBackground':s.paint()},
      {'stylePackage':s.string('fonts')},
    ]){expect(()=>f.decode(data(properties:p)),throwsFormatException);}
  });
  for(final rtl in [false,true]) {
    testWidgets('stopped default replaces inherited style rtl=$rtl',(t)async{
      await f.pump(t,data(outer:true,rtl:rtl));
      final n=target(t),v=inherited(t);
      expect(n.style,isA<AlwaysStoppedAnimation<TextStyle>>());expect(n.style.value,const TextStyle());
      expect(v.style,const TextStyle());expect(v.softWrap,true);expect(v.overflow,TextOverflow.clip);
      expect(v.maxLines,isNull);expect(v.textAlign,isNull);
      // The transition API does not forward either of these optional DefaultTextStyle arguments.
      expect(v.textWidthBasis,TextWidthBasis.parent);expect(v.textHeightBehavior,isNull);
      expect(t.takeException(),isNull);
    });
    testWidgets('all paragraph values update immediately while native State and child survive rtl=$rtl',(t)async{
      final p=<String,Object?>{'styleFontSize':f.number(20),'styleColor':{'kind':'color','argb':'0xFF123456'},
        'textAlign':s.en('TextAlign','end'),'softWrap':s.boolean(false),'overflow':s.en('TextOverflow','fade'),
        'maxLines':{'kind':'integer','value':2}};
      await f.pump(t,data(properties:p,rtl:rtl));
      final state=t.state(native()),child=t.element(find.text('Styled'));
      expect(inherited(t).style.fontSize,20);expect(inherited(t).textAlign,TextAlign.end);
      expect(inherited(t).softWrap,false);expect(inherited(t).overflow,TextOverflow.fade);expect(inherited(t).maxLines,2);
      await f.pump(t,data(properties:{...p,'styleFontSize':f.number(40),'maxLines':{'kind':'null'},'textAlign':{'kind':'null'}},rtl:rtl));
      expect(inherited(t).style.fontSize,40);expect(inherited(t).maxLines,isNull);expect(inherited(t).textAlign,isNull);
      expect(identical(state,t.state(native())),true);expect(identical(child,t.element(find.text('Styled'))),true);
      await t.pump(const Duration(milliseconds:500));expect(inherited(t).style.fontSize,40);
      expect(t.takeException(),isNull);
    });
    testWidgets('theme style token is evaluated before stopped animation rtl=$rtl',(t)async{
      await f.pump(t,data(properties:{'styleThemeTextStyle':{'kind':'themeToken','token':'material.textTheme.bodyLarge'}},rtl:rtl));
      expect(target(t).style.value.fontSize,isNotNull);expect(t.takeException(),isNull);
    });
  }
  testWidgets('complete shared complex TextStyle projection',(t)async{
    final source=jsonDecode(utf8.decode(m.complexTextModelBytesForViewTest())) as Map<String,dynamic>;
    Map<String,dynamic>? locate(Object? value){
      if(value is Map<String,dynamic>){
        if(value['type']=='flutter.widgets.Text')return value;
        for(final child in value.values){final result=locate(child);if(result!=null)return result;}
      }else if(value is List){for(final child in value){final result=locate(child);if(result!=null)return result;}}
      return null;
    }
    final p=locate(source)!['properties'] as Map<String,dynamic>;
    await f.pump(t,data(properties:{for(final e in p.entries)if(e.key.startsWith('style'))e.key:e.value}));
    final v=target(t).style.value;
    expect(v.fontSize,21);expect(v.foreground?.style,PaintingStyle.stroke);expect(v.foreground?.strokeWidth,2.5);
    expect(v.background?.color.toARGB32(),0x22112233);expect(v.shadows,hasLength(2));
    expect(v.fontFeatures?.first.feature,'liga');expect(v.fontVariations?.first.axis,'wght');
    expect(v.fontVariations?.first.value,700);expect(t.takeException(),isNull);
  });
  testWidgets('source animation remains inert and has an explicit stopped-empty fallback',(t)async{
    await f.pump(t,data(properties:{'style':{'kind':'dartObjectReferencePresence'},'maxLines':{'kind':'dartObjectReferencePresence'}},outer:true));
    final n=target(t);
    expect(n.style,isA<AlwaysStoppedAnimation<TextStyle>>());expect(n.style.value,const TextStyle());expect(n.maxLines,isNull);
    expect(find.byWidgetPredicate((w)=>w is Tooltip&&w.message?.contains('AlwaysStoppedAnimation<TextStyle>')==true),findsWidgets);
    await t.pump(const Duration(seconds:1));expect(inherited(t).style,const TextStyle());expect(t.takeException(),isNull);
  });
  testWidgets('required-child Canvas drop wraps occupied slot',(t)async{
    CanvasDropResolver? resolver;
    await f.pump(t,data(),drop:(r)=>resolver=r);
    final bounds=t.getRect(find.byType(CanvasDocumentView)),point=t.getCenter(find.text('Styled'));
    final result=resolver!(((point.dx-bounds.left)/bounds.width*1000000).round(),
      ((point.dy-bounds.top)/bounds.height*1000000).round(),
      CanvasPaletteDragSource(token:'style-transition',widgetType:type,traits:{}));
    expect(result?.parentWidgetId,a.builderId);expect(result?.slotName,'child');expect(result?.insertionIndex,0);
    expect(t.takeException(),isNull);
  });
  testWidgets('zero-size target remains selectable without replacing native State',(t)async{
    await f.pump(t,data(properties:{'styleFontSize':f.number(20)}));final state=t.state(native());
    final handle=find.byKey(const ValueKey('canvas-zero-size-widget-target-${a.builderId}'));
    expect(handle,findsNothing);
    await f.pump(t,data(properties:{'styleFontSize':f.number(0)}));await t.pumpAndSettle();
    expect(handle,findsOneWidget);expect(identical(state,t.state(native())),true);
    await f.pump(t,data(properties:{'styleFontSize':f.number(20)}));await t.pumpAndSettle();
    expect(handle,findsNothing);expect(identical(state,t.state(native())),true);expect(t.takeException(),isNull);
  });
}
