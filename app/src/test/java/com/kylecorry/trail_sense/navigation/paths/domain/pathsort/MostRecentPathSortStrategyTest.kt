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
//internal class MostRecentPathSortStrategyTest {
//
//    @Test
//    fun sort() {
//        val paths = listOf(
//            path(1),
//            path(2),
//            path(3),
//            path(5),
//            path(4),
//        )
//
//        val sort = MostRecentPathSortStrategy()
//
//        val sorted = sort.sort(paths).map { it.id }
//
//        Assertions.assertEquals(listOf(5L, 4L, 3L, 2L, 1L), sorted)
//    }
//
//    private fun path(id: Long): Path {
//        val defaultStyle =
//            PathStyle(LineStyle.Dashed, PathPointColoringStyle.None, Color.BLACK, true)
//        return Path(id, null, defaultStyle, createMetadata())
//    }
//
//    private fun createMetadata(): PathMetadata {
//        return PathMetadata(
//            Distance.meters(10f),
//            10,
//            null,
//            CoordinateBounds.from(Geofence(Coordinate.zero, Distance.meters(100f)))
//        )
//    }
//}