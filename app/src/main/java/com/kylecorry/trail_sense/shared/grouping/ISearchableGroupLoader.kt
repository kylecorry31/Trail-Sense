package com.kylecorry.trail_sense.shared.grouping

interface ISearchableGroupLoader<T : Groupable> {
    suspend fun getGroup(id: Long): T?
    suspend fun load(search: String?, group: Long?): List<T>
}