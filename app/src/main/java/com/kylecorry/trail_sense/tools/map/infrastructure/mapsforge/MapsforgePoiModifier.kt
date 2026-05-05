package com.kylecorry.trail_sense.tools.map.infrastructure.mapsforge

import org.mapsforge.map.datastore.PointOfInterest

interface MapsforgePoiModifier {
    fun modify(poi: PointOfInterest): PointOfInterest
}
