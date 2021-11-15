package com.kylecorry.trail_sense.navigation.paths.ui.commands

import com.kylecorry.trail_sense.shared.paths.Path

interface IPathCommand {

    fun execute(path: Path)

}