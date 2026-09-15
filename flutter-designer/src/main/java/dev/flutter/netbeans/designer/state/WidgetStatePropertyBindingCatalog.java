package dev.flutter.netbeans.designer.state;

import dev.flutter.netbeans.designer.catalog.BuiltInWidgetCatalog;
import dev.flutter.netbeans.designer.catalog.FloatingActionButtonWidgetPropertySchema;
import dev.flutter.netbeans.designer.catalog.PropertyDefinition;
import dev.flutter.netbeans.designer.catalog.PropertyValueConstraint;
import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.PropertyValueKind;
import dev.flutter.netbeans.designer.model.StateBinding;
import dev.flutter.netbeans.designer.model.StatePropertyBinding;
import dev.flutter.netbeans.designer.model.WidgetNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Reviewed runtime fields, including explicitly admitted data-constructor leaves, never inferred from preview values. */
public final class WidgetStatePropertyBindingCatalog {
    public record Descriptor(PropertyName propertyName, String dartArgumentName, String dartType,
            boolean nullable, Set<StatePropertyBinding.Transform> allowedTransforms) {
        public Descriptor { allowedTransforms = Set.copyOf(allowedTransforms); }
    }

    private static final Map<String, Set<String>> REVIEWED = reviewed();
    private WidgetStatePropertyBindingCatalog() {}

    public static Optional<Descriptor> find(WidgetNode widget, PropertyName property) {
        if (widget.type().equals(FloatingActionButtonWidgetPropertySchema.FLOATING_ACTION_BUTTON_TYPE)
                && !FloatingActionButtonWidgetPropertySchema.propertyAvailableInVariant(property.value(),
                        FloatingActionButtonWidgetPropertySchema.variant(widget))) return Optional.empty();
        if (property.value().equals("opacity") && widget.type().value().equals("flutter.widgets.Opacity")
                || property.value().equals("value") && Set.of("flutter.material.LinearProgressIndicator",
                        "flutter.material.CircularProgressIndicator", "flutter.material.RefreshProgressIndicator").contains(widget.type().value())) {
            return Optional.of(new Descriptor(property, property.value(), "double", !property.value().equals("opacity"),
                    Set.of(StatePropertyBinding.Transform.CLAMP)));
        }
        if (widget.type().value().equals("flutter.material.PaginatedDataTable") && property.value().equals("rowsPerPage"))
            return Optional.of(new Descriptor(property,"rowsPerPage","int",false,Set.of(StatePropertyBinding.Transform.DIRECT)));
        if (property.value().equals("sortColumnIndex") && Set.of("flutter.material.DataTable","flutter.material.PaginatedDataTable").contains(widget.type().value())) {
            return Optional.of(new Descriptor(property, "sortColumnIndex", "int", true, Set.of(StatePropertyBinding.Transform.DIRECT)));
        }
        if (property.value().equals("index") && widget.type().value().equals("flutter.widgets.IndexedStack")) {
            return Optional.of(new Descriptor(property, "index", "int", true, Set.of(StatePropertyBinding.Transform.CLAMP)));
        }
        if (!REVIEWED.getOrDefault(widget.type().value(), Set.of()).contains(property.value())) return Optional.empty();
        return BuiltInWidgetCatalog.getDefault().find(widget.type()).flatMap(definition -> definition.property(property))
                .flatMap(WidgetStatePropertyBindingCatalog::descriptor);
    }

    public static List<Descriptor> descriptors(WidgetNode widget) {
        return BuiltInWidgetCatalog.getDefault().find(widget.type()).stream()
                .flatMap(definition -> definition.properties().stream())
                .map(property -> find(widget, property.name())).flatMap(Optional::stream).toList();
    }

