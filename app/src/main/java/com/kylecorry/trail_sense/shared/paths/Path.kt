package com.kylecorry.trail_sense.shared.paths

import androidx.annotation.ColorInt

data class Path(
    val id: Long,
    val name: String,
    val points: List<PathPoint>,
    @ColorInt val color: Int,
    val style: PathStyle = PathStyle.Solid,
    val temporary: Boolean = false,
    val owner: PathOwner = PathOwner.User
)