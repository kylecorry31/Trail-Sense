package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideBinding
import com.kylecorry.trail_sense.databinding.ListItemTideBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.loading.TideLoaderFactory
import com.kylecorry.trail_sense.tools.tides.ui.tidelistitem.DefaultTideListItemFactory
import com.kylecorry.trail_sense.tools.tides.ui.tidelistitem.EstimatedTideListItemFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*


class TidesFragment : BoundFragment<FragmentTideBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private var displayDate = LocalDate.now()
    private val tideService = TideService()
    private lateinit var tideList: ListView<Tide>
    private var table: TideTable? = null
    private lateinit var chart: TideChart
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private var current: CurrentTideData? = null
    private var daily: DailyTideData? = null

    private var currentRefreshTimer = Timer {
        runInBackground { refreshCurrent() }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideBinding {
        return FragmentTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = TideChart(binding.chart)
        tideList = ListView(binding.tideList, R.layout.list_item_tide) { itemView, tide ->
            val tideBinding = ListItemTideBinding.bind(itemView)
            val isEstimated = this.table?.tides?.none { t -> t.time == tide.time } ?: true
            val factory = if (isEstimated){
                EstimatedTideListItemFactory(requireContext())
            } else {
                DefaultTideListItemFactory(requireContext())
            }
            val item = factory.create(tide)
            item.display(tideBinding)
        }

        binding.tideListDateText.text = formatService.formatRelativeDate(displayDate)

        CustomUiUtils.setButtonState(binding.tideCalibration, false)
        binding.tideCalibration.setOnClickListener {
            findNavController().navigate(R.id.action_tides_to_tideList)
        }
        binding.tideListDatePicker.setOnClickListener {
            Pickers.date(requireContext(), displayDate) {
                if (it != null) {
                    displayDate = it
                    onDisplayDateChanged()
                }
            }
        }

        binding.tideListDatePicker.setOnLongClickListener {
            displayDate = LocalDate.now()
            onDisplayDateChanged()
            true
        }

        binding.loading.isVisible = true
        runInBackground {
            val loader = TideLoaderFactory().getTideLoader(requireContext())
            table = loader.getTideTable()
            withContext(Dispatchers.Main) {
                if (isBound) {
                    binding.loading.isVisible = false
                    if (table == null) {
                        Alerts.dialog(
                            requireContext(),
                            getString(R.string.no_tides),
                            getString(R.string.calibrate_new_tide)
                        ) { cancelled ->
                            if (!cancelled) {
                                findNavController().navigate(R.id.action_tides_to_tideList)
                            }
                        }
                    } else {
                        onTideLoaded()
                    }
                }
            }
        }

        binding.nextDate.setOnClickListener {
            displayDate = displayDate.plusDays(1)
            onDisplayDateChanged()
        }

        binding.prevDate.setOnClickListener {
            displayDate = displayDate.minusDays(1)
            onDisplayDateChanged()
        }


        scheduleUpdates(20)
    }

    private fun onTideLoaded() {
        if (!isBound) return
        val tide = table ?: return
        binding.tideLocation.text = tide.name
            ?: if (tide.location != null) formatService.formatLocation(tide.location) else getString(
                android.R.string.untitled
            )
        runInBackground {
            refreshDaily()
            refreshCurrent()
        }
    }

    private fun onDisplayDateChanged() {
        if (!isBound) return
        runInBackground {
            refreshDaily()
        }
        binding.tideListDateText.text = formatService.formatRelativeDate(displayDate)
    }

    private fun updateDaily() {
        if (!isBound) return
        val daily = daily ?: return
        chart.plot(daily.waterLevels, daily.waterLevelRange)
        tideList.setData(daily.tides)

    }

    private fun updateCurrent() {
        if (!isBound) return
        val current = current ?: return
        val daily = daily ?: return

        binding.tideHeight.text =
            getTideTypeName(current.type) + if (current.waterLevel != null) {
                val height = Distance.meters(current.waterLevel).convertTo(units)
                " (${formatService.formatDistance(height, 2, true)})"
            } else {
                ""
            }
        val currentLevel = daily.waterLevels.minByOrNull {
            Duration.between(Instant.now(), it.time).abs()
        }
        val currentIdx = daily.waterLevels.indexOf(currentLevel)
        val point = chart.getPoint(currentIdx)
        binding.position.isInvisible =
            point.x == binding.chart.x && point.y == binding.chart.y || displayDate != LocalDate.now()
        binding.position.x = point.x - binding.position.width / 2f
        binding.position.y = point.y - binding.position.height / 2f
        binding.tideTendency.isVisible = true
        binding.tideTendency.setImageResource(if (current.rising) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
    }

    override fun onResume() {
        super.onResume()
        currentRefreshTimer.interval(Duration.ofMinutes(1))
        displayDate = LocalDate.now()
        onDisplayDateChanged()
    }

    override fun onPause() {
        super.onPause()
        currentRefreshTimer.stop()
    }

    override fun onUpdate() {
        super.onUpdate()
        updateCurrent()
    }

    private suspend fun refreshCurrent() {
        current = getCurrentTideData()
        withContext(Dispatchers.Main) {
            updateCurrent()
        }
    }

    private suspend fun refreshDaily() {
        daily = getDailyTideData(displayDate)
        withContext(Dispatchers.Main) {
            updateDaily()
        }
    }

    private suspend fun getCurrentTideData(): CurrentTideData? = withContext(Dispatchers.Default) {
        val table = table ?: return@withContext null
        val now = ZonedDateTime.now()
        val level = tideService.getWaterLevel(table, now)
        val isRising = tideService.isRising(table, now)
        val type = tideService.getCurrentTide(table, now)
        val withinTable = tideService.isWithinTideTable(table, now)
        CurrentTideData(if (withinTable) level else null, type, isRising)
    }

    private suspend fun getDailyTideData(date: LocalDate): DailyTideData? =
        withContext(Dispatchers.Default) {
            val table = table ?: return@withContext null
            val levels = tideService.getWaterLevels(table, date)
            val tides = tideService.getTides(table, date)
            val range = tideService.getRange(table)
            DailyTideData(levels, tides, range)
        }

    private fun getTideTypeName(tideType: TideType?): String {
        return when (tideType) {
            TideType.High -> getString(R.string.high_tide)
            TideType.Low -> getString(R.string.low_tide)
            null -> getString(R.string.half_tide)
        }
    }
}