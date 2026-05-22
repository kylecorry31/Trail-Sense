---
name: trail-sense-compose-fragment-conversion
description: Convert Trail Sense XML fragments to the repo's Compose fragment pattern. Use when migrating BoundFragment, TrailSenseReactiveFragment, XML layouts, view binding code, reactive hooks, or fragment UI tests to TrailSenseComposeFragment and shared Compose hooks.
---

# Trail Sense Compose Fragment Conversion

Convert XML-backed Trail Sense fragments to `TrailSenseComposeFragment`.

## Core Rules

- Replace `BoundFragment` or `TrailSenseReactiveFragment(layoutId)` with `TrailSenseComposeFragment`.
- Implement `@Composable override fun FragmentContent()` and remove `generateBinding`, `onViewCreated`, XML layout inflation, and `useView` calls.
- Prefer hooks from `com.kylecorry.trail_sense.shared.extensions.compose` over built-in Compose `remember`, `LaunchedEffect`, `DisposableEffect`, or direct lifecycle observers.
- Look up available custom hooks in `app/src/main/java/com/kylecorry/trail_sense/shared/extensions/compose/` instead of relying on memory or listing them in this skill.
- Keep side effects in `FragmentContent`; keep UI rendering in a private stateless composable with state and callbacks passed in.
- Prefer `useService<T>()` for app services, preferences, and formatters that are registered with the app service registry; only use `useMemo(context) { ...getInstance(context) }` for objects that are not available as services.
- Prefer `useBackgroundMemo(...)` for async loading and async derived values that feed UI state; reserve `useState` + `useEffect` for mutable UI state or side effects that are not simply producing a value.
- Delete the obsolete XML layout after its behavior is represented in Compose.
- Add `Modifier.testTag("<old_view_id_name>")` for interactive or asserted views so tests can use resource-id compatible Compose tags.
- Add previews for the stateless content composable when practical.
- Compose unit tests are not needed.
- A preview of the main content composable should be created.
- Use shared compose views from `app/src/main/java/com/kylecorry/trail_sense/shared/views/compose/` when possible.

## Pick The Reference

- XML reactive fragment: read [xml-reactive.md](references/xml-reactive.md) for `TrailSenseReactiveFragment`, `update()`, `useView`, and existing non-Compose hooks.
- XML bound fragment: read [xml-bound.md](references/xml-bound.md) for `BoundFragment`, view binding, `onViewCreated`, listeners, and direct preference/view mutation.

## Workflow

1. Read the fragment, its XML layout, related tests, and any callbacks other code calls on the fragment.
2. Identify state, side effects, lifecycle cleanup, services/preferences/navigation, and every XML view id used by tests.
3. Convert the fragment to `TrailSenseComposeFragment` and model state with shared Compose hooks:
   - Read arguments with `useArgument`.
   - Read navigation with `useNavController`.
   - Read registered dependencies with `useService`.
   - Load repository/service values with `useBackgroundMemo`.
   - Compute cheap derived display values with `useMemo`.
4. Build a private content composable using Material/Compose primitives or `AndroidView` for repo **custom** views that still need a View implementation. This should be used sparingly for non-Android SDK views that have consumers other than the one being migrated.
5. Move event listeners into callback parameters on the content composable.
6. Update tests from `R.id.*` view ids to `id("<testTag>")` where the node is Compose-only; use text matchers when they are more stable.
7. Run the smallest relevant compile/test target available.

## Validation Checklist

- Fragment extends `TrailSenseComposeFragment`.
- Imports custom hooks from `shared.extensions.compose`.
- Registered dependencies use `useService` instead of manually constructing singletons.
- Async UI values that are just loaded data use `useBackgroundMemo` instead of manual state/effect plumbing.
- No stale binding, XML layout id, or `useView` usage remains.
- Effects have the same dependencies and cleanup semantics as the old fragment.
- Tests reference Compose tags for Compose-only nodes.
- Deleted XML has no remaining references.

## Custom Android Views

When a custom defined view has other consumers, use the following approach:

- Create it in `AndroidView(factory = { context -> ... })`.
- Configure stable one-time view properties in `factory`.
- Store the instance with a custom Compose state hook if effects need to call methods on it.
- Start/stop or otherwise mutate it from effects in `FragmentContent`.
- Add cleanup so resources are released when the composition leaves.

## Other Notes
- `useBackgroundEffect` can be replaced with `useEffect` as the new composable hook supports suspend functions
- For XML `GridLayout` sections of repeated data points, prefer a Compose grid such as `LazyVerticalGrid(GridCells.Fixed(...))` with `Arrangement.spacedBy(...)` over hand-built row chunking.
- For icon-only Material actions that replace small XML `MaterialButton`s, prefer `IconButton` with Material icon-button defaults (for example filled colors/shape) when that better matches the old filled button affordance.
