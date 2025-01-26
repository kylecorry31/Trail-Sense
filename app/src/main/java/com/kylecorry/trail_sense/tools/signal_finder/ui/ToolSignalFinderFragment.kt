package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentSignalFinderBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolSignalFinderFragment : BoundFragment<FragmentSignalFinderBinding>() {

    private val sensors by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensors.getGPS() }
    private val cellSignal by lazy { sensors.getCellSignal(false) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val markdown by lazy { MarkdownService(requireContext()) }
    private val navigator by lazy { Navigator.getInstance(requireContext()) }

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

        binding.list.emptyView = binding.emptyText

        binding.disclaimer.text =
            markdown.toMarkdown(getString(R.string.cell_tower_disclaimer))
        binding.disclaimer.movementMethod = LinkMovementMethodCompat.getInstance()
    }

    override fun onUpdate() {
        super.onUpdate()
        effect2(
            signals,
            nearby,
            triggers.distance("location1", location ?: Coordinate.zero, Distance.meters(100f)),
            lifecycleHookTrigger.onResume()
        ) {

            val signalItems = signals.mapIndexed { index, signal ->
                ListItem(
                    index.toLong(),
                    formatter.formatCellNetwork(signal.network),
                    getString(
                        R.string.dot_separated_pair,
                        "${formatter.formatPercentage(signal.strength)} (${
                            formatter.formatDbm(signal.dbm)
                        })",
                        if (signal.isRegistered) {
                            getString(R.string.full_service)
                        } else {
                            getString(R.string.emergency_calls_only)
                        }
                    ),
                    icon = ResourceListIcon(
                        CellSignalUtils.getCellQualityImage(signal.quality),
                        CustomUiUtils.getQualityColor(signal.quality)
                    )
                )
            }

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
                            "$formattedBearing $formattedDirection"
                        ),
                        icon = ResourceListIcon(
                            R.drawable.cell_tower,
                            Resources.androidTextColorSecondary(requireContext())
                        ),
                        menu = listOf(
                            ListMenuItem(getString(R.string.navigate)) {
                                navigator.navigateTo(towerLocation, getString(R.string.cell_tower))
                                findNavController().openTool(Tools.NAVIGATION)
                            },
                            ListMenuItem(getString(R.string.create_beacon)) {
                                val bundle = bundleOf(
                                    "initial_location" to GeoUri(towerLocation)
                                )
                                findNavController().navigate(R.id.placeBeaconFragment, bundle)
                            }
                        )
                    )
                }
            }

            binding.list.setItems(signalItems + nearbyItems)
        }

        effect2(
            triggers.distance("location2", location ?: Coordinate.zero, Distance.meters(100f)),
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