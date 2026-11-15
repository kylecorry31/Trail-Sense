package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSession
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSessionProgress
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class PathNavigationStoreTest {

    private val store = PathNavigationStore(InMemoryPreferences())
    private val start = Coordinate(10.5, -20.25)

    @Test
    fun `nothing is saved initially`() {
        assertFalse(store.hasSavedRoute())
        assertNull(store.load())
    }

    @ParameterizedTest
    @MethodSource("sessions")
    fun `a saved session is loaded unchanged`(session: PathNavigationSession) {
        store.save(session)

        assertTrue(store.hasSavedRoute())
        assertEquals(session, store.load())
    }

    @Test
    fun `saving replaces the previous session`() {
        store.save(PathNavigationSession(1, PathFollow.Direction(PathDirection.ToEnd), start))
        val second = PathNavigationSession(2, PathFollow.ToPoint(9), start)

        store.save(second)

        assertEquals(second, store.load())
    }

    @Test
    fun `clearing removes the session`() {
        store.save(PathNavigationSession(1, PathFollow.Direction(PathDirection.ToEnd), start))

        store.clear()

        assertFalse(store.hasSavedRoute())
        assertNull(store.load())
    }

    companion object {
        private val origin = Coordinate(10.5, -20.25)

        @JvmStatic
        fun sessions() = listOf(
            PathNavigationSession(1, PathFollow.Direction(PathDirection.ToEnd), origin),
            PathNavigationSession(2, PathFollow.Direction(PathDirection.ToStart), origin),
            PathNavigationSession(3, PathFollow.ToPoint(42), origin),
            PathNavigationSession(
                4,
                PathFollow.Direction(PathDirection.ToEnd),
                origin,
                PathNavigationSessionProgress(Distance.meters(123.5f), Coordinate(11.0, -21.0))
            ),
            PathNavigationSession(
                5,
                PathFollow.ToPoint(7),
                origin,
                PathNavigationSessionProgress(Distance.meters(0f), origin)
            )
        )
    }
}
