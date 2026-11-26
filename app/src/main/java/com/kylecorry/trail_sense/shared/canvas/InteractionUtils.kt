package com.kylecorry.trail_sense.shared.canvas

object InteractionUtils {

    const val CLICK_SIZE_DP = 18f

    fun <T> getClickedPoints(
        tap: PixelCircle,
        points: List<Pair<T, PixelCircle>>
    ): List<Pair<T, PixelCircle>> {
        return points
            .filter { it.second.intersects(tap) }
            .reversed()
            .sortedBy {
                if (it.second.contains(tap.center)) {
                    return@sortedBy 0f
                }
                it.second.center.distanceTo(tap.center) - it.second.radius
            }
    }

}
