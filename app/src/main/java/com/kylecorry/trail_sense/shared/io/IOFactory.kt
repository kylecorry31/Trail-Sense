package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.gpx.GPXData

class IOFactory {

    fun createGpxService(fragment: AndromedaFragment): IOService<GPXData> {
        return GpxIOService(
            FragmentUriPicker(fragment),
            ExternalUriService(fragment.requireContext())
        )
    }

}