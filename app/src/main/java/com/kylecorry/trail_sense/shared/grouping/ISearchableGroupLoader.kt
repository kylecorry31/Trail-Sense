package com.kylecorry.trail_sense.shared.grouping

interface ISearchableGroupLoader<T : Groupable> {
    suspend fun load(search: String?, group: Long?): List<T>
}