---
name: android-material-buttons
description: >-
  Polishes Android XML buttons using Material Components (MaterialButton, rounded
  corners, strokes, ripples) and a small design-token set. Use when improving
  ugly or flat AppCompat buttons, styling game control bars, primary/CTA/outlined
  actions, or when the user asks for Material button styling on Android views (not
  Jetpack Compose).
---

# Android Material button polish (XML views)

## When to apply

- Replacing plain `Button` / `AppCompatButton` that look flat, wrong inset, or lack feedback.
- Dark UIs where controls need **edge definition** (stroke) without heavy elevation.
- Need **consistent families**: neutral control, accent, CTA, primary filled, outlined secondary.

## Baseline

1. **Theme** app with `Theme.MaterialComponents.DayNight.NoActionBar` (or another `Theme.MaterialComponents.*` without ActionBar). Map existing `colorPrimary`, `colorSecondary` / `colorAccent`, `android:windowBackground`, status/navigation colors as before.
2. **Dependency**: `com.google.android.material:material` (already typical on AndroidX apps).
3. In layouts, use **`com.google.android.material.button.MaterialButton`** with **`style="@style/..."`** — avoid repeating `backgroundTint` + raw `Button` per widget.

## Style families (extend Material parents)

| Role | Parent | Typical items |
|------|--------|----------------|
| Dense icon / grid control | `Widget.MaterialComponents.Button.UnelevatedButton` | `cornerRadius` 12–16dp, `backgroundTint`, `strokeWidth` 1dp, `strokeColor`, `rippleColor`, `android:minHeight`/`minWidth` 0, `insetTop`/`insetBottom` 0, horizontal padding 0 if icons only |
| Accent control (one highlighted key) | Same as control + override `backgroundTint`, `strokeColor`, `android:textColor` | |
| CTA (dominant action in a row) | `UnelevatedButton` | Slightly larger `cornerRadius` (e.g. 18dp), CTA fill + warm stroke |
| Primary screen action | `Widget.MaterialComponents.Button` | `cornerRadius` 16dp, bold label, `android:textAllCaps` false, `letterSpacing` ~0.02, optional `android:elevation` + `android:stateListAnimator` `@null` if you want flat shadow |
| Secondary / shop / ghost | `Widget.MaterialComponents.Button.OutlinedButton` | `strokeWidth` 1–1.5dp, stroke + text same accent, `rippleColor` tinted to accent |

Set **`android:fontFamily`** / **`fontFamily`** on styles if the app uses a bundled font.

## Color tokens

Add dedicated resources (names are illustrative; keep one palette per app):

- **Strokes**: semi-transparent lighter than fill (e.g. `ds_control_stroke`, `ds_cta_stroke`, `ds_primary_stroke`) — use `#AARRGGBB` with ~40–50% opacity on the relevant hue.
- **Ripples**: `ds_ripple_on_dark` (light white alpha) for dark fills; `ds_ripple_cta` (accent alpha) for outlined / orange surfaces.

Keep stroke/ripple out of “main” brand colors so buttons don’t look muddy.

## Layout checklist

- [ ] Root or parent has `xmlns:app` if any `app:*` attributes are used elsewhere (Material attrs are often on the widget via style).
- [ ] Replace `<Button` with `<com.google.android.material.button.MaterialButton` and reference a **style**.
- [ ] Remove duplicate per-button `backgroundTint` / `fontFamily` when the style already sets them.
- [ ] Adjust **margins** between controls (e.g. 6dp) so strokes don’t visually collide.

## Kotlin

`MaterialButton` subclasses `Button`; existing `findViewById<Button>(...)` and **`backgroundTintList`** (e.g. dynamic bomb button) usually work unchanged. Prefer `MaterialButton` in types only when you need Material-only APIs.

## Dialogs

If the app uses `AlertDialog` with a custom theme, prefer **`Theme.MaterialComponents.Dialog.Alert`** (or Material bridge) so button styling matches.

## XML comments

Resource XML uses **`<!-- -->`**. Do not use `/* */` inside `values/*.xml`.

## Reference in this repo

Illustrative implementation: `app/src/main/res/values/styles.xml`, `values/colors.xml` (stroke/ripple tokens), `layout/activity_main.xml`, `values/themes.xml`.
