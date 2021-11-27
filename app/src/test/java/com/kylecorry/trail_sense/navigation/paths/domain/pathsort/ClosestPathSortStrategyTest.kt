package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import android.graphics.Color
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.domain.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ClosestPathSortStrategyTest {

    @Test
    fun sort() {
        val paths = listOf(
            path(1, Coordinate(1.0, 0.0)),
            path(2, Coordinate(0.0, 0.0)),
            path(3, Coordinate(0.0, 1.0)),
            path(4, Coordinate(1.0, 2.0)),
            path(5, Coordinate(1.0, 1.0)),
        )

        val sort = ClosestPathSortStrategy(Coordinate.zero)

        val sorted = sort.sort(paths).map { it.id }

        assertEquals(listOf(2L, 1L, 3L, 5L, 4L), sorted)
    }

    private fun path(id: Long, center: Coordinate): Path {
        val defaultStyle =
            PathStyle(LineStyle.Dashed, PathPointColoringStyle.None, Color.BLACK, true)
        return Path(id, null, defaultStyle, createMetadata(center))
    }

    private fun createMetadata(center: Coordinate): PathMetadata {
        return PathMetadata(
            Distance.meters(0f),
            10,
            null,
            CoordinateBounds.Companion.from(Geofence(center, Distance.meters(100f)))
        )
    }

}