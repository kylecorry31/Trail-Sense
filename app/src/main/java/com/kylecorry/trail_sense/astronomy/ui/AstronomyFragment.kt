package com.kylecorry.trail_sense.astronomy.ui

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
import com.kylecorry.trail_sense.astronomy.domain.DateUtils
import com.kylecorry.trail_sense.astronomy.domain.moon.*
import com.kylecorry.trail_sense.astronomy.domain.sun.ISunTimesCalculator
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimes
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesCalculatorFactory
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.align
import com.kylecorry.trail_sense.shared.math.getPercentOfDuration
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trail_sense.shared.sensors.IGPS
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

class AstronomyFragment : Fragment() {

    private lateinit var gps: IGPS
    private lateinit var location: Coordinate

    private lateinit var sunTxt: TextView
    private lateinit var remDaylightTxt: TextView
    private lateinit var moonTxt: TextView
    private lateinit var sunStartTimeTxt: TextView
    private lateinit var sunEndTimeTxt: TextView
    private lateinit var moonRiseTimeTxt: TextView
    private lateinit var moonSetTimeTxt: TextView
    private lateinit var timer: Timer
    private lateinit var handler: Handler
    private lateinit var moonPosition: ImageView
    private lateinit var sunPosition: ImageView
    private lateinit var dayCircle: ImageView
    private lateinit var moonIconClock: IconClock
    private lateinit var sunIconClock: IconClock

    private lateinit var prevDateBtn: ImageButton
    private lateinit var nextDateBtn: ImageButton
    private lateinit var dateTxt: TextView

