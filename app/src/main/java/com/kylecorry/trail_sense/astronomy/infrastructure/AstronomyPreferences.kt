package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode
import java.time.LocalDate

class AstronomyPreferences(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)


    val sunTimesMode: SunTimesMode
        get() {
            return when(prefs.getString(context.getString(R.string.pref_sun_time_mode), "actual")){
                "actual" -> SunTimesMode.Actual
                "civil" -> SunTimesMode.Civil
                "nautical" -> SunTimesMode.Nautical
                "astronomical" -> SunTimesMode.Astronomical
                else -> SunTimesMode.Actual
            }
        }

    val centerSunAndMoon: Boolean
        get(){
            return prefs.getBoolean(context.getString(R.string.pref_center_sun_and_moon), false)
        }

    val showOnCompass: Boolean
        get(){
            val raw = prefs.getString(context.getString(R.string.pref_show_sun_moon_compass), "never")
            return raw == "always" || raw == "when_up"
        }

    val showOnCompassWhenDown: Boolean
        get() {
            val raw = prefs.getString(context.getString(R.string.pref_show_sun_moon_compass), "never")
            return raw == "always"
        }

    val sendSunsetAlerts: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_sunset_alerts), true)

    val sunsetAlertMinutesBefore: Long
        get() {
            return prefs.getString(context.getString(R.string.pref_sunset_alert_time), "60")?.toLong() ?: 60
        }

    val sunsetAlertLastSent: LocalDate
        get() {
            val raw = prefs.getString("sunset_alert_last_sent_date", LocalDate.MIN.toString()) ?: LocalDate.MIN.toString()
            return LocalDate.parse(raw)
        }

    fun setSunsetAlertLastSentDate(date: LocalDate){
        prefs.edit {
            putString("sunset_alert_last_sent_date", date.toString())
        }
    }

}