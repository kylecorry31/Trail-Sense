package com.kylecorry.trail_sense.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.sensors.gps.GPS
import java.util.*
import com.kylecorry.trail_sense.astronomy.Moon
import com.kylecorry.trail_sense.astronomy.Sun
import com.kylecorry.trail_sense.models.Coordinate
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AstronomyFragment : Fragment(), Observer {

    private lateinit var gps: GPS
    private lateinit var location: Coordinate
    private var gotLocation = false

    private lateinit var sunTxt: TextView
    private lateinit var sunTypeLbl: TextView
    private lateinit var remDaylightTxt: TextView
    private lateinit var moonTxt: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_astronomy, container, false)

        sunTxt = view.findViewById(R.id.sunrise_sunset)
        moonTxt = view.findViewById(R.id.moon_phase)
        sunTypeLbl = view.findViewById(R.id.sun_label)
        remDaylightTxt = view.findViewById(R.id.remaining_daylight)

        gps = GPS(context!!)
        location = gps.location

        return view
    }

    override fun onResume() {
        super.onResume()
        if (!gotLocation) {
            gps.addObserver(this)
            gps.start()
        }
    }

    override fun onPause() {
        super.onPause()
        gps.stop()
        gps.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (context == null) return
        if (o == gps){
            gotLocation = true
            location = gps.location
            gps.stop()
            updateUI()
        }
    }

    fun updateUI(){
        val sunrise = Sun.getSunrise(location)
        val sunset = Sun.getSunset(location)

        val currentTime = LocalTime.now()

        when {
            currentTime > sunset -> {
                val tomorrowSunrise = Sun.getSunrise(location, ZonedDateTime.now().plusDays(1))
                sunTypeLbl.text = getString(R.string.sunrise_label)
                sunTxt.text = formatTime(tomorrowSunrise)
                remDaylightTxt.text = ""
            }
            currentTime < sunrise -> {
                // Show today's sunrise
                sunTypeLbl.text = getString(R.string.sunrise_label)
                sunTxt.text = formatTime(sunrise)
                remDaylightTxt.text = ""
            }
            else -> {
                // Show today's sunset
                sunTypeLbl.text = getString(R.string.sunset_label)
                sunTxt.text = formatTime(sunset)
                val remainingDaylight = Duration.between(LocalTime.now(), sunset)
                remDaylightTxt.text = "${formatDuration(remainingDaylight)} of daylight left"
            }
        }


        val moonPhase = Moon.getPhase()

        moonTxt.text = moonPhase.longName

        // TODO: Calculate moon rise / set times


    }

    private fun formatTime(time: LocalTime): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val use24Hr = prefs.getBoolean(getString(R.string.pref_use_24_hour), false)

        return if (use24Hr){
            time.format(DateTimeFormatter.ofPattern("H:mm"))
        } else {
            time.format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return if (hours == 0L){
            "${minutes}m"
        } else {
            "${hours}h ${minutes}m"
        }

    }

}
