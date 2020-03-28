package com.kylecorry.trail_sense.ui

import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.sensors.gps.GPS
import java.util.*
import com.kylecorry.trail_sense.astronomy.Moon
import com.kylecorry.trail_sense.astronomy.Sun
import com.kylecorry.trail_sense.models.Coordinate
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

class AstronomyFragment : Fragment(), Observer {

    private lateinit var gps: GPS
    private lateinit var location: Coordinate
    private var gotLocation = false

    private lateinit var sunTxt: TextView
    private lateinit var remDaylightTxt: TextView
    private lateinit var moonTxt: TextView
    private lateinit var moonImg: ImageView
    private lateinit var sunProgress: ProgressBar
    private lateinit var sunStartTxt: TextView
    private lateinit var sunMiddleTxt: TextView
    private lateinit var sunEndTxt: TextView
    private lateinit var sunStartTimeTxt: TextView
    private lateinit var sunMiddleTimeTxt: TextView
    private lateinit var sunEndTimeTxt: TextView
    private lateinit var timer: Timer
    private lateinit var handler: Handler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_astronomy, container, false)

        sunTxt = view.findViewById(R.id.remaining_time)
        moonTxt = view.findViewById(R.id.moon_phase)
        moonImg = view.findViewById(R.id.moon_phase_img)
        remDaylightTxt = view.findViewById(R.id.remaining_time_lbl)
        sunProgress = view.findViewById(R.id.sun_bar)
        sunStartTxt = view.findViewById(R.id.sun_start)
        sunMiddleTxt = view.findViewById(R.id.sun_middle)
        sunEndTxt = view.findViewById(R.id.sun_end)
        sunStartTimeTxt = view.findViewById(R.id.sun_start_time)
        sunMiddleTimeTxt = view.findViewById(R.id.sun_middle_time)
        sunEndTimeTxt = view.findViewById(R.id.sun_end_time)

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
        handler = Handler(Looper.getMainLooper())
        timer = fixedRateTimer(period = 1000 * 60){
            handler.post { updateUI() }
        }

    }

    override fun onPause() {
        super.onPause()
        gps.stop()
        gps.deleteObserver(this)
        timer.cancel()
        sunProgress.visibility = View.INVISIBLE
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
        updateSunUI()
        updateMoonUI()
    }

    private fun updateMoonUI(){
        val moonPhase = Moon.getPhase()
        moonTxt.text = moonPhase.longName

        val moonImgId = when(moonPhase) {
            Moon.Phase.FirstQuarter -> R.drawable.moon_first_quarter
            Moon.Phase.Full -> R.drawable.moon_full
            Moon.Phase.LastQuarter -> R.drawable.moon_last_quarter
            Moon.Phase.New -> R.drawable.moon_new
            Moon.Phase.WaningCrescent -> R.drawable.moon_waning_crescent
            Moon.Phase.WaningGibbous -> R.drawable.moon_waning_gibbous
            Moon.Phase.WaxingCrescent -> R.drawable.moon_waxing_crescent
            Moon.Phase.WaxingGibbous -> R.drawable.moon_waxing_gibbous
        }

        moonImg.setImageResource(moonImgId)

        // TODO: Calculate moon rise / set times
    }

    private fun updateSunUI(){
        val sunrise = Sun.getSunrise(location)
        val sunset = Sun.getSunset(location)

        val currentTime = LocalDateTime.now()

        when {
            currentTime > sunset -> {
                // Time until tomorrow's sunrise
                val tomorrowSunrise = Sun.getSunrise(location, ZonedDateTime.now().plusDays(1))
                setNightProgress(sunset, currentTime, tomorrowSunrise)
            }
            currentTime < sunrise -> {
                // Time until today's sunrise
                val yesterdaySunset = Sun.getSunset(location, ZonedDateTime.now().minusDays(1))
                setNightProgress(yesterdaySunset, currentTime, sunrise)
            }
            else -> {
                setDayProgress(sunrise, currentTime, sunset)
            }
        }

        sunProgress.visibility = View.VISIBLE
    }

    private fun setNightProgress(sunset: LocalDateTime, current: LocalDateTime, sunrise: LocalDateTime){
        sunStartTxt.text = getString(R.string.sunset_label)
        sunEndTxt.text = getString(R.string.sunrise_label)
        sunMiddleTxt.text = getString(R.string.midnight_label)
        remDaylightTxt.text = getString(R.string.until_sunrise_label)

        sunProgress.progressDrawable.setTint(resources.getColor(R.color.night, null))

        setSunProgress(sunset, current, sunrise)
    }

    private fun setDayProgress(sunrise: LocalDateTime, current: LocalDateTime, sunset: LocalDateTime){
        sunStartTxt.text = getString(R.string.sunrise_label)
        sunEndTxt.text = getString(R.string.sunset_label)
        sunMiddleTxt.text = getString(R.string.noon_label)
        remDaylightTxt.text = getString(R.string.until_sunset_label)

        sunProgress.progressDrawable.setTint(resources.getColor(R.color.day, null))

        setSunProgress(sunrise, current, sunset)
    }


    private fun setSunProgress(start: LocalDateTime, current: LocalDateTime, end: LocalDateTime){
        val totalTime = Duration.between(start, end)
        val timeRemaining = Duration.between(current, end)
        sunProgress.progress = ((timeRemaining.seconds / totalTime.seconds.toFloat()) * 100).roundToInt()

        sunStartTimeTxt.text = formatTime(start)
        sunMiddleTimeTxt.text = formatTime(start.plus(totalTime.dividedBy(2)))
        sunEndTimeTxt.text = formatTime(end)

        sunTxt.text = formatDuration(timeRemaining)
    }


    private fun formatTime(time: LocalDateTime): String {
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
