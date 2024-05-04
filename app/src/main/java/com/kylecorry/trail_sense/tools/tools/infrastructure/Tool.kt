package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.shared.data.Identifiable

data class Tool(
    override val id: Long,
    val name: String,
    @DrawableRes val icon: Int,
    @IdRes val navAction: Int,
    val category: ToolCategory,
    val description: String? = null,
    val guideId: Int? = null,
    val isExperimental: Boolean = false,
    @IdRes val settingsNavAction: Int? = null,
    val quickActions: List<ToolQuickAction> = emptyList(),
    val additionalNavigationIds: List<Int> = emptyList(),
    val volumeActions: List<ToolVolumeAction> = emptyList(),
    val tiles: List<String> = emptyList(),
    val notificationChannels: List<ToolNotificationChannel> = emptyList(),
    val services: List<ToolService> = emptyList(),
    val diagnostics2: List<ToolDiagnostic2> = emptyList(),
    val isAvailable: (context: Context) -> Boolean = { true },
) : Identifiable {
    fun isOpen(currentNavId: Int): Boolean {
        return navAction == currentNavId || additionalNavigationIds.contains(currentNavId)
    }
}

