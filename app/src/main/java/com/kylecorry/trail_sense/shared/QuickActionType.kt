package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.andromeda.core.capitalizeWords
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

enum class QuickActionType(val id: Int) {
    None(-1),
    Paths(0),
    Flashlight(1),
    Clouds(2),
    Temperature(3),
    Ruler(5),
    Maps(7),
    Whistle(8),
    WhiteNoise(9),
    LowPowerMode(10),
    Thunder(11),
    Climate(12),
    SunsetAlert(13),
    NightMode(14),
    Backtrack(15),
    WeatherMonitor(16)
}

object QuickActionUtils {

    fun getName(context: Context, quickActionType: QuickActionType): String {
        return when (quickActionType) {
            QuickActionType.None -> context.getString(R.string.none)
            QuickActionType.Paths -> context.getString(R.string.paths)
            QuickActionType.Flashlight -> context.getString(R.string.flashlight_title)
            QuickActionType.Clouds -> context.getString(R.string.clouds)
            QuickActionType.Temperature -> context.getString(R.string.tool_temperature_estimation_title)
            QuickActionType.Ruler -> context.getString(R.string.tool_ruler_title)
            QuickActionType.Maps -> context.getString(R.string.photo_maps)
            QuickActionType.Whistle -> context.getString(R.string.tool_whistle_title)
            QuickActionType.WhiteNoise -> context.getString(R.string.tool_white_noise_title)
            QuickActionType.LowPowerMode -> context.getString(R.string.pref_low_power_mode_title)
            QuickActionType.Thunder -> context.getString(R.string.tool_lightning_title)
            QuickActionType.Climate -> context.getString(R.string.tool_climate)
            QuickActionType.SunsetAlert -> context.getString(R.string.sunset_alerts)
            QuickActionType.NightMode -> context.getString(R.string.night)
            QuickActionType.Backtrack -> context.getString(R.string.backtrack)
            QuickActionType.WeatherMonitor -> context.getString(R.string.weather_monitor)
        }.capitalizeWords()
    }

    fun tools(context: Context): List<QuickActionType> {
        return listOfNotNull(
            if (FlashlightSubsystem.getInstance(context).isAvailable()) QuickActionType.Flashlight else null,
            QuickActionType.Whistle,
            QuickActionType.LowPowerMode,
            QuickActionType.SunsetAlert,
            QuickActionType.WhiteNoise,
            QuickActionType.NightMode,
            QuickActionType.Backtrack,
            QuickActionType.WeatherMonitor
        )
    }

    fun navigation(context: Context): List<QuickActionType> {
        val list = mutableListOf(
            QuickActionType.None,
            QuickActionType.Paths,
            if (FlashlightSubsystem.getInstance(context).isAvailable()) QuickActionType.Flashlight else null,
            QuickActionType.Whistle,
            QuickActionType.Ruler,
            QuickActionType.LowPowerMode,
            QuickActionType.Maps,
            QuickActionType.NightMode,
            QuickActionType.Backtrack)

        return list.filterNotNull()
    }

    fun weather(context: Context): List<QuickActionType> {
        return listOfNotNull(
            QuickActionType.None,
            if (FlashlightSubsystem.getInstance(context).isAvailable()) QuickActionType.Flashlight else null,
            QuickActionType.Whistle,
            QuickActionType.Clouds,
            QuickActionType.Temperature,
            QuickActionType.LowPowerMode,
            QuickActionType.Thunder,
            QuickActionType.Climate,
            QuickActionType.NightMode,
            QuickActionType.WeatherMonitor,
        )
    }

    fun astronomy(context: Context): List<QuickActionType> {
        return listOfNotNull(
            QuickActionType.None,
            if (FlashlightSubsystem.getInstance(context).isAvailable()) QuickActionType.Flashlight else null,
            QuickActionType.Whistle,
            QuickActionType.WhiteNoise,
            QuickActionType.LowPowerMode,
            QuickActionType.SunsetAlert,
            QuickActionType.NightMode
        )
    }
}