package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS

class ImproveAccuracyAlerter(
    private val context: Context,
    private val customMessage: CharSequence? = null
) : IValueAlerter<List<ISensor>> {

    private val formatter = FormatService.getInstance(context)
    private val baseDistanceUnits = UserPreferences(context).baseDistanceUnits
    private val titleScale = 1.2f
    private val doneButton = context.getString(android.R.string.ok)

    override fun alert(value: List<ISensor>) {
        val gps = value.firstOrNull { it is ISatelliteGPS } as? ISatelliteGPS
        val compass = value.firstOrNull { it is ICompass } as? ICompass
        val orientation = value.firstOrNull { it is IOrientationSensor } as? IOrientationSensor
        val hasCompass = compass !is MockSensor
        val content = buildSpannedString {
            gps?.let {
                appendGPS(it)
                append("\n\n")
            }
            compass?.let {
                appendCompass(it)
            }
            orientation?.let {
                appendCompass(it)
            }
            customMessage?.let {
                append("\n\n")
                append(it)
            }
        }

        Alerts.dialog(
            context,
            context.getString(R.string.accuracy_info_title),
            content,
            contentView = if (hasCompass) CompassCalibrationView.sized(
                context,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Resources.dp(context, 200f).toInt()
            ) else null,
            cancelText = null,
            cancelOnOutsideTouch = false,
            okText = doneButton,
            scrollable = true
        )
    }

    private fun SpannableStringBuilder.appendCompass(compass: ISensor) {
        if (compass is MockSensor) {
            return
        }

        title(context.getString(R.string.pref_compass_sensor_title))

        appendLine(context.getString(R.string.calibrate_compass_dialog_content, doneButton))
    }

    private fun SpannableStringBuilder.appendGPS(gps: ISatelliteGPS) {
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