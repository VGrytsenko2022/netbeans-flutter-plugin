# SliverVisibility and SliverVisibility.maintain

Reviewed against the pinned Flutter 3.44.8 SDK and official Flutter API.

## Complete constructors

Default:
`const SliverVisibility({Key? key, required Widget sliver, Widget replacementSliver = const SliverToBoxAdapter(), bool visible = true, bool maintainState = false, bool maintainAnimation = false, bool maintainSize = false, bool maintainSemantics = false, bool maintainInteractivity = false})`.

Maintain:
`const SliverVisibility.maintain({Key? key, required Widget sliver, Widget replacementSliver = const SliverToBoxAdapter(), bool visible = true})`.

They have separate typed definitions and palette entries. Key remains
Designer-owned identity. The default exposes all six optional booleans;
.maintain exposes Visible and fixes all five maintenance flags to true.
Unlike box Visibility, this pinned sliver API has no maintainFocusability
parameter. Neither constructor has native event callbacks.

Explicit booleans use the shared centered checkbox. Omission preserves SDK
defaults; Restore Default removes the argument. Null, numeric/string coercions
and properties not present on the selected constructor are rejected. Row and
property-set identity remain stable across refresh, with editable reopened rows.

## Dependencies and runtime behavior

The default constructor enforces all four SDK assertions, even when visible:
maintainAnimation requires maintainState; maintainSize requires maintainAnimation;
maintainSemantics and maintainInteractivity each require maintainSize.
All 64 explicit boolean combinations are checked; fourteen are valid.
Set State, then Animation, then Size before enabling Semantics/Interactivity;
clear them in reverse order. Atomic property patches validate their final state.

- With Visible=true, the main sliver is shown.
- Hidden with Maintain state=false: the runtime main child is disposed and the
  replacement is mounted; both FD branches remain stored.
- Hidden with state retained but size not retained: the real SliverOffstage
  keeps the child laid out while contributing zero scroll/paint extent.
  TickerMode mutes hidden animations unless Maintain animation is enabled.
- Maintain size=true retains native scroll/layout extent while hiding paint.
  Maintain semantics and Maintain interactivity independently retain native
  accessibility and pointer behavior. .maintain enables both.
- Retained slivers do not add a focus exclusion: programmatic keyboard focus
  and key delivery remain possible while hidden. These flags do not override
  existing project-preview TextField/focus privacy rules.
- Changing maintenance flags can rebuild the runtime subtree and discard its
  state; normally only Visible should change dynamically.

## Slots, creation and source generation

Both constructors wrap an existing compatible sliver atomically. Their required
Sliver cannot be cleared or moved out; replacement is atomic and Undo restores
the original subtree. An optional Replacement sliver can be added, replaced,
removed, moved or wrapped, including through the tree while it is inactive.
Empty lists cannot create an incomplete required wrapper.

Both slots accept slivers, not boxes; direct SliverCrossAxisExpanded children
are rejected because they require their direct CrossAxisGroup parent.
The wrapper cannot be the document root. Empty/omitted Replacement sliver
is omitted from Dart to preserve the non-null SliverToBoxAdapter default,
never emitted as null or manufactured as an FD child. Explicit replacements
are generated even for .maintain, where Flutter itself ignores them.

Typed validation, import/const generation, candidate symbol evidence,
FD/source save/reopen, preservation of user code and exact Undo/Redo are kept
inside the existing contracts. No raw Dart escape hatch or schema relaxation.

## Canvas and hidden-branch editing

Canvas uses the actual SliverVisibility constructors and SDK renderers.
The active replacement branch is selectable/droppable; inactive and hidden
descendants have no synthetic Canvas geometry or inline editing targets.
This includes retained-size children with native interactivity enabled: native
pointer policy is not changed, but the Designer does not select hidden children.
The owning wrapper retains its compact handle, and both branches remain editable
through the widget tree. Empty active replacements provide bounded insertion
targets in both axes, reverse and RTL; offscreen targets stay excluded.
Painted-branch membership is cached once per immutable Canvas model.

