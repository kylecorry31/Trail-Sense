package com.kylecorry.trail_sense.tools.navigation.ui.commands

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.navigation.ui.AltitudeBottomSheet
import java.time.Instant

class ShowAltitudeSheetCommand(private val fragment: Fragment, private val altimeter: IAltimeter) :
    Command {
    override fun execute() {
        val sheet = AltitudeBottomSheet()
        sheet.currentAltitude = Reading(altimeter.altitude, Instant.now())
        sheet.show(fragment)
    }
}
