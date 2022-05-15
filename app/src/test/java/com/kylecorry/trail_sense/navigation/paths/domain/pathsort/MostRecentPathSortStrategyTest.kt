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

internal class MostRecentPathSortStrategyTest {

    @Test
    fun sort() = runBlocking {

        val pathService = mock<IPathService>()
        val loader = mock<IGroupLoader<IPath>>()

        whenever(loader.getChildren(0, null)).thenReturn(listOf(
            path(4),
            path(5),
        ))

        whenever(pathService.loader()).thenReturn(loader)

        val paths = listOf(
            path(1),
            path(2),
            group(0),
            path(3)
        )

        val sort = MostRecentPathSortStrategy(pathService)

        val sorted = sort.sort(paths).map { it.id }

        Assertions.assertEquals(listOf(0L, 3L, 2L, 1L), sorted)
    }

    private fun path(id: Long): Path {
        val defaultStyle =
            PathStyle(LineStyle.Dashed, PathPointColoringStyle.None, Color.BLACK, true)
        return Path(id, null, defaultStyle, createMetadata())
    }

    private fun group(id: Long): IPath {
        val group = mock<IPath>()
        whenever(group.id).thenReturn(id)
        whenever(group.isGroup).thenReturn(true)
        return group
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