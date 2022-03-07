package com.kylecorry.trail_sense.navigation.paths.domain

data class PathGroup(
    override val id: Long,
    val name: String,
    override val parentId: Long? = null
) : IPath {
    override val isGroup = true
}
