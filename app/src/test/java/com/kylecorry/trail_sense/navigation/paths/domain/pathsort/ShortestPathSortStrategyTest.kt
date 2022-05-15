package com.kylecorry.trail_sense.navigation.paths.domain.pathsort

import android.graphics.Color
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.domain.*
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class ShortestPathSortStrategyTest {

    @Test
    fun sort() = runBlocking {

        val pathService = mock<IPathService>()
        val loader = mock<IGroupLoader<IPath>>()

        whenever(loader.getChildren(6, null)).thenReturn(listOf(
            path(4, 6f),
            path(5, 7f),
        ))

        whenever(pathService.loader()).thenReturn(loader)

        val paths = listOf(
            path(1, 9f),
            path(2, 10f),
            group(6),
            path(3, 8f),
        )

        val sort = ShortestPathSortStrategy(pathService)

        val sorted = sort.sort(paths).map { it.id }

        Assertions.assertEquals(listOf(6L, 3L, 1L, 2L), sorted)
    }

    private fun path(id: Long, meters: Float): Path {
        val defaultStyle =
            PathStyle(LineStyle.Dashed, PathPointColoringStyle.None, Color.BLACK, true)
        return Path(id, null, defaultStyle, createMetadata(meters))
    }

    private fun group(id: Long): IPath {
        val group = mock<IPath>()
        whenever(group.id).thenReturn(id)
        whenever(group.isGroup).thenReturn(true)
        return group
    }

    private fun createMetadata(meters: Float): PathMetadata {
        return PathMetadata(
            Distance.meters(meters),
            10,
            null,
            CoordinateBounds.from(Geofence(Coordinate.zero, Distance.meters(100f)))
        )
    }
}