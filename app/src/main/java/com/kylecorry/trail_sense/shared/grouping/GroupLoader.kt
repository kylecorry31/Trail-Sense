package com.kylecorry.trail_sense.shared.grouping

class GroupLoader<T : Groupable>(
    private val groupLoader: suspend (id: Long?) -> T?,
    private val childLoader: suspend (id: Long?) -> List<T>
) : IGroupLoader<T> {

    override suspend fun getChildren(parentId: Long?, maxDepth: Int?): List<T> {
        return loadChildren(parentId, maxDepth)
    }

    override suspend fun getGroup(id: Long?): T? {
        return groupLoader.invoke(id)
    }

    private suspend fun loadChildren(id: Long?, maxDepth: Int?): List<T> {
        if (maxDepth != null && maxDepth <= 0) {
            return emptyList()
        }

        val children = childLoader.invoke(id)
        val newDepth = if (maxDepth == null) {
            null
        } else {
            maxDepth - 1
        }
        val subchildren = children.filter { it.isGroup }.flatMap { loadChildren(it.id, newDepth) }
        return children + subchildren
    }
}