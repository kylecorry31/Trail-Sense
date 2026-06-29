---
name: trail-sense-scaffold-quick-action
description: Scaffold a Trail Sense quick action with class, persisted Tools id, registration, string, and icon resources.
---

# Trail Sense Scaffold Quick Action

Add a quick action following Trail Sense conventions.

## Workflow

1. Identify the owning tool package, `Tools.<TOOL_ID>`, tool registration file, icon drawable, display string, and desired click/long-click behavior. This step is complete when every placeholder in the class and registration snippets below has a concrete value.
2. Search existing quick actions before editing. This step is complete when the new action does not duplicate an existing action and the next `Tools.QUICK_ACTION_*` id is known.
3. Create the quick action class under the path below. This step is complete when the class name, package, icon, click behavior, and long-click behavior match the requested action.

```text
app/src/main/java/com/kylecorry/trail_sense/tools/<tool_package>/quickactions/QuickAction<Name>.kt
```

4. Add a new unique `Tools.QUICK_ACTION_<NAME>` constant at the end of `Tools.kt`, using the next available integer. This step is complete when the new id is greater than existing quick-action ids and no existing id changed.
5. Register the action in the owning `<ToolName>ToolRegistration.kt` with `ToolQuickAction`. This step is complete when the registration references the new id, display string, and quick action constructor.
6. Add or reuse string and icon resources as needed. The scaffold is complete when the class compiles, the id is unique, the action appears in the owning tool registration, and no existing ids were renumbered.

## Quick Action Class

Use this template for what the new quick action scaffold should look like:

```kotlin
package com.kylecorry.trail_sense.tools.<tool_package>.quickactions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickAction<Name>(btn: QuickActionButtonView, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.<icon>)
    }

    override fun onClick() {
        super.onClick()
        // Do nothing right now
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.<TOOL_ID>)
        return true
    }
}
```

Fill in the placeholders, but don't add any logic.

## Tool Registration

In the owning `<ToolName>ToolRegistration.kt`:

```kotlin
import com.kylecorry.trail_sense.tools.<tool_package>.quickactions.QuickAction<Name>
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
```

Add or extend `quickActions` in the `Tool(...)` constructor:

```kotlin
quickActions = listOf(
    ToolQuickAction(
        Tools.QUICK_ACTION_<NAME>,
        context.getString(R.string.<display_string>),
        ::QuickAction<Name>
    )
),
```

## Tools.kt Id

Add the new constant near the other quick-action constants:

```kotlin
const val QUICK_ACTION_<NAME> = <next_available_int>
```

Do not renumber existing ids. Quick-action ids are persisted in user preferences.
