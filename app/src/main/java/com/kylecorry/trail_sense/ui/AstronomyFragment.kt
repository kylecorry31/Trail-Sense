package com.kylecorry.trail_sense.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.moon.MoonPhaseCalculator
import com.kylecorry.trail_sense.astronomy.moon.MoonTruePhase
import com.kylecorry.trail_sense.astronomy.sun.*
import com.kylecorry.trail_sense.shared.sensors.gps.GPS
import java.util.*
import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.toZonedDateTime
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

class AstronomyFragment : Fragment(), Observer {

    private lateinit var gps: GPS
    private lateinit var location: Coordinate

    private lateinit var sunTxt: TextView
    private lateinit var remDaylightTxt: TextView
    private lateinit var moonTxt: TextView
    private lateinit var moonImg: ImageView
    private lateinit var sunStartTimeTxt: TextView
    private lateinit var sunMiddleTimeTxt: TextView
    private lateinit var sunEndTimeTxt: TextView
    private lateinit var sunStartTomorrowTimeTxt: TextView
    private lateinit var sunMiddleTomorrowTimeTxt: TextView
    private lateinit var sunEndTomorrowTimeTxt: TextView
    private lateinit var timer: Timer
    private lateinit var handler: Handler
    private lateinit var sunChart: IStackedBarChart
    private lateinit var sunImg: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_astronomy, container, false)

        sunTxt = view.findViewById(R.id.remaining_time)
        moonTxt = view.findViewById(R.id.moon_phase)
        moonImg = view.findViewById(R.id.moon_phase_img)
        remDaylightTxt = view.findViewById(R.id.remaining_time_lbl)
        sunStartTimeTxt = view.findViewById(R.id.sun_start_time)
        sunMiddleTimeTxt = view.findViewById(R.id.sun_middle_time)
        sunEndTimeTxt = view.findViewById(R.id.sun_end_time)
        sunStartTomorrowTimeTxt = view.findViewById(R.id.sun_start_time_tomorrow)
        sunMiddleTomorrowTimeTxt = view.findViewById(R.id.sun_middle_time_tomorrow)
        sunEndTomorrowTimeTxt = view.findViewById(R.id.sun_end_time_tomorrow)
        sunImg = view.findViewById(R.id.sun_img)
        sunChart = MpStackedBarChart(view.findViewById(R.id.sun_chart))

        gps = GPS(context!!)
        location = gps.location

        return view
    }

    override fun onResume() {
        super.onResume()
        gps.addObserver(this)
        gps.updateLocation {}
        handler = Handler(Looper.getMainLooper())
        timer = fixedRateTimer(period = 1000 * 60) {
            handler.post { updateUI() }
        }

    }

    override fun onPause() {
        super.onPause()
        gps.deleteObserver(this)
        timer.cancel()
    }

    fun updateUI() {
        updateSunUI()
        updateMoonUI()
    }

    private fun updateMoonUI() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val showCurrentMoonPhase =
            prefs.getBoolean(getString(R.string.pref_show_current_moon_phase), false)

        val time = if (showCurrentMoonPhase) {
            ZonedDateTime.now()
        } else {
            LocalDate.now().atTime(LocalTime.MAX).toZonedDateTime()
        }

        val moonPhase = MoonPhaseCalculator().getPhase(time)
        moonTxt.text =
            "${moonPhase.phase.longName} (${moonPhase.illumination.roundToInt()}% illumination)"

        val moonImgId = when (moonPhase.phase) {
            MoonTruePhase.FirstQuarter -> R.drawable.moon_first_quarter
            MoonTruePhase.Full -> R.drawable.moon_full
            MoonTruePhase.ThirdQuarter -> R.drawable.moon_last_quarter
            MoonTruePhase.New -> R.drawable.moon_new
            MoonTruePhase.WaningCrescent -> R.drawable.moon_waning_crescent
            MoonTruePhase.WaningGibbous -> R.drawable.moon_waning_gibbous
            MoonTruePhase.WaxingCrescent -> R.drawable.moon_waxing_crescent
            MoonTruePhase.WaxingGibbous -> R.drawable.moon_waxing_gibbous
        }

        moonImg.setImageResource(moonImgId)

        // TODO: Calculate moon rise / set times
    }

    private fun updateSunUI() {
        val sunChartCalculator = SunTimesCalculatorFactory().create(context!!)

        val currentTime = LocalDateTime.now()
        val currentDate = currentTime.toLocalDate()
        val suntimes = sunChartCalculator.calculate(location, currentDate)

        populateSunChart()

        val sunrise = suntimes.up
        val sunset = suntimes.down

        displayTodaySunTimes()
        displayTomorrowSunTimes()

        when {
            currentTime > sunset -> {
                // Time until tomorrow's sunrise
                val tomorrowSunrise =
                    sunChartCalculator.calculate(location, currentDate.plusDays(1)).up
                sunTxt.text = formatDuration(Duration.between(currentTime, tomorrowSunrise))
                remDaylightTxt.text = getString(R.string.until_sunrise_label)
            }
            currentTime < sunrise -> {
                // Time until today's sunrise
                sunTxt.text = formatDuration(Duration.between(currentTime, sunrise))
                remDaylightTxt.text = getString(R.string.until_sunrise_label)
            }
            else -> {
                sunTxt.text = formatDuration(Duration.between(currentTime, sunset))
                remDaylightTxt.text = getString(R.string.until_sunset_label)
            }
        }
    }

    private fun getAllSunTimes(date: LocalDate): List<LocalDateTime> {
        val actual = ActualTwilightCalculator().calculate(location, date)
        val civil = CivilTwilightCalculator().calculate(location, date)
        val nautical = NauticalTwilightCalculator().calculate(location, date)
        val astronomical = AstronomicalTwilightCalculator().calculate(location, date)

        return listOf(
            astronomical.up,
            nautical.up,
            civil.up,
            actual.up,
            actual.down,
            civil.down,
            nautical.down,
            astronomical.down
        )

    }

    private fun displayTodaySunTimes() {
        val now = LocalDate.now()
        val sunTimes = SunTimesCalculatorFactory().create(context!!).calculate(location, now)

        sunStartTimeTxt.text = formatTime(sunTimes.up)
        sunMiddleTimeTxt.text = formatTime(SunTimes.getPeakTime(sunTimes.up, sunTimes.down))
        sunEndTimeTxt.text = formatTime(sunTimes.down)
    }

    private fun displayTomorrowSunTimes() {
        val tomorrow = LocalDate.now().plusDays(1)
        val sunTimes = SunTimesCalculatorFactory().create(context!!).calculate(location, tomorrow)

        sunStartTomorrowTimeTxt.text = formatTime(sunTimes.up)
        sunMiddleTomorrowTimeTxt.text = formatTime(SunTimes.getPeakTime(sunTimes.up, sunTimes.down))
        sunEndTomorrowTimeTxt.text = formatTime(sunTimes.down)
    }

    private fun populateSunChart() {
        val currentTime = LocalDateTime.now()
        val currentDate = currentTime.toLocalDate()
        val today = getAllSunTimes(currentDate)
        val tomorrow = getAllSunTimes(currentDate.plusDays(1))

        val sunTimes = today.toMutableList()
        sunTimes.addAll(tomorrow)

        val maxDuration = Duration.ofHours(14)

        val timesUntil = sunTimes
            .map { Duration.between(currentTime, it) }
            .map { if (it <= maxDuration) it else maxDuration }
            .map { if (it.isNegative) 0 else it.seconds }

        val sunEventDurations = mutableListOf<Long>()

        var cumulativeTime = 0L

        for (time in timesUntil) {
            val dt = time - cumulativeTime
            sunEventDurations.add(dt)
            cumulativeTime += dt
        }

        val colors = mutableListOf(
            R.color.night,
            R.color.astronomical_twilight,
            R.color.nautical_twilight,
            R.color.civil_twilight,
            R.color.day,
            R.color.civil_twilight,
            R.color.nautical_twilight,
            R.color.astronomical_twilight
        )

        colors.addAll(colors)

        sunChart.plot(sunEventDurations, colors.map { resources.getColor(it, null) })
    }

    private fun formatTime(time: LocalDateTime): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val use24Hr = prefs.getBoolean(getString(R.string.pref_use_24_hour), false)

        return if (use24Hr) {
            time.format(DateTimeFormatter.ofPattern("H:mm"))
        } else {
            time.format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return when {
            hours == 0L -> "${minutes}m"
            minutes == 0L -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o == gps) {
            location = gps.location
            updateUI()
        }
    }

}
