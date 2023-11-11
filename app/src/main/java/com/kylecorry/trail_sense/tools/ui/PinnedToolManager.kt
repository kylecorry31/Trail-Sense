package com.kylecorry.trail_sense.tools.ui

class PinnedToolManager {

    private val pinned = mutableSetOf<Long>()
    private val lock = Any()

    // TODO: Keep in sync with user prefs

    fun getPinnedToolIds(): List<Long> {
        return synchronized(lock) {
            pinned.toList()
        }
    }

    fun setPinnedToolIds(toolIds: List<Long>) {
        synchronized(lock) {
            pinned.clear()
            pinned.addAll(toolIds)
        }
    }

    fun pin(toolId: Long) {
        synchronized(lock) {
            pinned.add(toolId)
        }
    }

    fun unpin(toolId: Long) {
        synchronized(lock) {
            pinned.remove(toolId)
        }
    }

    fun isPinned(toolId: Long): Boolean {
        return synchronized(lock) {
            pinned.contains(toolId)
        }
    }

}