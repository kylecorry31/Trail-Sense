package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PathEditorTest {

    private val a = Coordinate(1.0, 1.0)
    private val b = Coordinate(2.0, 2.0)
    private val c = Coordinate(3.0, 3.0)
    private val d = Coordinate(4.0, 4.0)

    private fun editorWith(points: List<Coordinate>): PathEditor {
        return PathEditor().apply { points.forEach { add(it) } }
    }

    @Test
    fun addAppendsAndSelectsTheNewPoint() {
        val editor = editorWith(listOf(a, b))

        assertEquals(listOf(a, b), editor.points)
        assertEquals(1, editor.selectedIndex)
    }

    @Test
    fun addIgnoresTheLastPoint() {
        val editor = editorWith(listOf(a, b))

        editor.add(b)

        assertEquals(listOf(a, b), editor.points)
    }

    @Test
    fun addIgnoresTheSelectedMiddlePoint() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(1)

        editor.add(b)

        assertEquals(listOf(a, b, c), editor.points)
        assertFalse(editor.state.canRedo)
    }

    @Test
    fun addIgnoresTheSelectedFirstPoint() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(0)

        editor.add(a)

        assertEquals(listOf(a, b, c), editor.points)
    }

    @Test
    fun addIgnoresThePointAfterTheSelectedPoint() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(1)

        editor.add(c)

        assertEquals(listOf(a, b, c), editor.points)
    }

    @Test
    fun addAllowsReturningToEarlierPoint() {
        val editor = editorWith(listOf(a, b, a))

        assertEquals(listOf(a, b, a), editor.points)
    }

    @Test
    fun addAppendsWhenNothingIsSelected() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(null)

        editor.add(d)

        assertEquals(listOf(a, b, c, d), editor.points)
    }

    @Test
    fun addPrependsWhenFirstPointIsSelected() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(0)

        editor.add(d)

        assertEquals(listOf(d, a, b, c), editor.points)
        assertEquals(0, editor.selectedIndex)
    }

    @Test
    fun addInsertsAfterSelectedMiddlePoint() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(1)

        editor.add(d)

        assertEquals(listOf(a, b, d, c), editor.points)
        assertEquals(2, editor.selectedIndex)
    }

    @Test
    fun addAppendsWhenOnlyOnePointExists() {
        val editor = editorWith(listOf(a))
        editor.select(0)

        editor.add(b)

        assertEquals(listOf(a, b), editor.points)
    }

    @Test
    fun moveReplacesAndSelectsThePoint() {
        val editor = editorWith(listOf(a, b, c))

        editor.startMove()
        editor.move(0, d)

        assertEquals(listOf(d, b, c), editor.points)
        assertEquals(0, editor.selectedIndex)
    }

    @Test
    fun undoRevertsAMoveInOneStep() {
        val editor = editorWith(listOf(a, b))
        editor.startMove()
        editor.move(0, c)
        editor.move(0, d)

        editor.undo()

        assertEquals(listOf(a, b), editor.points)
    }

    @Test
    fun undoRestoresTheSelectionFromBeforeTheChange() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(1)
        editor.add(d)

        editor.undo()

        assertEquals(listOf(a, b, c), editor.points)
        assertEquals(1, editor.selectedIndex)
    }

    @Test
    fun redoReappliesTheChangeAndItsSelection() {
        val editor = editorWith(listOf(a, b, c))
        editor.select(1)
        editor.add(d)
        editor.undo()

        editor.redo()

        assertEquals(listOf(a, b, d, c), editor.points)
        assertEquals(2, editor.selectedIndex)
    }

    @Test
    fun aNewChangeClearsRedo() {
        val editor = editorWith(listOf(a, b))
        editor.undo()

        editor.add(c)

        assertFalse(editor.state.canRedo)
    }

    @Test
    fun undoAndRedoDoNothingWithoutHistory() {
        val editor = PathEditor()

        editor.undo()
        editor.redo()

        assertTrue(editor.points.isEmpty())
        assertFalse(editor.state.canUndo)
        assertFalse(editor.state.canRedo)
    }

    @Test
    fun selectingIsNotUndoable() {
        val editor = editorWith(listOf(a, b))
        editor.undo()

        editor.select(0)

        assertTrue(editor.state.canRedo)
    }

    @Test
    fun clearRemovesPointsSelectionAndHistory() {
        val editor = editorWith(listOf(a, b))
        editor.undo()

        editor.clear()

        assertTrue(editor.points.isEmpty())
        assertNull(editor.selectedIndex)
        assertEquals(PathEditState(canUndo = false, canRedo = false, hasSelection = false), editor.state)
    }
}
