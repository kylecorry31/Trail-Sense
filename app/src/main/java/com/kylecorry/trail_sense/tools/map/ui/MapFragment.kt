package com.kylecorry.trail_sense.tools.map.ui

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors

class MapFragment : TrailSenseReactiveFragment(R.layout.fragment_map) {
    override fun update() {
        val mapView = useView<MapView>(R.id.map)
        val navigation = useNavigationSensors(trueNorth = true)
        val context = useAndroidContext()

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

        // TODO: Only if locked / first time
        useEffect(mapView, navigation.location) {
            if (mapView.mapCenter == Coordinate.zero) {
                mapView.mapCenter = navigation.location
                mapView.metersPerPixel = 0.5f
            }
        }
    }
}