package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath

interface IPathLayer {
    fun setPaths(paths: List<IMappablePath>)
}