## Build and Test Commands

- Build debug APK: `./gradlew assembleDebug`
- Run all unit tests: `./gradlew testDebugUnitTest`
- Run a single test class: `./gradlew testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.weather.WeatherPredictionTest"`
- Run a single test method: `./gradlew testDebugUnitTest --tests "com.kylecorry.trail_sense.tools.weather.WeatherPredictionTest.testPrediction"`
- Run connected Android tests (requires emulator/device): `./gradlew connectedDebugAndroidTest`
- Build release APK: `./gradlew assembleRelease`
- Lint check: `./gradlew lint`

## Architecture Overview

Trail Sense is an offline Android wilderness survival app built with Kotlin. The codebase follows a modular tool-based architecture.

### Tool System

Each feature is a self-contained "tool" located in `app/src/main/java/com/kylecorry/trail_sense/tools/`. Tools are registered via `ToolRegistration` objects in `Tools.kt`.

A tool registration defines:
- Navigation destinations (fragments)
- Quick actions
- Home screen widgets
- Background services
- Notification channels
- Volume button actions
- Diagnostic scanners
- Map layers

Example: `FlashlightToolRegistration.kt` shows a complete tool with service, widget, quick actions, and volume controls.

### Key Directories

- `tools/` - Feature modules (navigation, weather, astronomy, beacons, paths, etc.)
- `shared/` - Common utilities, sensors, preferences, map layers
- `main/` - MainActivity, AppDatabase, application initialization
- `settings/` - User preferences fragments and repos
- `onboarding/` - First-run setup flow

### Data Layer

- **Room Database**: `AppDatabase.kt` defines all entities and DAOs. Migrations are inline in the companion object.
- **Preferences**: User settings accessed via `UserPreferences.kt` which delegates to feature-specific preference repos.
- **Subsystems**: Singletons providing access to sensors, location, files, notifications (`*Subsystem.kt` files).

### Map Layers

The map system supports two layer types in `shared/map_layers/`:
- **Tile layers**: `TileMapLayer` with `TileSource` implementations for raster map tiles
- **GeoJSON layers**: `GeoJsonLayer` with `GeoJsonSource` for vector features (points, lines, polygons)

Layer definitions are registered via `MapLayerDefinition` in tool registrations.

### Navigation

Single-activity architecture using Jetpack Navigation. Fragments are defined in `res/navigation/nav_graph.xml`. Tools specify their `navAction` ID in `ToolRegistration`.

### Background Services

Long-running features (backtrack, weather monitor, pedometer) use `ToolService` implementations that integrate with Android's foreground service system. Services are managed via `TrailSenseServiceUtils`.

### Formatting

Formatting is done using `FormatService` which respects user preferences for units, precision, and localization.

### Dependency Injection

Dependency injection is manual via the `AppServiceRegistry` singleton. Singleton services are registered and retrieved as needed.

### UI Framework

View Binding is used for most fragment layouts, but it is being gradually replaced with a custom reactive UI framework (`TrailSenseReactiveFragment`) which uses React like hooks (`useEffect`, `useMemo`, `useState`, etc).

### External Libraries

- **Andromeda**: In-house Android utility library for sensors, permissions, UI components, etc.
- **Sol**: Science and math calculations
- **Luna**: Hooks and reactive topics for state management

## Testing

- Unit tests: `app/src/test/java/` - Use JUnit 5 with `@Test` annotations
- Android tests: `app/src/androidTest/java/` - UI automation tests using UI Automator with a custom helper library (`AutomationLibrary`).
- Algorithm tests should verify against trusted external sources or hand calculations

## Guide
Each tool has a user guide located in `guides/en-US/guide_tool_<toolname>.txt` that should be updated with any feature change.

## Project Constraints

- **No Internet**: The app must work entirely offline
- **APK size**: Keep under 10 MB
- **Science-backed**: Features must be based on peer-reviewed research or verified against real-world data
- **Sensor-focused**: Prefer using phone sensors over stored guides/data
- **Minimum SDK:** 23 (Android 6.0)
- **Target SDK:** 36 (Android 16)
