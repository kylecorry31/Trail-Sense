package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.geopoint.GeoPointFile
import com.kylecorry.trail_sense.shared.data.AssetByteRangeReader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

object CellTowerModel {

    private const val PATH = "cell_towers/cell_towers.tsgeo"
    private const val TYPE = "cell_towers"

    // Towers are snapped to a grid of this size, so can't be more accurate than this resolution
    private const val ACCURACY_RESOLUTION = 0.01

    // Accounts for errors in the dataset
    private const val ACCURACY_SCALE = 2f

    private val mutex = Mutex()
    private var towers: GeoPointFile? = null

    fun getAccuracy(towerLocation: Coordinate): Distance {
        return Distance.meters(
            ACCURACY_SCALE * towerLocation.distanceTo(
                Coordinate(
                    towerLocation.latitude,
                    towerLocation.longitude + ACCURACY_RESOLUTION / 2
                )
            )
        )
    }

    suspend fun getTowers(
        bounds: CoordinateBounds,
        count: Int? = null
    ): List<ApproximateCoordinate> = onIO {
        val file = getFile() ?: return@onIO emptyList()

        // Each cell is trimmed to the closest towers before the next one is read, so only the closest are kept
        var closest = emptyList<ApproximateCoordinate>()
        for (cell in file.getCells(bounds.south, bounds.north, bounds.west, bounds.east)) {
            val cellTowers = mutableListOf<ApproximateCoordinate>()
            for (i in 0 until cell.size) {
                val location = Coordinate(cell.latitudes[i], cell.longitudes[i])
                if (bounds.contains(location)) {
                    cellTowers.add(ApproximateCoordinate(location.latitude, location.longitude, getAccuracy(location)))
                }
            }
            closest = closestTo(bounds, closest + closestTo(bounds, cellTowers, count), count)
        }
        closest
    }

    private fun closestTo(
        bounds: CoordinateBounds,
        towers: List<ApproximateCoordinate>,
        count: Int? = null
    ): List<ApproximateCoordinate> {
        return towers
            .sortedBy { it.coordinate.distanceTo(bounds.center) }
            .take(count ?: Int.MAX_VALUE)
    }

    private suspend fun getFile(): GeoPointFile? = mutex.withLock {
        towers?.let { return@withLock it }
        val file = try {
            GeoPointFile.open(AssetByteRangeReader(PATH))
        } catch (e: IOException) {
            null
        }
        if (file?.getStringProperty("type") != TYPE) {
            file?.close()
            return@withLock null
        }
        towers = file
        file
    }

}
