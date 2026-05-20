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
- Delete the obsolete XML layout after its behavior is represented in Compose.
- Add `Modifier.testTag("<old_view_id_name>")` for interactive or asserted views so tests can use resource-id compatible Compose tags.
- Add previews for the stateless content composable when practical.
- Compose unit tests are not needed.
- A preview of the main content composable should be created.

## Pick The Reference

- XML reactive fragment: read [xml-reactive.md](references/xml-reactive.md) for `TrailSenseReactiveFragment`, `update()`, `useView`, and existing non-Compose hooks.
- XML bound fragment: read [xml-bound.md](references/xml-bound.md) for `BoundFragment`, view binding, `onViewCreated`, listeners, and direct preference/view mutation.

## Workflow

1. Read the fragment, its XML layout, related tests, and any callbacks other code calls on the fragment.
2. Identify state, side effects, lifecycle cleanup, services/preferences/navigation, and every XML view id used by tests.
3. Convert the fragment to `TrailSenseComposeFragment` and model state with shared Compose hooks.
4. Build a private content composable using Material/Compose primitives or `AndroidView` for repo custom views that still need a View implementation.
5. Move event listeners into callback parameters on the content composable.
6. Update tests from `R.id.*` view ids to `id("<testTag>")` where the node is Compose-only; use text matchers when they are more stable.
7. Run the smallest relevant compile/test target available.

## Validation Checklist

- Fragment extends `TrailSenseComposeFragment`.
- Imports custom hooks from `shared.extensions.compose`.
- No stale binding, XML layout id, or `useView` usage remains.
- Effects have the same dependencies and cleanup semantics as the old fragment.
- Tests reference Compose tags for Compose-only nodes.
- Deleted XML has no remaining references.

## Custom Android Views

When keeping a repo View such as a camera/preview/custom drawing view:

- Create it in `AndroidView(factory = { context -> ... })`.
- Configure stable one-time view properties in `factory`.
- Store the instance with a custom Compose state hook if effects need to call methods on it.
- Start/stop or otherwise mutate it from effects in `FragmentContent`.
- Add cleanup so resources are released when the composition leaves.