package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.flashlight.Flashlight

enum class QuickActionType(val id: Int) {
    None(-1),
    Backtrack(0),
    Flashlight(1),
    Clouds(2),
    Temperature(3),
    Ruler(5),
    Maps(7),
    Whistle(8),
    WhiteNoise(9),
    LowPowerMode(10)
}

object QuickActionUtils {

    fun getName(context: Context, quickActionType: QuickActionType): String {
        return when (quickActionType) {
            QuickActionType.None -> context.getString(R.string.quick_action_none)
            QuickActionType.Backtrack -> context.getString(R.string.tool_backtrack_title)
            QuickActionType.Flashlight -> context.getString(R.string.flashlight_title)
            QuickActionType.Clouds -> context.getString(R.string.clouds)
            QuickActionType.Temperature -> context.getString(R.string.pref_temperature_holder_title)
            QuickActionType.Ruler -> context.getString(R.string.tool_ruler_title)
            QuickActionType.Maps -> context.getString(R.string.offline_maps)
            QuickActionType.Whistle -> context.getString(R.string.tool_whistle_title)
            QuickActionType.WhiteNoise -> context.getString(R.string.tool_white_noise_title)
            QuickActionType.LowPowerMode -> context.getString(R.string.pref_low_power_mode_title)
        }
    }

    fun navigation(context: Context): List<QuickActionType> {
        val list = mutableListOf(
            QuickActionType.None,
            QuickActionType.Backtrack,
            if (Flashlight.hasFlashlight(context)) QuickActionType.Flashlight else null,
            QuickActionType.Whistle,
            QuickActionType.Ruler,
            QuickActionType.LowPowerMode
        )

        if (UserPreferences(context).experimentalEnabled) {
            list.add(QuickActionType.Maps)
        }

        return list.filterNotNull()
    }

    fun weather(context: Context): List<QuickActionType> {
        return listOfNotNull(
            QuickActionType.None,
            if (Flashlight.hasFlashlight(context)) QuickActionType.Flashlight else null,
            QuickActionType.Whistle,
            QuickActionType.Clouds,
            QuickActionType.Temperature,
            QuickActionType.LowPowerMode
        )
    }

    fun astronomy(context: Context): List<QuickActionType> {
        return listOfNotNull(
            QuickActionType.None,
            if (Flashlight.hasFlashlight(context)) QuickActionType.Flashlight else null,
            QuickActionType.Whistle,
            QuickActionType.WhiteNoise,
            QuickActionType.LowPowerMode
        )
    }
}