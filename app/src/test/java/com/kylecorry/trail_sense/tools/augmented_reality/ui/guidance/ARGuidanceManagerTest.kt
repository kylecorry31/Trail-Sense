package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.sol.units.Coordinate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.ZonedDateTime

class ARGuidanceManagerTest {

    private val request = ARGuidanceRefreshRequest(
        mock(), Coordinate(42.0, -72.0), ZonedDateTime.parse("2026-09-26T12:00:00Z")
    )

    @Test
    fun startsEmptyAndRefreshWithoutTargetDoesNothing() = runBlocking {
        val manager = ARGuidanceManager()
        manager.updateTargetState(request)
        assertNull(manager.target.value)
        assertNull(manager.targetState.value)
    }

    @Test
    fun refreshPassesRequestAndPublishesLatestState() = runBlocking {
        val manager = ARGuidanceManager()
        var state = state("First")
        val target = object : ARGuidanceTarget {
            override suspend fun refresh(request: ARGuidanceRefreshRequest): ARGuidanceTargetState {
                assertSame(this@ARGuidanceManagerTest.request, request)
                return state
            }
        }
        manager.setTarget(target)
        manager.updateTargetState(request)
        assertSame(target, manager.target.value)
        awaitState(manager, state)

        state = state("Updated")
        manager.updateTargetState(request)
        awaitState(manager, state)
    }

    @Test
    fun replacingOrClearingTargetImmediatelyClearsState() = runBlocking {
        val manager = ARGuidanceManager()
        val first = state("First")
        manager.setTarget(target(first))
        manager.updateTargetState(request)
        awaitState(manager, first)

        val replacement = target(state("Second"))
        manager.setTarget(replacement)
        assertSame(replacement, manager.target.value)
        assertNull(manager.targetState.value)

        manager.updateTargetState(request)
        awaitState(manager, replacement.refresh(request))
        manager.setTarget(null)
        assertNull(manager.target.value)
        assertNull(manager.targetState.value)
    }

    @Test
    fun selectingSameTargetPreservesState() = runBlocking {
        val manager = ARGuidanceManager()
        val state = state("Target")
        val target = target(state)
        manager.setTarget(target)
        manager.updateTargetState(request)
        awaitState(manager, state)
        manager.setTarget(target)
        assertSame(state, manager.targetState.value)
    }

    @Test
    fun equivalentTargetPreservesOriginalTargetAndState() = runBlocking {
        val manager = ARGuidanceManager()
        val state = state("Target")
        val target = FixedTarget(state)
        manager.setTarget(target)
        manager.updateTargetState(request)
        awaitState(manager, state)

        manager.setTarget(target.copy())
        assertSame(target, manager.target.value)
        assertSame(state, manager.targetState.value)
    }

    @Test
    fun ignoresRefreshThatCompletesAfterReplacement() = runBlocking {
        assertStaleRefreshIgnored(target(state("Replacement")))
    }

    @Test
    fun ignoresRefreshThatCompletesAfterCancellation() = runBlocking {
        assertStaleRefreshIgnored(null)
    }

    private suspend fun assertStaleRefreshIgnored(replacement: ARGuidanceTarget?) = withTimeout(5000) {
        val manager = ARGuidanceManager()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val nextRefreshState = CompletableDeferred<ARGuidanceTargetState?>()
        var refreshCount = 0
        manager.setTarget(object : ARGuidanceTarget {
            override suspend fun refresh(request: ARGuidanceRefreshRequest): ARGuidanceTargetState {
                refreshCount++
                if (refreshCount == 1) {
                    started.complete(Unit)
                    release.await()
                } else {
                    nextRefreshState.complete(manager.targetState.value)
                }
                return state("Stale")
            }
        })
        manager.updateTargetState(request)
        started.await()
        // Queue another refresh to observe state after the in-flight refresh finishes.
        manager.updateTargetState(request)
        manager.setTarget(replacement)
        release.complete(Unit)
        assertNull(nextRefreshState.await())
        assertSame(replacement, manager.target.value)
        assertNull(manager.targetState.value)

        if (replacement != null) {
            manager.updateTargetState(request)
            awaitState(manager, replacement.refresh(request))
        }
    }

    private suspend fun awaitState(manager: ARGuidanceManager, state: ARGuidanceTargetState) {
        withTimeout(5000) { manager.targetState.first { it === state } }
    }

    private fun state(name: String) = ARGuidanceTargetState(ARGuidanceDisplayState(name, 0), mock())

    private fun target(state: ARGuidanceTargetState) = object : ARGuidanceTarget {
        override suspend fun refresh(request: ARGuidanceRefreshRequest) = state
    }

    private data class FixedTarget(val state: ARGuidanceTargetState) : ARGuidanceTarget {
        override suspend fun refresh(request: ARGuidanceRefreshRequest) = state
    }
}
