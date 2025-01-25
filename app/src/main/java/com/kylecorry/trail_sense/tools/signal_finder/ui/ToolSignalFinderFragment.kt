package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentSignalFinderBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel

class ToolSignalFinderFragment : BoundFragment<FragmentSignalFinderBinding>() {

    private val sensors by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensors.getGPS() }
    private val cellSignal by lazy { sensors.getCellSignal() }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private var location by state<Coordinate?>(null)
    private var signals by state<List<CellSignal>>(emptyList())
    private var nearby by state<List<Pair<Coordinate, List<CellNetwork>>>>(emptyList())
    private var loading by state(false)

    private val triggers = HookTriggers()

    private val queue = CoroutineQueueRunner()

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSignalFinderBinding {
        return FragmentSignalFinderBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(gps) {
            location = gps.location
        }
        observe(cellSignal) {
            // TODO: Trilaterate
            signals = cellSignal.signals
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        effect2(signals, nearby, lifecycleHookTrigger.onResume()) {
            val signalItems = signals.mapIndexed { index, signal ->
                ListItem(
                    index.toLong(),
                    formatter.formatCellNetwork(signal.network),
                    formatter.formatPercentage(signal.strength)
                )
            }

            // TODO: Display distance, direction, and option to navigate
            val nearbyItems = nearby.flatMapIndexed { index, (location, networks) ->
                networks.mapIndexed { networkIndex, network ->
                    ListItem(
                        (index * 1000 + networkIndex).toLong(),
                        formatter.formatCellNetwork(network),
                        formatter.formatLocation(location)
                    )
                }
            }

            binding.list.setItems(signalItems + nearbyItems)
        }

        effect2(
            triggers.distance("location", location ?: Coordinate.zero, Distance.meters(100f)),
            lifecycleHookTrigger.onResume()
        ) {
            location?.let {
                inBackground {
                    loading = true
                    queue.replace {
                        nearby = CellTowerModel.getTowers(
                            requireContext(),
                            Geofence(it, Distance.kilometers(20f))
                        )
                        loading = false
                    }
                }
            }
        }

        effect2(loading){
            binding.title.subtitle.text = if (loading) {
                getString(R.string.loading)
            } else {
                null
            }
        }
    }
}