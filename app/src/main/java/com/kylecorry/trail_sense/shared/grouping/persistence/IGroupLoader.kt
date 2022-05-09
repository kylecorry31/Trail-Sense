package com.kylecorry.trail_sense.shared.grouping.persistence

import com.kylecorry.trail_sense.shared.grouping.Groupable

interface IGroupLoader<T : Groupable> {
    suspend fun getChildren(parentId: Long?, maxDepth: Int? = null): List<T>
    suspend fun getGroup(id: Long?): T?
}