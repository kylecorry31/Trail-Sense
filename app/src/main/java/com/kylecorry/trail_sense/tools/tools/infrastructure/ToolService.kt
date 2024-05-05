package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import java.time.Duration

data class ToolService(
    val name: String,
    val getFrequency: (context: Context) -> Duration,
    val isActive: (context: Context) -> Boolean,
    /**
     * Disable the service (will not run again until enabled)
     */
    val disable: (context: Context) -> Unit,
    /**
     * Stop the service temporarily (will restart when the app is opened again)
     */
    val stop: (context: Context) -> Unit,
    /**
     * Restart the service if it is enabled
     */
    val restart: suspend (context: Context) -> Unit
)