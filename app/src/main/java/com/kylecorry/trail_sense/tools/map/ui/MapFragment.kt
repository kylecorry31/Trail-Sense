package com.kylecorry.trail_sense.tools.map.ui

import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useClickCallback
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.useFlow
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useDestroyEffect
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.BeaconDestinationView
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class MapFragment : TrailSenseReactiveFragment(R.layout.fragment_map) {
    override fun update() {
        val mapView = useView<MapView>(R.id.map)
        val cancelNavigationButton = useView<FloatingActionButton>(R.id.cancel_navigation_btn)
        val lockButton = useView<FloatingActionButton>(R.id.lock_btn)
        val zoomInButton = useView<FloatingActionButton>(R.id.zoom_in_btn)
        val zoomOutButton = useView<FloatingActionButton>(R.id.zoom_out_btn)
        val navigationSheetView = useView<BeaconDestinationView>(R.id.navigation_sheet)
        val navigation = useNavigationSensors(trueNorth = true)
        val context = useAndroidContext()
        val (lockMode, setLockMode) = useState(MapLockMode.Free)
        val sensors = useService<SensorService>()
        val hasCompass = useMemo(sensors) { sensors.hasCompass() }
        val navigator = useService<Navigator>()
        val destination = useFlow(navigator.destination, BackgroundMinimumState.Resumed)
        val prefs = useService<UserPreferences>()
        val activity = useActivity()
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