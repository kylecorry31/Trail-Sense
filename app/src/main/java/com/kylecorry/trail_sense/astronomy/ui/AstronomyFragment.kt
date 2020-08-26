package com.kylecorry.trail_sense.astronomy.ui

import android.content.res.ColorStateList
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstroAltitude
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.domain.moon.MoonTruePhase
import com.kylecorry.trail_sense.astronomy.domain.moon.Tide
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trail_sense.shared.sensors.IGPS
import com.kylecorry.trail_sense.shared.system.UiUtils
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
    private lateinit var timer: Timer
    private lateinit var handler: Handler
    private lateinit var moonPosition: ImageView
    private lateinit var sunPosition: ImageView
    private lateinit var detailList: RecyclerView
    private lateinit var adapter: DetailAdapter

    private lateinit var prevDateBtn: ImageButton
    private lateinit var nextDateBtn: ImageButton
    private lateinit var dateTxt: TextView
    private lateinit var chart: AstroChart

    private lateinit var displayDate: LocalDate

    private lateinit var sunTimesMode: SunTimesMode

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val astronomyService = AstronomyService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_astronomy, container, false)

        detailList = view.findViewById(R.id.astronomy_detail_list)
        val layoutManager = LinearLayoutManager(context)
        detailList.layoutManager = layoutManager

