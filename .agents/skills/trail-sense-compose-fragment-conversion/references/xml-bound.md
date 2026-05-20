# XML Bound Fragment Conversion

Use this for fragments extending `BoundFragment<SomeBinding>`.

## Old Shape

Bound XML fragments usually have:

- `generateBinding(...)` inflating a binding class.
- `onViewCreated(...)` wiring click/progress listeners and initial view state.
- Lifecycle overrides such as `onResume`/`onPause`.
- A fixed interval update call.

## Compose Shape

Convert to:

```kotlin
class ExampleFragment : TrailSenseComposeFragment() {
    @Composable
    override fun FragmentContent() {
        // services, preferences, state, and effects via custom Compose hooks
        ExampleContent(
            state = state,
            onAction = setState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

Remove binding generation and `onViewCreated`.

## Mapping Pattern

- Binding fields become Compose UI nodes or state values.
- Initial view setup becomes initial state, derived values, or an effect.
- Listener setup becomes callback parameters.
- `onResume`/`onPause` side effects become shared Compose lifecycle hooks unless the base class behavior itself must be overridden.
- The fixed interval update call can likely go away unless used for something like a countdown timer, at which point `useTimer` should be used. That call was needed to update the UI when state changed, but that is handled by Compose now.


## Examples
You can use the FragmentToolScreenFlashlight as an example. View its git history to see how it was converted to compose.