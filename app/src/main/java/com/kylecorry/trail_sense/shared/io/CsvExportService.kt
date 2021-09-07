package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.csv.CSVConvert

class CsvExportService(private val uriPicker: UriPicker, private val uriService: UriService) :
    ExportService<List<List<String>>> {
    override suspend fun export(data: List<List<String>>, filename: String): Boolean {
        val uri = uriPicker.create(filename, "text/csv") ?: return false
        val csvString = CSVConvert.toCSV(data)
        return uriService.write(uri, csvString)
    }
}