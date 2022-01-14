package com.kylecorry.trail_sense.navigation.paths.ui.commands

import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

interface IPathPointCommand {
    fun execute(path: Path, point: PathPoint)
}