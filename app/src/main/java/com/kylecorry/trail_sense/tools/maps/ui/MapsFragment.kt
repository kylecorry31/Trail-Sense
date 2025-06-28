package com.kylecorry.trail_sense.tools.maps.ui

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useNavigationSensors

class MapsFragment : TrailSenseReactiveFragment(R.layout.fragment_maps) {
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
        useEffect(navigation.location, navigation.locationAccuracy) {
            manager.onLocationChanged(navigation.location, navigation.locationAccuracy)
        }

        useEffect(navigation.bearing) {
            manager.onBearingChanged(navigation.bearing.value)
        }

        useEffect(mapView.mapBounds) {
            manager.onBoundsChanged(mapView.mapBounds)
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