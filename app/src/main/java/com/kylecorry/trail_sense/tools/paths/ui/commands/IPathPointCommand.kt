package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

interface IPathPointCommand {
    fun execute(path: Path, point: PathPoint)
}