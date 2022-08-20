package com.kylecorry.trail_sense.shared.debugging

import android.content.Context
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.trail_sense.navigation.domain.hiking.HikingService
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.io.Files

class DebugPathElevationsCommand(
    private val context: Context,
    private val raw: List<PathPoint>,
    private val smoothed: List<PathPoint>
) : DebugCommand() {

    private val hikingService = HikingService()

    override fun executeDebug() {
        val distances = hikingService.getDistances(smoothed)
        val header = listOf(listOf("distance", "raw", "smoothed"))
        val data = header + distances.zip(raw.sortedByDescending { it.id }
            .zip(smoothed)).map {
            listOf(it.first, it.second.first.elevation, it.second.second.elevation)
        }

        Files.debugFile(
            context,
            "path_elevations.csv",
            CSVConvert.toCSV(data)
        )
    }
}