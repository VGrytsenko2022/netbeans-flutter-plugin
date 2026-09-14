# SliverAppBar (Flutter 3.44.8)

## API and scope

All three constructors are admitted: SliverAppBar.new, SliverAppBar.medium and
SliverAppBar.large. Each has 131 typed property rows and five independent slots.
All 33 scalar constructor arguments are represented, including their structured
styles, and the five native child arguments. Key remains Designer-owned.

Sources: [SliverAppBar](https://api.flutter.dev/flutter/material/SliverAppBar-class.html),
[medium](https://api.flutter.dev/flutter/material/SliverAppBar/SliverAppBar.medium.html),
[large](https://api.flutter.dev/flutter/material/SliverAppBar/SliverAppBar.large.html).
Constructor assertions/defaults were checked against the pinned SDK's
packages/flutter/lib/src/material/app_bar.dart, not inferred from AppBar.

## Properties and events

Direct booleans, heights, spacing, colors, clip, directional Actions padding and
all SDK flags preserve omission and native nullability. Standard defaults to a
56px toolbar and unpinned; medium/large default to 64px and pinned.
Collapsed height must be at least the resolved toolbar height. Stretch trigger
offset must be strictly positive. Snap requires Floating: editing that pair is
one atomic command, including reset and Undo/Redo.

Shape supports five local border constructors plus strict project ShapeBorder
references/factories, covering custom and other SDK shapes. Icon themes, both
text styles and all eight SystemUiOverlayStyle fields have local editors plus
whole nullable typed references. Whole/local changes are atomic within their
own family; unrelated values and all slots survive. Actions padding additionally
supports typed EdgeInsetsGeometry references.

On stretch trigger is a native Event with Future<void> signature and no arguments.
It supports omission, explicit null, the async no-op preset and source handlers.
Project callbacks are retained and analyzed but never executed by Canvas.

## Slots, source and native preview

Leading, Title, Actions, Flexible space and Bottom are independently editable.
Bottom admits only PreferredSizeWidget implementations. SliverAppBar itself is a
Sliver, not a PreferredSizeWidget: place it in a Sliver slot, not Scaffold.appBar.
Source keeps the selected native constructor and all user-owned Dart.

Medium/large natively mount title twice. Canvas gives its mirror private stable
keys, while canonical model keys follow the active toolbar/expanded presentation.
This avoids duplicate GlobalKeys without replacing the native layout.
Isolated Canvas refreshes native stretch configuration when its offset changes;
the pinned SDK otherwise only refreshes that configuration when Stretch changes.

PreferredSize's Canvas specification now retains its existing Java trait and
forwards its actual preferred size; this also repairs its use in ordinary AppBar.
Visible drop zones are separated by native toolbar, expanded-space and bottom
regions, including RTL. Required wrappers still require actual content.

Project-owned styles, padding and callbacks are never evaluated by the runner.
Native/theme fallback and explicit preview limitations do not rewrite the model
or generated code. FD 16, Catalog API 15, Canvas model 19 and transport 1 remain.

## Current inventory

160 definitions: 152 with scalar Properties and eight structural; 6919 writable
rows (6901 outside Scaffold); 130 const-capable constructors. Material 47,
Scrolling 46. 155 ordinary destinations (140 AnyWidget and 15 trait-constrained)
produce 24800 candidates: 16527 accepted and 8273 rejected. Required wrappers 28.
631 boolean-only rows, 47 nullable boolean unions. Native Events 154 across
40 types; all callables 201 across 54 types.

## Verification

Verification used Flutter 3.44.8, JDK 25 and the canonical G: checkout.

- Full Designer core: 2331 tests, 20 conditional skips, no failures or errors.
- Final focused Java reports: 1371 tests, 25 conditional skips, no failures or
  errors after the accessibility-class rerun. This includes the three new
  contracts, all current palette/drop inventories, wrapper destinations,
  properties, generation, Analyzer witnesses, icons and runner manifests.
- The full reactor run completed 5726 tests with 207 skips and 10 assertion
  failures. Nine inventory/count assertions were corrected and rechecked above.
  The remaining unrelated working-tree change in
  JnaWindowsNativeCanvasApiTest.readsDpiFromTheExactHwndThroughTheInjectedNativeBoundary
  expects 145 while its injected boundary returns 144. It was left untouched;
  the full reactor is therefore not reported as green.
- SliverAppBarRealSdkTest passed with the pinned SDK explicitly enabled. It
  checks all three constructors, current/imported style and handler references,
  18 wrong/dynamic type rejections, pair-save admission, reopen and history.
  Nine generated Dart cases also pass native Flutter widget runtime tests.
- Complete Canvas suite: 4921 tests passed, including 51 SliverAppBar cases.
  Flutter analysis reports no issues. Release Web compilation passed.
- Native desktop manual verification in the user's running IDE was not performed.

Package verification passed after Maven install (tests separately verified above)
and nbm:cluster: 12 SVG variants, all 40 runner sources and all 35 Web artifact
files match their manifests/current inputs. The module, Designer, runner and
Analyzer JARs in the development cluster match the NBM byte for byte.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8780082 bytes. SHA-256:
`e1a4a1a7b1f10af15ed2d163378673e4e48287ef06477d2c87d3672100cd3372`.

The follow-up [FlexibleSpaceBar slice](FLEXIBLE_SPACE_BAR.md) is now implemented.
The inventory and test totals above remain the historical SliverAppBar snapshot.
Project-owned values and event code are not executed by the runner.
