import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as m;
import 'package:flutter_test/flutter_test.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;

const type='flutter.widgets.AnimatedDefaultTextStyle';
Map<String,Object?> string(String s)=>{'kind':'string','value':s};
Map<String,Object?> en(String t,String v)=>{'kind':'enum','type':t,'value':v};
Map<String,Object?> boolean(bool b)=>{'kind':'boolean','value':b};
Map<String,Object?> paint()=>{'kind':'paint','color':{'kind':'literal','argb':'0xFF123456'},
 'blendMode':'srcOver','style':'fill','strokeWidth':0.0,'strokeCap':'round','strokeJoin':'bevel',
 'strokeMiterLimit':4.0,'antiAlias':true,'filterQuality':'medium','invertColors':false};
Map<String,Object?> data({Map<String,Object?> properties=const{},bool rtl=false}){
 final raw=c.data(rtl:rtl);a.builder(raw)['type']=type;
 (a.builder(raw)['properties'] as Map).addAll(<String,Object?>{'style':string('local'),...properties});
 a.builder(raw)['slots']={'child':f.single(f.node(f.bodyId,'flutter.widgets.Text',{'data':string('Styled')}))};
 return raw;
}
Finder native()=>find.byWidgetPredicate((w)=>w is AnimatedDefaultTextStyle && w.key is GlobalKey);
AnimatedDefaultTextStyle target(WidgetTester t)=>t.widget<AnimatedDefaultTextStyle>(native());
TextStyle current(WidgetTester t)=>DefaultTextStyle.of(t.element(find.text('Styled'))).style;
void main(){
 testWidgets('complex shared style fields Paint shadows features and variations',(t)async{
   final source=jsonDecode(utf8.decode(m.complexTextModelBytesForViewTest())) as Map<String,dynamic>;
   Map<String,dynamic>? locate(Object? value){
     if(value is Map<String,dynamic>){
       if(value['type']=='flutter.widgets.Text')return value;
       for(final v in value.values){final found=locate(v);if(found!=null)return found;}
     }else if(value is List){for(final v in value){final found=locate(v);if(found!=null)return found;}}
     return null;
   }
   final p=(locate(source)!['properties'] as Map<String,dynamic>);
   await f.pump(t,data(properties:{for(final e in p.entries)if(e.key.startsWith('style'))e.key:e.value}));
   final s=target(t).style;
   expect(s.fontSize,21);expect(s.foreground?.style,PaintingStyle.stroke);expect(s.foreground?.strokeWidth,2.5);
   expect(s.background?.color.toARGB32(),0x22112233);expect(s.shadows,hasLength(2));
   expect(s.fontFeatures?.first.feature,'liga');expect(s.fontVariations?.first.axis,'wght');
   expect(s.fontVariations?.first.value,700);expect(t.takeException(),isNull);
 });
 testWidgets('required-child palette drop wraps a real occupied Child',(t)async{
   CanvasDropResolver? resolver;
   await f.pump(t,data(properties:{'styleFontSize':f.number(20)}),drop:(r)=>resolver=r);
   final surface=t.getRect(find.byType(CanvasDocumentView)),point=t.getCenter(find.text('Styled'));
   final drop=resolver!(((point.dx-surface.left)/surface.width*1000000).round(),
      ((point.dy-surface.top)/surface.height*1000000).round(),
      CanvasPaletteDragSource(token:'style-wrapper',widgetType:type,traits:{}));
   expect(drop?.parentWidgetId,a.builderId);expect(drop?.slotName,'child');expect(drop?.insertionIndex,0);
   expect(t.takeException(),isNull);
 });
 testWidgets('text shrinking to zero remains selectable and recovers',(t)async{
   await f.pump(t,data(properties:{'styleFontSize':f.number(20)}));
   final handle=find.byKey(const ValueKey('canvas-zero-size-widget-target-${a.builderId}'));
   expect(handle,findsNothing);
   await f.pump(t,data(properties:{'styleFontSize':f.number(0)}));await t.pumpAndSettle();
   expect(handle,findsOneWidget);
   await f.pump(t,data(properties:{'styleFontSize':f.number(20)}));await t.pumpAndSettle();
   expect(handle,findsNothing);expect(t.takeException(),isNull);
 });

 test('required domains and whole/local relationships are closed',(){
   for(final field in ['style','durationUs']){
     final raw=data();(a.builder(raw)['properties'] as Map).remove(field);
     expect(()=>f.decode(raw),throwsFormatException);
   }
   final raw=data();a.builder(raw)['slots']={'child':f.single(null)};
   expect(()=>f.decode(raw),throwsFormatException);
   for(final bad in [0,-1,9007199254740992,1.5]){
     expect(()=>f.decode(data(properties:{'maxLines':{'kind':'integer','value':bad}})),throwsFormatException);
   }
   for(final p in [
     {'style':{'kind':'dartObjectReferencePresence'},'styleFontSize':f.number(20)},
     {'styleColor':{'kind':'color','argb':'0xFF123456'},'styleForeground':paint()},
     {'styleBackgroundColor':{'kind':'color','argb':'0xFF123456'},'styleBackground':paint()},
     {'stylePackage':string('fonts')},
     {'textHeightBehavior':{'kind':'null'},'textHeightApplyFirstAscent':boolean(false)},
   ]){expect(()=>f.decode(data(properties:p)),throwsFormatException);}
 });
 testWidgets('exact local paragraph fields and no invented outer style merge',(t)async{
   await f.pump(t,data(properties:{
     'textAlign':en('TextAlign','end'),'softWrap':boolean(false),'overflow':en('TextOverflow','fade'),
     'maxLines':{'kind':'integer','value':2},'textWidthBasis':en('TextWidthBasis','longestLine'),
     'textHeightApplyFirstAscent':boolean(false),'textHeightApplyLastDescent':boolean(false),
     'textHeightLeadingDistribution':en('TextLeadingDistribution','even'),
     'styleInherit':boolean(false),'styleFontSize':f.number(22),'styleFontWeight':en('FontWeight','w700'),
     'styleLetterSpacing':f.number(1),'styleWordSpacing':f.number(2),
     'styleFontStyle':en('FontStyle','italic'),'styleTextBaseline':en('TextBaseline','ideographic'),
     'styleHeight':f.number(1.4),'styleLeadingDistribution':en('TextLeadingDistribution','even'),
     'styleLocaleLanguageCode':string('uk'),'styleLocaleScriptCode':string('Cyrl'),'styleLocaleCountryCode':string('UA'),
     'styleDecorationUnderline':boolean(true),'styleDecorationStyle':en('TextDecorationStyle','dotted'),
     'styleDecorationThickness':f.number(2),'styleOverflow':en('TextOverflow','ellipsis'),
     'styleDebugLabel':string('designer'),
   },rtl:true));
   final n=target(t);expect(n.style.inherit,false);expect(n.style.fontSize,22);expect(n.style.fontWeight,FontWeight.w700);
   expect(n.style.color,isNull);expect(n.style.locale,const Locale.fromSubtags(languageCode:'uk',scriptCode:'Cyrl',countryCode:'UA'));
   expect(n.style.decoration,TextDecoration.underline);expect(n.style.decorationStyle,TextDecorationStyle.dotted);
   expect(n.textAlign,TextAlign.end);expect(n.softWrap,false);expect(n.overflow,TextOverflow.fade);expect(n.maxLines,2);
   expect(n.textWidthBasis,TextWidthBasis.longestLine);expect(n.textHeightBehavior?.applyHeightToLastDescent,false);
   expect(current(t),n.style);expect(t.takeException(),isNull);
 });
 testWidgets('whole references are presence-only with explicit fallback',(t)async{
   await f.pump(t,data(properties:{for(final field in ['style','textHeightBehavior','maxLines','curve','durationUs','onEnd'])
     field:{'kind':'dartObjectReferencePresence'}}));
   final n=target(t);expect(n.style,const TextStyle());expect(n.maxLines,isNull);expect(n.textHeightBehavior,isNull);
   expect(n.curve,Curves.linear);expect(n.duration,const Duration(milliseconds:300));expect(n.onEnd,isNull);
   expect(find.byWidgetPredicate((w)=>w is Tooltip&&w.message?.contains('AnimatedDefaultTextStyle')==true),findsWidgets);
   expect(t.takeException(),isNull);
 });
 for(final curve in <String,Curve>{
    'linear': Curves.linear,
    'decelerate': Curves.decelerate,
    'fastLinearToSlowEaseIn': Curves.fastLinearToSlowEaseIn,
    'fastEaseInToSlowEaseOut': Curves.fastEaseInToSlowEaseOut,
    'ease': Curves.ease,
    'easeIn': Curves.easeIn,
    'easeInToLinear': Curves.easeInToLinear,
    'easeInSine': Curves.easeInSine,
    'easeInQuad': Curves.easeInQuad,
    'easeInCubic': Curves.easeInCubic,
    'easeInQuart': Curves.easeInQuart,
    'easeInQuint': Curves.easeInQuint,
    'easeInExpo': Curves.easeInExpo,
    'easeInCirc': Curves.easeInCirc,
    'easeInBack': Curves.easeInBack,
    'easeOut': Curves.easeOut,
    'linearToEaseOut': Curves.linearToEaseOut,
    'easeOutSine': Curves.easeOutSine,
    'easeOutQuad': Curves.easeOutQuad,
    'easeOutCubic': Curves.easeOutCubic,
    'easeOutQuart': Curves.easeOutQuart,
    'easeOutQuint': Curves.easeOutQuint,
    'easeOutExpo': Curves.easeOutExpo,
    'easeOutCirc': Curves.easeOutCirc,
    'easeOutBack': Curves.easeOutBack,
    'easeInOut': Curves.easeInOut,
    'easeInOutSine': Curves.easeInOutSine,
    'easeInOutQuad': Curves.easeInOutQuad,
    'easeInOutCubic': Curves.easeInOutCubic,
    'easeInOutCubicEmphasized': Curves.easeInOutCubicEmphasized,
    'easeInOutQuart': Curves.easeInOutQuart,
    'easeInOutQuint': Curves.easeInOutQuint,
    'easeInOutExpo': Curves.easeInOutExpo,
    'easeInOutCirc': Curves.easeInOutCirc,
    'easeInOutBack': Curves.easeInOutBack,
    'fastOutSlowIn': Curves.fastOutSlowIn,
    'slowMiddle': Curves.slowMiddle,
    'bounceIn': Curves.bounceIn,
    'bounceOut': Curves.bounceOut,
    'bounceInOut': Curves.bounceInOut,
    'elasticIn': Curves.elasticIn,
    'elasticOut': Curves.elasticOut,
    'elasticInOut': Curves.elasticInOut,

 }.entries){
   testWidgets('native text tween ${curve.key} and paragraph immediate update',(t)async{
     final p={'curve':string(curve.key)};
     await f.pump(t,data(properties:{...p,'styleFontSize':f.number(20)}));
     final state=t.state(native());
     await f.pump(t,data(properties:{...p,'styleFontSize':f.number(40),'textAlign':en('TextAlign','right')}));
     expect(target(t).textAlign,TextAlign.right);
     for(final v in [.25,.5,.75,1.0]){
       await t.pump(const Duration(milliseconds:25));
       expect(current(t).fontSize,closeTo(20+20*curve.value.transform(v),.02));
     }
     expect(identical(t.state(native()),state),true);expect(t.takeException(),isNull);
   });
 }
 for(final change in ['inherit','paint','overshoot']){
   testWidgets('unsafe $change transition stays usable preserves child and reports limitation',(t)async{
      await f.pump(t,data(properties:{'styleFontSize':f.number(20)}));
      final element=t.element(find.text('Styled')),state=t.state(native());
      final props=<String,Object?>{
       if(change=='inherit')'styleInherit':boolean(false),
       if(change=='paint')'styleForeground':paint(),
       if(change=='overshoot')'curve':string('easeInBack'),
       'styleFontSize':f.number(change=='overshoot'?0:24),
      };
      if(change=='overshoot'){await f.pump(t,data(properties:{'styleFontSize':f.number(0)}));await t.pumpAndSettle();props['styleFontSize']=f.number(100);}
      await f.pump(t,data(properties:props));await t.pumpAndSettle();
      expect(t.takeException(),isNull);expect(identical(element,t.element(find.text('Styled'))),true);
      expect(identical(state,t.state(native())),false);
      expect(find.byWidgetPredicate((w)=>w is Tooltip&&w.message?.contains('Showing the target without this transition')==true),findsWidgets);
      expect(target(t).style.fontSize,change=='overshoot'?100:24);
   });
 }
 testWidgets('SDK onEnd initial interruption zero duration and inert Canvas',(t)async{
    var ended=0;Widget build(double size,{int ms=100})=>Directionality(textDirection:TextDirection.ltr,
      child:AnimatedDefaultTextStyle(style:TextStyle(fontSize:size),duration:Duration(milliseconds:ms),onEnd:()=>ended++,child:const Text('Runtime')));
    await t.pumpWidget(build(20));await t.pumpAndSettle();expect(ended,0);
    await t.pumpWidget(build(40));await t.pump(const Duration(milliseconds:50));expect(ended,0);
    await t.pumpWidget(build(30));await t.pumpAndSettle();expect(ended,1);
    await t.pumpWidget(build(50,ms:0));await t.pumpAndSettle();expect(ended,2);
    await f.pump(t,data(properties:{'onEnd':{'kind':'dartObjectReferencePresence'},'styleFontSize':f.number(20)}));
    await f.pump(t,data(properties:{'onEnd':{'kind':'dartObjectReferencePresence'},'styleFontSize':f.number(30)}));
    await t.pumpAndSettle();expect(target(t).onEnd,isNull);expect(ended,2);expect(t.takeException(),isNull);
 });
}
