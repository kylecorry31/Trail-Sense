package com.kylecorry.trail_sense.tools.navigation.ui.commands

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.sharing.Share

class ShareLocationCommand(private val fragment: Fragment, private val gps: IGPS) : Command {
    override fun execute() {
        Share.shareLocation(fragment, gps.location)
    }
}
