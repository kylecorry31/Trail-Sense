package com.kylecorry.trail_sense.shared.grouping

class GroupLoader<T : Groupable>(
    private val rootLoader: suspend (id: Long?) -> T?,
    private val childLoader: suspend (id: Long?) -> List<T>,
    private val maxDepth: Int? = null
) : IGroupLoader<T> {
    override suspend fun load(id: Long?): List<T> {
        val root = listOfNotNull(rootLoader.invoke(id))
        val children = loadChildren(id, maxDepth)
        return root + children
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