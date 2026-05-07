package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.mapsforge

import org.mapsforge.map.datastore.PointOfInterest

interface MapsforgePoiModifier {
    fun modify(poi: PointOfInterest): PointOfInterest
}
