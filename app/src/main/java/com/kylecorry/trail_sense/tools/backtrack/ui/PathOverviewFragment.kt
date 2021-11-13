package com.kylecorry.trail_sense.tools.backtrack.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPathOverviewBinding
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.beacons.BeaconOwner
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.hiking.HikingDifficulty
import com.kylecorry.trail_sense.shared.hiking.HikingService
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.paths.PathPointColoringStyle
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.backtrack.domain.factories.*
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.DefaultPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.SelectedPointDecorator
import com.kylecorry.trail_sense.tools.backtrack.ui.commands.ChangePathColorCommand
import com.kylecorry.trail_sense.tools.backtrack.ui.commands.ChangePathLineStyleCommand
import com.kylecorry.trail_sense.tools.backtrack.ui.commands.ChangePointStyleCommand
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
    private val geologyService = GeologyService()
    private val hikingService = HikingService()
    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }

    private lateinit var chart: PathElevationChart
    private var path: Path? = null
    private var waypoints: List<PathPoint> = emptyList()
    private var pathId: Long = 0L
    private var selectedPointId: Long? = null
    private var calculatedDuration = Duration.ZERO
    private var elevationGain = Distance.meters(0f)
    private var elevationLoss = Distance.meters(0f)
    private var difficulty = HikingDifficulty.Easiest

    private val gainThreshold = Distance.meters(2.75f)
    private val paceFactor = 1.75f

    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathId = requireArguments().getLong("path_id")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart = PathElevationChart(binding.chart)

        binding.pathMapFullscreenToggle.setOnClickListener {
            isFullscreen = !isFullscreen
            binding.pathMapHolder.layoutParams = if (isFullscreen) {
                val legendHeight = Resources.dp(requireContext(), 72f).toInt()
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    binding.root.height - legendHeight
                )
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Resources.dp(requireContext(), 250f).toInt()
                )
            }
            binding.pathMapFullscreenToggle.setImageResource(if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_recenter)
            val timer = Timer {
                if (isBound) {
                    binding.root.scrollTo(0, binding.pathMapHolder.top)
                }
            }
            timer.once(Duration.ofMillis(30))
        }

        binding.pathImage.setOnPointClickListener {
            viewWaypoint(it)
        }

        binding.pathViewPointsBtn.setOnClickListener {
            viewPoints()
        }

        chart.setOnPointClickListener {
            viewWaypoint(it)
        }

        pathService.getLivePath(pathId).observe(viewLifecycleOwner, {
            path = it

            updateElevationPlot()
            updatePointStyleLegend()
            updatePathMap()
            onPathChanged()
        })

        pathService.getWaypointsLive(pathId).observe(viewLifecycleOwner, {
            waypoints = it.sortedByDescending { p -> p.id }
            val selected = selectedPointId
            if (selected != null && waypoints.find { it.id == selected } == null) {
                deselectPoint()
            }

            updateElevationPlot()
            updateHikingStats()
            updateElevationOverview()
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
            val command = ChangePathLineStyleCommand(requireContext(), lifecycleScope, path)
            command.execute()
        }

        binding.pathColor.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePathColorCommand(requireContext(), lifecycleScope, path)
            command.execute()
        }

        binding.pathPointStyle.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePointStyleCommand(requireContext(), lifecycleScope, path)
            command.execute()
        }
    }

    private fun updateElevationPlot(){
        chart.plot(waypoints.reversed(), path?.style?.color ?: prefs.navigation.defaultPathColor.color)
    }

    private fun updateHikingStats() {
        runInBackground {
            val reversed = waypoints.reversed()
            calculatedDuration =
                hikingService.getHikingDuration(reversed, gainThreshold, paceFactor)
            difficulty = hikingService.getHikingDifficulty(reversed, gainThreshold)
        }
    }

    private fun updateElevationOverview() {
        runInBackground {
            val path = waypoints.reversed()

            val elevations =
                path.mapNotNull { if (it.elevation == null) null else Distance.meters(it.elevation) }

            elevationGain =
                geologyService.getElevationGain(elevations, gainThreshold)
                    .convertTo(prefs.baseDistanceUnits)
            elevationLoss =
                geologyService.getElevationLoss(elevations, gainThreshold)
                    .convertTo(prefs.baseDistanceUnits)
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

        val duration = if (start != null && end != null && Duration.between(
                start,
                end
            ) > Duration.ofMinutes(1)
        ) {
            Duration.between(start, end)
        } else {
            calculatedDuration
        }

        binding.pathDuration.text = formatService.formatDuration(duration, false)
        binding.pathWaypoints.text = path.metadata.waypoints.toString()

        // Elevations
        binding.pathElevationGain.text = formatService.formatDistance(
            elevationGain,
            Units.getDecimalPlaces(elevationGain.units),
            false
        )
        binding.pathElevationLoss.text = formatService.formatDistance(
            elevationLoss,
            Units.getDecimalPlaces(elevationLoss.units),
            false
        )

        binding.pathDifficulty.text = formatService.formatHikingDifficulty(difficulty)

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
                if (path.style.point == PathPointColoringStyle.None) {
                    DefaultPointColoringStrategy(path.style.color)
                } else {
                    baseStrategy
                },
                NoDrawPointColoringStrategy()
            )
        }
    }

    private fun deselectPoint() {
        selectedPointId = null
        binding.pathSelectedPoint.isVisible = false
    }

    private fun updatePointStyleLegend() {

        val path = path ?: return

        val factory = getPointFactory()

        binding.pathPointStyle.text = listOf(
            getString(R.string.none),
            getString(R.string.cell_signal),
            getString(R.string.elevation),
            getString(R.string.time)
        )[path.style.point.ordinal]

        binding.pathLegend.colorScale = factory.createColorScale(waypoints)
        binding.pathLegend.labels = factory.createLabelMap(waypoints)
        binding.pathLegend.isVisible = path.style.point != PathPointColoringStyle.None
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
            false,
            formatService,
            prefs,
            { createBeacon(it) },
            { deleteWaypoint(it) },
            { navigateToWaypoint(it) },
            { /* Do nothing */ }
        )

        itemStrategy.display(itemBinding, item)
    }

    private fun viewPoints(){
        toast("TODO: Show the points")
    }

    private fun viewWaypoint(point: PathPoint) {
        selectedPointId = if (selectedPointId == point.id) {
            null
        } else {
            point.id
        }

        binding.pathSelectedPoint.removeAllViews()

        if (selectedPointId != null) {
            binding.pathSelectedPoint.isVisible = true
            val binding =
                ListItemWaypointBinding.inflate(layoutInflater, binding.pathSelectedPoint, true)
            drawWaypointListItem(binding, point)
        } else {
            deselectPoint()
        }

        onPathChanged()
    }

    private fun navigateToWaypoint(point: PathPoint) {
        val path = path ?: return
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
                        color = path.style.color,
                        owner = BeaconOwner.Backtrack
                    )
                    newTempId = beaconRepo.addBeacon(BeaconEntity.from(beacon))
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
                            deselectPoint()
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
        return when (path?.style?.point) {
            PathPointColoringStyle.CellSignal -> CellSignalPointDisplayFactory(requireContext())
            PathPointColoringStyle.Altitude -> AltitudePointDisplayFactory(requireContext())
            PathPointColoringStyle.Time -> TimePointDisplayFactory(requireContext())
            else -> NonePointDisplayFactory(requireContext())
        }
    }
}