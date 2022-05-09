package com.kylecorry.trail_sense.shared.grouping.sort

import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper

class GroupSort<T : Groupable, Value : Comparable<Value>>(
    private val mapper: ISuspendMapper<T, Value>,
    private val ascending: Boolean = true
) {

    suspend fun sort(items: List<T>): List<T> {
        val values = items.map { mapper.map(it) }

        return if (ascending) {
            items.zip(values)
                .sortedBy {
                    it.second
                }
                .map { it.first }
        } else {
            items.zip(values)
                .sortedByDescending {
                    it.second
                }
                .map { it.first }
        }
    }

}