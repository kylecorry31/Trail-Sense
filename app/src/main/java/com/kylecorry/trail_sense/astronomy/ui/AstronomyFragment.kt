package com.kylecorry.trail_sense.astronomy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.capitalizeWords
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstroPositions
import com.kylecorry.trail_sense.astronomy.domain.AstronomyEvent
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.commands.AstroChartData
import com.kylecorry.trail_sense.astronomy.ui.commands.CenteredAstroChartDataProvider
import com.kylecorry.trail_sense.astronomy.ui.commands.DailyAstroChartDataProvider
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.providers.*
import com.kylecorry.trail_sense.databinding.ActivityAstronomyBinding
import com.kylecorry.trail_sense.databinding.ListItemAstronomyDetailBinding
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.quickactions.AstronomyQuickActionBinder
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.setOnProgressChangeListener
import com.kylecorry.trail_sense.shared.views.UserError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class AstronomyFragment : BoundFragment<ActivityAstronomyBinding>() {

    private lateinit var gps: IGPS

    private lateinit var detailList: ListView<AstroField>
    private lateinit var chart: AstroChart

    private lateinit var sunTimesMode: SunTimesMode

    private val sensorService by lazy { SensorService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val astronomyService = AstronomyService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }
    private val markdownService by lazy { MarkdownService(requireContext()) }

    private var lastAstronomyEventSearch: AstronomyEvent? = null

    private var minChartTime = ZonedDateTime.now()
    private var maxChartTime = ZonedDateTime.now()
    private var currentSeekChartTime = ZonedDateTime.now()
    private val maxProgress = 60 * 24

    private var gpsErrorShown = false

    private val intervalometer = Timer {
        updateUI()
    }

    private val astroChartDataProvider by lazy {
        if (prefs.astronomy.centerSunAndMoon) {
            CenteredAstroChartDataProvider()
        } else {
            DailyAstroChartDataProvider()
        }
    }

    private var data = AstroChartData(emptyList(), emptyList())

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


        chart = AstroChart(binding.sunMoonChart, this::showTimeSeeker)

        binding.displayDate.setOnDateChangeListener {
            updateUI()
        }

        binding.displayDate.setOnCalendarLongPressListener {
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
                    val currentDate = binding.displayDate.date
                    binding.displayDate.date = astronomyService.findNextEvent(
                        search,
                        gps.location,
                        currentDate
                    ) ?: currentDate
                    updateUI()
                }
            }
        }

        gps = sensorService.getGPS()

        sunTimesMode = prefs.astronomy.sunTimesMode

        binding.timeSeeker.max = maxProgress

        binding.timeSeeker.setOnProgressChangeListener { progress, _ ->
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
            plotMoonImage(data.moon, currentSeekChartTime)
            plotSunImage(data.sun, currentSeekChartTime)
            updateSeekPositions()
        }
    }

    private fun showTimeSeeker() {
        binding.timeSeekerPanel.isVisible = true
        binding.astronomyDetailList.isVisible = false
        currentSeekChartTime = ZonedDateTime.now()
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
        val displayDate = binding.displayDate.date
        if (displayDate == LocalDate.now()) {
            plotMoonImage(data.moon)
            plotSunImage(data.sun)
        } else {
            chart.moveSun(null)
            chart.moveMoon(null)
        }
    }


    private fun getSunMoonPositions(time: ZonedDateTime): AstroPositions {
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
        binding.displayDate.date = LocalDate.now()
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
        inBackground {
            withContext(Dispatchers.Main) {
                detectAndShowGPSError()
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

        val displayDate = binding.displayDate.date

        val moonPhase = withContext(Dispatchers.Default) {
            if (displayDate == LocalDate.now()) {
                astronomyService.getCurrentMoonPhase()
            } else {
                astronomyService.getMoonPhase(displayDate)
            }
        }

        withContext(Dispatchers.Main) {
            chart.setMoonImage(getMoonImage(moonPhase.phase))
        }
    }

    private suspend fun updateAstronomyChart() {
        if (!isBound) {
            return
        }

        val displayDate = binding.displayDate.date

        data = onDefault {
            val time =
                if (displayDate == LocalDate.now()) ZonedDateTime.now() else displayDate.atStartOfDay()
                    .toZonedDateTime()
            astroChartDataProvider.get(gps.location, time)
        }

        minChartTime = data.sun.first().time.toZonedDateTime()
        maxChartTime = data.sun.last().time.toZonedDateTime()

        onMain {
            chart.plot(data.sun, data.moon)

            if (displayDate == LocalDate.now()) {
                plotMoonImage(data.moon)
                plotSunImage(data.sun)
            } else {
                chart.moveSun(null)
                chart.moveMoon(null)
            }
        }
    }


    private fun plotSunImage(
        altitudes: List<Reading<Float>>,
        time: ZonedDateTime = ZonedDateTime.now()
    ) {
        val instant = time.toInstant()
        val current = altitudes.minByOrNull {
            Duration.between(instant, it.time).abs()
        }
        chart.moveSun(current)
    }

    private fun plotMoonImage(
        altitudes: List<Reading<Float>>,
        time: ZonedDateTime = ZonedDateTime.now()
    ) {
        val instant = time.toInstant()
        val current = altitudes.minByOrNull {
            Duration.between(instant, it.time).abs()
        }
        chart.moveMoon(current)
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

        val displayDate = binding.displayDate.date

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
                binding.astronomyTitle.title.text =
                    formatService.formatDuration(Duration.between(currentTime, nextSunrise))
                binding.astronomyTitle.subtitle.text = getString(
                    R.string.until_sun_time, getSunriseWording()
                )
            } else if (nextSunset != null) {
                binding.astronomyTitle.title.text =
                    formatService.formatDuration(Duration.between(currentTime, nextSunset))
                binding.astronomyTitle.subtitle.text = getString(
                    R.string.until_sun_time, getSunsetWording()
                )
            } else if (astronomyService.isSunUp(gps.location)) {
                binding.astronomyTitle.title.text = getString(R.string.sun_up_no_set)
                binding.astronomyTitle.subtitle.text = getString(R.string.sun_does_not_set)
            } else {
                binding.astronomyTitle.title.text = getString(R.string.sun_down_no_set)
                binding.astronomyTitle.subtitle.text = getString(R.string.sun_does_not_rise)
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

    private fun detectAndShowGPSError() {
        if (gpsErrorShown) {
            return
        }

        if (gps is OverrideGPS && gps.location == Coordinate.zero) {
            val activity = requireActivity() as MainActivity
            val navController = findNavController()
            val error = UserError(
                ErrorBannerReason.LocationNotSet,
                getString(R.string.location_not_set),
                R.drawable.satellite,
                getString(R.string.set)
            ) {
                activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
                navController.navigate(R.id.calibrateGPSFragment)
            }
            activity.errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CachedGPS && gps.location == Coordinate.zero) {
            val error = UserError(
                ErrorBannerReason.NoGPS,
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
