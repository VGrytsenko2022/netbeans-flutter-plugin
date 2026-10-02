# BottomSheet

Pinned contract: Flutter 3.44.8, [constructor and properties](https://api.flutter.dev/flutter/material/BottomSheet/BottomSheet.html).
One palette definition, all 16 native arguments, 37 editable property rows:
16 direct constructor fields plus 21 local ShapeBorder fields. The Designer
Child slot is synthetic: it supplies the Widget returned by the builder,
not a nonexistent native BottomSheet.child argument.

## Content and lifecycle

- Builder starts with Child, emitting `(context) => child`; absent Child emits
  `(context) => const SizedBox.shrink()`. Content is a box, not a sliver.
- A verified WidgetBuilder reference/getter/zero-argument factory may replace
  that preset after Child is cleared. Retaining both is rejected atomically.
- On closing starts with a no-action closure. Disconnect restores that preset;
  a required constructor field cannot be removed or set to null. Existing
  handler methods remain untouched. Optional drag handlers support null/omission.
- Create, select, open and rename use the shared typed Events workflow.
  `onDragEnd` receives `DragEndDetails details, {required bool isClosing}`.
  Flutter calls it before onClosing when closing; onClosing may fire repeatedly.
- Enable drag and Show drag handle start explicitly false. Flutter's own
  defaults are true and theme/null respectively. Dragging, including a visible
  handle with enableDrag false, requires a controller. Without a source controller,
  both flags must remain explicitly false (including when restoring inherited
  handle behavior). A typed nullable source must resolve non-null at runtime
  whenever dragging is possible; the analyzer proves type, not runtime contents.
- BottomSheet mutates its AnimationController. The application supplies a
  long-lived controller, a ticker provider and disposal. No controller field,
  ticker mixin, showBottomSheet/showModalBottomSheet call, route or Navigator.pop
  is silently inserted into user code. This slice does not broaden the existing
  Designer State-owner parser to accept arbitrary mixin/class-header edits;
  controller owners in ordinary project code can expose typed references.

## Appearance

Literal/theme/typed-source colors, nonnegative elevation, nullable Clip,
finite nonnegative Size with local/source/null/unset editor, normalized
BoxConstraints and Key are supported. Ten local shape families include every
applicable field, physical/directional radii and typed whole ShapeBorder.
Whole shapes and local leaves are mutually exclusive and switch atomically.

Null/unset appearance follows BottomSheetTheme and native M2/M3 behavior.
M3 defaults to maxWidth 640 when no constraints are provided; M2 uses parent
constraints. A constrained sheet aligns bottom-center. Default handle size
is 32 x 4 after theme fallback. The builder output receives the native Material.

## Isolated Canvas boundary

Canvas instantiates the native BottomSheet with a privately owned/disposed
preview controller. It preserves literal appearance and local Child content.
Project controllers, getters, factories, builders and handlers are never
executed. Custom builders show an explicit unavailable placeholder; custom
shapes show unavailable shape preview, and other source-owned appearance is
identified in the tooltip and replaced by native defaults. Preview callbacks
keep the design visible, do not dismiss routes and do not change the FD model.
Empty content retains a Designer selection/drop target.

This does not add a route editor or unrestricted custom-code editor. Normal
Dart owns presentation, controller setup/disposal and dismissal. Native route
helpers remain distinct from this widget constructor.

## Contract verification

The slice includes Java constructor/proof/validation, editor and command
history/reopen tests, a pinned-SDK analyzer/native test, Canvas shape/source/
lifecycle tests, catalog/DnD parity and four reviewed SVG variants.
FD 17, Catalog API 16, Canvas model 20 and transport 1 remain unchanged.

Verified 2026-09-16:

- Java full reactor plus reruns of updated inventory assertions: 6,320 cases,
  6,043 passed, 277 environment-gated skips, no unresolved failures/errors.
- Pinned Flutter SDK: 205 generated native cases, 28 unsafe sources rejected;
  real pointer drag verifies start/end/closing order and caller-owned disposal.
- Canvas: all 12,096 tests passed (29 new BottomSheet cases); analyze clean.
- Maven install, nine package-metadata integration tests and nbm:cluster passed.
- NBM byte verification: current classes, 76 SVG variants, 41 runner sources,
  35 Web files, Flutter license, and all four cluster JARs match the package.
- NBM size: 9,404,416 bytes; SHA-256:
  `c5b373640c15f117ed26c82f6e1301b46212e1b800c26291dc9c2f47720ef180`.
- Web main.dart.js: 4,847,499 bytes; SHA-256:
  `c524c2818fc2371fc6987879db4fc7fb5eb1ec9ad5447b7965ede1669d3f7f78`.
- Manual validation in the user's running IDE was not performed.
