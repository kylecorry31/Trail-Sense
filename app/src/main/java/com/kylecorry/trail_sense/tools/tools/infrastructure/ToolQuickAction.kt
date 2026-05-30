package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.QuickActionButton

data class ToolQuickAction(
    val id: Int,
    val name: String,
    val create: (button: QuickActionButtonView, fragment: Fragment) -> QuickActionButton
)
