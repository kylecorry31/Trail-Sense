package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import android.content.Context
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

class BearingGuidanceTargetTest {
    private val context = mock<Context> {
        whenever(it.getString(R.string.bearing)).thenReturn("Bearing")
    }
    private val start = Coordinate(42.0, -72.0)
    private val request = ARGuidanceRefreshRequest(context, start, ZonedDateTime.now())

    @Test
    fun lockedBearingUsesGeographicDestination() = runBlocking {
        val destination = Destination.Bearing(Bearing.from(90f), false, 15f, start)
        val state = BearingGuidanceTarget(destination, true).refresh(request)
        assertEquals(destination.targetLocation, (state.point as GeographicARPoint).location)
    }

    @Test
    fun unlockedBearingPreservesNorthReference() = runBlocking {
        for (trueNorth in listOf(true, false)) {
            val destination = Destination.Bearing(Bearing.from(90f), trueNorth, 15f, start)
            val state = BearingGuidanceTarget(destination, false).refresh(request)
            val coordinate = (state.point as SphericalARPoint).coordinate
            assertEquals(90f, coordinate.bearing, 0.001f)
            assertEquals(trueNorth, coordinate.isTrueNorth)
        }
    }

    @Test
    fun bearingWithoutLocationFallsBackToDirection() = runBlocking {
        val destination = Destination.Bearing(Bearing.from(45f), true, 0f)
        val state = BearingGuidanceTarget(destination, true).refresh(request)
        assertEquals(45f, (state.point as SphericalARPoint).coordinate.bearing, 0.001f)
    }
}
