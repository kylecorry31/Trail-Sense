package com.kylecorry.trail_sense.tools.map.ui

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useClickCallback
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.useFlow
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useDestroyEffect
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.BeaconDestinationView
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.photo_maps.ui.MapDistanceSheet

class MapFragment : TrailSenseReactiveFragment(R.layout.fragment_map) {
    override fun update() {
        val mapView = useView<MapView>(R.id.map)
        val cancelNavigationButton = useView<FloatingActionButton>(R.id.cancel_navigation_btn)
        val lockButton = useView<FloatingActionButton>(R.id.lock_btn)
        val zoomInButton = useView<FloatingActionButton>(R.id.zoom_in_btn)
        val zoomOutButton = useView<FloatingActionButton>(R.id.zoom_out_btn)
        val menuButton = useView<FloatingActionButton>(R.id.menu_btn)
        val navigationSheetView = useView<BeaconDestinationView>(R.id.navigation_sheet)
        val mapDistanceSheetView = useView<MapDistanceSheet>(R.id.distance_sheet)
        val navigation = useNavigationSensors(trueNorth = true)
        val context = useAndroidContext()
        val (lockMode, setLockMode) = useState(MapLockMode.Free)
        val sensors = useService<SensorService>()
        val hasCompass = useMemo(sensors) { sensors.hasCompass() }
        val navigator = useService<Navigator>()
        val pathService = useService<PathService>()
        val destination = useFlow(navigator.destination, BackgroundMinimumState.Resumed)
        val prefs = useService<UserPreferences>()
        val formatter = useService<FormatService>()
        val activity = useActivity()
        val navController = useNavController()

        val screenLock = useMemo(prefs) {
            NavigationScreenLock(prefs.map.keepScreenUnlockedWhileOpen)
        }

        useEffect(screenLock, activity, resetOnResume, destination) {
            screenLock.updateLock(activity)
        }

        useDestroyEffect(screenLock, activity) {
            screenLock.releaseLock(activity)
        }

        useClickCallback(lockButton, lockMode, hasCompass) {
            setLockMode(getNextLockMode(lockMode, hasCompass))
        }

        useClickCallback(cancelNavigationButton, navigator) {
            navigator.cancelNavigation()
        }

        // Layers
        val manager = useMemo { MapToolLayerManager() }
        useEffectWithCleanup(manager, mapView, resetOnResume) {
            manager.resume(context, mapView)
            return@useEffectWithCleanup {
                manager.pause(context, mapView)
            }
        }

        // Distance
        val stopDistanceMeasurement = useCallback<Unit>(mapDistanceSheetView, manager) {
            manager.stopDistanceMeasurement()
            mapDistanceSheetView.hide()
        }

        val startDistanceMeasurement =
            useCallback(
                mapDistanceSheetView,
                stopDistanceMeasurement,
                navController,
                manager,
                pathService,
                navigation.location
            ) { location: Coordinate?, startWithUserLocation: Boolean ->
                val initialPoints = listOfNotNull(
                    if (startWithUserLocation) navigation.location else null,
                    location
                ).toTypedArray()
                manager.startDistanceMeasurement(*initialPoints)
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
                                bundleOf("path_id" to id)
                            )
                        }
                    }
                }
                mapDistanceSheetView.undoListener = {
                    manager.undoLastDistanceMeasurement()
                }
            }

        // Update layer values
        useEffect(mapView, manager, navigation.location, navigation.locationAccuracy) {
            manager.onLocationChanged(navigation.location, navigation.locationAccuracy)
            mapView.invalidate()
        }

        useEffect(mapView, manager, navigation.bearing) {
            manager.onBearingChanged(navigation.bearing.value)
            mapView.invalidate()
        }

        useEffect(manager, mapView.mapBounds) {
            manager.onBoundsChanged(mapView.mapBounds)
        }

        useEffect(mapView, manager, navigation.elevation) {
            manager.onElevationChanged(navigation.elevation)
            mapView.invalidate()
        }

        useEffect(lockMode, mapView, lockButton) {
            switchMapLockMode(lockMode, mapView, lockButton)
        }

        useEffect(mapView, lockMode, navigation.location) {
            if (mapView.mapCenter == Coordinate.zero) {
                mapView.mapCenter = navigation.location
                mapView.metersPerPixel = 0.5f
            }

            if (lockMode == MapLockMode.Location || lockMode == MapLockMode.Compass) {
                mapView.mapCenter = navigation.location
            }
        }

        useEffect(mapView, lockMode, navigation.bearing) {
            if (lockMode == MapLockMode.Compass) {
                mapView.mapAzimuth = navigation.bearing.value
            }
        }

        useEffect(zoomInButton, zoomOutButton) {
            CustomUiUtils.setButtonState(zoomInButton, false)
            CustomUiUtils.setButtonState(zoomOutButton, false)

            zoomInButton.setOnClickListener {
                mapView.zoom(2f)
            }

            zoomOutButton.setOnClickListener {
                mapView.zoom(0.5f)
            }
        }

        useEffect(cancelNavigationButton, navigationSheetView, destination, navigation) {
            cancelNavigationButton.isVisible = destination != null
            if (destination != null) {
                navigationSheetView.show(navigation, destination, true)
            } else {
                navigationSheetView.hide()
            }
        }

        useEffect(mapView, manager, navController, startDistanceMeasurement) {
            mapView.setOnLongPressListener { location ->
                if (manager.isMeasuringDistance()) {
                    return@setOnLongPressListener
                }
                manager.setSelectedLocation(location)

                Share.actions(
                    this,
                    formatter.formatLocation(location),
                    listOf(
                        ActionItem(getString(R.string.beacon), R.drawable.ic_location) {
                            val bundle = bundleOf(
                                "initial_location" to GeoUri(location)
                            )
                            navController.navigateWithAnimation(R.id.placeBeaconFragment, bundle)
                            manager.setSelectedLocation(null)
                        },
                        ActionItem(getString(R.string.navigate), R.drawable.ic_beacon) {
                            navigator.navigateTo(
                                location,
                                formatter.formatLocation(location),
                                BeaconOwner.Maps,
                                true
                            )
                            manager.setSelectedLocation(null)
                        },
                        ActionItem(getString(R.string.distance), R.drawable.ruler) {
                            startDistanceMeasurement(location, true)
                            manager.setSelectedLocation(null)
                        },
                    )
                ) {
                    manager.setSelectedLocation(null)
                }
            }
        }

        useEffect(mapDistanceSheetView, manager, prefs) {
            manager.setOnDistanceChangedCallback { distance ->
                val relative = distance
                    .convertTo(prefs.baseDistanceUnits)
                    .toRelativeDistance()
                mapDistanceSheetView.setDistance(relative)
            }
        }

        // Menu
        useEffect(menuButton) {
            CustomUiUtils.setButtonState(menuButton, false)
        }

        useClickCallback(menuButton, startDistanceMeasurement) {
            val actions = listOf(
                MapAction.Measure to getString(R.string.measure),
                MapAction.CreatePath to getString(R.string.create_path),
            )


            Pickers.menu(
                menuButton,
                actions.map { action -> action.second }
            ) { index ->
                when (actions[index].first) {
                    MapAction.Measure, MapAction.CreatePath -> startDistanceMeasurement(
                        null,
                        false
                    )
                }
                true
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
    }

    private fun switchMapLockMode(
        mode: MapLockMode,
        map: MapView,
        button: FloatingActionButton
    ) {
        when (mode) {
            MapLockMode.Location -> {
                // Disable pan
                map.isPanEnabled = false

                // Zoom in and center on location
                map.metersPerPixel = 0.5f

                // Reset the rotation
                map.mapAzimuth = 0f

                // Show as locked
                button.setImageResource(R.drawable.satellite)
                CustomUiUtils.setButtonState(button, true)
            }

            MapLockMode.Compass -> {
                // Disable pan
                map.isPanEnabled = false

                // Show as locked
                button.setImageResource(R.drawable.ic_compass_icon)
                CustomUiUtils.setButtonState(button, true)
            }

            MapLockMode.Free -> {
                // Enable pan
                map.isPanEnabled = true

                // Reset the rotation
                map.mapAzimuth = 0f

                // Show as unlocked
                button.setImageResource(R.drawable.satellite)
                CustomUiUtils.setButtonState(button, false)
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
        }
    }

    private enum class MapLockMode {
        Location,
        Compass,
        Free
    }
}