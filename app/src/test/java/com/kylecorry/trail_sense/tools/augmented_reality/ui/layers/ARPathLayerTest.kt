package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class ARPathLayerTest {
    private val layer = ARPathLayer(100f, false)
    private val start = Coordinate(42.0, -72.0)
    private val middle = Coordinate(42.001, -72.0)
    private val end = Coordinate(42.002, -72.0)
    private val path = Path(1, "Route", PathStyle.default().copy(visible = false), mock())
    private val destination = Destination.Path(path, listOf(start, middle, end).mapIndexed { index, point ->
        PathPoint(index.toLong(), path.id, point)
    })

    @Test
    fun hiddenDestinationIsShownAndRouteAdvances() {
        layer.destination = destination
        val initial = layer.getPaths(start).single()
        assertEquals(path.name, initial.name)
        assertEquals(path.style.color, initial.color)
        assertEquals(listOf(start, middle, end), initial.points.map { it.coordinate }.distinct())

        val advanced = layer.getPaths(middle).single()
        assertEquals(listOf(middle, end), advanced.points.map { it.coordinate }.distinct())
    }

    @Test
    fun destinationReplacesSavedPathAndClearingRestoresIt() {
        val saved = MappablePath(path.id, emptyList(), path.style.color, LineStyle.Solid)
        val other = saved.copy(id = 2)
        layer.setPaths(listOf(saved, other))
        layer.destination = destination
        val displayed = layer.getPaths(start)
        assertEquals(listOf(other.id, path.id), displayed.map { it.id })
        assertEquals(other, displayed.first())

        layer.destination = null
        assertEquals(listOf(saved, other), layer.getPaths(start))
    }

    @Test
    fun clearingHiddenDestinationRemovesRoute() {
        layer.destination = destination
        layer.getPaths(start)
        layer.destination = null
        assertEquals(emptyList<MappablePath>(), layer.getPaths(start))
    }
}
