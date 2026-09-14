# ListWheelScrollView contract

Flutter Designer targets the pinned Flutter 3.44.8
`widgets.ListWheelScrollView` constructor as a complete vertical slice.

## Properties and slots

The scrolling palette entry exposes all 18 reviewed constructor properties in
SDK order: `controller`, a closed `ScrollPhysics` preset, wheel geometry
(`diameterRatio`, `perspective`, `offAxisFraction`, `useMagnifier`,
`magnification`, `overAndUnderCenterOpacity`, required `itemExtent` and
`squeeze`), `onSelectedItemChanged`, `renderChildrenOutsideViewport`,
`clipBehavior`, `hitTestBehavior`, `restorationId`, `scrollBehavior`,
`dragStartBehavior` and `changeReportingBehavior`.

`itemExtent` is required by the SDK and is created with a positive 50 logical
pixel value. The model validates the SDK geometry ranges: diameter ratio,
magnification and squeeze are positive; perspective is greater than zero and
at most 0.01; opacity is between zero and one. Restoration IDs are non-empty.

The `children` list is the only slot. It is optional in the model so an empty
wheel can be created, and accepts any reviewed widget. Child order and stable
IDs are retained through palette DnD, Slots editing, Save/reopen and Undo/Redo.

## Callbacks, references and generation

`onSelectedItemChanged` is a typed `ValueChanged<int>` reference and is also
available through the Events tab. `controller`, `scrollBehavior` and callback
references preserve omission, explicit null and analyzed project bindings.
Generated Dart emits the exact values and imports; the isolated runner never
executes application controllers, behaviors, callbacks or factories.

`renderChildrenOutsideViewport: true` is only valid with `clipBehavior: none`
in Flutter. The editor reports the conflict without silently changing either
stored value. Physics and enum editors use the reviewed SDK presets and keep
the native default when reset.

## Canvas and verification

Canvas mounts the native SDK `ListWheelScrollView`, including geometry,
clipping, hit testing, restoration and ordered children. A preview diagnostic
identifies project-owned controller/behavior/callback references; it does not
claim to reproduce their application runtime. The list remains selectable and
drop-aware even when an empty wheel has no useful layout bounds.

`ListWheelScrollViewWidgetPropertySchemaTest`,
`ListWheelScrollViewDartGenerationTest` and
`canvas_list_wheel_scroll_view_test.dart` cover schema, generation, protocol,
native fields and ordered children. The palette, Properties, Slots,
Save/reopen, history and source-manifest checks are part of the Java module
verification. `CustomScrollView`/sliver constructors are a separate future
slice; CJK IME and Linux/macOS Canvas providers remain outside this palette
milestone by agreement.
