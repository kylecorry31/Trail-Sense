package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import java.time.Duration

data class ToolService(
    val name: String,
    val getFrequency: (context: Context) -> Duration,
    val isActive: (context: Context) -> Boolean,
    val disable: (context: Context) -> Unit
)