//        val dividerItemDecoration = DividerItemDecoration(
//            context,
//            layoutManager.orientation
//        )
//        detailList.addItemDecoration(dividerItemDecoration)

        adapter = DetailAdapter(listOf())
        detailList.adapter = adapter

        sunTxt = view.findViewById(R.id.remaining_time)
        remDaylightTxt = view.findViewById(R.id.remaining_time_lbl)
        sunPosition = view.findViewById(R.id.sun_position)
        moonPosition = view.findViewById(R.id.moon_position)

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
        updateAstronomyDetails()
    }

    private fun updateMoonUI() {
        if (context == null) {
            return
        }

        val moonPhase = astronomyService.getCurrentMoonPhase()
        moonPosition.setImageResource(getMoonImage(moonPhase.phase))
    }

    private fun updateAstronomyChart() {
        if (context == null) {
            return
        }

        val moonAltitudes: List<AstroAltitude>
        val sunAltitudes: List<AstroAltitude>
        val startHour: Float

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

        chart.plot(
            listOf(
                AstroChart.AstroChartDataset(
                    moonAltitudes,
                    resources.getColor(R.color.white, null)
                ),
                AstroChart.AstroChartDataset(
                    sunAltitudes,
                    resources.getColor(R.color.colorPrimary, null)
                )
            ),
            startHour
        )

        if (displayDate == LocalDate.now()) {
            val current =
                moonAltitudes.minBy { Duration.between(LocalDateTime.now(), it.time).abs() }
            val currentIdx = moonAltitudes.indexOf(current)
            val point = chart.getPoint(1, currentIdx)
            moonPosition.x = point.first - moonPosition.width / 2f
            moonPosition.y = point.second - moonPosition.height / 2f

            val point2 = chart.getPoint(2, currentIdx)
            sunPosition.x = point2.first - sunPosition.width / 2f
            sunPosition.y = point2.second - sunPosition.height / 2f


            if (moonPosition.height != 0) {
                moonPosition.visibility = View.VISIBLE
            }

            if (sunPosition.height != 0) {
                sunPosition.visibility = View.VISIBLE
            }

        } else {
            sunPosition.visibility = View.INVISIBLE
            moonPosition.visibility = View.INVISIBLE
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

    private fun updateAstronomyDetails() {
        if (context == null) {
            return
        }

        // Rise / set times
        val sunTimes = astronomyService.getSunTimes(gps.location, sunTimesMode, displayDate)
        val moonTimes = astronomyService.getMoonTimes(gps.location, displayDate)
        val solarNoon = astronomyService.getSolarNoon(gps.location, displayDate)
        val lunarNoon = astronomyService.getLunarNoon(gps.location, displayDate)

        val details = listOf(
            Pair(
                Pair(
                    Pair(R.drawable.sunrise, R.color.colorPrimary),
                    getSunriseWording()
                ), sunTimes.up
            ),
            Pair(
                Pair(
                    Pair(R.drawable.sunset, R.color.colorPrimary),
                    getSunsetWording()
                ), sunTimes.down
            ),
            // TODO: Get moon icons
            Pair(
                Pair(Pair(R.drawable.moonrise, null), getString(R.string.moon_rise)),
                moonTimes.up
            ),
            Pair(
                Pair(Pair(R.drawable.moonset, null), getString(R.string.moon_set)),
                moonTimes.down
            ),
            // TODO: Get solar/lunar noon images
            Pair(
                Pair(Pair(R.drawable.sun, R.color.colorPrimary), getString(R.string.solar_noon)),
                solarNoon
            ),
            Pair(
                Pair(Pair(R.drawable.moon_full, null), getString(R.string.lunar_noon)),
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


        details.add(AstroDetail.spacer())

        if (displayDate == LocalDate.now()) {
            // Moon phase
            val moonPhase = astronomyService.getCurrentMoonPhase()

            details.add(
                AstroDetail(
                    getMoonImage(moonPhase.phase),
                    getString(R.string.moon_phase),
                    getString(moonPhase.phase.longNameResource)
                )
            )
            details.add(
                AstroDetail(
                    R.drawable.illumination,
                    getString(R.string.moon_illumination),
                    getString(R.string.percent_format, moonPhase.illumination.roundToInt())
                )
            )

            details.add(AstroDetail.spacer())

            val moonAltitude =
                astronomyService.getMoonAltitude(gps.location).altitudeDegrees.roundToInt()
            val sunAltitude =
                astronomyService.getSunAltitude(gps.location).altitudeDegrees.roundToInt()

            // TODO: Add icons
            details.add(
                AstroDetail(
                    R.drawable.sun,
                    getString(R.string.sun_altitude),
                    getString(R.string.degree_format, sunAltitude),
                    R.color.colorPrimary
                )
            )
            details.add(
                AstroDetail(
                    R.drawable.moon_full,
                    getString(R.string.moon_altitude),
                    getString(R.string.degree_format, moonAltitude)
                )
            )

            val sunAzimuth = astronomyService.getSunAzimuth(gps.location).value.roundToInt()
            val moonAzimuth = astronomyService.getMoonAzimuth(gps.location).value.roundToInt()

            details.add(AstroDetail(R.drawable.sun, "Sun azimuth", getString(R.string.degree_format, sunAzimuth), R.color.colorPrimary))
            details.add(AstroDetail(R.drawable.moon_full, "Moon azimuth", getString(R.string.degree_format, moonAzimuth)))

        } else {
            val moonPhase = astronomyService.getMoonPhase(displayDate)
            details.add(
                AstroDetail(
                    getMoonImage(moonPhase.phase),
                    getString(R.string.moon_phase),
                    getString(moonPhase.phase.longNameResource)
                )
            )
            details.add(
                AstroDetail(
                    R.drawable.illumination,
                    getString(R.string.moon_illumination),
                    getString(R.string.percent_format, moonPhase.illumination.roundToInt())
                )
            )
        }

        details.add(AstroDetail.spacer())
        val tide = astronomyService.getTides(displayDate)
        details.add(
            AstroDetail(
                R.drawable.tides,
                getString(R.string.tidal_range),
                getTideString(tide),
                R.color.colorAccent
            )
        )

        adapter.details = details
    }

    private fun getTideString(tide: Tide): String {
        return when (tide) {
            Tide.Neap -> getString(R.string.tide_neap)
            Tide.Spring -> getString(R.string.tide_spring)
            Tide.Normal -> getString(R.string.tide_normal)
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
                R.string.until_sun_time, getSunriseWording()
            )
        } else if (nextSunset != null) {
            sunTxt.text = Duration.between(currentTime, nextSunset).formatHM()
            remDaylightTxt.text = getString(
                R.string.until_sun_time, getSunsetWording()
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

    inner class DetailHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var nameText: TextView = itemView.findViewById(R.id.astronomy_detail_name)
        private var valueText: TextView = itemView.findViewById(R.id.astronomy_detail_value)
        private var iconView: ImageView = itemView.findViewById(R.id.astronomy_detail_icon)

        fun bindToDetail(detail: AstroDetail) {
            if (detail.name == null) {
                nameText.text = ""
                valueText.text = ""
                iconView.visibility = View.INVISIBLE
                return
            }

            nameText.text = detail.name
            valueText.text = detail.value
            iconView.setImageResource(detail.icon)
            iconView.visibility = View.VISIBLE
            if (detail.tint != null) {
                iconView.imageTintList =
                    ColorStateList.valueOf(resources.getColor(detail.tint, null))
            } else {
                iconView.imageTintList =
                    ColorStateList.valueOf(UiUtils.androidTextColorSecondary(requireContext()))
            }
        }
    }


    inner class DetailAdapter(mDetails: List<AstroDetail>) : RecyclerView.Adapter<DetailHolder>() {

        var details: List<AstroDetail> = mDetails
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailHolder {
            val view = layoutInflater.inflate(R.layout.list_item_astronomy_detail, parent, false)
            return DetailHolder(view)
        }

        override fun getItemCount(): Int {
            return details.size
        }

        override fun onBindViewHolder(holder: DetailHolder, position: Int) {
            val detail = details[position]
            holder.bindToDetail(detail)
        }

    }


}
