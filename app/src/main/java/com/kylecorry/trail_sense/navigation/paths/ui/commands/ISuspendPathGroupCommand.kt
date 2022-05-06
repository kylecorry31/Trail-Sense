package com.kylecorry.trail_sense.navigation.paths.ui.commands

import com.kylecorry.trail_sense.navigation.paths.domain.PathGroup

interface ISuspendPathGroupCommand {

    suspend fun execute(group: PathGroup)

}