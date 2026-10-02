package com.kylecorry.trail_sense.tools.map.ui.terrain3d

import android.graphics.Bitmap
import androidx.core.view.drawToBitmap
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.sol.time.Time.toZonedDateTime
import java.time.Instant
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class SkyBody(val azimuth: Float, val altitude: Float)

class MapTerrain(
    val mesh: TerrainMesh,
    val texture: Bitmap,
    val sun: SkyBody,
    val moon: SkyBody
)

/**
 * Builds a [MapTerrain] from what the map is currently showing: the DEM for the visible bounds,
 * draped with a snapshot of the map.
 */
object MapTerrainLoader {
    private const val MAX_GRID_CELLS = 128
    private const val MIN_RESOLUTION = 1 / 240.0 / 4

    suspend fun load(mapView: MapView, time: Instant): MapTerrain {
        // Overlays are drawn in screen space, so they don't belong on the terrain
        val snapshot = onMain {
            mapView.drawOverlays = false
            try {
                mapView.drawToBitmap()
            } finally {
                mapView.drawOverlays = true
            }
        }

        val bounds = mapView.mapBounds
        val center = bounds.center
        val resolution = max(
            max(bounds.north - bounds.south, bounds.east - bounds.west) / MAX_GRID_CELLS,
            MIN_RESOLUTION
        )
        val grid = DEM.getElevationGrid(bounds, resolution)

        val projection = mapView.mapProjection
        val theta = Math.toRadians(-mapView.mapAzimuth.toDouble())
        val width = snapshot.width.toFloat()
        val height = snapshot.height.toFloat()
        val mesh = TerrainMesh.from(grid, center.latitude, center.longitude) { latitude, longitude ->
            val pixel = projection.toPixels(latitude, longitude)
            // The snapshot is rotated by the map azimuth about the view center
            val dx = pixel.x - width / 2
            val dy = pixel.y - height / 2
            val x = width / 2 + dx * cos(theta) - dy * sin(theta)
            val y = height / 2 + dx * sin(theta) + dy * cos(theta)
            (x / width).toFloat() to (y / height).toFloat()
        }

        val astronomy = AstronomyService()
        val zonedTime = time.toZonedDateTime()
        val sun = astronomy.getSunPosition(center, zonedTime)
        val moon = astronomy.getMoonPosition(center, zonedTime)
        return MapTerrain(
            mesh,
            snapshot,
            SkyBody(sun.azimuth.value, sun.altitude),
            SkyBody(moon.azimuth.value, moon.altitude)
        )
    }
}
