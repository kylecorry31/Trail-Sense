package com.kylecorry.trail_sense.tools.navigation.ui.commands

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.navigation.ui.LocationBottomSheet

class ShowLocationSheetCommand(private val fragment: Fragment, private val gps: ISatelliteGPS) :
    Command {
    override fun execute() {
        val sheet = LocationBottomSheet()
        sheet.gps = gps
        sheet.show(fragment)
    }
}
