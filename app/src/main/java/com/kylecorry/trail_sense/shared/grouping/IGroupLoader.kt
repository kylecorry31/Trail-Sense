package com.kylecorry.trail_sense.shared.grouping

interface IGroupLoader<T : Groupable> {
    suspend fun load(id: Long?, maxDepth: Int? = null): List<T>
}