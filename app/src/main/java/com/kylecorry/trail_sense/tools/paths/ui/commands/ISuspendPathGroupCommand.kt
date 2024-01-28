package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.tools.paths.domain.PathGroup

interface ISuspendPathGroupCommand {

    suspend fun execute(group: PathGroup)

}