package com.kylecorry.trail_sense.shared.grouping.filter

import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader

class GroupFilter<T : Groupable>(private val loader: IGroupLoader<T>) : IGroupFilter<T> {

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