# XML Reactive Fragment Conversion

Use this for fragments extending `TrailSenseReactiveFragment(layoutId)`.

## Old Shape

Reactive XML fragments usually put all behavior in `update()`:

- `useView<T>(R.id.some_view)` resolves XML views.
- Shared reactive hooks manage state, effects, resume/pause, and cleanup.
- Effects mutate views directly: visibility, image resources, click listeners, and custom view methods.

## Compose Shape

Convert to:

```kotlin
class ExampleFragment : TrailSenseComposeFragment() {
    @Composable
    override fun FragmentContent() {
        // custom Compose hooks and side effects here
        ExampleContent(
            state = state,
            onAction = { /* update state */ },
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

Keep state/effects in `FragmentContent`, not in the stateless content composable. Import hooks from `com.kylecorry.trail_sense.shared.extensions.compose` and check that package for the preferred hook before using built-in Compose state/effect APIs.

## Mapping Pattern

- `update()` body becomes `FragmentContent()`.
- Hook calls move to the Compose hook equivalents from `com.kylecorry.trail_sense.shared.extensions.compose`
- `useView` is removed. Compose-native widgets become Compose UI. Custom Android views that are not migrated become `AndroidView`.
- View visibility becomes conditional composition: `if (visible) { ... }`.
- `setImageResource`, `setText`, color changes, and similar mutations become parameters that choose Compose properties.
- `setOnClickListener` becomes `onClick`/callback parameters.

Besides defining and interacting with the views in compose, this should be close to a one to one mapping.

## Examples
You can use the ToolMagnifierFragment as an example. View its git history to see how it was converted to compose.
