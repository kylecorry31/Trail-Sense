//package com.kylecorry.trail_sense.navigation.paths.domain.pathsort
//
//import android.graphics.Color
//import com.kylecorry.sol.science.geology.CoordinateBounds
//import com.kylecorry.sol.science.geology.Geofence
//import com.kylecorry.sol.units.Coordinate
//import com.kylecorry.sol.units.Distance
//import com.kylecorry.trail_sense.navigation.paths.domain.*
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.Test
//
//internal class LongestPathSortStrategyTest {
//
//    @Test
//    fun sort() {
//        val paths = listOf(
//            path(1, 9f),
//            path(2, 10f),
//            path(3, 8f),
//            path(4, 6f),
//            path(5, 7f),
//        )
//
//        val sort = LongestPathSortStrategy()
//
//        val sorted = sort.sort(paths).map { it.id }
//
//        Assertions.assertEquals(listOf(2L, 1L, 3L, 5L, 4L), sorted)
//    }
//
//    private fun path(id: Long, meters: Float): Path {
//        val defaultStyle =
//            PathStyle(LineStyle.Dashed, PathPointColoringStyle.None, Color.BLACK, true)
//        return Path(id, null, defaultStyle, createMetadata(meters))
//    }
//
//    private fun createMetadata(meters: Float): PathMetadata {
//        return PathMetadata(
//            Distance.meters(meters),
//            10,
//            null,
//            CoordinateBounds.from(Geofence(Coordinate.zero, Distance.meters(100f)))
//        )
//    }
//}