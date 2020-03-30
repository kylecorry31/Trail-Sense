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
import com.kylecorry.trail_sense.shared.formatHM
import com.kylecorry.trail_sense.shared.toDisplayFormat
import com.kylecorry.trail_sense.shared.toZonedDateTime
import com.kylecorry.trail_sense.ui.astronomy.SunChart
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
    private lateinit var sunImg: ImageView

    private lateinit var sunChart: SunChart

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
        sunChart = SunChart(context!!, MpStackedBarChart(view.findViewById(R.id.sun_chart)))

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

    override fun update(o: Observable?, arg: Any?) {
        if (o == gps) {
            location = gps.location
            updateUI()
        }
    }

    private fun updateUI() {
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
        val today = currentTime.toLocalDate()
        val tomorrow = today.plusDays(1)
        val sunTimes = SunTimesCalculatorFactory().getAll().map { it.calculate(location, today) }.toMutableList()
        sunTimes.addAll(SunTimesCalculatorFactory().getAll().map { it.calculate(location, tomorrow) })

        sunChart.display(sunTimes)

        val todayTimes = sunChartCalculator.calculate(location, today)
        val tomorrowTimes = sunChartCalculator.calculate(location, tomorrow)

        displaySunTimes(todayTimes, sunStartTimeTxt, sunMiddleTimeTxt, sunEndTimeTxt)
        displaySunTimes(tomorrowTimes, sunStartTomorrowTimeTxt, sunMiddleTomorrowTimeTxt, sunEndTomorrowTimeTxt)

        displayTimeUntilNextSunEvent(currentTime, todayTimes, tomorrowTimes)
    }

    private fun displayTimeUntilNextSunEvent(currentTime: LocalDateTime, today: SunTimes, tomorrow: SunTimes){
        when {
            currentTime > today.down -> {
                // Time until tomorrow's sunrise
                sunTxt.text = Duration.between(currentTime, tomorrow.up).formatHM()
                remDaylightTxt.text = getString(R.string.until_sunrise_label)
            }
            currentTime < today.up -> {
                // Time until today's sunrise
                sunTxt.text = Duration.between(currentTime, today.up).formatHM()
                remDaylightTxt.text = getString(R.string.until_sunrise_label)
            }
            else -> {
                sunTxt.text = Duration.between(currentTime, today.down).formatHM()
                remDaylightTxt.text = getString(R.string.until_sunset_label)
            }
        }
    }


    private fun displaySunTimes(sunTimes: SunTimes, upTxt: TextView, noonTxt: TextView, downTxt: TextView){
        upTxt.text = sunTimes.up.toDisplayFormat(context!!)
        noonTxt.text = sunTimes.noon.toDisplayFormat(context!!)
        downTxt.text = sunTimes.down.toDisplayFormat(context!!)
    }

}
