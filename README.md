# Flutter and Dart Support for Apache NetBeans

Starter architecture for first-class Dart + Flutter support in Apache NetBeans 31.

## Developer contact and project support

- Email: [hrytsenkovalentyn@gmail.com](mailto:hrytsenkovalentyn@gmail.com)
- Telegram channel: [netbeans_flutter_plugin](https://t.me/netbeans_flutter_plugin)
- Support the project: [Donate via PayPal](https://www.paypal.com/donate/?hosted_button_id=GRQBC554NA356)

If you find this plugin useful, please consider supporting its development.
These contacts and the donation link are also available in the NetBeans Plugin Description.

Current palette milestone: **245 admitted built-in definitions**, now including
`BottomSheet`, `SimpleDialog` and `SimpleDialogOption`,
`AlertDialog`, `AlertDialog.adaptive`, `Dialog`, `Dialog.fullscreen`, `TimePickerDialog`, `InputDatePickerFormField`, `CalendarDatePicker`, `DateRangePickerDialog`, `DatePickerDialog` and `PaginatedDataTable` with a typed, long-lived `DataTableSource`,
`DataTable`, `DataColumn`, `DataRow`, `DataRow.byIndex`, `DataCell`, `DataCell.empty`, `Table`, `TableRow`, `TableCell`, `Flow`, `Flow.unwrapped`, `CustomMultiChildLayout`, `LayoutId`, `CustomSingleChildLayout`, `CustomPaint`, `ShaderMask`, `BackdropFilter`, `BackdropFilter.grouped`, `BackdropGroup`, `ImageFiltered`, `ColorFiltered`, `RawImage`, `FadeInImage`, `AnimatedIcon`, `AnimatedModalBarrier`, `ModalBarrier`, `MatrixTransition`, `AlignTransition`, `DecoratedBoxTransition`, `RelativePositionedTransition`, `PositionedTransition`, `SizeTransition`, `RotationTransition`, `ScaleTransition`, `SlideTransition`, `FadeTransition`, `SliverFadeTransition`, `DefaultTextStyleTransition`, `DefaultTextStyle`, `DefaultTextStyle.merge`, `Theme`, `AnimatedTheme`, `AnimatedSwitcher`, `AnimatedCrossFade`, `AnimatedFractionallySizedBox`, `AnimatedPhysicalModel`, `AnimatedDefaultTextStyle`, `AnimatedPositioned`, `AnimatedPositioned.fromRect`, `AnimatedPositionedDirectional`, `AnimatedSize`, `AnimatedContainer`, `AnimatedRotation`, `AnimatedScale`, `AnimatedSlide`, `AnimatedPadding`, `AnimatedAlign`, `AnimatedOpacity`,
`TweenAnimationBuilder`, `ValueListenableBuilder`, `AnimatedBuilder`, `ListenableBuilder` and `DeviceOrientationBuilder` (box and sliver projections), `OrientationBuilder`, `LayoutBuilder`, `FlexibleSpaceBarSettings`, `FlexibleSpaceBar`, `SliverAppBar`, `SliverAppBar.medium`, `SliverAppBar.large`,
`MenuAnchor`, `MenuItemButton`, `SubmenuButton`, `MenuBar`, `NavigationBar`, `NavigationRail`, `TooltipTheme`, `TooltipVisibility`,
`Tooltip`, `ExpansionTile`, both `RadioListTile` constructors, `SwitchListTile`,
`NavigationDrawer`, `Drawer`, `BottomAppBar` and `BottomNavigationBar`,
the five interaction wrappers, `Material`, `Scrollbar`, `PageView`,
`GridView.extent`, `ListWheelScrollView`, `CustomScrollView`, `SliverMainAxisGroup`, `SliverCrossAxisGroup`,
`SliverVariedExtentList.list`, `SliverVariedExtentList.builder`, `SliverVariedExtentList.new`,
`SliverPrototypeExtentList.list`, `SliverPrototypeExtentList.builder`, `SliverPrototypeExtentList.new`,
`SliverFixedExtentList.list`, `SliverFixedExtentList.builder`, `SliverFixedExtentList.new`,
`SliverFloatingHeader`, `PinnedHeaderSliver`, `SliverResizingHeader`, `SliverPersistentHeader`, `SliverLayoutBuilder`, `SliverAnimatedOpacity`, `SliverSafeArea`, `SliverVisibility`, `SliverVisibility.maintain`, `SliverOffstage`, `SliverIgnorePointer`, `SliverOpacity`, `SliverConstrainedCrossAxis`, `SliverCrossAxisExpanded`, `SliverFillViewport`, `SliverFillViewport.delegate`, `SliverFillRemaining`, `SliverPadding`, `SliverToBoxAdapter`, `SliverList.list`, `SliverGrid.count`, `SliverGrid.extent`,
`PreferredSize` and `Builder`.
The historical 92-widget number is a planning target, not the complete Flutter
inventory or a verified remaining-work count. The full original ordered list
is not preserved; further widgets are admitted from the pinned Flutter API.
The current catalog has **8,214 writable rows** (8,196 outside `Scaffold`) and
**201 const-capable definitions**. All 245 definitions have reviewed
Canvas/Create/DnD capability; 236 expose typed Properties and nine are
propertyless/structural definitions. Native Events comprise **200 rows across
72 widget types**; all supported callables total 271 across 101 widget types,
including one positioning delegate, seven sliver child-index delegates, one
SliverFillViewport semantic-index callback, one matrix transform delegate and one shader computation delegate.
Formats are FD 17, Catalog API 16, Canvas model 20 and transport 1.
Historical milestone totals below are not current remaining-work counts.

### BottomSheet: native surface, content and drag events

All 16 constructor arguments, 37 editable rows including ten shape families,
Child or typed WidgetBuilder, and three Events with exact named isClosing.
Creation sets enableDrag/showDragHandle false until a controller is supplied.
The application owns routes, dismissal and controller disposal. Isolated
Canvas owns only its preview controller and never runs project code.
See [BottomSheet](docs/BOTTOM_SHEET.md).

### SimpleDialog and SimpleDialogOption: native choices

All 17 / 4 constructor arguments: 98 / 3 writable rows, Title/Children/Child
slots, two complete TextStyle families and ten shape families. The option's
nullable onPressed Event supports user handlers and verified Dart sources;
unset/null disables selection. Dialog results remain with showDialog and
Navigator.pop. Canvas uses native widgets without executing project sources.
See [SimpleDialog and SimpleDialogOption](docs/SIMPLE_DIALOG.md).

### AlertDialog: Material and adaptive alerts

All 28 / 32 native constructor arguments: 107 / 111 editable rows, four optional
Icon/Title/Content/Actions slots, two complete TextStyle families and ten local
shapes. Adaptive follows the preview profile's target platform, preserving native
Material versus Cupertino argument behavior. See [AlertDialog](docs/ALERT_DIALOG.md).

### Dialog: standard and fullscreen surfaces

Both const constructors expose every native argument: 34 editable rows for
Dialog (including ten complete local shape families) and five for
Dialog.fullscreen, each with an optional Child. Typed sources, theme defaults,
physical keyboard insets, 43 animation curves and 33 semantic roles are retained.
Routes own dismissal/results; child widgets own Events. See [Dialog](docs/DIALOG.md)
for source and accessibility preview boundaries.

### TimePickerDialog: time input, entry modes and route results

All 15 native constructor arguments: 13 property rows and two Icon-only slots.
Structured HH:mm editing or verified TimeOfDay sources; dial/input and both
only-modes, orientation, localized labels, empty initial input and restoration.
The mode-change Event does not return a selected time: await the Navigator result.
Canvas isolates interaction and source execution. See [TimePickerDialog](docs/TIME_PICKER_DIALOG.md).

### InputDatePickerFormField: date input, validation and form Events

All 16 constructor arguments, locale-aware parsing, inclusive bounds, custom
calendar/predicate sources, all keyboard presets and application-owned focus.
Submit and Form.save are distinct Events. Empty validation never emits null;
changed initialDate updates same-key native text. Canvas isolates project code.
See [InputDatePickerFormField](docs/INPUT_DATE_PICKER_FORM_FIELD.md).

### CalendarDatePicker: inline calendar and native Events

All ten constructor arguments, nullable initial selection, typed date/calendar
sources, day/year modes, selection/month Events and an editable date predicate.
Native state is initial-only; change Key to reset it. Canvas previews local dates
without executing project code. See [CalendarDatePicker](docs/CALENDAR_DATE_PICKER.md).

### DateRangePickerDialog: inclusive ranges and native route results

All 23 constructor arguments, a two-field range editor, typed DateTimeRange and
custom CalendarDelegate sources, four entry modes and two Icon-only slots.
Create/edit the three-argument selectable-day predicate. Save/OK returns the
range through Navigator; the isolated Canvas does not execute project sources.
See [DateRangePickerDialog](docs/DATE_RANGE_PICKER_DIALOG.md).

### DatePickerDialog: dates, calendars and route results

All 22 constructor arguments are covered by 20 typed property rows and two
Icon-only visual slots. Dates support validated Gregorian values or typed
DateTime sources; calendars support Gregorian or project CalendarDelegate.
Create/edit the selectable-day predicate and the native mode-change handler.
The selected date is a Navigator route result, not an invented onChanged event.
See [DatePickerDialog](docs/DATE_PICKER_DIALOG.md) for Canvas limits and usage.

### PaginatedDataTable: source-owned rows and pagination

All constructor arguments, three native events, six State consumers, visual
header/actions/columns and an editable long-lived DataTableSource starter.
Canvas explicitly previews zero source rows; application data is tested with
Run/Debug. See [PaginatedDataTable](docs/PAGINATED_DATA_TABLE.md).

### DataTable: Material tables and native events

All six table/descriptor forms are available with complete constructor fields,
typed sources, local styles/state maps, atomic grid editing and 13 native event
rows. Selection and sort indicators can bind to State; application logic remains
user-owned. See [DataTable](docs/DATA_TABLE.md).

### Table: rows, columns and cell alignment

Layout → **Table**, **TableRow** and **TableCell** expose native table layout,
all six width and alignment families, row decorations, table borders and typed
keys/sources. The Rows and columns editor changes the rectangular grid atomically
with Undo/Redo; cell content uses ordinary child slots. Canvas renders local values
without executing project factories. See [Table](docs/TABLE.md).

### Flow: source-owned paint-time layout

Layout → **Flow** and **Flow.unwrapped** expose Delegate, Clip behavior, ordered
Children and shared Key. An editable Dart starter is inserted atomically;
typed project delegates retain native paint transforms, opacity and repaint
notifications. Canvas explicitly previews the finite starter without executing
project code. See [Flow](docs/FLOW.md).

### CustomMultiChildLayout and LayoutId: coordinated layout

Layout → **CustomMultiChildLayout** and **LayoutId** provide a typed delegate,
unique typed child IDs, an editable starter, source/history preservation and
native layout in generated Dart. Canvas uses private IDs and an explicitly
disclosed finite preview without executing project code.
See [CustomMultiChildLayout and LayoutId](docs/CUSTOM_MULTI_CHILD_LAYOUT.md).

### CustomSingleChildLayout: source-owned layout

Layout → **CustomSingleChildLayout** exposes a required typed Delegate, shared
Key and optional Child. First insertion adds an editable Dart starter class as
one undoable operation. Source controls size, constraints, positioning and relayout;
Canvas explicitly uses a finite centered 128 × 96 preview without running project code.
See [CustomSingleChildLayout](docs/CUSTOM_SINGLE_CHILD_LAYOUT.md).

### CustomPaint: source-owned painting

Basic → **CustomPaint** exposes Painter, Foreground painter, local/source Size,
both cache hints and optional Child. Typed project painters retain native repaint,
hit testing and semantics in generated Dart. Isolated Canvas does not execute
project code and clearly discloses inert-painter/Size.zero preview substitutions.
See [CustomPaint](docs/CUSTOM_PAINT.md).

### ShaderMask: gradient and custom shaders

Basic → **ShaderMask** supports all three gradient families, ordered color stops,
theme colors, directional geometry, tiling, rotation and all 29 BlendModes.
A typed ShaderCallback supports custom shader code with create/navigate/rename/
disconnect actions. Canvas renders local gradients; source callbacks use an
explicit opaque-white fallback without executing project code.
See [ShaderMask](docs/SHADER_MASK.md).

### BackdropFilter: background filtering and groups

Basic now includes **BackdropFilter**, **BackdropFilter.grouped**, and
**BackdropGroup**. The complete six ImageFilter factories and all three
ImageFilterConfig factories are supported, including layout-bounded sampling,
composition, Enabled, all BlendModes and typed shared keys. Their 52 Properties
preserve inactive drafts and source ownership. Native clipping and grouping
semantics are retained; no automatic clip or project-code execution is added.
See [BackdropFilter](docs/BACKDROP_FILTER.md).

### ImageFiltered: native image filters

Basic → **ImageFiltered** covers all six factories: blur (including bounds),
dilate, erode, full 4 x 4 matrix, typed composition, and caller-owned shader.
All 17 Properties, Enabled checkbox, optional Child and source/history are
supported. Canvas previews local filters natively and substitutes documented
identity values for project code. Shader requires Impeller in the running app.
See [ImageFiltered](docs/IMAGE_FILTERED.md).

### ColorFiltered: child pixel filters

Basic → **ColorFiltered** supports all five ColorFilter families: mode (all 29
BlendModes), a complete 4 x 5 matrix, both gamma conversions, and saturation.
All 24 property rows are editable; inactive drafts survive mode changes.
A typed non-null ColorFilter source can replace the local filter. Canvas uses
identity for project filters without executing project code. Child is optional;
native layout, input and semantics are preserved. No native Events are invented.
See [ColorFiltered](docs/COLOR_FILTERED.md).

### RawImage: decoded image painting

All 16 constructor parameters, including typed `dart:ui.Image?`, nullable
opacity animations and nine-patch `Rect?` sources. Local opacity creates a
stopped animation. Images, decoding and disposal stay in project Dart code;
Canvas explicitly previews opaque handles as null, never as asset providers.
See [RawImage](docs/RAW_IMAGE.md).

### FadeInImage: placeholder-to-image transition

All 23 general-constructor parameters, two independent asset/resize or typed
project providers, both error builders, exact native durations/curves, layout
and semantics. Network/memory/file/custom providers are source-owned; Canvas
never executes them. Named assetNetwork/memoryNetwork combinations use the
equivalent provider composition, not separate constructor aliases.
See [FadeInImage](docs/FADE_IN_IMAGE.md).

### AnimatedIcon: animated Material glyphs

All 14 Flutter animated icons, with SVG previews at 0%, 50% and 100%; all six
constructor parameters, typed project animations and native theme/direction
behavior. Canvas uses explicit safe substitutes for project code. Custom data
subclasses cannot bypass Flutter's private icon-data cast. No native Events are
invented. See [AnimatedIcon](docs/ANIMATED_ICON.md).

### AnimatedModalBarrier: animated modal color

All seven parameters with required local/typed Animation<Color?>, transparent
stopped-null creation, correct nullable semantics defaults, native On dismiss
actions and application-owned clipping notifier. Live animation is retained in
Dart; Canvas isolates it without running project callbacks. Required color
cannot be omitted. See [AnimatedModalBarrier](docs/ANIMATED_MODAL_BARRIER.md).


### ModalBarrier: modal input and accessibility barrier

All seven parameters, including nullable source Color/On dismiss/Clip details
notifier, centered Boolean editors and native Event handler actions. Generated
Dart retains Navigator.maybePop and SDK platform semantics; Canvas suppresses
dismissal, navigation and sounds, labels source fallbacks and safely explains
unbounded layout. See [ModalBarrier](docs/MODAL_BARRIER.md).

### MatrixTransition: explicit matrix animation

Required local/typed Animation<double>, fixed local matrix or application-owned
TransformCallback, physical Alignment, all FilterQuality values and optional
Child. Includes callback creation/binding/renaming/disconnection, native 2D/3D
rendering, strict source evidence and source-isolated Canvas. Local matrices are
fixed; animation-dependent computation stays in the typed Dart callback.
See [MatrixTransition](docs/MATRIX_TRANSITION.md).

### AlignTransition: explicit alignment animation

Required physical/RTL-aware local alignment or typed Animation<AlignmentGeometry>,
nullable width/height factors (including typed project sources) and a required
Child are implemented across Properties, FD save/reopen, generation and native
Canvas. Factors update immediately; alignment follows the source animation.
Canvas labels source substitutions and preserves child state while quarantining
non-finite local paint offsets. No timing controls or events are invented.
See [AlignTransition](docs/ALIGN_TRANSITION.md).

### DecoratedBoxTransition: explicit decoration animation

Required local BoxDecoration or typed Animation<Decoration>, background/foreground
painting and a required Child are implemented across Properties, FD save/reopen,
Dart generation and native Canvas. The shared editor includes decoration images,
borders, radii, shadows, gradients and theme colors. ShapeDecoration/custom
decorations are supported through project animation references. Canvas labels
these sources and uses an empty stopped preview; generated Dart stays live.
The decoration does not add padding or clip its child.
See [DecoratedBoxTransition](docs/DECORATED_BOX_TRANSITION.md).

### RelativePositionedTransition: rectangle animation with reference size

All required inputs are represented: Animation<Rect?>, reference Size and Child.
Local signed LTWH/Size fields, a stopped null-value animation and independent
typed project sources retain native physical Stack positioning, including RTL
and actual-parent-size changes. No proportional scaling is invented.
See [RelativePositionedTransition](docs/RELATIVE_POSITIONED_TRANSITION.md).

### PositionedTransition: explicit relative-rectangle animation

Required Rectangle animation and Child are supported, including all eight strict
Animation<RelativeRect> source forms and local physical left/top/right/bottom
insets. Palette insertion wraps a direct Stack child; it cannot create an invalid
empty wrapper. Local edge edits select local mode atomically. Native Stack layout,
clipping, zero-size selection and source-isolated Canvas behavior are preserved.
See [PositionedTransition](docs/POSITIONED_TRANSITION.md).

### SizeTransition: explicit size-factor animation

All five scalar arguments and optional Child are supported, including deprecated
Axis alignment, nullable physical/directional Alignment and cross-axis factor.
Signed local numbers or strict Animation<double> drive native clipped layout.
Negative factors collapse the main axis; parent constraints remain authoritative.
Canvas isolates source references while generated Dart follows live animations.
See [SizeTransition](docs/SIZE_TRANSITION.md).

### RotationTransition: explicit turns animation

All three scalar arguments and optional Child are supported: signed local turns
or strict Animation<double>, physical Alignment and nullable FilterQuality.
One turn is 360 degrees clockwise even in RTL; zero is identity, with no modulo
or shortest-path conversion. Source animation/controller lifetime stays in Dart.
Canvas previews source references at zero/center and safely withholds invalid
matrix frames without changing stored values or discarding child State.
See [RotationTransition](docs/ROTATION_TRANSITION.md).

### ScaleTransition: explicit scale animation

All three scalar arguments and optional Child are supported: signed local scale
or strict Animation<double>, physical Alignment/references and nullable FilterQuality.
Zero scale suppresses painting/hits without removing layout; negative scale flips
both axes. Constant AlwaysStoppedAnimation reports forward, so configured filters
still apply; dismissed/completed project animations disable filtering.
See [ScaleTransition](docs/SCALE_TRANSITION.md).

### SlideTransition: explicit position animation

Required position accepts local signed fractions or strict Animation<Offset>
references; optional Text direction and Transform hit tests preserve native
semantics. Unset/null direction is physical even in RTL. Generated Dart follows
live animations; isolated Canvas reports its stopped-zero source fallback.
See [SlideTransition](docs/SLIDE_TRANSITION.md).

### FadeTransition and SliverFadeTransition

Required Animation<double> opacity is editable as a local stopped number or a
strict typed source reference, with complete semantics and box/sliver slots.
Generated Dart follows live project animations; isolated Canvas never executes
them and reports its stopped opacity-1 fallback. See [Fade transitions](docs/FADE_TRANSITION.md).

### DefaultTextStyleTransition: explicit text-style animation

All five scalar constructor arguments, required Child and 31 local TextStyle
fields. Local emits a constant stopped animation; strict Animation<TextStyle>
references support project-owned controllers and live updates in generated
Dart. Canvas preserves State and child identity while keeping project code
inert with an explicit fallback. No Duration/Curve/On end is invented.
See [DefaultTextStyleTransition](docs/DEFAULT_TEXT_STYLE_TRANSITION.md).

### DefaultTextStyle: immediate defaults and native merge

Both insertable variants expose 41 writable fields and required Child: every
local TextStyle field, paragraph settings, nullable typed references and whole
or local TextHeightBehavior. Ordinary defaults replace; merge null/unset
inherits, including Max lines. Stable editors, atomic wrapping and native
Canvas are shared. The SDK fallback sentinel is intentionally non-insertable.
See [DefaultTextStyle](docs/DEFAULT_TEXT_STYLE.md).

### Theme: immediate inherited Material theme

Required ThemeData and Child, six SDK factory profiles (Material 2/3) and
strict local/imported ThemeData references. Edits apply without animation;
the child identity and native IconTheme/Cupertino/selection inheritance are
preserved. ThemeData inner fields remain source-owned, not flattened into
the Properties sheet. See [Theme](docs/THEME.md).

### AnimatedTheme: animate a complete Material theme

Four constructor properties and required Child, six SDK ThemeData factory
profiles (Material 2/3), all curves, optional 200 ms Duration, typed whole
ThemeData references and native On end. Arbitrary ThemeData remains source-owned;
its inner fields are not individually flattened into the widget's Properties.
See [AnimatedTheme](docs/ANIMATED_THEME.md).

### AnimatedSwitcher: replacing the current child

All six pinned constructor properties, optional Child, independent curves and
durations, and both editable typed builders are supported. Retained Canvas
entries have isolated instrumentation keys. The pinned SDK's default-transition
key limitation and the explicitly labeled Canvas workaround are documented in
[AnimatedSwitcher](docs/ANIMATED_SWITCHER.md).

### AnimatedCrossFade: two-child cross-fade and size animation

Both required children are explicit editable tree nodes. All 10 pinned constructor
properties, three independent curves, reversible Duration, typed layoutBuilder and
onEnd are supported. See [AnimatedCrossFade](docs/ANIMATED_CROSS_FADE.md).

### AnimatedFractionallySizedBox: animated available-space fractions

Six typed properties, optional Child, physical/directional alignment, independent
nullable or project-owned factors, all 43 curves and native On end.
See [AnimatedFractionallySizedBox](docs/ANIMATED_FRACTIONALLY_SIZED_BOX.md) for
the pinned null-tween behavior and preview-only layout/transition guards.

### AnimatedPhysicalModel: animated physical layer

Complete constructor with 11 typed rows, required Child wrapping, physical
elliptical/null/reference radii, elevation, both colors and independent animation
flags, all 43 curves and native On end. Shape and clipping change immediately.
See [AnimatedPhysicalModel](docs/ANIMATED_PHYSICAL_MODEL.md) for source contracts
and explicit Canvas-only overshoot containment.

### AnimatedDefaultTextStyle: animated inherited text style

Complete public constructor with 44 typed rows, including all local TextStyle
constructor fields, whole-style references, paragraph settings and native On end.
Create by wrapping an existing box child. Paragraph changes are immediate;
incompatible native style transitions have a visible Canvas-only fallback.
See [AnimatedDefaultTextStyle](docs/ANIMATED_DEFAULT_TEXT_STYLE.md).

### AnimatedPositioned: native Stack position and size animation

Complete ordinary, fromRect and directional constructors: 26 typed rows,
required Child, native On end, all 43 curves and exact typed references.
Drag onto an existing Stack child to wrap it. Axis edits are atomic; Canvas
preserves ParentData, RTL and moving zero-size handles.
See [AnimatedPositioned](docs/ANIMATED_POSITIONED.md) for placement and preview boundaries.

### AnimatedSize: native child-size animation

Complete public constructor: six typed rows, optional Child, native On end,
nullable reverse duration and all 43 curves. Native RTL, clipping and
constraint behavior are preserved; project references remain isolated in Canvas.
See [AnimatedSize](docs/ANIMATED_SIZE.md), including the pinned reverse-timing caveat.

### AnimatedContainer: animated geometry and decoration

Complete constructor with 15 rows, optional Child, nullable typed references,
all 43 curves and native On end. Eight SDK tweens, atomic background/clipping
edits, real-SDK proof and shared matrix containment.
See [AnimatedContainer](docs/ANIMATED_CONTAINER.md).

### AnimatedRotation: implicit paint rotation

Full signed Turns or typed double binding, physical Alignment, nullable Filter
quality, Duration, all 43 Curve presets, On end and optional Child. Native Canvas
retains full revolutions, clockwise RTL, layout, Child state and transformed hits.
Unsafe non-finite matrix frames are isolated and labeled without changing source
or discarding the Child. See [AnimatedRotation](docs/ANIMATED_ROTATION.md).

### AnimatedScale: implicit paint scaling

Full finite signed Scale or typed double binding, physical Alignment, nullable
Filter quality, Duration, all 43 Curve presets, On end and optional Child.
Native Canvas preserves layout, negative/zero transforms, child identity and
animation-only filter application. Project values stay inert and labeled.
See [AnimatedScale](docs/ANIMATED_SCALE.md).

### AnimatedSlide: implicit fractional translation

Full signed Offset or typed project reference, Duration, all 43 Curve presets,
On end and optional Child. Native Canvas preserves layout, physical offsets in
RTL, overshoot, transformed hit testing and child identity; custom values remain
inert and labeled. See [AnimatedSlide](docs/ANIMATED_SLIDE.md).

### AnimatedPadding: implicit physical and directional insets

Full nonnegative EdgeInsetsGeometry, including typed mixed project geometry,
Duration, all 43 Curve presets, On end and optional Child. Canvas preserves
native interpolation, overshoot clamping, RTL and child identity; custom values
remain inert and labeled. See [AnimatedPadding](docs/ANIMATED_PADDING.md).

### AnimatedAlign: implicit alignment and size-factor animation

Full physical/directional or typed AlignmentGeometry, independent nullable
width/height factors, Duration, Curve, On end and optional Child. Native Canvas
preserves child state; project bindings are inert and labeled. The documented
Flutter 3.44.8 null-factor tween behavior is regression-tested.
See [AnimatedAlign](docs/ANIMATED_ALIGN.md).

### AnimatedOpacity: implicit child transparency animation

All constructor arguments: Opacity, Duration, Curve, On end, Always include
semantics and optional Child, with the shared Key identity. Canvas animates
native opacity while keeping application callbacks inert and custom values labeled.
See [AnimatedOpacity](docs/ANIMATED_OPACITY.md).

### TweenAnimationBuilder: owned tween and implicit animation

Typed Tween<T>/ValueWidgetBuilder<T>, atomic generic type editing, full Duration
and Curve bindings, native On end and optional matching box/sliver Child.
Canvas uses an isolated preview tween and never executes application bindings.
See [TweenAnimationBuilder](docs/TWEEN_ANIMATION_BUILDER.md).

### ValueListenableBuilder: typed values and atomic bindings

Paired ValueListenable<T>/ValueWidgetBuilder<T>, nullable and project/typedef types,
optional Child and box/sliver placements. The type dialog edits type, nullability,
source and builder atomically. Canvas uses an inert labeled Child fallback;
application sources and callbacks are never executed there.
See [ValueListenableBuilder](docs/VALUE_LISTENABLE_BUILDER.md).

### AnimatedBuilder: application-owned animation source

Animation accepts any typed Listenable, alongside TransitionBuilder and optional
Child, in box/sliver placement projections. The real constructor receives
animation, not its read-only listenable alias. Native controller ticks,
forward/reverse, source replacement, Child reuse and owner disposal are tested.
Canvas uses an immutable source and labels custom behavior as unavailable;
it does not execute project objects or callbacks.
See [AnimatedBuilder](docs/ANIMATED_BUILDER.md).

### ListenableBuilder: notification-driven construction

Typed Listenable and TransitionBuilder bindings plus optional Child are available
in box and sliver placement projections. Generated Dart uses the real native
constructor, including listener replacement and removal without disposing the
application-owned source. Canvas shows Child with a preview-owned inert source;
project objects/getters/factories and builders are never executed there.
Child must match its projection's box/sliver protocol.
See [ListenableBuilder](docs/LISTENABLE_BUILDER.md).

### DeviceOrientationBuilder: MediaQuery-driven construction

The complete required OrientationWidgetBuilder contract is available for box
and sliver placement. Both generate the same unnamed Flutter constructor; the
sliver entry is a Designer placement projection, not an invented SDK constructor.
Native MediaQuery orientation wins over parent dimensions and permits build-time
intrinsic layout for suitable box results. Typed source references, stable editing,
Canvas, save/history and fail-closed placement share the existing pipeline.
See [DeviceOrientationBuilder](docs/DEVICE_ORIENTATION_BUILDER.md).

### OrientationBuilder: parent portrait/landscape construction

One required typed OrientationWidgetBuilder accepts Empty or a project
function/getter/factory. Native orientation comes from parent constraints, not
MediaQuery: width greater than height is landscape; square and fully unbounded
constraints are portrait. Canvas keeps custom project code inert and labeled;
source generation, strict type proof and the complete save/history pipeline
retain the actual callback. See [OrientationBuilder](docs/ORIENTATION_BUILDER.md).

### LayoutBuilder: constraint-dependent box construction

The complete required LayoutWidgetBuilder contract accepts a closed empty preset
or a strict typed project function/getter/factory, including package members.
Empty emits SizedBox.shrink() and respects parent constraints. Source proof rejects
nullable/dynamic callbacks and nullable/dynamic results. Canvas uses the native
LayoutBuilder, retains empty-node selection, and explicitly labels custom content
as unavailable because it never executes project builders. Flutter's box-result
and no-intrinsic-layout restrictions remain unchanged.
See [LayoutBuilder](docs/LAYOUT_BUILDER.md).

### FlexibleSpaceBarSettings: explicit inherited app-bar state

All six native values and a required box Child. Palette drops wrap an existing
child atomically; required opacity and extents are explicit Designer creation
values, not invented SDK defaults. Matching Java/Canvas checks enforce
minExtent <= currentExtent <= maxExtent. Native inheritance and nearest-provider
updates do not add layout constraints. Raw Sliver children are not admitted.
See [FlexibleSpaceBarSettings](docs/FLEXIBLE_SPACE_BAR_SETTINGS.md).

### FlexibleSpaceBar: collapsing titles and stretching backgrounds

All five constructor properties plus independent Title and Background slots.
Three collapse modes, all eight stretch-effect combinations (including no effects),
directional padding and typed nullable padding/list references share the native
generation and source-proof pipeline. Canvas uses inherited app-bar settings;
missing settings or project-owned values have explicit preview notices.
See [FlexibleSpaceBar](docs/FLEXIBLE_SPACE_BAR.md).

### SliverAppBar: all three native scrolling app bars

Standard, medium and large constructors each expose 131 typed property rows,
five independent slots and the async Stretch trigger event. Native pinned,
floating, snap and stretch behavior preserves SDK defaults. Whole/local styles,
Save/reopen and Undo/Redo share the existing typed pipeline.
See [SliverAppBar](docs/SLIVER_APP_BAR.md).

### SliverFloatingHeader: content-sized floating headers

Required box Child with an explicit 48 x 48 SizedBox creation starter, replaceable
through Slots. Both snap modes and the complete AnimationStyle are supported:
independent show/hide durations, all 43 curves, noAnimation, null and typed
nullable project references. Canvas uses the native scrolling/snap behavior;
project-owned values remain inert with an explicit preview warning.
See [SliverFloatingHeader](docs/SLIVER_FLOATING_HEADER.md).

### PinnedHeaderSliver: content-sized pinned headers

One optional box Child, measured by native Flutter and pinned at the viewport
start. Supports size changes, both scroll axes, reverse/RTL, Slots editing,
source history and Canvas selection/drop without a delegate or artificial height.
See [PinnedHeaderSliver](docs/PINNED_HEADER_SLIVER.md).

### SliverResizingHeader: prototype-sized pinned headers

Three optional box slots: Min extent prototype, Max extent prototype and Child.
Native Flutter measures the hidden prototypes and resizes the visible child while
scrolling. All three slots support editing, replacement and move through Slots;
Canvas pointer drops target only visible content. No delegate or scalar stand-ins
are needed. See [SliverResizingHeader](docs/SLIVER_RESIZING_HEADER.md).

### SliverPersistentHeader: pinned and floating headers

Required Delegate accepts a typed project delegate reference/getter/factory.
The first drop adds an editable concrete starter class to user-owned Dart in the
same save/undo operation. Both Pinned and Floating use the shared checkbox.
Custom delegate content, extents, snap, stretch and animation are retained in
generated Dart. Canvas never executes project code and labels its 56–112 logical-pixel
header preview. See [SliverPersistentHeader](docs/SLIVER_PERSISTENT_HEADER.md).

### SliverLayoutBuilder: constraint-dependent sliver construction

Required Builder accepts an empty-sliver preset or a typed project function,
getter, member or zero-argument factory. Generated Dart runs the native builder
at layout time. The isolated Canvas never executes project code; its zero-extent
preview has a selectable handle and an explicit custom-content warning.
The callback must return a sliver, not a box; that runtime rule cannot be proved
from Flutter's Widget return type alone. No fake child slots or Events are added.
See [SliverLayoutBuilder](docs/SLIVER_LAYOUT_BUILDER.md).

### SliverAnimatedOpacity: implicit sliver transparency animation

Scrolling now includes target Opacity, all 43 Curves presets or a typed Curve
reference, required Duration in microseconds or a typed Duration reference,
Always include semantics and an optional Sliver. On end is available in Events.
Canvas runs the native animation with safe defaults for project-owned values
and never executes project callbacks. See [SliverAnimatedOpacity](docs/SLIVER_ANIMATED_OPACITY.md).

### SliverSafeArea: physical system insets for slivers

Scrolling now includes the complete SliverSafeArea constructor: Left, Top,
Right and Bottom checkboxes, typed physical Minimum insets, and a required
Sliver. It atomically wraps an existing sliver and removes consumed system
padding for descendants, avoiding double-counted insets in nested safe areas.
Minimum remains active for disabled sides. There is no sliver
maintainBottomViewPadding parameter. See [SliverSafeArea](docs/SLIVER_SAFE_AREA.md).

### SliverVisibility: replacement and retained hidden slivers

Both SliverVisibility constructors are available, including .maintain.
The default constructor exposes six boolean properties with the SDK's four
dependency rules; .maintain fixes all five maintenance flags to true. Both
wrap an existing required Sliver and expose an optional Replacement sliver.
Hidden/inactive branches stay editable through the tree without leaking
Canvas drop geometry. Native state, animation, semantics, pointer and scroll
behavior are verified. See [SliverVisibility](docs/SLIVER_VISIBILITY.md).

### SliverOffstage: hide slivers without losing their state

Scrolling now includes the full SliverOffstage constructor: optional Offstage
checkbox and optional Sliver slot. Hidden children remain laid out and mounted,
but do not paint, receive pointer hits, expose semantics or occupy scroll space.
Hidden descendants remain editable through the widget tree; their Canvas
selection/drop geometry is suppressed until shown again. Empty slots safely
use a zero-extent adapter. See [SliverOffstage](docs/SLIVER_OFFSTAGE.md).

### SliverIgnorePointer: sliver pointer and semantics control

Scrolling includes both native fields, Ignoring and the deprecated nullable
Ignoring semantics override, plus an optional Sliver slot. Boolean values use
checkboxes; omission and explicit null remain distinct. Layout/painting/scroll
extent and keyboard focus are retained while pointer hit testing follows Flutter.
Empty slots use a zero-extent adapter, with compact Designer selection handles.
See [SliverIgnorePointer](docs/SLIVER_IGNORE_POINTER.md).

### SliverOpacity: native sliver transparency

Scrolling includes the complete constructor: stable key, required opacity in
[0, 1], optional Always include semantics checkbox and optional Sliver slot.
Creation starts opaque; zero retains layout, scroll extent and native hit testing.
Empty slots lower to a zero-extent SliverToBoxAdapter in Canvas and generated Dart,
avoiding Flutter 3.44.8's null-child RenderProxySliver assertion without adding
a model node. Editing, insertion, move, save/reopen and Undo/Redo are covered.
See [SliverOpacity](docs/SLIVER_OPACITY.md).

### SliverConstrainedCrossAxis: maximum lane width or height

Scrolling now includes the complete native constructor: stable key, required
`maxExtent` and required `sliver`. Wrap an existing sliver, then set a nonnegative
number or Infinity. The creation preset is 120 logical pixels, not an SDK default.
Native Canvas follows the smaller of this limit and available cross-axis space;
in CrossAxisGroup the constrained lane does not flex. Tree/Canvas wrapping,
Properties, generation, candidate analysis, save/reopen and Undo/Redo are covered.
See [SliverConstrainedCrossAxis](docs/SLIVER_CONSTRAINED_CROSS_AXIS.md).

### SliverCrossAxisExpanded: proportional sliver lanes

Scrolling now includes the complete native constructor: stable key, required
positive integer `flex` and required `sliver`. Drop it onto an existing direct
child of `SliverCrossAxisGroup` to wrap that child atomically, then edit Flex.
The initial 1 is a Designer preset, not an SDK default. Empty creation and invalid
ParentData placement are rejected. Properties, native Canvas, generation,
candidate analysis, save/reopen and Undo/Redo are covered.
See [SliverCrossAxisExpanded](docs/SLIVER_CROSS_AXIS_EXPANDED.md).

### SliverCrossAxisGroup: side-by-side sliver lanes

Scrolling includes the complete native constructor: generator-owned stable key
and required ordered `slivers` list. Empty/nested groups, mixed main-axis groups,
typed Slots, generation, save/reopen/history and native Canvas are supported.
Default children share the cross axis equally; scroll extent follows the longest
child. Neighboring lanes have separate hit/drop zones. There are no scalar fields
or events on the group. Unequal shares now use SliverCrossAxisExpanded;
maximum widths/heights now use the separate SliverConstrainedCrossAxis wrapper.
See [SliverCrossAxisGroup](docs/SLIVER_CROSS_AXIS_GROUP.md).

### SliverMainAxisGroup: ordered and nested sliver groups

Scrolling now includes the full native constructor: the generator-owned stable
key and required ordered `slivers` list, including an empty list and nested groups.
Slots supports insertion, movement, reordering, removal and clearing; box children
need a `SliverToBoxAdapter`. Native Canvas inherits the viewport axis, reverse
and text direction without creating another scroll view. The group has no
additional scalar properties or callbacks.
See [SliverMainAxisGroup](docs/SLIVER_MAIN_AXIS_GROUP.md).

### SliverVariedExtentList: per-item extent callbacks

Scrolling includes all three native constructors: `.list`, `.builder` and `.new`.
All 13 non-key constructor fields are editable across those variants, with five
typed callable bindings and a visual Children list only for `.list`.
The required `itemExtentBuilder` accepts a project function/getter/factory or
the explicit Designer 48 px preset. Null callback and unset are rejected;
the callback result may be null only outside the actual item range.
Project extent callbacks use an explicitly labeled natural-size approximation
in isolated Canvas; generated Dart keeps the exact reference.
See [SliverVariedExtentList](docs/SLIVER_VARIED_EXTENT_LIST.md).

### MenuAnchor: complete native anchor, menu style and explicit opener workflow

Material → **MenuAnchor** covers all 16 scalar constructor parameters and 203
local MenuStyle leaves: **219 rows**. Menu children is a required list argument
that may be empty; Child is optional. Whole MenuStyle references/null and local
style leaves are exclusive. The native panel resolves styles against an empty
WidgetState set: all nine local state buckets remain generated data, but are
not fabricated hover/press states. Sparse sizes, sides and shapes use the actual
panel MenuTheme/default fallback, not ButtonStyle defaults. A local density axis
constructs VisualDensity with the omitted peer's native default 0; resetting both
restores native inherited density. Generated source retains native root/nested
placement. Canvas keeps menus inside its isolated form overlay and explicitly
diagnoses unavailable project builder/controller/link behavior; its preview is
not a claim of exact application overlay placement.

On open, On close and On animation status changed are Events. Builder stays in
Properties with its exact `Widget Function(BuildContext, MenuController, Widget?)`
signature. **Create Menu Builder** explicitly inserts a fixed, user-owned TextButton
opener and binds it atomically; it neither creates a controller field nor replaces
Child/menu children/callbacks. Project builder references remain supported.
Canvas never executes project builders or controllers. The deprecated
anchorTapClosesMenu field is retained and emitted but is inert in Flutter 3.44.8.
Four effective boolean State consumers bring the shared inventory to
**166 properties across 55 widget types**, without an invented open-state producer.
See [MenuAnchor contract and verification status](docs/MENU_ANCHOR.md).

### MenuBar: complete horizontal menu surface

Material → **MenuBar** exposes all 206 typed rows: the required `children` list,
nullable `MenuController`, `Clip` behavior, whole/local `MenuStyle` editing and
the nine state buckets shared with the native menu theme. Its child list is a
real Slots editor, and native Flutter children render directly on Canvas.
Project-owned controller/style references are retained and diagnosed but never
executed in the isolated preview. MenuBar has no direct Events; child menu
entries own activation and shortcut behavior. See the
[MenuBar contract and verification status](docs/MENU_BAR.md).

### NavigationBar: complete Material navigation surface

Material → **NavigationBar** exposes all 15 reviewed constructor properties:
selection and activation, theme-aware colors/elevation, indicator shape,
label behavior/style/padding, animation duration and bottom view-padding policy.
`selectedIndex` is created at zero and validated against the destination list;
the optional `onDestinationSelected` callback is retained as a typed reference
or no-op stub and is never executed by Canvas. Destinations are a real list
slot (empty is allowed in the model and diagnosed in preview); each destination
is edited as an ordinary nested widget. The native Canvas mounts valid lists
with the SDK `NavigationBar`, preserves selection, and gives a deterministic
diagnostic for lists shorter than two entries. Save/reopen, history, palette DnD,
Properties and generation share the same typed contract. See the
[NavigationBar contract and verification status](docs/NAVIGATION_BAR.md).

### NavigationRail: complete Material navigation rail

Material → **NavigationRail** exposes all 20 reviewed constructor properties:
nullable selection, theme-aware appearance, extension/label policy, icon and
text themes, indicator, rail sizing, placement and scrolling. `selectedIndex` is
required but nullable and is created at zero; `leading` and `trailing` are
optional widget slots, while `destinations` is a required empty-valid list.
Canvas adapts each destination widget as an icon with a deterministic label and
normalizes only invalid SDK assertion combinations for the isolated preview.
Typed callbacks, project-owned references, generated Dart, Properties, history,
Save/reopen and palette DnD retain the exact model values. See the
[NavigationRail contract and verification status](docs/NAVIGATION_RAIL.md).

### NavigationDrawer: complete Material navigation drawer

Material → **NavigationDrawer** exposes all nine reviewed constructor properties:
theme-aware colors, elevation, indicator shape, nullable selection, destination
callback and tile padding. `children` is a required empty-valid list slot;
`header` and `footer` are optional widget slots. Canvas adapts nested children
to bounded `NavigationDrawerDestination` previews with deterministic labels,
while generated Dart preserves the original child identities and callback
references. Invalid selected indexes are diagnosed without clamping, and
application callbacks or ShapeBorder references are never executed in Canvas.
See the [NavigationDrawer contract and verification status](docs/NAVIGATION_DRAWER.md).

### Drawer: complete Material drawer surface

Material → **Drawer** exposes all eight reviewed constructor properties:
theme-aware background, shadow and surface-tint colors, non-negative elevation
and width, an optional `ShapeBorder` reference, semantic label and `Clip`
behavior. Its optional `child` is a real slot. Canvas renders the SDK Drawer
with the stored values; application-owned shape references remain retained and
diagnosed but are never executed in the isolated preview. Save/reopen, history,
palette DnD, Properties and generated Dart preserve child identity and values.
See the [Drawer contract and verification status](docs/DRAWER.md).

### BottomAppBar: complete Material bottom app bar surface

Material → **BottomAppBar** exposes all nine reviewed constructor properties:
theme-aware colors, non-negative elevation/notch margin/height, nullable
`NotchedShape`, `Clip` behavior, padding, shadow and surface tint. Its optional
`child` is a real slot. Canvas renders the native SDK widget with stored values;
application-owned notch-shape references remain retained and diagnosed but are
never executed in the isolated preview. Save/reopen, history, palette DnD,
Properties and generated Dart preserve child identity and values. See the
[BottomAppBar contract and verification status](docs/BOTTOM_APP_BAR.md).

### BottomNavigationBar: complete Material item navigation surface

Material → **BottomNavigationBar** exposes all 20 reviewed constructor
properties: typed `onTap`, non-negative `currentIndex`, elevation, fixed or
shifting type, theme-aware colors, icon and label sizing, icon/text styles,
label visibility, mouse cursor, feedback, landscape layout and legacy color
scheme behavior. The required `items` list is a real Slots editor with model
cardinality `0..10000`; Flutter requires at least two items at runtime, so the
Canvas reports a deterministic diagnostic for shorter lists. Item widgets are
adapted to native `BottomNavigationBarItem` entries with stable generated
labels, while source generation preserves their order and identities. Project
callbacks, themes, styles and cursor references remain retained and diagnosed,
never executed in the isolated Canvas. See the
[BottomNavigationBar contract and verification status](docs/BOTTOM_NAVIGATION_BAR.md).

### Material: complete Material surface wrapper

Material exposes all 12 reviewed constructor properties: `MaterialType`,
elevation, theme-aware colors, text style, mutually exclusive border radius or
`ShapeBorder`, border paint order, `Clip` behavior, animation duration and color
animation. Its optional `child` is a real slot. The Canvas mounts Flutter's
native `Material`; project-owned shape, text-style and duration references are
retained and diagnosed without executing project code. Save/reopen, history,
palette DnD, Properties and generated Dart preserve the typed values. See the
[Material contract and verification status](docs/MATERIAL.md).

### Scrollbar: complete Material scroll wrapper

Material → **Scrollbar** exposes all eight reviewed constructor properties:
nullable thumb/track visibility, thickness, `Radius`, interaction, controller,
notification predicate and orientation. Its required `child` is a real Slots
entry, so the scrollable subtree remains editable and its stable identity is
preserved through palette DnD, Save/reopen and Undo/Redo. Explicit nullable
booleans use the centered checkbox editor; `<not set>` keeps the SDK/theme
default.

Canvas mounts the native SDK `Scrollbar`. The isolated preview supplies a
scroll-position-owning viewport around the required child so arbitrary Designer
children cannot trigger Flutter's unattached-position assertion. Project-owned
controller, predicate and `Radius` references are retained, generated and
diagnosed but never executed in the isolated runner; left/right are vertical
orientations and top/bottom are horizontal. See the
[Scrollbar contract and verification status](docs/SCROLLBAR.md).

### ListWheelScrollView: complete native wheel scrolling slice

Scrolling → **ListWheelScrollView** exposes all 18 reviewed constructor fields,
including bounded wheel geometry, required `itemExtent`, closed physics and
change-reporting presets, clipping/hit testing, restoration and a real ordered
`children` Slots list. The selection callback is a typed Events reference;
controller, behavior and callback bindings are retained and diagnosed rather
than executed by the isolated Canvas. Save/reopen, history, palette DnD and
generated Dart preserve exact values and child identity. See the
[ListWheelScrollView contract and verification status](docs/LIST_WHEEL_SCROLL_VIEW.md).

### CustomScrollView and SliverToBoxAdapter: complete sliver scrolling slice

Scrolling → **CustomScrollView** exposes the reviewed Flutter 3.44.8 scrolling,
semantics, restoration, clipping and hit-testing fields, plus a typed `slivers`
list. **SliverToBoxAdapter** is the first accepted sliver child and provides an
optional `child` slot, so ordinary Designer widgets can participate in the
viewport without violating Flutter's RenderSliver contract. The model rejects
ordinary widgets in `slivers`, while Palette/tree/Canvas DnD, Properties,
Save/reopen, history and generated Dart preserve sliver identity and order.
Canvas mounts the native `CustomScrollView` and uses the real
`SliverToBoxAdapter` inside that viewport. Slivers are rejected as document
roots and inside ordinary box slots; empty slivers have external selection
handles and drop geometry without changing generated application layout. See the
[CustomScrollView contract and verification status](docs/CUSTOM_SCROLL_VIEW.md).

### Static SliverList and SliverGrid

Scrolling now includes **SliverList.list**, **SliverGrid.count** and
**SliverGrid.extent**. All properties of these static constructors, ordered
children, typed placement, generation, persistence and native Canvas are
implemented. Six further entries cover SliverList.builder / separated / new
and SliverGrid.builder / list / new, with typed project builders/delegates,
all constructor fields and explicit empty/default presets. Isolated Canvas
labels dynamic-content and custom-grid preview limits; generated Dart keeps
the exact project references. See the [static sliver contract](docs/SLIVER_CHILDREN.md)
and [builder/delegate contract](docs/SLIVER_DYNAMIC.md).

### SliverPadding: physical and directional sliver insets

Scrolling → **SliverPadding** includes the complete reviewed constructor:
required non-negative `EdgeInsetsGeometry padding` and one optional `sliver`.
The shared editor supports physical/directional values and typed project
references, including getters, members and zero-argument factories. Sliver-only
placement, nested padding, RTL/reversed axes, stable Properties, Save/reopen,
generated Dart and Canvas share the same contract. Project geometry is not
executed in isolated Canvas: a visible notice marks its zero-inset approximation
while retaining the nested sliver. See the [SliverPadding contract](docs/SLIVER_PADDING.md).

### SliverPrototypeExtentList: measurement-only prototype and native lazy children

Scrolling → **SliverPrototypeExtentList.list**, **SliverPrototypeExtentList.builder**
and **SliverPrototypeExtentList.new** expose all three native constructors,
ten writable fields and a separate Prototype item slot in every variant.
The prototype is laid out but not painted or hit-tested. Its main-axis size
determines every visible child's extent. Edit it through the tree/Slots; an
empty slot uses an explicit Designer SizedBox(48 x 48) preset, not an SDK default.
Visual list children and typed builder/delegate content remain independent.
Native Canvas respects both axes/reverse/RTL without inventing prototype hit
handles. Project callbacks/delegates retain the explicit isolated-preview limit.
See [SliverPrototypeExtentList contract](docs/SLIVER_PROTOTYPE_EXTENT_LIST.md).

### SliverFixedExtentList: fixed-size sliver children, builders and delegates

Scrolling → **SliverFixedExtentList.list**, **SliverFixedExtentList.builder** and
**SliverFixedExtentList.new** cover all three native constructors and all 14
writable fields. Every child gets the required non-negative main-axis itemExtent;
the initial 48 logical pixels is a Designer preset, and zero is valid. Visual
ordered children, nullable-returning lazy builders with optional itemCount,
key-to-index lookup and typed list/builder/custom delegates are supported.
Canvas uses the native render sliver in both axes, reverse and RTL, with an
explicit limitation message when project code cannot be previewed.
See [SliverFixedExtentList contract](docs/SLIVER_FIXED_EXTENT_LIST.md).

### SliverFillViewport: viewport-sized visual children and project delegates

Scrolling → **SliverFillViewport** exposes viewport fraction, end padding and
implicit accessibility scrolling, with a visual box-children list and all five
SliverChildListDelegate configuration fields. **SliverFillViewport.delegate**
accepts typed project list/builder/custom delegates, getters and factories.
Both generate the native unnamed Flutter constructor; the delegate suffix is a
Designer palette distinction. Canvas never executes project code and labels
delegate-content/semantic-callback preview limitations explicitly.
See [SliverFillViewport contract](docs/SLIVER_FILL_VIEWPORT.md).

### SliverFillRemaining: native remaining-space and overscroll behavior

Scrolling → **SliverFillRemaining** covers both boolean constructor parameters
(`hasScrollBody`, `fillOverscroll`) and the optional box `child` slot.
Unset preserves Flutter's true/false defaults; explicit values use the shared
centered checkbox editor. All three native layout branches, sliver-only parent
placement, box-only child admission, stable Properties, persistence, Undo/Redo,
Dart generation and Canvas are reviewed. See the
[SliverFillRemaining contract](docs/SLIVER_FILL_REMAINING.md).

### PreferredSize: complete preferred-size wrapper

Layout → **PreferredSize** exposes the required typed `Size preferredSize`
and required single `child` slot from Flutter 3.44.8. Palette nodes start at
`Size(100, 56)`; the Size editor accepts only finite non-negative dimensions
and keeps the value atomic. The widget advertises its size to parents such as
`AppBar` and `Scaffold` without constraining the child. It is a required-child
wrapper source for Palette/tree/Canvas DnD, while the exact child slot remains
available in Properties. Native and exact-Web Canvas, generated Dart,
Save/reopen, history and reviewed light/dark SVG icons share the same contract.
See the [PreferredSize contract and verification status](docs/PREFERRED_SIZE.md).

### Builder: complete callback-driven subtree wrapper

Layout → **Builder** exposes the required typed `WidgetBuilder builder` callback
from Flutter 3.44.8. Palette creation supplies a reviewed `noop` callback so a
new node is always valid; the callback is edited as a typed project-owned symbol
in Properties and cannot be reset to `<not set>`. Builder has no child slots.
Generated Dart preserves the callback identifier, while the isolated native and
exact-Web Canvas use a bounded 48 × 36 placeholder and never execute project
callback code. The same contract is covered by palette DnD, Save/reopen,
history, and reviewed SVG icons.

### MenuItemButton: complete menu item, style and shortcut hints

Material → **MenuItemButton** exposes all 13 scalar SDK arguments, a reviewed
Enabled activation selector, eight local shortcut fields and 498 shared local
ButtonStyle leaves: **547 rows**. Child, Leading icon and Trailing icon are all
optional; there is no invented required label or Long press event. Styles use
native MenuButtonTheme and MenuItemButton defaults, including Clip.none.

Shortcuts support SingleActivator with all 432 reviewed non-modifier keyboard
keys, CharacterActivator with the exact Unicode string, and a strict whole
MenuSerializableShortcut reference/factory or null. They display hints only:
the Designer does not register global shortcuts or execute project logic.
CharacterActivator itself accepts any string, but pinned MenuItemButton hint
serialization requires one UTF-16 code unit. The model/source preserve other
strings; Canvas diagnoses and omits only their unrenderable hint rather than
claiming those values are safe to mount in the real application.
Whole/local shortcut and style branches are exclusive; explicit compound edits
preserve compatible modifiers and Undo history without raw Dart expressions.

On pressed, On hover and On focus change are the three Events; the two style
layer builders stay in Properties. Four reviewed State consumers cover enabled,
autofocus, requestFocusOnHover and closeOnActivate, bringing that milestone's State coverage to
**162 properties across 54 widget types**. Enabled lowers to conditional
onPressed, not a nonexistent SDK argument, and never overwrites stored handlers.
There is no MenuItemButton State producer. Project node/controller lifecycle
and callback bodies remain user-owned. See the
[MenuItemButton contract and verification status](docs/MENU_ITEM_BUTTON.md).

### TooltipTheme: complete inherited data with explicit whole/local ownership

Material → **TooltipTheme** exposes all 15 pinned `TooltipThemeData` fields,
31 shared local TextStyle leaves and a strict whole Data reference: **47 rows**
with a required Child and atomic wrapping. Whole Data supports project references,
getters, members and zero-argument factories; it is non-null and exclusive with
all local fields, including explicit null and State bindings. Conflicting edits
are rejected without erasing values. Local mode constructs a fresh data object;
with every field omitted it emits `const TooltipThemeData()`.

The nearest TooltipTheme replaces the whole outer theme rather than merging
individual fields; explicit descendant Tooltip values take precedence. Height
and Constraints retain the SDK's non-null exclusion. Styling reuses structured
BoxDecoration and strict Decoration/TextStyle references. Three durations preserve
signed portable microseconds, typed Duration references and explicit null. Fresh
construction preserves `exitDuration`, which the pinned SDK's `copyWith` drops;
no theme-copy or interpolation operation is introduced.

Prefer below, Exclude from semantics and Enable feedback support nullable boolean
State bindings inside the generated data object. They add no Events or producer;
the reviewed State inventory at that milestone was **158 properties across 53 widget types**. Project
theme objects remain user-owned and are not executed in Canvas. See the
[TooltipTheme contract and verification status](docs/TOOLTIP_THEME.md).

### TooltipVisibility: inherited tooltip policy with a required Child

Material → **TooltipVisibility** exposes the complete pinned constructor: one
required boolean Visible and one required Child, created by atomic wrapping.
Designer creates an explicit `visible: true`; Flutter itself requires the value.
The nearest scope wins, so an inner true scope can override an outer false scope.
Visible supports reviewed boolean State bindings (Direct, Not and Equals), without
adding a callback or a tooltip-visibility State producer. At its milestone the
shared consumer inventory reached **155 properties across 52 widget types**.

The existing Designer placement policy keeps Expanded/Flexible/Spacer directly
under Row/Column; put TooltipVisibility inside Expanded/Flexible instead. This
is a shared model limitation, not a stricter TooltipVisibility SDK requirement.

False disables descendant Material Tooltip visuals for hover, tap, long-press
and programmatic display without hiding their anchors. Pinned Flutter removes
the RawTooltip subtree and any open overlay: child semantics remain available,
but the Tooltip's own message annotation and arbitrary unkeyed child runtime
state or focus retention must not be assumed. See the
[TooltipVisibility contract and verification status](docs/TOOLTIP_VISIBILITY.md).

### Tooltip: plain/rich content, complete styling and exact trigger semantics

Material → **Tooltip** covers all 22 non-slot SDK fields in **53 typed rows**,
with an optional Child and an explicit Wrap action. Exactly one of Message and
Rich message is non-null. Plain creation uses `Tooltip`; content-mode changes
are atomic. Rich message accepts strict InlineSpan references/factories: complete
TextSpan, WidgetSpan and custom span trees remain user-owned Dart, without a local
span-tree editor. Decoration supports the shared structured BoxDecoration and
strict Decoration references, including ShapeDecoration/custom implementations.
TextStyle supports a whole reference or all 31 shared local style leaves.

Three durations preserve exact signed integer microseconds, typed Duration
references and explicit null. `onTriggered()` is the only native Event;
`positionDelegate` is an ordinary typed Properties delegate returning Offset,
not a Widget builder. In pinned Flutter 3.44.8, tap/long-press invoke the Event,
but hover and programmatic visibility do not. Five boolean State consumers are
supported, with no invented visibility producer. Canvas does not execute project
spans/delegates; unavailable content and approximations are explicitly labeled.
See the [Tooltip contract and verification status](docs/TOOLTIP.md).

### ExpansionTile: required Title, expandable Children and full animation styles

Material → **ExpansionTile** covers all 28 non-slot SDK fields in **76 typed rows**.
Title is required; Leading, Subtitle, Trailing and the Children list are optional.
Palette creation wraps the selected widget into **Title**, not Children, as one
atomic operation. The two independent shapes expose all ten reviewed constructors;
AnimationStyle supports whole references, noAnimation and all four local fields,
including exact signed microsecond durations and all 43 Curves presets.

`onExpansionChanged(bool isExpanded)` is a native Event. Six boolean State
consumers are supported, but `initiallyExpanded` is an initialization seed, not a
controlled value or State consumer. Runtime expansion belongs to the SDK or a
project-owned ExpansibleController. Flutter 3.44.8 ignores AnimationStyle's
reverseDuration here; it remains stored and generated, without an invented effect.
Custom references and unsafe mounted animation/layout values are explicitly
approximated in Canvas without executing project code or rewriting generated Dart.
See the [ExpansionTile contract and verification status](docs/EXPANSION_TILE.md).

### RadioListTile: typed generic selection, Standard/Adaptive and complete tile styles

Material → **RadioListTile** covers 37 non-slot SDK leaves plus three Designer
generic/constructor selectors, expanded to **153 typed rows**, with independent
`title`, `subtitle` and `secondary` slots. Six builtin types and simple project
class/enum/typedef references retain exact `T` / `T?` proof; values are never
coerced. Both native events, legacy nullable groupValue State and eight reviewed
boolean consumers use the shared source/history workflow.

The real matching RadioGroup is the modern owner; its non-null selection wins
over legacy groupValue, and activation calls the group before an optional
legacy callback. Selected appearance remains independent. Standard retains
inactive useCupertinoCheckmarkStyle values/bindings. Explicit enabled true keeps
the SDK runtime requirement for a callback or matching RadioGroup, while Three
Line requires Subtitle. There is no invented groupRegistry or focusColor field.
All shape, color, radio side/radius, cursor and density compounds retain their
original model paths and strict references. Canvas never executes project code.
See the [RadioListTile contract and measured verification](docs/RADIO_LIST_TILE.md).

### SwitchListTile: both constructors, complete compound styles and controlled State

Material → **SwitchListTile** covers Standard and Adaptive, all 41 non-slot
SDK leaves plus the Designer constructor selector, expanded to **236 property
rows**, and optional `title`, `subtitle`, `secondary` slots. Creation stores
`value: false`, a no-op `onChanged`, and Standard. Explicit null onChanged
disables activation; there is no invented Enabled or Tristate property.

All four events have strict typed callback references and user-owned handler
source. The bool State producer and six reviewed dependent boolean properties
retain literal previews and exact history. Adaptive-only applyCupertinoTheme
values and bindings remain stored, inactive and ungenerated in Standard.
Selected is independent of Value, and explicit Three Line requires Subtitle.

Whole and local colors, state maps, all 13 Icon leaves per thumb state, shape,
cursor and density use the shared typed editors. Image providers remain the
reviewed declared-asset / ExactAsset / ResizeImage family, not arbitrary
Network/File/project providers. The isolated Canvas never executes project
references or callbacks; custom-reference approximations remain explicit.
See the [full SwitchListTile contract and verification status](docs/SWITCH_LIST_TILE.md).

### ListTile: complete rows, styles and four independent slots

Material → **ListTile** covers all 37 non-key constructor arguments: four optional
slots (`leading`, `title`, `subtitle`, `trailing`) and 33 scalar arguments expanded
to 176 typed property rows. Creation keeps every slot empty and preserves SDK
defaults without fabricating text or callbacks. Explicit Three Line requires a
Subtitle; removing that subtitle is blocked until Three Line is reset or disabled.

The three TextStyle families each support a whole typed reference or 31 local
fields; Shape supports a whole reference or the reviewed local shape family.
Colors and content padding can be edited locally or supplied by strict typed
project references. Local icon/text state colors and state cursors require an
explicit Default entry. Switching whole/local modes is one atomic history step.
Optional callbacks preserve unset, explicit null and No-op, including when
Enabled is false. Isolated Canvas never executes project references or callbacks.

ListTile uses the real SDK's theme, state-color, text-style and Material ink
behavior. Unsafe resolved geometry is diagnosed without rewriting stored values;
an intervening painted background retains the SDK's hidden-ink warning rather
than receiving an invented Material wrapper. See the
[official constructor](https://api.flutter.dev/flutter/material/ListTile/ListTile.html)
and [ADR-116](docs/DECISIONS.md#adr-116--listtile-complete-constructor-and-contextual-layout).

### CheckboxListTile: standard and adaptive rows with three slots

Material → **CheckboxListTile** covers both `CheckboxListTile.new` and
`CheckboxListTile.adaptive`, all 38 direct constructor arguments and their
closed typed projections as 154 property rows. `title`, `subtitle` and
`secondary` are optional single any-widget slots; sparse creation preserves
Flutter defaults. Explicit `value: null` requires `tristate: true`, and an
explicit `isThreeLine: true` requires `subtitle`, matching the SDK assertions.

The Canvas constructs the actual standard or adaptive SDK widget and preserves
the platform-specific checkbox branch. State-aware colors, cursors, shapes,
side, density and callback presence use the same typed editors and validation
as the model; project callbacks and references are never executed in the
isolated preview. See the
[official constructor](https://api.flutter.dev/flutter/material/CheckboxListTile/CheckboxListTile.html)
and [ADR-117](docs/DECISIONS.md#adr-117--checkboxlisttile-complete-constructor-and-adaptive-preview).

### RadioGroup: shared typed selection

Material → **RadioGroup** wraps an existing widget or subtree; it never creates
an empty required child. Its single const constructor is completely covered:
Group Value, required On Changed and required Child, with Value Type and Nullable
Value Type as two additional Designer controls (four property rows plus the slot).
Creation stores String and No-op without inventing a selected value.

Use matching `Radio<T>` descendants: only the nearest group with the exact same
type, including nullability, supplies their shared selection. The type dialog
changes its three dependent fields atomically without rewriting descendants or
discarding callbacks. Explicit null, unset and false remain distinct in storage.
Project class/enum/typedef references, nullable-selection proofs and strict
non-null callback proofs use the same bounded source-analysis contract as Radio.

Canvas uses actual typed SDK groups for built-in literal values. Project code
is never executed; unresolved project identity and invalid runtime selection
receive explicit preview diagnostics. See the
[official constructor](https://api.flutter.dev/flutter/widgets/RadioGroup/RadioGroup.html)
and [ADR-115](docs/DECISIONS.md#adr-115--radiogroup-typed-shared-selection-and-runtime-scope).

### Radio: both constructors and explicit group-value types

Material → **Radio** adds Standard or Adaptive Radio with 107 typed rows covering
all 20/21 non-key SDK parameters, explicit generic type selection and local
state-color, radius and border alternatives. Creation uses `String`, Value
`option` and an explicitly stored No-op callback; removing that callback really
omits it. Enabled is the actual nullable SDK parameter, not a Designer switch
that silently removes callbacks.

Value type supports String, int, double, num, bool, Object and a named project
class, enum or typedef. Nullable types and nullable group-value references remain
distinct. The type dialog edits type, nullability, value and group value together
as one Undo step; it never silently discards callback or registry references.
Complex generic types can be named through a project typedef, without a raw Dart
expression field. Built-in core names preserve the user's import scope and must
resolve to their exact SDK declarations; a hidden/shadowed name is rejected,
and a project typedef can supply an explicit scoped type instead.

Generated Dart preserves the selected `Radio<T>` and strictly checks nullable
values, callbacks and registry consumption. Canvas renders actual SDK radios for
built-in literal types. Custom types, unresolved values or explicit project
registries receive a concrete preview-unavailable message because Canvas cannot
execute project code to determine their equality or selection. Styling and
callback isolation remain explicit; the source/model is retained unchanged.
See the [official Radio API](https://api.flutter.dev/flutter/material/Radio-class.html)
and [ADR-114](docs/DECISIONS.md#adr-114--radio-generic-identity-nullable-values-and-registry-consumption).

### RangeSlider: both range endpoints and complete constructor coverage

Material → **RangeSlider** inserts a leaf with Start 0, End 1 and Enabled true.
All 15 non-key SDK arguments are covered by 37 typed rows: both values, bounds,
divisions, labels, callbacks, colors, padding, nullable year2023 and nine local
states each for overlay color and mouse cursor. The SDK has one non-const
constructor, not an adaptive variant.

Start must not exceed End; both must fit Min/Max. Invalid edits are rejected
without clamping. Labels accept local text (an omitted peer becomes empty),
explicit null, or a strictly analyzed RangeLabels reference/factory. Whole/local
label, overlay and cursor changes are atomic and retain native Undo/Redo.
State entries preserve omitted versus explicit-null fallback.

Save, reopen and continue editing the same stable Properties cells. Canvas uses
the actual SDK RangeSlider with controlled values; gestures select the node
without rewriting the range. Project objects and callbacks are never executed,
and unavailable preview branches are explicitly diagnosed.
See the [official RangeSlider API](https://api.flutter.dev/flutter/material/RangeSlider/RangeSlider.html)
and [ADR-113](docs/DECISIONS.md#adr-113--rangeslider-paired-values-labels-and-stateful-cursor).

### Slider: both constructors and all SDK parameters

Material → **Slider** inserts a leaf with Value 0 and Enabled true. The 33 typed
rows cover all 22 non-key standard constructor parameters, the adaptive branch,
activation policy and nine optional overlay-color states. Value and secondary
track share validated Min/Max bounds; divisions can be omitted, explicitly null
or a positive portable integer. Invalid edits are rejected without clamping peers.

Both constructors support strict ValueChanged<double> callbacks, the semantic
formatter, focus/cursor references, all four interaction modes, six value-indicator
modes and the deprecated nullable year2023 option. Standard-only padding selects
the standard constructor; choosing adaptive clears padding in one Undo step.
Other properties ignored by the Apple SDK branch remain stored.

Canvas uses the actual Material or Cupertino SDK branch and never runs project
callbacks or mutates Value from a gesture. Constructor-legal but unsafe preview
combinations retain their exact model/source and receive a concrete diagnostic.
See the [official Slider API](https://api.flutter.dev/flutter/material/Slider-class.html)
and [ADR-112](docs/DECISIONS.md#adr-112--slider-ranges-interaction-and-adaptive-rendering).

### Switch: both constructors, images and complete state icons

Material → **Switch** inserts a leaf with Value false and Enabled true. Standard
and Adaptive expose all 27/28 non-key SDK parameters through 201 typed rows:
30 direct/policy fields, 36 state colors, nine state outline widths and 126 local
state-Icon fields. Key continues to use stable widget identity.

Thumb, track, outline and overlay colors, outline widths and thumb icons each
support a strict whole-project state-property reference or local state entries.
Omitted entries continue local resolution; explicit null delegates to the SDK.
An Icon mode with no glyph creates Icon(null), which differs from a null resolver.
All thirteen Icon constructor fields are retained, including fields the Switch
painter does not use. The glyph editor distinguishes omission from explicit None.

Both image providers support declared project/package assets, exact scale and
ResizeImage. Image-error handlers require their matching image; resetting a
provider clears only its handler in the same Undo step. Apply Cupertino theme
selects Adaptive automatically; returning to Standard clears that adaptive-only
argument. Other stored styling survives constructor changes.

The real SDK supplies controlled value, drag/focus behavior, Material 2/3 and
adaptive Apple styling, including SwitchTheme adaptations. Canvas never executes
project callbacks or object factories. Save/reopen, continued edits, stable
Properties, native Undo/Redo and rejection rollback are part of the same slice.
See the [official Switch API](https://api.flutter.dev/flutter/material/Switch-class.html)
and [ADR-111](docs/DECISIONS.md#adr-111--switch-state-icons-images-and-adaptive-styling).

### Checkbox: standard, adaptive and full state styling

Material → **Checkbox** inserts an independent leaf with Value false and Enabled
true. Both Standard and Adaptive expose all 19 SDK constructor parameters through
106 typed rows, including density axes, ten outlined shape families, fill/overlay
state colors and plain or stateful sides. Key continues to use stable widget identity.

Value is required: false, true or explicit null (mixed). Setting null also enables
Tristate; disabling or resetting Tristate while mixed sets Value false in the same
undoable edit. Concrete Booleans stay centered checkboxes. Whole project references
and local color/shape/side fields switch atomically, without recreating Properties.

Local state maps distinguish omission from explicit null and preserve state priority.
Plain BorderSide keeps the SDK's unselected-only behavior; Stateful side also resolves
selected states. Adaptive uses the real SDK Cupertino branch where applicable and
retains ignored Material-only fields. Project callbacks/objects are proved for Dart
generation but never executed by the isolated Canvas. The checkbox value is controlled
by its stored model; preview interaction does not silently change it.

Pinned Flutter 3.44.8 cannot paint LinearBorder or directional corner radii in
Checkbox: its painters omit TextDirection. Canvas reports this specific SDK
limitation and previews the SDK's default shape; saved fields and generated Dart
remain exact. Safe physical-corner shapes render normally.

See the [official Checkbox API](https://api.flutter.dev/flutter/material/Checkbox-class.html)
and [ADR-110](docs/DECISIONS.md#adr-110--checkbox-controlled-nullable-value-and-state-properties).

### IconButton: all four constructors and complete styling

Material → **IconButton** wraps an existing widget in its required **Icon** slot;
it does not invent a glyph. **Constructor** selects standard, filled, filledTonal
or outlined. **Selected icon** is optional; **Selected** preserves unset/null,
false and true without discarding either icon. All 524 typed rows are present,
including direct colors/layout/focus/callbacks, all 498 local ButtonStyle leaves
and a strict whole-style project-reference alternative.

Actual SDK constructors supply Material 2/3, IconTheme and IconButtonTheme behavior.
Partial composite style fields preserve the unedited direct size/density axis;
other fields retain normal SDK precedence. Material 2 ignores the documented
M3-only styling/selection arguments. Project Dart is never executed by Canvas.
Save/reopen, continued edits, native Undo/Redo and required-slot guards belong to
the same slice. Only catalog metadata expands to 1024 rows; stored properties,
validation and atomic patches remain at 512, with dense legal IconButtons at 505.
See the [official IconButton API](https://api.flutter.dev/flutter/material/IconButton-class.html)
and [ADR-109](docs/DECISIONS.md#adr-109--iconbutton-required-icon-and-full-state-style).

### FloatingActionButton: all four constructors

Material → **FloatingActionButton** creates an empty standard button. **Constructor**
selects [standard, small, large or extended](https://api.flutter.dev/flutter/material/FloatingActionButton-class.html).
Standard/small/large allow an empty Child; extended uses Child as its required
Label and adds an optional Icon. A constructor change never discards a populated
Icon or required Label; incompatible transitions explain what must be changed.

All 78 typed rows are available: the direct constructor controls, five colors,
five elevations, focus/cursor/callback references, tooltip and behavior,
extended spacing/padding, 31 TextStyle leaves, ten built-in ShapeBorder families
and a strict custom ShapeBorder reference. Shape and TextStyle alternatives
change atomically and preserve unrelated fields and native Undo/Redo history.
Boolean fields retain centered checkboxes; optional fields retain `<not set>`.

**Hero tag** distinguishes the SDK default (unset), explicit null (no Hero),
string/integer/double/Boolean literals, and analyzed non-null Object references.
Custom objects generate exactly; `dynamic` and nullable references fail the
strict type check. The isolated Canvas cannot evaluate application objects, so
only that unresolved Hero wrapper is disabled with a diagnostic; the button
and its children still render. Known duplicate tags are reported, not rewritten.
Flutter requires unique Hero tags when multiple buttons share a route.

Canvas uses the actual four SDK constructors and component theme, with retained
child state, M2/M3 defaults, RTL, empty children and collapsed extended mode.
It never invokes application callbacks or adopts application focus nodes.
Save/reopen, further editing, DnD, slot mutation and history remain part of the
slice. Source formats and existing resource limits are unchanged.

### FilledButton: all four constructors and complete filled/tonal styles

Material → **FilledButton** creates a standard button, including in an empty
destination. **Constructor** selects [standard, icon, tonal or tonalIcon](https://api.flutter.dev/flutter/material/FilledButton-class.html)
from the pinned Flutter 3.44.8 API. Standard and tonal accept an empty Child and
generate the required `child: null`. Icon and tonalIcon require a Child, which
becomes their Label; their separate Icon is optional. Add a Child before selecting
an icon constructor. Clear/move/replace checks protect that label across the tree
and every slot editor. Switching icon ↔ tonalIcon preserves both subtrees and icon
alignment; move or clear an occupied Icon before returning to standard or tonal.

The 510 typed fields comprise 11 direct controls, nine 54-leaf state/default
style buckets, 12 common style fields and a complete type-checked project
`ButtonStyle`. All WidgetStates, text styles, paint/color, shapes/outlines, sizes,
padding, mouse cursors, animation, splash and both layer builders are covered.
Whole and local styles are exclusive; their transitions are atomic and undoable.
Callbacks, FocusNode and WidgetStatesController reuse strict project-reference
validation. Disabled activation handlers remain stored without emitted calls,
unused imports or symbol-proof obligations.

All four constructors omit Clip behavior as `Clip.none`; explicit null is distinct
and enables the SDK's automatic clipping with layer builders. None exposes
`isSemanticButton`. Standard and tonal are const-capable; the icon constructors
are not. Generated local styles and Canvas use `FilledButtonTheme` and the actual
filled or tonal defaults, with constructor/theme/local icon-alignment precedence
and RTL layout. Canvas preserves child state across all four modes and isolates
application callbacks/builders/controllers; a whole project style has an explicitly
approximate SDK-default preview.

The slice includes stable Properties, DnD, Save/reopen/further editing, native
Undo/Redo and four SVGs. It does not alter the older button contracts, formats
13/14/18 or existing 512-property/2048-probe/45-second limits.

### OutlinedButton: standard and icon constructors, complete outlined styles

Material → **OutlinedButton** wraps an existing child. **Constructor** selects the
[standard](https://api.flutter.dev/flutter/material/OutlinedButton/OutlinedButton.html)
or [icon](https://api.flutter.dev/flutter/material/OutlinedButton/OutlinedButton.icon.html)
constructor from Flutter 3.44.8. The stable Child becomes the label in icon mode;
the optional Icon must be cleared or moved before returning to standard.

Its 510 typed fields cover 11 direct controls, 486 state-style leaves, 12 common
style fields and a complete type-checked project `ButtonStyle`. Every WidgetState
plus default is editable. Whole and local styles are exclusive; transitions are
atomic and undoable. Callbacks, focus/controllers and both layer builders reuse
strict project-reference validation. Inactive activation callbacks remain stored
without emitted calls, unused imports or symbol-proof obligations.

Both constructors omit Clip behavior as null; explicit null preserves the SDK's
automatic clipping when layer builders are present. Neither constructor accepts
`isSemanticButton`. Standard is const-capable, while `.icon` is not. The generated
style and actual Canvas resolve against `OutlinedButtonTheme` and OutlinedButton
defaults, not TextButton's. The default outline overrides the shape's own side:
edit both the shape and outline to control both. Icon placement follows constructor
alignment, then theme, local style and finally start, respecting RTL.

The isolated Canvas retains the child and local interaction across constructor,
icon and style changes. Project callbacks/builders/controllers are not executed;
whole project styles have an explicitly approximate SDK-default preview. The slice
includes stable Properties, DnD, save/reopen/further editing, native Undo/Redo and
four SVGs. TextButton/ElevatedButton contracts and formats 13/14/18 are unchanged.

### TextButton: standard and icon constructors, complete style surface

Material → **TextButton** wraps an existing child. **Constructor** selects the
[standard](https://api.flutter.dev/flutter/material/TextButton/TextButton.html)
or [icon](https://api.flutter.dev/flutter/material/TextButton/TextButton.icon.html)
constructor from Flutter 3.44.8. Child remains required; in icon mode it becomes
the SDK label, with an optional separate Icon slot. Switching back to standard
requires moving or clearing an occupied Icon slot first, so no widget is lost.

Its 511 typed Properties comprise 12 direct controls, 486 state-style leaves,
12 common style fields and a whole `ButtonStyle` reference. Local style editing
covers the default and all eight WidgetStates, resolving disabled → error →
dragged → pressed → selected → scrolledUnder → hovered → focused → default.
Type-checked project `ButtonStyle` references support custom combined-state maps,
shapes and text styles beyond the local projection. Whole and local styles are
mutually exclusive; switching between them is one undoable edit.

Press/long-press, hover/focus callbacks, FocusNode, WidgetStatesController and both
ButtonLayerBuilder fields accept type-checked current/imported references and
zero-argument factories. Enabled without activation handlers emits a no-op press;
long-press-only keeps `onPressed: null`; disabling retains stored references but
emits null activation handlers. Enabled and Constructor are required controls.
Standard-only semantic role and Clip behavior distinguish omission, explicit null
and concrete values. Explicit Boolean values remain centered checkboxes.

Changing an inactive callback remains undoable even when generated Dart is unchanged.
Saving such pending metadata writes only `.fd` when Source is clean; mixed manual
Source edits retain their native Undo/Redo order. Failed or rolled-back metadata
saves retain the pending edit for retry.

The isolated Canvas uses the actual SDK constructors, styles, theme defaults and
local button states. It never runs project callbacks/builders or shares application
focus/controllers. Diagnostics identify those limits; a project-defined whole style
is explicitly an approximate SDK-default preview, not a faithful rendering of that
style. Child state survives constructor, icon and diagnostic changes. The slice
includes stable editors, save/reopen/further editing, Undo/Redo, DnD and four SVGs.
Formats remain `.fd` 13, Catalog API 14 and Canvas model 18.

### RefreshIndicator: pull-to-refresh with all three constructors

Material → **RefreshIndicator** wraps an existing child, normally a vertical
ListView. Its 13 Properties cover all 12 SDK scalar fields across the pinned
Flutter 3.44.8 [material](https://api.flutter.dev/flutter/material/RefreshIndicator/RefreshIndicator.html),
[adaptive](https://api.flutter.dev/flutter/material/RefreshIndicator/RefreshIndicator.adaptive.html)
and [noSpinner](https://api.flutter.dev/flutter/material/RefreshIndicator/RefreshIndicator.noSpinner.html)
constructors, plus the required **Constructor** selector. The required Child slot
supports wrapping/replacement, not removal or an incomplete empty wrapper.

**On refresh** accepts a type-checked project `RefreshCallback`, including imported
functions and factories. Unset emits `onRefresh: () async {}` so a new wrapper is
valid before a real handler is configured; it does not fetch application data.
**Notification predicate** offers default/depthZero/all presets or a type-checked
`ScrollNotificationPredicate`. **On status change** accepts
`ValueChanged<RefreshIndicatorStatus?>` and switches to noSpinner. Switching to
noSpinner clears its five unsupported spinner fields; setting one of those fields
switches back to material and clears On status change. Each change is atomic and
undoable. Required Constructor cannot be unset; all other explicit values can reset.

Displacement, signed Edge offset and Stroke width, both colors, Trigger mode,
Elevation and semantics retain typed editors. Adaptive follows Theme.platform;
Apple targets use the actual Cupertino spinner and ignore the SDK's documented
Material-only fields. NoSpinner retains the refresh/status lifecycle without a spinner.
The child must be able to overscroll; choose suitable scroll physics explicitly.

The isolated Canvas keeps the real SDK wrapper and visible child but never runs
project callbacks. An explicit project On refresh or custom predicate disables
refresh activation with a concrete preview diagnostic; it is not substituted with
a successful refresh or the default filter. An unconfigured handler uses the same
generated no-op. Project status callbacks are reported as unexecuted without
blocking the SDK cycle. Save/reopen, further editing, Undo/Redo and DnD cover all
three variants. Formats remain `.fd` 13, Catalog API 14 and Canvas model 18.

### RefreshProgressIndicator: refresh arrow and independent insets

Material → **RefreshProgressIndicator** exposes all 12 fields of the pinned
Flutter 3.44.8 [constructor](https://api.flutter.dev/flutter/material/RefreshProgressIndicator/RefreshProgressIndicator.html).
It is a const-capable leaf with no stored creation defaults. It paints the actual
SDK refresh arrow/arc; it is not the separate pull-to-refresh `RefreshIndicator` wrapper.
Value, all three color inputs, stroke width/alignment/cap, elevation, independent
physical/directional indicator margin and padding, and both semantics fields are editable.

**Stroke width** has three distinct modes: `<not set>` omits the argument and uses
2.5; **Inherited (null)** writes `strokeWidth: null`, allowing the theme width or
SDK fallback 4; an explicit finite number preserves its signed value. The custom
editor keeps drafts until OK, and Restore Default removes the argument. Zero and
null survive save/reopen, further editing and Undo/Redo as different values.

**Value color** supports stopped literal/theme/null colors and type-checked project
`Animation<Color?>` references. The isolated Canvas does not execute project code;
it reports that preview limit while preserving the reference and generated Dart.
Canvas preserves SDK clamping, refresh-arrow transitions, foreground opacity,
Material background/elevation and theme defaults. Incompatible resolved geometry,
including a visible arrow in a non-square inner paint area, produces an explicit
diagnostic without changing saved values or inventing dimensions. Inherited
Circular-only arguments are not exposed because this constructor does not accept them.
Formats remain `.fd` 13, Catalog API 14 and Canvas model 18.

### CircularProgressIndicator: Material and adaptive progress

Material → **CircularProgressIndicator** exposes 15 Properties: all 14 optional
fields of the pinned Flutter 3.44.8 [constructor](https://api.flutter.dev/flutter/material/CircularProgressIndicator/CircularProgressIndicator.html)
plus **Constructor**. Choose `material` or [`adaptive`](https://api.flutter.dev/flutter/material/CircularProgressIndicator/CircularProgressIndicator.adaptive.html);
both are const-capable and have no Child slot. Creation stores only the material
variant, leaving progress, size and appearance at SDK defaults.

The adaptive constructor has no Color argument. Switching to adaptive clears Color;
setting Color switches back to material. Each transition is one undoable edit and
preserves unrelated fields. Value and Controller likewise switch atomically. Value
color reuses the stopped literal/theme, stopped null and typed `Animation<Color?>`
choices; Controller accepts a typed `AnimationController` reference.

Stroke width and alignment retain signed finite values, including alignment beyond
−1…1. Track gap also accepts positive Infinity. Stroke cap, normalized BoxConstraints,
physical/directional padding, colors, semantics and the optional centered Year 2023
checkbox all retain explicit values separately from Restore Default. Canvas uses
the actual SDK, including M2/M3/theme precedence, progress clamping and animation;
invalid resolved geometry receives a diagnostic, not an invented radius or clamp.

With adaptive and resolved `Theme.platform` iOS/macOS, Flutter uses
CupertinoActivityIndicator: Background color becomes its tick color and Value
controls partial reveal. Other Material properties, including project animation/
controller references, constraints, padding and semantics overrides, are retained
but ignored there as in the pinned SDK. On Material paths, the isolated Canvas
explicitly reports that it cannot execute project animations/controllers.
Stable Properties, save/reopen/further editing, Undo/Redo and Palette DnD apply to
both variants. Formats remain `.fd` 13, Catalog API 14 and Canvas model 18.

### LinearProgressIndicator: progress, animation and track appearance

Material → **LinearProgressIndicator** exposes all 13 fields of the pinned
Flutter 3.44.8 [constructor](https://api.flutter.dev/flutter/material/LinearProgressIndicator/LinearProgressIndicator.html).
It has no Child slot or stored creation defaults. Unset **Value** selects the SDK's
indeterminate animation; a number selects determinate progress. Signed finite values
are retained exactly, while Flutter clamps their displayed progress to 0–1.
Setting **Controller** clears Value, and setting Value clears Controller, as one
undoable edit. Controller accepts a type-checked project `AnimationController` reference.

**Value color** has separate modes for default, a literal/theme color wrapped in
`AlwaysStoppedAnimation<Color>`, an explicit stopped null color, and a type-checked
project `Animation<Color?>` reference. Null color is not a nullable animation:
dynamic, nullable outer references and wrong types fail the analyzer's strict proof.
Project animation/controller code is not executed in the isolated Canvas; its exact
preview limitation is reported without altering the saved reference or generated Dart.

**Minimum height**, **Stop indicator radius** and **Track gap** support positive Infinity
as well as their numeric domains. Min height must be positive; signed stop radius/gap
values retain SDK behavior. Colors accept literal ARGB or ColorScheme roles, and
**Border radius** accepts physical/directional elliptical corners. **Year 2023** uses
the centered optional checkbox; false requests the newer appearance. Semantics label
and value remain independent optional strings; determinate values should be numbers
such as `45` or `45%`. All fields support Restore Default.

Canvas renders the real SDK indicator with theme precedence, RTL, determinate and
indeterminate animation, and progress semantics. Give it bounded width (for example
inside SizedBox when used in a Row); infinite Min height also needs bounded height.
Unavailable constraints report a diagnostic, not an invented size. The pinned SDK's
M2/new-appearance stop-color edge is likewise reported explicitly. Stable Properties,
save/reopen, further editing, Undo/Redo and ordinary Palette DnD are covered.
Formats stay `.fd` 13, Catalog API 14 and Canvas model 18.

### CircleAvatar: images, initials and flexible radii

Material → **CircleAvatar** covers all nine scalar fields of the pinned Flutter
3.44.8 [constructor](https://api.flutter.dev/flutter/material/CircleAvatar/CircleAvatar.html)
and its optional **Child** slot. Creation needs no asset and keeps SDK defaults:
`const CircleAvatar()` uses radius 20. Background/foreground colors accept literal
ARGB or a reviewed ColorScheme role; **Background image** and **Foreground image**
reuse declared AssetImage, ExactAssetImage and ResizeImage editors. Each image has
its own typed error callback. A callback requires that image; clearing the image
also clears its callback atomically, with one Undo.

**Radius** selects a fixed radius; **Min radius**/**Max radius** select bounds.
Switching between these alternatives clears only the conflicting radius fields.
All three accept nonnegative numbers or explicit **Infinity**. Unset is distinct:
all three unset means radius 20, while only Max radius = Infinity leaves the avatar
free to use the parent's available size. Infinite minimum sizes need bounded parent
constraints. Inverted effective minimum/maximum diameters are rejected, not clamped.
Generated Infinity is the constant expression `(1.0 / 0.0)`, exactly positive
infinity, so no new `dart:core` import changes the surrounding user code's scope.

Canvas uses the real CircleAvatar with M2/M3 color inheritance, animated finite
size/color changes and unscaled child text. Circular decorations crop the images,
not an arbitrary Child. Foreground image paints above Child; background image is
behind it. Missing or corrupt image layers report their exact failure while leaving
the valid fallback/Child visible. Callback identifiers stay out of the isolated
preview and user callback code is not executed there. Provider support remains the
existing asset-backed model; this slice does not add network/file/custom providers.

Properties preserve unset/reset, stable cells, save/reopen/further editing and
Undo/Redo. Finite/infinite constraint transitions replace only the preview's SDK
shell to avoid unsupported constraint interpolation, retaining child identity.
Formats stay `.fd` 13, Catalog API 14 and Canvas model 18.

### Badge: labels, dots and numeric counters

Material → **Badge** covers both pinned Flutter 3.44.8
[Badge and Badge.count](https://api.flutter.dev/flutter/material/Badge-class.html)
constructors in one palette item. All 41 typed fields are available: ten direct
settings and all 31 existing TextStyle leaves, including theme roles, locale,
font families/package, Paint, shadows, OpenType features, variations and decorations.
The optional **Label** and **Child** are separate widget slots. Creation stores no
scalar defaults and generates the equivalent of `const Badge()`, a small dot
(empty optional slots may be emitted as null). A label produces a large badge.

Set **Count** (zero or greater) to use non-const `Badge.count`. **Max count** must
be positive and requires Count; when omitted its SDK value is 999. Counts above
that maximum display `maximum+`; zero is not an unset value. Count mode owns its
generated label, so first move or clear an existing Label slot before setting Count.
Count-mode label insertion is unavailable with a concrete explanation, not silent
data loss. Reset Count atomically resets Max count and restores the ordinary mode;
shared appearance, Child and one-step Undo are preserved.

Canvas uses the actual SDK, including BadgeTheme precedence and M3 badge defaults
even in an M2 application. Text color overrides style color, but foreground Paint
still takes precedence as in TextStyle.copyWith. Alignment supports physical/RTL
coordinates. The SDK adds its compatibility `(0, 8)` offset for labels and ignores
offset for small dots. Hidden labels leave Child visible; the stored Label remains
editable in the tree. Both empty slots have insertion targets, including tiny or
hidden badges. Properties retain unset/reset, centered Boolean checkboxes and
stable cells through save/reopen/further editing and Undo/Redo. Formats stay 13/14/18.
Canvas also compensates for the pinned SDK's stale large-size render update while
preserving child input/focus and active label editing. Generated Dart remains the
standard Badge API; the workaround is local to Designer preview.

Existing shared save limitation: an extra explicit false decoration flag can change
Designer data without changing generated Dart. If another Dart/Designer revision
is still unsaved, save or undo that revision before retrying this edit. The failed
attempt preserves both files, the draft and history; after Save the same value is
supported through the existing Designer-only persistence path.
After Undo/Redo, a decoration reset can meet the same save-first requirement.

### Card: all three Material variants and editable shapes

Material → **Card** supports the pinned Flutter 3.44.8
[Card, Card.filled and Card.outlined](https://api.flutter.dev/flutter/material/Card-class.html)
constructors in one palette item. Its 31 typed rows include all constructor fields,
a required **Variant** selector and complete parameters for ten built-in shape
choices. The optional **Child** is edited as a slot. Only the Designer variant is
stored on creation; Dart starts with `const Card()` and keeps SDK/theme defaults.

Shape choices include rounded/beveled/continuous/superellipse rectangles, circle,
oval, stadium, linear edges, star and polygon. Edit the border side, physical or
directional elliptical radii, fractional star points, rounding, rotation in degrees
and each linear edge. Shape changes clear incompatible fields atomically; one Undo
restores the previous shape. Optional fields support **Restore Default**, and both
Boolean fields retain the centered checkbox plus separate `<not set>` state.

Other ShapeBorder families, compound shapes and custom shapes use analyzed typed
Dart references. The isolated Canvas cannot execute them and reports an explicit
preview-unavailable state while retaining the child and selection. The same honest
state protects Canvas from star/polygon counts above its 4096-point budget; generated
Dart and stored values remain unchanged. Normal shapes render the actual Card with
CardTheme/M2/M3 precedence, clipping, border paint order and semantics.

### VerticalDivider: vertical Material separator

Material → **VerticalDivider** exposes all six properties of the pinned Flutter 3.44.8
[constructor](https://api.flutter.dev/flutter/material/VerticalDivider/VerticalDivider.html):
**Width**, **Thickness**, **Indent**, **End indent**, **Color**, and **Radius**.
All fields support unset/reset; creation keeps `const VerticalDivider()`.
Width is the whole horizontal space, not line thickness. Indent is the **top**
space and End indent the **bottom** space; RTL does not reverse these two fields.
The physical/directional elliptical radius editor and literal/semantic colors are reused.

Canvas uses the real SDK VerticalDivider and DividerTheme/M2/M3 defaults, including
RTL resolution of directional corner radii. Give the parent a bounded height
(for example a Row inside SizedBox, or an IntrinsicHeight Row with other children).
Designer does not invent a height or wrap the widget automatically. Zero-width
selection, ordinary DnD, stable Properties, save/reopen/further editing and Undo/Redo
are covered. Nonzero radius needs positive effective thickness to avoid the pinned
SDK's hairline limitation: debug asserts; release ignores the radius.
Formats remain 13/14/18; no assets, slots or extra construction mode are added.

### Divider: horizontal Material separator

Material → **Divider** exposes all six properties of the pinned Flutter 3.44.8
[constructor](https://api.flutter.dev/flutter/material/Divider/Divider.html):
**Height**, **Thickness**, **Indent**, **End indent**, **Color**, and **Radius**.
Every field supports unset/reset; creation preserves `const Divider()`.
Radius uses the existing physical/directional editor with independent elliptical
corner radii. Color supports literal ARGB and reviewed ColorScheme roles.

Canvas renders the actual SDK Divider. Local values override DividerTheme and then
Material defaults: space 16, zero indents; Material 2 uses a hairline and dividerColor,
Material 3 uses thickness 1 and outlineVariant. Leading/trailing indents and directional
radii follow LTR/RTL. Divider is a leaf, not a wrapper or text-editable widget.
Palette/tree/slot insertion, movement, stable Properties, save/reopen/further editing
and Undo/Redo are covered without changing schema/API/Canvas versions 13/14/18.

Pinned SDK limitation: a nonzero radius with effective thickness 0 triggers a
Border.paint assertion in debug; release ignores that radius. Set positive thickness
for rounded lines. This also matters when thickness is unset under Material 2.
The Designer does not silently change thickness or reject theme-dependent omission.

### ImageIcon: asset-backed and empty icons

Basic → **ImageIcon** covers all four properties of the pinned Flutter 3.44.8
[constructor](https://api.flutter.dev/flutter/widgets/ImageIcon/ImageIcon.html):
the required nullable **Image**, optional **Size**, **Color** and **Semantic label**.
The image editor supports **None (empty image icon)** or the existing declared-asset
AssetImage/ExactAssetImage providers, with optional ResizeImage dimensions, policy
and upscaling. None is explicit Dart `null`, not `<not set>` or a failed image.
The required image argument cannot be removed/reset; the other three fields can.
Creation selects the first available declared image asset, otherwise None, so
missing assets never block adding this widget. Existing Image behavior is unchanged.

The actual ImageIcon inherits size/color and theme opacity from IconTheme. Local
size/color override their defaults, but inherited opacity still affects local color.
Unlike Icon, it does not inherit font axes, shadows or text scaling. The SDK uses
BoxFit.scaleDown and one outer semantic label; a null image reserves its icon size.
Existing Canvas asset validation, placeholders and diagnostics apply to non-null
providers. Save/reopen/further editing and Undo/Redo preserve null/provider changes.
The provider editor remains asset-backed, not arbitrary network/file/custom Dart.
The newer `useOriginalColors` argument is absent from the pinned SDK and is not
admitted. Formats remain 13/14/18; the SDK is not upgraded by this slice.

### IconTheme: inherited icon styling, including merge

Basic → **IconTheme** covers the [widget and merge helper](https://api.flutter.dev/flutter/widgets/IconTheme-class.html)
and all nine [IconThemeData fields](https://api.flutter.dev/flutter/widgets/IconThemeData/IconThemeData.html):
size, fill, weight, grade, optical size, color, opacity, shadows and text scaling.
The nine SDK fields allow unset/reset; the required **Merge inherited theme** choice
starts false. Direct mode replaces the outer theme and fills unspecified consumer
values from Flutter's fallback; merge mode inherits each unspecified field.
The required `data` is generated even when every SDK field is unset.

Colors and ordered shadows use the existing literal/semantic-theme editors;
an explicit empty shadow list clears inherited shadows. Opacity accepts finite
values and Flutter clamps the effective result to 0–1, applying it to both local
and inherited icon colors. It does not fade the entire child subtree or shadows.
Local Icon fields retain their SDK precedence. The required child supports atomic
wrapping/replacement, save/reopen/further editing and Undo/Redo. The fallback data
is representable through explicit field values; custom IconThemeData subclasses
and dynamic resolver expressions are not part of this closed value editor.
Schema/API/Canvas remain 13/14/18.

### DefaultSelectionStyle: cursor and selection defaults, including merge

Basic → **DefaultSelectionStyle** exposes the complete insertable
[constructor](https://api.flutter.dev/flutter/widgets/DefaultSelectionStyle/DefaultSelectionStyle.html)
and [merge helper](https://api.flutter.dev/flutter/widgets/DefaultSelectionStyle/merge.html).
Edit **Cursor color**, **Selection color**, **Mouse cursor**, and the Designer-only
**Merge inherited style** checkbox. Both colors accept ARGB literals or Material
ColorScheme roles. The cursor list contains all 36 SystemMouseCursors, defer and
uncontrolled, plus WidgetStateMouseCursor clickable/adaptiveClickable/textable.

Merge is an explicit required construction choice, initially false. False creates
the ordinary widget: unset SDK fields are null and shadow
the outer style. True uses `.merge`, inheriting each unset field independently;
explicit fields override the inherited value. The mode is never emitted as a Dart
argument, and `.merge` correctly prevents const propagation to its ancestors.
Reset restores omission for the three SDK fields without replacing the property
sheet. Merge always retains true or false and cannot be reset to an absent mode. The required child
supports atomic wrapping/replacement, save/reopen/further edits and Undo/Redo.

The actual SDK controls consumer precedence. A TextField's explicit cursor color
wins, while its mouse cursor is configured separately. Selection colors and hover
cursors affect selectable Text; this wrapper does not itself enable text selection.
The SDK `.fallback` constructor is intentionally not insertable: its child throws
when mounted. Custom cursor subclasses/resolver callbacks are outside the closed
41-preset model. Schema/API/Canvas remain 13/14/18.

### DefaultTextHeightBehavior: inherited text height defaults

Basic → **DefaultTextHeightBehavior** covers the required
[TextHeightBehavior value and child](https://api.flutter.dev/flutter/widgets/DefaultTextHeightBehavior/DefaultTextHeightBehavior.html).
The value exposes all three settings: **Apply height to first ascent**, **Apply
height to last descent** and **Leading distribution** (`proportional` or `even`).
Both booleans use centered checkboxes; all three fields allow unset/reset, preserving
the value constructor's true/true/proportional defaults. The managed key is unchanged.

Even with every field unset, generation supplies `const TextHeightBehavior()` to
the required argument. An inner default wrapper resets the inherited behavior; it
does not merge unspecified leaves with an outer wrapper. A local Text behavior
overrides inheritance, and a non-null DefaultTextStyle textHeightBehavior takes
precedence for descendant Text. The controls affect the first line's ascent, last
line's descent and distribution of added leading; they do not set font size or
TextStyle.height themselves.

Drop on an existing widget to wrap it. Required child replacement is atomic; the
child cannot be cleared or moved away independently. Actual SDK inheritance,
dynamic text layout, tree/Canvas selection, F2 editing, save/reopen/further editing
and Undo/Redo use the complete slice. Schema/API/Canvas stay 13/14/18.

### TickerMode: control animation tickers without hiding content

Basic → **TickerMode** covers the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/TickerMode/TickerMode.html):
required **Enabled**, optional **Force frames** and required **child**. The managed
key remains outside the property rows. Drop TickerMode on an existing widget to
wrap it; the child can be replaced but never cleared or moved away independently.

Enabled starts checked as an explicit Designer creation value, not an SDK default.
It accepts true/false but cannot be unset or reset away. Force frames accepts
unset/false/true; omission preserves Flutter's false default. Both use the shared
centered checkbox renderer/editor. Force frames can increase battery use while
the device would otherwise be idle, so enable it only when needed.

Actual SDK nesting combines Enabled with ancestor AND and Force frames with
ancestor OR. Disabling tickers mutes callbacks from widget-aware ticker providers;
elapsed time continues, so this is not an animation timeline pause. Child state,
layout, painting, focus and labels remain, and Designer selection/F2 editing stay
available. The static TickerMode.merge helper adds a Builder but no extra editable
behavior: its null requests have the same effective ticker values as enabled=true
and omitted forceFrames in the bare constructor. Persistence, further editing after
reopen, both flags, required-child replacement and Undo/Redo use the same transaction
flow. Schema/API/Canvas versions stay 13/14/18.

### Visibility: hide content with explicit retention and replacement

Basic → **Visibility** covers the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/Visibility/Visibility.html):
required `child`, optional non-null `replacement`, `visible` and all six `maintain…`
flags; `key` remains Designer-managed. Seven centered checkboxes preserve explicit
false/true versus `<not set>`; omitted visible means true, omitted maintenance flags
mean false. No constructor defaults are persisted on creation.

Drop Visibility on an existing widget to wrap it. Tree wrapping supports root and
non-root content; Canvas wrapping uses existing non-root targets. **Slots → child**
supports atomic replacement but not clearing. **Slots → replacement** supports
add/replace/clear; clearing omits the Dart argument and restores `SizedBox.shrink()`,
never `replacement: null`. An inactive replacement remains saved and editable.

Checkbox edits enforce Flutter's dependency chain in one Undo step: animation
requires state, size requires animation, semantics/interactivity require size,
and focusability requires state. Enabling a flag enables missing prerequisites;
disabling/resetting one resets its currently true dependents, preserving unrelated
values and explicit false settings. To obtain the exact
[Visibility.maintain behavior](https://api.flutter.dev/flutter/widgets/Visibility/Visibility.maintain.html),
set all six maintenance flags to true. Designer emits the equivalent default
constructor with those explicit flags, not a second palette type.

Actual Flutter Visibility controls disposal, tickers, size, painting, semantics,
pointer hits and focus. Changing maintenance flags can discard runtime child state;
only visible should normally toggle during runtime. Hidden/inactive branches remain
editable in the widget tree and Properties, but do not expose misleading Canvas
handles or resurrect themselves for F2. Save/reopen/further editing, generation,
Undo/Redo and rollback cover both slots and all flags. Schema/API/model stay
13/14/18, NBFC 1.

For pinned Flutter 3.44.8, Canvas also invalidates the real Visibility render
object's semantics on maintained-size visibility changes. This fixes stale Canvas
accessibility without replacing SDK rendering. Generated applications use Flutter's
Visibility directly; this Canvas-only workaround does not patch their SDK.

### ExcludeFocusTraversal: skip traversal without forbidding direct focus

Accessibility → **ExcludeFocusTraversal** covers the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/ExcludeFocusTraversal/ExcludeFocusTraversal.html):
optional `excluding` (omitted = true), required `child` and Designer-managed `key`.
The centered checkbox preserves explicit false/true versus `<not set>`; **Restore
Default** removes the explicit argument. No creation default is persisted.

Drop it onto an existing widget to wrap the subtree atomically. Tree wrapping
supports root/non-root widgets; Canvas supports non-root targets. **Slots → child**
replaces the required child but cannot clear it. Expanded/Flexible/Spacer cannot
be wrapped because their direct Row/Column ParentData placement must remain valid.

Unlike ExcludeFocus, this widget skips descendants during keyboard traversal but
still permits explicit focus requests and retains an already focused descendant.
Other focus restrictions still apply. It does not rewrite a descendant's own
configured `skipTraversal`, although the effective getter becomes true under an
excluded ancestor. Nested `excluding: false` cannot override an excluded ancestor.
Layout, paint, pointer hits and semantic labels remain unchanged. Typed properties,
slots, actual Canvas rendering, generation/provenance, Save/reopen/further edits,
Undo/Redo, rollback and F2 Text editing use the existing contracts. Schema/API/model
stay 13/14/18, NBFC 1.

### ExcludeFocus: control descendant focus without blocking pointer hits

Accessibility → **ExcludeFocus** covers the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/ExcludeFocus/ExcludeFocus.html):
optional `excluding` (omitted = true), required `child` and Designer-managed `key`.
The centered checkbox keeps explicit true/false separate from `<not set>`;
**Restore Default** removes the explicit argument. No default is forced into `.fd`.

Drop the palette item onto an existing widget to wrap it atomically. Tree wrapping
accepts root or non-root widgets; the Canvas route uses existing non-root targets.
The required child is replaceable through **Slots**, but cannot be cleared, removed
or replaced with an empty prototype. Expanded/Flexible/Spacer retain their direct
Row/Column parent-data rules and cannot be wrapped by ExcludeFocus.

The actual SDK widget prevents descendants from receiving focus while excluding.
Turning exclusion on unfocuses an already focused descendant; turning it off allows
focus again but does not restore it automatically. It does not remove labels,
block pointer hits or change visual layout/painting. Typed Properties/Slots,
generation/provenance, Save/reopen/further editing, Undo/Redo and rollback use the
existing transaction flow. Schema/API/model stay 13/14/18, NBFC 1.

F2 Text editing remains available through a separate Designer-only focus branch;
it does not enable focus in application controls. Drag previews also reject
moving a required child out of its wrapper before showing an accepted target.

### IndexedSemantics: explicit indexes for accessible scrolling

Accessibility → **IndexedSemantics** covers the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/IndexedSemantics/IndexedSemantics.html):
required integer `index`, optional `child` and Designer-managed `key`. Edit
**Semantics → Index** and **Slots → child**. New palette nodes start at index 0;
this is a Designer prototype value, not a Flutter default. The required field
cannot be unset. The shared native/Web integer contract admits
`-9007199254740991..9007199254740991`; this portability limit is not an SDK minimum
or maximum. Flutter does not impose a nonnegative constructor constraint.

Actual IndexedSemantics/RenderIndexedSemantics annotates the first child semantic
node, which can be a container for multiple semantic children, not necessarily a
leaf. Layout, paint and ordinary hits stay unchanged. For manual ListView indexes,
set `addSemanticIndexes: false` and supply the intended `semanticChildCount`;
Designer does not silently rewrite either parent setting or renumber siblings.
Synthetic Designer labels and selection actions on this wrapper and its descendants
are suppressed without inventing semantic boundaries; accessible wrapper identity
and editing remain in the NetBeans widget tree. Designer annotations on ancestors
outside the indexed subtree remain unchanged. Pointer/F2 editing and preview warnings
remain available. Properties/Slots, all placement/move routes, Save/reopen/further
editing and Undo/Redo use the existing typed transaction flow. Schema/API/model
remain 13/14/18, NBFC 1.

### MergeSemantics: one accessibility node for related content

Accessibility → **MergeSemantics** covers the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/MergeSemantics/MergeSemantics.html):
optional `child`; `key` remains Designer-managed. There are no scalar constructor
properties. Identity and the `Slots` child editor remain available, including
add/replace/clear; edit nested widgets through their own Properties.

Canvas uses actual MergeSemantics/RenderMergeSemantics. Descendant labels and states
merge into one semantic node without changing layout, paint or ordinary hits.
Flutter combines labels with newlines; if multiple descendants handle the same
semantic action, the first in tree order handles it. Conflicting states can yield
a misleading combined description, so group only content representing one control.
Designer selection, Palette/tree/Canvas placement and moves, Save/reopen/further
child and descendant editing, Undo/Redo and rollback use the existing transaction
flow. Synthetic Designer labels and semantic selection actions inside the group
are excluded from the merge; individual accessible editing remains in the widget
tree, while mouse selection and preview diagnostics remain available. Empty nodes
keep external targets. Schema/API/model remain 13/14/18, NBFC 1.

### BlockSemantics: hide previously painted accessibility content

Accessibility → **BlockSemantics** covers the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/BlockSemantics/BlockSemantics.html):
optional `blocking` (omitted = true) and `child`; `key` remains Designer-managed.
The centered checkbox preserves explicit false/true separately from `<not set>`,
which **Restore Default** restores. No default is forced into the saved model.

The actual Canvas widget hides semantics of earlier-painted content below the same
semantic boundary, retaining its own child and later-painted content. It does not
exclude its descendants like ExcludeSemantics or filter pointer hits like
AbsorbPointer. Layout, painting and ordinary hit testing remain unchanged.
Properties/Slots, Palette/tree/Canvas placement and moves, Save/reopen/further
editing, Undo/Redo and rollback use the existing typed transaction flow. Empty
nodes retain external Designer targets. Schema/API/model remain 13/14/18, NBFC 1.

### AbsorbPointer: block pointer hits without changing layout or paint

Basic → **AbsorbPointer** exposes the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/AbsorbPointer/AbsorbPointer.html):
`absorbing` (omitted = true), deprecated `ignoringSemantics` (omitted = null) and
optional `child`; `key` remains Designer-managed identity. Both boolean fields use
centered checkboxes with a separate `<not set>` state restored by **Restore Default**.
The deprecated field is fully supported and labeled: null retains semantic labels
but blocks user actions while absorbing, false preserves actions, and true removes
the semantic subtree regardless of absorbing.

Canvas uses actual `AbsorbPointer`/`RenderAbsorbPointer`. When absorbing, neither
the child nor a widget behind it receives the pointer hit: this is not IgnorePointer
pass-through. Layout and painting remain unchanged. The Designer can still select
the AbsorbPointer body; tree selection, child-slot edits, geometric DnD and F2
editing of a selected Text descendant remain available. Empty zero-size nodes keep
external Designer targets without inserting fake layout content. Properties/Slots,
all placement/move routes, save/reopen/further edits, Undo/Redo and rollback use the
existing transaction boundary. Schema/API/model stay 13/14/18 and NBFC stays 1.

### IgnorePointer: pointer pass-through with complete semantics controls

Basic → **IgnorePointer** exposes both boolean fields in the complete
[Flutter constructor](https://api.flutter.dev/flutter/widgets/IgnorePointer/IgnorePointer.html):
`ignoring` (omitted = true) and `ignoringSemantics` (omitted = null), plus optional
`child`; `key` remains Designer-managed identity. Explicit values use centered
checkboxes. **Restore Default** returns to `<not set>` without conflating false
with omission. The deprecated `ignoringSemantics` field remains fully editable and
is clearly marked: null follows `ignoring` for semantics actions while retaining
labels, false preserves those actions, and true excludes the semantic subtree.

Canvas instantiates the actual `IgnorePointer`/`RenderIgnorePointer`. Ignored
content still lays out and paints normally; pointer hits can reach widgets behind
it, unlike `AbsorbPointer`. Designer selection and child-drop affordances remain
separate from that runtime behavior. Select an ignored node in the tree or through
its compact Canvas handle. The handle uses available space outside the widget;
only when no such space fits does its explicit 36px Designer target overlap the
body. Properties/Slots, Palette/tree/Canvas placement,
movement, Save/reopen/further edits, Undo/Redo and rollback use the existing typed
transaction boundary. No raw expression or new protocol is needed: schema/API/model
stay 13/14/18 and NBFC framing/control/wire stay 1.

### RepaintBoundary: real repaint isolation

Basic → **RepaintBoundary** covers the complete default
[Flutter constructor](https://api.flutter.dev/flutter/widgets/RepaintBoundary/RepaintBoundary.html):
optional `child`; `key` remains Designer-managed identity. There are no scalar
constructor properties to omit. Use the `Slots` tab to insert, replace or clear the
child, then select descendants to edit their own properties. Palette/tree/Canvas
placement, movement, stable selection, Save/reopen/further-edit, Undo/Redo and
rejected-change rollback use the same transaction boundary.

Canvas constructs the actual RepaintBoundary/RenderRepaintBoundary, giving the child
its own paint layer. It preserves layout, constraints, semantics and hit testing;
empty nodes keep external Designer selection/drop targets without a fake child.
It is a repaint-isolation tool, not a guarantee of faster rendering or a raster-cache
policy setting. The SDK `wrap`/`wrapAll` helpers only derive wrapper keys; they do not
add another rendering branch or a persisted `childIndex` property. No new scalar
editor, value encoding or protocol is needed: schema/API/model stay 13/14/18.

### PhysicalShape: reviewed shapes and custom path clippers

Basic → **PhysicalShape** exposes all five non-key properties from the
[Flutter constructor](https://api.flutter.dev/flutter/widgets/PhysicalShape/PhysicalShape.html):
required `clipper`, `clipBehavior`, `elevation`, required `color` and `shadowColor`,
plus an optional `child`. The structured clipper editor offers rounded rectangle,
beveled rectangle, continuous rectangle, rounded superellipse, circle and stadium,
using real `ShapeBorderClipper` and Flutter border classes. Four corner X/Y radii
support physical or directional geometry; directional shapes require explicit LTR
or RTL because this SDK helper does not resolve ambient directionality. Circle and
stadium ignore but retain stored radii/direction when switching shapes.

The same editor accepts an existing current/declared-package `CustomClipper<Path>`
value or a const/non-const zero-argument call. Configured arguments belong in a
project getter/factory, not raw `.fd` source. Exact non-null analyzer proof gates
these references. The isolated Canvas cannot run project code: custom references
retain the child with an accessible preview-unavailable warning, without fabricated
fill, shadow or clipping. Built-in presets render actual PhysicalShape geometry,
shadows, translucent/theme colors and all four clip modes on native/Web profiles.

Creation starts with a rounded rectangle and literal color `0xFF2196F3`, so no
project helper is required to drag it from the Palette. Properties/Slots, DnD,
save/reopen/further-edit, Undo/Redo and rollback share the existing transaction flow.
The closed `shapeBorderClipper` value advances `.fd` to 13, Catalog API to 14 and
Canvas model to 18; NBFC framing/control/wire remain 1. Older `.fd` schemas migrate
only on an admitted edit. Full physical desktop acceptance remains deferred.

### PhysicalModel: shape, clipping and elevation shadows

Basic → **PhysicalModel** exposes all six non-key properties from the
[Flutter constructor](https://api.flutter.dev/flutter/widgets/PhysicalModel/PhysicalModel.html):
`shape`, `clipBehavior`, `borderRadius`, `elevation`, required `color` and
`shadowColor`, plus an optional `child`. Palette creation supplies literal
`0xFF2196F3` for the required color; both colors can use literal ARGB or a reviewed
theme color. Elevation accepts finite non-negative values. All four clip modes,
both shapes, per-corner elliptical radii and translucent colors are supported.

The API takes concrete physical `BorderRadius`, not `BorderRadiusDirectional`:
the editor and validation enforce this distinction. Omission preserves the SDK's
null radius default. Circle ignores but retains the radius, so changing back to
rectangle restores it. On non-square bounds, Flutter's circle shape fills an oval.
The Canvas uses real `PhysicalModel`/`RenderPhysicalModel` painting and shadows;
empty nodes use external Designer selection/drop targets without fake children.

Properties/Slots, Palette/tree/Canvas placement, generation, Save/reopen/further
editing, Undo/Redo and four light/dark SVG variants are covered. The reusable
physical-only radius constraint established contributor Catalog API 13; that
milestone retained `.fd` 12 and Canvas model 17. PhysicalShape advances them above.

### ClipRSuperellipse: rounded-superellipse clipping

Basic → **ClipRSuperellipse** exposes all non-key constructor arguments:
`borderRadius`, `clipper`, `clipBehavior`, plus the optional `child` slot. Physical
and directional elliptical corner radii reuse the structured border-radius editor;
omission preserves `BorderRadius.zero` and `Clip.antiAlias`. All four clip modes
are supported. The real Flutter widget owns continuous corner geometry and radius
clamping, not a `ClipRRect` approximation.

`clipper` supports a closed `CustomClipper<RSuperellipse>` reference in the current
or a declared package library, including members and const/non-const zero-argument
invocations. Configured arguments belong in a project getter/factory, not raw Dart
inside `.fd`. Exact non-null analyzer proof rejects `dynamic`, nullable and wrong
generic clipper types. Flutter ignores `borderRadius` when a clipper is configured;
the radius remains editable and is preserved when the clipper is reset. The isolated
Canvas cannot execute that project code, so it retains the child with a clear,
accessible preview-unavailable warning. Default geometry renders as the real
`ClipRSuperellipse`.

Properties/Slots, Palette/tree/Canvas DnD, stable selection, Save/reopen/further edits,
Undo/Redo and four light/dark SVG variants share the same contract. This slice
introduced no value-format change; its historical aggregate boundary was `.fd` 13 /
Catalog API 14 / Canvas model 18.

### ClipPath: complete API branches within the typed Designer boundary

Basic → **ClipPath** exposes `clipper`, `shape`, `clipBehavior`, and the optional
`child` slot. Omission uses Flutter's default rectangular path and `Clip.antiAlias`.
`clipper` accepts a typed `CustomClipper<Path>` reference; `shape` accepts a typed
`ShapeBorder` reference and selects the real
[`ClipPath.shape`](https://api.flutter.dev/flutter/widgets/ClipPath/shape.html)
static helper. These two properties are mutually exclusive: accepting the other
branch switches them atomically as one undoable change. Each reference editor supports the current library or a
declared `package:` library, an optional member, and an existing value or a const /
non-const zero-argument invocation. Configure constructor arguments in a project
getter or factory; raw Dart text is not stored in `.fd`.

The analyzer must prove the exact non-null type before a change is accepted.
`ClipPath.shape` is never emitted as const, even for a const shape, and its ancestors
correctly lose const eligibility. Save/reopen, further edits, Undo/Redo, child-slot
editing and Palette/tree/Canvas placement use the same revision-bound workflow.
As with the earlier custom clippers, the isolated Canvas cannot execute project
code: a custom clipper or shape receives a visible, accessible **preview unavailable**
warning while preserving the child. Default `ClipPath` renders as the real widget.
This slice left the value algebra unchanged. The later PhysicalShape value sets
the historical `.fd` 13 / Catalog API 14 / Canvas model 18 boundary.

## Direction

The project intentionally implements **IDE support first** and keeps the visual designer isolated until the core plugin is useful by itself.

The current usable workflow is:

1. Open/detect a Flutter project.
2. Locate and validate the Flutter SDK.
3. Edit highlighted Dart sources with the SDK Analysis Server and NetBeans DAP integration.
4. Discover and select Desktop, Mobile, or Web run targets.
5. Create and manage Android Virtual Devices, or launch an already configured iOS simulator.
6. Run or debug a Flutter application.
7. Open Flutter DevTools for the active application from NetBeans.
8. Use Hot Reload, Hot Restart, Stop, and the NetBeans Output window.
9. Build, clean, resolve packages, analyze sources, and run all/file/single tests through native NetBeans tooling UI.
10. Edit `pubspec.yaml` with Flutter-aware completion and semantic diagnostics.
11. On Windows, open a valid paired `.dart`/`.fd` Designer document in a
    native Flutter Canvas, switch among exact Android/iOS/desktop
    adaptive preview targets allowed by the project, and synchronize stable
    widget selection between the Canvas and the widget tree. The tree
    remains fully expanded, hides its redundant expansion controls and uses
    scoped one-pixel golden parent-child connectors, without changing the look
    of other NetBeans trees. A project
    with `web/` also receives a browser-sized Web layout preview on the native
    engine; browser-only runtime behavior is not emulated. The Windows Canvas
    accepts the eighty-nine capability-authorized Palette widgets (`Scaffold`,
    `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`, `Wrap`, `Padding`, `Center`,
    `SizedBox`, `AspectRatio`, `Container`, `Opacity`, `Align`,
    `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
    `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `PreferredSize`, `ListBody`, `OverflowBar`, `SafeArea`,
    `ListView`, `GridView.count`, `SingleChildScrollView`, `Text`, `Icon`, `Image`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer`, `AbsorbPointer`, `ExcludeSemantics`, `BlockSemantics`, `MergeSemantics`, `IndexedSemantics`, `ExcludeFocus`, `ExcludeFocusTraversal`, `Visibility`, `TickerMode`, `DefaultTextHeightBehavior`, `DefaultSelectionStyle`, `IconTheme`, `ImageIcon`, `Divider`, `VerticalDivider`, `Card`, `Badge`, `CircleAvatar`, `LinearProgressIndicator`, `CircularProgressIndicator`, `RefreshProgressIndicator`, `RefreshIndicator`, `TextButton`, `OutlinedButton`, `FilledButton`, `FloatingActionButton`, `IconButton`, `Checkbox`, `Switch`, `Slider`, `RangeSlider`, `Radio`, `RadioGroup`, `ListTile`, and `CheckboxListTile`) through a
    fail-closed 6,675-cell catalog matrix with 6,286 accepted and 389 rejected
    combinations, with paired generation,
    analysis, Save and Undo/Redo.
    The same Palette token may be dropped on an exact widget-tree row when that
    parent has one unambiguous compatible slot; ambiguous multi-slot parents
    such as `Scaffold` fail closed instead of guessing a destination.
    Existing non-root widgets may also be dragged within the same tree: drop on
    a uniquely compatible container or on a before/after insertion line. The
    exact immutable subtree and every stable ID are preserved, and the drop is
    replanned against the latest revision before one `MoveWidget` is admitted.
    While the tree drag is active, an available Canvas paints the exact future
    destination with a thin amber marker; the tree operation does not depend on
    Canvas readiness.
    Each Design tab defaults to `Fit`, also offers 25–200% manual zoom, and
    exposes native Canvas scrollbars whenever the fixed logical profile no
    longer fits. Zoom and scroll are view-only and are never written to `.fd`.
12. On the Windows Canvas, double-click or press F2 on the selected existing
    `Text` widget to edit its `data` through a real Flutter `TextField` and
    `TextInputClient`. Enter inserts a newline; Ctrl+Enter commits and Escape
    cancels only when Flutter reports no active composing range. IME preedit
    remains runner-local. One admitted final commit is bound to the exact
    session, revision, layout, interaction fence and selected stable ID, then
    produces at most one existing `SetProperty(data)` command; unchanged text
    is a no-op. Deterministic Flutter and Java protocol/session plus
    view/mutation-bridge tests cover this Windows slice, but physical CJK IME
    acceptance remains open because the current gate host has no
    composition-capable input method. This does not claim a Linux, macOS or
    runtime-faithful Web implementation.

With the core IDE workflow stable, version 0.1.3 is now building the
Matisse-like Flutter Designer in staged, non-authorizing slices.

## Modules

- `flutter-core-api` — stable Java contracts and shared domain objects.
- `dart-analysis` — lifecycle-safe Dart Language Server process and raw LSP transport.
- `flutter-sdk` — SDK discovery, validation and Flutter CLI process execution.
- `flutter-project` — Flutter project recognition and project metadata.
- `flutter-run` — target discovery, Android SDK/AVD lifecycle services, configured emulator launch, managed machine-mode run sessions, DevTools process integration, immutable Build/Clean and tooling commands, and Analyze/Test protocol parsers.
- `flutter-canvas-runner` — versioned Flutter/Windows sources for the isolated
  native Canvas child process, its bounded read-only model protocol and stable-ID
  selection bridge, optional exact widget-move destination projection, packaged
  with the plugin.
- `netbeans-plugin` — NetBeans UI integration and actions.
- `netbeans-runtime-it` — assembled NetBeans 30 gates for the packaged module, persisted SDK settings, Flutter project lifecycle, Dart MIME/editor registrations, Flutter actions, and optional real-SDK editor behavior.
- `flutter-designer` — NetBeans-independent `.fd` schema, model, validation,
  generation, command, persistence-planning and bounded read-only Canvas payload
  boundary for the Designer.

## Requirements

- Apache NetBeans IDE 31 for the current development runtime
- JDK 21+
- Maven 3.9+
- Flutter SDK installed separately

## Build

```bash
mvn clean install
```

The Java-only modules can also be worked on independently. The NetBeans module
continues to compile against the `RELEASE300` APIs.

For version highlights and installation instructions, see the [0.1.2 release notes](docs/RELEASE_NOTES_0.1.2.md). The complete release history is in [CHANGELOG.md](CHANGELOG.md).

## Configure Flutter and Dart

The plugin adds a dedicated `Flutter` category to `Tools > Options`. On its first start it imports a valid Flutter SDK from `flutter.sdk`, `FLUTTER_HOME`, `FLUTTER_ROOT`, or `PATH`. For Dart it respects `dart.sdk`, `DART_HOME`, and `DART_SDK`, then uses Flutter's bundled Dart SDK when available, and finally checks `PATH`.

If discovery finds nothing, open `Tools > Options > Flutter`, select the SDK folders manually, and validate them before applying the settings. `Tools > Check Flutter and Dart SDKs` uses the same saved configuration.

## Create or open a Flutter project

- Choose `File > New Project > Flutter > Flutter Application` to generate the standard base application with the configured SDK. Its `Target Platforms` step supports Android, iOS, Web, Windows, macOS, and Linux with Recommended, Mobile, Desktop, Web, All, and custom selections. The wizard passes the non-empty selection to `flutter create --template app --platforms=...`, verifies the requested platform directories, adds the project-wide Light/Dark theme catalog described below, opens the generated project, and selects `lib/main.dart`.
- Choose `File > Open Project` and select any existing Flutter directory containing `pubspec.yaml` and `lib`. It opens as a native NetBeans project with Flutter identity, a logical file tree, Dart source groups, and standard project operations.
- To extend an existing Flutter application (`project_type: app`), choose `Flutter > Add Flutter Platforms...` or the same command in the project's context menu. Only canonical paths that are completely absent can be selected; an existing directory, file, or symbolic link is treated as occupied and is never overwritten. The operation uses the configured Flutter SDK, native Output and progress, then verifies that Flutter created every selected real directory. Flutter module, package, plugin, and unknown project types are rejected with a concrete reason.
- In an open Flutter project choose `File > New File > Dart > Dart Class`. The wizard uses the selected project folder, converts an UpperCamelCase name such as `OrderRepository` to `order_repository.dart`, writes the class and opens it in the Dart editor.
- Choose `File > New File > Flutter Designer > Flutter Designer Form` to create
  a complete Designer pair. The target is restricted to `lib` or one of its
  subfolders. A Dart target such as `lib/account/profile.dart` is paired with
  the JSON model `.fd_templates/account/profile.fd`; the current schema-v13
  `source.dartFile` value remains the Dart basename `profile.dart`. Schema v5
  added structured `AlignmentGeometry`, `BoxConstraints`, `Matrix4` and
  `BoxDecoration` values for `Container`; schema v6 adds the shared asset-only
  `ImageProviderValue` and complete typed `DecorationImage`; schema v7 represents
  positive infinity in every BoxConstraints bound as JSON `null`; schema v8
  adds the atomic finite non-negative `Size` value; schema v9 adds the atomic
  finite signed `Offset` value; schema v10 adds the exact payload-free `null`
  property value used by `IndexedStack.index`; schema v11 adds a top-level
  typed `BorderRadiusGeometry` value for `ClipRRect.borderRadius`; schema v12
  adds the closed Dart-object reference used by the clipping widgets' `clipper` rows
  and `ClipPath.shape`, limited to
  a current-library or canonical `package:` root symbol declared by the
  project's `.dart_tool/package_config.json`, optional member and
  reference or zero-argument invocation. Schema v4's closed nullable `IconData`
  remains supported. Schema v13 adds the structured ShapeBorderClipper value.
  Schema v1-v12 forms migrate in memory and become canonical v13 only
  after an admitted edit. The model stores a safe
  logical app/package asset identity, never image bytes, a filesystem path or
  an executable Dart expression.
- Dart sources and Flutter Designer `.fd` models use distinct theme-aware file
  icons in Projects, Files, and the corresponding New File wizard entries.
- NetBeans `Delete` is available on either member of a complete Designer pair
  and removes both the `lib/.../*.dart` source and mirrored `.fd_templates/.../*.fd`
  model. The action closes a clean shared Designer/Source editor before touching
  disk; incomplete, unsafe, read-only or unsaved pairs fail closed.
- NetBeans `Rename` is also available from either member of a complete, clean
  current-version pair. A canonical lower-snake-case basename renames both
  mirrored files and updates only the current `.fd` `source.dartFile`; the Dart bytes and
  `source.className` are deliberately unchanged. The implementation stages both
  paths, writes and verifies the canonical model metadata, and exact-byte rolls
  back a failed operation. This is an in-process rollback guarantee rather than
  a durable crash-recovery journal.
- NetBeans `Copy`/`Paste` is available from either physical member of a complete,
  clean pair. Paste currently duplicates the Dart and `.fd` files only inside
  that pair's existing mirrored relative folder, choosing a jointly free
  `_copy`, `_copy_2`, ... basename across both trees. The Dart bytes remain
  exact; the canonical duplicate model receives a new `documentId` and changes
  only `source.dartFile`. A clean open Source/Designer editor remains open.
  Clipboard transfer uses the pair-aware NetBeans node flavor only, without a
  one-file loader flavor or operating-system file-list flavor. Cross-directory
  Copy awaits defined relative-URI rebasing semantics.
- NetBeans `Cut`/`Paste` moves a complete clean Designer pair between already
  existing mirrored folders in the same Flutter project. The basename and the
  exact Dart/`.fd` bytes are unchanged. A successful Paste consumes the
  pair-only clipboard transfer, closes a clean source editor, retires the old
  path-bound DataObjects and creates fresh owners at the destination. Missing
  mirrored folders, collisions, read-only or linked paths, unsaved files and
  Dart directives whose binding could change all fail closed. The final proof
  and commit share an EDT admission under exact NetBeans 30 MasterFS file locks
  and folder child-cache mutexes; another runtime shape is rejected rather than
  guessed. Generic DataObject Move remains unavailable. Schema-v1 asset paths are relative to the
  Flutter project/pubspec root, never to the `.fd` location, and opaque
  `extensions` metadata must remain location-independent.

## Project-wide Flutter themes

New Flutter applications contain one shared theme source at
`.fd_templates/project.fdtheme` and its deterministic generated Dart API at
`lib/theme/app_theme.dart`. The defaults are Light and Dark Material seed
themes with `ThemeMode.system`; `lib/main.dart` is wired to `AppTheme.light`,
`AppTheme.dark` and `AppTheme.mode`. Individual `.fd` files do not duplicate
theme definitions, so every Designer form and runtime screen can consume the
same project catalog.

Choose `Flutter > Edit Flutter Themes...` (also available from a Flutter
project's context menu), or open `project.fdtheme`. NetBeans opens the docked
`Themes` tab beside `Palette`, with explicit Save and Reload actions. The editor
changes the default mode and active light/dark definitions, and can add,
duplicate, edit, enable, disable or remove custom definitions. A disabled
definition remains in the descriptor catalog but is omitted from the generated
Dart map. `Enable project themes` can make `MaterialApp` use Flutter's defaults
without deleting the catalog; enabling it again restores the selected project
definitions. The editor's `General`, `Colors`, `Typography` and `Components`
tabs expose all
46 supported non-deprecated Material `ColorScheme` roles and all 15 Material 3
`TextTheme` roles. Each text role has 13 typed optional fields for colors,
font metrics/family, weight/style and decoration; omission means inherit the
seed-derived Material value, while text colors may use either an exact ARGB
literal or another semantic `ColorScheme` role. The compact Components editor
adds exactly 36 typed color leaves for Scaffold, AppBar, Icon and
ElevatedButton states with the same literal/semantic/inherit modes.

Schema v2 adds the portable project-wide switch, schema v3 adds per-definition
switches, schema v4 adds typed color and typography role overrides, and the
current schema v5 adds the closed component-color table. Schema v1-v3
descriptors remain readable and acquire empty override tables; schema v4
acquires empty components in memory. An explicit Save writes canonical v5.
Generated Dart applies
the same ordered `ColorScheme.copyWith` and `TextTheme.copyWith` construction as
the native Canvas, then applies component themes before local widget values.
The generated Dart file is marked as generated and guarded
by the SHA-256 stored in the descriptor; if it was edited outside the theme
editor, Save reports the conflict and leaves those bytes untouched.

Opening an older project does not create or rewrite theme files automatically.
Invoking the editor offers explicit default-theme initialization only when the
existing `lib/main.dart` matches the safely recognized Flutter application
template; unsupported or occupied paths fail without partial writes. The file
formats are documented by the frozen
[`project-theme-v1.schema.json`](docs/flutter-designer/project-theme-v1.schema.json),
[`project-theme-v2.schema.json`](docs/flutter-designer/project-theme-v2.schema.json),
[`project-theme-v3.schema.json`](docs/flutter-designer/project-theme-v3.schema.json)
and [`project-theme-v4.schema.json`](docs/flutter-designer/project-theme-v4.schema.json)
contracts, plus the current
[`project-theme-v5.schema.json`](docs/flutter-designer/project-theme-v5.schema.json).

## Run and debug a Flutter application

Flutter execution actions are available from the top-level `Flutter` menu and from a Flutter project's context menu.

1. Make a Flutter application active (or set it as the main project). NetBeans' standard configuration selector in the Run toolbar is populated asynchronously from `flutter devices --machine`, but exposes only targets whose Android, iOS, Web, Windows, macOS, or Linux platform directory is actually configured in that project. Run and Debug revalidate the match immediately before launch. The open project refreshes the cached list automatically on a five-second fixed delay, while a successful `Add Flutter Platforms...` refreshes it immediately; transient failures use a bounded 2/5/15/30-second backoff without blocking the UI or discarding the last good list. Choosing an entry remembers the target per project. `Flutter > Select Run Target...` remains available for an immediate refresh and the detailed target dialog.
2. For Android, open `Flutter > Device Manager`. It discovers the Android SDK from `android.sdk`, `ANDROID_SDK_ROOT`, `ANDROID_HOME`, Flutter configuration, or platform defaults; lists exact ADB devices and AVD states; and provides Create, Start, Stop, Restart, Wipe Data, Delete, Refresh, and Select Target. Create uses only installed stable-channel system images and never accepts licenses implicitly. Start, Restart, and Wipe wait for the exact AVD to boot and then select its exact ADB serial in the Flutter toolbar. Wipe and Delete require explicit confirmation. Closing the window or cancelling the boot wait does not terminate an emulator that has already started.
3. `Flutter > Launch Mobile Emulator...` is available only when the active application contains Android or iOS platform scaffolding. It starts an already configured definition, waits for a uniquely matching new Flutter device, and selects it. The operation has a native cancellable NetBeans progress indicator with Loading, Checking devices, Launching, Waiting, Selecting, and Ready phases.
4. The standard NetBeans `Build Project`, `Clean Project`, and `Clean and Build Project` commands are available from the project context menu and main Run menu. Build captures the selected toolbar target and creates its release artifact with `flutter build windows|linux|macos|web|apk|ios`; Android produces an APK. Clean runs `flutter clean`. Clean and Build validates the target first, then runs Clean and Build sequentially as one cancellable NetBeans action and does not build after a failed or cancelled Clean.
5. Choose `Run Flutter Project` or `Debug Flutter Project`. Run uses a managed `flutter run --machine` session. Debug starts the app paused, waits for its VM service, starts Flutter's debug adapter, and attaches the NetBeans DAP debugger. NetBeans shows native progress for the complete session, including its Starting, Running, and Stopping phases; Cancel in the progress indicator requests an orderly Stop.
6. Invoking Run or Debug again while the current application is Starting or Running asks whether to stop that session and restart in the requested mode and toolbar target. Declining leaves the current session untouched. Changing the toolbar target does not move an already running session; it selects the destination for the next Run, Debug, Build, or confirmed restart.
7. While the app is running, use `Hot Reload`, `Hot Restart`, or `Stop Flutter Application`. Flutter logs, lifecycle messages, emulator progress, and debugger diagnostics are written to a named NetBeans Output tab.
8. After the running application publishes its VM Service URI, choose `Flutter > Open DevTools`. The plugin starts DevTools with the configured Dart SDK on `127.0.0.1` and an automatically assigned port, connects it to that exact application, and opens the resulting URL in the browser configured in NetBeans. Choosing Open again reopens the current URL instead of starting a duplicate server. `Flutter > Stop DevTools` stops only the DevTools server; stopping or replacing the Flutter session, or closing the project, also stops its server automatically.

This integration launches the SDK-provided browser DevTools with native NetBeans actions, Output, and cancellable progress. An embedded DevTools surface and Flutter Inspector/widget-tree UI are separate future work.

Dart files are registered as `text/x-dart` with an incremental lexer, theme-aware syntax categories, a NetBeans EditorKit, a Fonts & Colors preview, and lexer-aware two-space typing indentation. The lexer handles Dart keywords, built-in types, numbers, nested comments, raw and triple strings, interpolation, malformed input recovery, and Unicode identifiers. Enter between `{}` expands an indented body and a leading `}` is aligned without treating delimiters inside strings or comments as code. Typing support reads the live incremental token hierarchy and affected line text instead of copying and re-lexing the whole document for each keystroke.

Opening a Dart source lazily starts the configured SDK's `dart language-server --protocol=lsp` through the NetBeans 30 LSP client; the connection uses the `dart` language id and the Flutter project root, keeps stderr outside the protocol stream, and is stopped when the project closes. NetBeans' standard editor UI consumes diagnostics, completion, definition, references, rename, document/range formatting, Quick Fixes, and source actions. Completion can add an import when Dart returns it as resolved `additionalTextEdits`; missing-import diagnostics offer the SDK fix, and Organize Imports applies the server-provided workspace edit. A narrow NetBeans 30 compatibility bridge preserves the exact original completion `data` and `textEdit` in a private Base64 envelope, temporarily hides the top-level `textEdit`, and restores both before forwarding `completionItem/resolve`; Dart then adds `additionalTextEdits` rather than reconstructing the main edit. NetBeans 30's standard `CompletionProviderImpl` does not execute `resolved.command`, so part-file and other multi-file imports returned in that form require the diagnostic Quick Fix. The status bar reports process start or restart; a deduplicated project-specific notification names a startup failure and opens `Tools > Options > Flutter`. Headless adapter tests, a real-Dart-SDK protocol test, and an optional assembled-runtime editor E2E gate cover the supported paths.

DAP breakpoints, stepping, and variables are available in debug sessions. The NetBeans 30 DAP bridge normalizes omitted or unsupported output categories to the protocol's `console` default and declares Debug ready only after successful `attach` and `configurationDone` responses plus Flutter's `flutter.appStarted` event. A bounded attach watchdog terminates an adapter that cannot complete that handshake.

## Packages, analysis, tests, and pubspec

The standard NetBeans project actions provide Build, Clean and Clean and Build, while the top-level `Flutter` menu and the Flutter project context menu provide `Flutter Pub Get`, `Flutter Analyze`, and `Flutter Test`. The same test support is exposed through NetBeans' standard project `Test` and `Test Single` commands. The top menu also provides `Test Current Dart File` and `Test at Caret` for a literal `test(...)` or `testWidgets(...)` declaration.

These one-shot processes use NetBeans' execution infrastructure rather than a hidden buffered CLI call. Each command gets a named Output tab, native progress, and a Stop control that terminates the process tree. Only one one-shot tooling command runs per Flutter project at a time, and closing the project cancels its active command without blocking the UI.

`Flutter Analyze` runs without an implicit package download and converts reported Dart locations into clickable Output links. `Flutter Test` consumes the public newline-JSON reporter protocol, preserves interleaved suite/test events, maps passed, failed, errored, skipped, and aborted tests into the standard NetBeans Test Results model, and supports rerun from the completed session. The adapter uses NetBeans 30's public `CoreManager` boundary; friend-only Test Results UI classes are intentionally not linked by the plugin.

For `pubspec.yaml`, NetBeans' bundled YAML editor continues to own YAML syntax support. The plugin adds Ctrl+Space completion for pub/Dart/Flutter keys, SDK dependencies, and bounded local-package discovery. Semantic diagnostics cover required package/SDK fields, section types, conflicting dependency sources, Flutter option types, and missing local dependency or asset paths. Other YAML files are not affected.

## Run the plugin in a development IDE

This repository builds a NetBeans plugin, not a standalone Java application. Build the plugin, assemble its development cluster, and then launch a separate NetBeans instance:

```powershell
mvn clean install
mvn nbm:cluster
mvn nbm:run-ide -Dnetbeans.installation=G:/netbeans
```

The command uses the installed Apache NetBeans IDE 31 runtime at `G:/netbeans`.
The development instance uses `target/userdir`, so it does not reuse the
settings of the NetBeans instance in which the project is open. The current
development package is `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
with the matching `netbeans-flutter-plugin-0.1.3-SNAPSHOT.jar` beside it.
The output base name is `netbeans-flutter-plugin`; the Maven artifactId and NetBeans
module identity are unchanged. The previously published stable 0.1.2 package retains
its historical name `netbeans-plugin/target/netbeans-plugin-0.1.2.nbm`.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

## Status

This is an architectural starter, not yet a production Flutter plugin. Flutter/Dart SDK settings, first-start discovery, platform-selective application creation and later platform addition, Dart-class creation, native project recognition, Dart lexer/highlighting and typing indentation, validated diagnostics/completion/import assistance/navigation/refactoring/formatting/Quick Fixes through the Dart LSP bridge, visible Analysis Server lifecycle, the automatically refreshed standard NetBeans toolbar selector for Desktop/Mobile/Web targets, target-aware Build/Clean/Clean and Build, Android Device Manager, cancellable configured-emulator launch, Run/Debug through Flutter's machine and DAP protocols, cancellable native progress, confirmed session restart, Hot Reload/Restart/Stop, project-scoped browser DevTools launch, native Pub Get/Analyze/Test execution, standard Test Results mapping, and `pubspec.yaml` completion/semantic diagnostics are implemented. Version 0.1.2 focused on lifecycle hardening and native NetBeans integration.

The unreleased 0.1.3 Designer now includes the first Windows native Canvas
slice. Each eligible `.fd` Design tab embeds an isolated real
`FlutterView` without PNG, screenshot or pixel-frame transport and publishes one
bounded validated protocol-v18 model restricted by the exact built-in capability
gate to `Scaffold`, `AppBar`, `ElevatedButton`, `TextField`, `Column`, `Row`,
`Wrap`, `Text`, `Icon`, `Image`, `Padding`, `Center`, `Align`, `FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`,
`SizedBox`, `AspectRatio`, `Stack`, `IndexedStack`, `Expanded`, `Flexible`, `Spacer`, `Baseline`,
`IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`, `ListBody`, `OverflowBar`, `SafeArea`, `ListView`, `GridView.count`, `SingleChildScrollView`, `ColoredBox`, `Placeholder`, `Directionality`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect`, `ClipPath`, `ClipRSuperellipse`, `PhysicalModel`, `PhysicalShape`, `RepaintBoundary`, `IgnorePointer`, `AbsorbPointer`, `ExcludeSemantics`, `BlockSemantics`, `MergeSemantics`, `IndexedSemantics`, `ExcludeFocus`, `ExcludeFocusTraversal`, `Visibility`, `TickerMode`, `DefaultTextHeightBehavior`, `DefaultSelectionStyle`, `IconTheme`, `ImageIcon`, `Divider`, `VerticalDivider`, `Card`, `Badge`, `CircleAvatar`, `LinearProgressIndicator`, `CircularProgressIndicator`, `RefreshProgressIndicator`, `RefreshIndicator`, `TextButton`, `OutlinedButton`, `FilledButton`, `FloatingActionButton`, `IconButton`, `Checkbox`, `Switch`, `Slider`, `RangeSlider`, `Radio`, `RadioGroup`, `ListTile`, `CheckboxListTile`, `Container`
and `Opacity`.
The toolbar now preserves exact Android Phone,
Android Tablet, iPhone, iPad, Windows Desktop, macOS Desktop and Linux Desktop
targets and carries each target into Flutter's adaptive theme semantics on the
bound Windows engine. These are appearance previews, not device runtimes. Web
uses the same native engine with an exact browser-sized responsive viewport;
it does not claim `kIsWeb`, browser fonts, DOM or plugin behavior. Stable widget
IDs synchronize selection between the Canvas, the revision-bound Explorer widget
tree and standard Properties. The eighty-eight non-`Scaffold` widgets expose 3877 typed
read/write property rows. `AppBar` contributes 120 independently resettable
leaves across behavior, layout, colors/elevation, shape, icon themes, text
styles and system-UI overlay groups, plus exact `leading`, `title`, `actions`,
`flexibleSpace` and `bottom` slots. `Text` contributes 59 independently editable leaves in
seven sections; every optional leaf supports Restore Default, and generated
Dart and native Canvas assemble them identically into `TextStyle`,
`StrutStyle`, `Locale`, `TextScaler` and `TextHeightBehavior`. Text colors,
paints and shadows may use either exact ARGB values or semantic Material
`ColorScheme` roles; a `TextTheme` role can be selected as the base style and
all explicit leaves remain local overrides. Structured editors cover the safe
serializable `Paint` subset, ordered `Shadow` values, OpenType `FontFeature`
tags and `FontVariation` axes. The deprecated `Text.textScaleFactor` argument,
`key`, arbitrary Dart expressions and unsupported shader/filter object graphs
remain outside this slice. `Icon` contributes its typed positional `icon`
value plus all 12 supported named constructor properties. Its searchable
bundled Material Icons catalog contains 8,825 entries locked to Flutter 3.44.8;
the built-in chooser admits only **None** or one exact registry glyph. The
Properties value cell and chooser rows display that glyph beside its readable
name by asynchronously loading the exact manifest-verified Material font from
the currently resolved Flutter SDK; there is no system-font fallback, and unavailable
preview bytes leave the text selector usable with a neutral placeholder. Generated
applications must keep `flutter.uses-material-design: true`
so those glyphs are available at runtime. `Icon` is a leaf; its omitted
theme-backed fields inherit from `IconTheme`, while `blendMode` and `fontWeight`
remain direct local arguments. Generated Dart and the native Canvas have exact
argument parity. The active Design lookup supplies the standard NetBeans Palette
with the exact eighty-nine widgets listed above. `ElevatedButton` adds 286 typed leaves: seven direct
behavior/callback fields, five 54-leaf state groups for default, disabled,
pressed, hovered and focused values, and nine common layout/feedback fields.
Its callbacks store strict Dart identifiers only—never arbitrary expressions.
Generated Dart uses a direct sparse `ButtonStyle`; scalar state leaves use
disabled/pressed/hovered/focused/default precedence, while compound leaves are
combined independently from default through active focused, hovered and pressed
fragments. Disabled styling is isolated from enabled fragments. Omitted leaves
fall through to the non-null `ElevatedButtonTheme` value as one atomic object,
then Flutter defaults. When neither source supplies `fixedSize`, an omitted
axis uses Flutter's infinity sentinel; a finite effective maximum on that axis
clamps the sentinel exactly as `ButtonStyleButton` does. Local minimum and
maximum constraints resolve as one state-aware pair; each maximum axis widens
to its effective minimum, while states without local bounds keep Flutter's
theme/default fallback. Local Text
font family, fallback and package leaves follow the same active-state layering.
Each package applies to the current layered `TextStyle`, including TextTheme;
fallback-only package overrides replace one prior package prefix and never
synthesize a `.../null` family. Local Text theme/inherit configuration is admitted only
with one explicit transition-safe inherit mode across reachable states. The Canvas
receives only `callbackPresence`, never callback identifiers, and installs inert
typed closures that cannot execute project handlers. The state projections
cover all `SystemMouseCursors`, six closed shape presets (`roundedRectangle`,
`roundedSuperellipse`, `stadium`, `circle`, `beveledRectangle` and
`continuousRectangle`) and four splash presets (`InkSplash`, `InkRipple`,
`InkSparkle` and `NoSplash`). Effective text color is owned by
`ButtonStyle.foregroundColor`, so `TextStyle.color` is intentionally excluded;
`ButtonStyle.iconAlignment` is also intentionally excluded from this
arbitrary-child slice: pinned Flutter 3.44.8 reads it only from the private
icon/label child created by `ElevatedButton.icon`, while the ordinary
`ElevatedButton` constructor passes its required `child` through unchanged.
runtime-only keys, focus/state controllers and builders remain outside the
serializable contract. `Container` adds all 13 reviewed non-widget constructor
properties: `alignment`, `padding`, `color`, `isAntiAlias`, `decoration`,
`foregroundDecoration`, `width`, `height`, `constraints`, `margin`, `transform`,
`transformAlignment` and `clipBehavior`. Its structured Properties editors
preserve physical/directional alignment and radii, normalized constraints,
column-major Matrix4 storage, theme-aware decoration colors, exact borders,
ordered shadows and linear/radial/sweep gradients. `color` is mutually exclusive
with `decoration`, non-`none` clipping requires a decoration, and related changes
are committed atomically. Its typed `DecorationImage` covers the pinned 13 SDK
arguments—`image`, `onError`, `colorFilter`, `fit`, `alignment`, `centerSlice`,
`repeat`, `matchTextDirection`, `scale`, `opacity`, `filterQuality`,
`invertColors` and `isAntiAlias`—including all five reviewed `ColorFilter`
variants. `centerSlice` requires a positive-area non-negative rectangle and
permits fit omitted, `fill`, `contain`, `fitWidth`, `fitHeight` or `scaleDown`;
`cover` and `none` are rejected. `onError` is a validated two-argument callback
identifier, never callback source or raw Dart.

The provider matrix is deliberately closed: `AssetImage` or `ExactAssetImage`,
optionally wrapped once by bounded `ResizeImage`; `FileImage`, `MemoryImage`,
`NetworkImage` and custom user-selectable providers remain deferred. A reserved
unresolved ASSET identity is designer state, not a third provider choice; Canvas
and generated Dart map only that state to reviewed built-in placeholder bytes.
The IDE selects only
declared app/package assets discovered from `pubspec.yaml` and
`.dart_tool/package_config.json`, verifies PNG/JPEG/GIF/WebP bytes and their
dimensions, rejects absolute/backslash/traversal/symlink-escape paths and applies
the exact Flutter 3.44.8 DPR algorithm pinned to framework revision
`058e0af2c2b57e369d905a03ac9748b0ebf543c6`.

Canvas model protocol v18 over NBFC framing v1 negotiates
`asset.imageBytes.v1` and sends referenced,
revision-scoped compressed resources only, each addressed by the lowercase raw
SHA-256 of its immutable bytes and checked against exact descriptor size, digest
and order. No filesystem path or callback identifier crosses that boundary.
Native and the internal exact-Web runtime build the same real `DecorationImage`
from an internal `MemoryImage(bytes, scale: resolvedScale)` plus at most one `ResizeImage`;
their `centerSlice` admission mirrors the pinned codecs: native exact resize
uses Flutter's asymmetric missing-axis derivation and honors explicit upscale,
whereas Web rounds either missing axis and returns the intrinsic image whenever
the derived target would upscale. A `fit` target that collapses an axis to zero
is rejected rather than treated as a synthetic one-pixel decode.
Authenticated media/decode/resize/center-slice failures quarantine only the
affected resource; framing, identity, digest, ordering and exact model-resource
coverage failures remain fatal. Unresolved or quarantined assets render a
deterministic non-interactive placeholder with the
logical identity, code and reason. Selection/layout frames, guides and drop zones remain outside
the decorated/transformed `Container`. The Image tab exposes typed accessible
controls and inventory status, and one accepted structured/dependent edit is
one Undo/Redo unit. The optional `child` remains a named single any-widget slot
rather than a property row. At that historical milestone the catalog exposed exactly 3894
writable rows across eighty-nine widgets, including 3877 across the eighty-eight
non-`Scaffold` definitions; eighty-two definitions use reviewed const constructors.
`.fd` is v13 and the Canvas model protocol is 18. SafeArea's physical-insets
constraint adds the exported `EdgeInsetsValues.directionalAllowed` component,
and `IndexedStack.index` adds the exact payload-free null value; the top-level
typed border-radius geometry used by `ClipRRect` established contributor Catalog
API 11, and its typed project Dart-object reference establishes API 12. Exact-Web product selection remains
separately gated; the currently routed Web choice is the native-engine layout
preview.

`Opacity` is a supported complete vertical slice. Its exact Flutter 3.44.8
contract is the canonical `flutter.widgets.Opacity` const constructor from
`package:flutter/widgets.dart`: required named finite `double opacity` in the
inclusive range `[0, 1]`, optional named `bool alwaysIncludeSemantics` whose
omitted Flutter default is `false`, and one optional single any-widget `child`.
`key` is excluded. A new prototype stores only `opacity: 1.0` and an empty
`child`; omission of `alwaysIncludeSemantics` remains distinct from an explicit
value and generates no argument. The real native and exact-Web projections use
Flutter `Opacity`, not a paint approximation or `AnimatedOpacity`. Zero opacity
does not disable hit testing. It normally removes child semantics, while
`alwaysIncludeSemantics: true` retains them. IDE-owned selection, hit and drop
overlays remain outside the effect, including the zero-size empty-child target.
No Theme or Directionality input is added. This uses the existing double,
boolean and single-slot encodings, so `.fd` schema v6, Catalog API 5, Canvas
model v11, NBFC framing v1 and control/wire v1 remain unchanged.

`Align` is a supported complete vertical slice. Its exact Flutter 3.44.8
contract is the canonical const `flutter.widgets.Align` constructor from
`package:flutter/widgets.dart`: optional `AlignmentGeometry alignment` with
omitted `Alignment.center` default, optional finite non-negative `widthFactor`
and `heightFactor`, and one optional single any-widget `child`. New prototypes
store no properties; omission of either factor remains distinct from explicit
`1`, because a null factor expands a bounded axis. Physical alignment is
direction-independent, while directional alignment resolves against LTR/RTL;
coordinates may extrapolate outside `[-1, 1]`. Native and exact-Web projections
build real Flutter `Align`, and an empty factor-driven zero-size widget retains
an IDE-only selection/drop target. Existing alignment, numeric and single-slot
encodings keep all protocol versions unchanged.

`FractionallySizedBox` adds the corresponding const fractional-layout contract:
optional physical/directional alignment, optional finite non-negative width and
height factors, and one optional any-widget child. `Stack` adds four optional
layout leaves and an ordered list of non-positioned children; its clip value is
passed to Flutter exactly, while extrapolated non-positioned alignment and
descendant paint-only overflow do not become RenderStack visual overflow and are
not clipped. `Expanded` adds optional non-negative `flex` and a required child;
Palette creation atomically wraps an existing direct Row/Column child and never
creates a terminal placeholder. Its child editor is replacement-only.

`Image` is a const leaf with 22 reviewed properties. Its required asset-only
provider is initialized from the deterministic first sorted declared project
asset when one is available. When the inventory is empty, refreshing, verifying
or temporarily unavailable, Palette/tree/Canvas insertion still creates the
`Image` in a compatible slot with an explicit editable placeholder. The
Properties value is shown as `<choose asset>`; Canvas and generated Dart render
a small built-in placeholder without referring to a nonexistent project file.
After adding a PNG/JPEG/GIF/WebP file such as `assets/example.png`, declare it in
the existing `flutter:` block and choose it in **Image data**:

```yaml
flutter:
  uses-material-design: true
  assets:
    - assets/example.png
```

Platform launcher resources such as `android/.../ic_launcher.png`, Apple
`Assets.xcassets` entries and `web/icons/...` are not Flutter runtime assets for
this widget. The unresolved value is persisted as a reserved Designer sentinel,
so Save/reopen keeps the widget valid and its image provider editable; it is
never emitted as an `AssetImage` path. The planner rechecks the latest inventory
at commit: it selects the first current asset or retains the placeholder if none
is usable.
The four center-slice coordinates are all-or-none, strictly ordered and
incompatible with `BoxFit.cover`/`none`.

`TextField` is a const Material leaf named **Text Field** in the Palette. Its 54
optional rows are grouped as Input (14), Layout (9), Behavior (11), Cursor and
selection (11), Callbacks (8) and Restoration (1), with no creation dialog or
stored constructor defaults. Designer persists only reviewed constructor intent,
not typed text, selection, controller or focus state. Cursor-radius and
scroll-padding edits are atomic pair/quartet operations. Generated Dart and the
real non-interactive Canvas TextField use a `LayoutBuilder`/`SizedBox` guard:
unbounded width receives 240 logical pixels and an expanding field under
unbounded height receives 120.

`ListView` completed the originally agreed core Palette as a non-const static
`ListView(children: ...)` slice. That initial slice exposed 17 optional constructor-intent rows:
scroll axis/direction, primary-controller policy, six reviewed physics presets,
shrink-wrap, padding, fixed item extent, child lifecycle/repaint/semantic-index
flags, pixel cache extent, semantic child count, drag and keyboard behavior,
restoration ID, clipping and hit testing. Its ordered `children` slot is the
nineteenth any-widget destination. `semanticChildCount` cannot exceed the static
child count; numeric cache extent emits `ScrollCacheExtent.pixels`. Controller,
builders/delegates, `itemExtentBuilder`, `prototypeItem`, deprecated
`cacheExtent`, `key` and raw Dart were excluded from that initial slice. Generated Dart and Canvas build
a real ListView and preserve vertical/horizontal, reverse and LTR/RTL insertion
geometry. Their shared `LayoutBuilder`/`SizedBox` guard supplies width 240 or
height 120 whenever the viewport cross axis is unbounded, and supplies the same
fallback on an unbounded main axis only when `shrinkWrap` is false. The
originally agreed eight-item core list—`Container`,
`Row`, `Column`, `Text`, `Image`, Button through `ElevatedButton`, `TextField`
and `ListView`—is therefore complete 8/8; this does not mean that every Flutter
widget is implemented.

**Current G5 extension:** ListView now has **18 optional property rows**.
`itemExtentBuilder` is appended under Properties → Layout at argument order 18;
the original property orders and the `children` slot at order 11 are unchanged.
It accepts omission, explicit null and strict `ItemExtentBuilder?` project
references/getters/members/zero-argument factories, including nullable callbacks.
Any builder reference conflicts with fixed `itemExtent`; explicit null does not,
and neither value is silently cleared. The callback must return a valid extent
for every actual child; a null result is only for an out-of-range index, not
default sizing or truncation. Canvas never executes project builders and
explicitly approximates custom references with normal SDK child sizing.
Dynamic constructors/delegates, `prototypeItem` and other initial exclusions
remain outside this extension. See the [G5 contract and verification](docs/LIST_VIEW_ITEM_EXTENT_BUILDER.md).

`Wrap` is the first post-core Palette slice. The const default constructor
exposes all nine non-`key` arguments: axis, child/run alignment, finite
`spacing` and `runSpacing` (including negative values), cross-axis alignment,
text and vertical direction, and clipping. Its ordered `children` list is the
twentieth any-widget destination. Generated Dart and Canvas construct the real
Flutter Wrap and preserve framework run layout. An empty Wrap retains an
IDE-only 36-pixel selection target; both empty and populated Wrap nodes use the
full rendered rectangle as a terminal append zone because wrapped runs do not
have one stable terminal edge.

`FittedBox` is the second post-core Palette slice. Its const default constructor
exposes optional `BoxFit fit`, physical/directional `AlignmentGeometry alignment`
and `Clip clipBehavior`, plus one optional single any-widget `child`. Omission
preserves Flutter's `BoxFit.contain`, `Alignment.center` and `Clip.none`
defaults. Generated Dart and Canvas construct the real Flutter FittedBox: Flutter
owns scaling, directional alignment resolves through LTR/RTL, and clipping is
applied only through the selected clip behavior. An empty zero-size FittedBox
retains an IDE-only 36-pixel selection/drop target without changing generated
Dart or Flutter layout. Properties, Palette/tree/Canvas DnD, slot editing,
same-tree movement, Save/reopen, Undo/Redo and the four reviewed light/dark SVG
icon variants use the same closed catalog contract. At that milestone the
practical Material/Base Designer backlog was 22/92 complete.

`ConstrainedBox` is the third complete post-core Palette slice. Its pinned
Flutter 3.44.8 constructor is non-const, requires one typed
`BoxConstraints constraints` value and accepts one optional single any-widget `child`. The
constraints editor represents finite, unbounded and expanding width/height axes:
each finite minimum must not exceed its maximum, positive infinity is valid for
an upper bound, and an infinite minimum is valid only with an infinite maximum.
New instances store the neutral `0..infinity` constraint on both axes. Generated
Dart and both Canvas projections construct the real Flutter ConstrainedBox, so
the framework combines the additional constraints with the incoming parent
constraints. An empty zero-size instance retains a non-layout-affecting Designer
selection/drop target. Properties, creation, Palette/tree/Canvas DnD, exact-slot
editing, same-tree movement, Save/reopen, Undo/Redo and reviewed SVG identity
share the same closed contract. At that milestone the practical backlog was
23/92 complete with 69 remaining, and the Layout category contained 15 items.

[`UnconstrainedBox`](https://api.flutter.dev/flutter/widgets/UnconstrainedBox/UnconstrainedBox.html)
is the fourth complete post-core Palette slice and occupies Layout order 107,
immediately after ConstrainedBox. Its pinned Flutter 3.44.8 const constructor
exposes optional `textDirection`, `alignment`, `constrainedAxis` and
`clipBehavior` arguments plus one optional single any-widget `child`. New
instances persist no property defaults: omission preserves centered alignment,
no retained constrained axis and `Clip.none`; omitted `textDirection` uses the
ambient `Directionality` when directional alignment needs resolution. Generated
Dart and both Canvas projections construct the real Flutter UnconstrainedBox,
so Flutter removes both incoming axes or retains exactly the selected horizontal
or vertical axis. Empty zero-size instances retain only the bounded,
non-layout-affecting Designer selection/drop target. Properties, creation,
Palette/tree/Canvas DnD, exact-slot editing, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 24/92 complete with 68 remaining,
and Layout contained 16 items.

[`LimitedBox`](https://api.flutter.dev/flutter/widgets/LimitedBox/LimitedBox.html)
is the fifth complete post-core Palette slice and occupies Layout order 108,
between UnconstrainedBox and Stack. Its pinned Flutter 3.44.8 const constructor
exposes optional non-negative `maxWidth` and `maxHeight` arguments followed by
one optional single any-widget `child`. New instances persist no property
defaults. Finite values are emitted explicitly, while omission canonically
preserves Flutter's `double.infinity` default. Generated Dart and both Canvas
projections construct the real Flutter LimitedBox, so a limit applies only when
the incoming maximum constraint on that axis is unbounded. Properties, creation,
Palette/tree/Canvas DnD, exact-slot editing, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 25/92 complete with 67 remaining,
and Layout contained 17 items.

[`OverflowBox`](https://api.flutter.dev/flutter/widgets/OverflowBox/OverflowBox.html)
is the sixth complete post-core Palette slice and occupies Layout order 109,
between LimitedBox and Stack. Its pinned Flutter 3.44.8 const constructor
exposes optional `alignment`, `minWidth`, `maxWidth`, `minHeight`, `maxHeight`
and `fit` arguments followed by one optional single any-widget `child`. New
instances persist no property defaults. Omitted bounds inherit the corresponding
parent constraint; explicit overrides are finite non-negative doubles and each
present minimum must not exceed its matching maximum. Omitted alignment and fit
preserve `Alignment.center` and `OverflowBoxFit.max`; `deferToChild` is also
supported. Explicit non-finite overrides remain outside the reviewed bounded
contract. Generated Dart and both Canvas projections construct the real Flutter
OverflowBox, including physical/directional alignment and `max` versus
`deferToChild` sizing. Properties, creation, Palette/tree/Canvas DnD, exact-slot
editing, same-tree movement, Save/reopen, Undo/Redo and reviewed light/dark SVG
identity share the same closed contract. At that milestone the practical backlog
was 26/92 complete with 65 remaining, and Layout contained 18 items.

[`Flexible`](https://api.flutter.dev/flutter/widgets/Flexible/Flexible.html) is
the seventh complete post-core Palette slice and occupies Layout order 130,
immediately after Expanded. Its pinned Flutter 3.44.8 const constructor exposes
optional non-negative portable integer `flex`, optional `FlexFit.loose`/`tight`
and one required single any-widget `child`. New detached prototypes persist no
property defaults; omission preserves `flex: 1` and `FlexFit.loose`. Palette,
tree and Canvas creation atomically wrap an existing direct Row or Column child,
never create an empty terminal placeholder, and reject wrapping either Expanded
or Flexible because the inner parent-data widget would cease to be a direct Flex
child. Generated Dart and both Canvas projections construct the real Flutter
Flexible, including zero-flex inflexible layout and loose or tight positive-flex
allocation. Properties, required-child replacement, Palette/tree/Canvas DnD,
same-tree movement, Save/reopen, Undo/Redo and reviewed light/dark SVG identity
share the same closed contract. At that milestone the practical backlog was
27/92 complete with 65 remaining, and Layout contained 19 items.

[`Spacer`](https://api.flutter.dev/flutter/widgets/Spacer/Spacer.html) is the
eighth complete post-core Palette slice and occupies Layout order 140,
immediately after Flexible. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes
one optional positive portable integer `flex`; omission preserves Flutter's
default `1`, while zero, negative and over-limit values are rejected. Spacer has
no slots and is inserted only as a direct `Row.children` or `Column.children` leaf.
Expanded and Flexible cannot wrap Spacer because Spacer internally builds the
flex parent-data path that must remain directly below Row or Column. Generated
Dart emits the real Flutter Spacer. Both Canvas projections keep that real
Spacer directly under the Flex and provide Designer selection through the
surface overlay, never through an invalid render-object wrapper. Properties,
Palette/tree/Canvas insertion, same-tree movement, Save/reopen, Undo/Redo and
reviewed light/dark SVG identity share the same closed contract. At that
milestone the practical backlog was 28/92 complete with 64 remaining, and
Layout contained 20 items.

[`Baseline`](https://api.flutter.dev/flutter/widgets/Baseline/Baseline.html) is
the ninth complete post-core Palette slice and occupies Layout order 150,
immediately after Spacer. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes required finite-double `baseline`,
required `TextBaseline.alphabetic`/`ideographic` `baselineType`, and one
optional single any-widget `child`. Because Flutter provides no constructor
defaults, a detached Designer prototype stores the reviewed visible starting
values `baseline: 24.0` and `baselineType: TextBaseline.alphabetic`; both remain
fully editable, while NaN, infinity, wrong enum types and raw Dart fail closed.
Generated Dart and both Canvas projections construct the real Flutter Baseline.
A childless node retains framework `constraints.smallest` layout (often zero)
and receives only the non-layout-affecting Designer selection/drop target.
Properties, exact-slot
editing, Palette/tree/Canvas insertion, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 29/92 complete with 63 remaining,
and Layout contained 21 items. `.fd` remains v7, Catalog
API remains 6, Canvas model remains v12, and NBFC framing plus Canvas
control/wire remain version 1.

[`IntrinsicHeight`](https://api.flutter.dev/flutter/widgets/IntrinsicHeight/IntrinsicHeight.html)
is the tenth complete post-core Palette slice and occupies Layout order 160,
immediately after Baseline. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes no writable arguments and one optional
single any-widget `child`. Generated Dart and both Canvas projections construct
the real Flutter IntrinsicHeight, so Flutter performs the speculative intrinsic
height pass while still honoring the parent's constraints. Because that pass is
relatively expensive and can be O(N²) in tree depth, the Palette and slot
descriptions keep the performance warning visible. An empty or collapsed node
receives only a bounded, non-layout-affecting Designer selection/drop target.
Exact-slot editing, Palette/tree/Canvas insertion, same-tree movement,
Save/reopen, Undo/Redo and reviewed light/dark SVG identity share the same
closed contract. At that milestone the practical backlog was 30/92 complete
with 62 remaining, and Layout contained 22 items. `.fd` remains v7, Catalog API
remains 6, Canvas model remains v12, and NBFC framing plus Canvas control/wire
remain version 1.

[`IntrinsicWidth`](https://api.flutter.dev/flutter/widgets/IntrinsicWidth/IntrinsicWidth.html)
is the eleventh complete post-core Palette slice and occupies Layout order 170,
immediately after IntrinsicHeight. Its pinned Flutter 3.44.8 const constructor
from `package:flutter/widgets.dart` exposes optional `stepWidth` and
`stepHeight` doubles plus one optional single any-widget `child`. Both steps
must be finite and non-negative. Null and explicit zero remain distinct saved
values; Flutter treats either as no snapping on that axis, while a positive
step rounds the corresponding intrinsic child extent up to its next multiple.
Generated Dart and both Canvas projections construct the real IntrinsicWidth
under the parent's constraints. Palette and Properties descriptions expose its
relatively expensive speculative layout pass and worst-case O(N²) tree-depth
behavior. Empty or collapsed nodes receive only a bounded,
non-layout-affecting Designer selection/drop target. Property and exact-slot
editing, Palette/tree/Canvas insertion, same-tree movement, Save/reopen,
Undo/Redo and reviewed light/dark SVG identity share the same closed contract.
At that milestone the practical backlog was 31/92 complete with 61 remaining,
and Layout contained 23 items. `.fd` remains
v7, Catalog API remains 6, Canvas model remains v12, and NBFC framing plus
Canvas control/wire remain version 1.

[`Offstage`](https://api.flutter.dev/flutter/widgets/Offstage/Offstage.html) is
the twelfth complete post-core Palette slice and occupies Layout order 180,
immediately after IntrinsicWidth. Its pinned Flutter 3.44.8 const constructor
from `package:flutter/widgets.dart` exposes one optional boolean `offstage`
(default `true`) and one optional single any-widget `child`. Omission and
explicit `true` are distinct saved/history states even though both hide the
child; explicit `false` restores ordinary layout, painting, hit testing and
semantics. Generated Dart and both Canvas projections construct the real
Offstage. When hidden, Flutter still lays the child out and keeps it active and
focusable, including running animations, while suppressing paint, hit testing
and semantics and normally contributing no parent space. Properties and
Palette descriptions recommend removing a subtree for long-term hiding when
that ongoing work would waste resources. A real zero-sized result retains a
bounded 36x36, non-layout-affecting selection/drop target outside the Offstage
effect. Property and exact-slot editing, Palette/tree/Canvas insertion,
same-tree movement, Save/reopen, Undo/Redo and reviewed light/dark SVG identity
share the same closed contract. At that milestone the practical backlog was
32/92 complete with 60 remaining, and Layout contained 24 items. `.fd` remained
v7, Catalog API remained 6 and Canvas model remained v12; NBFC framing plus
Canvas control/wire remained version 1.

[`SizedOverflowBox`](https://api.flutter.dev/flutter/widgets/SizedOverflowBox/SizedOverflowBox.html)
is the thirteenth complete post-core Palette slice and occupies Layout order
190 immediately after Offstage. Its pinned Flutter 3.44.8 const constructor
from `package:flutter/widgets.dart` exposes the required structured `Size size`,
optional physical/directional `alignment` (default `Alignment.center`) and one
optional single any-widget `child`. A detached prototype stores the visible,
finite non-negative starting size `Size(100, 100)`; width and height remain one
atomic typed value in persistence, history and Properties. Generated Dart and
both Canvas projections construct the real SizedOverflowBox: its own size is
the requested size constrained by its parent, while its child receives the
original incoming constraints and may paint outside the box according to the
selected alignment. Flutter still clips hit testing to the parent's bounds.
A true zero-size result retains only the bounded 36x36 Designer selection/drop
target. Property and exact-slot editing, Palette/tree/Canvas insertion,
same-tree movement, Save/reopen and further editing, Undo/Redo and reviewed
light/dark SVG identity share the same closed contract. The practical backlog
was 33/92 complete with 59 remaining, and Layout contained 25 items. `.fd`
was v8, Catalog API was 7 and Canvas model was v13; NBFC framing plus Canvas
control/wire remained version 1.

[`Transform`](https://api.flutter.dev/flutter/widgets/Transform/Transform.html)
is the fourteenth complete post-core Palette slice and occupies Layout order
200 immediately after SizedOverflowBox, matching Flutter's canonical Layout
catalog. Its pinned Flutter 3.44.8 const `Transform.new` constructor from
`package:flutter/widgets.dart` exposes required structured `Matrix4 transform`,
optional signed finite `Offset origin`, optional physical/directional
`alignment`, optional `transformHitTests` (default `true`), optional
`FilterQuality.none/low/medium/high` and one optional single any-widget `child`.
A detached prototype stores only `Matrix4.identity()`; all optional values stay
absent so their framework defaults remain distinct from explicit values. The
new Offset editor changes finite signed `dx` and `dy` atomically. Generated Dart
and both Canvas projections construct the real paint-time Transform without
changing the child's layout size. Origin and alignment compose, optional
filtering is preserved, and child hit tests follow the transform only when
`transformHitTests` is true. Designer selection/drop geometry follows the same
effective transform without changing Flutter layout, with a bounded 36x36
Designer target only for a real zero-size node. Non-finite, projective-horizon,
and non-invertible surface geometry fails closed instead of inventing an AABB
hit region; finite rotated/skewed DnD uses exact local containment. Property and exact-slot editing,
Palette/tree/Canvas insertion, same-tree
movement, Save/reopen and further editing, Undo/Redo and reviewed light/dark SVG
identity share the same closed contract. The named `.rotate`, `.translate`,
`.scale` and `.flip` convenience constructors remain explicitly outside this
slice; equivalent matrices remain expressible through `Transform.new`. The
practical backlog is now 34/92 complete with 58 remaining, and Layout contains
26 items. `.fd` is v9, Catalog API is 8 and Canvas model is v14; NBFC framing
plus Canvas control/wire remain version 1.

[`RotatedBox`](https://api.flutter.dev/flutter/widgets/RotatedBox/RotatedBox.html)
is the fifteenth complete post-core Palette slice and occupies Layout order 210
immediately after Transform. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes required signed integer `quarterTurns`
and one optional single any-widget `child`. A detached prototype stores `1`,
making the clockwise layout-time quarter turn immediately visible. The accepted
range is the exact shared native/Web interval `-9007199254740991` through
`9007199254740991`; larger magnitudes fail closed before generation. Generated
Dart and both Canvas projections construct the real Flutter RotatedBox. Odd
turns exchange the child's layout axes, even turns retain them, and Flutter
paints the equivalent rotation modulo four while the Designer preserves the
exact stored integer. Property and exact-slot editing, Palette/tree/Canvas
insertion, same-tree movement, Save/reopen and further signed editing, Undo/Redo
and reviewed light/dark SVG identity share the same closed contract. The
practical backlog is now 35/92 complete with 57 remaining, and Layout contains
27 items. `.fd` remains v9, Catalog API remains 8 and Canvas model remains v14;
NBFC framing plus Canvas control/wire remain version 1.

[`ListBody`](https://api.flutter.dev/flutter/widgets/ListBody/ListBody.html)
is the sixteenth complete post-core Palette slice and occupies Layout order 220
immediately after RotatedBox. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes optional `mainAxis` and `reverse`, whose
omission preserves `Axis.vertical` and `false`, plus one ordered any-widget
`children` list. A detached prototype omits both properties and starts with an
empty list. Generated Dart emits the real bare `ListBody`, without a synthetic
wrapper. The native and exact-Web Canvas place that real widget in an
axis-matched design-time viewport so it receives the unbounded main-axis and
bounded cross-axis constraints required by `RenderListBody`; this preview guard
is neither saved nor generated. Empty nodes accept insertion index zero, while
populated terminal geometry follows the configured axis, reversal and ambient
text direction. Property and list-slot editing, Palette/tree/Canvas insertion,
same-tree movement/reordering, Save/reopen and further editing, Undo/Redo and
reviewed light/dark SVG identity share one closed contract. The practical
backlog is now 36/92 complete with 55 remaining, and Layout contains 28 items.
`.fd` remains v9, Catalog API remains 8 and Canvas model remains v14; NBFC
framing plus Canvas control/wire remain version 1.

[`OverflowBar`](https://api.flutter.dev/flutter/widgets/OverflowBar/OverflowBar.html)
is the seventeenth complete post-core Palette slice and occupies Layout order
230 immediately after ListBody. Its pinned Flutter 3.44.8 const constructor from
`package:flutter/widgets.dart` exposes optional finite signed `spacing`,
nullable `alignment`, finite signed `overflowSpacing`, `overflowAlignment`,
`overflowDirection`, nullable `textDirection`, and one ordered any-widget
`children` list. A detached prototype omits all six properties and starts with
an empty list, preserving Flutter's `0.0`, null, `0.0`, `start`, `down` and
ambient-direction defaults. Generated Dart emits the real bare `OverflowBar`.
For a nonempty node, the native and exact-Web Canvas bound an otherwise
unbounded preview width only when `alignment` is non-null; null alignment keeps
Flutter's natural width. They retain a 36x36 empty selection/drop target; those
presentation guards are neither saved nor generated. The real rendered mode selects DnD geometry:
effective LTR/RTL direction orders a fitting horizontal bar, while
`overflowDirection` orders a vertical overflow column. Property and list-slot
editing, Palette/tree/Canvas insertion, same-tree movement/reordering,
Save/reopen and further editing, Undo/Redo and reviewed light/dark SVG identity
share one closed contract. The practical backlog is now 37/92 complete with 55
remaining, and Layout contains 29 items. `.fd` remains v9, Catalog API remains
8 and Canvas model remains v14; NBFC framing plus Canvas control/wire remain
version 1.

[`GridView.count`](https://api.flutter.dev/flutter/widgets/GridView/GridView.count.html)
is the eighteenth complete post-core Palette slice and occupies Scrolling order
20 immediately after ListView. Its pinned Flutter 3.44.8 non-const named
constructor from `package:flutter/widgets.dart` exposes 21 typed rows plus one
ordered any-widget `children` list: scrolling axis, reversal, primary policy,
reviewed physics preset, shrink wrap, padding, required positive
`crossAxisCount`, main/cross spacing, child aspect ratio, optional main-axis
extent, child lifecycle flags, pixel cache extent, semantic child count, drag
and keyboard-dismiss behavior, restoration ID, clipping and hit testing. A new
node stores only `crossAxisCount: 2` and starts empty; all optional rows remain
unset so Flutter keeps its constructor defaults. Generated Dart and Canvas use
the real `GridView.count` behind the same 240-wide/120-high guard when required
by unbounded constraints; `mainAxisExtent`, when set, determines the tile main
extent instead of the aspect-ratio-derived value. Controller-owned state,
builders/delegates, other named constructors, `scrollBehavior`, `key` and the
deprecated raw `cacheExtent` are deliberately unsupported. At the
`GridView.count` milestone, the practical backlog was 38/92 complete with 54
remaining; Layout had 29 items and Scrolling had 2. `.fd` remained v9, Catalog
API remained 8 and Canvas model remained v14; NBFC framing plus Canvas
control/wire remained version 1.

[`SingleChildScrollView`](https://api.flutter.dev/flutter/widgets/SingleChildScrollView/SingleChildScrollView.html)
is the nineteenth complete post-core Palette slice and occupies Scrolling order
30 immediately after `GridView.count`. Its pinned Flutter 3.44.8 const
constructor from `package:flutter/widgets.dart` exposes 10 typed rows plus one
optional any-widget `child`: scrolling axis, reversal, padding, primary policy,
a reviewed physics preset, drag-start behavior, clipping, hit testing,
restoration ID and keyboard dismissal. A new node stores no explicit properties
and begins with an empty child slot, preserving Flutter's exact defaults.
Controller-owned state and arbitrary physics graphs are deliberately
unsupported. Generated Dart and Canvas construct the real
`SingleChildScrollView`. Because the widget deliberately shrink-wraps in both
axes, it does not receive the ListView/GridView generated viewport guard;
Canvas alone retains a non-layout-affecting 36x36 target when the real node is
empty or zero-size. The practical backlog is now 39/92 complete with 53
remaining. Layout remains at 29 items and Scrolling contains 3. `.fd` remains
v9, Catalog API remains 8 and Canvas model remains v14; NBFC framing plus Canvas
control/wire remain version 1.

[`ColoredBox`](https://api.flutter.dev/flutter/widgets/ColoredBox/ColoredBox.html)
is the twentieth complete post-core Palette slice and occupies Basic order 40
after `Image`. Its pinned Flutter 3.44.8 const constructor exposes required
`color`, optional `isAntiAlias` whose omitted default is `true`, and one optional
any-widget `child`; only the common `key` argument is excluded. A new node stores
the required literal `Color(0xFF2196F3)`, leaves anti-aliasing omitted and begins
with an empty child. The color editor admits exact ARGB or one reviewed Material
`ColorScheme` token. Literal color output remains const; a token emits
`Theme.of(context).colorScheme...` and correctly makes the surrounding
`ColoredBox` non-const. Generated Dart and Canvas construct the real widget.
Canvas alone retains a non-layout-affecting 36x36 target when an empty box has
zero size; that overlay is never persisted. At the `ColoredBox` milestone, the
practical backlog was 40/92 complete with 52 remaining; Layout had 29 items,
Scrolling 3 and Basic 4. `.fd` remained v9, Catalog API 8 and Canvas model v14;
NBFC framing plus Canvas control/wire remained version 1.

[`SafeArea`](https://api.flutter.dev/flutter/widgets/SafeArea/SafeArea.html)
is the twenty-first complete post-core Palette slice and occupies Layout order
240 after `OverflowBar`. Its pinned Flutter 3.44.8 const constructor exposes
optional `left`, `top`, `right`, `bottom`, signed finite physical
`EdgeInsets minimum`, and `maintainBottomViewPadding`, plus one required
any-widget `child`; only the common `key` is excluded. The detached wrapper
payload omits all six properties, preserving Flutter's `true`,
`EdgeInsets.zero` and `false` defaults, but is never admitted as a standalone
node.
`EdgeInsetsDirectional` is rejected because the public argument is physical
`EdgeInsets`. Palette creation never inserts the incomplete prototype: the
shared generic wrapper command surrounds one existing widget and fills the
required child in the same atomic command. The widget tree
can wrap either the document root or a non-root row; the current Canvas target
wire intentionally exposes only non-root children, so root wrapping remains
available through the tree. Expanded, Flexible and Spacer cannot be wrapped
because their ParentData must remain a direct Row/Column child. Generated Dart
and Canvas construct the real `SafeArea`. At that milestone the practical backlog
was 41/92 complete with 51 remaining; Layout contained 30 items, Scrolling 3, Basic 4 and
Material 4. `.fd` remains v9 and Canvas model remains v14; the exported
`EdgeInsetsValues.directionalAllowed` constraint advances Catalog API to 9.
NBFC framing plus Canvas control/wire remain version 1.

[`Placeholder`](https://api.flutter.dev/flutter/widgets/Placeholder/Placeholder.html)
is the twenty-second complete post-core Palette slice and occupies Basic order
50 after `ColoredBox`. Its pinned Flutter 3.44.8 const constructor exposes
optional `color`, `strokeWidth`, `fallbackWidth` and `fallbackHeight`, plus one
optional any-widget `child`; only the common `key` is excluded. A new node
stores no explicit properties and begins with an empty child, preserving
Flutter's exact `Color(0xFF455A64)`, `2.0`, `400.0` and `400.0` defaults.
Explicit numeric values must be finite and non-negative. The color editor
admits exact ARGB or one reviewed Material `ColorScheme` token; a token removes
`const` from generated Dart. Generated Dart and native/exact-Web Canvas
construct the real `Placeholder`, including its real fallback sizing and
optional child. Canvas-only selection and drop affordances remain outside the
widget. At that milestone the practical backlog was 42/92 complete with 50
remaining; Layout contained 30 items, Scrolling 3, Basic 5 and Material 4.
Existing value and
transport shapes keep `.fd` v9, Catalog API 9, Canvas model v14 and NBFC
framing/control/wire v1 unchanged.

[`Directionality`](https://api.flutter.dev/flutter/widgets/Directionality/Directionality.html)
is the twenty-third complete post-core Palette slice and occupies Basic order
60 after `Placeholder`. Its pinned Flutter 3.44.8 const constructor exposes
required `TextDirection textDirection` and one required any-widget `child`;
only the common `key` is excluded. Flutter provides no direction default, so
Designer creation persists the explicit reviewed `TextDirection.ltr` value.
Palette, tree and Canvas creation never publish an incomplete node: the shared
generic wrapper command surrounds one existing subtree and fills the required
child in the same atomic command. The property remains editable between exact
`ltr` and `rtl`, and real generated Dart/native/exact-Web Canvas direction
is inherited by directional descendants. Canvas-only selection and drop
affordances remain outside the inherited widget, which owns no render object.
At the Directionality milestone the practical backlog was 43/92 complete with
49 remaining; Layout contained 30 items, Scrolling 3, Basic 6 and Material 4.
Existing value and transport shapes kept `.fd` v9, Catalog API 9, Canvas model
v14 and NBFC framing/control/wire v1 unchanged.

[`DecoratedBox`](https://api.flutter.dev/flutter/widgets/DecoratedBox/DecoratedBox.html)
is the twenty-fourth complete post-core Palette slice and occupies Basic order
70 after `Directionality`. Its pinned Flutter 3.44.8 const constructor exposes
required `Decoration decoration`, optional `DecorationPosition position` with
the exact `background` default, and one optional any-widget `child`; only the
common `key` is excluded. The closed Designer branch is the already complete
typed `BoxDecoration` algebra used by Container, including literal and reviewed
theme colors, asset images, borders, physical or directional radii, ordered
shadows, gradients, blend mode and box shape. Custom `Decoration` subclasses
and raw Dart expressions remain outside the reviewed model. New nodes persist
an empty rectangular `BoxDecoration()` so the required property is valid before
the first edit. Generated Dart and native/exact-Web Canvas construct the real
widget and preserve whether the decoration paints behind or in front of its
child; Canvas-only selection and drop affordances remain outside the painted
effect. At the DecoratedBox milestone the practical backlog was 44/92 complete
with 48 remaining; Layout contained 30 items, Scrolling 3, Basic 7 and Material
4. Existing value and transport shapes kept `.fd` v9, Catalog API 9, Canvas
model v14 and NBFC framing/control/wire v1 unchanged.

[`ExcludeSemantics`](https://api.flutter.dev/flutter/widgets/ExcludeSemantics/ExcludeSemantics.html)
is the first Accessibility Palette item, at category order 400 and item order
10. Its complete Flutter 3.44.8 const constructor exposes optional
`bool excluding` with exact omitted default `true` and one optional any-widget
`child`; only the common `key` is excluded. New nodes keep `excluding` omitted.
Generated Dart and native/exact-Web Canvas construct the real widget: omitted or
explicit `true` removes the application child's semantics subtree, while
explicit `false` preserves it. Layout, paint and hit testing still proxy the
child. The `ExcludeSemantics` node's own Designer selection, hit/drop and
accessibility wrapper remains outside the effect, including a transient target
for an empty zero-size node. Descendant Canvas semantics labels follow the real
subtree exclusion; those widgets remain available in the NetBeans widget tree.
At that milestone the practical backlog was 45/92 complete with 47 remaining;
Layout contained 30 items, Scrolling 3, Basic 7, Material 4 and Accessibility 1. Existing value
and transport shapes kept `.fd` v9, Catalog API 9, Canvas model v14 and NBFC
framing/control/wire v1 unchanged.

[`IndexedStack`](https://api.flutter.dev/flutter/widgets/IndexedStack/IndexedStack.html)
is the next completed fixed-inventory Layout slice, at item order 115 beside
`Stack`. Its complete Flutter 3.44.8 const constructor contributes optional
`alignment`, `textDirection`, `clipBehavior`, `sizing` and nullable `index`,
plus ordered `children`. A new node stores no values and no children. **Index**
may remain `<not set>` for Flutter's default `0`, hold a non-negative child
index, or hold explicit `null` to display none; validation keeps every concrete
index within the current child list, while preserving Flutter's empty-list
index-zero exception. Generated Dart and both Canvas routes construct the real
widget. Its layout is the largest child's size, while paint, hit testing and
application semantics expose only the selected child; every child remains in
the ordered Designer model and NetBeans widget tree. At that milestone the
practical backlog was 46/92 complete with 45 remaining; Layout contained 31 items, Scrolling 3,
Basic 7, Material 4 and Accessibility 1. The exact null value advances `.fd`
to v10, Catalog API to 10 and Canvas model to v15; NBFC framing and Canvas
control/wire remain v1.

[`ClipRect`](https://api.flutter.dev/flutter/widgets/ClipRect/ClipRect.html)
is the next completed fixed-inventory Basic slice, at item order 80 after
`DecoratedBox`. Its safe Flutter 3.44.8 const contract exposes optional typed
`clipper`, optional `clipBehavior` and one optional single `child` slot. Omitting clip behavior keeps
Flutter's `Clip.hardEdge` default; the closed editor also admits `none`,
`antiAlias` and `antiAliasWithSaveLayer`. Generated Dart and both Canvas routes
construct the real widget when no custom clipper is set, while Designer selection and empty drop affordances
remain outside clipping. Schema v12 represents `CustomClipper<Rect>` by an
analyzed current-library or declared-package reference; the isolated Canvas
receives presence only and shows an explicit preview-unavailable state.
For an omitted child, the real node remains zero-size and the existing external
36x36 Designer target carries the warning, tooltip and semantics reason.
At that milestone the practical backlog was 47/92 complete with 45 remaining;
Layout contained 31 items, Scrolling 3, Basic 8, Material 4 and Accessibility 1. The surface had 41
reviewed const definitions and 736 writable rows, including 719 outside
`Scaffold`; `.fd` v10, Catalog API 10, Canvas model v15 and NBFC
framing/control/wire v1 remain unchanged.

[`ClipOval`](https://api.flutter.dev/flutter/widgets/ClipOval/ClipOval.html)
is the next completed fixed-inventory Basic slice, at item order 90 after
`ClipRect`. Its safe Flutter 3.44.8 const contract exposes optional typed
`clipper`, optional `clipBehavior` and one optional single `child` slot. Omitting clip behavior keeps
Flutter's `Clip.antiAlias` default; the closed editor also admits `none`,
`hardEdge` and `antiAliasWithSaveLayer`. Generated Dart and both Canvas routes
construct the real widget without a custom clipper, with the default oval inscribed in the child's
layout bounds, while Designer selection and empty drop affordances remain
outside clipping. Schema v12 represents `CustomClipper<Rect>` by the same
analyzed current-library or declared-package reference; the isolated Canvas
receives presence only and shows an explicit preview-unavailable state. For an
omitted child, the real node remains zero-size and the existing external 36x36
Designer target carries that unavailable-preview reason. At that
milestone the practical backlog was 48/92 complete with 44 remaining; Layout contained 31
items, Scrolling 3, Basic 9, Material 4 and Accessibility 1. The surface had 42
reviewed const definitions and 737 writable rows, including 720 outside
`Scaffold`; `.fd` v10, Catalog API 10, Canvas model v15 and NBFC
framing/control/wire v1 remained unchanged.

[`ClipRRect`](https://api.flutter.dev/flutter/widgets/ClipRRect/ClipRRect.html)
is the next completed fixed-inventory Basic slice, at item order 100 after
`ClipOval`. Its safe Flutter 3.44.8 contract exposes optional typed
`borderRadius`, optional typed `clipper`, optional closed `clipBehavior` and one
optional single `child` slot. The radius editor supports physical `BorderRadius` and directional
`BorderRadiusDirectional` geometry with finite, non-negative elliptical corner
radii; omission preserves `BorderRadius.zero`. Omitting clip behavior preserves
`Clip.antiAlias`; the editor also admits `none`, `hardEdge` and
`antiAliasWithSaveLayer`. The clipper editor references an existing value or a
zero-argument constructor, factory or function from the current library or an imported
`package:` library declared in `.dart_tool/package_config.json`, with an optional member and an explicit
const or non-const zero-argument invocation. Deterministic
generation adds its import, and Dart analysis proves the result is assignable to
`CustomClipper<RRect>`. Flutter ignores `borderRadius` when `clipper` is non-null.
Because the isolated runner never executes project Dart, both Canvas routes keep
the child and show an explicit preview-unavailable warning for that branch; they
do not fake the custom clip with `borderRadius`. Without a custom clipper, both
routes construct the real widget. Designer selection and empty drop affordances
remain outside clipping. With an omitted child, the zero-size real node uses
the same external 36x36 Designer target for the warning and complete reason.
The practical
palette at that historical milestone had 89 admitted widgets; Layout contained 31 items,
Scrolling 3, Basic 23, Material 26 and Accessibility 6. The surface has 82
reviewed const definitions and 3894 writable rows, including 3877 outside
`Scaffold`. PhysicalShape adds five rows, six structured shape presets and a typed
project clipper branch. Its closed value sets `.fd` v13, Catalog API 14 and Canvas
model v18. The later RepaintBoundary adds a structural child slot without changing
these versions or the writable-row count. IgnorePointer and AbsorbPointer each add
two complete boolean fields and an optional child without another version change.
BlockSemantics adds one boolean field and an optional child with the same versions.
MergeSemantics adds a structural child slot with no new scalar row or encoding.
IndexedSemantics adds the required signed index row and an optional child slot.
ExcludeFocus adds one optional boolean row and a required child wrapper slot.
ExcludeFocusTraversal adds one optional boolean row and a required child wrapper
slot with traversal-only exclusion instead of focus ineligibility.
DefaultTextHeightBehavior adds three optional leaves assembled into its required
TextHeightBehavior value, plus a required child wrapper; formats remain unchanged.
TickerMode adds required enabled and optional forceFrames boolean rows with a
required child wrapper, without another format or protocol change.
Visibility adds seven optional boolean rows, a required child and an optional
replacement slot whose empty value omits the non-nullable Dart argument.
NBFC framing/control/wire remains v1.

Seventy-three any-widget slots provide the reusable destination contract, including
the optional `ListTile.leading`, `ListTile.title`, `ListTile.subtitle`, `ListTile.trailing`, `CheckboxListTile.title`, `CheckboxListTile.subtitle`, `CheckboxListTile.secondary`, `Visibility.replacement`, `Card.child`, `Badge.label`, `Badge.child`, `CircleAvatar.child`, standard/tonal `FilledButton.child`, standard/small/large `FloatingActionButton.child`, extended `FloatingActionButton.icon`, `IconButton.selectedIcon`, and icon-mode `TextButton.icon`, `OutlinedButton.icon` and `FilledButton.icon` slots:
`Scaffold.body`, `Scaffold.floatingActionButton`, `Column.children`,
`Row.children`, `ListView.children`, `GridView.count.children`, `ListBody.children`, `OverflowBar.children`,
`Wrap.children`, `Center.child`, `Align.child`,
`FractionallySizedBox.child`, `FittedBox.child`, `ConstrainedBox.child`,
`UnconstrainedBox.child`, `LimitedBox.child`, `OverflowBox.child`,
`Padding.child`, `SizedBox.child`, `AspectRatio.child`, `Container.child`,
`Opacity.child`, `ColoredBox.child`, `Placeholder.child`, `DecoratedBox.child`, `ClipRect.child`, `ClipOval.child`, `ClipRRect.child`, `ClipPath.child`, `ClipRSuperellipse.child`, `PhysicalModel.child`, `PhysicalShape.child`, `RepaintBoundary.child`, `IgnorePointer.child`, `AbsorbPointer.child`, `ExcludeSemantics.child`, `BlockSemantics.child`, `MergeSemantics.child`, `IndexedSemantics.child`, `Baseline.child`, `IntrinsicHeight.child`, `IntrinsicWidth.child`, `Offstage.child`, `SizedOverflowBox.child`, `Transform.child`, `RotatedBox.child`, `SingleChildScrollView.child`, `Stack.children`, `IndexedStack.children`,
`ElevatedButton.child`, and AppBar's `leading`, `title`, `actions` and
`flexibleSpace`. `Scaffold.appBar` and `AppBar.bottom` accept only
`PreferredSizeWidget`, currently the reviewed AppBar. PreferredSize is a
required-child wrapper source and is not an empty-slot destination. The 75 insertable
destinations and 89 sources form 6,675 candidate cells: 6,286 accepted and 389
rejected. Expanded and Flexible each wrap only an existing direct
`Row.children`/`Column.children` child; Spacer inserts only into those same two
list slots. The other 86 sources enter all 73 any-widget slots, and only AppBar
enters the two trait-bound slots. Expanded and Flexible's required `child` slots
are replacement-only and are therefore not insertable matrix destinations.
SafeArea, Directionality, ExcludeFocus, ExcludeFocusTraversal, Visibility, TickerMode, DefaultTextHeightBehavior, DefaultSelectionStyle, IconTheme, RefreshIndicator, TextButton, OutlinedButton, IconButton and RadioGroup use the generic required-slot wrapper mode without
a Row/Column-only outer placement restriction. Their required Icon/Child slots are
likewise excluded from the insertable matrix; all fourteen sources enter all 70
any-widget destinations, and none can wrap Expanded, Flexible or Spacer because
their ParentData must remain directly under Row/Column.
`ElevatedButton.child` is an optional-single, required-named-but-nullable slot;
an empty button deterministically emits `child: null`. An empty `Row`, `Column`,
`ListView`, `GridView.count`, `ListBody` or `OverflowBar` exposes its complete bounded design-time area as
insertion index `0`; once populated, only its terminal append zone is admitted.
`ListBody` resolves that edge from `mainAxis`, `reverse` and ambient
`Directionality`. `OverflowBar` resolves its active horizontal or overflow-column
mode from rendered geometry, then applies effective text direction or
`overflowDirection`. `Stack.children`
uses the complete Stack rectangle for both empty and populated z-order appends;
`Wrap.children` likewise uses the complete Wrap rectangle for terminal appends;
AppBar actions use their dedicated logical actions zone. The standard
widget tree accepts the same Palette operations on an exact row: `Row`, `Column`,
`Stack`, `ListView`, `GridView.count`, `ListBody`, `OverflowBar` and `Wrap` append to `children`, while an empty `Center`, `Align`,
`FractionallySizedBox`, `FittedBox`, `ConstrainedBox`, `UnconstrainedBox`, `LimitedBox`, `OverflowBox`, `Baseline`, `IntrinsicHeight`, `IntrinsicWidth`, `Offstage`, `SizedOverflowBox`, `Transform`, `RotatedBox`,
`Padding`, `SizedBox`, `AspectRatio`, `Container`, `SingleChildScrollView`,
`Opacity`, `ColoredBox`, `DecoratedBox`, `ClipRect`, `ClipOval`, `ClipRRect` or `ElevatedButton`
receives its
`child`. Parents with several compatible catalog slots, including AppBar and
Scaffold, remain rejected as ambiguous by flattened-tree drop; select the
parent and use its `Slots`
Properties tab to choose the exact named destination.
PreferredSize, SafeArea, Directionality, ExcludeFocus, ExcludeFocusTraversal, Visibility, TickerMode, DefaultTextHeightBehavior, DefaultSelectionStyle, IconTheme, RefreshIndicator, TextButton, OutlinedButton and IconButton are wrapper
sources whose required Icon/Child slots are not empty-slot destinations. Visibility's
optional replacement remains an insertable destination. Dropping any of these wrapper
sources on an exact widget-tree row atomically wraps that
root or non-root subtree. The Canvas route exposes the same operation only for
non-root children and offers no synthetic root target.
An existing non-root widget can be moved within the same widget tree by dropping
on a uniquely compatible container (`ON`) or at a visible before/after boundary
(`INSERT`) of a list slot. The planner applies the catalog acceptance and
cardinality matrix after source removal, rejects root moves, cycles, required-
source violations, full or incompatible slots, invalid indices and exact
no-ops, and preserves the complete subtree and all stable IDs. Commit re-reads
the latest immutable document and rejects a target whose exact command changed
after hover. Selection returns to the moved stable ID. When generation proves
that Dart bytes are unchanged, the same command uses the exact `FD_ONLY` path:
only canonical `.fd` is committed while live/disk Dart and Source Undo remain
byte-for-byte unchanged. A negotiated `widget.movePreview.v1` runner paints the
future destination in amber, but missing Canvas capability never disables the
Swing-tree move. Every slot-capable widget exposes compact native `General` and
`Slots` tabs in the standard Properties window; leaf widgets remain untabbed.
Every catalog-declared slot, including an empty optional slot, is projected in
`Slots`. Its transactional custom editor can add a reviewed Palette widget, move/reorder an existing
widget into the exact named slot, or remove one direct child (`Clear` for an
occupied single slot). It re-plans against the bound immutable revision and
commits one command through the existing Save/Undo pipeline only after OK;
Cancel is a no-op. An occupied single slot is never replaced implicitly. List
insertion is append-only in this first safe slice, while existing list children
can be moved to any catalog-valid post-removal position; list clear-all is
deliberately not exposed as a sequence of partial deletes.
The standard
pair-aware Copy/Paste, Cut/Move, Rename and Delete
described above are also enabled; Linux/macOS native hosts and the broader
writable UI remain future work. Embedded DevTools and its Flutter Inspector are
a separate future milestone.

Canvas presentation is independent from responsive layout: Android Phone
remains 390×844 logical pixels, Web remains 1440×900, and so on, regardless of
the current IDE pane size. `Fit` changes only the paint transform; `100%` shows
the profile at its logical size. Manual zoom can overflow the embedded surface,
in which case Flutter-owned horizontal/vertical scrolling, mouse-wheel
scrolling, Shift+wheel horizontal scrolling and Ctrl+wheel zoom remain aligned
with Flutter hit testing and Palette drop coordinates.
