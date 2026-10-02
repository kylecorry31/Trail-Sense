package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.ZonedDateTime

class PathGuidanceTargetTest {
    private val start = Coordinate(42.0, -72.0)
    private val request = ARGuidanceRefreshRequest(mock(), start, ZonedDateTime.now())

    @Test
    fun pathTargetAdvancesWithLocation() = runBlocking {
        val middle = Coordinate(42.001, -72.0)
        val end = Coordinate(42.002, -72.0)
        val path = Path(1, "Trail", PathStyle.default(), mock())
        val destination = Destination.Path(path, listOf(start, middle, end).mapIndexed { index, point ->
            PathPoint(index.toLong(), path.id, point)
        })
        val target = PathGuidanceTarget(destination)
        val first = target.refresh(request)
        assertEquals("Trail", first.display.name)
        val firstLocation = (first.point as GeographicARPoint).location
        assertEquals(25f, start.distanceTo(firstLocation), 1f)
        assertEquals(-72.0, firstLocation.longitude, 0.00001)
        val next = target.refresh(request.copy(location = middle))
        val nextLocation = (next.point as GeographicARPoint).location
        assertEquals(25f, middle.distanceTo(nextLocation), 1f)
        assertTrue(nextLocation.latitude > middle.latitude)
    }
}
