package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import org.mapsforge.core.model.Tag
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MapReadResult
import org.mapsforge.map.datastore.PointOfInterest
import org.mapsforge.map.rendertheme.RenderContext

class PeakElevationMapReadResultModifier(
    private val units: DistanceUnits,
    private val formatter: FormatService
) : MapReadResultModifier {

    override fun process(
        renderContext: RenderContext,
        mapReadResult: MapReadResult,
        mapDataStore: MapDataStore
    ) {
        mapReadResult.pois.replaceAll { modify(it) }
    }

    private fun modify(poi: PointOfInterest): PointOfInterest {
        val elevation = getPeakElevation(poi) ?: return poi
        val tier = PeakTierCaptionGenerator.getElevationTier(elevation)

        val baseTags = poi.tags.map { if (it.key == ELEVATION) Tag(ELEVATION, formatElevation(elevation)) else it }

        return PointOfInterest(poi.layer, baseTags + PROCESSED_TAG + Tag(ELEVATION_TIER, tier.toString()), poi.position)
    }

    private fun isPeak(poi: PointOfInterest): Boolean {
        return poi.tags.any { it.key == NATURAL && it.value == PEAK }
    }

    private fun formatElevation(elevationMeters: Float): String {
        val distance = Distance.meters(elevationMeters).convertTo(units)
        return formatter.formatDistance(distance, 0)
    }

    private fun getPeakElevation(poi: PointOfInterest): Float? {
        if (!isPeak(poi) || PROCESSED_TAG in poi.tags) {
            return null
        }
        
        return poi.tags.firstOrNull { it.key == ELEVATION }?.value?.let { tag ->
            elevationRegex.find(tag.replace(",", ""))?.value?.toFloatCompat()
        }
    }

    companion object {
        private const val ELEVATION = "ele"
        private const val ELEVATION_TIER = "trail_sense_elevation_tier"
        private const val NATURAL = "natural"
        private const val PEAK = "peak"
        private val PROCESSED_TAG = Tag("trail_sense_peak_elevation", "yes")
        private val elevationRegex = Regex("[-+]?\\d+(?:\\.\\d+)?")
    }
}
