package dev.flutter.netbeans.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DartStaticTypeProbeTest {
    @Test void shaderCallbackRequiresReviewedFlutterContext() {
        for (String uri : List.of("package:flutter/widgets.dart", "package:flutter/material.dart"))
            assertEquals("ShaderCallback", new DartStaticTypeProbe(10,5,0,5,"ShaderCallback",uri).expectedDartType());
        for (String uri : List.of("dart:ui", "dart:core", "package:app/fake.dart", "package:flutter/rendering.dart"))
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,"ShaderCallback",uri));
    }

    @Test void rawImageAndNullableAnimationHaveClosedReviewedProofContexts() {
        for (String type : List.of("Image?", "Rect?", "Animation<double>?")) {
            for (String uri : List.of("package:flutter/widgets.dart", "package:flutter/material.dart"))
                assertEquals(type, new DartStaticTypeProbe(10,5,0,5,type,uri).expectedDartType());
            for (String uri : List.of("package:app/fake.dart", "dart:core", "dart:ui"))
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,uri));
        }
        for (String type : List.of("ui.Image?", "Animation<dynamic>?", "Animation<double?>?", "Rect?;exit()"))
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,"package:flutter/widgets.dart"));
    }

    @Test void fadeInImageProviderAndNullableErrorBuilderRequireReviewedFlutterContext() {
        for (String type : List.of("ImageProvider<Object>", "ImageErrorWidgetBuilder?")) {
            for (String uri : List.of("package:flutter/widgets.dart", "package:flutter/material.dart"))
                assertEquals(type, new DartStaticTypeProbe(10,5,0,5,type,uri).expectedDartType());
            for (String uri : List.of("package:app/fake.dart", "dart:core", "package:flutter/foundation.dart"))
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,uri));
        }
    }

    @Test
    void modalBarrierNullableCallbackAndNotifierUseExactReviewedContexts() {
        for (String type : List.of("VoidCallback?", "ValueNotifier<EdgeInsets>?")) {
            assertEquals(type, probe(type).expectedDartType());
            for (String uri : List.of("package:app/fake.dart", "package:flutter/foundation.dart", "dart:core"))
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,uri));
        }
        for (String type : List.of("ValueNotifier<EdgeInsetsGeometry>?", "ValueNotifier<EdgeInsets?>?", "ValueNotifier<dynamic>?", "ValueNotifier<ui.EdgeInsets>?", "ValueNotifier<EdgeInsets>?;exit()"))
            assertThrows(IllegalArgumentException.class, () -> probe(type));
    }

    @Test
    void acceptsClosedTypesWithReviewedNullableScalarsAndOneOptionalNullableArgument() {
        for (String type : List.of("ShapeBorder", "CustomClipper<RRect>",
                "Animation<Color>", "Animation<Color?>", "AnimationController", "Object?",
                "bool?", "String?", "int?", "double?", "num?", "FocusNode?")) {
            assertEquals(type, probe(type).expectedDartType());
        }
    }

    @Test
    void appBarNullableStylesAndFixedFoundationServicesWitnessesStayClosed() {
        for (String type : List.of("ShapeBorder?", "IconThemeData?", "TextStyle?", "SystemUiOverlayStyle?", "AsyncCallback"))
            assertEquals(type, probe(type).expectedDartType());
        for (String type : List.of("AsyncCallback", "SystemUiOverlayStyle?")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,"package:app/fake.dart"));
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,"package:flutter/services.dart"));
        }
    }

    @Test
    void sourceTypeOverridesAreClosedRadioIdentitiesAndPreserveLegacyConstructor() {
        assertEquals(Optional.empty(), probe("Object").sourceTypeOverride());
        for (String expected : List.of("Type", "Object", "Object?", "ValueChanged<Object?>", "RadioGroupRegistry<Object>")) {
            for (String type : List.of("String", "Object?", "_LocalEnum", "project_alias.Choice?")) {
                assertEquals(Optional.of(type), new DartStaticTypeProbe(10, 5, 0, 5, expected,
                        "package:flutter/widgets.dart", Optional.of(type)).sourceTypeOverride());
            }
        }
        for (String type : List.of("", "Choice??", "a.b.c", "List<Choice>", "void Function()",
                "Choice;exit()", "Choice /* comment */", " Choice", "Choice\n")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "Object", "package:flutter/widgets.dart", Optional.of(type)), type);
        }
        assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                "FocusNode", "package:flutter/widgets.dart", Optional.of("Other")));
        assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                "FocusNode?", "package:flutter/widgets.dart", Optional.of("Other")));
    }

    @Test
    void notificationTypesHaveAClosedIndependentBoundAndExactCallbackArgument() {
        var selected = new DartStaticTypeProbe(10, 5, 0, 5, "Type", "package:flutter/widgets.dart",
                Optional.of("project.CustomNotification"), Optional.of("Notification"));
        assertEquals(Optional.of("Notification"), selected.sourceTypeBound());
        assertEquals(Optional.empty(), probe("Type").sourceTypeBound());
        assertEquals(Optional.of("project.CustomNotification"), new DartStaticTypeProbe(10, 5, 0, 5,
                "NotificationListenerCallback<Notification>", "package:flutter/widgets.dart",
                Optional.of("project.CustomNotification")).sourceTypeOverride());
        for (String bound : List.of("Object", "Notification?", "project.Notification", "Notification;exit()", "")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "Type", "package:flutter/widgets.dart", Optional.of("Custom"), Optional.of(bound)));
        }
        for (String type : List.of("Custom?", "p.Custom?")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "Type", "package:flutter/widgets.dart", Optional.of(type), Optional.of("Notification")));
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "NotificationListenerCallback<Notification>", "package:flutter/widgets.dart", Optional.of(type)));
        }
        assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                "Type", "package:flutter/widgets.dart", Optional.empty(), Optional.of("Notification")));
        assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                "Object", "package:flutter/widgets.dart", Optional.of("Custom"), Optional.of("Notification")));
    }

    @Test
    void admitsOnlyTheExactReviewedScaffoldFunctionSignatureWithFlutterScope() {
        String signature = "Widget? Function(BuildContext, Animation<double>)";
        assertEquals(signature, probe(signature).expectedDartType());
        for (String invalid : List.of("Widget Function(BuildContext, Animation<double>)",
                "Widget? Function(BuildContext, Animation<num>)", "Widget? Function(BuildContext?, Animation<double>)",
                "Widget? Function(BuildContext, Animation<double>)?", "Widget? Function(BuildContext,Animation<double>)",
                "Widget? Function(BuildContext, Animation<double>) /* ignored */", "Widget? Function()",
                "Widget? Function(BuildContext, Animation<double>);exit()")) {
            assertThrows(IllegalArgumentException.class, () -> probe(invalid), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                signature, "package:app/fake.dart"));
        assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                signature, "package:flutter/widgets.dart", Optional.of("Custom")));
    }

    @Test
    void admitsOnlyReviewedNullableTextFieldBuildersAndSdkLibraries() {
        for (String suffix : List.of("", "?")) {
            String counter = "InputCounterWidgetBuilder" + suffix;
            String menu = "EditableTextContextMenuBuilder" + suffix;
            assertEquals(counter, new DartStaticTypeProbe(10, 5, 0, 5, counter,
                    "package:flutter/material.dart").expectedDartType());
            assertEquals(menu, probe(menu).expectedDartType());
            assertEquals(menu, new DartStaticTypeProbe(10, 5, 0, 5, menu,
                    "package:flutter/material.dart").expectedDartType());
            assertThrows(IllegalArgumentException.class, () -> probe(counter));
            for (String name : List.of(counter, menu)) {
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                        name, "package:app/fake.dart"));
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                        name, "package:flutter/material.dart", Optional.of("Custom")));
            }
        }
        for (String type : List.of("InputCounterWidgetBuilder??", "EditableTextContextMenuBuilder??",
                "WidgetBuilder?", "InputCounterWidgetBuilder?;exit()", "Widget? Function(BuildContext, {required int currentLength})")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    type, "package:flutter/material.dart"), type);
        }
    }

    @Test
    void itemExtentBuilderHasAClosedNullableFormAndReviewedSdkContext() {
        for (String type : List.of("ItemExtentBuilder", "ItemExtentBuilder?")) {
            for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
                assertEquals(type, new DartStaticTypeProbe(10, 5, 0, 5, type, library).expectedDartType());
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                        type, library, Optional.of("Custom")));
            }
            for (String library : List.of("package:app/fake.dart", "package:flutter/rendering.dart")) {
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5, type, library));
            }
        }
        for (String type : List.of("ItemExtentBuilder??", "SliverLayoutDimensions?", "SliverLayoutDimensions;exit()",
                "double? Function(int, SliverLayoutDimensions)", "ItemExtentBuilder ?")) {
            assertThrows(IllegalArgumentException.class, () -> probe(type), type);
        }
    }

    @Test
    void sliverLayoutBuilderUsesOnlyReviewedNonNullableSdkTypeAndLibrary() {
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            assertEquals("SliverLayoutWidgetBuilder", new DartStaticTypeProbe(10, 5, 0, 5,
                    "SliverLayoutWidgetBuilder", library).expectedDartType());
        }
        for (String library : List.of("package:app/fake.dart", "package:flutter/rendering.dart", "dart:core")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "SliverLayoutWidgetBuilder", library));
        }
        for (String type : List.of("SliverLayoutWidgetBuilder?", "SliverLayoutWidgetBuilder;exit()",
                "Widget Function(BuildContext, SliverConstraints)")) {
            assertThrows(IllegalArgumentException.class, () -> probe(type));
        }
    }

    @Test
    void boxLayoutBuilderUsesOnlyReviewedNonNullableSdkTypeAndLibrary() {
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            assertEquals("LayoutWidgetBuilder", new DartStaticTypeProbe(10, 5, 0, 5,
                    "LayoutWidgetBuilder", library).expectedDartType());
        }
        for (String library : List.of("package:app/fake.dart", "package:flutter/rendering.dart", "dart:core")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "LayoutWidgetBuilder", library));
        }
        for (String type : List.of("LayoutWidgetBuilder?", "LayoutWidgetBuilder;exit()",
                "Widget Function(BuildContext, BoxConstraints)")) {
            assertThrows(IllegalArgumentException.class, () -> probe(type));
        }
    }

    @Test
    void valueListenableProofUsesClosedSelectedTypeAndSdkLibrary() {
        for(String family:List.of("ValueListenable<Object>","ValueWidgetBuilder<Object>","Tween<Object>"))
            for(String selected:List.of("double","String?","types.Rows","types.Payload?")) {
                var probe=new DartStaticTypeProbe(10,5,0,5,family,"package:flutter/widgets.dart",java.util.Optional.of(selected));
                assertEquals(selected,probe.sourceTypeOverride().orElseThrow());
                assertThrows(IllegalArgumentException.class,()->new DartStaticTypeProbe(10,5,0,5,family,"package:app/fake.dart",java.util.Optional.of(selected)));
            }
        for(String bad:List.of("List<int>","T;exit()","T??","types.Rows()"))
            assertThrows(IllegalArgumentException.class,()->new DartStaticTypeProbe(10,5,0,5,"ValueListenable<Object>","package:flutter/widgets.dart",java.util.Optional.of(bad)));
    }

    @Test
    void listenableBuilderUsesOnlyReviewedNonNullableSdkTypesAndLibraries() {
        for(String type:List.of("Listenable","TransitionBuilder")) {
            for(String library:List.of("package:flutter/widgets.dart","package:flutter/material.dart"))
                assertEquals(type,new DartStaticTypeProbe(10,5,0,5,type,library).expectedDartType());
            for(String library:List.of("package:app/fake.dart","package:flutter/rendering.dart","dart:core"))
                assertThrows(IllegalArgumentException.class,()->new DartStaticTypeProbe(10,5,0,5,type,library));
            assertThrows(IllegalArgumentException.class,()->probe(type+"?"));
        }
    }

    @Test
    void orientationBuilderUsesOnlyReviewedNonNullableSdkTypeAndLibrary() {
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            assertEquals("OrientationWidgetBuilder", new DartStaticTypeProbe(10, 5, 0, 5,
                    "OrientationWidgetBuilder", library).expectedDartType());
        }
        for (String library : List.of("package:app/fake.dart", "package:flutter/rendering.dart", "dart:core")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5,
                    "OrientationWidgetBuilder", library));
        }
        for (String type : List.of("OrientationWidgetBuilder?", "OrientationWidgetBuilder;exit()",
                "Widget Function(BuildContext, Orientation)")) {
            assertThrows(IllegalArgumentException.class, () -> probe(type));
        }
    }

    @Test
    void rejectsNullableOuterTypesAndTypeExpressionEscapeHatches() {
        for (String type : List.of("", "Animation?", "FocusNode??", "FocusScopeNode?", "MouseCursor?", "Animation<Color>?",
                "Animation<Color?>?", "Animation<Color??>", "Animation< Color?>",
                "Animation<Color? >", "Animation<other.Color?>", "other.Animation<Color?>",
                "Animation<List<Color?>>", "Animation<Color?, Color>", "Animation<Color?>;exit()",
                "Animation<Color/*comment*/?>", "Animation<Color>\n", "Animation<>")) {
            assertThrows(IllegalArgumentException.class, () -> probe(type), type);
        }
    }

    @Test void crossFadeBuilderRequiresExactNonNullableWidgetsContext() {
        for(String library:List.of("package:flutter/widgets.dart","package:flutter/material.dart"))
            assertEquals("AnimatedCrossFadeBuilder",new DartStaticTypeProbe(10,5,0,5,"AnimatedCrossFadeBuilder",library).expectedDartType());
        for(String library:List.of("package:app/fake.dart","package:flutter/rendering.dart","dart:core"))
            assertThrows(IllegalArgumentException.class,()->new DartStaticTypeProbe(10,5,0,5,"AnimatedCrossFadeBuilder",library));
        assertThrows(IllegalArgumentException.class,()->probe("AnimatedCrossFadeBuilder?"));
    }

    @Test void switcherBuildersRequireExactNonNullableWidgetsContext() {
        for(String type:List.of("AnimatedSwitcherTransitionBuilder","AnimatedSwitcherLayoutBuilder")) {
            for(String library:List.of("package:flutter/widgets.dart","package:flutter/material.dart"))
                assertEquals(type,new DartStaticTypeProbe(10,5,0,5,type,library).expectedDartType());
            for(String library:List.of("package:app/fake.dart","package:flutter/rendering.dart","dart:core"))
                assertThrows(IllegalArgumentException.class,()->new DartStaticTypeProbe(10,5,0,5,type,library));
            assertThrows(IllegalArgumentException.class,()->probe(type+"?"));
        }
    }
    @Test void animatedIconDataUsesReviewedMaterialLibrary() {
        assertEquals("AnimatedIconData", new DartStaticTypeProbe(10,5,0,5,"AnimatedIconData","package:flutter/material.dart").expectedDartType());
        for (String library : List.of("package:flutter/widgets.dart","dart:core","package:app/fake.dart"))
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,"AnimatedIconData",library));
        for (String type : List.of("AnimatedIconData?","AnimatedIconData;exit()"))
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,"package:flutter/material.dart"));
    }
    @Test void colorFilterRequiresExactNonNullableReviewedSdkContext() {
        for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart")) {
            assertEquals("ColorFilter", new DartStaticTypeProbe(10, 5, 0, 5, "ColorFilter", library).expectedDartType());
        }
        for (String library : List.of("package:app/fake.dart", "package:flutter/rendering.dart", "dart:core")) {
            assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10, 5, 0, 5, "ColorFilter", library));
        }
        for (String type : List.of("ColorFilter?", "ColorFilter;exit()", "other.ColorFilter")) {
            assertThrows(IllegalArgumentException.class, () -> probe(type));
        }
    }

    @Test void imageFilterShaderAndTypedStorageUseOnlyReviewedProofContexts() {
        for (String type : List.of("ImageFilter", "FragmentShader", "Float64List")) {
            for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart"))
                assertEquals(type, new DartStaticTypeProbe(10,5,0,5,type,library).expectedDartType());
            for (String library : List.of("package:app/fake.dart", "package:flutter/rendering.dart", "dart:core"))
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,library));
            assertThrows(IllegalArgumentException.class, () -> probe(type + "?"));
        }
    }

    @Test void backdropTypesUseOnlyFixedRenderingWitnessContext() {
        for (String type : List.of("ImageFilterConfig", "BackdropKey?")) {
            for (String library : List.of("package:flutter/widgets.dart", "package:flutter/material.dart"))
                assertEquals(type, new DartStaticTypeProbe(10,5,0,5,type,library).expectedDartType());
            for (String library : List.of("package:app/fake.dart", "package:flutter/rendering.dart", "dart:core"))
                assertThrows(IllegalArgumentException.class, () -> new DartStaticTypeProbe(10,5,0,5,type,library));
        }
        assertThrows(IllegalArgumentException.class, () -> probe("ImageFilterConfig?"));
        assertThrows(IllegalArgumentException.class, () -> probe("BackdropKey??"));
    }

    private static DartStaticTypeProbe probe(String type) {
        return new DartStaticTypeProbe(10, 5, 0, 5, type, "package:flutter/widgets.dart");
    }
}
