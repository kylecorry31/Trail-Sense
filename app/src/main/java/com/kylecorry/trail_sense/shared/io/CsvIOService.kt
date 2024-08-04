package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.luna.streams.readText

class CsvIOService(private val uriPicker: UriPicker, private val uriService: UriService) :
    IOService<List<List<String>>> {
    override suspend fun export(data: List<List<String>>, filename: String): Boolean {
        val uri = uriPicker.create(filename, "text/csv") ?: return false
        val csvString = CSVConvert.toCSV(data)
        return uriService.write(uri, csvString)
    }

    override suspend fun import(): List<List<String>>? = onIO {
        tryOrDefault(null) {
            val uri =
                uriPicker.open(
                    listOf(
                        "text/csv",
                        "application/csv",
                        "text/comma-separated-values",
                        "text/plain"
                    )
                )
                    ?: return@onIO null
            val stream = uriService.inputStream(uri) ?: return@onIO null
            CSVConvert.parse(stream.readText())
        }
    }
}