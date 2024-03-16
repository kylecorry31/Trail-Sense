package com.kylecorry.trail_sense.shared.canvas

object InteractionUtils {

    const val CLICK_SIZE_DP = 18f

    /**
     * Gets the points that were clicked, sorted by the closest to the center of the tap
     * @param tap the position of the tap
     * @param points the points to check
     * @return the points that were clicked
     */
    fun <T> getClickedPoints(
        tap: PixelCircle,
        points: List<Pair<T, PixelCircle>>
    ): List<Pair<T, PixelCircle>> {
        return points
            .filter { it.second.intersects(tap) }
            .reversed()
            .sortedBy {
                if (it.second.contains(tap.center)) {
                    // The point is centered
                    return@sortedBy 0f
                }
                // The circle does not overlap with the center, so calculate the distance to the nearest point on the circle
                it.second.center.distanceTo(tap.center) - it.second.radius
            }
    }

}