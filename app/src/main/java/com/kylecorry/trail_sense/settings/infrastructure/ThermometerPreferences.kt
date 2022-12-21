package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toFloatCompat
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.thermometer.ITemperatureCalibrator
import com.kylecorry.trail_sense.shared.sensors.thermometer.RangeTemperatureCalibrator
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource

class ThermometerPreferences(context: Context) : PreferenceRepo(context), IThermometerPreferences {
    override val source by StringEnumPreference(
        cache, getString(R.string.pref_thermometer_source),
        mapOf(
            "historic" to ThermometerSource.Historic,
            "sensor" to ThermometerSource.Sensor
        ),
        ThermometerSource.Historic
    )

    override var smoothing: Float
        get() {
            return (cache.getInt(context.getString(R.string.pref_temperature_smoothing))
                ?: 0) / 1000f
        }
        set(value) {
            val scaled = (value * 1000).coerceIn(0f, 1000f)
            cache.putInt(
                context.getString(R.string.pref_temperature_smoothing),
                scaled.toInt()
            )
        }

    override var minBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_c))
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_uncalibrated_temp_c),
                value.toString()
            )
        }

    override var minActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_c))
            ?.toFloatCompat() ?: 0f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_calibrated_temp_c),
                value.toString()
            )
        }

    override var maxBatteryTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_c))
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_uncalibrated_temp_c),
                value.toString()
            )
        }

    override var maxActualTemperature: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_c))
            ?.toFloatCompat() ?: 100f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_calibrated_temp_c),
                value.toString()
            )
        }

    override var minBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_uncalibrated_temp_f))
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_uncalibrated_temp_f),
                value.toString()
            )
        }

    override var minActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_min_calibrated_temp_f))
            ?.toFloatCompat() ?: 32f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_min_calibrated_temp_f),
                value.toString()
            )
        }

    override var maxBatteryTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_uncalibrated_temp_f))
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_uncalibrated_temp_f),
                value.toString()
            )
        }

    override var maxActualTemperatureF: Float
        get() = cache.getString(context.getString(R.string.pref_max_calibrated_temp_f))
            ?.toFloatCompat() ?: 212f
        set(value) {
            cache.putString(
                context.getString(R.string.pref_max_calibrated_temp_f),
                value.toString()
            )
        }
    override val calibrator: ITemperatureCalibrator
        get() = RangeTemperatureCalibrator(
            minBatteryTemperature,
            maxBatteryTemperature,
            minActualTemperature,
            maxActualTemperature
        )

    override fun resetThermometerCalibration() {
        minActualTemperature = 0f
        minActualTemperatureF = 32f
        maxActualTemperature = 100f
        maxActualTemperatureF = 212f
        minBatteryTemperature = 0f
        minBatteryTemperatureF = 32f
        maxBatteryTemperature = 100f
        maxBatteryTemperatureF = 212f
    }
}