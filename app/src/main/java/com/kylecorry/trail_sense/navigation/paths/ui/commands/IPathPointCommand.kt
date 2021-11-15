package com.kylecorry.trail_sense.navigation.paths.ui.commands

import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPoint

interface IPathPointCommand {
    fun execute(path: Path, point: PathPoint)
}