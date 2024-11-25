package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTideListBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.ToggleTideTableVisibilityCommand
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import com.kylecorry.trail_sense.tools.tides.ui.mappers.TideTableAction
import com.kylecorry.trail_sense.tools.tides.ui.mappers.TideTableListItemMapper

class TideListFragment : BoundFragment<FragmentTideListBinding>() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val tideRepo by lazy { TideTableRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val mapper by lazy {
        TideTableListItemMapper(
            requireContext(),
            this::onTideTableAction
        )
    }
    private val tideTypeCommand by lazy { CurrentTideTypeCommand(TideService(requireContext())) }

    private val tideLock = Any()
    private val tideTypeRunner = ParallelCoroutineRunner(4)
    private var tides by state(emptyList<Pair<TideTable, TideType?>>())

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideListBinding {
        return FragmentTideListBinding.inflate(layoutInflater, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tideList.emptyView = binding.tidesEmptyText

        refreshTides()

        binding.addBtn.setOnClickListener {
            createTide()
        }
    }

    private fun deleteTide(tide: TideTable) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_tide_prompt),
            getTideTitle(tide)
        ) { cancelled ->
            if (!cancelled) {
                inBackground {
                    onIO {
                        tideRepo.deleteTideTable(tide)
                    }

                    refreshTides()
                }
            }
        }
    }

    private fun onTideTableAction(tide: TideTable, action: TideTableAction) {
        when (action) {
            TideTableAction.Select -> selectTide(tide)
            TideTableAction.Edit -> editTide(tide)
            TideTableAction.Delete -> deleteTide(tide)
            TideTableAction.ToggleVisibility -> toggleVisibility(tide)
        }
    }

    private fun toggleVisibility(tide: TideTable) {
        inBackground {
            ToggleTideTableVisibilityCommand(requireContext()).execute(tide)
            refreshTides()
        }
    }

    private fun editTide(tide: TideTable) {
        findNavController().navigate(
            R.id.action_tideList_to_createTide,
            bundleOf("edit_tide_id" to tide.id)
        )
    }

    private fun createTide() {
        findNavController().navigate(R.id.action_tideList_to_createTide)
    }

    private fun selectTide(tide: TideTable) {
        prefs.tides.lastTide = tide.id
        findNavController().navigateUp()
    }

    private fun getTideTitle(tide: TideTable): String {
        return tide.name
            ?: if (tide.location != null) formatService.formatLocation(tide.location) else getString(
                android.R.string.untitled
            )
    }

    private fun refreshTides() {
        inBackground {
            tides = tideRepo.getTideTables().sortedWith(compareBy({ it.isEditable }, { tide ->
                tide.location?.distanceTo(gps.location) ?: Float.POSITIVE_INFINITY
            })).map { it to (null as TideType?) }

            // Update the tide types in parallel and update each time one is done
            tideTypeRunner.run(tides.mapIndexed { index, tide ->
                {
                    val type = tideTypeCommand.execute(tide.first)
                    synchronized(tideLock) {
                        val t = tides.toMutableList()
                        t[index] = tide.first to type
                        tides = t
                    }
                }
            })
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        effect("tides", tides, lifecycleHookTrigger.onResume()) {
            binding.tideList.setItems(tides, mapper)
        }
    }

}