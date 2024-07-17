package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context

// TODO: Indicate required parameters, so it can be matched with a broadcast
data class ToolAction(
    val id: String,
    val name: String,
    val action: Action,
    val isEnabled: (context: Context) -> Boolean = { true }
)