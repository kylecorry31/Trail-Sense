package com.kylecorry.trail_sense.astronomy.ui

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.domain.moon.MoonTruePhase
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trail_sense.shared.sensors.IGPS
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

class AstronomyFragment : Fragment() {

    private lateinit var gps: IGPS

    private lateinit var sunTxt: TextView
    private lateinit var remDaylightTxt: TextView
    private lateinit var moonTxt: TextView
    private lateinit var timer: Timer
    private lateinit var handler: Handler
    private lateinit var moonPosition: ImageView
    private lateinit var sunPosition: ImageView
    private lateinit var moonPositionArrow: ImageView
    private lateinit var moonIndicatorCircle: ImageView

    private lateinit var prevDateBtn: ImageButton
    private lateinit var nextDateBtn: ImageButton
    private lateinit var dateTxt: TextView
    private lateinit var chart: AstroChart

    private lateinit var displayDate: LocalDate

    private lateinit var sunTimesMode: SunTimesMode

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val astronomyService = AstronomyService()

    private lateinit var riseSetTimes: List<Pair<TextView, TextView>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_astronomy, container, false)


        riseSetTimes = listOf(
            Pair(view.findViewById(R.id.sun_start), view.findViewById(R.id.sun_start_time)),
            Pair(view.findViewById(R.id.sun_end), view.findViewById(R.id.sun_end_time)),
            Pair(view.findViewById(R.id.moon_start), view.findViewById(R.id.moon_rise_time)),
            Pair(view.findViewById(R.id.moon_end), view.findViewById(R.id.moon_set_time))
        )

        sunTxt = view.findViewById(R.id.remaining_time)
        moonTxt = view.findViewById(R.id.moon_phase)
        remDaylightTxt = view.findViewById(R.id.remaining_time_lbl)
        sunPosition = view.findViewById(R.id.sun_position)
        moonPosition = view.findViewById(R.id.moon_position)
        moonPositionArrow = view.findViewById(R.id.moon_position_arrow)
        moonIndicatorCircle = view.findViewById(R.id.moon_indicator_circle)

        dateTxt = view.findViewById(R.id.date)
        nextDateBtn = view.findViewById(R.id.next_date)
        prevDateBtn = view.findViewById(R.id.prev_date)

        chart = AstroChart(view.findViewById(R.id.moonChart))

        prevDateBtn.setOnClickListener {
            displayDate = displayDate.minusDays(1)
            updateUI()
        }

        nextDateBtn.setOnClickListener {
            displayDate = displayDate.plusDays(1)
            updateUI()
        }

        gps = GPS(requireContext())

        sunTimesMode = prefs.astronomy.sunTimesMode

        return view
    }

    override fun onResume() {
        super.onResume()
        displayDate = LocalDate.now()
        gps.start(this::onLocationUpdate)
        handler = Handler(Looper.getMainLooper())
        timer = fixedRateTimer(period = 1000 * 60) {
            handler.post { updateUI() }
        }
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        gps.stop(this::onLocationUpdate)
        timer.cancel()
    }

    private fun onLocationUpdate(): Boolean {
        updateUI()
        return false
    }

    private fun updateUI() {
        if (context == null) {
            return
        }
        dateTxt.text = getDateString(displayDate)
        updateSunUI()
        updateMoonUI()
        updateAstronomyChart()
        updateRiseSetTimes()
    }

    private fun updateMoonUI() {
        if (context == null) {
            return
        }

        val moonPhase = astronomyService.getCurrentMoonPhase()

        moonPosition.setImageResource(getMoonImage(moonPhase.phase))
        moonTxt.text = "${moonPhase.phase.longName} (${moonPhase.illumination.roundToInt()}%)"
    }

    private fun updateAstronomyChart() {
        if (context == null) {
            return
        }

        val altitudes = astronomyService.getTodayMoonAltitudes(gps.location)
        val sunAltitudes = astronomyService.getTodaySunAltitudes(gps.location)

        println(astronomyService.getSunAzimuth(gps.location).value)

        val current = altitudes.minBy { Duration.between(LocalDateTime.now(), it.time).abs() }
        val currentIdx = altitudes.indexOf(current)

        chart.plot(
            listOf(
                AstroChart.AstroChartDataset(altitudes, resources.getColor(R.color.white, null)),
                AstroChart.AstroChartDataset(
                    sunAltitudes,
                    resources.getColor(R.color.colorPrimary, null)
                )
            )
        )

        val point = chart.getPoint(1, currentIdx)
        moonPosition.x = point.first - moonPosition.width / 2f
        moonPosition.y = point.second - moonPosition.height / 2f
        moonIndicatorCircle.x = point.first - moonIndicatorCircle.width / 2f
        moonIndicatorCircle.y = point.second - moonIndicatorCircle.height / 2f

        val point2 = chart.getPoint(2, currentIdx)
        sunPosition.x = point2.first - sunPosition.width / 2f
        sunPosition.y = point2.second - sunPosition.height / 2f

        if (isVerticallyOverlapping(moonPosition, sunPosition)) {
            moonIndicatorCircle.visibility = View.VISIBLE
            if (moonPosition.y > chart.y + chart.height / 2f) {
                moonPositionArrow.rotation = 0f
                align(
                    moonPositionArrow,
                    null,
                    HorizontalConstraint(sunPosition, HorizontalConstraintType.Left),
                    VerticalConstraint(sunPosition, VerticalConstraintType.Top),
                    HorizontalConstraint(sunPosition, HorizontalConstraintType.Right)
                )
                align(
                    moonPosition,
                    null,
                    HorizontalConstraint(moonPositionArrow, HorizontalConstraintType.Left),
                    VerticalConstraint(moonPositionArrow, VerticalConstraintType.Top),
                    HorizontalConstraint(moonPositionArrow, HorizontalConstraintType.Right)
                )
                moonPositionArrow.visibility = View.VISIBLE
            } else {
                moonPositionArrow.rotation = 180f
                align(
                    moonPositionArrow,
                    VerticalConstraint(sunPosition, VerticalConstraintType.Bottom),
                    HorizontalConstraint(sunPosition, HorizontalConstraintType.Left),
                    null,
                    HorizontalConstraint(sunPosition, HorizontalConstraintType.Right)
                )
                align(
                    moonPosition,
                    VerticalConstraint(moonPositionArrow, VerticalConstraintType.Bottom),
                    HorizontalConstraint(moonPositionArrow, HorizontalConstraintType.Left),
                    null,
                    HorizontalConstraint(moonPositionArrow, HorizontalConstraintType.Right)
                )
                moonPositionArrow.visibility = View.VISIBLE
            }
        } else {
            moonPositionArrow.visibility = View.INVISIBLE
            moonIndicatorCircle.visibility = View.INVISIBLE
        }

    }

    private fun isVerticallyOverlapping(first: View, second: View): Boolean {
        if (first.y > second.y && first.y > second.y + second.height) {
            return false
        }

        if (second.y > first.y && second.y > first.y + first.height) {
            return false
        }

        return true
    }

    private fun updateSunUI() {
        if (context == null) {
            return
        }

        displayTimeUntilNextSunEvent()
    }

    private fun updateRiseSetTimes() {
        if (context == null) {
            return
        }

        val sunTimes = astronomyService.getSunTimes(gps.location, sunTimesMode, displayDate)
        val moonTimes = astronomyService.getMoonTimes(gps.location, displayDate)

        val times = listOf(
            Pair(getSunriseWording(), sunTimes.up),
            Pair(getSunsetWording(), sunTimes.down),
            Pair(getString(R.string.moon_rise), moonTimes.up),
            Pair(getString(R.string.moon_set), moonTimes.down)
        ).sortedBy { it.second }

        times.forEachIndexed { index, time ->
            riseSetTimes[index].first.text = time.first
            riseSetTimes[index].second.text = getTimeString(time.second)
        }
    }

    private fun getMoonImage(phase: MoonTruePhase): Int {
        return when (phase) {
            MoonTruePhase.FirstQuarter -> R.drawable.moon_first_quarter
            MoonTruePhase.Full -> R.drawable.moon_full
            MoonTruePhase.ThirdQuarter -> R.drawable.moon_last_quarter
            MoonTruePhase.New -> R.drawable.moon_new
            MoonTruePhase.WaningCrescent -> R.drawable.moon_waning_crescent
            MoonTruePhase.WaningGibbous -> R.drawable.moon_waning_gibbous
            MoonTruePhase.WaxingCrescent -> R.drawable.moon_waxing_crescent
            MoonTruePhase.WaxingGibbous -> R.drawable.moon_waxing_gibbous
        }
    }

    private fun displayTimeUntilNextSunEvent() {
        val currentTime = LocalDateTime.now()
        val nextSunrise = astronomyService.getNextSunrise(gps.location, sunTimesMode)
        val nextSunset = astronomyService.getNextSunset(gps.location, sunTimesMode)

        if (nextSunrise != null && (nextSunset == null || nextSunrise.isBefore(nextSunset))) {
            sunTxt.text = Duration.between(currentTime, nextSunrise).formatHM()
            remDaylightTxt.text = getString(
                R.string.until_sun_time, getSunriseWording().toLowerCase(
                    getLocale()
                )
            )
        } else if (nextSunset != null) {
            sunTxt.text = Duration.between(currentTime, nextSunset).formatHM()
            remDaylightTxt.text = getString(
                R.string.until_sun_time, getSunsetWording().toLowerCase(
                    getLocale()
                )
            )
        } else if (astronomyService.isSunUp(gps.location)) {
            sunTxt.text = getString(R.string.sun_up_no_set)
            remDaylightTxt.text = getString(R.string.sun_does_not_set)
        } else {
            sunTxt.text = getString(R.string.sun_down_no_set)
            remDaylightTxt.text = getString(R.string.sun_does_not_rise)
        }
    }

    private fun getLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            resources.configuration.locale
        }
    }

    private fun getTimeString(time: LocalDateTime?): String {
        return time?.toDisplayFormat(requireContext()) ?: "-"
    }

    private fun getDateString(date: LocalDate): String {
        val now = LocalDate.now()
        return when {
            date == now -> {
                getString(R.string.today)
            }
            date == now.plusDays(1) -> {
                getString(R.string.tomorrow)
            }
            date == now.minusDays(1) -> {
                getString(R.string.yesterday)
            }
            date.year == now.year -> {
                date.format(DateTimeFormatter.ofPattern(getString(R.string.this_year_format)))
            }
            else -> {
                date.format(DateTimeFormatter.ofPattern(getString(R.string.other_year_format)))
            }
        }
    }

    private fun getSunsetWording(): String {
        return when (sunTimesMode) {
            SunTimesMode.Actual -> getString(R.string.sunset_label)
            SunTimesMode.Civil -> getString(R.string.sun_civil)
            SunTimesMode.Nautical -> getString(R.string.sun_nautical)
            SunTimesMode.Astronomical -> getString(R.string.sun_astronomical)
        }
    }

    private fun getSunriseWording(): String {
        return when (sunTimesMode) {
            SunTimesMode.Actual -> getString(R.string.sunrise_label)
            SunTimesMode.Civil -> getString(R.string.sun_civil)
            SunTimesMode.Nautical -> getString(R.string.sun_nautical)
            SunTimesMode.Astronomical -> getString(R.string.sun_astronomical)
        }
    }

}
