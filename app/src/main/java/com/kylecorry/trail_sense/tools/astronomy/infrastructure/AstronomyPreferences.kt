package com.kylecorry.trail_sense.tools.astronomy.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.LocalDate
import java.time.LocalTime

class AstronomyPreferences(private val context: Context) {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    val sunTimesMode: SunTimesMode
        get() {
            return when (cache.getString(context.getString(R.string.pref_sun_time_mode))) {
                "civil" -> SunTimesMode.Civil
                "nautical" -> SunTimesMode.Nautical
                "astronomical" -> SunTimesMode.Astronomical
                else -> SunTimesMode.Actual
            }
        }

    val centerSunAndMoon: Boolean
        get() {
            return cache.getBoolean(context.getString(R.string.pref_center_sun_and_moon)) ?: false
        }

    val showOnCompass: Boolean
        get() {
            val raw =
                cache.getString(context.getString(R.string.pref_show_sun_moon_compass)) ?: "never"
            return raw == "always" || raw == "when_up"
        }

    val showOnCompassWhenDown: Boolean
        get() {
            val raw =
                cache.getString(context.getString(R.string.pref_show_sun_moon_compass)) ?: "never"
            return raw == "always"
        }

    var sendSunsetAlerts by BooleanPreference(
        cache,
        context.getString(R.string.pref_sunset_alerts),
        false
    )

    var sendSunriseAlerts by BooleanPreference(
        cache,
        context.getString(R.string.pref_sunrise_alerts),
        false
    )

    val sendAstronomyAlerts: Boolean
        get() {
            return sendLunarEclipseAlerts || sendMeteorShowerAlerts || sendSolarEclipseAlerts
        }

    // TODO: Let the user set this
    var astronomyAlertTime: LocalTime = LocalTime.of(10, 0)

    var sendLunarEclipseAlerts by BooleanPreference(
        cache,
        context.getString(R.string.pref_send_lunar_eclipse_alerts),
        false
    )

    var sendSolarEclipseAlerts by BooleanPreference(
        cache,
        context.getString(R.string.pref_send_solar_eclipse_alerts),
        false
    )

    var sendMeteorShowerAlerts by BooleanPreference(
        cache,
        context.getString(R.string.pref_send_meteor_shower_alerts),
        false
    )

    val sunsetAlertMinutesBefore: Long
        get() {
            return (cache.getString(context.getString(R.string.pref_sunset_alert_time))
                ?: "60").toLong()
        }
    val sunriseAlertMinutesBefore: Long
        get() {
            return (cache.getString(context.getString(R.string.pref_sunrise_alert_time))
                ?: "0").toLong()
        }

    val sunsetAlertLastSent: LocalDate
        get() {
            val raw = (cache.getString(context.getString(R.string.pref_sunset_alert_last_sent_date))
                ?: LocalDate.MIN.toString())
            return LocalDate.parse(raw)
        }
    val sunriseAlertLastSent: LocalDate
        get() {
            val raw =
                (cache.getString(context.getString(R.string.pref_sunrise_alert_last_sent_date))
                    ?: LocalDate.MIN.toString())
            return LocalDate.parse(raw)
        }

    fun setSunsetAlertLastSentDate(date: LocalDate) {
        cache.putString(
            context.getString(R.string.pref_sunset_alert_last_sent_date),
            date.toString()
        )
    }

    fun setSunriseAlertLastSentDate(date: LocalDate) {
        cache.putString(
            context.getString(R.string.pref_sunrise_alert_last_sent_date),
            date.toString()
        )
    }

    val leftButton: Int
        get() {
            val id = cache.getString(context.getString(R.string.pref_astronomy_quick_action_left))
                ?.toIntCompat()
            return id ?: Tools.QUICK_ACTION_FLASHLIGHT
        }

    val rightButton: Int
        get() {
            val id = cache.getString(context.getString(R.string.pref_astronomy_quick_action_right))
                ?.toIntCompat()
            return id ?: Tools.QUICK_ACTION_SUNSET_ALERT
        }

    val startCameraIn3DView by BooleanPreference(
        cache,
        context.getString(R.string.pref_start_camera_in_3d_view),
        true
    )

    // Alarms
    val useAlarmForSunsetAlert by BooleanPreference(
        cache,
        context.getString(R.string.pref_astronomy_use_alarm_for_sunset_alert),
        false
    )

    val useAlarmForSunriseAlert by BooleanPreference(
        cache,
        context.getString(R.string.pref_astronomy_use_alarm_for_sunrise_alert),
        false
    )

}