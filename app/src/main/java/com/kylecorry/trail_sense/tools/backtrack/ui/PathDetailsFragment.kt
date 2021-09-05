package com.kylecorry.trail_sense.tools.backtrack.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.filterSatisfied
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPathBottomSheetBinding
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.beacons.BeaconOwner
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.domain.factories.*
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.SelectedPointDecorator
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.IsCurrentPathSpecification
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.IsValidBacktrackPointSpecification
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class PathDetailsFragment : BoundFragment<FragmentPathBottomSheetBinding>() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { Preferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val compass by lazy { sensorService.getCompass() }
    private val waypointRepo by lazy { WaypointRepo.getInstance(requireContext()) }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val geoService = GeologyService()

    private var drawPathToGPS = false
    private var path: List<PathPoint> = emptyList()
    private var pathId: Long = 0L
    private var selectedPointId: Long? = null
    private lateinit var listView: ListView<PathPoint>


    private var pointColoringStyle = PointColoringStyle.None

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathId = requireArguments().getLong("path_id")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = ListView(binding.waypointsList, R.layout.list_item_waypoint) { itemView, item ->
            drawWaypointListItem(ListItemWaypointBinding.bind(itemView), item)
        }
        listView.addLineSeparator()

        binding.pathImage.setOnPointClickListener {
            viewWaypoint(it)
        }

        waypointRepo.getWaypointsByPath(pathId).observe(viewLifecycleOwner, {
            path = filterCurrentWaypoints(it).sortedByDescending { p -> p.time }
            val selected = selectedPointId
            if (selected != null && path.find { it.id == selected } == null) {
                selectedPointId = null
            }
            listView.setData(path)
            updatePointStyleLegend()
            onPathChanged()
        })

        gps.asLiveData().observe(viewLifecycleOwner, {
            compass.declination = getDeclination()
            // TODO: Only update path when needed
            onPathChanged()
        })

        compass.asLiveData().observe(viewLifecycleOwner, {
            // TODO: Only update path when needed
            onPathChanged()
        })

        binding.pathPointStyle.setOnClickListener {
            Pickers.item(
                requireContext(), "", listOf(
                    getString(R.string.path),
                    getString(R.string.cell_signal),
                    getString(R.string.altitude),
                    getString(R.string.time)
                ),
                defaultSelectedIndex = pointColoringStyle.ordinal
            ) {
                if (it != null) {
                    pointColoringStyle =
                        PointColoringStyle.values().find { style -> style.ordinal == it }
                            ?: PointColoringStyle.None
                    cache.putInt("pref_path_waypoint_style", pointColoringStyle.ordinal)
                    updatePointStyleLegend()
                    onPathChanged()
                }
            }
        }

        val lastStyle = cache.getInt("pref_path_waypoint_style")
        pointColoringStyle =
            PointColoringStyle.values().find { style -> style.ordinal == lastStyle }
                ?: PointColoringStyle.None

        updatePointStyleLegend()
        onPathChanged()
    }

    private fun filterCurrentWaypoints(waypoints: List<PathPoint>): List<PathPoint> {
        return waypoints.filterSatisfied(IsValidBacktrackPointSpecification(prefs.navigation.backtrackHistory))
    }

    private fun onPathChanged() {
        if (!isBound || throttle.isThrottled()) {
            return
        }

        drawPathToGPS = IsCurrentPathSpecification(requireContext()).isSatisfiedBy(pathId)

        val distance = geoService.getPathDistance(path.map { it.coordinate })
            .convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        val start = path.lastOrNull()?.time
        val end = path.firstOrNull()?.time

        binding.pathTimes.text = if (start != null && end != null) {
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        } else {
            ""
        }

        binding.pathDuration.text = if (start != null && end != null) {
            formatService.formatDuration(Duration.between(start, end), false)
        } else {
            ""
        }

        binding.pathWaypoints.text = path.size.toString()

        binding.pathDistance.text =
            formatService.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)

        binding.pathImage.path = if (drawPathToGPS) {
            listOf(getGPSWaypoint(pathId)) + path
        } else {
            path
        }
        binding.pathImage.location = gps.location
        binding.pathImage.azimuth = compass.bearing.value

        val factory = getPointFactory()

        val baseStrategy = factory.createColoringStrategy(path)

        val selected = selectedPointId

        binding.pathPointValue.text = if (selected != null) {
            val found = path.find { it.id == selected }
            if (found != null) {
                factory.createValueStrategy(path).getValue(found)
            } else {
                ""
            }
        } else {
            ""
        }

        binding.pathImage.pointColoringStrategy = if (selected == null) {
            baseStrategy
        } else {
            SelectedPointDecorator(
                selected,
                baseStrategy,
                NoDrawPointColoringStrategy()
            )
        }

        binding.pathImage.arePointsHighlighted =
            selected != null || pointColoringStyle != PointColoringStyle.None
    }

    private fun updatePointStyleLegend() {

        val factory = getPointFactory()

        binding.pathPointStyle.text = listOf(
            getString(R.string.path),
            getString(R.string.cell_signal),
            getString(R.string.altitude),
            getString(R.string.time)
        )[pointColoringStyle.ordinal]

        binding.pathLegend.colorScale = factory.createColorScale(path)
        binding.pathLegend.labels = factory.createLabelMap(path)
    }

    private fun getGPSWaypoint(pathId: Long): PathPoint {
        return gps.getPathPoint(pathId)
    }

    private enum class PointColoringStyle {
        None,
        CellSignal,
        Altitude,
        Time
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPathBottomSheetBinding {
        return FragmentPathBottomSheetBinding.inflate(layoutInflater, container, false)
    }

    private fun getDeclination(): Float {
        return if (!prefs.useAutoDeclination) {
            prefs.declinationOverride
        } else {
            geoService.getMagneticDeclination(gps.location, gps.altitude)
        }
    }


    // Waypoints
    private fun drawWaypointListItem(itemBinding: ListItemWaypointBinding, item: PathPoint) {
        val itemStrategy = WaypointListItem(
            requireContext(),
            selectedPointId == item.id,
            formatService,
            prefs,
            { createBeacon(it) },
            { deleteWaypoint(it) },
            { navigateToWaypoint(it) },
            { viewWaypoint(it) }
        )

        itemStrategy.display(itemBinding, item)
    }

    private fun viewWaypoint(point: PathPoint) {
        selectedPointId = if (selectedPointId == point.id) {
            null
        } else {
            point.id
        }
        // TODO: Only redraw the last selected point and new point
        listView.setData(path)
        if (selectedPointId != null) {
            tryOrNothing {
                listView.scrollToPosition(path.indexOf(point), true)
            }
        }
        onPathChanged()
    }

    private fun navigateToWaypoint(point: PathPoint) {
        tryOrNothing {
            runInBackground {
                val waypointTime = point.time ?: Instant.now()
                val date = waypointTime.toZonedDateTime()
                val time = date.toLocalTime()
                var newTempId: Long
                withContext(Dispatchers.IO) {
                    val tempBeaconId =
                        beaconRepo.getTemporaryBeacon(BeaconOwner.Backtrack)?.id ?: 0L
                    val beacon = Beacon(
                        tempBeaconId,
                        getString(
                            R.string.waypoint_beacon_title_template,
                            formatService.formatDate(
                                date,
                                includeWeekDay = false
                            ), formatService.formatTime(time, includeSeconds = false)
                        ),
                        point.coordinate,
                        visible = false,
                        elevation = point.elevation,
                        temporary = true,
                        color = prefs.navigation.backtrackPathColor.color,
                        owner = BeaconOwner.Backtrack
                    )
                    beaconRepo.addBeacon(BeaconEntity.from(beacon))

                    newTempId =
                        beaconRepo.getTemporaryBeacon(BeaconOwner.Backtrack)?.id ?: 0L
                }

                withContext(Dispatchers.Main) {
                    findNavController().navigate(
                        R.id.action_navigation,
                        bundleOf("destination" to newTempId)
                    )
                }
            }
        }
    }

    private fun deleteWaypoint(point: PathPoint) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete_waypoint_prompt),
            getWaypointTitle(point)
        ) { cancelled ->
            if (!cancelled) {
                runInBackground {
                    withContext(Dispatchers.IO) {
                        waypointRepo.deleteWaypoint(WaypointEntity.from(point))
                    }
                    withContext(Dispatchers.Main) {
                        if (selectedPointId == point.id) {
                            selectedPointId = null
                        }
                    }
                }
            }
        }
    }

    private fun createBeacon(waypoint: PathPoint) {
        AppUtils.placeBeacon(
            requireContext(),
            MyNamedCoordinate(waypoint.coordinate, getWaypointTitle(waypoint))
        )
    }

    private fun getWaypointTitle(waypoint: PathPoint): String {
        val waypointTime = waypoint.time ?: Instant.now()
        val date = waypointTime.toZonedDateTime()
        val time = date.toLocalTime()
        return getString(
            R.string.waypoint_beacon_title_template,
            formatService.formatDate(
                date,
                includeWeekDay = false
            ), formatService.formatTime(time, includeSeconds = false)
        )
    }

    private fun getPointFactory(): IPointDisplayFactory {
        return when (pointColoringStyle) {
            PointColoringStyle.None -> PathDisplayFactory(requireContext())
            PointColoringStyle.CellSignal -> CellSignalPointDisplayFactory(requireContext())
            PointColoringStyle.Altitude -> AltitudePointDisplayFactory(requireContext())
            PointColoringStyle.Time -> TimePointDisplayFactory(requireContext())
        }
    }
}