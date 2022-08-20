package com.kylecorry.trail_sense.shared.debugging

import android.content.Context
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.io.Files

class DebugElevationsCommand(
    private val context: Context,
    private val readings: List<Reading<Float>>,
    private val smoothed: List<Reading<Float>>
) : DebugCommand() {
    override fun executeDebug() {
        val header = listOf(listOf("time", "raw", "smoothed"))
        val data = header + readings.zip(smoothed).map {
            listOf(
                it.first.time.toEpochMilli(),
                it.first.value,
                it.second.value
            )
        }

        Files.debugFile(
            context,
            "altitude_history.csv",
            CSVConvert.toCSV(data)
        )
    }
}