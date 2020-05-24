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

    val showOnCompass: Boolean
        get() = prefs.getBoolean(context.getString(R.string.pref_show_sun_moon_compass), false)

}