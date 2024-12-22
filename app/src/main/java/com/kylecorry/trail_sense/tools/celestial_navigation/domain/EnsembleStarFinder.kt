package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.graphics.Bitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate

class EnsembleStarFinder(
    private vararg val finders: StarFinder,
    private val mergeDistance: Float = 2f,
    private val requireAll: Boolean = false
) : StarFinder {
    override fun findStars(image: Bitmap): List<PixelCoordinate> {
        val stars = finders.flatMap { it.findStars(image) }

        val votes = mutableListOf<Pair<PixelCoordinate, Int>>()
        for (star in stars) {
            val closeVotes = votes.filter { it.first.distanceTo(star) < mergeDistance }
            if (closeVotes.isEmpty()) {
                votes.add(star to 1)
            } else {
                val vote = closeVotes.maxByOrNull { it.second }!!
                votes.remove(vote)
                votes.add(vote.first to vote.second + 1)
            }
        }

        // Only accept the stars that have majority votes
        val minVotes = if (requireAll) finders.size else (finders.size / 2 + 1)
        return votes.filter { it.second > minVotes }.map { it.first }
    }
}