package com.kylecorry.trail_sense.astronomy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.capitalizeWords
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.time.Time.roundNearestMinute
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstroPositions
import com.kylecorry.trail_sense.astronomy.domain.AstronomyEvent
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.providers.*
import com.kylecorry.trail_sense.databinding.ActivityAstronomyBinding
import com.kylecorry.trail_sense.databinding.ListItemAstronomyDetailBinding
import com.kylecorry.trail_sense.quickactions.AstronomyQuickActionBinder
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.USER_ERROR_GPS_NOT_SET
import com.kylecorry.trail_sense.shared.USER_ERROR_NO_GPS
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.UserError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class AstronomyFragment : BoundFragment<ActivityAstronomyBinding>() {

    private lateinit var gps: IGPS

    private lateinit var detailList: ListView<AstroField>
    private lateinit var chart: AstroChart

    private lateinit var displayDate: LocalDate

    private lateinit var sunTimesMode: SunTimesMode

    private val sensorService by lazy { SensorService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { Preferences(requireContext()) }
    private val astronomyService = AstronomyService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }
    private val markdownService by lazy { MarkdownService(requireContext()) }

    private var lastAstronomyEventSearch: AstronomyEvent? = null

    private var moonAltitudes: List<Pair<LocalDateTime, Float>> = emptyList()
    private var sunAltitudes: List<Pair<LocalDateTime, Float>> = emptyList()
    private var minChartTime = LocalDateTime.now()
    private var maxChartTime = LocalDateTime.now()
    private var currentSeekChartTime = LocalDateTime.now()
    private val maxProgress = 60 * 24

    private var gpsErrorShown = false

    private var uiUpdateJob: Job? = null

    private val intervalometer = Timer {
        updateUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AstronomyQuickActionBinder(
            this,
            binding,
            prefs.astronomy
        ).bind()

        val recyclerView = binding.astronomyDetailList
        detailList =
            ListView(recyclerView, R.layout.list_item_astronomy_detail) { itemView, field ->
                val itemBinding = ListItemAstronomyDetailBinding.bind(itemView)
                field.display(itemBinding)
            }


        chart = AstroChart(binding.sunMoonChart)

        binding.datePicker.setOnClickListener {
            Pickers.date(requireContext(), displayDate) {
                if (it != null) {
                    displayDate = it
                    updateUI()
                }
            }
        }

        binding.datePicker.setOnLongClickListener {
            val options = listOf(
                AstronomyEvent.FullMoon,
                AstronomyEvent.NewMoon,
                AstronomyEvent.QuarterMoon,
                AstronomyEvent.MeteorShower,
                AstronomyEvent.LunarEclipse,
                AstronomyEvent.Supermoon
            )
            Pickers.item(
                requireContext(),
                getString(R.string.find_next_occurrence),
                listOf(
                    getString(R.string.full_moon),
                    getString(R.string.new_moon),
                    getString(R.string.quarter_moon),
                    getString(R.string.meteor_shower),
                    getString(R.string.lunar_eclipse),
                    getString(R.string.supermoon)
                ).map { it.capitalizeWords() },
                options.indexOf(lastAstronomyEventSearch)
            ) {
                if (it != null) {
                    val search = options[it]
                    lastAstronomyEventSearch = search
                    displayDate = astronomyService.findNextEvent(
                        search,
                        gps.location,
                        displayDate
                    ) ?: displayDate
                    updateUI()
                }
            }
            true
        }

        binding.nextDate.setOnClickListener {
            displayDate = displayDate.plusDays(1)
            updateUI()
        }

        binding.prevDate.setOnClickListener {
            displayDate = displayDate.minusDays(1)
            updateUI()
        }

        gps = sensorService.getGPS()

        sunTimesMode = prefs.astronomy.sunTimesMode

        binding.sunPosition.setOnClickListener {
            showTimeSeeker()
        }

        binding.moonPosition.setOnClickListener {
            showTimeSeeker()
        }

        binding.timeSeeker.max = maxProgress

        binding.timeSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val seconds = (Duration.between(
                    minChartTime,
                    maxChartTime
                ).seconds * progress / maxProgress.toFloat()).toLong()
                currentSeekChartTime = minChartTime.plusSeconds(seconds)
                binding.seekTime.text =
                    formatService.formatTime(
                        currentSeekChartTime.toLocalTime(),
                        includeSeconds = false
                    )
                plotCelestialBodyImage(binding.moonPosition, moonAltitudes, 0, currentSeekChartTime)
                plotCelestialBodyImage(binding.sunPosition, sunAltitudes, 1, currentSeekChartTime)
                updateSeekPositions()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

    }

    private fun showTimeSeeker() {
        binding.timeSeekerPanel.isVisible = true
        binding.astronomyDetailList.isVisible = false
        currentSeekChartTime = LocalDateTime.now()
        binding.seekTime.text =
            formatService.formatTime(currentSeekChartTime.toLocalTime(), includeSeconds = false)

        binding.timeSeeker.progress = getSeekProgress()

        binding.closeSeek.setOnClickListener {
            hideTimeSeeker()
        }

        updateSeekPositions()
    }

    private fun updateSeekPositions() {
        val positions = getSunMoonPositions(currentSeekChartTime)
        binding.sunPositionText.text = markdownService.toMarkdown(
            getString(
                R.string.sun_moon_position_template,
                getString(R.string.sun),
                formatService.formatDegrees(positions.sunAltitude),
                formatService.formatDegrees(positions.sunAzimuth)
            )
        )

        binding.moonPositionText.text = markdownService.toMarkdown(
            getString(
                R.string.sun_moon_position_template,
                getString(R.string.moon),
                formatService.formatDegrees(positions.moonAltitude),
                formatService.formatDegrees(positions.moonAzimuth)
            )
        )
    }

    private fun getSeekProgress(): Int {
        val totalDuration = Duration.between(minChartTime, maxChartTime).seconds
        val currentDuration = Duration.between(minChartTime, currentSeekChartTime).seconds
        val progress = maxProgress * currentDuration / totalDuration.toFloat()
        return progress.toInt()
    }

    private fun hideTimeSeeker() {
        binding.timeSeekerPanel.isVisible = false
        binding.astronomyDetailList.isVisible = true
        plotCelestialBodyImage(binding.moonPosition, moonAltitudes, 0)
        plotCelestialBodyImage(binding.sunPosition, sunAltitudes, 1)
    }


    private fun getSunMoonPositions(time: LocalDateTime): AstroPositions {
        val moonAltitude =
            astronomyService.getMoonAltitude(gps.location, time)
        val sunAltitude =
            astronomyService.getSunAltitude(gps.location, time)

        val declination =
            if (!prefs.navigation.useTrueNorth) getDeclination() else 0f

        val sunAzimuth =
            astronomyService.getSunAzimuth(gps.location, time).withDeclination(-declination).value
        val moonAzimuth =
            astronomyService.getMoonAzimuth(gps.location, time)
                .withDeclination(-declination).value

        return AstroPositions(
            moonAltitude,
            sunAltitude,
            moonAzimuth,
            sunAzimuth
        )
    }

    override fun onResume() {
        super.onResume()
        displayDate = LocalDate.now()
        requestLocationUpdate()
        intervalometer.interval(Duration.ofMinutes(1), Duration.ofMillis(200))
        updateUI()

        if (cache.getBoolean("cache_tap_sun_moon_shown") != true) {
            cache.putBoolean("cache_tap_sun_moon_shown", true)
            Alerts.toast(requireContext(), getString(R.string.tap_sun_moon_hint))
        }

    }

    override fun onPause() {
        super.onPause()
        tryOrNothing {
            uiUpdateJob?.cancel()
        }
        gps.stop(this::onLocationUpdate)
        intervalometer.stop()
        gpsErrorShown = false
    }

    private fun requestLocationUpdate() {
        if (gps.hasValidReading) {
            onLocationUpdate()
        } else {
            gps.start(this::onLocationUpdate)
        }
    }

    private fun onLocationUpdate(): Boolean {
        updateUI()
        return false
    }

    private fun getDeclination(): Float {
        return declination.getDeclination()
    }

    private fun updateUI() {
        if (!isBound) {
            return
        }
        uiUpdateJob = runInBackground {
            withContext(Dispatchers.Main) {
                detectAndShowGPSError()
                binding.date.text = getDateString(displayDate)
            }

            updateSunUI()
            updateMoonUI()
            updateAstronomyChart()
            updateAstronomyDetails()
        }
    }

    private suspend fun updateMoonUI() {
        if (!isBound) {
            return
        }

        val moonPhase = withContext(Dispatchers.Default) {
            astronomyService.getCurrentMoonPhase()
        }

        withContext(Dispatchers.Main) {
            binding.moonPosition.setImageResource(getMoonImage(moonPhase.phase))
        }
    }

    private suspend fun updateAstronomyChart() {
        if (!isBound) {
            return
        }

        val startHour: Float

        withContext(Dispatchers.Default) {
            if (displayDate == LocalDate.now() && prefs.astronomy.centerSunAndMoon) {
                val startTime = LocalDateTime.now().roundNearestMinute(10).minusHours(12)
                startHour = startTime.hour + startTime.minute / 60f

                minChartTime = startTime
                maxChartTime = startTime.plusHours(24)

                moonAltitudes = astronomyService.getCenteredMoonAltitudes(
                    gps.location,
                    LocalDateTime.now()
                )
                sunAltitudes = astronomyService.getCenteredSunAltitudes(
                    gps.location,
                    LocalDateTime.now()
                )
            } else {
                startHour = 0f
                minChartTime = displayDate.atStartOfDay()
                maxChartTime = minChartTime.plusHours(24)
                moonAltitudes = astronomyService.getMoonAltitudes(gps.location, displayDate)
                sunAltitudes = astronomyService.getSunAltitudes(gps.location, displayDate)
            }
        }

        withContext(Dispatchers.Main) {
            chart.plot(
                listOf(
                    AstroChart.AstroChartDataset(
                        moonAltitudes,
                        resources.getColor(R.color.white, null)
                    ),
                    AstroChart.AstroChartDataset(
                        sunAltitudes,
                        resources.getColor(R.color.sun, null)
                    )
                ),
                startHour
            )

            if (displayDate == LocalDate.now()) {
                plotCelestialBodyImage(binding.moonPosition, moonAltitudes, 0)
                plotCelestialBodyImage(binding.sunPosition, sunAltitudes, 1)
            } else {
                binding.sunPosition.visibility = View.INVISIBLE
                binding.moonPosition.visibility = View.INVISIBLE
            }
        }
    }


    private fun plotCelestialBodyImage(
        image: ImageView,
        altitudes: List<Pair<LocalDateTime, Float>>,
        datasetId: Int,
        time: LocalDateTime = LocalDateTime.now()
    ) {
        val current = altitudes.minByOrNull {
            Duration.between(time, it.first).abs()
        }
        val currentIdx = altitudes.indexOf(current)
        val point = chart.getPoint(datasetId, currentIdx)
        image.x = point.first - image.width / 2f
        image.y = point.second - image.height / 2f

        if (image.height != 0) {
            image.visibility = View.VISIBLE
        }
    }

    private suspend fun updateSunUI() {
        if (!isBound) {
            return
        }

        displayTimeUntilNextSunEvent()
    }

    private suspend fun updateAstronomyDetails() {
        if (!isBound) {
            return
        }

        val fields = mutableListOf<AstroField>()

        withContext(Dispatchers.Default) {

            val fieldProvider = Group(
                SunMoonTimesProvider(prefs.astronomy.showNoon),
                Section(DaylightProvider(sunTimesMode)),
                Conditional(Section(CivilTimesProvider())) {
                    prefs.astronomy.showCivilTimes
                },
                Conditional(Section(NauticalTimesProvider())) {
                    prefs.astronomy.showNauticalTimes
                },
                Conditional(Section(AstronomicalSunTimesProvider())) {
                    prefs.astronomy.showAstronomicalTimes
                },
                Section(
                    Group(
                        MoonPhaseProvider(),
                        Conditional(MeteorShowerProvider()) { prefs.astronomy.showMeteorShowers },
                        Conditional(LunarEclipseProvider()) { prefs.astronomy.showLunarEclipses }
                    )
                )
            )

            fields.addAll(fieldProvider.getFields(displayDate, gps.location))
        }

        withContext(Dispatchers.Main) {
            detailList.setData(fields)
        }

    }


    private fun getMoonImage(phase: MoonTruePhase): Int {
        return MoonPhaseImageMapper().getPhaseImage(phase)
    }

    private suspend fun displayTimeUntilNextSunEvent() {
        val currentTime = LocalDateTime.now()

        var nextSunrise: LocalDateTime?
        var nextSunset: LocalDateTime?
        withContext(Dispatchers.Default) {
            nextSunrise = astronomyService.getNextSunrise(gps.location, sunTimesMode)
            nextSunset = astronomyService.getNextSunset(gps.location, sunTimesMode)
        }

        withContext(Dispatchers.Main) {
            if (nextSunrise != null && (nextSunset == null || nextSunrise?.isBefore(nextSunset) == true)) {
                binding.remainingTime.text =
                    formatService.formatDuration(Duration.between(currentTime, nextSunrise))
                binding.remainingTimeLbl.text = getString(
                    R.string.until_sun_time, getSunriseWording()
                )
            } else if (nextSunset != null) {
                binding.remainingTime.text =
                    formatService.formatDuration(Duration.between(currentTime, nextSunset))
                binding.remainingTimeLbl.text = getString(
                    R.string.until_sun_time, getSunsetWording()
                )
            } else if (astronomyService.isSunUp(gps.location)) {
                binding.remainingTime.text = getString(R.string.sun_up_no_set)
                binding.remainingTimeLbl.text = getString(R.string.sun_does_not_set)
            } else {
                binding.remainingTime.text = getString(R.string.sun_down_no_set)
                binding.remainingTimeLbl.text = getString(R.string.sun_does_not_rise)
            }
        }
    }

    private fun getDateString(date: LocalDate): String {
        return formatService.formatRelativeDate(date)
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

    private fun detectAndShowGPSError() {
        if (gpsErrorShown) {
            return
        }

        if (gps is OverrideGPS && gps.location == Coordinate.zero) {
            val activity = requireActivity() as MainActivity
            val navController = findNavController()
            val error = UserError(
                USER_ERROR_GPS_NOT_SET,
                getString(R.string.location_not_set),
                R.drawable.satellite,
                getString(R.string.set)
            ) {
                activity.errorBanner.dismiss(USER_ERROR_GPS_NOT_SET)
                navController.navigate(R.id.calibrateGPSFragment)
            }
            activity.errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CachedGPS && gps.location == Coordinate.zero) {
            val error = UserError(
                USER_ERROR_NO_GPS,
                getString(R.string.location_disabled),
                R.drawable.satellite
            )
            (requireActivity() as MainActivity).errorBanner.report(error)
            gpsErrorShown = true
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityAstronomyBinding {
        return ActivityAstronomyBinding.inflate(layoutInflater, container, false)
    }

}
