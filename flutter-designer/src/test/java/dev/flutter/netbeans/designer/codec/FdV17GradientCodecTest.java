package dev.flutter.netbeans.designer.codec;
import dev.flutter.netbeans.designer.catalog.*;
import dev.flutter.netbeans.designer.model.*;
import dev.flutter.netbeans.designer.generation.DartRegionGenerator;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FdV17GradientCodecTest {
 private final FdDocumentCodec codec=new FdDocumentCodec();
 private static DesignerDocument document(WidgetNode root){
  var r=new ManagedRegion("0".repeat(64));
  return new DesignerDocument(Optional.of("urn:netbeans-flutter-designer:schema:fd:17"),StableId.random(),
    new DartSourceDescriptor("sample.dart","Sample",WidgetClassKind.STATEFUL,Optional.empty(),new ManagedRegions(r,r)),
    Optional.empty(),root,Extensions.empty());
 }
 private String json(DesignerDocument doc)throws Exception{return new String(codec.encode(doc).copyBytes(),StandardCharsets.UTF_8);}
 @Test void standaloneGradientRejectsUnknownMalformedAndLegacyEnvelopes()throws Exception{
  var value=document(ShaderMaskContractTest.node());var valid=json(value);
  assertTrue(valid.contains("\"kind\": \"gradient\""));assertTrue(valid.contains("\"gradient\":"));
  for(String bad:List.of(
    valid.replace("\"kind\": \"gradient\"","\"kind\": \"gradient\", \"extra\": true"),
    valid.replace("\"gradient\":","\"unknown\":"),
    valid.replace("\"kind\": \"linear\"","\"kind\": \"conic\""),
    valid.replace("\"tileMode\": \"clamp\"","\"tileMode\": \"invalid\""),
    valid.replace("\"stop\": 1","\"stop\": -1"))){
    assertNotEquals(valid,bad);assertInstanceOf(FdDecodeResult.Invalid.class,codec.decode(bad.getBytes(StandardCharsets.UTF_8)));
  }
  for(int version=1;version<17;version++){
    var old=valid.replace("\"schemaVersion\": 17","\"schemaVersion\": "+version);
    assertInstanceOf(FdDecodeResult.Invalid.class,codec.decode(old.getBytes(StandardCharsets.UTF_8)));
  }
  var future=valid.replace("\"schemaVersion\": 17","\"schemaVersion\": 18").getBytes(StandardCharsets.UTF_8);
  var newer=assertInstanceOf(FdDecodeResult.UnsupportedNewer.class,codec.decode(future));
  assertArrayEquals(future,newer.original().copyBytes());
 }
 @Test void nonGradientPalettePrototypeModelsMigrateWithoutChangingAnyGeneratedDart()throws Exception{
  var catalog=BuiltInWidgetCatalog.getDefault();var generator=new DartRegionGenerator();int checked=0;
  for(var definition:catalog.definitions()){
    if(definition.typeId().equals(ShaderMaskWidgetPropertySchema.TYPE))continue;
    var values = new LinkedHashMap<PropertyName, PropertyValue>();
    for (var property : definition.properties()) {
      if (property.parameter().required() && property.creationDefault().isEmpty()
          && property.acceptedKinds().contains(PropertyValueKind.IMAGE_PROVIDER))
        values.put(property.name(), PropertyValue.ImageProviderValue.asset("assets/test.png"));
    }
    var node=WidgetNodePrototypeFactory.create(definition,StableId.random(),values);
    var original=document(node);var bytes=json(original).replace("\"schemaVersion\": 17","\"schemaVersion\": 16")
      .replace("schema:fd:17","schema:fd:16").getBytes(StandardCharsets.UTF_8);
    var migrated=assertInstanceOf(FdDecodeResult.Current.class,codec.decode(bytes));
    assertTrue(migrated.migrated());assertEquals(16,migrated.sourceSchemaVersion());
    assertArrayEquals(bytes,migrated.original().copyBytes());assertEquals(original,migrated.document());
    var before=generator.generate(original,catalog);var after=generator.generate(migrated.document(),catalog);
    assertEquals(before,after,definition.typeId().value());checked++;
  }
  assertEquals(234,checked);
 }
}
