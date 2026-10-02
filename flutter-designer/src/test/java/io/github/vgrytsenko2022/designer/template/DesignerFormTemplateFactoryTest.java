package io.github.vgrytsenko2022.designer.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vgrytsenko2022.designer.codec.FdDecodeResult;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.model.PropertyName;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import io.github.vgrytsenko2022.designer.model.SlotName;
import io.github.vgrytsenko2022.designer.model.WidgetSlot;
import io.github.vgrytsenko2022.designer.model.WidgetClassKind;
import io.github.vgrytsenko2022.designer.source.DartSourceIntegrityScanner;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DesignerFormTemplateFactoryTest {
    private final DesignerFormTemplateFactory factory =
            new DesignerFormTemplateFactory();

    @Test
    void createsCanonicalScaffoldPairWithMatchingManagedHashes() throws Exception {
        DesignerFormTemplate template = factory.create(
                "order_screen.dart", "OrderScreen");

        FdDecodeResult.Current decoded = assertInstanceOf(
                FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(template.fdBytes()));
        assertEquals(template.document(), decoded.document());
        assertEquals("order_screen.dart", decoded.document().source().dartFile());
        assertEquals("OrderScreen", decoded.document().source().className());
        assertEquals(WidgetClassKind.STATELESS, decoded.document().source().widgetKind());
        assertTrue(new DartSourceIntegrityScanner()
                .scan(template.dartBytes(), decoded.document().source())
                .onDiskDeclaredMatch());

        assertEquals("flutter.material.Scaffold",
                decoded.document().root().type().value());
        WidgetSlot.SingleSlot body = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                decoded.document().root().slots().get(new SlotName("body")));
        WidgetSlot.SingleSlot child = assertInstanceOf(
                WidgetSlot.SingleSlot.class,
                body.child().orElseThrow().slots().get(new SlotName("child")));
        PropertyValue.StringValue text = assertInstanceOf(
                PropertyValue.StringValue.class,
                child.child().orElseThrow().properties().get(new PropertyName("data")));
        assertEquals("Hello from NetBeans", text.value());

        String dart = new String(template.dartBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("class OrderScreen extends StatelessWidget"));
        assertTrue(dart.contains("Scaffold("));
    }

    @Test
    void rejectsUnsafeOrNonDartSourceNamesAndInvalidClassNames() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create("../order.dart", "OrderScreen"));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create("order.txt", "OrderScreen"));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create("order_screen.dart", "orderScreen"));
    }

    @Test
    void returnsDefensiveByteCopiesAndFreshStableIds() throws Exception {
        DesignerFormTemplate first = factory.create("home.dart", "Home");
        DesignerFormTemplate second = factory.create("home.dart", "Home");
        assertNotEquals(first.document().documentId(), second.document().documentId());

        byte[] dart = first.dartBytes();
        byte[] fd = first.fdBytes();
        dart[0] ^= 1;
        fd[0] ^= 1;
        assertNotEquals(dart[0], first.dartBytes()[0]);
        assertNotEquals(fd[0], first.fdBytes()[0]);
    }

    @Test
    void createsExplicitStatefulPairWithBuildOwnedBySeparateStateClass() throws Exception {
        DesignerFormTemplate template = factory.create("order_screen.dart", "OrderScreen", WidgetClassKind.STATEFUL);
        FdDecodeResult.Current decoded = assertInstanceOf(FdDecodeResult.Current.class,
                new FdDocumentCodec().decode(template.fdBytes()));
        assertEquals(template.document(), decoded.document());
        assertEquals(WidgetClassKind.STATEFUL, decoded.document().source().widgetKind());
        String dart = new String(template.dartBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("class OrderScreen extends StatefulWidget {\n  const OrderScreen({super.key});"));
        assertTrue(dart.contains("@override\n  State<OrderScreen> createState() => _OrderScreenState();"));
        assertTrue(dart.contains("class _OrderScreenState extends State<OrderScreen> {\n"
                + "  // <netbeans-flutter-designer region=\"build\">"));
        assertEquals(1, dart.split("Widget build\\(BuildContext context\\)", -1).length - 1);
        var integrity = new DartSourceIntegrityScanner().scan(template.dartBytes(), decoded.document().source());
        assertTrue(integrity.onDiskDeclaredMatch(), integrity.diagnostics().toString());
        assertEquals(factory.create("other.dart", "Other").document().source().managedRegions(),
                template.document().source().managedRegions(), "Class kind must not alter identical managed payload hashes.");
    }

    @Test
    void supportsPrivateStatefulOwnerAndRejectsMissingKind() throws Exception {
        var template = factory.create("_private.dart", "_Private", WidgetClassKind.STATEFUL);
        String dart = new String(template.dartBytes(), StandardCharsets.UTF_8);
        assertTrue(dart.contains("State<_Private> createState() => _PrivateState();"));
        assertTrue(dart.contains("class _PrivateState extends State<_Private>"));
        assertThrows(NullPointerException.class, () -> factory.create("screen.dart", "Screen", null));
    }
}
