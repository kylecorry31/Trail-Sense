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

    fun createCsvService(activity: AndromedaActivity): IOService<List<List<String>>> {
        return CsvIOService(
            ActivityUriPicker(activity),
            ExternalUriService(activity)
        )
    }

    fun createCsvService(fragment: AndromedaFragment): IOService<List<List<String>>> {
        return CsvIOService(
            FragmentUriPicker(fragment),
            ExternalUriService(fragment.requireContext())
        )
    }

}