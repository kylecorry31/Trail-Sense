package com.kylecorry.trail_sense.shared.grouping.persistence

import com.kylecorry.trail_sense.shared.grouping.Groupable

abstract class GroupDeleter<T : Groupable>(private val loader: GroupLoader<T>) {
    suspend fun delete(group: T) {
        val children = loader.getChildren(group.id, 1)

        // Delete items
        val items = children.filterNot { it.isGroup }
        deleteItems(items)

        // Delete groups
        val groups = children.filter { it.isGroup }
        groups.forEach { delete(it) }

        // Delete self
        deleteGroup(group)
    }

    protected abstract suspend fun deleteItems(items: List<T>)
    protected abstract suspend fun deleteGroup(group: T)
}