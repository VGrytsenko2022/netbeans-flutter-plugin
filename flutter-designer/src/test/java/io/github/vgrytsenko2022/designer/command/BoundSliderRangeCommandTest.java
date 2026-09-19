package io.github.vgrytsenko2022.designer.command;

import io.github.vgrytsenko2022.designer.catalog.BuiltInWidgetCatalog;
import io.github.vgrytsenko2022.designer.catalog.WidgetNodePrototypeFactory;
import io.github.vgrytsenko2022.designer.codec.FdDocumentCodec;
import io.github.vgrytsenko2022.designer.generation.DartRegionGenerator;
import io.github.vgrytsenko2022.designer.model.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundSliderRangeCommandTest {
    private static final StableId CONTROL = StableId.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final StableId DOCUMENT = StableId.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final PropertyName MIN = new PropertyName("min");
    private static final PropertyName MAX = new PropertyName("max");
    private static final List<String> TYPES = List.of("flutter.material.Slider", "flutter.material.RangeSlider");

    @Test
    void previewInsideNewBoundsDoesNotProveTheRetainedRuntimeInitializer() throws Exception {
        for (String type : TYPES) {
            DesignerCommandSession bound = bind(open(type, Map.of()));
            DesignerCommandSession preview = preview(bound, type);
            assertArrayEquals(bound.current().dartCandidateBytes(), preview.current().dartCandidateBytes(),
                    "Preview changes must not rewrite the initialized runtime field");
            assertRejected(preview, new SetProperty(CONTROL, MIN, number("0.5")), "/min");
            assertRejected(preview, new SetProperty(CONTROL, MAX, number("0.9")), "/max");
            assertRejected(preview, new SetProperty(CONTROL, MIN, number("-1")), "/min");
        }
    }

    @Test
    void restoreDefaultCannotNarrowAnExistingBoundRuntimeRange() throws Exception {
        for (String type : TYPES) {
            Map<PropertyName, PropertyValue> values = type.endsWith("RangeSlider")
                    ? Map.of(MIN, number("-1"), MAX, number("2"),
                            new PropertyName("valuesStart"), number("-0.5"), new PropertyName("valuesEnd"), number("1.5"))
                    : Map.of(MIN, number("-1"), MAX, number("2"), new PropertyName("value"), number("-0.5"));
            DesignerCommandSession preview = preview(bind(open(type, values)), type);
            assertRejected(preview, new ResetProperty(CONTROL, MIN), "/min");
            assertRejected(preview, new ResetProperty(CONTROL, MAX), "/max");
            assertRejected(preview, new PatchProperties(CONTROL, List.of(
                    new PatchProperties.ResetPatch(MIN), new PatchProperties.ResetPatch(MAX))), "/min");
        }
    }

    @Test
    void multiPropertyPatchCannotHideABoundRangeChangeBehindValidPreviews() throws Exception {
        for (String type : TYPES) {
            DesignerCommandSession bound = bind(open(type, Map.of()));
            List<PatchProperties.Patch> patch = type.endsWith("RangeSlider")
                    ? List.of(new PatchProperties.SetPatch(new PropertyName("valuesStart"), number("0.6")),
                            new PatchProperties.SetPatch(new PropertyName("valuesEnd"), number("0.8")),
                            new PatchProperties.SetPatch(MIN, number("0.5")))
                    : List.of(new PatchProperties.SetPatch(new PropertyName("value"), number("0.8")),
                            new PatchProperties.SetPatch(MIN, number("0.5")));
            assertRejected(bound, new PatchProperties(CONTROL, patch), "/min");
        }
    }

    @Test
    void exactRangeNoOpsAndPreviewOnlyEditsRemainAvailable() throws Exception {
        for (String type : TYPES) {
            PropertyValue zero = new PropertyValue.IntegerValue(BigInteger.ZERO);
            PropertyValue one = new PropertyValue.IntegerValue(BigInteger.ONE);
            DesignerCommandSession bound = bind(open(type, Map.of(MIN, zero, MAX, one)));
            assertEquals(DesignerCommandStatus.NO_CHANGE, bound.apply(new SetProperty(CONTROL, MIN, zero)).status());
            assertEquals(DesignerCommandStatus.NO_CHANGE, bound.apply(new SetProperty(CONTROL, MAX, one)).status());
            assertEquals(DesignerCommandStatus.NO_CHANGE, bind(open(type, Map.of()))
                    .apply(new ResetProperty(CONTROL, MIN)).status());
            DesignerCommandSession changed = applied(bound, new PatchProperties(CONTROL, List.of(
                    new PatchProperties.SetPatch(MIN, zero),
                    new PatchProperties.SetPatch(MAX, one),
                    new PatchProperties.SetPatch(new PropertyName(type.endsWith("RangeSlider") ? "valuesStart" : "value"), number("0.5")))));
            assertEquals(bound.current().document().root().stateBinding(), changed.current().document().root().stateBinding());
            assertArrayEquals(bound.current().dartCandidateBytes(), changed.current().dartCandidateBytes());
        }
    }

    @Test
    void removingTheBindingUnlocksBoundsWithoutDeletingUserMembers() throws Exception {
        for (String type : TYPES) {
            DesignerCommandSession preview = preview(bind(open(type, Map.of())), type);
            DesignerCommandSession removed = applied(preview, new RemoveStateBinding(CONTROL));
            DesignerCommandSession changed = applied(removed, new SetProperty(CONTROL, MIN, number("0.5")));
            assertTrue(changed.current().document().root().stateBinding().isEmpty());
            String source = new String(changed.current().dartCandidateBytes(), StandardCharsets.UTF_8);
            assertTrue(source.contains("_controlled ="), source);
            assertTrue(source.contains("void _changed("), source);
            var reopened = DesignerCommandSession.open(changed.current().fdSnapshot(),
                    changed.current().dartCandidateBytes(), BuiltInWidgetCatalog.getDefault());
            assertTrue(reopened.ready(), reopened.diagnostics().toString());
        }
    }

    private static void assertRejected(DesignerCommandSession before, DesignerCommand command, String pathSuffix) {
        var result = before.apply(command);
        assertEquals(DesignerCommandStatus.REJECTED, result.status(), result.diagnostics().toString());
        assertFalse(result.changed());
        assertSame(before, result.session());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code() == DesignerCommandDiagnosticCode.STATE_BINDING_REJECTED
                        && diagnostic.path().endsWith(pathSuffix)
                        && diagnostic.message().contains("Remove the State binding")), result.diagnostics().toString());
        assertArrayEquals(before.current().fdBytes(), result.session().current().fdBytes());
        assertArrayEquals(before.current().dartCandidateBytes(), result.session().current().dartCandidateBytes());
    }

    private static DesignerCommandSession preview(DesignerCommandSession session, String type) {
        if (type.endsWith("RangeSlider")) {
            return applied(session, new PatchProperties(CONTROL, List.of(
                    new PatchProperties.SetPatch(new PropertyName("valuesStart"), number("0.6")),
                    new PatchProperties.SetPatch(new PropertyName("valuesEnd"), number("0.8")))));
        }
        return applied(session, new SetProperty(CONTROL, new PropertyName("value"), number("0.8")));
    }

    private static DesignerCommandSession bind(DesignerCommandSession session) {
        return applied(session, new CreateStateBinding(CONTROL, "_controlled", "_changed"));
    }

    private static DesignerCommandSession applied(DesignerCommandSession session, DesignerCommand command) {
        var result = session.apply(command);
        assertEquals(DesignerCommandStatus.APPLIED, result.status(), result.diagnostics().toString());
        return result.session();
    }

    private static PropertyValue number(String value) {
        return new PropertyValue.DoubleValue(new BigDecimal(value));
    }

    private static DesignerCommandSession open(String type, Map<PropertyName, PropertyValue> values) throws Exception {
        var catalog = BuiltInWidgetCatalog.getDefault();
        var node = WidgetNodePrototypeFactory.create(catalog.find(new WidgetTypeId(type)).orElseThrow(), CONTROL, values);
        var provisional = new DesignerDocument(DOCUMENT, descriptor("0".repeat(64), "0".repeat(64)), node);
        var generated = new DartRegionGenerator().generate(provisional, catalog).generated().orElseThrow();
        var document = new DesignerDocument(DOCUMENT,
                descriptor(generated.imports().normalizedSha256(), generated.build().normalizedSha256()), node);
        String source = "// <netbeans-flutter-designer region=\"imports\">\n" + generated.imports().payload()
                + "// </netbeans-flutter-designer>\n\nclass Sample extends StatefulWidget {\n"
                + "  const Sample({super.key});\n  @override\n  State<Sample> createState() => FormLogic();\n}\n"
                + "class FormLogic extends State<Sample> {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + generated.build().payload()
                + "  // </netbeans-flutter-designer>\n}\n";
        var opened = DesignerCommandSession.open(new FdDocumentCodec().encode(document),
                source.getBytes(StandardCharsets.UTF_8), catalog);
        assertTrue(opened.ready(), opened.diagnostics().toString());
        return opened.session().orElseThrow();
    }

    private static DartSourceDescriptor descriptor(String imports, String build) {
        return new DartSourceDescriptor("sample.dart", "Sample", WidgetClassKind.STATEFUL, Optional.of(DartRegionGenerator.PROFILE_ID),
                new ManagedRegions(new ManagedRegion(imports), new ManagedRegion(build)));
    }
}
