package com.kylecorry.trail_sense.shared.grouping.count

import com.kylecorry.trail_sense.shared.grouping.persistence.GroupLoader

class GroupCounter(private val loader: GroupLoader<*>) : IGroupCounter {
    override suspend fun count(groupId: Long?): Int {
        return loader.getChildren(groupId, null).filterNot { it.isGroup }.count()
    }
}