    private lateinit var displayDate: LocalDate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_astronomy, container, false)

        sunTxt = view.findViewById(R.id.remaining_time)
        moonTxt = view.findViewById(R.id.moon_phase)
        remDaylightTxt = view.findViewById(R.id.remaining_time_lbl)
        sunStartTimeTxt = view.findViewById(R.id.sun_start_time)
        sunEndTimeTxt = view.findViewById(R.id.sun_end_time)

        moonRiseTimeTxt = view.findViewById(R.id.moon_rise_time)
        moonSetTimeTxt = view.findViewById(R.id.moon_set_time)

        sunPosition = view.findViewById(R.id.sun_position)
        moonPosition = view.findViewById(R.id.moon_position)
        dayCircle = view.findViewById(R.id.day_circle)
        moonIconClock = IconClock(dayCircle, moonPosition)
        sunIconClock = IconClock(dayCircle, sunPosition)

        dateTxt = view.findViewById(R.id.date)
        nextDateBtn = view.findViewById(R.id.next_date)
        prevDateBtn = view.findViewById(R.id.prev_date)

        prevDateBtn.setOnClickListener {
            displayDate = displayDate.minusDays(1)
            updateUI()
        }

        nextDateBtn.setOnClickListener {
            displayDate = displayDate.plusDays(1)
            updateUI()
        }

        gps = GPS(requireContext())

        return view
    }

    override fun onResume() {
        super.onResume()
        location = gps.location
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
        location = gps.location
        updateUI()
        return false
    }

    private fun updateUI() {
        dateTxt.text = getDateString(displayDate)
        updateSunUI()
        updateMoonUI()
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

    private fun updateMoonUI() {
        if (context == null) {
            return
        }

        val time = ZonedDateTime.now()

        val moonPhase = MoonPhaseCalculator().getPhase(time)
        val calculator = AltitudeMoonTimesCalculator()

        val today = calculator.calculate(gps.location, displayDate)

        moonRiseTimeTxt.text = today.up?.toDisplayFormat(requireContext()) ?: "-"
        moonSetTimeTxt.text = today.down?.toDisplayFormat(requireContext()) ?: "-"

        moonPosition.setImageResource(getMoonImage(moonPhase.phase))

        moonTxt.text = "${moonPhase.phase.direction} (${moonPhase.illumination.roundToInt()}%)"

        updateMoonPosition(LocalDateTime.now(), calculator)

        align(moonTxt,
            VerticalConstraint(moonPosition, VerticalConstraintType.Bottom),
            HorizontalConstraint(moonPosition, HorizontalConstraintType.Left),
            null,
            HorizontalConstraint(moonPosition, HorizontalConstraintType.Right))
    }

    private fun updateSunUI() {
        if (context == null) {
            return
        }
        val sunChartCalculator = SunTimesCalculatorFactory().create(requireContext())

        val currentTime = LocalDateTime.now()

        val todayTimes = sunChartCalculator.calculate(location, currentTime.toLocalDate())
        val tomorrowTimes = sunChartCalculator.calculate(location, LocalDate.now().plusDays(1))

        val displayDateTimes = sunChartCalculator.calculate(location, displayDate)
        displaySunTimes(displayDateTimes, sunStartTimeTxt, sunEndTimeTxt)

        displayTimeUntilNextSunEvent(currentTime, todayTimes, tomorrowTimes)

        updateSunPosition(currentTime, sunChartCalculator)
    }

    private fun updateSunPosition(currentTime: LocalDateTime, calculator: ISunTimesCalculator) {
        val today = calculator.calculate(gps.location, LocalDate.now())
        val tomorrow = calculator.calculate(gps.location, LocalDate.now().plusDays(1))
        val yesterday = calculator.calculate(gps.location, LocalDate.now().minusDays(1))

        val percent = when {
            currentTime.isAfter(today.down) -> {
                getPercentOfDuration(today.down, tomorrow.up, currentTime)
            }
            currentTime.isAfter(today.up) -> {
                getPercentOfDuration(today.up, today.down, currentTime)
            }
            else -> {
                getPercentOfDuration(yesterday.down, today.up, currentTime)
            }
        }

        val angle = if (currentTime.isAfter(today.up) && currentTime.isBefore(today.down)) {
            // Day time
            180 * percent
        } else {
            // Night time
            180 + 180 * percent
        }

        sunIconClock.display(angle, 0.98f)
    }

    private fun updateMoonPosition(currentTime: LocalDateTime, calculator: IMoonTimesCalculator) {
        val today = calculator.calculate(gps.location, LocalDate.now())
        val tomorrow = calculator.calculate(gps.location, LocalDate.now().plusDays(1))
        val yesterday = calculator.calculate(gps.location, LocalDate.now().minusDays(1))

        val isUp = MoonStateCalculator().isUp(today, currentTime.toLocalTime())

        val percent = if (isUp) {
            val lastUp = DateUtils.getClosestPastTime(currentTime, listOf(today.up, yesterday.up))
            val nextDown =
                DateUtils.getClosestFutureTime(currentTime, listOf(today.down, tomorrow.down))

            if (lastUp != null && nextDown != null) {
                getPercentOfDuration(lastUp, nextDown, currentTime)
            } else {
                0f
            }
        } else {
            val lastDown =
                DateUtils.getClosestPastTime(currentTime, listOf(today.down, yesterday.down))
            val nextUp = DateUtils.getClosestFutureTime(currentTime, listOf(today.up, tomorrow.up))

            if (lastDown != null && nextUp != null) {
                getPercentOfDuration(lastDown, nextUp, currentTime)
            } else {
                0f
            }
        }

        val angle = if (isUp) {
            // Day time
            180 * percent
        } else {
            // Night time
            180 + 180 * percent
        }

        moonIconClock.display(angle, 0.5f)
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

    private fun displayTimeUntilNextSunEvent(
        currentTime: LocalDateTime,
        today: SunTimes,
        tomorrow: SunTimes
    ) {
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


    private fun displaySunTimes(sunTimes: SunTimes, upTxt: TextView, downTxt: TextView) {
        upTxt.text = sunTimes.up.toDisplayFormat(requireContext())
        downTxt.text = sunTimes.down.toDisplayFormat(requireContext())
    }

}
