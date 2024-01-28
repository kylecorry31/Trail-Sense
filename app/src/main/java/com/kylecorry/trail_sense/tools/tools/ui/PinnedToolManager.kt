package com.kylecorry.trail_sense.tools.tools.ui

import com.kylecorry.trail_sense.shared.UserPreferences

class PinnedToolManager(private val prefs: UserPreferences) {

    private val pinned = mutableSetOf<Long>()
    private val lock = Any()

    init {
        val all = prefs.toolPinnedIds
        synchronized(lock) {
            pinned.clear()
            pinned.addAll(all)
        }
    }

    private fun getPinnedToolIds(): List<Long> {
        return synchronized(lock) {
            pinned.toList()
        }
    }

    fun setPinnedToolIds(toolIds: List<Long>) {
        synchronized(lock) {
            pinned.clear()
            pinned.addAll(toolIds)
        }
        prefs.toolPinnedIds = getPinnedToolIds()
    }

    fun pin(toolId: Long) {
        synchronized(lock) {
            pinned.add(toolId)
        }
        prefs.toolPinnedIds = getPinnedToolIds()
    }

    fun unpin(toolId: Long) {
        synchronized(lock) {
            pinned.remove(toolId)
        }
        prefs.toolPinnedIds = getPinnedToolIds()
    }

    fun isPinned(toolId: Long): Boolean {
        return synchronized(lock) {
            pinned.contains(toolId)
        }
    }
}