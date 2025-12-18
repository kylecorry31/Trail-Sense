package com.kylecorry.trail_sense.tools.navigation.map_layers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.navigation.domain.Destination

class NavigationLayer : GeoJsonLayer<NavigationGeoJsonSource>(NavigationGeoJsonSource()) {

    var useLocationWithBearing: Boolean
        get() = source.useLocationWithBearing
        set(value) {
            source.useLocationWithBearing = value
            invalidate()
        }

    fun setMyLocation(location: Coordinate?) {
        source.myLocation = location
        invalidate()
    }

    fun setDestination(destination: Destination?) {
        source.destination = destination
        invalidate()
    }

    fun setPreferences(prefs: NavigationMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
        invalidate()
    }
}