package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Coordinate

data class PathEditState(
    val canUndo: Boolean,
    val canRedo: Boolean,
    val hasSelection: Boolean
)

class PathEditor {

    private val _points = mutableListOf<Coordinate>()
    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    val points: List<Coordinate>
        get() = _points

    var selectedIndex: Int? = null
        private set

    val state: PathEditState
        get() = PathEditState(
            undoStack.isNotEmpty(),
            redoStack.isNotEmpty(),
            selectedIndex != null
        )

    fun add(location: Coordinate) {
        val selected = selectedIndex
        val index = when {
            _points.size < 2 || selected == null || selected == _points.lastIndex -> _points.size
            selected == 0 -> 0
            else -> selected + 1
        }

        if (location == _points.getOrNull(index - 1) || location == _points.getOrNull(index)) {
            return
        }

        insert(index, location)
    }

    private fun insert(index: Int, location: Coordinate) {
        recordUndo()
        _points.add(index, location)
        selectedIndex = index
    }

    fun startMove() {
        recordUndo()
    }

    fun move(index: Int, location: Coordinate) {
        _points[index] = location
        selectedIndex = index
    }

    fun select(index: Int?) {
        selectedIndex = index
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshot())
        restore(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshot())
        restore(next)
    }

    fun clear() {
        _points.clear()
        undoStack.clear()
        redoStack.clear()
        selectedIndex = null
    }

    private fun recordUndo() {
        undoStack.addLast(snapshot())
        redoStack.clear()
    }

    private fun snapshot(): Snapshot {
        return Snapshot(_points.toList(), selectedIndex)
    }

    private fun restore(snapshot: Snapshot) {
        _points.clear()
        _points.addAll(snapshot.points)
        selectedIndex = snapshot.selectedIndex
    }

    private class Snapshot(val points: List<Coordinate>, val selectedIndex: Int?)
}
