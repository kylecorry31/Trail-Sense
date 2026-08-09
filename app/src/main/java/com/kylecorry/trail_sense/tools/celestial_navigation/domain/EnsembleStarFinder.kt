package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate

class EnsembleStarFinder(
    private vararg val finders: StarFinder,
    private val mergeDistance: Float = 2f,
    private val requireAll: Boolean = false
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val votes = mutableListOf<Pair<PixelCoordinate, Int>>()
        for (star in finders.flatMap { it.findStars(image) }) {
            val vote = votes.filter { it.first.distanceTo(star) < mergeDistance }
                .maxByOrNull { it.second }
            if (vote == null) {
                votes.add(star to 1)
            } else {
                votes.remove(vote)
                votes.add(vote.first to vote.second + 1)
            }
        }

        val minimumVotes = if (requireAll) finders.size else finders.size / 2 + 1
        return votes.filter { it.second >= minimumVotes }.map { it.first }
    }
}
