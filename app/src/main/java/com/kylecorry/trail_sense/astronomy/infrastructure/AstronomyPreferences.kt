package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.astronomy.SunTimesMode
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import java.time.LocalDate

class AstronomyPreferences(private val context: Context) {

    private val cache by lazy { Cache(context) }

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

    val sendSunsetAlerts: Boolean
        get() = cache.getBoolean(context.getString(R.string.pref_sunset_alerts)) ?: true

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
        get() = true

    val astronomicalSeasons: Boolean
        get() = true

    fun setSunsetAlertLastSentDate(date: LocalDate) {
        cache.putString("sunset_alert_last_sent_date", date.toString())
    }

}