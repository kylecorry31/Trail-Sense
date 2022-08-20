package com.kylecorry.trail_sense.shared.debugging

import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.extensions.ifDebug

abstract class DebugCommand : Command {

    protected abstract fun executeDebug()

    override fun execute() {
        ifDebug {
            tryOrLog {
                executeDebug()
            }
        }
    }

}