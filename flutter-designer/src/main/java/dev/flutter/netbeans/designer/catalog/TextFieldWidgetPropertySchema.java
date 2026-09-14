package dev.flutter.netbeans.designer.catalog;

import dev.flutter.netbeans.designer.model.PropertyName;
import dev.flutter.netbeans.designer.model.WidgetTypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reviewed Designer projection of the stable scalar {@code TextField}
 * constructor surface in Flutter 3.44.8.
 *
 * <p>Controller, focus, formatter, decoration, style and other
 * owner-managed graph values are deliberately outside this projection. The
 * flattened radius and scroll-padding leaves remain independently editable;
 * validation and Dart generation reassemble them into their Flutter values.
 * Counter and context-menu builders use nullable typed project references.</p>
 */
public final class TextFieldWidgetPropertySchema {
    public static final WidgetTypeId TEXT_FIELD_TYPE =
            new WidgetTypeId("flutter.material.TextField");
    public static final int CONSTRUCTOR_PROPERTY_COUNT = 56;
    public static final String INPUT_COUNTER_BUILDER_TYPE = "InputCounterWidgetBuilder?";
    public static final String CONTEXT_MENU_BUILDER_TYPE = "EditableTextContextMenuBuilder?";

    public enum Group {
        INPUT("textFieldInput", "Input",
                "Keyboard, capitalization, suggestions, obscuring, and line limits."),
        LAYOUT("textFieldLayout", "Layout",
                "Text alignment, direction, scrolling inset, and clipping."),
        BEHAVIOR("textFieldBehavior", "Behavior",
                "Editing, focus, interaction, handwriting, and platform behavior."),
        CURSOR_AND_SELECTION("textFieldCursorSelection", "Cursor and selection",
                "Text cursor geometry, colors, selection boxes, and pointer cursor."),
        CALLBACKS("textFieldCallbacks", "Callbacks",
                "Named handlers invoked for editing and pointer events."),
        RESTORATION("textFieldRestoration", "Restoration",
                "Stable restoration metadata for framework-owned field state."),
        BUILDERS("textFieldBuilders", "Builders",
                "Typed counter and context-menu builders; project code is not executed in Canvas.");

        private final String setName;
        private final String displayName;
        private final String description;

        Group(String setName, String displayName, String description) {
            this.setName = setName;
            this.displayName = displayName;
            this.description = description;
        }

        public String setName() {
            return setName;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }

    public enum Target {
        DIRECT,
        KEYBOARD_TYPE_PRESET,
        TEXT_ALIGN_VERTICAL_PRESET,
        MAX_LENGTH,
        CURSOR_RADIUS,
        SCROLL_PADDING,
        MOUSE_CURSOR_PRESET
    }

    /** Metadata for one exact persisted TextField property. */
    public record Definition(
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int dartOrder) {

        public Definition {
            Objects.requireNonNull(group, "group");
            displayName = requireText(displayName, "displayName");
            description = requireText(description, "description");
            Objects.requireNonNull(target, "target");
            dartName = requireText(dartName, "dartName");
            if (dartOrder < 0) {
                throw new IllegalArgumentException("dartOrder must be non-negative");
            }
        }
    }

    private static final Map<String, Definition> DEFINITIONS = definitions();

    private TextFieldWidgetPropertySchema() {
    }

    public static Optional<Definition> find(PropertyName name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(DEFINITIONS.get(name.value()));
    }

    public static Optional<Definition> find(String name) {
        return Optional.ofNullable(DEFINITIONS.get(Objects.requireNonNull(name, "name")));
    }

    public static boolean isSynthesized(PropertyName name) {
        return find(name).map(value -> value.target() != Target.DIRECT).orElse(false);
    }

