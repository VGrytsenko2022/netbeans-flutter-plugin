package io.github.vgrytsenko2022.designer.catalog;

import io.github.vgrytsenko2022.designer.model.*;
import java.time.LocalTime;
import java.util.*;

/** All Flutter 3.44.8 TimePickerDialog arguments: thirteen properties and two Icon slots. */
public final class TimePickerDialogWidgetPropertySchema {
    public static final WidgetTypeId TYPE=new WidgetTypeId("flutter.material.TimePickerDialog");
    public static final String TIME_PATTERN="(?:[01][0-9]|2[0-3]):[0-5][0-9]";
    public static final String DESCRIPTION="Material time picker dialog. Confirm returns TimeOfDay through Navigator; cancel returns null. "
            +"onEntryModeChanged reports input/dial changes, not time selection. Initial time, entry mode and orientation are native restorable state. "
            +"Canvas never executes project sources or closes a Navigator route.";
    public static List<PropertyDefinition> properties() {
        var shared=DatePickerDialogWidgetPropertySchema.properties().stream().collect(java.util.stream.Collectors.toMap(p->p.name().value(),p->p));
        var result=new ArrayList<PropertyDefinition>();
        for(String name:List.of("key","initialTime","cancelText","confirmText","helpText","errorInvalidText","hourLabelText",
                "minuteLabelText","restorationId","initialEntryMode","orientation","onEntryModeChanged","emptyInitialInput")) {
            var p=shared.get(name);
            List<PropertyValueConstraint> c;
            if(name.equals("initialTime"))c=List.of(new PropertyValueConstraint.StringPattern(TIME_PATTERN,"24-hour time HH:mm"),
                    new PropertyValueConstraint.DartObjectReferenceValues("TimeOfDay"));
            else if(name.equals("initialEntryMode"))c=List.of(en("package:flutter/material.dart","TimePickerEntryMode","dial","input","dialOnly","inputOnly"));
            else if(name.equals("orientation"))c=List.of(en("package:flutter/widgets.dart","Orientation","portrait","landscape"),nil());
            else if(name.equals("onEntryModeChanged"))c=List.of(new PropertyValueConstraint.StringPattern("noop","Explicit no-op callback"),
                    new PropertyValueConstraint.DartObjectReferenceValues("EntryModeChangeCallback"),nil());
            else if(name.equals("emptyInitialInput"))c=List.of(new PropertyValueConstraint.AnyValue(PropertyValueKind.BOOLEAN));
            else c=p==null?List.of(new PropertyValueConstraint.StringLength(0,16384),nil()):p.constraints();
            result.add(new PropertyDefinition(new PropertyName(name),DartParameter.named(result.size(),name.equals("initialTime")),c,
                    name.equals("initialTime")?Optional.of(new PropertyValue.StringValue("09:00")):Optional.empty()));
        }
        if(result.size()!=13)throw new IllegalStateException("TimePickerDialog property count changed");
        return List.copyOf(result);
    }
    public static LocalTime time(String value) {
        if(value==null||!value.matches(TIME_PATTERN))throw new IllegalArgumentException("Use 24-hour time HH:mm: hours 00–23 and minutes 00–59.");
        return LocalTime.of(Integer.parseInt(value.substring(0,2)),Integer.parseInt(value.substring(3,5)));
    }
    public static List<String> presets(String name) { return name.equals("onEntryModeChanged")?List.of("noop"):List.of(); }
    public static String label(String name) { return DatePickerDialogWidgetPropertySchema.label(name); }
    public static String group(String name) { return name.equals("onEntryModeChanged")?"Events":name.endsWith("Text")?"Labels and validation":"Time and behavior"; }
    public static String help(String name) {
        return switch(name) {
            case "initialTime" -> "Required HH:mm (00:00–23:59), or a verified TimeOfDay reference/getter/factory. No date, timezone or seconds. Initial/restorable value, not controlled State; change Key to reset.";
            case "initialEntryMode" -> "dial, input, dialOnly or inputOnly. Only variants hide the toggle button. Initial/restorable mode; the native mode-change callback does not return a selected time.";
            case "orientation" -> "Optional portrait/landscape override. Null/omission follows MediaQuery size; forced orientation is initial/restorable dialog state. Change Key to reset.";
            case "onEntryModeChanged" -> "Optional void Function(TimePickerEntryMode). Called on an entry-mode change, not time selection or confirm/cancel. Await the Navigator TimeOfDay/null result.";
            case "emptyInitialInput" -> "Default false. Initially empty hour/minute input fields instead of initialTime; applies to input/inputOnly, not the dial. Toggling into input may create empty fields too.";
            case "restorationId" -> "Persists selected time, mode, orientation and validation state in a surrounding RestorationScope. Restoring a dismissed route also requires a restorable DialogRoute.";
            case "key" -> "String ValueKey, typed Key, null or omission. Change Key to recreate initial/restorable dialog state.";
            default -> "Optional localized "+name+" override. Null/omission keeps the native localized label; empty text is explicit.";
        };
    }
    private static PropertyValueConstraint nil(){return new PropertyValueConstraint.AnyValue(PropertyValueKind.NULL);}
    private static PropertyValueConstraint en(String uri,String type,String...values){return new PropertyValueConstraint.EnumValues(new DartSymbolReference(uri,type),List.of(values));}
    private TimePickerDialogWidgetPropertySchema(){}
}
