package com.kylecorry.trail_sense.tools.ui

import com.kylecorry.andromeda.preferences.IPreferences

class PinnedToolManager(private val prefs: IPreferences) {

    private val pinned = mutableSetOf<Long>()
    private val lock = Any()

    private val key = "pinned_tools"

    init {
        // TODO: Listen for changes
        val all = readPrefs()
        synchronized(lock) {
            pinned.clear()
            pinned.addAll(all)
        }
    }

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
        writePrefs(getPinnedToolIds())
    }

    fun pin(toolId: Long) {
        synchronized(lock) {
            pinned.add(toolId)
        }
        writePrefs(getPinnedToolIds())
    }

    fun unpin(toolId: Long) {
        synchronized(lock) {
            pinned.remove(toolId)
        }
        writePrefs(getPinnedToolIds())
    }

    fun isPinned(toolId: Long): Boolean {
        return synchronized(lock) {
            pinned.contains(toolId)
        }
    }

    private fun readPrefs(): List<Long> {
        val str = prefs.getString(key) ?: return listOf(
            6L, // Navigation
            20L, // Weather
            14L // Astronomy
        )
        return str.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun writePrefs(toolIds: List<Long>) {
        prefs.putString(key, toolIds.joinToString(","))
    }

}