    /** Returns the 56 reviewed leaves, retaining all original persisted orders. */
    public static Map<String, Definition> definitions() {
        LinkedHashMap<String, Definition> values = new LinkedHashMap<>();
        int order = 0;
        add(values, "keyboardType", Group.INPUT, "Keyboard type",
                "Reviewed TextInputType preset; omitted preserves Flutter line-aware inference.",
                Target.KEYBOARD_TYPE_PRESET, "keyboardType", order++);
        add(values, "textInputAction", Group.INPUT, "Input action",
                "Action button shown by the platform keyboard.", order++);
        add(values, "textCapitalization", Group.INPUT, "Capitalization",
                "Automatic capitalization behavior for entered text.", order++);
        add(values, "textAlign", Group.LAYOUT, "Text align",
                "Horizontal alignment of editable text.", order++);
        add(values, "textAlignVertical", Group.LAYOUT, "Vertical align",
                "Reviewed TextAlignVertical static preset.",
                Target.TEXT_ALIGN_VERTICAL_PRESET, "textAlignVertical", order++);
        add(values, "textDirection", Group.LAYOUT, "Text direction",
                "Explicit right-to-left or left-to-right direction.", order++);
        add(values, "readOnly", Group.BEHAVIOR, "Read only",
                "Whether text can be selected but not modified.", order++);
        add(values, "showCursor", Group.CURSOR_AND_SELECTION, "Show cursor",
                "Explicitly show or hide the text cursor.", order++);
        add(values, "autofocus", Group.BEHAVIOR, "Autofocus",
                "Request focus when the field first appears at runtime.", order++);
        add(values, "obscuringCharacter", Group.INPUT, "Obscuring character",
                "Exactly one BMP Unicode scalar, matching Flutter's one UTF-16-unit assertion.",
                order++);
        add(values, "obscureText", Group.INPUT, "Obscure text",
                "Replace entered text with the obscuring character.", order++);
        add(values, "autocorrect", Group.INPUT, "Autocorrect",
                "Explicitly enable or disable platform autocorrection.", order++);
        add(values, "smartDashesType", Group.INPUT, "Smart dashes",
                "Explicit smart-dash substitution mode.", order++);
        add(values, "smartQuotesType", Group.INPUT, "Smart quotes",
                "Explicit smart-quote substitution mode.", order++);
        add(values, "enableSuggestions", Group.INPUT, "Suggestions",
                "Whether the platform may offer input suggestions.", order++);
        add(values, "maxLines", Group.INPUT, "Maximum lines",
                "Positive maximum line count; omitted preserves Flutter's one-line default.",
                order++);
        add(values, "minLines", Group.INPUT, "Minimum lines",
                "Positive minimum line count no greater than the effective maximum.", order++);
        add(values, "expands", Group.LAYOUT, "Expand",
                "Fill the available height; generated Dart supplies null line limits.", order++);
        add(values, "maxLength", Group.INPUT, "Maximum length",
                "Positive character limit, or -1 for TextField.noMaxLength.",
                Target.MAX_LENGTH, "maxLength", order++);
        add(values, "maxLengthEnforcement", Group.INPUT, "Length enforcement",
                "How input beyond maxLength is handled.", order++);
        add(values, "onChanged", Group.CALLBACKS, "On changed",
                "Handler invoked after the editable value changes.", order++);
        add(values, "onEditingComplete", Group.CALLBACKS, "On editing complete",
                "Handler invoked when editing completes.", order++);
        add(values, "onSubmitted", Group.CALLBACKS, "On submitted",
                "Handler invoked with submitted text.", order++);
        add(values, "onAppPrivateCommand", Group.CALLBACKS, "On app private command",
                "Handler invoked for platform-specific private input commands.", order++);
        add(values, "enabled", Group.BEHAVIOR, "Enabled",
                "Explicitly enable or disable the field.", order++);
        add(values, "ignorePointers", Group.BEHAVIOR, "Ignore pointers",
                "Explicitly ignore pointer input without changing visual enabled state.", order++);
        add(values, "cursorWidth", Group.CURSOR_AND_SELECTION, "Cursor width",
                "Non-negative cursor width in logical pixels.", order++);
        add(values, "cursorHeight", Group.CURSOR_AND_SELECTION, "Cursor height",
                "Optional non-negative cursor height in logical pixels.", order++);
        add(values, "cursorRadiusX", Group.CURSOR_AND_SELECTION, "Cursor radius X",
                "Non-negative horizontal radius; set together with Cursor radius Y.",
                Target.CURSOR_RADIUS, "cursorRadius", order++);
        add(values, "cursorRadiusY", Group.CURSOR_AND_SELECTION, "Cursor radius Y",
                "Non-negative vertical radius; set together with Cursor radius X.",
                Target.CURSOR_RADIUS, "cursorRadius", order++);
        add(values, "cursorOpacityAnimates", Group.CURSOR_AND_SELECTION,
                "Animate cursor opacity", "Explicit cursor opacity animation behavior.", order++);
        add(values, "cursorColor", Group.CURSOR_AND_SELECTION, "Cursor color",
                "Literal ARGB color or semantic Material ColorScheme role.", order++);
        add(values, "cursorErrorColor", Group.CURSOR_AND_SELECTION, "Error cursor color",
                "Cursor color while InputDecorator displays an error.", order++);
        add(values, "selectionHeightStyle", Group.CURSOR_AND_SELECTION,
                "Selection height style", "BoxHeightStyle used for selection highlights.", order++);
        add(values, "selectionWidthStyle", Group.CURSOR_AND_SELECTION,
                "Selection width style", "BoxWidthStyle used for selection highlights.", order++);
        add(values, "keyboardAppearance", Group.INPUT, "Keyboard appearance",
                "Explicit light or dark platform keyboard appearance.", order++);
        add(values, "scrollPaddingLeft", Group.LAYOUT, "Scroll padding left",
                "Non-negative left inset; set together with all scroll-padding edges.",
                Target.SCROLL_PADDING, "scrollPadding", order++);
        add(values, "scrollPaddingTop", Group.LAYOUT, "Scroll padding top",
                "Non-negative top inset; set together with all scroll-padding edges.",
                Target.SCROLL_PADDING, "scrollPadding", order++);
        add(values, "scrollPaddingRight", Group.LAYOUT, "Scroll padding right",
                "Non-negative right inset; set together with all scroll-padding edges.",
                Target.SCROLL_PADDING, "scrollPadding", order++);
        add(values, "scrollPaddingBottom", Group.LAYOUT, "Scroll padding bottom",
                "Non-negative bottom inset; set together with all scroll-padding edges.",
                Target.SCROLL_PADDING, "scrollPadding", order++);
        add(values, "dragStartBehavior", Group.BEHAVIOR, "Drag start behavior",
                "Whether drag coordinates begin at pointer down or drag start.", order++);
        add(values, "enableInteractiveSelection", Group.BEHAVIOR,
                "Interactive selection", "Explicitly enable or disable selection gestures.", order++);
        add(values, "selectAllOnFocus", Group.BEHAVIOR, "Select all on focus",
                "Explicitly select all text whenever the field gains focus.", order++);
        add(values, "onTap", Group.CALLBACKS, "On tap",
                "Handler invoked for field taps.", order++);
        add(values, "onTapAlwaysCalled", Group.CALLBACKS, "Always call on tap",
                "Invoke onTap for consecutive taps as well as the first tap.", order++);
        add(values, "onTapOutside", Group.CALLBACKS, "On tap outside",
                "Handler invoked for pointer-down events outside the tap region.", order++);
        add(values, "onTapUpOutside", Group.CALLBACKS, "On tap up outside",
                "Handler invoked for pointer-up events outside the tap region.", order++);
        add(values, "mouseCursor", Group.CURSOR_AND_SELECTION, "Mouse cursor",
                "Reviewed SystemMouseCursors preset.",
                Target.MOUSE_CURSOR_PRESET, "mouseCursor", order++);
        add(values, "clipBehavior", Group.LAYOUT, "Clip behavior",
                "Clipping mode for editable content.", order++);
        add(values, "restorationId", Group.RESTORATION, "Restoration ID",
                "Non-empty stable identifier for framework state restoration.", order++);
        add(values, "stylusHandwritingEnabled", Group.BEHAVIOR, "Stylus handwriting",
                "Whether stylus handwriting input is enabled.", order++);
        add(values, "enableIMEPersonalizedLearning", Group.BEHAVIOR,
                "IME personalized learning", "Allow the input method to learn personalized data.",
                order++);
        add(values, "enableInlinePrediction", Group.BEHAVIOR, "Inline prediction",
                "Explicitly enable or disable inline platform predictions.", order++);
        add(values, "canRequestFocus", Group.BEHAVIOR, "Can request focus",
                "Whether the runtime field may request primary focus.", order++);
        add(values, "buildCounter", Group.BUILDERS, "Counter builder",
                "Nullable InputCounterWidgetBuilder project reference or explicit null. Omission/null uses the default counter; a callback returning null hides the counter and its Semantics. Required named parameters are currentLength, maxLength (int?) and isFocused. Project code never runs in Canvas.", order++);
        add(values, "contextMenuBuilder", Group.BUILDERS, "Context menu builder",
                "Nullable EditableTextContextMenuBuilder project reference or explicit null. Omission uses the SDK menu; explicit null disables it. A non-null callback receives BuildContext and EditableTextState and returns a non-null Widget. Project code never runs in Canvas.", order++);

        if (values.size() != CONSTRUCTOR_PROPERTY_COUNT || order != CONSTRUCTOR_PROPERTY_COUNT) {
            throw new ExceptionInInitializerError(
                    "TextField schema must expose exactly " + CONSTRUCTOR_PROPERTY_COUNT
                    + " properties; actual=" + values.size());
        }
        return Collections.unmodifiableMap(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            int order) {
        add(values, name, group, displayName, description,
                Target.DIRECT, name, order);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            Group group,
            String displayName,
            String description,
            Target target,
            String dartName,
            int order) {
        Definition definition = new Definition(
                group, displayName, description, target, dartName, order);
        if (values.putIfAbsent(name, definition) != null) {
            throw new IllegalStateException("Duplicate TextField property schema: " + name);
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
