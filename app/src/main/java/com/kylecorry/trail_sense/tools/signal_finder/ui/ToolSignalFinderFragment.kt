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
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel

class ToolSignalFinderFragment : BoundFragment<FragmentSignalFinderBinding>() {

    private val sensors by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensors.getGPS() }
    private val cellSignal by lazy { sensors.getCellSignal(false) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

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
        val resolution = memo2 {
            val accuracy =
                CellTowerModel.accuracy.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            formatter.formatDistance(accuracy, Units.getDecimalPlaces(accuracy.units))
        }

        effect2(signals, nearby, location, lifecycleHookTrigger.onResume()) {

            // TODO: Indicate if the signal is from the user's carrier
            val signalItems = signals.mapIndexed { index, signal ->
                ListItem(
                    index.toLong(),
                    formatter.formatCellNetwork(signal.network),
                    formatter.formatPercentage(signal.strength)
                )
            }

            // TODO: Option to navigate
            val nearbyItems = nearby.flatMapIndexed { index, (towerLocation, networks) ->
                val distance =
                    Distance.meters((location ?: Coordinate.zero).distanceTo(towerLocation))
                        .convertTo(prefs.baseDistanceUnits).toRelativeDistance()
                val direction = (location ?: Coordinate.zero).bearingTo(towerLocation)
                val formattedDistance =
                    formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units))
                val formattedBearing = formatter.formatDegrees(direction.value, replace360 = true)
                val formattedDirection = formatter.formatDirection(direction.direction)
                networks.mapIndexed { networkIndex, network ->
                    ListItem(
                        (index * 1000 + networkIndex).toLong(),
                        formatter.formatCellNetwork(network),
                        getString(
                            R.string.dot_separated_pair,
                            formattedDistance,
                            // TODO: Add an approximate disclaimer/link at the bottom of the screen
                            "${formattedBearing} ${formattedDirection}\nApproximate from OpenCelliD (± ${resolution})"
                        )
                    )
                }
            }

            binding.list.setItems(signalItems + nearbyItems)
        }

        effect2(
            triggers.distance("location", location ?: Coordinate.zero, Distance.meters(100f)),
            lifecycleHookTrigger.onResume()
        ) {
            val location = location ?: return@effect2
            inBackground {
                loading = true
                queue.replace {
                    nearby = CellTowerModel.getTowers(
                        requireContext(),
                        Geofence(location, Distance.kilometers(20f)),
                        5
                    ).sortedBy { location.distanceTo(it.first) }
                    loading = false
                }
            }
        }

        effect2(loading) {
            binding.title.subtitle.text = if (loading) {
                getString(R.string.loading)
            } else {
                null
            }
        }
    }
}