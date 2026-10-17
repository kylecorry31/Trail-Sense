package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PathDirectionRecommenderTest {

    private val start = Coordinate(0.0, 0.0)
    private val end = Coordinate(0.0, 1.0)

    @Test
    fun recommendsToEndWhenNearerTheStart() {
        val direction = recommend(location = Coordinate(0.0, 0.1))

        assertEquals(PathDirection.ToEnd, direction)
    }

    @Test
    fun recommendsToStartWhenNearerTheEnd() {
        val direction = recommend(location = Coordinate(0.0, 0.9))

        assertEquals(PathDirection.ToStart, direction)
    }

    @Test
    fun recommendsToStartWhileRecordingEvenIfNearerTheStart() {
        val direction = recommend(location = Coordinate(0.0, 0.1), isRecording = true)

        assertEquals(PathDirection.ToStart, direction)
    }

    @Test
    fun hasNoRecommendationForLoops() {
        assertNull(recommend(location = Coordinate(0.0, 0.9), isLoop = true))
        assertNull(recommend(location = Coordinate(0.0, 0.9), isRecording = true, isLoop = true))
    }

    @Test
    fun hasNoRecommendationWhenEquidistant() {
        assertNull(recommend(location = Coordinate(0.0, 0.5)))
    }

    private fun recommend(
        location: Coordinate,
        isRecording: Boolean = false,
        isLoop: Boolean = false
    ): PathDirection? {
        return PathDirectionRecommender.recommend(start, end, location, isRecording, isLoop)
    }
}
