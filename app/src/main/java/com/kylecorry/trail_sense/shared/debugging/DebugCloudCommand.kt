package com.kylecorry.trail_sense.shared.debugging

import android.content.Context
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.trail_sense.shared.io.FileSubsystem

class DebugCloudCommand(
    private val context: Context,
    private val features: List<Float>
) : DebugCommand() {

    override fun executeDebug() {
        val header = listOf(
            listOf(
                "NRBR",
                "EN",
                "CON",
                "GLCM AVG",
                "GLCM STDEV",
                "BIAS"
            )
        )
        val data = header + listOf(features)

        FileSubsystem.getInstance(context).writeDebug(
            "cloud.csv",
            CSVConvert.toCSV(data)
        )
    }
}