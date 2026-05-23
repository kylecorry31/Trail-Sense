package com.kylecorry.trail_sense.tools.ai_assistant.domain

import android.graphics.Bitmap

data class AiContext(
    val toolId: String,
    val toolName: String,
    val sensorData: Map<String, Any>,
    val image: Bitmap?,
    val summary: String
)