Flutter 3.44.8's maintained-size visibility renderer marks only paint dirty
when Visible changes. Canvas additionally invalidates that actual render
object's semantics, preventing stale labels/assertions with a rebuilt hidden
child while preserving native layout, paint and interaction behavior.
Generated application code remains the standard Flutter constructor; the plugin
does not patch the user's SDK. Native cached-child characterization and Canvas
live-semantic transition tests are separate.

## Inventory at this slice

150 admitted definitions: 144 typed and six structural; 6,506 writable rows
(6,488 outside Scaffold), 120 const-capable definitions, 39 Scrolling entries.
555 boolean-only rows and 44 nullable boolean unions. Required-child wrappers
increase to 27. There are 135 insertion destinations: 124 AnyWidget and eleven
trait-restricted (nine Sliver and two PreferredSize). The full 20,250
source/destination matrix contains 14,550 accepted and 5,700 rejected combinations.
Native Events remain 150 across 36 types; all callables remain 196 across 49.
FD 16, Catalog API 15, Canvas model 19 and transport 1 are unchanged.
These are admitted definitions, not a total or remaining count for all Flutter.

## Verification

452 dedicated Flutter tests passed, covering every valid maintenance mode,
both constructors, omitted/empty/explicit replacement, horizontal/vertical,
reverse/RTL, strict invalid domains/dependencies, state/disposal, animation,
keyboard/pointer behavior, live semantics, hidden and offscreen drop/move
geometry, source-aware required wrapping and selection exclusion.

The separately enabled real SDK test passed: one JUnit, zero skips/failures/
errors, 45.353 seconds, 24 candidate-analysis transitions and 160 generated
native Dart cases. Exact FD/source reopening and user-code preservation,
required wrapping, replacement insertion/movement, property patches/resets
and Undo/Redo were checked.

The complete Flutter runner passed 4,027 tests after the final geometry-cache
change. Analysis of lib/test found no issues. The release Web build succeeded:
main.dart.js is 3,694,556 bytes, SHA-256
`59dc120dc16d36445f3b5dc86c380c5ae1247b203c5402a73432c858a6d730f7`. Source and Web manifests retain strict size/hash checks.

The complete Java core reactor passed 2,297 tests, 20 conditional skips,
zero failures/errors and 314 fresh Surefire XML reports.

The broad core/NetBeans integration suite passed 1,186 tests (360 core,
826 NetBeans), with 16 conditional skips and no failures/errors in 88 fresh
Surefire XML reports. The real-SDK test ran separately without a skip.
Coverage includes palette/tree/Canvas wrapping and insertion, optional slots,
all placement destinations, centered checkbox contracts, SVG variants,
accessibility, Java/Dart parity and strict source/Web artifact validation.
The unrelated full NetBeans reactor was not rerun because of its recorded
Windows AWT shutdown issue.

NBM install and development-cluster creation succeeded. Strict package checks
verified the schema class, eight light/dark 16/32 SVG assets, forty runner
sources and thirty-five Web files against current inputs. All three plugin,
Designer and runner development-cluster JARs exactly match the NBM.

Artifact: `netbeans-plugin/target/netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`,
8,724,930 bytes; SHA-256
`3d77a51de342e726207831e02b6b2baa674687b2b32e36da60773903e4191e8e`.

Manual NetBeans desktop testing or an IDE restart are not claimed.
CJK IME and Linux/macOS Canvas providers remain deferred.
Next palette slice at this milestone: SliverSafeArea (now completed; see [SliverSafeArea](SLIVER_SAFE_AREA.md)).

## Sources

- [Default constructor and dependencies](https://api.flutter.dev/flutter/widgets/SliverVisibility/SliverVisibility.html)
- [Maintain constructor](https://api.flutter.dev/flutter/widgets/SliverVisibility/SliverVisibility.maintain.html)
- Pinned SDK: packages/flutter/lib/src/widgets/visibility.dart.
