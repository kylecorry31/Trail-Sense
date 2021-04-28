package com.kylecorry.trail_sense.astronomy.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.MainActivity
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.databinding.ActivityAstronomyBinding
import com.kylecorry.trail_sense.quickactions.LowPowerQuickAction
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.tools.flashlight.ui.QuickActionFlashlight
import com.kylecorry.trail_sense.tools.whistle.ui.QuickActionWhistle
import com.kylecorry.trail_sense.tools.whitenoise.ui.QuickActionWhiteNoise
import com.kylecorry.trailsensecore.domain.astronomy.MeteorShowerPeak
import com.kylecorry.trailsensecore.domain.astronomy.RiseSetTransitTimes
import com.kylecorry.trailsensecore.domain.astronomy.SunTimesMode
import com.kylecorry.trailsensecore.domain.astronomy.moon.MoonTruePhase
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import com.kylecorry.trailsensecore.infrastructure.view.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class AstronomyFragment : BoundFragment<ActivityAstronomyBinding>() {

    private lateinit var gps: IGPS

    private lateinit var detailList: ListView<AstroDetail>
    private lateinit var chart: AstroChart

    private lateinit var displayDate: LocalDate

    private lateinit var sunTimesMode: SunTimesMode

    private val sensorService by lazy { SensorService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { Cache(requireContext()) }
    private val astronomyService = AstronomyService()
    private val geoService = GeoService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private var leftQuickAction: QuickActionButton? = null
    private var rightQuickAction: QuickActionButton? = null

    private var gpsErrorShown = false

    private val intervalometer = Intervalometer {
        updateUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leftQuickAction =
            getQuickActionButton(prefs.astronomy.leftQuickAction, binding.astronomyLeftQuickAction)
        leftQuickAction?.onCreate()

        rightQuickAction = getQuickActionButton(
            prefs.astronomy.rightQuickAction,
            binding.astronomyRightQuickAction
        )
        rightQuickAction?.onCreate()

        val recyclerView = binding.astronomyDetailList
        detailList =
            ListView(recyclerView, R.layout.list_item_astronomy_detail) { itemView, detail ->
                val nameText: TextView = itemView.findViewById(R.id.astronomy_detail_name)
                val valueText: TextView = itemView.findViewById(R.id.astronomy_detail_value)
                val iconView: ImageView = itemView.findViewById(R.id.astronomy_detail_icon)

                if (detail.name == null) {
                    nameText.text = ""
                    valueText.text = ""
                    iconView.visibility = View.INVISIBLE
                    return@ListView
                }

                nameText.text = detail.name
                valueText.text = detail.value
                iconView.setImageResource(detail.icon)
                iconView.visibility = View.VISIBLE
                when {
                    detail.tint == -1 -> {
                        iconView.imageTintList = null
                    }
                    detail.tint != null -> {
                        iconView.imageTintList =
                            ColorStateList.valueOf(resources.getColor(detail.tint, null))
                    }
                    else -> {
                        iconView.imageTintList =
                            ColorStateList.valueOf(UiUtils.androidTextColorSecondary(requireContext()))
                    }
                }
            }


        chart = AstroChart(binding.sunMoonChart)

        binding.datePicker.setOnClickListener {
            UiUtils.pickDate(requireContext(), displayDate) {
                if (it != null) {
                    displayDate = it
                    updateUI()
                }
            }
        }

        gps = sensorService.getGPS()

        sunTimesMode = prefs.astronomy.sunTimesMode

        binding.sunPosition.setOnClickListener {
            openDetailsDialog()
        }

        binding.moonPosition.setOnClickListener {
            openDetailsDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        leftQuickAction?.onDestroy()
        rightQuickAction?.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        leftQuickAction?.onResume()
        rightQuickAction?.onResume()
        displayDate = LocalDate.now()
        requestLocationUpdate()
        intervalometer.interval(Duration.ofMinutes(1), Duration.ofMillis(200))
        updateUI()

        if (cache.getBoolean("cache_tap_sun_moon_shown") != true) {
            cache.putBoolean("cache_tap_sun_moon_shown", true)
            UiUtils.shortToast(requireContext(), getString(R.string.tap_sun_moon_hint))
        }

    }

    override fun onPause() {
        super.onPause()
        leftQuickAction?.onPause()
        rightQuickAction?.onPause()
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
        return if (!prefs.useAutoDeclination) {
            prefs.declinationOverride
        } else {
            geoService.getDeclination(gps.location, gps.altitude)
        }
    }

    private fun updateUI() {
        if (context == null) {
            return
        }
        lifecycleScope.launch {
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
        if (context == null) {
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
        if (context == null) {
            return
        }

        val moonAltitudes: List<Pair<LocalDateTime, Float>>
        val sunAltitudes: List<Pair<LocalDateTime, Float>>
        val startHour: Float

        withContext(Dispatchers.Default) {
            if (displayDate == LocalDate.now() && prefs.astronomy.centerSunAndMoon) {
                val startTime = LocalDateTime.now().roundNearestMinute(10).minusHours(12)
                startHour = startTime.hour + startTime.minute / 60f

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
                val current =
                    moonAltitudes.minByOrNull {
                        Duration.between(LocalDateTime.now(), it.first).abs()
                    }
                val currentIdx = moonAltitudes.indexOf(current)
                val point = chart.getPoint(1, currentIdx)
                binding.moonPosition.x = point.first - binding.moonPosition.width / 2f
                binding.moonPosition.y = point.second - binding.moonPosition.height / 2f

                val point2 = chart.getPoint(2, currentIdx)
                binding.sunPosition.x = point2.first - binding.sunPosition.width / 2f
                binding.sunPosition.y = point2.second - binding.sunPosition.height / 2f

                if (binding.moonPosition.height != 0) {
                    binding.moonPosition.visibility = View.VISIBLE
                }

                if (binding.sunPosition.height != 0) {
                    binding.sunPosition.visibility = View.VISIBLE
                }

            } else {
                binding.sunPosition.visibility = View.INVISIBLE
                binding.moonPosition.visibility = View.INVISIBLE
            }
        }
    }

    private suspend fun updateSunUI() {
        if (context == null) {
            return
        }

        displayTimeUntilNextSunEvent()
    }

    private fun openDetailsDialog() {
        // Altitude and azimuth
        val moonAltitude =
            astronomyService.getMoonAltitude(gps.location)
        val sunAltitude =
            astronomyService.getSunAltitude(gps.location)

        val declination =
            if (!prefs.navigation.useTrueNorth) getDeclination() else 0f

        val sunAzimuth =
            astronomyService.getSunAzimuth(gps.location).withDeclination(-declination).value
        val moonAzimuth =
            astronomyService.getMoonAzimuth(gps.location).withDeclination(-declination).value

        UiUtils.alert(
            requireContext(), getString(R.string.sun_and_moon), getString(
                R.string.sun_and_moon_position_details,
                getString(R.string.degree_format, sunAltitude),
                getString(R.string.degree_format, sunAzimuth),
                getString(R.string.degree_format, moonAltitude),
                getString(R.string.degree_format, moonAzimuth)
            )
        )
    }

    private suspend fun updateAstronomyDetails() {
        if (context == null) {
            return
        }

        var details: MutableList<AstroDetail>

        withContext(Dispatchers.Default) {
            // Rise / set times
            val sunTimes =
                astronomyService.getSunTimes(gps.location, SunTimesMode.Actual, displayDate)
            val duskDawn =
                astronomyService.getSunTimes(gps.location, SunTimesMode.Civil, displayDate)
            val moonTimes = astronomyService.getMoonTimes(gps.location, displayDate)
            val solarNoon = astronomyService.getSolarNoon(gps.location, displayDate)
            val lunarNoon = astronomyService.getLunarNoon(gps.location, displayDate)
            val meteorShower = astronomyService.getMeteorShower(gps.location, displayDate)

            // Sun and moon times
            details = listOf(
                Pair(
                    Pair(
                        R.drawable.ic_sun_rise to -1,
                        getString(R.string.sunrise_label)
                    ), sunTimes.rise?.toLocalDateTime()
                ),
                Pair(
                    Pair(
                        R.drawable.ic_sun_set to -1,
                        getString(R.string.sunset_label)
                    ), sunTimes.set?.toLocalDateTime()
                ),
                // TODO: Get moon icons
                Pair(
                    Pair(R.drawable.ic_moon_rise to -1, getString(R.string.moon_rise)),
                    moonTimes.rise?.toLocalDateTime()
                ),
                Pair(
                    Pair(R.drawable.ic_moon_set to -1, getString(R.string.moon_set)),
                    moonTimes.set?.toLocalDateTime()
                ),
                // TODO: Get solar/lunar noon images
                Pair(
                    Pair(R.drawable.ic_sun to -1, getString(R.string.solar_noon)),
                    solarNoon
                ),
                Pair(
                    Pair(R.drawable.ic_moon to -1, getString(R.string.lunar_noon)),
                    lunarNoon
                )
            ).filterNot { it.second == null }.sortedBy { it.second?.toLocalTime() }.map {
                AstroDetail(
                    it.first.first.first,
                    it.first.second,
                    getTimeString(it.second),
                    it.first.first.second
                )
            }.toMutableList()

            val duskDawnDetails = listOf(
                Pair(
                    Pair(
                        Pair(R.drawable.ic_sun_rise, -1),
                        getString(R.string.sun_dawn)
                    ),
                    duskDawn.rise?.toLocalDateTime()
                ),
                Pair(
                    Pair(
                        Pair(R.drawable.ic_sun_set, -1),
                        getString(R.string.sun_dusk)
                    ),
                    duskDawn.set?.toLocalDateTime()
                )
            ).filterNot { it.second == null }.sortedBy { it.second?.toLocalTime() }.map {
                AstroDetail(
                    it.first.first.first,
                    it.first.second,
                    getTimeString(it.second),
                    it.first.first.second
                )
            }

            // Add dusk and dawn
            if (duskDawnDetails.isNotEmpty()) {
                details.add(AstroDetail.spacer())
                details.addAll(duskDawnDetails)
            }

            details.add(AstroDetail.spacer())

            // Add the moon phase
            if (displayDate == LocalDate.now()) {
                // Moon phase
                val moonPhase = astronomyService.getCurrentMoonPhase()
                val illuminationString =
                    if (prefs.astronomy.showMoonIllumination) " (" + formatService.formatPercentage(
                        moonPhase.illumination
                    ) + ")" else ""

                details.add(
                    AstroDetail(
                        getMoonImage(moonPhase.phase),
                        getString(R.string.moon_phase),
                        getMoonPhaseString(moonPhase.phase) + illuminationString,
                        -1
                    )
                )
            } else {
                val moonPhase = astronomyService.getMoonPhase(displayDate)
                val illuminationString =
                    if (prefs.astronomy.showMoonIllumination) " (" + formatService.formatPercentage(
                        moonPhase.illumination
                    ) + ")" else ""
                details.add(
                    AstroDetail(
                        getMoonImage(moonPhase.phase),
                        getString(R.string.moon_phase),
                        getMoonPhaseString(moonPhase.phase) + illuminationString,
                        -1
                    )
                )
            }

            if (meteorShower != null && prefs.astronomy.showMeteorShowers) {
                details.add(
                    AstroDetail(
                        R.drawable.ic_meteor,
                        getString(R.string.meteor_shower),
                        getMeteorShowerTime(displayDate, meteorShower),
                        null
                    )
                )
            }
        }

        withContext(Dispatchers.Main) {
            detailList.setData(details)
        }

    }

    private fun getMeteorShowerTime(today: LocalDate, meteorShower: MeteorShowerPeak): String {
        return if (meteorShower.peak.toLocalDate() == today) {
            formatService.formatTime(meteorShower.peak.toLocalTime(), false)
        } else {
            getString(
                R.string.tomorrow_at,
                formatService.formatTime(meteorShower.peak.toLocalTime(), false)
            )
        } + "\n${getString(R.string.meteors_per_hour, meteorShower.shower.rate)}"
    }

    private fun getMoonImage(phase: MoonTruePhase): Int {
        return when (phase) {
            MoonTruePhase.FirstQuarter -> R.drawable.ic_moon_first_quarter
            MoonTruePhase.Full -> R.drawable.ic_moon
            MoonTruePhase.ThirdQuarter -> R.drawable.ic_moon_third_quarter
            MoonTruePhase.New -> R.drawable.ic_moon_new
            MoonTruePhase.WaningCrescent -> R.drawable.ic_moon_waning_crescent
            MoonTruePhase.WaningGibbous -> R.drawable.ic_moon_waning_gibbous
            MoonTruePhase.WaxingCrescent -> R.drawable.ic_moon_waxing_crescent
            MoonTruePhase.WaxingGibbous -> R.drawable.ic_moon_waxing_gibbous
        }
    }

    private fun getMoonPhaseString(phase: MoonTruePhase): String {
        return getString(
            when (phase) {
                MoonTruePhase.FirstQuarter -> R.string.first_quarter
                MoonTruePhase.Full -> R.string.full_moon
                MoonTruePhase.ThirdQuarter -> R.string.third_quarter
                MoonTruePhase.New -> R.string.new_moon
                MoonTruePhase.WaningCrescent -> R.string.waning_crescent
                MoonTruePhase.WaningGibbous -> R.string.waning_gibbous
                MoonTruePhase.WaxingCrescent -> R.string.waxing_crescent
                MoonTruePhase.WaxingGibbous -> R.string.waxing_gibbous
            }
        )
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
                binding.remainingTime.text = Duration.between(currentTime, nextSunrise).formatHM()
                binding.remainingTimeLbl.text = getString(
                    R.string.until_sun_time, getSunriseWording()
                )
            } else if (nextSunset != null) {
                binding.remainingTime.text = Duration.between(currentTime, nextSunset).formatHM()
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

    private fun getTimeString(time: LocalDateTime?): String {
        return time?.toDisplayFormat(requireContext()) ?: "-"
    }

    private fun getDateString(date: LocalDate): String {
        val now = LocalDate.now()

        return when (date) {
            now -> {
                getString(R.string.today)
            }
            now.plusDays(1) -> {
                getString(R.string.tomorrow)
            }
            now.minusDays(1) -> {
                getString(R.string.yesterday)
            }
            else -> {
                DateUtils.formatDateTime(
                    requireContext(),
                    date.atStartOfDay().toEpochMillis(),
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_RELATIVE
                )
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

    private fun getQuickActionButton(
        type: QuickActionType,
        button: FloatingActionButton
    ): QuickActionButton {
        return when (type) {
            QuickActionType.Whistle -> QuickActionWhistle(button, this)
            QuickActionType.Flashlight -> QuickActionFlashlight(button, this)
            QuickActionType.WhiteNoise -> QuickActionWhiteNoise(button, this)
            QuickActionType.LowPowerMode -> LowPowerQuickAction(button, this)
            else -> QuickActionNone(button, this)
        }
    }

    data class AstroDetail(
        val icon: Int,
        val name: String?,
        val value: String,
        val tint: Int? = null
    ) {
        companion object {
            fun spacer(): AstroDetail {
                return AstroDetail(0, null, "")
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityAstronomyBinding {
        return ActivityAstronomyBinding.inflate(layoutInflater, container, false)
    }

}
