package com.kylecorry.trail_sense.tools.map.ui

import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.luna.hooks.Ref
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.annotateWithLinks
import com.kylecorry.trail_sense.shared.extensions.compose.useActivity
import com.kylecorry.trail_sense.shared.extensions.compose.useAndroidContext
import com.kylecorry.trail_sense.shared.extensions.compose.useCallback
import com.kylecorry.trail_sense.shared.extensions.compose.useCoordinatePreference
import com.kylecorry.trail_sense.shared.extensions.compose.useDestroyEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useEffectWithCleanup
import com.kylecorry.trail_sense.shared.extensions.compose.useFloatPreference
import com.kylecorry.trail_sense.shared.extensions.compose.useFlow
import com.kylecorry.trail_sense.shared.extensions.compose.useIntPreference
import com.kylecorry.trail_sense.shared.extensions.compose.useMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useNavController
import com.kylecorry.trail_sense.shared.extensions.compose.useNavigationSensors
import com.kylecorry.trail_sense.shared.extensions.compose.usePauseEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useRef
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.DateTimeSliderSheet
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigationSheetView
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.MapDistanceSheet
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import java.time.Instant
import androidx.compose.ui.graphics.Color as ComposeColor

class MapFragment : TrailSenseComposeFragment() {
    @Composable
    override fun FragmentContent() {
        val mapViewRef = useRef<MapView?>(null)
        val navigationSheetViewRef = useRef<NavigationSheetView?>(null)
        val mapDistanceSheetViewRef = useRef<MapDistanceSheet?>(null)
        val timeSheetRef = useRef<DateTimeSliderSheet?>(null)
        val sensorStatusBadgesRef = useRef<SensorStatusBadgeView?>(null)
        val (mapTime, setMapTime) = useState<Instant?>(null)
        val (attribution, setAttribution) = useState<CharSequence?>(null)
        val (hasTimeDependentLayers, setHasTimeDependentLayers) = useState(false)
        val (isTimeSheetVisible, setIsTimeSheetVisible) = useState(false)
        val (isMenuOpen, setIsMenuOpen) = useState(false)

        useEffect(timeSheetRef, mapTime, hasTimeDependentLayers) {
            val timeSheet = timeSheetRef.current ?: return@useEffect
            if (!hasTimeDependentLayers && isTimeSheetVisible) {
                setMapTime(null)
                timeSheet.hide()
                setIsTimeSheetVisible(false)
            }

            timeSheet.onTimeChanged = {
                setMapTime(it)
            }
        }

        val navigation = useNavigationSensors(trueNorth = true)
        val context = useAndroidContext()
        val sensors = useService<SensorService>()
        val hasCompass = useMemo(sensors) { sensors.hasCompass() }
        val navigator = useService<Navigator>()
        val pathService = useService<PathService>()
        val destination = useFlow(navigator.destination2, state = BackgroundMinimumState.Resumed)
        val prefs = useService<UserPreferences>()
        val formatter = useService<FormatService>()
        val activity = useActivity()
        val navController = useNavController()
        val (lockMode, setLockMode) = useLockMode()

        val screenLight = useMemo(activity) {
            ScreenTorch(activity.window)
        }

        val screenLock = useMemo(prefs) {
            NavigationScreenLock(prefs.map.keepScreenUnlockedWhileOpen)
        }

        useEffect(mapViewRef, prefs, resetOnResume) {
            val mapView = mapViewRef.current ?: return@useEffect
            mapView.useDensityPixelsForZoom = !prefs.map.highDetailMode
            mapView.layerManager.invalidate()
            mapView.invalidate()
        }

        useEffect(screenLock, activity, resetOnResume, destination) {
            screenLock.updateLock(activity)
        }

        useDestroyEffect(screenLock, activity) {
            screenLock.releaseLock(activity)
        }

        // Layers
        val manager = useMemo { MapToolLayerManager() }
        useEffect(manager, mapViewRef, mapTime, manager.key) {
            val mapView = mapViewRef.current ?: return@useEffect
            manager.setTime(mapView, mapTime)
        }
        useEffectWithCleanup(manager, mapViewRef, resetOnResume) {
            val mapView = mapViewRef.current ?: return@useEffectWithCleanup {}
            manager.resume(context, mapView, this@MapFragment)
            return@useEffectWithCleanup {
                manager.pause(mapView)
            }
        }

        useEffect(manager.key, mapViewRef) {
            val mapView = mapViewRef.current ?: return@useEffect
            setHasTimeDependentLayers(mapView.layerManager.getLayers().any { it.isTimeDependent })
        }

        useEffect(mapViewRef, manager.key) {
            val mapView = mapViewRef.current ?: return@useEffect
            setAttribution(mapView.getAttribution(context))
        }

        val layerEditSheet = useMemo(prefs) {
            MapLayersBottomSheet(
                MapToolRegistration.MAP_ID
            )
        }

        usePauseEffect(layerEditSheet) {
            tryOrNothing {
                layerEditSheet.setOnDismissListener(null)
                layerEditSheet.dismiss()
            }
        }

        val adjustLayers = useCallback<Unit>(manager, layerEditSheet, context, mapViewRef) {
            val mapView = mapViewRef.current ?: return@useCallback
            manager.pause(mapView)
            layerEditSheet.setOnDismissListener {
                manager.resume(context, mapView, this@MapFragment)
                setHasTimeDependentLayers(
                    mapView.layerManager.getLayers().any { it.isTimeDependent })
            }
            layerEditSheet.show(this)
        }

        // Distance
        val stopDistanceMeasurement = useCallback<Unit>(mapDistanceSheetViewRef, manager) {
            val mapDistanceSheetView = mapDistanceSheetViewRef.current ?: return@useCallback
            manager.stopDistanceMeasurement()
            mapDistanceSheetView.hide()
        }

        val startDistanceMeasurement =
            useCallback(
                mapDistanceSheetViewRef,
                stopDistanceMeasurement,
                navController,
                manager,
                pathService,
                navigation.location
            ) { location: Coordinate?, startWithUserLocation: Boolean ->
                val mapDistanceSheetView = mapDistanceSheetViewRef.current ?: return@useCallback
                val initialPoints = listOfNotNull(
                    if (startWithUserLocation) navigation.location else null,
                    location
                ).toTypedArray()
                manager.startDistanceMeasurement(initialPoints)
                mapDistanceSheetView.show()
                mapDistanceSheetView.cancelListener = {
                    stopDistanceMeasurement()
                }
                mapDistanceSheetView.createPathListener = {
                    inBackground {
                        val id = CreatePathCommand(
                            pathService,
                            prefs.navigation,
                            null
                        ).execute(manager.getDistanceMeasurementPoints())

                        onMain {
                            navController.navigateWithAnimation(
                                R.id.pathDetailsFragment,
                                Bundle().apply {
                                    putLong("path_id", id)
                                }
                            )
                        }
                    }
                }
                mapDistanceSheetView.undoListener = {
                    manager.undoLastDistanceMeasurement()
                }
            }

        // Update layer values
        useEffect(mapViewRef, navigation.location, navigation.locationAccuracy) {
            val mapView = mapViewRef.current ?: return@useEffect
            mapView.userLocation = navigation.location
            mapView.userLocationAccuracy = navigation.locationAccuracy
            mapView.invalidate()
        }

        useEffect(mapViewRef, navigation.bearing) {
            val mapView = mapViewRef.current ?: return@useEffect
            mapView.userAzimuth = navigation.bearing
        }

        useEffect(manager, mapViewRef, manager.key) {
            manager.onBoundsChanged()
        }

        useEffect(lockMode, mapViewRef, screenLight, context, resetOnResume) {
            val mapView = mapViewRef.current ?: return@useEffect
            switchMapLockMode(lockMode, mapView, screenLight, context)
        }

        usePauseEffect(screenLight) {
            screenLight.off()
            requireMainActivity().setBottomNavigationEnabled(true)
        }

        useSavedMapState(mapViewRef)

        useEffect(mapViewRef, lockMode, navigation.location) {
            val mapView = mapViewRef.current ?: return@useEffect
            if (mapView.mapCenter == Coordinate.zero) {
                mapView.mapCenter = navigation.location
                mapView.resolution = 2f
            }

            if (lockMode == MapLockMode.Location || lockMode == MapLockMode.Compass) {
                mapView.mapCenter = navigation.location
            }
        }

        useEffect(mapViewRef, lockMode, navigation.bearing) {
            val mapView = mapViewRef.current ?: return@useEffect
            if (lockMode == MapLockMode.Compass) {
                mapView.mapAzimuth = navigation.bearing.value
            }
        }

        useEffect(navigationSheetViewRef, destination, navigation) {
            val navigationSheetView = navigationSheetViewRef.current ?: return@useEffect
            if (destination != null) {
                navigationSheetView.updateNavigationSensorValues(navigation)
                navigationSheetView.setTrueNorthOverride(true)
                navigationSheetView.show(destination, true)
            } else {
                navigationSheetView.hide()
            }
        }

        useEffect(mapViewRef, manager, navController, startDistanceMeasurement, prefs) {
            val mapView = mapViewRef.current ?: return@useEffect
            mapView.setOnLongPressListener { location ->
                if (manager.isMeasuringDistance()) {
                    return@setOnLongPressListener
                }
                manager.setSelectedLocation(location)
                inBackground {
                    val elevation = Distance.meters(DEM.getElevation(location).elevation)

                    onMain {
                        Share.actions(
                            this@MapFragment,
                            formatter.formatLocation(location),
                            listOf(
                                ActionItem(getString(R.string.beacon), R.drawable.ic_location) {
                                    val bundle = Bundle().apply {
                                        putParcelable("initial_location", GeoUri(location))
                                    }
                                    navController.navigateWithAnimation(
                                        R.id.placeBeaconFragment,
                                        bundle
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(getString(R.string.navigate), R.drawable.ic_beacon) {
                                    navigator.navigateTo(
                                        location,
                                        formatter.formatLocation(location),
                                        BeaconOwner.Maps
                                    )
                                    manager.setSelectedLocation(null)
                                },
                                ActionItem(getString(R.string.distance), R.drawable.ruler) {
                                    startDistanceMeasurement(location, true)
                                    manager.setSelectedLocation(null)
                                },
                            ),
                            subtitle = getString(
                                R.string.elevation_value,
                                formatter.formatElevation(elevation)
                            ),
                        ) {
                            manager.setSelectedLocation(null)
                        }
                    }
                }
            }
        }

        useEffect(mapDistanceSheetViewRef, manager, prefs) {
            val mapDistanceSheetView = mapDistanceSheetViewRef.current ?: return@useEffect
            manager.setOnDistanceChangedCallback { distance ->
                val relative = distance
                    .convertTo(prefs.baseDistanceUnits)
                    .toRelativeDistance()
                mapDistanceSheetView.setDistance(relative)
            }
        }

        // Disclaimer
        useEffect(context) {
            CustomUiUtils.disclaimer(
                context,
                getString(R.string.map),
                getString(R.string.map_tool_disclaimer),
                "pref_map_tool_disclaimer_shown"
            )
        }

        // Sensor status
        useEffect(sensorStatusBadgesRef, navigation.gps, navigation.compass) {
            val sensorStatusBadges = sensorStatusBadgesRef.current ?: return@useEffect
            val gps = navigation.gps ?: return@useEffect
            val compass = navigation.compass ?: return@useEffect
            sensorStatusBadges.setSensors(gps as ISatelliteGPS, compass)
            sensorStatusBadges.setOnClickListener {
                ImproveAccuracyAlerter(context).alert(sensorStatusBadges.getSensors())
            }
        }

        MapContent(
            lockMode = lockMode,
            attribution = attribution,
            hasTimeDependentLayers = hasTimeDependentLayers,
            isTimeSheetVisible = isTimeSheetVisible,
            isMenuOpen = isMenuOpen,
            mapViewRef = mapViewRef,
            navigationSheetViewRef = navigationSheetViewRef,
            mapDistanceSheetViewRef = mapDistanceSheetViewRef,
            timeSheetRef = timeSheetRef,
            sensorStatusBadgesRef = sensorStatusBadgesRef,
            onZoomIn = { mapViewRef.current?.zoom(2f) },
            onZoomOut = { mapViewRef.current?.zoom(0.5f) },
            onLock = { setLockMode(getNextLockMode(lockMode, hasCompass)) },
            onTime = {
                val timeSheet = timeSheetRef.current ?: return@MapContent
                if (isTimeSheetVisible) {
                    setMapTime(null)
                    timeSheet.hide()
                    setIsTimeSheetVisible(false)
                } else {
                    timeSheet.setTime(mapTime)
                    timeSheet.show()
                    setIsTimeSheetVisible(true)
                }
            },
            onMenuOpenChange = setIsMenuOpen,
            onMenuAction = { action ->
                setIsMenuOpen(false)
                when (action) {
                    MapAction.Measure, MapAction.CreatePath -> startDistanceMeasurement(null, false)
                    MapAction.AdjustLayers -> adjustLayers()
                    MapAction.Trace -> setLockMode(MapLockMode.Trace)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    private fun switchMapLockMode(
        mode: MapLockMode,
        map: MapView,
        screenLight: ScreenTorch,
        context: android.content.Context
    ) {
        // Reset trace mode defaults
        map.isInteractive = true
        map.isZoomEnabled = true
        screenLight.off()
        requireMainActivity().setBottomNavigationEnabled(true)

        when (mode) {
            MapLockMode.Location -> {
                // Disable pan
                map.isPanEnabled = false

                // Zoom in and center on location
                map.resolution = 2f

                // Reset the rotation
                map.mapAzimuth = 0f
            }

            MapLockMode.Compass -> {
                // Disable pan
                map.isPanEnabled = false
            }

            MapLockMode.Free -> {
                // Enable pan
                map.isPanEnabled = true

                // Reset the rotation
                map.mapAzimuth = 0f
            }

            MapLockMode.Trace -> {
                CustomUiUtils.disclaimer(
                    context,
                    getString(R.string.trace),
                    getString(R.string.map_trace_instructions),
                    "disclaimer_shown_map_trace",
                    cancelText = null
                )

                // Disable all interaction
                map.isInteractive = false
                map.isZoomEnabled = false

                // Full brightness
                screenLight.on()

                // Hide the bottom navigation
                requireMainActivity().setBottomNavigationEnabled(false)
            }
        }
    }

    private fun getNextLockMode(mode: MapLockMode, hasCompass: Boolean): MapLockMode {
        return when (mode) {
            MapLockMode.Location -> {
                if (hasCompass) {
                    MapLockMode.Compass
                } else {
                    MapLockMode.Free
                }
            }

            MapLockMode.Compass -> {
                MapLockMode.Free
            }

            MapLockMode.Free -> {
                MapLockMode.Location
            }

            MapLockMode.Trace -> {
                MapLockMode.Free
            }
        }
    }

    @Composable
    private fun useSavedMapState(mapViewRef: Ref<MapView?>) {
        val prefs = useService<UserPreferences>()
        val shouldSave = useMemo(prefs, resetOnResume) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (center, setCenter) = useCoordinatePreference("${key}_coordinate")
        val (scale, setScale) = useFloatPreference("${key}_scale")

        useEffect(mapViewRef, shouldSave) {
            val mapView = mapViewRef.current ?: return@useEffect
            if (!shouldSave) {
                setCenter(null)
                setScale(null)
            } else {
                // Intentionally not in the useEffect dependencies
                center?.let { mapView.mapCenter = it }
                scale?.let { mapView.resolutionPixels = it }
            }

            mapView.setOnScaleChangeListener {
                if (shouldSave) {
                    setScale(it)
                }
            }
            mapView.setOnCenterChangeListener {
                if (shouldSave) {
                    setCenter(it)
                }
            }
        }
    }

    @Composable
    private fun useLockMode(): Pair<MapLockMode, (MapLockMode) -> Unit> {
        val prefs = useService<UserPreferences>()
        val shouldSave = useMemo(prefs, resetOnResume) {
            prefs.map.saveMapState
        }
        val key = "cache_map_state"
        val (savedLockModeId, setSavedLockModeId) = useIntPreference("${key}_lock_mode")
        val savedLockMode = useMemo(savedLockModeId) {
            MapLockMode.entries.firstOrNull { it.id == savedLockModeId && it != MapLockMode.Trace }
        }
        val (lockMode, setLockMode) = useState(savedLockMode ?: MapLockMode.Free)
        val exposedSetLockMode = useCallback(shouldSave) { newLockMode: MapLockMode ->
            setLockMode(newLockMode)
            if (shouldSave) {
                setSavedLockModeId(newLockMode.id)
            }
        }

        useEffect(shouldSave) {
            if (!shouldSave) {
                setSavedLockModeId(null)
            }
        }

        return lockMode to exposedSetLockMode
    }
}

private enum class MapLockMode(val id: Int) {
    Location(1),
    Compass(2),
    Free(3),
    Trace(4)
}

@Composable
private fun MapContent(
    lockMode: MapLockMode,
    attribution: CharSequence?,
    hasTimeDependentLayers: Boolean,
    isTimeSheetVisible: Boolean,
    isMenuOpen: Boolean,
    mapViewRef: Ref<MapView?>,
    navigationSheetViewRef: Ref<NavigationSheetView?>,
    mapDistanceSheetViewRef: Ref<MapDistanceSheet?>,
    timeSheetRef: Ref<DateTimeSliderSheet?>,
    sensorStatusBadgesRef: Ref<SensorStatusBadgeView?>,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLock: () -> Unit,
    onTime: () -> Unit,
    onMenuOpenChange: (Boolean) -> Unit,
    onMenuAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTraceMode = lockMode == MapLockMode.Trace
    val lockIcon = when (lockMode) {
        MapLockMode.Compass -> R.drawable.ic_compass_icon
        MapLockMode.Trace -> R.drawable.lock
        else -> R.drawable.satellite
    }
    val isLockActive = lockMode != MapLockMode.Free

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { context ->
                    MapView(context, null).apply {
                        id = R.id.map
                        setBackgroundColor(Color.rgb(127, 127, 127))
                        mapViewRef.current = this
                    }
                },
                modifier = Modifier
                    .matchParentSize()
                    .keepScreenOn()
                    .testTag("map")
            )

            if (attribution != null) {
                // TODO: This isn't rendering the markdown links correctly
                Text(
                    text = annotateWithLinks(attribution.toString()),
                    color = ComposeColor.White,
                    fontSize = 8.sp,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(ComposeColor.Black.copy(alpha = 0.53f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("map_attribution")
                )
            }

            if (!isTraceMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp)
                ) {
                    MapFab(
                        icon = R.drawable.ic_menu_dots,
                        active = false,
                        onClick = { onMenuOpenChange(true) },
                        modifier = Modifier.testTag("menu_btn")
                    )
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { onMenuOpenChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.measure)) },
                            onClick = { onMenuAction(MapAction.Measure) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.create_path)) },
                            onClick = { onMenuAction(MapAction.CreatePath) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.layers)) },
                            onClick = { onMenuAction(MapAction.AdjustLayers) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.trace)) },
                            onClick = { onMenuAction(MapAction.Trace) }
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 48.dp)
            ) {
                if (!isTraceMode && hasTimeDependentLayers) {
                    MapFab(
                        icon = R.drawable.ic_tool_clock,
                        active = isTimeSheetVisible,
                        onClick = onTime,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("time_btn")
                    )
                }
                if (!isTraceMode) {
                    MapFab(
                        icon = R.drawable.ic_add,
                        active = false,
                        onClick = onZoomIn,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .testTag("zoom_in_btn")
                    )
                    MapFab(
                        icon = R.drawable.ic_zoom_out,
                        active = false,
                        onClick = onZoomOut,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("zoom_out_btn")
                    )
                }
                MapFab(
                    icon = lockIcon,
                    active = isLockActive,
                    onClick = onLock,
                    modifier = Modifier.testTag("lock_btn")
                )
            }

            if (!isTraceMode) {
                AndroidView(
                    factory = { context ->
                        SensorStatusBadgeView(context).apply {
                            id = R.id.sensor_status_badges
                            alpha = 0.6f
                            sensorStatusBadgesRef.current = this
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 32.dp)
                        .testTag("sensor_status_badges")
                )
            }
        }

        AndroidView(
            factory = { context ->
                DateTimeSliderSheet(context, null).apply {
                    id = R.id.time_sheet
                    isVisible = false
                    timeSheetRef.current = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("time_sheet")
        )
        AndroidView(
            factory = { context ->
                NavigationSheetView(context).apply {
                    id = R.id.navigation_sheet
                    isVisible = false
                    navigationSheetViewRef.current = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("navigation_sheet")
        )
        AndroidView(
            factory = { context ->
                MapDistanceSheet(context, null).apply {
                    id = R.id.distance_sheet
                    isVisible = false
                    mapDistanceSheetViewRef.current = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("distance_sheet")
        )
    }
}

@Composable
private fun MapFab(
    icon: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    FloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = FloatingActionButtonDefaults.smallShape,
        elevation = FloatingActionButtonDefaults.loweredElevation(),
        modifier = modifier
            .size(40.dp)
            .alpha(0.86f)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun MapContentPreview() {
    val mapViewRef = useRef<MapView?>(null)
    val navigationSheetViewRef = useRef<NavigationSheetView?>(null)
    val mapDistanceSheetViewRef = useRef<MapDistanceSheet?>(null)
    val timeSheetRef = useRef<DateTimeSliderSheet?>(null)
    val sensorStatusBadgesRef = useRef<SensorStatusBadgeView?>(null)

    MaterialTheme {
        MapContent(
            lockMode = MapLockMode.Free,
            attribution = null,
            hasTimeDependentLayers = true,
            isTimeSheetVisible = false,
            isMenuOpen = false,
            mapViewRef = mapViewRef,
            navigationSheetViewRef = navigationSheetViewRef,
            mapDistanceSheetViewRef = mapDistanceSheetViewRef,
            timeSheetRef = timeSheetRef,
            sensorStatusBadgesRef = sensorStatusBadgesRef,
            onZoomIn = {},
            onZoomOut = {},
            onLock = {},
            onTime = {},
            onMenuOpenChange = {},
            onMenuAction = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
