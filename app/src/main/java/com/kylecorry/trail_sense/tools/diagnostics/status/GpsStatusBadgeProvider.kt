package com.kylecorry.trail_sense.tools.diagnostics.status

import android.content.Context
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.InactiveGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS

class GpsStatusBadgeProvider(private val gps: ISatelliteGPS, private val context: Context) :
    StatusBadgeProvider {

    private val formatter = FormatService.getInstance(context)
    private val prefs = UserPreferences(context)

    override fun getBadge(): StatusBadge {
        return StatusBadge(getName(), getColor(), R.drawable.satellite)
    }

    @ColorInt
    private fun getColor(): Int {
        if (gps is OverrideGPS) {
            return AppColor.Green.color
        }

        if (gps is InactiveGPS || !GPS.isAvailable(context)) {
            return AppColor.Red.color
        }

        if (isSearching()) {
            return AppColor.Yellow.color
        }

        return CustomUiUtils.getQualityColor(gps.quality)
    }

    private fun getName(): String {
        if (gps is OverrideGPS) {
            return context.getString(R.string.gps_user)
        }

        if (gps is InactiveGPS || !GPS.isAvailable(context)) {
            return context.getString(R.string.unavailable)
        }

        if (isSearching()) {
            return context.getString(R.string.gps_searching)
        }

        return formatter.formatQuality(gps.quality)
    }

    private fun isSearching(): Boolean {
        val isTimedOut = gps is CustomGPS && gps.isTimedOut
        return getAppService<LocationSubsystem>().isLocationStale || !gps.hasValidReading || isTimedOut
    }
}
