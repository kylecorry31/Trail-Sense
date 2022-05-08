package com.kylecorry.trail_sense.shared.grouping

class GroupMapper<T : Groupable, Value>(
    private val loader: GroupLoader<T>,
    private val map: suspend (item: T) -> Value
) {

    suspend fun get(item: T): List<Value> {
        if (!item.isGroup) {
            return listOf(map(item))
        }

        val children = loader.getChildren(item.id, null).filterNot { it.isGroup }
        return children.map { map(it) }
    }

}