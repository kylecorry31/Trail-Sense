package com.kylecorry.trail_sense.tools.map.ui

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useClickCallback
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors
import com.kylecorry.trail_sense.shared.sensors.SensorService

class MapFragment : TrailSenseReactiveFragment(R.layout.fragment_map) {
    override fun update() {
        val mapView = useView<MapView>(R.id.map)
        val lockButton = useView<FloatingActionButton>(R.id.lock_btn)
        val navigation = useNavigationSensors(trueNorth = true)
        val context = useAndroidContext()
        val (lockMode, setLockMode) = useState(MapLockMode.Free)
        val sensors = useService<SensorService>()
        val hasCompass = useMemo(sensors) { sensors.hasCompass() }

        useClickCallback(lockButton, lockMode, hasCompass) {
            setLockMode(getNextLockMode(lockMode, hasCompass))
        }

        // Layers
        val manager = useMemo { MapToolLayerManager() }
        useEffectWithCleanup(manager, mapView) {
            manager.resume(context, mapView)
            return@useEffectWithCleanup {
                manager.pause(context, mapView)
            }
        }

        // Update layer values
        useEffect(manager, navigation.location, navigation.locationAccuracy) {
            manager.onLocationChanged(navigation.location, navigation.locationAccuracy)
        }

        useEffect(manager, navigation.bearing) {
            manager.onBearingChanged(navigation.bearing.value)
        }

        useEffect(manager, mapView.mapBounds) {
            manager.onBoundsChanged(mapView.mapBounds)
        }

        useEffect(manager, navigation.elevation) {
            manager.onElevationChanged(navigation.elevation)
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