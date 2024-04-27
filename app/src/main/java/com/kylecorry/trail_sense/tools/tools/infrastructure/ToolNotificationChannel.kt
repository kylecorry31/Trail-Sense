package com.kylecorry.trail_sense.tools.tools.infrastructure

data class ToolNotificationChannel(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int,
    val muteSound: Boolean = false,
    val showBadge: Boolean = true
)