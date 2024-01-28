package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.tools.paths.domain.Path

interface IPathCommand {

    fun execute(path: Path)

}