package com.kylecorry.trail_sense.navigation.infrastructure.share

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.qr.infrastructure.LocationQREncoder

class LocationQRSender(private val fragment: Fragment) : ILocationSender {
    override fun send(location: Coordinate, format: CoordinateFormat?) {
        CustomUiUtils.showQR(
            fragment,
            fragment.getString(R.string.location),
            LocationQREncoder().encode(location)
        )
    }
}