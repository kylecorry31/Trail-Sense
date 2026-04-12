package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.fragments.BoundFragment

import com.kylecorry.andromeda.fragments.observe
import kotlinx.coroutines.runBlocking
import com.kylecorry.andromeda.sense.pedometer.Pedometer

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPedometerHistoryPageBinding
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.chart.label.DayOfMonthChartLabelFormatter
import com.kylecorry.trail_sense.shared.views.chart.label.DayOfWeekChartLabelFormatter
import com.kylecorry.trail_sense.tools.pedometer.domain.PedometerSession
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.PedometerSessionRepo
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Single fragment used by all four history tabs (Day/Week/Month/Year).
 * Renders the same layout with period-specific chart data, stats, and navigation.
 */
class FragmentPedometerPeriodHistory : BoundFragment<FragmentPedometerHistoryPageBinding>() {

    enum class PeriodType { DAY, WEEK, MONTH, YEAR }

    private val sessionRepo by lazy { PedometerSessionRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val counter by lazy { StepCounter(PreferencesSubsystem.getInstance(requireContext()).preferences) }
    private val paceCalculator by lazy { StrideLengthPaceCalculator(prefs.pedometer.strideLength) }
    private lateinit var chart: PedometerChart

    // Live current-speed sensor (Day tab only)
    private val instantSpeedometer by lazy {
        CurrentPaceSpeedometer(Pedometer(requireContext()), paceCalculator)
    }

    private var periodType: PeriodType = PeriodType.DAY
    private var currentDay: LocalDate = LocalDate.now()
    private var currentWeekStart: LocalDate = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private var currentMonth: YearMonth = YearMonth.now()
    private var currentYear: Year = Year.now()
    private var earliestDate: LocalDate? = null

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPedometerHistoryPageBinding {
        return FragmentPedometerHistoryPageBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        periodType = PeriodType.entries[arguments?.getInt(ARG_PERIOD, 0) ?: 0]
        chart = PedometerChart(binding.periodChart)

        binding.periodPrev.setOnClickListener { navigatePrev() }
        binding.periodNext.setOnClickListener { navigateNext() }

        setupStatsLayout()

        if (periodType == PeriodType.DAY) {
            binding.statCurrentSpeed.title = formatService.formatSpeed(0f)
            observe(instantSpeedometer) { onCurrentSpeedUpdate() }
            scheduleUpdates(1000L)
        } else {
            scheduleUpdates(30000L)
        }

        // Synchronously fetch earliest date and chart data so the UI is fully populated
        // before the fragment's view is ever displayed — no flicker, no "No Data" flash.
        earliestDate = runBlocking { sessionRepo.getEarliest() }
            ?.startTime?.atZone(ZoneId.systemDefault())?.toLocalDate()
        loadData()
    }

    override fun onUpdate() {
        super.onUpdate()
        loadData()
    }

    fun refresh() {
        if (!isAdded || !::chart.isInitialized) return
        earliestDate = runBlocking { sessionRepo.getEarliest() }
            ?.startTime?.atZone(ZoneId.systemDefault())?.toLocalDate()
        loadData()
    }

    // region Setup

    private fun setupStatsLayout() {
        // Day: Steps, Distance, Current Speed, Avg Speed
        binding.statCurrentSpeed.isVisible = periodType == PeriodType.DAY

        // Week+: Daily Avg Steps + Distance
        val showDaily = periodType != PeriodType.DAY
        binding.statDailyAvgSteps.isVisible = showDaily
        binding.statDailyAvgDist.isVisible = showDaily

        // Month+: Weekly Avg Steps + Distance
        val showWeekly = periodType == PeriodType.MONTH || periodType == PeriodType.YEAR
        binding.statWeeklyAvgSteps.isVisible = showWeekly
        binding.statWeeklyAvgDist.isVisible = showWeekly

        // Year: Monthly Avg Steps + Distance
        val showMonthly = periodType == PeriodType.YEAR
        binding.statMonthlyAvgSteps.isVisible = showMonthly
        binding.statMonthlyAvgDist.isVisible = showMonthly

        // Avg Speed: always visible (live counter on Day, session-based on Week/Month/Year)
        binding.statAvgSpeed.isVisible = true
    }

    // endregion

    // region Navigation

    private fun navigatePrev() {
        when (periodType) {
            PeriodType.DAY -> currentDay = currentDay.minusDays(1)
            PeriodType.WEEK -> currentWeekStart = currentWeekStart.minusWeeks(1)
            PeriodType.MONTH -> currentMonth = currentMonth.minusMonths(1)
            PeriodType.YEAR -> currentYear = currentYear.minusYears(1)
        }
        loadData()
    }

    private fun navigateNext() {
        when (periodType) {
            PeriodType.DAY -> currentDay = currentDay.plusDays(1)
            PeriodType.WEEK -> currentWeekStart = currentWeekStart.plusWeeks(1)
            PeriodType.MONTH -> currentMonth = currentMonth.plusMonths(1)
            PeriodType.YEAR -> currentYear = currentYear.plusYears(1)
        }
        loadData()
    }

    private fun updateNavArrows() {
        val today = LocalDate.now()
        val earliest = earliestDate ?: today

        when (periodType) {
            PeriodType.DAY -> {
                setArrowEnabled(binding.periodNext, currentDay < today)
                setArrowEnabled(binding.periodPrev, currentDay > earliest)
            }
            PeriodType.WEEK -> {
                val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                setArrowEnabled(binding.periodNext, currentWeekStart < thisWeekStart)
                setArrowEnabled(binding.periodPrev,
                    currentWeekStart > earliest.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
            }
            PeriodType.MONTH -> {
                setArrowEnabled(binding.periodNext, currentMonth < YearMonth.now())
                setArrowEnabled(binding.periodPrev, currentMonth > YearMonth.from(earliest))
            }
            PeriodType.YEAR -> {
                setArrowEnabled(binding.periodNext, currentYear < Year.now())
                setArrowEnabled(binding.periodPrev, currentYear > Year.from(earliest))
            }
        }
    }

    private fun setArrowEnabled(button: ImageButton, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1.0f else 0.3f
    }

    // endregion

    // region Data Loading

    private fun loadData() {
        if (!isAdded || !::chart.isInitialized) return
        updateNavArrows()

        when (periodType) {
            PeriodType.DAY -> loadDay()
            PeriodType.WEEK -> loadWeek()
            PeriodType.MONTH -> loadMonth()
            PeriodType.YEAR -> loadYear()
        }
    }

    private fun loadDay() {
        binding.periodLabel.text = formatService.formatRelativeDate(currentDay)

        val zone = ZoneId.systemDefault()
        val startOfDay = currentDay.atStartOfDay(zone).toInstant()
        val endOfDay = currentDay.plusDays(1).atStartOfDay(zone).toInstant()
        val isToday = currentDay == LocalDate.now()

        // Dim current speed when viewing past days (no live sensor data available)
        binding.statCurrentSpeed.alpha = if (isToday) 1.0f else 0.38f

        val sessions = runBlocking { sessionRepo.getRange(startOfDay, endOfDay) }

        val bucketSteps = LongArray(96)
        val bucketDist = FloatArray(96)
        for (s in sessions) {
            val zdt = s.startTime.atZone(zone)
            val bucket = zdt.hour * 4 + zdt.minute / 15
            bucketSteps[bucket] += s.steps
            bucketDist[bucket] += s.distance
        }

        if (isToday) {
            val liveSteps = counter.steps
            val liveStart = counter.startTime
            if (liveSteps > 0 && liveStart != null) {
                val zdt = liveStart.atZone(zone)
                val bucket = zdt.hour * 4 + zdt.minute / 15
                bucketSteps[bucket] += liveSteps
                bucketDist[bucket] += paceCalculator.distance(liveSteps).meters().value
            }
        }

        // Dynamic X range: find first/last bucket with steps, add a 1-hour buffer each side
        val firstActive = bucketSteps.indexOfFirst { it > 0L }
        val lastActive  = bucketSteps.indexOfLast  { it > 0L }
        val startBucket = if (firstActive < 0) 0  else (firstActive - 4).coerceAtLeast(0)
        val endBucket   = if (lastActive  < 0) 95 else (lastActive  + 4).coerceAtMost(95)
        val xMin = startBucket * 0.25f - 0.125f
        val xMax = endBucket   * 0.25f + 0.125f

        // Pick a nice step size (in minutes) so labels fall on round boundaries.
        // Smaller visible range → finer granularity (down to 15 min).
        val visibleHours = xMax - xMin
        val stepMinutes = when {
            visibleHours <= 2f  -> 15
            visibleHours <= 4f  -> 15
            visibleHours <= 7f  -> 30
            visibleHours <= 13f -> 60
            visibleHours <= 20f -> 120
            else                -> 180  // 3-hour steps → 0,3,6,9,12,15,18,21 + 23:59
        }
        val stepHours = stepMinutes / 60f

        // Snap chart boundaries inward to the nearest step grid so every label is
        // at an exact H:mm time (e.g., 19:00, 19:30, 20:00 …) rather than a fractional hour.
        val labelStart = (Math.ceil(xMin.toDouble() / stepHours) * stepHours).toFloat()
        // For a full-day view extend the right boundary to 24 h so the formatter can
        // emit "23:59" as the final label, giving users a clear end-of-day anchor.
        val labelEnd = if (xMax >= 23.5f) 24.0f
                       else (Math.floor(xMax.toDouble() / stepHours) * stepHours).toFloat()
        val intervals  = ((labelEnd - labelStart) / stepHours).roundToInt()
        // Fall back to 2 labels if the snapped range collapsed (extremely short window).
        val xLabelCount = if (intervals <= 0) 2 else intervals + 1

        val chartData = (startBucket..endBucket).map { i ->
            Pair(i * 0.25f, bucketSteps[i].toFloat())
        }

        // Use snapped boundaries so labels align perfectly to the step grid.
        chart.plot(chartData, labelStart, labelEnd, xLabelCount, HourMinuteLabelFormatter())

        // Avg speed denominator: count only 15-min buckets that had steps ("active" time).
        // This avoids diluting the speed with idle hours and gives a realistic walking pace.
        val activeBuckets = bucketSteps.count { it > 0L }
        updateDayStats(bucketSteps.sum(), bucketDist.sum(), activeBuckets)
    }

    private fun loadWeek() {
        val weekEnd = currentWeekStart.plusDays(6)
        val labelFmt = DateTimeFormatter.ofPattern("MMM d")
        binding.periodLabel.text = "${currentWeekStart.format(labelFmt)} – ${weekEnd.format(labelFmt)}"

        val zone = ZoneId.systemDefault()
        val startInstant = currentWeekStart.atStartOfDay(zone).toInstant()
        val endInstant = currentWeekStart.plusDays(7).atStartOfDay(zone).toInstant()
        val isThisWeek = currentWeekStart == LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val sessions = runBlocking { sessionRepo.getRange(startInstant, endInstant) }

        val daily = aggregateByDay(sessions, currentWeekStart, 7)
        if (isThisWeek) addLiveSession(daily, currentWeekStart)

        val chartData = daily.mapIndexed { i, bucket ->
            Pair(i.toFloat() + 1, bucket.steps.toFloat())
        }

        chart.plot(
            chartData,
            xMin = 1f,
            xMax = 7f,
            xLabelCount = 7,
            xLabelFormatter = DayOfWeekChartLabelFormatter(currentWeekStart)
        )

        updateWeekStats(daily, sessions)
    }

    private fun loadMonth() {
        binding.periodLabel.text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

        val zone = ZoneId.systemDefault()
        val monthStart = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val startInstant = monthStart.atStartOfDay(zone).toInstant()
        val endInstant = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        val isThisMonth = currentMonth == YearMonth.now()

        val sessions = runBlocking { sessionRepo.getRange(startInstant, endInstant) }

        val daily = aggregateByDay(sessions, monthStart, daysInMonth)
        if (isThisMonth) addLiveSession(daily, monthStart)

        val chartData = daily.mapIndexed { i, bucket ->
            Pair(i.toFloat() + 1, bucket.steps.toFloat())
        }
        // ~every 3rd day: 10–11 labels depending on month length.
        // Integer division handles all lengths (28→10, 29→10, 30→11, 31→11).
        val monthLabelCount = (daysInMonth - 1) / 3 + 1
        chart.plot(chartData, 1f, daysInMonth.toFloat(), monthLabelCount,
            DayOfMonthChartLabelFormatter())

        updateMonthStats(daily, sessions)
    }

    private fun loadYear() {
        binding.periodLabel.text = currentYear.toString()

        val zone = ZoneId.systemDefault()
        val yearStart = currentYear.atDay(1)
        val yearEnd = currentYear.plusYears(1).atDay(1)
        val startInstant = yearStart.atStartOfDay(zone).toInstant()
        val endInstant = yearEnd.atStartOfDay(zone).toInstant()
        val isThisYear = currentYear == Year.now()

        val sessions = runBlocking { sessionRepo.getRange(startInstant, endInstant) }

        val monthly = aggregateByMonth(sessions, currentYear)
        if (isThisYear) addLiveSessionToMonthly(monthly)

        val chartData = monthly.mapIndexed { i, bucket ->
            Pair(i.toFloat() + 1, bucket.steps.toFloat())
        }
        chart.plot(chartData, 1f, 12f, 12, MonthOfYearLabelFormatter())

        val daysInYear = if (currentYear.isLeap) 366 else 365
        val daily = aggregateByDay(sessions, yearStart, daysInYear)
        if (isThisYear) addLiveSession(daily, yearStart)
        updateYearStats(daily, monthly, sessions)
    }

    // endregion

    // region Stats Display

    private fun updateDayStats(totalSteps: Long, totalDistance: Float, activeBuckets: Int) {
        binding.statSteps.title = DecimalFormatter.format(totalSteps, 0)
        binding.statDistance.title = formatDist(totalDistance)

        // Avg speed: distance / active walking time only.
        // "Active time" = number of 15-min buckets that had at least 1 step × 15 min.
        // This excludes all idle time so the result reflects actual walking pace.
        val activeTimeSec = activeBuckets * 15 * 60
        binding.statAvgSpeed.title = if (activeTimeSec > 0 && totalDistance > 0f) {
            formatService.formatSpeed(totalDistance / activeTimeSec)
        } else {
            formatService.formatSpeed(0f)
        }
    }

    /** Called when the instant speedometer sensor has a new reading */
    private fun onCurrentSpeedUpdate() {
        if (periodType != PeriodType.DAY || !isAdded) return
        val isToday = currentDay == LocalDate.now()
        if (isToday) {
            // Show 0.0 before a valid reading arrives (sensor needs ~10s to calibrate)
            val speed = if (instantSpeedometer.hasValidReading) instantSpeedometer.speed.speed else 0f
            binding.statCurrentSpeed.title = formatService.formatSpeed(speed)
        } else {
            binding.statCurrentSpeed.title = formatService.formatSpeed(0f)
        }
    }

    private fun updateWeekStats(daily: List<Bucket>, sessions: List<PedometerSession>) {
        val totalSteps = daily.sumOf { it.steps }
        val totalDist = daily.sumOf { it.distance.toDouble() }.toFloat()
        val daysWithData = daily.count { it.steps > 0 }

        binding.statSteps.title = DecimalFormatter.format(totalSteps, 0)
        binding.statDistance.title = formatDist(totalDist)

        // Daily averages
        if (daysWithData > 0) {
            binding.statDailyAvgSteps.title = DecimalFormatter.format(totalSteps / daysWithData, 0)
            binding.statDailyAvgDist.title = formatDist(totalDist / daysWithData)
        } else {
            binding.statDailyAvgSteps.title = getString(R.string.dash)
            binding.statDailyAvgDist.title = getString(R.string.dash)
        }

        binding.statAvgSpeed.title = computeAvgSpeed(totalDist, sessions)
    }

    private fun updateMonthStats(daily: List<Bucket>, sessions: List<PedometerSession>) {
        val totalSteps = daily.sumOf { it.steps }
        val totalDist = daily.sumOf { it.distance.toDouble() }.toFloat()
        val daysWithData = daily.count { it.steps > 0 }

        binding.statSteps.title = DecimalFormatter.format(totalSteps, 0)
        binding.statDistance.title = formatDist(totalDist)

        // Daily averages
        if (daysWithData > 0) {
            binding.statDailyAvgSteps.title = DecimalFormatter.format(totalSteps / daysWithData, 0)
            binding.statDailyAvgDist.title = formatDist(totalDist / daysWithData)
        } else {
            binding.statDailyAvgSteps.title = getString(R.string.dash)
            binding.statDailyAvgDist.title = getString(R.string.dash)
        }

        // Weekly averages (total / number of weeks with data)
        val weeksWithData = countWeeksWithData(daily)
        if (weeksWithData > 0) {
            binding.statWeeklyAvgSteps.title = DecimalFormatter.format(totalSteps / weeksWithData, 0)
            binding.statWeeklyAvgDist.title = formatDist(totalDist / weeksWithData)
        } else {
            binding.statWeeklyAvgSteps.title = getString(R.string.dash)
            binding.statWeeklyAvgDist.title = getString(R.string.dash)
        }

        binding.statAvgSpeed.title = computeAvgSpeed(totalDist, sessions)
    }

    private fun updateYearStats(daily: List<Bucket>, monthly: List<Bucket>, sessions: List<PedometerSession>) {
        val totalSteps = daily.sumOf { it.steps }
        val totalDist = daily.sumOf { it.distance.toDouble() }.toFloat()
        val daysWithData = daily.count { it.steps > 0 }

        binding.statSteps.title = DecimalFormatter.format(totalSteps, 0)
        binding.statDistance.title = formatDist(totalDist)

        // Daily averages
        if (daysWithData > 0) {
            binding.statDailyAvgSteps.title = DecimalFormatter.format(totalSteps / daysWithData, 0)
            binding.statDailyAvgDist.title = formatDist(totalDist / daysWithData)
        } else {
            binding.statDailyAvgSteps.title = getString(R.string.dash)
            binding.statDailyAvgDist.title = getString(R.string.dash)
        }

        // Weekly averages
        val weeksWithData = countWeeksWithData(daily)
        if (weeksWithData > 0) {
            binding.statWeeklyAvgSteps.title = DecimalFormatter.format(totalSteps / weeksWithData, 0)
            binding.statWeeklyAvgDist.title = formatDist(totalDist / weeksWithData)
        } else {
            binding.statWeeklyAvgSteps.title = getString(R.string.dash)
            binding.statWeeklyAvgDist.title = getString(R.string.dash)
        }

        // Monthly averages
        val monthsWithData = monthly.count { it.steps > 0 }
        if (monthsWithData > 0) {
            binding.statMonthlyAvgSteps.title = DecimalFormatter.format(totalSteps / monthsWithData, 0)
            binding.statMonthlyAvgDist.title = formatDist(totalDist / monthsWithData)
        } else {
            binding.statMonthlyAvgSteps.title = getString(R.string.dash)
            binding.statMonthlyAvgDist.title = getString(R.string.dash)
        }

        binding.statAvgSpeed.title = computeAvgSpeed(totalDist, sessions)
    }

    // endregion

    // region Formatting Helpers

    private fun formatDist(meters: Float): String {
        val dist = Distance.meters(meters)
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        return formatService.formatDistance(dist, Units.getDecimalPlaces(dist.units), false)
    }

    /**
     * Average speed using only sessions that had actual steps (exclude zero-step sessions
     * so idle sessions don't inflate the denominator and drive speed toward zero).
     */
    private fun computeAvgSpeed(totalDistMeters: Float, sessions: List<PedometerSession>): String {
        val activeSessions = sessions.filter { it.steps > 0 }
        val totalSec = activeSessions.sumOf { Duration.between(it.startTime, it.endTime).seconds }
        return if (totalSec > 0 && totalDistMeters > 0f) {
            formatService.formatSpeed(totalDistMeters / totalSec.toFloat())
        } else {
            formatService.formatSpeed(0f)
        }
    }

    /** Count how many 7-day chunks in the daily bucket list have any data */
    private fun countWeeksWithData(daily: List<Bucket>): Int {
        var count = 0
        for (weekStart in daily.indices step 7) {
            val weekEnd = minOf(weekStart + 7, daily.size)
            if (daily.subList(weekStart, weekEnd).any { it.steps > 0 }) {
                count++
            }
        }
        return count
    }

    // endregion

    // region Aggregation Helpers

    private data class Bucket(var steps: Long = 0, var distance: Float = 0f)

    private fun addLiveSession(daily: MutableList<Bucket>, periodStart: LocalDate) {
        val liveSteps = counter.steps
        val liveStart = counter.startTime ?: return
        if (liveSteps <= 0) return
        val todayIndex = (LocalDate.now().toEpochDay() - periodStart.toEpochDay()).toInt()
        if (todayIndex in daily.indices) {
            daily[todayIndex].steps += liveSteps
            daily[todayIndex].distance += paceCalculator.distance(liveSteps).meters().value
        }
    }

    private fun aggregateByDay(
        sessions: List<PedometerSession>,
        periodStart: LocalDate,
        days: Int
    ): MutableList<Bucket> {
        val zone = ZoneId.systemDefault()
        val result = MutableList(days) { Bucket() }
        for (session in sessions) {
            val sessionDate = session.startTime.atZone(zone).toLocalDate()
            val dayIndex = (sessionDate.toEpochDay() - periodStart.toEpochDay()).toInt()
            if (dayIndex in 0 until days) {
                result[dayIndex].steps += session.steps
                result[dayIndex].distance += session.distance
            }
        }
        return result
    }

    private fun aggregateByMonth(
        sessions: List<PedometerSession>,
        year: Year
    ): MutableList<Bucket> {
        val result = MutableList(12) { Bucket() }
        val zone = ZoneId.systemDefault()
        for (session in sessions) {
            val monthIndex = session.startTime.atZone(zone).monthValue - 1
            result[monthIndex].steps += session.steps
            result[monthIndex].distance += session.distance
        }
        return result
    }

    private fun addLiveSessionToMonthly(monthly: MutableList<Bucket>) {
        val liveSteps = counter.steps
        if (liveSteps <= 0) return
        val monthIndex = LocalDate.now().monthValue - 1
        if (monthIndex in monthly.indices) {
            monthly[monthIndex].steps += liveSteps
            monthly[monthIndex].distance += paceCalculator.distance(liveSteps).meters().value
        }
    }

    // endregion

    // region Label Formatters

    /**
     * Formats a fractional-hour X value as "H:mm" (e.g. 9.5 → "9:30", 14.25 → "14:15").
     * Values outside 00:00–23:59 are suppressed so no garbage appears at axis edges.
     */
    private class HourMinuteLabelFormatter : com.kylecorry.andromeda.views.chart.label.ChartLabelFormatter {
        override fun format(value: Float): String {
            val totalMinutes = Math.round(value * 60)
            if (totalMinutes < 0) return ""
            // 24:00 is the virtual end-of-day anchor; show as "23:59" to keep it within the day.
            if (totalMinutes >= 24 * 60) return "23:59"
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return String.format("%d:%02d", h, m)
        }
    }

    private class MonthOfYearLabelFormatter : com.kylecorry.andromeda.views.chart.label.ChartLabelFormatter {
        override fun format(value: Float): String {
            val m = value.toInt()
            return if (m in 1..12) {
                java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            } else ""
        }
    }

    // endregion

    companion object {
        private const val ARG_PERIOD = "period_type"

        fun newInstance(type: PeriodType): FragmentPedometerPeriodHistory {
            return FragmentPedometerPeriodHistory().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PERIOD, type.ordinal)
                }
            }
        }
    }
}
