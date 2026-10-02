# SimpleDialog and SimpleDialogOption

Pinned SDK: Flutter 3.44.8. Public API:
[SimpleDialog](https://api.flutter.dev/flutter/material/SimpleDialog/SimpleDialog.html)
and [SimpleDialogOption](https://api.flutter.dev/flutter/material/SimpleDialogOption-class.html).
Implementation was also checked against the pinned SDK's material/dialog.dart.

## Complete constructor surface

SimpleDialog exposes all 17 arguments: key, title, titlePadding, titleTextStyle,
children, contentPadding, contentTextStyle, backgroundColor, elevation,
shadowColor, surfaceTintColor, semanticLabel, insetPadding, clipBehavior, shape,
alignment and constraints. The property sheet contains 98 rows: 15 direct
arguments, 21 local shape fields and two full 31-field TextStyle families.
Title is an optional single-widget slot; Children is an optional ordered list.

SimpleDialogOption exposes all four arguments: key, onPressed, padding and child.
Three property rows plus the optional Child slot. Both constructors are const
when their complete argument graphs are constant.

No injected layout, content, callback, theme or appearance defaults are saved.
Physical/directional dialog title/content padding is non-null when explicitly
supplied; omission preserves Flutter's 24/24/24/0 and 0/12/0/16 spacing.
Option padding accepts only physical EdgeInsets?; omission/null uses horizontal
24 and vertical 8. Insets are non-negative. SimpleDialog insetPadding is physical
EdgeInsets? and retains DialogTheme defaults on null/omission.

Two TextStyle families include typography, colors/paints, shadows, fallback fonts,
package, locale, decoration, features and variations. Whole source/null and
local leaves are mutually exclusive. Ten native local ShapeBorder families
retain their applicable parameters. Shape and style changes are atomic undoable
commands; stable property nodes preserve the current editing surface.

## Events, source ownership and native behavior

Only SimpleDialogOption has an Event: optional VoidCallback? onPressed. Handler
actions use the existing Events workflow; typed nullable references, getters,
factories and explicit null are retained. Unset/null disables selection.
There is no invented onConfirm/onChanged or implicit Navigator action.

The application owns showDialog, modal barrier policy, navigation and results.
An option's user handler can call Navigator.pop(context, result). User code
outside managed regions survives edits, undo/redo, save and reopen. SDK tests
verify the actual route result in addition to static source proof.

SimpleDialog retains native intrinsic width, title/text scaling and scrollable
choices. Lazy viewports must be bounded when placed in intrinsic-layout content;
the designer does not rewrite the widget tree to hide invalid Flutter layouts.

## Isolated Canvas

Canvas renders native SimpleDialog/SimpleDialogOption and preserves all local
appearance, placement, empty-slot drop targets and zero-size selection handles.
A supplied callback is represented by an inert callback, never application code.
For a nullable source Canvas cannot know the runtime nullness; presence previews
the option as enabled and the source-execution limitation is disclosed.
Project-valued appearance uses disclosed native defaults. Project ShapeBorder,
or shapes outside the isolated preview's complexity budget, has an explicit
unavailable preview instead of silently substituting a different border.
Native dialog routes are not opened inside the design surface.

## Inventory

244 admitted definitions, 235 typed and nine structural/propertyless;
8,177 writable rows (8,159 outside Scaffold), 200 const-capable definitions.
70 Material items. 267 callable rows across 100 definitions; 197 Events across
71 types. Three new optional slot destinations bring the placement matrix to
53,192 cells: 34,402 accepted and 18,790 rejected, across 218 destinations.
FD 17, Catalog API 16, Canvas model 20 and transport 1 remain unchanged.

## Verification

- Dedicated Canvas: 35 tests passed; flutter analyze reported no issues.
- Real SDK: 193 native cases across six platforms and both directions,
  including an option returning a real dialog result; 40 unsafe source cases rejected.
- Core, typed editors and command tests cover all constructor fields, 100 shape
  switches, every local text-style leaf, source constraints and atomic history/reopen.
- Full Canvas regression: 12,067 tests passed. After a diagnostic-label-only change,
  the final 117 SimpleDialog/AlertDialog tests and flutter analyze passed again.
- Java: full reactor followed by targeted reruns for seven stale event/callable
  inventory assertions. Final fresh reports cover 6,311 tests: 6,035 passed,
  276 conditionally skipped, zero outstanding failures/errors. The SDK-gated
  test is covered separately above, not inferred from a skipped default run.
- Final install and all nine package-metadata integration tests passed.
  NBM verification confirms current classes, 72 icon variants, 41 runner sources,
  35 Web artifact files and four byte-identical development-cluster JARs.
- Web main.dart.js: 4838126 bytes, SHA-256
  `b4b354d5f57fa77e71bfb372b50487a71bb14ba35f282f2c42aba02f10eacd86`.
- NBM: 9394745 bytes, SHA-256
  `674d61209fae491a3a19962c6f904dd0bc4ba4a198f5389aa685a77c32d1061e`.

NetBeans shutdown-hook/fork-exit and adversarial-test log warnings are not
treated as test results; the outcomes above use fresh XML reports and exit status.

Manual installed-IDE testing has not been performed. No installed IDE, userdir,
user sample application, CJK IME or platform Canvas provider was modified.
