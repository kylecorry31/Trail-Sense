package com.kylecorry.trail_sense.tools.tides.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.DailyTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadTideTableCommand
import com.kylecorry.trail_sense.tools.tides.ui.mappers.TideListItemMapper
import java.time.Duration
import java.time.Instant
import java.time.LocalDate


class TidesFragment : BoundFragment<FragmentTideBinding>() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val tideService by lazy { TideService(requireContext()) }
    private var table: TideTable? by state(null)
    private lateinit var chart: TideChart
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private var current: CurrentTideData? by state(null)
    private var daily: DailyTideData? by state(null)
    private var displayDate by state(LocalDate.now())
    private val mapper by lazy { TideListItemMapper(requireContext()) }
    private val currentTideCommand by lazy { CurrentTideCommand(tideService) }
    private val dailyTideCommand by lazy { DailyTideCommand(tideService) }
    private val loadTideCommand by lazy { LoadTideTableCommand(requireContext()) }
    private val triggers = HookTriggers()

    private val currentTideRunner = CoroutineQueueRunner()
    private val dailyTideRunner = CoroutineQueueRunner()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideBinding {
        return FragmentTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = TideChart(binding.chart)

        binding.tideTitle.rightButton.setOnClickListener {
            table = null
            findNavController().navigate(R.id.action_tides_to_tideList)
        }

        binding.loading.isVisible = true

        binding.tideListDate.setOnDateChangeListener {
            displayDate = it
        }

        scheduleUpdates(INTERVAL_1_FPS)
    }

    private fun updateTideList(tides: List<Tide>) {
        val updatedTides = tides.map {
            val matchingTide = table?.tides?.firstOrNull { t ->
                Duration.between(t.time, it.time).abs() < Duration.ofMinutes(2)
            }

            it.copy(height = matchingTide?.height)
        }

        binding.tideList.setItems(updatedTides, mapper)
    }

    private fun loadTideTable(newTable: TideTable? = null) {
        if (!isBound) return
        binding.loading.isVisible = true
        binding.tideList.setItems(emptyList())
        inBackground {
            val updatedTable = newTable ?: loadTideCommand.execute()
            val locationSubsystem = LocationSubsystem.getInstance(requireContext())
            if (updatedTable?.isAutomaticNearbyTide == true && locationSubsystem.locationAge > Duration.ofMinutes(
                    30
                )
            ) {
                locationSubsystem.updateLocation()
            }

            table = updatedTable
            onMain {
                if (!isBound) return@onMain
                binding.loading.isVisible = false
                if (table == null) {
                    val cancelled = CoroutineAlerts.dialog(
                        requireContext(),
                        getString(R.string.no_tides),
                        getString(R.string.calibrate_new_tide)
                    )
                    if (!cancelled) {
                        findNavController().navigate(R.id.action_tides_to_tideList)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (table == null) {
            displayDate = LocalDate.now()
            ShowTideDisclaimerCommand(this) {
                loadTideTable()
            }.execute()
        }
    }

    override fun onPause() {
        super.onPause()
        currentTideRunner.cancel()
        dailyTideRunner.cancel()
    }

    override fun onUpdate() {
        super.onUpdate()

        effect("displayDate", displayDate, lifecycleHookTrigger.onResume()) {
            // This will only trigger a subsequent change event if it actually changes
            binding.tideListDate.date = displayDate
        }

        effect("update_daily", table, displayDate, lifecycleHookTrigger.onResume()) {
            inBackground {
                dailyTideRunner.replace {
                    daily = table?.let { dailyTideCommand.execute(it, displayDate) }
                }
            }
        }

        effect(
            "update_current",
            table,
            displayDate,
            lifecycleHookTrigger.onResume(),
            triggers.frequency("tide_current", Duration.ofMinutes(1))
        ) {
            inBackground {
                currentTideRunner.replace {
                    current = table?.let { currentTideCommand.execute(it) }
                }
            }
        }

        effect("current", current, lifecycleHookTrigger.onResume()) {
            val current = current ?: return@effect
            val name = TideFormatter(requireContext()).getTideTypeName(current.type)
            val waterLevel = if (current.waterLevel != null) {
                val height = Distance.meters(current.waterLevel).convertTo(units)
                " (${formatService.formatDistance(height, 2, true)})"
            } else {
                ""
            }
            @SuppressLint("SetTextI18n")
            binding.tideTitle.title.text = name + waterLevel
            binding.tideTitle.title.setCompoundDrawables(
                Resources.dp(requireContext(), 24f).toInt(),
                left = if (current.rising) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            )
            CustomUiUtils.setImageColor(
                binding.tideTitle.title,
                Resources.androidTextColorSecondary(requireContext())
            )
        }

        effect("daily", daily, displayDate, lifecycleHookTrigger.onResume()) {
            val daily = daily ?: return@effect
            chart.plot(daily.waterLevels, daily.waterLevelRange)
            updateTideList(daily.tides)
        }

        effect("highlight", daily, displayDate, lifecycleHookTrigger.onResume()) {
            val daily = daily ?: return@effect
            val currentLevel = daily.waterLevels.minByOrNull {
                Duration.between(Instant.now(), it.time).abs()
            }
            if (currentLevel != null && displayDate == LocalDate.now()) {
                chart.highlight(currentLevel, daily.waterLevelRange)
            } else {
                chart.removeHighlight()
            }
        }

        effect("name", table, lifecycleHookTrigger.onResume()) {
            val table = table ?: return@effect
            binding.tideTitle.subtitle.text = table.name
                ?: if (table.location != null) formatService.formatLocation(table.location) else getString(
                    android.R.string.untitled
                )
        }

    }

}