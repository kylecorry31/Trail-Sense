package com.kylecorry.trail_sense.navigation.paths.domain

data class FullPath(val path: Path, val points: List<PathPoint>, val parent: PathGroup? = null)