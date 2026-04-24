package com.kylecorry.trail_sense.tools.navigation.ui.commands

import android.os.Bundle
import androidx.navigation.NavController
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class CreateBeaconHereCommand(
    private val gps: IGPS,
    private val altimeter: IAltimeter,
) : Command<NavController> {
    override fun execute(value: NavController) {
        if (gps.hasValidReading) {
            val bundle = Bundle().apply {
                putParcelable(
                    "initial_location", GeoUri(
                        gps.location,
                        if (altimeter.hasValidReading) altimeter.altitude else gps.altitude
                    )
                )
            }
            value.openTool(Tools.BEACONS, bundle)
        } else {
            value.openTool(Tools.BEACONS)
        }
    }
}
