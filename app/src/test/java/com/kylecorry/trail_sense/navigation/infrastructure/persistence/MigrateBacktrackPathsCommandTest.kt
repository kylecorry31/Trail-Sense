package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.infrastructure.IPathService
import com.kylecorry.trail_sense.shared.paths.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

internal class MigrateBacktrackPathsCommandTest {

    private lateinit var prefs: IPathPreferences
    private lateinit var pathService: IPathService
    private lateinit var command: MigrateBacktrackPathsCommand

    @BeforeEach
    fun setup() {
        prefs = mock()
        whenever(prefs.defaultPathStyle).thenReturn(
            PathStyle(
                LineStyle.Dotted,
                PathPointColoringStyle.None,
                0,
                true
            )
        )

        pathService = mock()
        command = MigrateBacktrackPathsCommand(pathService, prefs)
    }

    @Test
    fun worksWhenThereAreNoPaths() = runBlocking {
        whenever(pathService.getWaypoints()).thenReturn(mapOf())

        command.execute()

        verify(pathService, times(1)).endBacktrackPath()
        verify(pathService, times(0)).addPath(any())
        verify(pathService, times(0)).moveWaypointsToPath(any(), any())
    }

    @Test
    fun worksWhenThereIsAnEmptyPath() = runBlocking {
        whenever(pathService.getWaypoints()).thenReturn(mapOf(5L to listOf()))
        whenever(pathService.addPath(any())).thenReturn(1L)

        command.execute()

        val inorder = inOrder(pathService)
        inorder.verify(pathService).endBacktrackPath()
        inorder.verify(pathService).addPath(any())
        inorder.verify(pathService).moveWaypointsToPath(listOf(), 1L)
        inorder.verifyNoMoreInteractions()
    }

    @Test
    fun worksWhenThereIsAPath() = runBlocking {

        val path1 = listOf(
            PathPoint(
                1,
                5,
                Coordinate.zero
            ),
            PathPoint(
                2,
                5,
                Coordinate.zero
            ),
        )

        val expectedPath1 = Path2(
            0L,
            null,
            prefs.defaultPathStyle,
            PathMetadata.empty,
            temporary = true
        )

        whenever(pathService.getWaypoints()).thenReturn(mapOf(5L to path1))
        whenever(pathService.addPath(expectedPath1)).thenReturn(1L)

        command.execute()

        val inorder = inOrder(pathService)
        inorder.verify(pathService).endBacktrackPath()
        inorder.verify(pathService).addPath(expectedPath1)
        inorder.verify(pathService).moveWaypointsToPath(path1.map { it.copy(pathId = 0L) }, 1L)
        inorder.verifyNoMoreInteractions()
    }

    @Test
    fun worksWhenThereAreMultiplePaths() = runBlocking {

        val path1 = listOf(
            PathPoint(
                1,
                5,
                Coordinate.zero
            ),
            PathPoint(
                2,
                5,
                Coordinate.zero
            ),
        )

        val expectedPath1 = Path2(
            0L,
            null,
            prefs.defaultPathStyle,
            PathMetadata.empty,
            temporary = true
        )

        val path2 = listOf(
            PathPoint(
                3,
                6,
                Coordinate.zero
            ),
            PathPoint(
                4,
                6,
                Coordinate.zero
            ),
        )

        val expectedPath2 = Path2(
            0L,
            null,
            prefs.defaultPathStyle,
            PathMetadata.empty,
            temporary = true
        )

        whenever(pathService.getWaypoints()).thenReturn(mapOf(5L to path1, 6L to path2))
        var i = 0L
        whenever(pathService.addPath(any())).then { ++i }

        command.execute()

        val inorder = inOrder(pathService)
        inorder.verify(pathService).endBacktrackPath()
        inorder.verify(pathService).addPath(expectedPath1)
        inorder.verify(pathService).moveWaypointsToPath(path1.map { it.copy(pathId = 0L) }, 1L)
        inorder.verify(pathService).addPath(expectedPath2)
        inorder.verify(pathService).moveWaypointsToPath(path2.map { it.copy(pathId = 0L) }, 2L)
        inorder.verifyNoMoreInteractions()
    }
}