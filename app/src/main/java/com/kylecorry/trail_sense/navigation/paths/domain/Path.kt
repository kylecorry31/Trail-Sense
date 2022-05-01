package com.kylecorry.trail_sense.navigation.paths.domain

data class Path(
    override val id: Long,
    val name: String?,
    val style: PathStyle,
    val metadata: PathMetadata,
    val temporary: Boolean = false,
    override val parentId: Long? = null
) : IPath {
    override val isGroup = false
    override val count: Int? = null
}