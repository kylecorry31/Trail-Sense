package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import android.graphics.Color
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class NamePathSortStrategyTest {

    @Test
    fun sort() = runBlocking {
        val paths = listOf(
            path(1, "bc"),
            path(2, "abc"),
            path(3, "c"),
            group(6, "g"),
            group(7, "ab"),
            path(4, null),
            path(5, null),
        )

        val sort = NamePathSortStrategy()

        val sorted = sort.sort(paths).map { it.id }

        Assertions.assertEquals(listOf(7L, 2L, 1L, 3L, 6L, 4L, 5L), sorted)
    }

    private fun path(id: Long, name: String?): Path {
        val defaultStyle =
            PathStyle(LineStyle.Dashed, PathPointColoringStyle.None, Color.BLACK, true)
        return Path(id, name, defaultStyle, createMetadata())
    }

    private fun group(id: Long, name: String): IPath {
        return PathGroup(id, name)
    }

    private fun createMetadata(): PathMetadata {
        return PathMetadata(
            Distance.meters(10f),
            10,
            null,
            CoordinateBounds.from(Geofence(Coordinate.zero, Distance.meters(100f)))
        )
    }
}