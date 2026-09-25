package com.kylecorry.trail_sense.shared.debugging

import android.content.Context
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.clouds.domain.classification.CloudCNNClassifier

class DebugCloudCommand(
    private val context: Context,
    private val features: List<Float>
) : DebugCommand() {

    override fun executeDebug() {
        val header = listOf(CloudCNNClassifier.CLOUD_GENUSES.map { it.name } + "CLEAR")
        val data = header + listOf(features)

        FileSubsystem.getInstance(context).writeDebug(
            "cloud.csv",
            CSVConvert.toCSV(data)
        )
    }
}
