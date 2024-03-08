package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath

/**
 * A layer that displays paths
 */
interface IPathLayer {

    /**
     * Set the paths to display
     */
    fun setPaths(paths: List<IMappablePath>)
}