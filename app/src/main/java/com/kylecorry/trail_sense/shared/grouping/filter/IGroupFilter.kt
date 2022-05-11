package com.kylecorry.trail_sense.shared.grouping.filter

import com.kylecorry.trail_sense.shared.grouping.Groupable

interface IGroupFilter<T : Groupable> {
    suspend fun filter(
        groupId: Long? = null,
        includeGroups: Boolean = false,
        maxDepth: Int? = null,
        predicate: (T) -> Boolean
    ): List<T>
}