package com.kylecorry.trail_sense.calibration.ui

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.sensors.NullSensor
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS

class ImproveAccuracyAlerter(private val context: Context) : IValueAlerter<List<ISensor>> {

    private val formatter = FormatService.getInstance(context)
    private val baseDistanceUnits = UserPreferences(context).baseDistanceUnits
    private val titleScale = 1.2f
    private val doneButton = context.getString(android.R.string.ok)

    override fun alert(value: List<ISensor>) {
        val gps = value.firstOrNull { it is IGPS } as? IGPS
        val compass = value.firstOrNull { it is ICompass } as? ICompass
        val hasCompass = compass !is NullSensor
        val content = buildSpannedString {
            gps?.let {
                appendGPS(it)
                append("\n\n")
            }
            compass?.let {
                appendCompass(it)
            }
        }

        Alerts.dialog(
            context,
            context.getString(R.string.accuracy_info_title),
            content,
            contentView = if (hasCompass) CompassCalibrationView.withFrame(
                context,
                height = Resources.dp(context, 200f).toInt()
            ) else null,
            cancelText = null,
            cancelOnOutsideTouch = false,
            okText = doneButton
        )
    }

    private fun SpannableStringBuilder.appendCompass(compass: ICompass) {
        if (compass is NullSensor) {
            return
        }

        title(context.getString(R.string.pref_compass_sensor_title))

        appendLine(context.getString(R.string.calibrate_compass_dialog_content, doneButton))
    }

    private fun SpannableStringBuilder.appendGPS(gps: IGPS) {
        if (gps is OverrideGPS || gps is CachedGPS) {
            return
        }

        title(context.getString(R.string.gps))

        appendLine(context.getString(R.string.gps_accuracy_tip))
        appendLine()

        // Horizontal accuracy
        gps.horizontalAccuracy?.let {
            val accuracyText = context.getString(
                R.string.accuracy_distance_format,
                formatter.formatDistance(Distance.meters(it).convertTo(baseDistanceUnits))
            )
            bold {
                append(context.getString(R.string.gps_location_accuracy) + ": ")
            }
            appendLine(accuracyText)
        }

        // Vertical accuracy
        gps.verticalAccuracy?.let {
            val accuracyText = context.getString(
                R.string.accuracy_distance_format,
                formatter.formatDistance(Distance.meters(it).convertTo(baseDistanceUnits))
            )
            bold {
                append(context.getString(R.string.gps_elevation_accuracy) + ": ")
            }
            appendLine(accuracyText)
        }

        // Satellites
        bold {
            append(context.getString(R.string.gps_satellites) + ": ")
        }
        appendLine((gps.satellites ?: 0).toString())
    }

    private fun SpannableStringBuilder.title(title: String) {
        bold {
            scale(titleScale) {
                appendLine(title)
            }
        }
    }

}