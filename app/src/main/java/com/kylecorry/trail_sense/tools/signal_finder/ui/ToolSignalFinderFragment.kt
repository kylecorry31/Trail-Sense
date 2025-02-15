package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCellSignalSensor
import com.kylecorry.trail_sense.shared.extensions.useCoroutineQueue
import com.kylecorry.trail_sense.shared.extensions.useLocation
import com.kylecorry.trail_sense.shared.extensions.useService
import com.kylecorry.trail_sense.shared.extensions.useTopic
import com.kylecorry.trail_sense.shared.extensions.useTriggers
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class ToolSignalFinderFragment : TrailSenseReactiveFragment(R.layout.fragment_signal_finder) {

    override fun update() {
        // Views
        val list = useView2<AndromedaListView>(R.id.list)
        val disclaimer = useView2<TextView>(R.id.disclaimer)
        val emptyText = useView2<TextView>(R.id.empty_text)
        val title = useView2<Toolbar>(R.id.title)

        // Services
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val markdown = useService<MarkdownService>()
        val navigator = useService<Navigator>()

        // Other objects
        val queue = useCoroutineQueue()
        val triggers = useTriggers()

        // Sensor readings
        val signals = useCellSignals()
        val location = useLocation(Duration.ofSeconds(5))

        // State
        val (nearby, setNearby) = useState<List<Pair<Coordinate, List<CellNetwork>>>>(emptyList())
        val (loading, setLoading) = useState(false)

        list.emptyView = emptyText

        // Set up the disclaimer
        useEffect(disclaimer) {
            disclaimer.text = markdown.toMarkdown(getString(R.string.cell_tower_disclaimer))
            disclaimer.movementMethod = LinkMovementMethodCompat.getInstance()
        }

        // List items
        useEffect(
            list,
            signals,
            nearby,
            distance(location, Distance.meters(100f))
        ) {

            val signalItems = signals.mapIndexed { index, signal ->
                ListItem(
                    index.toLong(),
                    formatter.formatCellNetwork(signal.network),
                    formatter.join(
                        formatter.formatPercentage(signal.strength),
                        formatter.formatTime(signal.time),
                        if (signal.isRegistered) {
                            getString(R.string.full_service)
                        } else {
                            getString(R.string.emergency_calls_only)
                        },
                        separator = FormatService.Separator.Dot
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
                        formatter.join(
                            getString(R.string.cell_tower),
                            formattedDistance,
                            "$formattedBearing $formattedDirection",
                            separator = FormatService.Separator.Dot
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

            list.setItems(signalItems + nearbyItems)
        }

        // Cell tower updating
        useEffect(
            distance(location, Distance.meters(100f))
        ) {
            inBackground {
                setLoading(true)
                queue.replace {
                    setNearby(CellTowerModel.getTowers(
                        requireContext(),
                        Geofence(location, Distance.kilometers(20f)),
                        5
                    ).sortedBy { location.distanceTo(it.first) })
                    setLoading(false)
                }
            }
        }

        // Loading
        useEffect(title, loading) {
            title.subtitle.text = if (loading) {
                getString(R.string.loading)
            } else {
                null
            }
        }
    }

    private fun useCellSignals(): List<CellSignal> {
        val cellSignal = useCellSignalSensor(false)
        return useTopic(cellSignal, emptyList()) { it.signals }
    }
}