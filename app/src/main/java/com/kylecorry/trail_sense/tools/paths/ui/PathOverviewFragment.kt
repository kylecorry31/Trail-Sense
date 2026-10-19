package com.kylecorry.trail_sense.tools.paths.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.pickers.MenuItem
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.databinding.FragmentPathOverviewBinding
import com.kylecorry.trail_sense.databinding.ListItemWaypointBinding
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.DebugPathElevationsCommand
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.extensions.range
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.navigation.NavControllerAppNavigation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathDirectionRecommender
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.beacon.IPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.beacon.TemporaryPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.factories.IPointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.AddPathPointCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathColorCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathLineStyleCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePointStyleCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.CreateBeaconFromPointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.DeletePointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ExportPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.KeepPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.MoveIPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.NavigateToPointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.RenamePathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ReplacePathElevationsCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.TogglePathVisibilityCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch
import java.time.Duration


class PathOverviewFragment : BoundFragment<FragmentPathOverviewBinding>() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private var pathUpdateVersion by state(0L)

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS(tag = "PathOverviewFragment") }
    private val compass by lazy { sensorService.getCompass() }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            prefs,
            gps
        )
    }
    private val hikingService = HikingService()
    private var isRecording = false
    private val pathService by lazy { PathService.getInstance(requireContext()) }

    private var pointSheet: PathPointsListFragment? = null

    private lateinit var chart: PathElevationChart
    private var path: Path? = null
    private var waypoints: List<PathPoint> = emptyList()
    private var pathId: Long = 0L
    private var selectedPointId: Long? = null
    private var calculatedDuration = Duration.ZERO
    private var elevationGain = Distance.meters(0f)
    private var elevationLoss = Distance.meters(0f)
    private var elevationRange: Range<Distance>? = null
    private var slopes: List<Triple<PathPoint, PathPoint, Float>> = emptyList()
    private var difficulty = HikingDifficulty.Easy

    private val scaleBarLayer = ScaleBarLayer()

    private var lastBounds = CoordinateBounds.empty

    // TODO: Eventually move all of this to the path layer (including waypoint rendering)
    private val layer = ConfigurableGeoJsonLayer()
    private val myLocationLayer =
        GeoJsonLayer(MyLocationGeoJsonSource(), MyLocationGeoJsonSource.SOURCE_ID)
    private val paceFactor = HikingService.DEFAULT_PACE_FACTOR

    private var isFullscreen = false

    private val converter: IPathPointBeaconConverter by lazy {
        TemporaryPathPointBeaconConverter(
            getString(R.string.waypoint)
        )
    }

    private val navigator by lazy { getAppService<Navigator>() }

    private val beaconNavigator: IBeaconNavigator by lazy {
        BeaconNavigator(
            BeaconService(requireContext()),
            NavControllerAppNavigation(findNavController()),
            navigator
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathId = requireArguments().getLong("path_id")
        layer.renderer.configureLineStringRenderer(shouldRenderWithDrawLines = prefs.paths.useFastPathRendering)
        layer.setOnClickListener {
            if (selectedPointId != null) {
                deselectPoint()
                return@setOnClickListener true
            }
            val point = waypoints.firstOrNull { point -> point.id == it.id }
            if (point != null) {
                viewWaypoint(point)
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Populate the last known location
        updateUserLocation()

        binding.pathImage.start()
        Tools.subscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
            this::onBacktrackChanged
        )
    }

    override fun onPause() {
        super.onPause()
        binding.pathImage.stop()
        Tools.unsubscribe(
            PathsToolRegistration.BROADCAST_BACKTRACK_STATE_CHANGED,
            this::onBacktrackChanged
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = PathElevationChart(binding.chart)

        binding.pathImage.isInteractive = true

        binding.pathImage.setOnTouchListener { v, event ->
            binding.pathScroll.isScrollable = event.action == MotionEvent.ACTION_UP
            false
        }

        binding.pathMapFullscreenToggle.setOnClickListener {
            isFullscreen = !isFullscreen
            binding.pathMapHolder.layoutParams = if (isFullscreen) {
                val legendHeight = Resources.dp(requireContext(), 72f).toInt()
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    binding.pathScroll.height - legendHeight
                )
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Resources.dp(requireContext(), 250f).toInt()
                ).also {
                    it.marginStart = Resources.dp(requireContext(), 16f).toInt()
                    it.marginEnd = Resources.dp(requireContext(), 16f).toInt()
                }
            }
            binding.pathMapFullscreenToggle.setImageResource(if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_recenter)
            val timer = CoroutineTimer {
                if (isBound) {
                    binding.pathScroll.scrollTo(0, binding.pathMapHolder.top)
                    binding.pathImage.fitIntoView(lastBounds)
                }
            }
            timer.once(Duration.ofMillis(30))
        }

        binding.addPointBtn.setOnClickListener {
            inBackground {
                var wasSuccessful = false
                val job = launch {
                    AddPathPointCommand(requireContext(), pathId).execute()
                    wasSuccessful = true
                }

                Alerts.withCancelableLoading(
                    requireContext(),
                    getString(R.string.loading),
                    onCancel = { job.cancel() }) {
                    job.join()
                    if (wasSuccessful) {
                        toast(getString(R.string.point_added))
                    }
                }
            }
        }

        binding.stopRecordingBtn.setOnClickListener {
            stopRecording()
        }

        binding.toEndBtn.setOnClickListener {
            followPath(PathFollow.Direction(PathDirection.ToEnd))
        }

        binding.toStartBtn.setOnClickListener {
            followPath(PathFollow.Direction(PathDirection.ToStart))
        }

        binding.pathTitle.rightButton.setOnClickListener {
            showPathMenu()
        }

        binding.pathTitle.subtitle.setOnClickListener {
            movePath()
        }

        chart.setOnPointClickListener {
            viewWaypoint(it)
        }

        binding.pathElevationMin.setOnClickListener {
            val point = waypoints.filter { it.elevation != null }.minByOrNull { it.elevation!! }
            point?.let { viewWaypoint(it) }
        }

        binding.pathElevationMax.setOnClickListener {
            val point = waypoints.filter { it.elevation != null }.maxByOrNull { it.elevation!! }
            point?.let { viewWaypoint(it) }
        }

        observe(pathService.getLivePath(pathId)) {
            path = it
            binding.root.post {
                updateParent()
                updateElevationPlot()
                updatePointStyleLegend()
                updatePathMap()
                onPathChanged()
            }
        }

        observe(pathService.getWaypointsLive(pathId)) {
            onWaypointsChanged(it)
        }

        observe(gps) {
            updateDeclination()
            updateFollowEmphasis()
            updateUserLocation()
        }

        observe(compass) {
            binding.pathImage.userAzimuth = compass.bearing
        }

        binding.pathImage.setLayers(
            listOf(layer, myLocationLayer, scaleBarLayer)
        )

        binding.pathLineStyle.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePathLineStyleCommand(requireContext(), this)
            command.execute(path)
        }

        binding.pathColor.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePathColorCommand(requireContext(), this)
            command.execute(path)
        }

        binding.pathPointStyle.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePointStyleCommand(requireContext(), this)
            command.execute(path)
        }
    }

    private fun updateUserLocation() {
        binding.pathImage.userLocation = gps.location
        binding.pathImage.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }
    }

    private fun onWaypointsChanged(waypoints: List<PathPoint>) {
        inBackground {
            onDefault {
                this@PathOverviewFragment.waypoints =
                    hikingService.correctElevations(
                        waypoints.sortedBy { it.id },
                        prefs.paths.elevationSmoothing
                    ).reversed()
            }
            isRecording = isRecordingPath()
            onIO {
                DebugPathElevationsCommand(
                    requireContext(),
                    waypoints,
                    this@PathOverviewFragment.waypoints
                ).execute()
            }
            onMain {
                val selected = selectedPointId
                if (selected != null && waypoints.none { it.id == selected }) {
                    deselectPoint()
                }
                pointSheet?.setPoints(this@PathOverviewFragment.waypoints)
                updateRecordingBanner()
                updateFollowEmphasis()
                updateElevationOverview()
                updateHikingStats()
                updatePathMap()
                updatePointStyleLegend()
                onPathChanged()
            }
        }
    }

    private fun updateParent() {
        val path = path ?: return
        inBackground {
            val parent = onIO { pathService.getGroup(path.parentId) }
            if (isBound) {
                binding.pathTitle.subtitle.text = parent?.name ?: getString(R.string.no_group)
            }
        }
    }

    private fun updateElevationPlot() {
        chart.plot(
            waypoints.reversed(),
            path?.style?.color ?: prefs.paths.defaultPathColor.color
        )
    }

    private suspend fun updateHikingStats() = onDefault {
        val reversed = waypoints.reversed()
        difficulty = hikingService.getHikingDifficulty(reversed)
        calculatedDuration =
            hikingService.getHikingDuration(reversed, paceFactor)
    }

    private fun movePath() {
        val path = path ?: return
        val command = MoveIPathCommand(requireContext(), pathService)
        inBackground {
            command.execute(path)
        }
    }

    private suspend fun updateElevationOverview() {
        onDefault {
            val path = waypoints.reversed()

            val (loss, gain) = hikingService.getElevationLossGain(path)

            val units = prefs.baseDistanceUnits
            elevationGain = gain.convertTo(units)
            elevationLoss = loss.convertTo(units)
            elevationRange =
                path.mapNotNull { it.elevation?.let { Distance.meters(it).convertTo(units) } }
                    .range()
            slopes = hikingService.getSlopes(path)
            slopes.forEach {
                it.first.slope = it.third
            }
            val first = slopes.lastOrNull()
            first?.first?.slope = first.third
        }
        onMain {
            updateElevationPlot()
        }
    }

    private fun showPathMenu() {
        val path = path ?: return
        val menuItems = listOfNotNull(
            MenuItem(getString(R.string.rename)) {
                renamePath(path)
                true
            },
            if (path.temporary) {
                MenuItem(getString(R.string.keep_forever)) {
                    keepPath(path)
                    true
                }
            } else {
                null
            },
            MenuItem(getString(if (path.style.visible) R.string.hide else R.string.show)) {
                togglePathVisibility(path)
                true
            },
            MenuItem(getString(R.string.export)) {
                exportPath(path)
                true
            },
            MenuItem(getString(R.string.simplify)) {
                simplifyPath(path)
                true
            },
            MenuItem(getString(R.string.replace_elevations)) {
                replaceElevations(path)
                true
            },
            MenuItem(getString(R.string.points)) {
                viewPoints()
                true
            }
        )

        Pickers.menu(binding.pathTitle.rightButton, menuItems)
    }

    private fun simplifyPath(path: Path) {
        val command = SimplifyPathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun replaceElevations(path: Path) {
        val command = ReplacePathElevationsCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun exportPath(path: Path) {
        val command = ExportPathCommand(
            requireContext(),
            this,
            IOFactory().createGpxService(this),
            pathService
        )
        command.execute(path)
    }

    private fun togglePathVisibility(path: Path) {
        val command = TogglePathVisibilityCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun updatePathMap() {
        if (!isBound) {
            return
        }
        lastBounds = Geography.getBounds(waypoints.map { it.coordinate })
        binding.pathImage.fitIntoView(lastBounds)
        updateMapLayer()
    }

    private fun updateRecordingBanner() {
        if (!isBound) {
            return
        }
        binding.pathRecordingBanner.isVisible = isRecording
    }

    private suspend fun isRecordingPath(): Boolean = onIO {
        pathService.getBacktrackPathId() == pathId && BacktrackScheduler.isOn(requireContext())
    }

    @Suppress("UnusedParameter")
    private suspend fun onBacktrackChanged(data: Bundle) = refreshRecording()

    private suspend fun refreshRecording() {
        isRecording = isRecordingPath()
        onMain {
            updateRecordingBanner()
            updateFollowEmphasis()
        }
    }

    private fun stopRecording() {
        inBackground {
            onIO {
                Tools.getService(requireContext(), PathsToolRegistration.SERVICE_BACKTRACK)
                    ?.disable()
            }
            refreshRecording()
        }
    }

    private val canFollow: Boolean
        get() = (path?.metadata?.distance?.value ?: 0f) > 0f

    private fun updateFollowEmphasis() {
        if (!isBound) {
            return
        }
        // The waypoints are ordered from newest to oldest
        val start = waypoints.lastOrNull()?.coordinate
        val end = waypoints.firstOrNull()?.coordinate
        // The recording path's end is the user's location, so a GPS reading isn't needed
        val hasLocation = isRecording || gps.hasValidReading
        val recommended = if (start != null && end != null && hasLocation) {
            PathDirectionRecommender.recommend(start, end, gps.location, isRecording)
        } else {
            null
        }
        emphasize(binding.toStartBtn, recommended == PathDirection.ToStart)
        emphasize(binding.toEndBtn, recommended == PathDirection.ToEnd)
    }

    private fun emphasize(button: MaterialButton, emphasized: Boolean) {
        val (background, foreground) = if (emphasized) {
            androidx.appcompat.R.attr.colorPrimary to com.google.android.material.R.attr.colorOnPrimary
        } else {
            com.google.android.material.R.attr.colorSecondaryContainer to
                    com.google.android.material.R.attr.colorOnSecondaryContainer
        }
        button.backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(button, background))
        button.setTextColor(MaterialColors.getColor(button, foreground))
    }

    private fun onPathChanged() {
        pathUpdateVersion++
    }

    override fun onUpdate() {
        super.onUpdate()
        if (!isBound) {
            return
        }
        effect("path", pathUpdateVersion, resetOnResume) {
            updatePathDetails()
        }
    }

    private fun updatePathDetails() {
        val path = path ?: return

        binding.followOptions.isVisible = canFollow

        binding.pathLineStyle.text = getLineStyleNames(requireContext()).getValue(path.style.line)

        val distance =
            path.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        binding.pathTitle.title.text = PathNameFactory(requireContext()).getName(path)

        val duration = path.metadata.duration
            ?.let { Duration.between(it.start, it.end) }
            ?.takeIf { it > Duration.ofMinutes(1) }
            ?: calculatedDuration

        binding.pathDuration.title = formatService.formatDuration(duration, false)
        binding.pathWaypoints.title = path.metadata.waypoints.toString()

        // Elevations
        binding.pathElevationGain.title = formatService.formatDistance(
            elevationGain,
            Units.getDecimalPlaces(elevationGain.units),
            false
        )
        binding.pathElevationLoss.title = formatService.formatDistance(
            elevationLoss,
            Units.getDecimalPlaces(elevationLoss.units),
            false
        )

        val elevationRange = elevationRange
        binding.pathElevationMin.isVisible = elevationRange != null
        binding.pathElevationMax.isVisible = elevationRange != null

        elevationRange?.let {
            binding.pathElevationMin.title = formatService.formatDistance(
                it.start,
                Units.getDecimalPlaces(it.start.units),
                false
            )

            binding.pathElevationMax.title = formatService.formatDistance(
                it.end,
                Units.getDecimalPlaces(it.end.units),
                false
            )
        }

        binding.pathDifficulty.title = formatService.formatHikingDifficulty(difficulty)

        binding.pathDistance.title =
            formatService.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )

        Colors.setImageColor(binding.pathColor, path.style.color)

        updateMapLayer()

        updateDebugStats()
    }

    private fun updateMapLayer() {
        val path = path ?: return
        val waypointFeatures = waypoints.asBeacons(
            requireContext(),
            path.style.point,
            selectedPointId
        ).map {
            GeoJsonFeature.point(
                it.coordinate,
                it.id,
                it.name,
                color = it.color,
                icon = it.icon?.id,
                iconColor = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color),
                size = 8f,
                iconSize = 8f * 0.75f,
                isClickable = true,
                strokeColor = Color.TRANSPARENT
            )
        }
        val endpointFeatures = listOfNotNull(
            waypoints.minByOrNull { it.id }?.let {
                GeoJsonFeature.point(
                    it.coordinate,
                    id = "path-start-${path.id}",
                    color = path.style.color,
                    strokeColor = Color.WHITE,
                    strokeWeight = 1f,
                    size = 8f
                )
            },
            waypoints.maxByOrNull { it.id }?.let {
                GeoJsonFeature.point(
                    it.coordinate,
                    id = "path-end-${path.id}",
                    color = Color.WHITE,
                    icon = BeaconIcon.Flag.id,
                    iconColor = path.style.color,
                    strokeColor = path.style.color,
                    strokeWeight = 1f,
                    size = 12f,
                    iconSize = 8f
                )
            }
        )

        val pathFeature = GeoJsonFeature.lineString(
            waypoints.map { it.coordinate },
            path.id,
            path.name,
            lineStyle = path.style.line,
            color = path.style.color
        )

        layer.setData(GeoJsonFeatureCollection(endpointFeatures + waypointFeatures + pathFeature))
    }

    private fun updateDeclination() {
        inBackground {
            onIO {
                compass.declination = declinationProvider.getDeclination()
            }
        }
    }

    private fun deselectPoint() {
        selectedPointId = null
        binding.pathSelectedPoint.isVisible = false
        chart.removeHighlight()
        updateMapLayer()
    }

    private fun updatePointStyleLegend() {

        val path = path ?: return

        val factory = getPointFactory()

        binding.pathPointStyle.text = getPointStyleNames(requireContext()).getValue(path.style.point)

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

    // Waypoints
    private fun drawWaypointListItem(itemBinding: ListItemWaypointBinding, item: PathPoint) {
        val itemStrategy = WaypointListItem(
            requireContext(),
            formatService,
            { createBeacon(it) },
            { deleteWaypoint(it) },
            { navigateToWaypoint(it) },
            { navigateDirectlyToWaypoint(it) },
            { /* Do nothing */ },
            canFollow
        )

        itemStrategy.display(itemBinding, item)
    }

    private fun viewPoints() {
        binding.pathScroll.scrollTo(0, 0)
        val sheet = PathPointsListFragment()
        pointSheet = sheet
        sheet.canFollow = canFollow
        sheet.show(this)
        sheet.onCreateBeaconListener = { createBeacon(it) }
        sheet.onDeletePointListener = { deleteWaypoint(it) }
        sheet.onNavigateToPointListener = { navigateToWaypoint(it) }
        sheet.onNavigateDirectlyToPointListener = { navigateDirectlyToWaypoint(it) }
        sheet.onViewPointListener = { viewWaypoint(it) }
        sheet.setPoints(waypoints)
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
            chart.highlight(point)
            val binding =
                ListItemWaypointBinding.inflate(layoutInflater, binding.pathSelectedPoint, true)
            drawWaypointListItem(binding, point)
        } else {
            deselectPoint()
        }

        onPathChanged()
    }

    private fun navigateToWaypoint(point: PathPoint) {
        followPath(PathFollow.ToPoint(point.id))
    }

    private fun followPath(follow: PathFollow) {
        val selectedPath = path ?: return
        inBackground {
            navigator.navigateAlongPath(selectedPath, follow)
            onMain {
                findNavController().navigate(
                    R.id.action_navigation,
                    Bundle().apply {
                        putBoolean(NavigatorFragment.ARG_NAVIGATION_STARTED, true)
                    }
                )
            }
        }
    }

    private fun navigateDirectlyToWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = NavigateToPointCommand(
            this,
            converter,
            beaconNavigator
        )
        tryOrNothing {
            command.execute(path, point)
        }
    }

    private fun deleteWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = DeletePointCommand(requireContext(), this)
        command.execute(path, point)
    }

    private fun createBeacon(point: PathPoint) {
        val path = path ?: return
        val command = CreateBeaconFromPointCommand(requireContext())
        command.execute(path, point)
    }

    private fun getPointFactory(): IPointDisplayFactory {
        return getPointFactory(requireContext(), path?.style?.point ?: PathPointColoringStyle.None)
    }

    private fun updateDebugStats() {
        if (!isDebug()) {
            return
        }

        binding.pathsTiming.isVisible = true

        inBackground {
            onDefault {
                val readings = waypoints
                    .filter { it.time != null }
                    .sortedBy { it.time }
                    .zipWithNext { a, b -> Duration.between(a.time, b.time).seconds / 60f }

                if (readings.isEmpty()) {
                    return@onDefault
                }

                val mean = Statistics.mean(readings).roundPlaces(2)
                val stdev = Statistics.stdev(readings, mean = mean).roundPlaces(2)
                val max = readings.maxOrNull()?.roundPlaces(2) ?: 0f
                val median = Statistics.median(readings).roundPlaces(2)
                val quantile75 = Statistics.quantile(readings, 0.75f).roundPlaces(2)
                val quantile90 = Statistics.quantile(readings, 0.9f).roundPlaces(2)
                onMain {
                    @Suppress("SetTextI18n")
                    binding.pathsTiming.text = "Mean: $mean\n" +
                            "Stdev: $stdev\n" +
                            "Max: $max\n" +
                            "Median: $median\n" +
                            "75th: $quantile75\n" +
                            "90th: $quantile90"
                }
            }
        }
    }
}
