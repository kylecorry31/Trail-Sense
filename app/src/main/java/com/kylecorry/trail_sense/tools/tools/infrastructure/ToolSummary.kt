package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.tools.tools.ui.summaries.ToolSummaryView

data class ToolSummary(
    val id: String,
    val name: String,
    val size: ToolSummarySize = ToolSummarySize.Full,
    val create: (root: FrameLayout, fragment: Fragment) -> ToolSummaryView
)

enum class ToolSummarySize {
    Half,
    Full
}
