package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.gpx.GPXData

class IOFactory {

    fun createGpxService(fragment: AndromedaFragment): IOService<GPXData> {
        return GpxIOService(
            FragmentUriPicker(fragment),
            ExternalUriService(fragment.requireContext())
        )
    }

    fun createCsvService(activity: AndromedaActivity): ExportService<List<List<String>>> {
        return CsvExportService(
            ActivityUriPicker(activity),
            ExternalUriService(activity)
        )
    }

}