package com.kylecorry.trail_sense.tools.navigation.ui.commands

import androidx.navigation.NavController
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class OpenBeaconsCommand : Command<NavController> {
    override fun execute(value: NavController) {
        value.openTool(Tools.BEACONS)
    }
}
