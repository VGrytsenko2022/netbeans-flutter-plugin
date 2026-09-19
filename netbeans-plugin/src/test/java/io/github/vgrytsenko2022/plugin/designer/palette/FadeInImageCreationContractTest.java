package io.github.vgrytsenko2022.plugin.designer.palette;
import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.*;
import io.github.vgrytsenko2022.plugin.designer.properties.FlutterImageAssetChoices;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FadeInImageCreationContractTest {
 @Test void noAssetsStillCreatesOnceAndDeclaredAssetsInitializeBothIndependentFields(){
  var definition=BuiltInWidgetCatalog.getDefault().find(FadeInImageWidgetPropertySchema.TYPE).orElseThrow();
  for(boolean available:List.of(false,true)){
   var choices=available?new FlutterImageAssetChoices(List.of(new FlutterImageAssetChoices.Choice(Optional.of("pictures"),"assets/a.png","A")),Optional.empty()):FlutterImageAssetChoices.empty();
   var allocations=new AtomicInteger();
   var node=FlutterImageWidgetCreationValues.createPrototype(definition,choices,()->{allocations.incrementAndGet();return StableId.random();});
   assertEquals(1,allocations.get());assertEquals(Set.of(new PropertyName("placeholder"),new PropertyName("image")),node.properties().keySet());
   for(var value:node.properties().values()){
    var provider=assertInstanceOf(PropertyValue.ImageProviderValue.class,value);assertEquals(!available,provider.isUnresolved());
    if(available){assertEquals("assets/a.png",provider.assetName());assertEquals(Optional.of("pictures"),provider.packageName());}
   }
  }
 }
}