    public static Optional<String> validationError(WidgetNode widget) {
        for (var entry : widget.propertyBindings().entrySet()) {
            Optional<Descriptor> found = find(widget, entry.getKey());
            if (found.isEmpty()) return Optional.of("State property binding is not supported for "
                    + widget.type().value() + "." + entry.getKey().value()
                        + "; only reviewed runtime fields are allowed; their documented SDK runtime requirements still apply.");
            Descriptor descriptor = found.orElseThrow();
            StatePropertyBinding binding = entry.getValue();
            if (binding.transform() == StatePropertyBinding.Transform.CLAMP && entry.getKey().value().equals("value")
                    && widget.properties().containsKey(new PropertyName("controller"))) {
                return Optional.of("Remove the Animation Controller before binding progress Value to State; these runtime inputs are mutually exclusive.");
            }
            if (!descriptor.allowedTransforms().contains(binding.transform()) || !compatible(descriptor, binding)) {
                return Optional.of("The State field type/transform does not match " + entry.getKey().value()
                        + " (" + descriptor.dartType() + (descriptor.nullable() ? "?" : "") + ").");
            }
            if (widget.stateBinding().isPresent() && WidgetStateBindingCatalog.find(widget)
                    .map(control -> control.runtimeArgumentName().equals(descriptor.dartArgumentName())).orElse(false)) {
                return Optional.of("Remove the widget's State action binding before binding its controlled argument separately.");
            }
        }
        return Optional.empty();
    }

    private static boolean compatible(Descriptor descriptor, StatePropertyBinding binding) {
        return switch (binding.transform()) {
            case TO_STRING, TEXT -> descriptor.dartType().equals("String");
            case EQUALS, NOT -> descriptor.dartType().equals("bool");
            case CLAMP -> descriptor.dartType().equals("int") ? binding.type() == StateBinding.Type.INT
                    : binding.type() == StateBinding.Type.DOUBLE;
            case DIRECT -> {
                String input = WidgetStateBindingCatalog.dartType(binding.type(), binding.referenceType());
                boolean nullableInput = input.endsWith("?");
                String base = nullableInput ? input.substring(0, input.length() - 1) : input;
                yield (!nullableInput || descriptor.nullable()) && (descriptor.dartType().equals(base)
                        || descriptor.dartType().equals("num") && Set.of("int", "double").contains(base));
            }
        };
    }

