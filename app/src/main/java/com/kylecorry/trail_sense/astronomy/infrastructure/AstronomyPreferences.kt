package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.toIntCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionType
import java.time.LocalDate
import java.time.LocalTime

class AstronomyPreferences(private val context: Context) {

    private val cache by lazy { Preferences(context) }

    val showMoonIllumination: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_moon_illumination)) ?: false

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

    val sendAstronomyAlerts: Boolean
        get() {
            return sendLunarEclipseAlerts || sendMeteorShowerAlerts
        }

    // TODO: Let the user set this
    var astronomyAlertTime: LocalTime = LocalTime.of(10, 0)

    val sendLunarEclipseAlerts by BooleanPreference(
        cache,
        context.getString(R.string.pref_send_lunar_eclipse_alerts),
        false
    )

    val sendMeteorShowerAlerts by BooleanPreference(
        cache,
        context.getString(R.string.pref_send_meteor_shower_alerts),
        false
    )

    val sunsetAlertMinutesBefore: Long
        get() {
            return (cache.getString(context.getString(R.string.pref_sunset_alert_time))
                ?: "60").toLong()
        }

    val sunsetAlertLastSent: LocalDate
        get() {
            val raw = (cache.getString("sunset_alert_last_sent_date") ?: LocalDate.MIN.toString())
            return LocalDate.parse(raw)
        }

    val showMeteorShowers: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_meteor_showers)) ?: true

    val showLunarEclipses by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_lunar_eclipses),
        true
    )

    val showCivilTimes: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_civil_times)) ?: true

    val showNauticalTimes: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_nautical_times)) ?: false

    val showAstronomicalTimes: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_astronomical_times)) ?: false

    val showNoon: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_show_noon)) ?: true

    fun setSunsetAlertLastSentDate(date: LocalDate) {
        cache.putString("sunset_alert_last_sent_date", date.toString())
    }

    val leftQuickAction: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_astronomy_quick_action_left))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id }
                ?: QuickActionType.Flashlight
        }

    val rightQuickAction: QuickActionType
        get() {
            val id = cache.getString(context.getString(R.string.pref_astronomy_quick_action_right))
                ?.toIntCompat()
            return QuickActionType.values().firstOrNull { it.id == id }
                ?: QuickActionType.WhiteNoise
        }

}