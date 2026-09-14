import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_default_text_style_test.dart' as s;
import 'canvas_model_test.dart' as m;
import 'canvas_sliver_fill_remaining_test.dart' as f;

const type = 'flutter.widgets.DefaultTextStyle';
const mergeType = '$type.merge';
Map<String,Object?> data({bool merge=false,bool rtl=false,Map<String,Object?> properties=const{},bool outer=false}) {
  final raw=s.data(rtl:rtl);
  final node=a.builder(raw);
  node['type']=merge?mergeType:type;
  node['properties']=<String,Object?>{if(!merge)'style':s.string('local'),...properties};
  if(outer) {
    final child=Map<String,Object?>.from(node);
    node
      ..clear()
      ..addAll(f.node('ae6e434b-3dcd-445b-9255-0328a7393959',type,{
        'style':s.string('local'),'styleColor':{'kind':'color','argb':'0xFF123456'},
        'styleFontSize':f.number(24),'softWrap':s.boolean(false),'overflow':s.en('TextOverflow','fade'),
        'maxLines':{'kind':'integer','value':3},'textAlign':s.en('TextAlign','right'),
        'textWidthBasis':s.en('TextWidthBasis','longestLine'),
        'textHeightApplyFirstAscent':s.boolean(false),'textHeightApplyLastDescent':s.boolean(false),
      },{'child':f.single(child)}));
  }
  return raw;
}
DefaultTextStyle target(WidgetTester t)=>DefaultTextStyle.of(t.element(find.text('Styled')));
void main(){
 for(final merge in [false,true]) {
  final name=merge?mergeType:type;
  test('$name required child and style domains',(){
    final missing=data(merge:merge);
    (a.builder(missing)['properties'] as Map).remove('style');
    if(merge) {expect(()=>f.decode(missing),returnsNormally);}
    else {expect(()=>f.decode(missing),throwsFormatException);}
    a.builder(missing)['slots']={'child':f.single(null)};
    expect(()=>f.decode(missing),throwsFormatException);
    for(final v in [0,-1,9007199254740992,1.5]){
      expect(()=>f.decode(data(merge:merge,properties:{'maxLines':{'kind':'integer','value':v}})),throwsFormatException);
    }
    for(final field in ['style','softWrap','overflow','textWidthBasis']){
      void check() { f.decode(data(merge:merge,properties:{field:{'kind':'null'}})); }
      if(merge){expect(check,returnsNormally);}else{expect(check,throwsFormatException);}
    }
    for(final p in [
      {'style':{'kind':'dartObjectReferencePresence'},'styleFontSize':f.number(20)},
      {'styleColor':{'kind':'color','argb':'0xFF123456'},'styleForeground':s.paint()},
      {'styleBackgroundColor':{'kind':'color','argb':'0xFF123456'},'styleBackground':s.paint()},
      {'stylePackage':s.string('fonts')},
      {'textHeightBehavior':{'kind':'null'},'textHeightApplyFirstAscent':s.boolean(false)},
    ]) {
      expect(()=>f.decode(data(merge:merge,properties:{'style':s.string('local'),...p})),throwsFormatException);
    }
    if(merge){
      expect(()=>f.decode(data(merge:true,properties:{'styleFontSize':f.number(20)})),throwsFormatException);
      expect(()=>f.decode(data(merge:true,properties:{'style':{'kind':'null'},'styleFontSize':f.number(20)})),throwsFormatException);
    }
  });
  for(final rtl in [false,true]){
    testWidgets('$name replacement versus merge rtl=$rtl',(t)async{
      await f.pump(t,data(merge:merge,rtl:rtl,outer:true));
      final n=target(t);
      expect(n.style.fontSize,merge?24:null);expect(n.style.color,merge?const Color(0xFF123456):null);
      expect(n.softWrap,!merge);expect(n.overflow,merge?TextOverflow.fade:TextOverflow.clip);
      expect(n.maxLines,merge?3:null);expect(n.textAlign,merge?TextAlign.right:null);
      expect(n.textWidthBasis,merge?TextWidthBasis.longestLine:TextWidthBasis.parent);
      expect(n.textHeightBehavior?.applyHeightToFirstAscent,merge?false:null);
      expect(t.takeException(),isNull);
    });
    testWidgets('$name local style paragraph height and instant edit rtl=$rtl',(t)async{
      final p=<String,Object?>{'style':s.string('local'),'styleFontSize':f.number(22),
        'styleFontWeight':s.en('FontWeight','w700'),'styleDecorationUnderline':s.boolean(true),
        'styleLocaleLanguageCode':s.string('uk'),'styleLocaleScriptCode':s.string('Cyrl'),
        'styleLocaleCountryCode':s.string('UA'),'textAlign':s.en('TextAlign','end'),
        'textHeightApplyFirstAscent':s.boolean(false)};
      await f.pump(t,data(merge:merge,rtl:rtl,outer:true,properties:p));
      final n=target(t),element=t.element(find.text('Styled'));
      expect(n.style.fontSize,22);expect(n.style.color,merge?const Color(0xFF123456):null);
      expect(n.style.fontWeight,FontWeight.w700);expect(n.style.decoration,TextDecoration.underline);
      expect(n.style.locale,const Locale.fromSubtags(languageCode:'uk',scriptCode:'Cyrl',countryCode:'UA'));
      expect(n.textAlign,TextAlign.end);
      // TextHeightBehavior is replaced as a whole; the missing leaf does not inherit false.
      expect(n.textHeightBehavior?.applyHeightToFirstAscent,false);
      expect(n.textHeightBehavior?.applyHeightToLastDescent,true);
      await f.pump(t,data(merge:merge,rtl:rtl,outer:true,properties:{...p,'styleFontSize':f.number(36)}));
      expect(target(t).style.fontSize,36);expect(identical(element,t.element(find.text('Styled'))),true);
      // Material may animate its own outer text theme; no Designer animation wrapper is added.
      expect(s.native(),findsNothing);
      expect(t.takeException(),isNull);
    });
  }
  testWidgets('$name full shared complex TextStyle projection',(t)async{
    final source=jsonDecode(utf8.decode(m.complexTextModelBytesForViewTest())) as Map<String,dynamic>;
    Map<String,dynamic>? locate(Object? v){
      if(v is Map<String,dynamic>){if(v['type']=='flutter.widgets.Text')return v;
        for(final x in v.values){final found=locate(x);if(found!=null)return found;}}
      else if(v is List){for(final x in v){final found=locate(x);if(found!=null)return found;}}
      return null;
    }
    final props=locate(source)!['properties'] as Map<String,dynamic>;
    await f.pump(t,data(merge:merge,properties:{'style':s.string('local'),
      for(final e in props.entries)if(e.key.startsWith('style'))e.key:e.value}));
    final n=target(t).style;
    expect(n.fontSize,21);expect(n.foreground?.style,PaintingStyle.stroke);expect(n.foreground?.strokeWidth,2.5);
    expect(n.background?.color.toARGB32(),0x22112233);expect(n.shadows,hasLength(2));
    expect(n.fontFeatures?.first.feature,'liga');expect(n.fontVariations?.first.axis,'wght');
    expect(n.fontVariations?.first.value,700);expect(t.takeException(),isNull);
  });
  testWidgets('$name project code is inert with explicit preview fallback',(t)async{
    await f.pump(t,data(merge:merge,outer:true,properties:{
      for(final f in ['style','maxLines','textHeightBehavior'])f:{'kind':'dartObjectReferencePresence'}}));
    final n=target(t);
    expect(n.style.fontSize,merge?24:null);expect(n.maxLines,merge?3:null);
    expect(n.textHeightBehavior?.applyHeightToFirstAscent,merge?false:null);
    expect(find.byWidgetPredicate((w)=>w is Tooltip&&w.message?.contains('DefaultTextStyle')==true),findsWidgets);
    expect(t.takeException(),isNull);
  });
  testWidgets('$name wraps occupied child via Canvas drop',(t)async{
    CanvasDropResolver? resolver;
    await f.pump(t,data(merge:merge),drop:(r)=>resolver=r);
    final bounds=t.getRect(find.byType(CanvasDocumentView)),point=t.getCenter(find.text('Styled'));
    final d=resolver!(((point.dx-bounds.left)/bounds.width*1000000).round(),
      ((point.dy-bounds.top)/bounds.height*1000000).round(),
      CanvasPaletteDragSource(token:'default-style',widgetType:name,traits:{}));
    expect(d?.parentWidgetId,a.builderId);expect(d?.slotName,'child');expect(d?.insertionIndex,0);
    expect(t.takeException(),isNull);
  });
  testWidgets('$name zero-sized text remains selectable',(t)async{
    await f.pump(t,data(merge:merge,properties:{'style':s.string('local'),'styleFontSize':f.number(20)}));
    final handle=find.byKey(const ValueKey('canvas-zero-size-widget-target-${a.builderId}'));
    expect(handle,findsNothing);
    await f.pump(t,data(merge:merge,properties:{'style':s.string('local'),'styleFontSize':f.number(0)}));
    await t.pumpAndSettle();expect(handle,findsOneWidget);
    await f.pump(t,data(merge:merge,properties:{'style':s.string('local'),'styleFontSize':f.number(20)}));
    await t.pumpAndSettle();expect(handle,findsNothing);expect(t.takeException(),isNull);
  });
 }
 testWidgets('merge null and omission inherit; inherit false replaces parent style',(t)async{
   await f.pump(t,data(merge:true,outer:true,properties:{
     for(final field in ['style','textAlign','softWrap','overflow','maxLines','textWidthBasis','textHeightBehavior'])
       field:{'kind':'null'}}));
   expect(target(t).maxLines,3);expect(target(t).style.fontSize,24);expect(target(t).softWrap,false);
   await f.pump(t,data(merge:true,outer:true,properties:{
     'style':s.string('local'),'styleInherit':s.boolean(false),'styleFontSize':f.number(18)}));
   expect(target(t).style.inherit,false);expect(target(t).style.fontSize,18);expect(target(t).style.color,isNull);
   expect(target(t).maxLines,3);expect(t.takeException(),isNull);
 });
}