    private static Optional<Descriptor> descriptor(PropertyDefinition property) {
        // A whitelist is necessary: generic BOOLEAN metadata also describes synthetic flags and nested values.
        // Reviewed enabled consumers retain their SDK mounted activation assertions; this is not a proof of runtime ancestry.
        String type = property.constraints().stream().filter(PropertyValueConstraint.AnyValue.class::isInstance)
                .map(PropertyValueConstraint::kind).filter(kind -> kind == PropertyValueKind.STRING || kind == PropertyValueKind.BOOLEAN)
                .map(kind -> kind == PropertyValueKind.STRING ? "String" : "bool").findFirst().orElse(null);
        if (property.name().value().equals("quarterTurns")) type = "int";
        if (type == null) return Optional.empty();
        Set<StatePropertyBinding.Transform> transforms = type.equals("String")
                ? Set.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.TO_STRING, StatePropertyBinding.Transform.TEXT)
                : type.equals("bool") ? Set.of(StatePropertyBinding.Transform.DIRECT, StatePropertyBinding.Transform.NOT,
                        StatePropertyBinding.Transform.EQUALS) : Set.of(StatePropertyBinding.Transform.DIRECT);
        return Optional.of(new Descriptor(property.name(), property.name().value(), type,
                property.acceptedKinds().contains(PropertyValueKind.NULL), transforms));
    }

    private static Map<String, Set<String>> reviewed() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        add(result, "widgets.Text", "data semanticsLabel softWrap");
        add(result, "widgets.Icon", "semanticLabel applyTextScaling");
        add(result, "widgets.Image", "semanticLabel excludeFromSemantics gaplessPlayback isAntiAlias matchTextDirection");
        add(result, "widgets.ImageIcon", "semanticLabel");
        add(result, "material.Scaffold", "extendBody extendBodyBehindAppBar primary resizeToAvoidBottomInset drawerEnableOpenDragGesture endDrawerEnableOpenDragGesture restorationId");
        add(result, "material.AppBar", "automaticallyImplyLeading primary centerTitle excludeHeaderSemantics forceMaterialTransparency animateColor");
        for (String type : List.of("ElevatedButton", "TextButton", "OutlinedButton", "FilledButton")) add(result, "material." + type, "autofocus");
        add(result, "material.IconButton", "autofocus tooltip isSelected enableFeedback");
        add(result, "material.FloatingActionButton", "tooltip autofocus isExtended enableFeedback mini");
        add(result, "material.Checkbox", "autofocus semanticLabel isError");
        add(result, "material.Switch", "autofocus");
        add(result, "material.Radio", "autofocus");
        add(result, "material.Slider", "autofocus label year2023");
        add(result, "material.RangeSlider", "year2023");
        add(result, "material.ListTile", "enabled selected dense autofocus enableFeedback internalAddSemanticForOnTap");
        add(result, "material.CheckboxListTile", "dense autofocus enableFeedback selected isError enabled");
        add(result, "material.SwitchListTile", "dense autofocus enableFeedback selected internalAddSemanticForOnTap applyCupertinoTheme");
        add(result, "material.RadioListTile", "toggleable dense selected autofocus enableFeedback enabled internalAddSemanticForOnTap useCupertinoCheckmarkStyle");
        add(result, "material.ExpansionTile", "showTrailingIcon maintainState dense enableFeedback enabled internalAddSemanticForOnTap");
        add(result, "material.Tooltip", "preferBelow excludeFromSemantics enableTapToDismiss enableFeedback ignorePointer");
        add(result, "material.TooltipVisibility", "visible");
        add(result, "material.TooltipTheme", "preferBelow excludeFromSemantics enableFeedback");
        add(result, "material.MenuItemButton", "enabled autofocus requestFocusOnHover closeOnActivate");
        add(result, "material.MenuAnchor", "consumeOutsideTap crossAxisUnconstrained useRootOverlay animated");
        add(result, "material.SubmenuButton", "useRootOverlay animated");
        add(result, "material.Badge", "isLabelVisible");
        add(result, "material.Card", "semanticContainer borderOnForeground");
        add(result, "material.PaginatedDataTable", "sortAscending showCheckboxColumn showFirstLastButtons showEmptyRows");
        add(result, "material.DataTable", "sortAscending");
        add(result, "material.DataRow", "selected");
        add(result, "material.DataRow.byIndex", "selected");
        for (String type : List.of("LinearProgressIndicator", "CircularProgressIndicator", "RefreshProgressIndicator")) {
            add(result, "material." + type, "semanticsLabel semanticsValue year2023");
        }
        add(result, "material.RefreshIndicator", "semanticsLabel semanticsValue");
        add(result, "material.TextField", "readOnly showCursor autofocus autocorrect enableSuggestions enabled ignorePointers enableInteractiveSelection selectAllOnFocus stylusHandwritingEnabled enableIMEPersonalizedLearning enableInlinePrediction canRequestFocus cursorOpacityAnimates onTapAlwaysCalled restorationId");
        for (String type : List.of("ListView", "GridView")) add(result, "widgets." + type,
                "reverse shrinkWrap addAutomaticKeepAlives addRepaintBoundaries addSemanticIndexes restorationId");
        add(result, "widgets.SingleChildScrollView", "reverse restorationId");
        add(result, "widgets.Offstage", "offstage");
        add(result, "widgets.SafeArea", "left top right bottom maintainBottomViewPadding");
        add(result, "widgets.IgnorePointer", "ignoring ignoringSemantics");
        add(result, "widgets.GestureDetector", "excludeFromSemantics trackpadScrollCausesScale");
        add(result, "widgets.MouseRegion", "opaque");
        add(result, "widgets.Focus", "autofocus canRequestFocus skipTraversal descendantsAreFocusable descendantsAreTraversable includeSemantics");
        add(result, "widgets.AbsorbPointer", "absorbing ignoringSemantics");
        add(result, "widgets.ExcludeSemantics", "excluding");
        add(result, "widgets.BlockSemantics", "blocking");
        add(result, "widgets.ExcludeFocus", "excluding");
        add(result, "widgets.ExcludeFocusTraversal", "excluding");
        add(result, "widgets.TickerMode", "enabled");
        add(result, "widgets.Visibility", "visible");
        add(result, "widgets.ListBody", "reverse");
        add(result, "widgets.RotatedBox", "quarterTurns");
        add(result, "widgets.Transform", "transformHitTests");
        return Map.copyOf(result);
    }

    private static void add(Map<String, Set<String>> result, String type, String names) {
        result.put("flutter." + type, Set.of(names.split(" ")));
    }
}
