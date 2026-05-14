package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.units.Coordinate
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.datastore.Way
import kotlin.math.abs

object WayCentroidCalculator {

    fun calculate(ways: List<Way>): LatLong? {
        // https://mathworld.wolfram.com/PolygonArea.html
        // https://mathworld.wolfram.com/PolygonCentroid.html
        var totalArea = 0.0
        var centroidLat = 0.0
        var centroidLon = 0.0

        for (ring in ways.flatMap { it.latLongs.toList() }) {
            if (ring.size < 3) {
                continue
            }

            var area2 = 0.0
            var cx = 0.0
            var cy = 0.0

            val n = ring.size
            val lastIndex = if (samePoint(ring.first(), ring.last())) {
                n - 2
            } else {
                n - 1
            }

            for (i in 0..lastIndex) {
                val p1 = ring[i]
                val p2 = ring[if (i == lastIndex) 0 else i + 1]

                val x1 = p1.longitude
                val y1 = p1.latitude
                val x2 = p2.longitude
                val y2 = p2.latitude

                val cross = x1 * y2 - x2 * y1

                area2 += cross
                cx += (x1 + x2) * cross
                cy += (y1 + y2) * cross
            }

            centroidLon += cx
            centroidLat += cy
            totalArea += area2 / 2
        }

        if (Arithmetic.isZero(totalArea.toFloat())) {
            return null
        }

        return LatLong(
            (centroidLat / (6 * totalArea)).coerceIn(-90.0, 90.0),
            Coordinate.toLongitude(centroidLon / (6 * totalArea))
        )
    }

    private fun samePoint(a: LatLong?, b: LatLong?): Boolean {
        if (a == null || b == null) return false

        return abs(a.latitude - b.latitude) < 1e-12 &&
                abs(a.longitude - b.longitude) < 1e-12
    }
}
