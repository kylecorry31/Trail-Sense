---
name: trail-sense-android-tests
description: Add UI automation tests to Trail-Sense Android app using AutomationLibrary. Use when asked to create, add, write, or implement automated tests, UI tests, integration tests, or androidTests for Trail Sense tools. Covers test class structure, AutomationLibrary functions, and testing patterns.
---

# Trail Sense Android Tests

Create UI automation tests for Trail Sense tools using `AutomationLibrary` and `ToolTestBase`.

## Test Structure

```kotlin
package com.kylecorry.trail_sense.tools.<toolname>

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.*
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.*
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class Tool<Name>Test : ToolTestBase(Tools.<TOOL_ID>) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(R.id.title, string(R.string.tool_title))

        canCreateItem()
        canEditItem()
        canDeleteItem()
        verifyQuickAction()
    }

    private fun canCreateItem() { /* ... */ }
    private fun canEditItem() { /* ... */ }
    private fun canDeleteItem() { /* ... */ }
    private fun verifyQuickAction() { /* ... */ }
}
```

**Location**: `app/src/androidTest/java/com/kylecorry/trail_sense/tools/<toolname>/Tool<Name>Test.kt`

## Selection Strategy: Text Over IDs

**Prefer text-based selection** for most interactions. Use IDs only when necessary.

### Use Text For

```kotlin
// Clicking tabs, buttons, menu items, options
click(string(R.string.distance))
click(string(R.string.delete))
click("High")
click("Test Group")

// Verifying text anywhere on screen
hasText(string(R.string.no_paths))
hasText("Test Path")

// Dialog inputs (by label/hint)
input(string(R.string.name), "My Item")
input(string(R.string.distance), "1.0")

// Checkbox state by label
isChecked(string(R.string.tide_clock))
```

### Use IDs Only For

```kotlin
// Title bars (for verification)
hasText(R.id.paths_title, string(R.string.paths))
hasText(R.id.tide_title, "Tide 1")

// Add/play buttons (no text label)
click(R.id.add_btn)
click(R.id.play_btn)

// Specific input fields
input(R.id.searchbox, "query")
input(R.id.tide_name, "Tide 1")
input(R.id.utm, "42, -72")

// Result/data views
hasText(R.id.result, "3.2808 ft")
hasText(R.id.total_percent_packed, "50% packed")

// Charts and special views
isVisible(R.id.chart)
scrollToEnd(R.id.scroll_view)

// Toolbar buttons
click(toolbarButton(R.id.paths_title, Side.Right))
```

### Andromeda List Item IDs

List items use Andromeda library IDs:

```kotlin
click(com.kylecorry.andromeda.views.R.id.title)
hasText(com.kylecorry.andromeda.views.R.id.title, "Item Name")
hasText(com.kylecorry.andromeda.views.R.id.description, "Details")
click(com.kylecorry.andromeda.views.R.id.checkbox)
click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
click(com.kylecorry.andromeda.views.R.id.menu_btn)
```

## Common Patterns

### Create/Edit/Delete Flow

```kotlin
private fun canCreateItem() {
    click(R.id.add_btn)
    click(string(R.string.new_item))
    input(string(R.string.name), "Test Item")
    clickOk()
    hasText("Test Item")
}

private fun canEditItem() {
    clickListItemMenu(string(R.string.edit))
    input("Test Item", "Test Item 2")
    clickOk()
    hasText("Test Item 2")
}

private fun canDeleteItem() {
    clickListItemMenu(string(R.string.delete))
    clickOk()
    not { hasText("Test Item 2", waitForTime = 0) }
}
```

### Toolbar Menu

```kotlin
click(toolbarButton(R.id.title, Side.Right))
click(string(R.string.export))
```

### Quick Actions

```kotlin
private fun verifyQuickAction() {
    TestUtils.openQuickActions()
    click(quickAction(Tools.QUICK_ACTION_ID))
    // Verify action occurred
    TestUtils.closeQuickActions()
}
```

### Optional Elements

```kotlin
optional {
    hasText(string(R.string.disclaimer))
    clickOk()
}
```

### Navigate Back

```kotlin
backUntil { isVisible(R.id.paths_title, waitForTime = 1000) }
```

## Key Functions

| Function | Usage |
|----------|-------|
| `click(text)` | Click by text (contains match) |
| `click(text, exact = true)` | Click by exact text |
| `click(R.id.x)` | Click by ID |
| `hasText(text)` | Verify text on screen |
| `hasText(R.id.x, text)` | Verify text in view |
| `hasText(R.id.x, Regex(...))` | Verify regex pattern |
| `input(R.id.x, text)` | Enter text by ID |
| `input(label, text)` | Enter text by label |
| `clickOk()` | Click OK in dialogs |
| `clickListItemMenu(label)` | Click list item overflow menu |
| `optional { }` | Ignore failures |
| `not { }` | Assert action fails |
| `string(R.string.x)` | Get string resource |
| `scrollUntil { }` | Scroll until condition |
| `backUntil { }` | Press back until condition |

## Running Tests

```bash
./gradlew connectedDebugAndroidTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.kylecorry.trail_sense.tools.notes.ToolNotesTest
```

## Source Files

For complete API details and additional functions, read:
- `app/src/androidTest/java/com/kylecorry/trail_sense/test_utils/AutomationLibrary.kt`
- `app/src/androidTest/java/com/kylecorry/trail_sense/test_utils/TestUtils.kt`
- `app/src/androidTest/java/com/kylecorry/trail_sense/test_utils/views/`

For example tests, see existing tool tests in:
- `app/src/androidTest/java/com/kylecorry/trail_sense/tools/*/Tool*Test.kt`
