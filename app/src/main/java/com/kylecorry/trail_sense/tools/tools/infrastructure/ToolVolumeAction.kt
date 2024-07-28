package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction

data class ToolVolumeAction(
    val priority: ToolVolumeActionPriority,
    val isActive: (context: Context, isToolOpen: Boolean, fragment: AndromedaFragment) -> Boolean,
    val create: (fragment: AndromedaFragment) -> VolumeAction
)