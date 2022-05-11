package com.kylecorry.trail_sense.shared.grouping.filter

import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader

class GroupFilter<T : Groupable>(private val loader: GroupLoader<T>) : IGroupFilter<T> {

    override suspend fun filter(
        groupId: Long?,
        includeGroups: Boolean,
        maxDepth: Int?,
        predicate: (T) -> Boolean
    ): List<T> {
        val values = loader.getChildren(groupId, maxDepth)
        return values.filter { (includeGroups || !it.isGroup) && predicate(it) }
    }

}