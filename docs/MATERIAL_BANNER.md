# MaterialBanner

Pinned Flutter 3.44.8:
[MaterialBanner constructor](https://api.flutter.dev/flutter/material/MaterialBanner/MaterialBanner.html).
All 18 arguments are represented by 15 direct fields and three slots. The
contentTextStyle family adds 31 shared local leaves: 46 editable rows total.

## Slots and creation

- Content is a required single box child; Actions is a required list of 1..10,000
  box widgets; Leading is optional. Slivers and non-widget descriptors are rejected.
- Creation inserts Text('Message') and a TextButton with Text('Action') and the
  standard enabled no-action closure. These are real model children with
  deterministic, owner-derived IDs, not Canvas-only decorations.
- Insert into an empty container is supported without wrapping an existing child.
  Add/remove/reorder/replace actions uses normal Slots/history. Removing the last
  action or the required content fails atomically. Additional actions may be any
  valid box widgets, not only buttons.

## Properties and source proof

Key accepts string ValueKey, strictly verified Key?, null or omission. Colors
accept literals, theme tokens, Color? sources or null. Elevation is nonnegative;
minActionBarHeight accepts nonnegative local integer/decimal or strict non-null
double, defaulting to 52. Sources must also satisfy their runtime bounds.

Padding, margin and leadingPadding accept nonnegative physical/directional
EdgeInsetsGeometry, strict nullable source references/getters/factories, or null.
Directional values resolve using native LTR/RTL. The default margin adds ten
pixels below an elevated banner, otherwise zero. Null appearance delegates to
MaterialBannerTheme and pinned Material 2/3 defaults. surfaceTintColor remains
supported, although Flutter recommends tone-based surface colors instead.

forceActionsBelow is a non-null Boolean; omission is false. One action normally
sits beside content; multiple actions or forceActionsBelow place actions below.
Native OverflowBar handles wrapping, with start/end/center overflowAlignment.
Flutter clamps content text scaling to 1.5; multi-row actions have native scaling.

contentTextStyle supports the complete shared local family (including paints,
locale, theme base, shadows, font features/variations and decoration) or strict
TextStyle?. Whole style/null is exclusive with local leaves. Switching clears
the opposing representation in one command; foreground/color and
background/backgroundColor are likewise exclusive. Font package requires a
family or fallback. Null/omission falls back to the banner theme, then bodyMedium.

## Animation and events

Unlike SnackBar, null/omitted animation is a valid static MaterialBanner. A local
0..1 value generates AlwaysStoppedAnimation<double>. Strict Animation<double>?
references, getters and factories preserve application-owned lifetime.

onVisible is optional VoidCallback? and fires only on the first completed
animation status. A static banner or already-stopped local animation does not
produce that event. Create/select/open/rename/disconnect preserve user methods
outside generated regions and survive history and save/reopen.

ScaffoldMessenger.showMaterialBanner supplies/replaces animation and owns
queuing, hide/remove and the controller's closed Future. Actions do not close
the banner automatically: their handlers decide when to hide/remove it.
No implicit messenger calls, routes, controller fields, timer, dismissal code
or invented onClosed Event are generated.

## Canvas boundary

The isolated preview renders the native MaterialBanner and all model children.
Static null/omitted animation remains static; any supplied local/source
animation previews at progress 1 so the banner remains selectable. Pointer
and focus activation are blocked; no user getter, factory or handler runs.
Source-owned appearance uses native defaults and diagnostics name those fields.
Explicit-null/whole-source text style preserves theme fallback, not an empty
TextStyle. Local TextStyle leaves render normally. Key identity is retained.

Formats remain FD 17, Catalog API 16, Canvas model 20 and transport 1.
Catalog totals: 248 definitions, 8,306 rows, 239 typed/9 propertyless, 204 const,
736 Boolean-only/57 nullable-Boolean rows. The optional-destination matrix has
54,808 cases: 35,519 accepted and 19,289 rejected across 221 destinations.

## Verification

- Full Maven reactor regression: 6,341 tests, 280 explicitly skipped optional
  environment/SDK cases, no failures or errors (6,061 executed successfully).
- Separately enabled real Flutter 3.44.8 SDK gate: 294 generated/native cases
  passed, and all 28 unsafe source bindings were rejected. Coverage includes
  Material 2/3, all six TargetPlatform values, LTR/RTL, all constructor fields, local styles,
  typed nullable sources/factories and native messenger lifecycle.
- Full Canvas suite: 12,153 tests passed; the 25 MaterialBanner tests also
  passed again after lint cleanup. Flutter analyze reports no issues.
- Release Web build passed. All 41 packaged-runner source manifest entries
  match current file sizes and SHA-256 hashes.
- Final install recheck: 84 selected Java tests passed, including the enabled
  real-Web artifact check, followed by all nine package-metadata integration
  tests. Development cluster assembly passed; all four checked cluster JARs
  match the NBM. Packaged classes, 88 recent palette SVG variants, 28 animated
  preview SVGs/license, 41 runner sources and 35 Web artifact files were verified.
- NBM: `netbeans-flutter-plugin-0.1.3-SNAPSHOT.nbm`, 9,433,083 bytes;
  SHA-256 `c4d4fd0fc74907b959a1bce4369acdbc20a1b99bbbbf4c7c2ab084825e9277ee`.

Manual validation in the user's running IDE has not been performed.
