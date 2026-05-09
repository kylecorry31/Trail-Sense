package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import org.mapsforge.core.model.Tag
import org.mapsforge.map.datastore.PointOfInterest

class PeakElevationPoiModifier(
    private val units: DistanceUnits,
    private val formatter: FormatService
) : MapsforgePoiModifier {

    override fun modify(poi: PointOfInterest): PointOfInterest {
        val elevation = getPeakElevation(poi) ?: return poi
        val formattedElevation = formatElevation(elevation)
        val formattedTags = poi.tags.map {
            if (it.key == ELEVATION) {
                Tag(ELEVATION, formattedElevation)
            } else {
                it
            }
        }
        return PointOfInterest(poi.layer, formattedTags, poi.position)
    }

    private fun isPeak(poi: PointOfInterest): Boolean {
        return poi.tags.any { it.key == NATURAL && it.value == PEAK }
    }

    private fun formatElevation(elevationMeters: Float): String {
        val distance = Distance.meters(elevationMeters).convertTo(units)
        return formatter.formatDistance(distance, 0)
    }

    private fun getPeakElevation(poi: PointOfInterest): Float? {
        if (!isPeak(poi)) {
            return null
        }

        return poi.tags.firstOrNull { it.key == ELEVATION }?.value?.let { tag ->
            elevationRegex.find(tag.replace(",", ""))?.value?.toFloatCompat()
        }
    }

    companion object {
        private const val ELEVATION = "ele"
        private const val NATURAL = "natural"
        private const val PEAK = "peak"
        private val elevationRegex = Regex("[-+]?\\d+(?:\\.\\d+)?")
    }
}
