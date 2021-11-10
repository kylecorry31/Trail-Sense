package com.kylecorry.trail_sense.tools.backtrack.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPathOverviewBinding
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.beacons.BeaconOwner
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.paths.LineStyle
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.paths.PathPointColoringStyle
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.backtrack.domain.factories.*
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.SelectedPointDecorator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class PathOverviewFragment : BoundFragment<FragmentPathOverviewBinding>() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val compass by lazy { sensorService.getCompass() }
    private val navigationService = NavigationService()
    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }

    private lateinit var chart: PathElevationChart
    private var path: Path? = null
    private var waypoints: List<PathPoint> = emptyList()
    private var pathId: Long = 0L
    private var selectedPointId: Long? = null
    private var calculatedDuration = Duration.ZERO

    private var pointColoringStyle = PathPointColoringStyle.None

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathId = requireArguments().getLong("path_id")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart = PathElevationChart(binding.chart)

        binding.pathImage.setOnPointClickListener {
            viewWaypoint(it)
        }

        pathService.getLivePath(pathId).observe(viewLifecycleOwner, {
            path = it
            pointColoringStyle = it?.style?.point ?: PathPointColoringStyle.None
            updatePointStyleLegend()
            updatePathMap()
            onPathChanged()
        })
        pathService.getWaypointsLive(pathId).observe(viewLifecycleOwner, {
            waypoints = it.sortedByDescending { p -> p.id }
            calculatedDuration = navigationService.pathDuration(waypoints, 1.78816f)
            val selected = selectedPointId
            if (selected != null && waypoints.find { it.id == selected } == null) {
                selectedPointId = null
            }
            chart.plot(waypoints.reversed())
            updatePathMap()
            updatePointStyleLegend()
            onPathChanged()
        })

        gps.asLiveData().observe(viewLifecycleOwner, {
            compass.declination = getDeclination()
            onPathChanged()
        })

        compass.asLiveData().observe(viewLifecycleOwner, {
            onPathChanged()
        })

        binding.pathLineStyle.setOnClickListener {
            val path = path ?: return@setOnClickListener
            Pickers.item(
                requireContext(), getString(R.string.line_style), listOf(
                    getString(R.string.solid),
                    getString(R.string.dotted),
                    getString(R.string.arrow)
                ),
                defaultSelectedIndex = path.style.line.ordinal
            ) {
                if (it != null) {
                    val line =
                        LineStyle.values().find { style -> style.ordinal == it } ?: LineStyle.Dotted
                    runInBackground {
                        withContext(Dispatchers.IO) {
                            pathService.addPath(path.copy(style = path.style.copy(line = line)))
                        }
                        withContext(Dispatchers.Main) {
                            updatePathMap()
                            onPathChanged()
                        }
                    }
                }
            }
        }

        binding.pathColor.setOnClickListener {
            val path = path ?: return@setOnClickListener
            CustomUiUtils.pickColor(
                requireContext(),
                AppColor.values().firstOrNull { it.color == path.style.color } ?: AppColor.Gray,
                getString(R.string.path_color)
            ) {
                if (it != null) {
                    runInBackground {
                        withContext(Dispatchers.IO) {
                            pathService.addPath(path.copy(style = path.style.copy(color = it.color)))
                        }
                        withContext(Dispatchers.Main) {
                            updatePathMap()
                            onPathChanged()
                        }
                    }
                }
            }
        }

        binding.pathPointStyle.setOnClickListener {
            Pickers.item(
                requireContext(), getString(R.string.point_style), listOf(
                    getString(R.string.none),
                    getString(R.string.cell_signal),
                    getString(R.string.elevation),
                    getString(R.string.time)
                ),
                defaultSelectedIndex = pointColoringStyle.ordinal
            ) {
                if (it != null) {
                    pointColoringStyle =
                        PathPointColoringStyle.values().find { style -> style.ordinal == it }
                            ?: PathPointColoringStyle.None
                    runInBackground {
                        withContext(Dispatchers.IO) {
                            val path = path ?: return@withContext
                            pathService.addPath(path.copy(style = path.style.copy(point = pointColoringStyle)))
                        }
                        withContext(Dispatchers.Main) {
                            updatePointStyleLegend()
                            onPathChanged()
                        }
                    }
                }
            }
        }
    }

    private fun updatePathMap() {
        val path = path ?: return
        if (!isBound) {
            return
        }
        binding.pathImage.pathColor = path.style.color
        binding.pathImage.pathStyle = path.style.line
        binding.pathImage.path = waypoints
    }

    private fun onPathChanged() {
        val path = path ?: return

        if (!isBound || throttle.isThrottled()) {
            return
        }

        binding.pathLineStyle.text = listOf(
            getString(R.string.solid),
            getString(R.string.dotted),
            getString(R.string.arrow)
        )[path.style.line.ordinal]

        val distance =
            path.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        val start = path.metadata.duration?.start
        val end = path.metadata.duration?.end


        binding.pathTimes.text = if (!path.name.isNullOrBlank()) {
            path.name
        } else if (start != null && end != null) {
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        } else {
            getString(android.R.string.untitled)
        }

        val duration = if (start != null && end != null && Duration.between(start, end) > Duration.ofMinutes(1)) {
            Duration.between(start, end)
        } else {
            calculatedDuration
        }

        binding.pathDuration.text = formatService.formatDuration(duration, false)
        binding.pathWaypoints.text = path.metadata.waypoints.toString()

        binding.pathDistance.text =
            formatService.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )

        CustomUiUtils.setImageColor(binding.pathColor, path.style.color)

        binding.pathImage.location = gps.location
        binding.pathImage.azimuth = compass.bearing.value

        val factory = getPointFactory()

        val baseStrategy = factory.createColoringStrategy(waypoints)

        val selected = selectedPointId

        binding.pathImage.pointColoringStrategy = if (selected == null) {
            baseStrategy
        } else {
            SelectedPointDecorator(
                selected,
                baseStrategy,
                NoDrawPointColoringStrategy()
            )
        }
    }

    private fun updatePointStyleLegend() {

        val factory = getPointFactory()

        binding.pathPointStyle.text = listOf(
            getString(R.string.none),
            getString(R.string.cell_signal),
            getString(R.string.elevation),
            getString(R.string.time)
        )[pointColoringStyle.ordinal]

        binding.pathLegend.colorScale = factory.createColorScale(waypoints)
        binding.pathLegend.labels = factory.createLabelMap(waypoints)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPathOverviewBinding {
        return FragmentPathOverviewBinding.inflate(layoutInflater, container, false)
    }

    private fun getDeclination(): Float {
        return declination.getDeclination()
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
                        color = prefs.navigation.defaultPathColor.color,
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
                        pathService.deleteWaypoint(point)
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
            PathPointColoringStyle.None -> NonePointDisplayFactory(requireContext())
            PathPointColoringStyle.CellSignal -> CellSignalPointDisplayFactory(requireContext())
            PathPointColoringStyle.Altitude -> AltitudePointDisplayFactory(requireContext())
            PathPointColoringStyle.Time -> TimePointDisplayFactory(requireContext())
        }
    }
}