---
name: trail-sense-compose-fragment-conversion
description: Convert Trail Sense XML fragments to the repo's Compose fragment pattern. Use when migrating BoundFragment, TrailSenseReactiveFragment, XML layouts, view binding code, reactive hooks, or fragment UI tests to TrailSenseComposeFragment and shared Compose hooks.
---

# Trail Sense Compose Fragment Conversion

Convert XML-backed Trail Sense fragments to `TrailSenseComposeFragment`.

## Core Rules

- Preserve behavior and source intent as closely as possible. This is a migration, not a cleanup or redesign.
- Replace `BoundFragment` or `TrailSenseReactiveFragment(layoutId)` with `TrailSenseComposeFragment`.
- Implement `@Composable override fun FragmentContent()` and remove `generateBinding`, `onViewCreated`, XML layout inflation, and `useView` calls.
- Prefer hooks from `com.kylecorry.trail_sense.shared.extensions.compose` over built-in Compose `remember`, `LaunchedEffect`, `DisposableEffect`, or direct lifecycle observers.
- Look up available custom hooks in `app/src/main/java/com/kylecorry/trail_sense/shared/extensions/compose/` instead of relying on memory or listing them in this skill.
- Keep side effects in `FragmentContent`; keep UI rendering in a private stateless composable with state and callbacks passed in.
- Delete the obsolete XML layout after its behavior is represented in Compose.
- Add `Modifier.testTag("<old_view_id_name>")` for interactive or asserted views so tests can use resource-id compatible Compose tags.
- Add previews for the stateless content composable when practical.
- Compose unit tests are not needed.
- A preview of the main content composable should be created.
- Use shared compose views from `app/src/main/java/com/kylecorry/trail_sense/shared/views/compose/` when possible.
- Do not remove existing code comments unless the exact behavior they describe no longer exists. Carry relevant comments forward near the migrated behavior.
- Do not replace imports with fully-qualified names or aliases unless there is a real name conflict that cannot be avoided. Prefer normal imports matching the original style.
- Do not wrap standard Android SDK widgets such as `TextView`, `Button`, or `ImageView` in `AndroidView`. Convert them to Compose primitives. Use helpers such as `annotateWithLinks` when links need to be preserved.
- For repo custom views kept via `AndroidView`, store view instances in `useRef`, not `useState`. Name refs after the old local view variable plus `Ref` (for example `mapDistanceSheetViewRef`), assign `ref.current = this` in the Compose layout factory, and inside effects/callbacks immediately restore the old local name with `val mapDistanceSheetView = mapDistanceSheetViewRef.current ?: return@useEffect`.
- Don't make unrelated stylistic changes to the code. It is better for the diff to be focused on the migration than to also include formatting, import organization, or other cleanups.

## Pick The Reference

- XML reactive fragment: read [xml-reactive.md](references/xml-reactive.md) for `TrailSenseReactiveFragment`, `update()`, `useView`, and existing non-Compose hooks.
- XML bound fragment: read [xml-bound.md](references/xml-bound.md) for `BoundFragment`, view binding, `onViewCreated`, listeners, and direct preference/view mutation.

## Workflow

1. Read the fragment, its XML layout, related tests, and any callbacks other code calls on the fragment.
2. Identify state, side effects, lifecycle cleanup, services/preferences/navigation, and every XML view id used by tests.
3. Map layout relationships before editing: constraints, stacking order, visibility, `gone` behavior, anchored overlays, and which views remain visible when other views hide.
4. Convert the fragment to `TrailSenseComposeFragment` and model state with shared Compose hooks.
5. Build a private content composable using Material/Compose primitives or `AndroidView` for repo **custom** views that still need a View implementation. This should be used sparingly for non-Android SDK views that have consumers other than the one being migrated.
6. Move event listeners into callback parameters on the content composable.
7. Update tests from `R.id.*` view ids to `id("<testTag>")` where the node is Compose-only; use text matchers when they are more stable.
8. Run the smallest relevant compile/test target available.

## Validation Checklist

- Fragment extends `TrailSenseComposeFragment`.
- Imports custom hooks from `shared.extensions.compose`.
- No stale binding, XML layout id, or `useView` usage remains.
- Effects have the same dependencies and cleanup semantics as the old fragment.
- The Compose layout preserves the XML layout's behavioral relationships, including which content shrinks vs overlays when sheets/bars appear.
- Conditional visibility matches the original behavior
- Existing explanatory comments and ordinary imports are preserved where still applicable.
- SDK widgets have been migrated to Compose primitives rather than wrapped in `AndroidView`.
- Retained custom view instances use `useRef` with `*ViewRef` names, not nullable `useState`.
- Tests reference Compose tags for Compose-only nodes.
- Deleted XML has no remaining references.

## Custom Android Views

When a custom defined view has other consumers, use the following approach:

- Create it in `AndroidView(factory = { context -> ... })`.
- Configure stable one-time view properties in `factory`.
- Store the instance with `useRef` if effects need to call methods on it.
- Start/stop or otherwise mutate it from effects in `FragmentContent`.
- Add cleanup so resources are released when the composition leaves.

## Other Notes
- `useBackgroundEffect` can be replaced with `useEffect` as the new composable hook supports suspend functions
