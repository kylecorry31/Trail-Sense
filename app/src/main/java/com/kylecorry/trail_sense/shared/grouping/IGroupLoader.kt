package com.kylecorry.trail_sense.shared.grouping

interface IGroupLoader<T : Groupable> {
    suspend fun getChildren(parentId: Long?, maxDepth: Int? = null): List<T>
    suspend fun getGroup(id: Long?): T?
}