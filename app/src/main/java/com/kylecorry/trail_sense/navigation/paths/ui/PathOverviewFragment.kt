package com.kylecorry.trail_sense.navigation.paths.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPathOverviewBinding
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.navigation.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.navigation.domain.hiking.HikingService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.navigation.paths.domain.factories.*
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.DefaultPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.SelectedPointDecorator
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.ui.commands.*
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration


class PathOverviewFragment : BoundFragment<FragmentPathOverviewBinding>() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val throttle = Throttle(20)

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(false) }
    private val compass by lazy { sensorService.getCompass() }
    private val hikingService = HikingService()
    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }

    private var pointSheet: PathPointsListFragment? = null

    private lateinit var chart: PathElevationChart
    private var path: Path? = null
    private var waypoints: List<PathPoint> = emptyList()
    private var pathId: Long = 0L
    private var selectedPointId: Long? = null
    private var calculatedDuration = Duration.ZERO
    private var elevationGain = Distance.meters(0f)
    private var elevationLoss = Distance.meters(0f)
    private var difficulty = HikingDifficulty.Easiest

    private val paceFactor = 1.75f

    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathId = requireArguments().getLong("path_id")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart = PathElevationChart(binding.chart)

        binding.pathImage.setOnTouchListener { v, event ->
            binding.root.isScrollable = event.action == MotionEvent.ACTION_UP
            false
        }

        binding.pathMapFullscreenToggle.setOnClickListener {
            isFullscreen = !isFullscreen
            binding.pathImage.recenter()
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

        binding.menuButton.setOnClickListener {
            showPathMenu()
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

            pointSheet?.setPoints(waypoints)
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
            val command = ChangePathLineStyleCommand(requireContext(), lifecycleScope)
            command.execute(path)
        }

        binding.pathColor.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePathColorCommand(requireContext(), lifecycleScope)
            command.execute(path)
        }

        binding.pathPointStyle.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePointStyleCommand(requireContext(), lifecycleScope)
            command.execute(path)
        }
    }

    private fun updateElevationPlot() {
        chart.plot(
            waypoints.reversed(),
            path?.style?.color ?: prefs.navigation.defaultPathColor.color
        )
    }

    private fun updateHikingStats() {
        runInBackground {
            val reversed = waypoints.reversed()
            calculatedDuration =
                hikingService.getHikingDuration(reversed, paceFactor)
            difficulty = hikingService.getHikingDifficulty(reversed)
        }
    }

    private fun updateElevationOverview() {
        runInBackground {
            val path = waypoints.reversed()

            val gainLoss = hikingService.getElevationLossGain(path)

            elevationGain = gainLoss.second.convertTo(prefs.baseDistanceUnits)
            elevationLoss = gainLoss.first.convertTo(prefs.baseDistanceUnits)
        }
    }

    private fun showPathMenu() {
        val path = path ?: return
        val actions = listOf(
            PathAction.Rename,
            PathAction.Keep,
            PathAction.ToggleVisibility,
            PathAction.Export,
            PathAction.Simplify
        )

        Pickers.menu(
            binding.menuButton, listOf(
                getString(R.string.rename),
                if (path.temporary) getString(R.string.keep_forever) else null,
                if (prefs.navigation.useRadarCompass || prefs.navigation.areMapsEnabled) {
                    if (path.style.visible) getString(R.string.hide) else getString(
                        R.string.show
                    )
                } else null,
                getString(R.string.export),
                getString(R.string.simplify)
            )
        ) {
            when (actions[it]) {
                PathAction.Export -> exportPath(path)
                PathAction.Rename -> renamePath(path)
                PathAction.Keep -> keepPath(path)
                PathAction.ToggleVisibility -> togglePathVisibility(path)
                PathAction.Simplify -> simplifyPath(path)
            }
            true
        }
    }

    private fun simplifyPath(path: Path) {
        val command = SimplifyPathCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
    }

    private fun exportPath(path: Path) {
        val command = ExportPathCommand(
            requireContext(),
            lifecycleScope,
            IOFactory().createGpxService(this),
            pathService
        )
        command.execute(path)
    }

    private fun togglePathVisibility(path: Path) {
        val command = TogglePathVisibilityCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(requireContext(), lifecycleScope, pathService)
        command.execute(path)
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
            getString(R.string.arrow),
            getString(R.string.dashed),
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
                DefaultPointColoringStrategy(Color.WHITE),
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
            formatService,
            { createBeacon(it) },
            { deleteWaypoint(it) },
            { navigateToWaypoint(it) },
            { /* Do nothing */ }
        )

        itemStrategy.display(itemBinding, item)
    }

    private fun viewPoints() {
        binding.root.scrollTo(0, 0)
        pointSheet = PathPointsListFragment()
        pointSheet?.show(this)
        pointSheet?.onCreateBeaconListener = { createBeacon(it) }
        pointSheet?.onDeletePointListener = { deleteWaypoint(it) }
        pointSheet?.onNavigateToPointListener = { navigateToWaypoint(it) }
        pointSheet?.onViewPointListener = { viewWaypoint(it) }
        pointSheet?.setPoints(waypoints)
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
        val command = NavigateToPointCommand(
            requireContext(),
            lifecycleScope,
            findNavController()
        )
        tryOrNothing {
            command.execute(path, point)
        }
    }

    private fun deleteWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = DeletePointCommand(requireContext(), lifecycleScope)
        command.execute(path, point)
    }

    private fun createBeacon(point: PathPoint) {
        val path = path ?: return
        val command = CreateBeaconFromPointCommand(requireContext())
        command.execute(path, point)
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