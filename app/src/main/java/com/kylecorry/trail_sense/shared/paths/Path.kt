package com.kylecorry.trail_sense.shared.paths

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.database.Identifiable

data class Path(
    val id: Long,
    val name: String,
    val points: List<PathPoint>,
    @ColorInt val color: Int,
    val style: LineStyle = LineStyle.Solid,
    val temporary: Boolean = false,
    val owner: PathOwner = PathOwner.User
)

data class Path2(
    override val id: Long,
    val name: String,
    val points: List<PathPoint>,
    val style: PathStyle,
    val temporary: Boolean = false,
    val owner: PathOwner = PathOwner.User
) : Identifiable