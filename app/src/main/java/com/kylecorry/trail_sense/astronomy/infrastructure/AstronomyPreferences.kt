package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode

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

}