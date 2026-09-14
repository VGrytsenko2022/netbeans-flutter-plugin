package dev.flutter.netbeans.plugin.designer;

import static org.junit.jupiter.api.Assertions.*;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.WidgetDefinition;
import dev.flutter.netbeans.designer.catalog.WidgetNodePrototypeFactory;
import dev.flutter.netbeans.designer.command.CreateEventHandler;
import dev.flutter.netbeans.designer.command.CreateStateBinding;
import dev.flutter.netbeans.designer.command.RemoveStateBinding;
import dev.flutter.netbeans.designer.command.DesignerCommand;
import dev.flutter.netbeans.designer.command.RenameEventHandler;
import dev.flutter.netbeans.designer.command.RenameStateField;
import dev.flutter.netbeans.designer.command.ResetProperty;
import dev.flutter.netbeans.designer.command.SetProperty;
import dev.flutter.netbeans.designer.events.WidgetEventCatalog;
import dev.flutter.netbeans.designer.events.WidgetEventDescriptor;
import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValue;
import dev.flutter.netbeans.designer.model.StableId;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.model.WidgetNode;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import dev.flutter.netbeans.designer.model.StateBinding;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import dev.flutter.netbeans.designer.command.BindPropertyToState;
import dev.flutter.netbeans.designer.command.RemovePropertyStateBinding;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.state.WidgetStateBindingCatalog;
import dev.flutter.netbeans.plugin.designer.properties.FlutterWidgetEventsContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class FlutterDesignerEventsBridgeTest {
    private static final StableId ID = StableId.parse("8fd25e35-76ab-4c0a-9280-dd6f00418ed7");
    private static final PropertyName CHANGED = new PropertyName("onChanged");
    private static final String IMPORTS = "import 'package:flutter/material.dart';\n";
    private static final String BUILD = "  @override\n  Widget build(BuildContext context) => const SizedBox();\n";
    private static final DartSourceDescriptor SOURCE_DESCRIPTOR = sourceDescriptor(WidgetClassKind.STATELESS);
    private static final byte[] SOURCE = ("// 😀 source location is UTF-16\n"
            + "// <netbeans-flutter-designer region=\"imports\">\n" + IMPORTS
            + "// </netbeans-flutter-designer>\nclass Screen extends StatelessWidget {\n"
            + "  // <netbeans-flutter-designer region=\"build\">\n" + BUILD
            + "  // </netbeans-flutter-designer>\n"
            + "  void _changed(String value) { void local() {} }\n"
            + "  Future<void> _refresh() async {}\n"
            + "  void _pressed() {}\n}\n").getBytes(StandardCharsets.UTF_8);

    @Test
    void discoveryIsReadOnlyAndOffersDirectSourceMethodsAsCandidates() {
        var ops = new Operations();
        var bridge = bridge("flutter.material.TextField", ops);
        var candidates = bridge.discover(CHANGED).toCompletableFuture().join();
        assertEquals(List.of("_changed", "_refresh", "_pressed"), candidates.stream().map(FlutterWidgetEventsContext.Handler::name).toList());
        assertTrue(candidates.getFirst().signature().contains("String"));
        assertEquals(1, ops.reads);
        assertTrue(ops.commands.isEmpty());
        assertEquals(0, ops.navigationOffset);
    }

    @Test
    void allCatalogEventsDispatchTypedAtomicCreateWithoutAutomaticSourceNavigation() {
        int count = 0;
        for (WidgetDefinition definition : BuiltInWidgetCatalog.getDefault().definitions()) {
            for (WidgetEventDescriptor event : WidgetEventCatalog.eventsFor(definition)) {
                if (event.kind() != WidgetEventDescriptor.Kind.EVENT) continue;
                var prototype = WidgetNodePrototypeFactory.create(definition, ID);
                var values = new LinkedHashMap<>(prototype.properties());
                if (!event.availableVariants().isEmpty()) values.put(new PropertyName("variant"),
                        new PropertyValue.StringValue(event.availableVariants().getFirst()));
                event.requiredCompanionProperties().forEach(name -> values.put(name,
                        PropertyValue.ImageProviderValue.asset("assets/avatar.png")));
                var widget = new WidgetNode(ID, definition.typeId(), values, prototype.slots());
                var ops = new Operations();
                var bridge = new FlutterDesignerEventsBridge(widget, definition, SOURCE_DESCRIPTOR, ops);
                bridge.create(event.propertyName(), "_handleEvent").toCompletableFuture().join();
                assertEquals(List.of(new CreateEventHandler(ID, event.propertyName(), "_handleEvent")), ops.commands);
                assertNull(ops.navigationSource, "Create stays in Designer; navigation is an explicit user action.");
                assertEquals(0, ops.reads);
                count++;
            }
        }
        assertEquals(174, count);
        var rejected = new Operations();
        rejected.mutation = CompletableFuture.failedFuture(new IllegalStateException("Analyzer rejected candidate"));
        var failure = assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge("flutter.material.TextField", rejected).create(CHANGED, "_newHandler").toCompletableFuture().join());
        assertTrue(failure.getCause().getMessage().contains("Analyzer rejected"));
        assertNull(rejected.navigationSource);
    }

    @Test
    void bindingRechecksCandidateAndRoutesThroughAnalyzerAdmission() {
        var ops = new Operations();
        var bridge = bridge("flutter.material.TextField", ops);
        var candidate = bridge.discover(CHANGED).toCompletableFuture().join().getFirst();
        bridge.bind(CHANGED, candidate).toCompletableFuture().join();
        assertEquals(List.of(new SetProperty(ID, CHANGED, new PropertyValue.CallbackValue("_changed"))), ops.commands);
        var foreign = new FlutterWidgetEventsContext.Handler("_missing", "_missing", "void _missing(String value)");
        assertThrows(java.util.concurrent.CompletionException.class, () -> bridge.bind(CHANGED, foreign).toCompletableFuture().join());
        assertEquals(1, ops.commands.size());
    }

    @Test
    void notificationSubtypeBindingKeepsNarrowUserMethodAndRoutesImportedCallbacksThroughTypedAdmission() {
        var definition = definition("flutter.widgets.NotificationListener");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var values = new LinkedHashMap<>(prototype.properties());
        values.put(new PropertyName("notificationType"), new PropertyValue.StringValue("ScrollNotification"));
        var widget = new WidgetNode(ID, definition.typeId(), values, prototype.slots());
        var ops = new Operations();
        ops.source = new String(SOURCE, StandardCharsets.UTF_8).replace("  void _pressed() {}", "  bool _scrollNotice(ScrollNotification notification) { return notification.depth == 0; }")
                .getBytes(StandardCharsets.UTF_8);
        byte[] original = ops.source.clone(); var bridge = new FlutterDesignerEventsBridge(widget, definition, SOURCE_DESCRIPTOR, ops);
        var property = new PropertyName("onNotification");
        var candidate = bridge.discover(property).toCompletableFuture().join().stream().filter(handler -> handler.name().equals("_scrollNotice")).findFirst().orElseThrow();
        assertTrue(candidate.signature().contains("ScrollNotification"));
        bridge.bind(property, candidate).toCompletableFuture().join();
        var external = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"), "scrollNotice",
                Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        bridge.editBinding(property, Optional.of(external)).toCompletableFuture().join();
        assertEquals(List.of(new SetProperty(ID, property, new PropertyValue.CallbackValue("_scrollNotice")), new SetProperty(ID, property, external)), ops.commands);
        assertArrayEquals(original, ops.source, "Bridge selection does not rewrite or widen the existing bool method; strict selected-T proof belongs to admission.");
        assertNull(ops.navigationSource);
        assertEquals(new PropertyValue.StringValue("ScrollNotification"), widget.properties().get(new PropertyName("notificationType")));
    }

    @Test
    void everyDisconnectPreservesRequiredDefaultsAndDoesNotChangeEnabled() {
        int count = 0;
        for (WidgetDefinition definition : BuiltInWidgetCatalog.getDefault().definitions()) {
            for (WidgetEventDescriptor event : WidgetEventCatalog.eventsFor(definition)) {
                if (event.kind() != WidgetEventDescriptor.Kind.EVENT) continue;
                var prototype = WidgetNodePrototypeFactory.create(definition, ID);
                var values = new LinkedHashMap<>(prototype.properties());
                values.put(event.propertyName(), event.bindingValue("_handler", definition.property(event.propertyName()).orElseThrow()));
                if (definition.property(new PropertyName("enabled")).isPresent()) {
                    values.put(new PropertyName("enabled"), new PropertyValue.BooleanValue(false));
                }
                var widget = new WidgetNode(ID, definition.typeId(), values, prototype.slots());
                var ops = new Operations();
                new FlutterDesignerEventsBridge(widget, definition, SOURCE_DESCRIPTOR, ops)
                        .disconnect(event.propertyName()).toCompletableFuture().join();
                assertEquals(1, ops.commands.size());
                if (event.required()) {
                    var set = assertInstanceOf(SetProperty.class, ops.commands.getFirst());
                    assertEquals(event.propertyName(), set.propertyName());
                    assertEquals(event.creationDefault().orElseGet(PropertyValue.NullValue::new), set.value());
                } else assertEquals(new ResetProperty(ID, event.propertyName()), ops.commands.getFirst());
                assertEquals(values, widget.properties(), "The immutable captured widget must not be edited in-place");
                assertEquals(0, ops.reads, "Disconnect does not rewrite the user's handler body");
                count++;
            }
        }
        assertEquals(174, count);
    }

    @Test
    void unavailableVariantAndImageCompanionRejectBindingWithoutChangingOtherProperties() {
        var refreshOps = new Operations();
        var refresh = bridge("flutter.material.RefreshIndicator", refreshOps);
        var error = assertThrows(java.util.concurrent.CompletionException.class,
                () -> refresh.create(new PropertyName("onStatusChange"), "_status").toCompletableFuture().join());
        assertTrue(error.getCause().getMessage().contains("noSpinner"));
        assertTrue(refreshOps.commands.isEmpty());
        var avatarOps = new Operations();
        var avatar = bridge("flutter.material.CircleAvatar", avatarOps);
        error = assertThrows(java.util.concurrent.CompletionException.class,
                () -> avatar.create(new PropertyName("onBackgroundImageError"), "_imageError").toCompletableFuture().join());
        assertTrue(error.getCause().getMessage().contains("backgroundImage"));
        assertTrue(avatarOps.commands.isEmpty());
    }

    @Test
    void focusOmittedVariantUsesStandardWhileExternalRejectsOnlyNewKeyBindings() {
        var definition = definition("flutter.widgets.Focus");
        for (String name : List.of("onKeyEvent", "onKey")) {
            var property = new PropertyName(name);
            var standard = new WidgetNode(ID, definition.typeId(), java.util.Map.of(), java.util.Map.of());
            var ops = new Operations();
            var bridge = new FlutterDesignerEventsBridge(standard, definition, SOURCE_DESCRIPTOR, ops);
            bridge.create(property, "_key").toCompletableFuture().join();
            bridge.editBinding(property, Optional.of(new PropertyValue.CallbackValue("_pressed"))).toCompletableFuture().join();
            bridge.bind(property, bridge.discover(property).toCompletableFuture().join().getLast()).toCompletableFuture().join();
            assertEquals(3, ops.commands.size(), "An omitted Focus variant is Standard, including key callbacks.");
            var values = new LinkedHashMap<PropertyName, PropertyValue>();
            values.put(new PropertyName("variant"), new PropertyValue.StringValue("withExternalFocusNode"));
            values.put(new PropertyName("focusNode"), new PropertyValue.DartObjectReferenceValue(Optional.empty(), "focusNode",
                    Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
            values.put(property, new PropertyValue.CallbackValue("_pressed"));
            var externalOps = new Operations();
            var external = new FlutterDesignerEventsBridge(new WidgetNode(ID, definition.typeId(), values, java.util.Map.of()),
                    definition, SOURCE_DESCRIPTOR, externalOps);
            assertThrows(java.util.concurrent.CompletionException.class, () -> external.create(property, "_newKey").toCompletableFuture().join());
            assertThrows(java.util.concurrent.CompletionException.class, () -> external.editBinding(property,
                    Optional.of(new PropertyValue.CallbackValue("_pressed"))).toCompletableFuture().join());
            assertThrows(java.util.concurrent.CompletionException.class, () -> external.bind(property,
                    new FlutterWidgetEventsContext.Handler("_pressed", "_pressed", "void _pressed()")).toCompletableFuture().join());
            assertTrue(externalOps.commands.isEmpty());
            external.navigate(property).toCompletableFuture().join(); assertNotNull(externalOps.navigationSource);
            external.rename(property, "_renamedKey").toCompletableFuture().join();
            external.disconnect(property).toCompletableFuture().join();
            assertEquals(List.of(new RenameEventHandler(ID, property, "_renamedKey"), new ResetProperty(ID, property)), externalOps.commands);
            external.create(new PropertyName("onFocusChange"), "_focusChanged").toCompletableFuture().join();
            assertEquals(3, externalOps.commands.size(), "onFocusChange remains active in both constructors.");
        }
    }

    @Test
    void staleDiscoveryOrActionCannotReachMutationBoundary() {
        var ops = new Operations();
        ops.pendingSource = new CompletableFuture<>();
        var bridge = bridge("flutter.material.TextField", ops);
        var discovery = bridge.discover(CHANGED).toCompletableFuture();
        ops.reason = "The selected revision changed.";
        ops.pendingSource.complete(SOURCE);
        assertThrows(java.util.concurrent.CompletionException.class, discovery::join);
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.create(CHANGED, "_new").toCompletableFuture().join());
        assertTrue(ops.commands.isEmpty());
        assertEquals(1, ops.reads);
    }

    @Test
    void advancedReferencePreservesImportedCallablesAndRejectsGuessedExternalRename() {
        var definition = definition("flutter.material.TextButton");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var external = new PropertyValue.DartObjectReferenceValue(Optional.of("package:app/handlers.dart"),
                "Handlers", Optional.of("pressed"), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var values = new LinkedHashMap<>(prototype.properties());
        PropertyName pressed = new PropertyName("onPressed");
        var ops = new Operations();
        var bridge = new FlutterDesignerEventsBridge(prototype, definition, SOURCE_DESCRIPTOR, ops);
        bridge.editBinding(pressed, Optional.of(external)).toCompletableFuture().join();
        assertEquals(List.of(new SetProperty(ID, pressed, external)), ops.commands);
        values.put(pressed, external);
        var configured = new FlutterDesignerEventsBridge(
                new WidgetNode(ID, definition.typeId(), values, prototype.slots()), definition, SOURCE_DESCRIPTOR, ops);
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> configured.rename(pressed, "_new").toCompletableFuture().join());
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> configured.navigate(pressed).toCompletableFuture().join());
        assertEquals(1, ops.commands.size());
    }

    @Test
    void localNavigationUsesUtf16OffsetAndRenameUsesAtomicCommand() {
        var definition = definition("flutter.material.TextField");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var values = new LinkedHashMap<>(prototype.properties());
        values.put(CHANGED, new PropertyValue.CallbackValue("_changed"));
        var ops = new Operations();
        var bridge = new FlutterDesignerEventsBridge(
                new WidgetNode(ID, definition.typeId(), values, prototype.slots()), definition, SOURCE_DESCRIPTOR, ops);
        bridge.navigate(CHANGED).toCompletableFuture().join();
        assertEquals('{', new String(SOURCE, StandardCharsets.UTF_8).charAt(ops.navigationOffset));
        assertArrayEquals(SOURCE, ops.navigationSource);
        assertTrue(ops.commands.isEmpty());
        bridge.rename(CHANGED, "_updated").toCompletableFuture().join();
        assertEquals(List.of(new RenameEventHandler(ID, CHANGED, "_updated")), ops.commands);
        assertEquals(1, ops.navigationCalls, "Rename does not navigate automatically.");
    }

    @Test
    void navigationNeedsNoIntermediateSaveButRejectsStaleAsyncSourceRead() {
        var definition = definition("flutter.material.TextField");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var values = new LinkedHashMap<>(prototype.properties());
        values.put(CHANGED, new PropertyValue.CallbackValue("_changed"));
        var ops = new Operations();
        var bridge = new FlutterDesignerEventsBridge(
                new WidgetNode(ID, definition.typeId(), values, prototype.slots()), definition, SOURCE_DESCRIPTOR, ops);
        bridge.create(CHANGED, "_next").toCompletableFuture().join();
        bridge.navigate(CHANGED).toCompletableFuture().join();
        assertEquals(1, ops.navigationCalls, "Go to Handler has no intermediate-save gate.");
        assertEquals(List.of(new CreateEventHandler(ID, CHANGED, "_next")), ops.commands,
                "Navigation must not submit a mutation or save the pending command.");

        ops.pendingSource = new CompletableFuture<>();
        var pending = bridge.navigate(CHANGED).toCompletableFuture();
        ops.reason = "The selected Designer revision changed.";
        ops.pendingSource.complete(SOURCE);
        var failure = assertThrows(java.util.concurrent.CompletionException.class, pending::join);
        assertTrue(failure.getCause().getMessage().contains("revision changed"));
        assertEquals(1, ops.navigationCalls, "A stale async result must not navigate.");
    }

    @Test
    void statefulDiscoveryBindingAndNavigationUseTheVerifiedStateOwnerNotANameGuess() {
        var definition = definition("flutter.material.TextField");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var values = new LinkedHashMap<>(prototype.properties());
        values.put(CHANGED, new PropertyValue.CallbackValue("_changed"));
        var ops = new Operations();
        ops.source = statefulSource();
        var bridge = new FlutterDesignerEventsBridge(
                new WidgetNode(ID, definition.typeId(), values, prototype.slots()), definition,
                sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        var candidates = bridge.discover(CHANGED).toCompletableFuture().join();
        assertEquals(List.of("_changed", "_stateOnly"), candidates.stream()
                .map(FlutterWidgetEventsContext.Handler::name).toList());
        bridge.navigate(CHANGED).toCompletableFuture().join();
        String text = new String(ops.source, StandardCharsets.UTF_8);
        assertTrue(ops.navigationOffset > text.indexOf("class ScreenLogic"));
        assertEquals('{', text.charAt(ops.navigationOffset));
        assertArrayEquals(ops.source, ops.navigationSource);
        bridge.bind(CHANGED, candidates.get(1)).toCompletableFuture().join();
        assertEquals(List.of(new SetProperty(ID, CHANGED, new PropertyValue.CallbackValue("_stateOnly"))), ops.commands);
        bridge.rename(CHANGED, "_renamed").toCompletableFuture().join();
        assertEquals(new RenameEventHandler(ID, CHANGED, "_renamed"), ops.commands.get(1));
    }

    @Test
    void statefulSourceWithUnverifiedOwnerCannotDiscoverOrNavigateToAnOuterMethod() {
        var definition = definition("flutter.material.TextField");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var values = new LinkedHashMap<>(prototype.properties());
        values.put(CHANGED, new PropertyValue.CallbackValue("_changed"));
        var ops = new Operations();
        ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                .replace("createState() => ScreenLogic()", "createState() => OtherLogic()")
                .getBytes(StandardCharsets.UTF_8);
        var bridge = new FlutterDesignerEventsBridge(
                new WidgetNode(ID, definition.typeId(), values, prototype.slots()), definition,
                sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        var failure = assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.discover(CHANGED).toCompletableFuture().join());
        assertTrue(failure.getCause().getMessage().contains("not verified"));
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.navigate(CHANGED).toCompletableFuture().join());
        assertEquals(0, ops.navigationCalls);
        assertTrue(ops.commands.isEmpty());
    }

    @Test
    void stateBindingCreateAndRemoveUseOnlyAtomicAnalyzedCommandsForEverySupportedControl() {
        for (var definition : BuiltInWidgetCatalog.getDefault().definitions().stream()
                .filter(WidgetStateBindingCatalog::supports).toList()) {
            var prototype = WidgetNodePrototypeFactory.create(definition, ID);
            var values = new LinkedHashMap<>(prototype.properties());
            if (definition.typeId().value().equals("flutter.material.IconButton")) values.put(new PropertyName("isSelected"), new PropertyValue.BooleanValue(false));
            var widget = new WidgetNode(ID, prototype.type(), values, prototype.slots());
            var ops = new Operations();
            var bridge = new FlutterDesignerEventsBridge(widget, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
            assertEquals("", bridge.stateBindingUnavailableReason());
            bridge.createStateBinding("_value", "_changed").toCompletableFuture().join();
            StateBinding.Action action = widget.type().value().equals("flutter.material.IconButton")
                    || widget.type().value().equals("flutter.material.ListTile") ? StateBinding.Action.TOGGLE : StateBinding.Action.CHANGE;
            assertEquals(List.of(new CreateStateBinding(ID, "_value", "_changed", action, Optional.empty(), "", Optional.empty())), ops.commands);
            assertEquals(0, ops.reads, "Source ownership is verified by the atomic command admission, not edited here.");
            assertNull(ops.navigationSource);
            var binding = WidgetStateBindingCatalog.createBinding(widget, "_value", "_changed");
            var bound = new WidgetNode(ID, widget.type(), widget.properties(), widget.slots(), widget.extensions(), Optional.of(binding));
            new FlutterDesignerEventsBridge(bound, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops)
                    .removeStateBinding().toCompletableFuture().join();
            assertEquals(new RemoveStateBinding(ID), ops.commands.getLast());
        }
    }

    @Test
    void stateBindingRejectsStatelessStaleIndependentHandlerAndAnalyzerFailureWithoutLocalEdits() {
        var ops = new Operations();
        var stateless = bridge("flutter.material.Switch", ops);
        assertTrue(stateless.stateBindingUnavailableReason().contains("Stateful"));
        assertThrows(java.util.concurrent.CompletionException.class, () -> stateless.createStateBinding("_value", "_changed").toCompletableFuture().join());
        assertTrue(ops.commands.isEmpty());
        var definition = definition("flutter.material.Switch");
        var widget = WidgetNodePrototypeFactory.create(definition, ID);
        var stateful = new FlutterDesignerEventsBridge(widget, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        ops.reason = "Stale revision";
        assertThrows(java.util.concurrent.CompletionException.class, () -> stateful.createStateBinding("_value", "_changed").toCompletableFuture().join());
        assertTrue(ops.commands.isEmpty());
        ops.reason = "";
        var values = new LinkedHashMap<>(widget.properties());
        values.put(CHANGED, new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_custom", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty()));
        var independent = new FlutterDesignerEventsBridge(new WidgetNode(ID, definition.typeId(), values, widget.slots()),
                definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        var rejection = assertThrows(java.util.concurrent.CompletionException.class,
                () -> independent.createStateBinding("_value", "_changed").toCompletableFuture().join());
        assertTrue(rejection.getCause().getMessage().contains("Disconnect"));
        assertTrue(ops.commands.isEmpty());
        ops.mutation = CompletableFuture.failedFuture(new IllegalStateException("Analyzer rejected State assignment"));
        assertThrows(java.util.concurrent.CompletionException.class, () -> stateful.createStateBinding("_value", "_changed").toCompletableFuture().join());
        assertTrue(widget.stateBinding().isEmpty());
        assertEquals(1, ops.commands.size());
        assertEquals(0, ops.navigationCalls);
    }

    @Test
    void privateStateFieldDiscoveryAndDependentPropertyBindingRecheckSourceBeforeAdmission() {
        var definition = definition("flutter.widgets.Text");
        var widget = WidgetNodePrototypeFactory.create(definition, ID);
        var ops = new Operations();
        ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                .replace("  void _stateOnly", "  double _amount = 0.5;\n  void _stateOnly").getBytes(StandardCharsets.UTF_8);
        var bridge = new FlutterDesignerEventsBridge(widget, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        var fields = bridge.discoverStateFields().toCompletableFuture().join();
        var field = fields.stream().filter(value -> value.fieldName().equals("_amount")).findFirst().orElseThrow();
        assertEquals(StateBinding.Type.DOUBLE, field.type());
        assertTrue(ops.commands.isEmpty(), "Discovery never mutates source.");
        var dependency = new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(),
                StatePropertyBinding.Transform.TO_STRING, Optional.empty());
        PropertyName data = new PropertyName("data");
        bridge.bindPropertyToState(data, dependency).toCompletableFuture().join();
        assertEquals(new BindPropertyToState(ID, data, dependency), ops.commands.getFirst());
        assertEquals(2, ops.reads, "Apply re-reads exact current source instead of trusting a stale dropdown.");
        ops.source = statefulSource();
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.bindPropertyToState(data, dependency).toCompletableFuture().join());
        assertEquals(1, ops.commands.size());
        var bound = new WidgetNode(ID, widget.type(), widget.properties(), widget.slots(), widget.extensions(),
                Optional.empty(), java.util.Map.of(data, dependency));
        new FlutterDesignerEventsBridge(bound, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops)
                .removePropertyStateBinding(data).toCompletableFuture().join();
        assertEquals(new RemovePropertyStateBinding(ID, data), ops.commands.getLast());
    }

    @Test
    void reuseAndControllerInitialTextKeepExplicitClosedCommandParameters() {
        var ops = new Operations();
        ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                .replace("  void _stateOnly", "  bool _selected = false;\n  void _stateOnly").getBytes(StandardCharsets.UTF_8);
        var definition = definition("flutter.material.Switch");
        var widget = WidgetNodePrototypeFactory.create(definition, ID);
        var bridge = new FlutterDesignerEventsBridge(widget, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        var field = bridge.discoverStateFields().toCompletableFuture().join().stream()
                .filter(value -> value.fieldName().equals("_selected")).findFirst().orElseThrow();
        bridge.createStateBinding("_selected", "_switchChanged", StateBinding.Action.CHANGE,
                Optional.empty(), "", Optional.of(field)).toCompletableFuture().join();
        assertEquals(new CreateStateBinding(ID, "_selected", "_switchChanged", StateBinding.Action.CHANGE,
                Optional.empty(), "", Optional.of(field)), ops.commands.getLast());
        var textDefinition = definition("flutter.material.TextField");
        var text = WidgetNodePrototypeFactory.create(textDefinition, ID);
        var textBridge = new FlutterDesignerEventsBridge(text, textDefinition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        textBridge.createStateBinding("_controller", "_textChanged", StateBinding.Action.CHANGE,
                Optional.empty(), "Hello\n$world", Optional.empty()).toCompletableFuture().join();
        assertEquals("Hello\n$world", ((CreateStateBinding) ops.commands.getLast()).initialText());
    }

    @Test
    void fieldDiscoveryRejectsStatelessAndChangedRevisionWithoutMutation() {
        var ops = new Operations();
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge("flutter.widgets.Text", ops).discoverStateFields().toCompletableFuture().join());
        assertEquals(0, ops.reads);
        var definition = definition("flutter.widgets.Text");
        var bridge = new FlutterDesignerEventsBridge(WidgetNodePrototypeFactory.create(definition, ID),
                definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        ops.pendingSource = new CompletableFuture<>();
        var discovery = bridge.discoverStateFields().toCompletableFuture();
        ops.reason = "Stale State revision";
        ops.pendingSource.complete(statefulSource());
        assertTrue(assertThrows(java.util.concurrent.CompletionException.class, discovery::join).getCause().getMessage().contains("Stale"));
        assertTrue(ops.commands.isEmpty());
    }

    @Test
    void customTypeFieldHintsComeFromFrozenWholeFormMetadataAndRecheckCurrentSource() {
        var textDefinition = definition("flutter.widgets.Text");
        var text = WidgetNodePrototypeFactory.create(textDefinition, ID);
        var radioDefinition = definition("flutter.widgets.RadioGroup");
        var radio = WidgetNodePrototypeFactory.create(radioDefinition, StableId.random());
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "Choice", Optional.empty(),
                PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var properties = new LinkedHashMap<>(radio.properties());
        properties.put(new PropertyName("valueType"), reference);
        properties.remove(new PropertyName("groupValue"));
        var action = new StateBinding("_choice", "_choiceChanged", StateBinding.Type.NULLABLE_REFERENCE,
                Optional.of(reference), Optional.empty());
        var typedRadio = new WidgetNode(radio.id(), radio.type(), properties, radio.slots(), radio.extensions(), Optional.of(action));
        var root = new WidgetNode(StableId.random(), new WidgetTypeId("flutter.widgets.Column"), java.util.Map.of(),
                java.util.Map.of(new dev.flutter.netbeans.designer.model.SlotName("children"),
                        new dev.flutter.netbeans.designer.model.WidgetSlot.ListSlot(List.of(text, typedRadio))));
        var ops = new Operations();
        ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                .replace("class ScreenLogic", "enum Choice { first, second }\nclass ScreenLogic")
                .replace("  void _stateOnly", "  Choice? _choice = Choice.first;\n  Choice? _notInModel = null;\n  void _stateOnly")
                .getBytes(StandardCharsets.UTF_8);
        var bridge = new FlutterDesignerEventsBridge(text, textDefinition, sourceDescriptor(WidgetClassKind.STATEFUL), root, ops);
        var fields = bridge.discoverStateFields().toCompletableFuture().join();
        var field = fields.stream().filter(value -> value.fieldName().equals("_choice")).findFirst().orElseThrow();
        assertEquals(StateBinding.Type.NULLABLE_REFERENCE, field.type());
        assertEquals(Optional.of(reference), field.referenceType());
        assertFalse(fields.stream().anyMatch(value -> value.fieldName().equals("_notInModel")), "Arbitrary source type names are not guessed.");
        var dependency = new StatePropertyBinding(field.fieldName(), field.type(), field.referenceType(), StatePropertyBinding.Transform.TO_STRING, Optional.empty());
        bridge.bindPropertyToState(new PropertyName("data"), dependency).toCompletableFuture().join();
        assertEquals(new BindPropertyToState(ID, new PropertyName("data"), dependency), ops.commands.getLast());
        ops.source = statefulSource();
        assertTrue(bridge.discoverStateFields().toCompletableFuture().join().stream().noneMatch(value -> value.fieldName().equals("_choice")),
                "A removed custom field must disappear even though an older admitted model mentions it.");
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.bindPropertyToState(new PropertyName("data"), dependency).toCompletableFuture().join());
        assertEquals(1, ops.commands.size());
    }

    @Test
    void boundControlFieldNavigationUsesExactSourceAndRenameOnlySubmitsAnAtomicCommand() {
        var definition = definition("flutter.material.Switch");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var binding = WidgetStateBindingCatalog.createBinding(prototype, "_selected", "_changed");
        var widget = new WidgetNode(ID, prototype.type(), prototype.properties(), prototype.slots(), prototype.extensions(), Optional.of(binding));
        var ops = new Operations();
        ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                .replace("  void _stateOnly", "  bool _selected = false;\n  bool _other = true;\n  void _stateOnly")
                .getBytes(StandardCharsets.UTF_8);
        byte[] unchanged = ops.source.clone();
        var bridge = new FlutterDesignerEventsBridge(widget, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        bridge.navigateToStateField("_selected").toCompletableFuture().join();
        assertEquals(new String(ops.source, StandardCharsets.UTF_8).indexOf("_selected ="), ops.navigationOffset,
                "Navigation targets the declaration name in UTF-16, even after a non-BMP character.");
        assertArrayEquals(unchanged, ops.navigationSource);
        assertEquals(1, ops.reads, "Navigation proves and opens one exact source snapshot.");
        assertTrue(ops.commands.isEmpty(), "Go to Field is read-only and never saves.");
        bridge.renameStateField("_selected", "_renamedSelected").toCompletableFuture().join();
        assertEquals(List.of(new RenameStateField(ID, "_selected", "_renamedSelected")), ops.commands);
        assertEquals(2, ops.reads, "Rename rechecks the live field independently of a previous navigation.");
        assertEquals(1, ops.navigationCalls, "Rename must not navigate automatically.");
        assertArrayEquals(unchanged, ops.source, "The bridge must not edit source outside atomic command admission.");
        assertEquals("_changed", widget.stateBinding().orElseThrow().handlerName(), "Field rename does not rename the callback.");
        ops.mutation = CompletableFuture.failedFuture(new IllegalStateException("Analyzer rejected renamed State field"));
        var failure = assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.renameStateField("_selected", "_rejected").toCompletableFuture().join());
        assertTrue(failure.getCause().getMessage().contains("Analyzer rejected"));
        assertEquals(1, ops.navigationCalls);
        assertArrayEquals(unchanged, ops.source);
    }

    @Test
    void fieldManagementRejectsUnboundMissingWrongTypeAndStaleFieldsBeforeMutationOrNavigation() {
        var definition = definition("flutter.material.Switch");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var binding = WidgetStateBindingCatalog.createBinding(prototype, "_selected", "_changed");
        var widget = new WidgetNode(ID, prototype.type(), prototype.properties(), prototype.slots(), prototype.extensions(), Optional.of(binding));
        var ops = new Operations();
        ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                .replace("  void _stateOnly", "  bool _selected = false;\n  bool _other = true;\n  void _stateOnly")
                .getBytes(StandardCharsets.UTF_8);
        var bridge = new FlutterDesignerEventsBridge(widget, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        var unbound = assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.renameStateField("_other", "_renamed").toCompletableFuture().join());
        assertTrue(unbound.getCause().getMessage().contains("no current binding"));
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.navigateToStateField("_other").toCompletableFuture().join());
        assertEquals(0, ops.reads, "An unrelated dropdown candidate does not authorize field management.");
        for (String declaration : List.of("", "  String _selected = 'wrong type';\n")) {
            ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                    .replace("  void _stateOnly", declaration + "  void _stateOnly").getBytes(StandardCharsets.UTF_8);
            var rejection = assertThrows(java.util.concurrent.CompletionException.class,
                    () -> bridge.renameStateField("_selected", "_renamed").toCompletableFuture().join());
            assertTrue(rejection.getCause().getMessage().contains("_selected"));
            assertThrows(java.util.concurrent.CompletionException.class,
                    () -> bridge.navigateToStateField("_selected").toCompletableFuture().join());
        }
        ops.pendingSource = new CompletableFuture<>();
        var pending = bridge.navigateToStateField("_selected").toCompletableFuture();
        ops.reason = "The selected State revision changed.";
        ops.pendingSource.complete(ops.source);
        assertTrue(assertThrows(java.util.concurrent.CompletionException.class, pending::join).getCause().getMessage().contains("revision changed"));
        assertThrows(java.util.concurrent.CompletionException.class,
                () -> bridge.renameStateField("_selected", "_renamed").toCompletableFuture().join());
        assertTrue(ops.commands.isEmpty());
        assertEquals(0, ops.navigationCalls);
    }

    @Test
    void consumerOnlyFieldManagementDoesNotRequireAControlStateBindingOrRenameItsTransform() {
        var definition = definition("flutter.widgets.Text");
        var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var dependency = new StatePropertyBinding("_counter", StateBinding.Type.INT, Optional.empty(),
                StatePropertyBinding.Transform.TO_STRING, Optional.empty());
        var widget = new WidgetNode(ID, prototype.type(), prototype.properties(), prototype.slots(), prototype.extensions(),
                Optional.empty(), java.util.Map.of(new PropertyName("data"), dependency));
        var ops = new Operations();
        ops.source = new String(statefulSource(), StandardCharsets.UTF_8)
                .replace("  void _stateOnly", "  int _counter = 2;\n  void _stateOnly").getBytes(StandardCharsets.UTF_8);
        var bridge = new FlutterDesignerEventsBridge(widget, definition, sourceDescriptor(WidgetClassKind.STATEFUL), ops);
        assertTrue(bridge.stateBindingUnavailableReason().contains("not supported"), "Text is a consumer, not a control.");
        bridge.navigateToStateField("_counter").toCompletableFuture().join();
        assertEquals(new String(ops.source, StandardCharsets.UTF_8).indexOf("_counter ="), ops.navigationOffset);
        assertTrue(ops.commands.isEmpty());
        bridge.renameStateField("_counter", "_newCounter").toCompletableFuture().join();
        assertEquals(List.of(new RenameStateField(ID, "_counter", "_newCounter")), ops.commands);
        assertEquals(1, ops.navigationCalls);
        assertEquals(StatePropertyBinding.Transform.TO_STRING, widget.propertyBindings().get(new PropertyName("data")).transform());
        assertTrue(widget.stateBinding().isEmpty());
    }

    @Test void menuBuilderCreationIsScopedAtomicAndDoesNotTurnBuilderIntoANativeEvent() {
        var ops = new Operations(); var bridge = bridge("flutter.material.MenuAnchor", ops);
        bridge.createMenuAnchorBuilder("_buildMenu").toCompletableFuture().join();
        assertEquals(List.of(new dev.flutter.netbeans.designer.command.CreateMenuAnchorBuilder(ID, "_buildMenu")), ops.commands);
        assertEquals(0, ops.reads); assertEquals(0, ops.navigationCalls);
        assertThrows(java.util.concurrent.CompletionException.class, () -> bridge.create(new PropertyName("builder"), "_event").toCompletableFuture().join());
        assertThrows(java.util.concurrent.CompletionException.class, () -> bridge("flutter.material.MenuItemButton", ops).createMenuAnchorBuilder("_wrong").toCompletableFuture().join());
        ops.reason = "The selected revision is stale.";
        assertThrows(java.util.concurrent.CompletionException.class, () -> bridge.createMenuAnchorBuilder("_stale").toCompletableFuture().join()); assertEquals(1, ops.commands.size());
    }

    @Test void menuBuilderNavigationRequiresCurrentDirectUserOwnedMethodAndCannotOverwriteABinding() {
        var definition = definition("flutter.material.MenuAnchor"); var prototype = WidgetNodePrototypeFactory.create(definition, ID);
        var reference = new PropertyValue.DartObjectReferenceValue(Optional.empty(), "_buildMenu", Optional.empty(), PropertyValue.DartObjectReferenceValue.Access.REFERENCE, Optional.empty());
        var widget = new WidgetNode(ID, prototype.type(), java.util.Map.of(new PropertyName("builder"), reference), prototype.slots());
        var ops = new Operations(); ops.source = new String(SOURCE, StandardCharsets.UTF_8).replace("  void _pressed() {}", "  void _pressed() {}\n  Widget _buildMenu(BuildContext context, MenuController controller, Widget? child) => child ?? const SizedBox();").getBytes(StandardCharsets.UTF_8);
        var bridge = new FlutterDesignerEventsBridge(widget, definition, SOURCE_DESCRIPTOR, ops);
        bridge.navigateToMenuAnchorBuilder().toCompletableFuture().join(); assertEquals(new String(ops.source, StandardCharsets.UTF_8).indexOf("=> child ?? const SizedBox();"), ops.navigationOffset); assertEquals(1, ops.navigationCalls); assertTrue(ops.commands.isEmpty());
        assertThrows(java.util.concurrent.CompletionException.class, () -> bridge.createMenuAnchorBuilder("_another").toCompletableFuture().join()); assertTrue(ops.commands.isEmpty());
        ops.pendingSource = new CompletableFuture<>(); var pending = bridge.navigateToMenuAnchorBuilder().toCompletableFuture(); ops.reason = "Readiness was withdrawn."; ops.pendingSource.complete(ops.source);
        assertThrows(java.util.concurrent.CompletionException.class, pending::join); assertEquals(1, ops.navigationCalls);
    }

    @Test void modalBarrierFalseDismissibleRetainsEditableCallbackAndNullResetSemantics(){
        var d=definition("flutter.widgets.ModalBarrier");var prototype=WidgetNodePrototypeFactory.create(d,ID);
        var p=new PropertyName("onDismiss");var values=new LinkedHashMap<>(prototype.properties());
        values.put(new PropertyName("dismissible"),new PropertyValue.BooleanValue(false));
        var widget=new WidgetNode(ID,prototype.type(),values,prototype.slots());var ops=new Operations();
        var bridge=new FlutterDesignerEventsBridge(widget,d,SOURCE_DESCRIPTOR,ops);
        bridge.create(p,"_dismiss").toCompletableFuture().join();
        assertEquals(new CreateEventHandler(ID,p,"_dismiss"),ops.commands.removeFirst());
        bridge.editBinding(p,Optional.of(new PropertyValue.NullValue())).toCompletableFuture().join();
        assertEquals(new SetProperty(ID,p,new PropertyValue.NullValue()),ops.commands.removeFirst());
        values.put(p,new PropertyValue.NullValue());widget=new WidgetNode(ID,prototype.type(),values,prototype.slots());
        bridge=new FlutterDesignerEventsBridge(widget,d,SOURCE_DESCRIPTOR,ops);
        bridge.disconnect(p).toCompletableFuture().join();assertEquals(new ResetProperty(ID,p),ops.commands.removeFirst());
        assertEquals(new PropertyValue.BooleanValue(false),widget.properties().get(new PropertyName("dismissible")));
        assertTrue(ops.commands.isEmpty());assertEquals(0,ops.reads);
    }

    @Test void animatedModalBarrierFalseDismissibleRetainsEditableCallbackAndNullResetSemantics(){
        var d=definition("flutter.widgets.AnimatedModalBarrier");var prototype=WidgetNodePrototypeFactory.create(d,ID);
        var p=new PropertyName("onDismiss");var values=new LinkedHashMap<>(prototype.properties());
        values.put(new PropertyName("dismissible"),new PropertyValue.BooleanValue(false));
        var widget=new WidgetNode(ID,prototype.type(),values,prototype.slots());var ops=new Operations();
        var bridge=new FlutterDesignerEventsBridge(widget,d,SOURCE_DESCRIPTOR,ops);
        bridge.create(p,"_dismiss").toCompletableFuture().join();
        assertEquals(new CreateEventHandler(ID,p,"_dismiss"),ops.commands.removeFirst());
        bridge.editBinding(p,Optional.of(new PropertyValue.NullValue())).toCompletableFuture().join();
        assertEquals(new SetProperty(ID,p,new PropertyValue.NullValue()),ops.commands.removeFirst());
        values.put(p,new PropertyValue.NullValue());widget=new WidgetNode(ID,prototype.type(),values,prototype.slots());
        bridge=new FlutterDesignerEventsBridge(widget,d,SOURCE_DESCRIPTOR,ops);
        bridge.disconnect(p).toCompletableFuture().join();assertEquals(new ResetProperty(ID,p),ops.commands.removeFirst());
        assertEquals(new PropertyValue.BooleanValue(false),widget.properties().get(new PropertyName("dismissible")));
        assertTrue(ops.commands.isEmpty());assertEquals(0,ops.reads);
    }

    @Test void matrixTransformDelegateUsesReviewedActionsAndDisconnectsToIdentity(){
        var d=definition("flutter.widgets.MatrixTransition");
        var prototype=WidgetNodePrototypeFactory.create(d,ID);
        var p=new PropertyName("onTransform");var ops=new Operations();
        var bridge=new FlutterDesignerEventsBridge(prototype,d,SOURCE_DESCRIPTOR,ops);
        bridge.create(p,"_compute").toCompletableFuture().join();
        assertEquals(new CreateEventHandler(ID,p,"_compute"),ops.commands.removeFirst());
        var ref=new PropertyValue.DartObjectReferenceValue(Optional.empty(),"_compute",Optional.empty(),PropertyValue.DartObjectReferenceValue.Access.REFERENCE,Optional.empty());
        var values=new java.util.LinkedHashMap<>(prototype.properties());values.put(p,ref);
        var bound=new WidgetNode(ID,prototype.type(),values,prototype.slots());
        bridge=new FlutterDesignerEventsBridge(bound,d,SOURCE_DESCRIPTOR,ops);
        bridge.rename(p,"_next").toCompletableFuture().join();
        assertEquals(new dev.flutter.netbeans.designer.command.RenameEventHandler(ID,p,"_next"),ops.commands.removeFirst());
        bridge.disconnect(p).toCompletableFuture().join();
        assertEquals(new SetProperty(ID,p,dev.flutter.netbeans.designer.catalog.MatrixTransitionWidgetPropertySchema.identity()),ops.commands.removeFirst());
        ops.source=new String(SOURCE,StandardCharsets.UTF_8).replace("  void _pressed() {}",
            "  void _pressed() {}\n  Matrix4 _compute(double animationValue) => Matrix4.identity();").getBytes(StandardCharsets.UTF_8);
        var candidate=bridge.discover(p).toCompletableFuture().join().stream().filter(x->x.name().equals("_compute")).findFirst().orElseThrow();
        bridge.bind(p,candidate).toCompletableFuture().join();assertTrue(ops.commands.isEmpty(),"Already bound");
        bridge.navigate(p).toCompletableFuture().join();assertEquals(1,ops.navigationCalls);
        ops.reason="The selected revision is stale.";
        var stale=bridge;
        assertThrows(java.util.concurrent.CompletionException.class,()->stale.disconnect(p).toCompletableFuture().join());
        assertTrue(ops.commands.isEmpty());
    }

    private static byte[] statefulSource() {
        return ("// 😀 State owner does not follow a generated name convention\n"
                + "// <netbeans-flutter-designer region=\"imports\">\n" + IMPORTS
                + "// </netbeans-flutter-designer>\nclass Screen extends StatefulWidget {\n"
                + "  const Screen({super.key});\n"
                + "  @override\n  State<Screen> createState() => ScreenLogic();\n"
                + "  void _outerOnly() {}\n  void _changed(String value) {}\n}\n"
                + "class ScreenLogic extends State<Screen> {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n" + BUILD
                + "  // </netbeans-flutter-designer>\n"
                + "  void _changed(String value) { print(value); }\n"
                + "  void _stateOnly(String value) {}\n}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static WidgetDefinition definition(String type) {
        return BuiltInWidgetCatalog.getDefault().find(new WidgetTypeId(type)).orElseThrow();
    }

    private static FlutterDesignerEventsBridge bridge(String type, Operations operations) {
        var definition = definition(type);
        return new FlutterDesignerEventsBridge(WidgetNodePrototypeFactory.create(definition, ID), definition, SOURCE_DESCRIPTOR, operations);
    }

    private static DartSourceDescriptor sourceDescriptor(WidgetClassKind kind) {
        return new DartSourceDescriptor("screen.dart", "Screen", kind, Optional.empty(),
                new ManagedRegions(new ManagedRegion(DartManagedRegionHashing.normalizedSha256(IMPORTS)),
                        new ManagedRegion(DartManagedRegionHashing.normalizedSha256(BUILD))));
    }

    private static final class Operations implements FlutterDesignerEventsBridge.Operations {
        String reason = "";
        int reads;
        final List<DesignerCommand> commands = new ArrayList<>();
        CompletableFuture<byte[]> pendingSource;
        CompletableFuture<Void> mutation = CompletableFuture.completedFuture(null);
        byte[] source = SOURCE;
        int navigationOffset;
        byte[] navigationSource;
        int navigationCalls;
        @Override public String unavailableReason() { return reason; }
        @Override public CompletionStage<byte[]> sourceBytes() {
            reads++;
            return pendingSource != null ? pendingSource : CompletableFuture.completedFuture(source);
        }
        @Override public CompletionStage<Void> submit(DesignerCommand command, String target) {
            commands.add(command);
            return mutation;
        }
        @Override public CompletionStage<Void> navigate(byte[] expectedSource, int utf16Offset) {
            navigationCalls++;
            navigationSource = expectedSource;
            navigationOffset = utf16Offset;
            return CompletableFuture.completedFuture(null);
        }
    }
}
