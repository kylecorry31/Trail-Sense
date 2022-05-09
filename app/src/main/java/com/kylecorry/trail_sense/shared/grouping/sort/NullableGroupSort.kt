package com.kylecorry.trail_sense.shared.grouping.sort

import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper

class NullableGroupSort<T : Groupable, Value : Comparable<Value>>(
    private val mapper: ISuspendMapper<T, Value?>,
    private val ascending: Boolean = true,
    private val sortNullsLast: Boolean = true
) {

    suspend fun sort(items: List<T>): List<T> {
        val values = items.map { mapper.map(it) }

        return if (ascending) {
            items.zip(values)
                .sortedWith(compareBy(if (sortNullsLast) nullsLast() else nullsFirst()) {
                    it.second
                })
                .map { it.first }
        } else {
            // Switched nulls first / last since the order is descending
            items.zip(values)
                .sortedWith(compareByDescending(if (sortNullsLast) nullsFirst() else nullsLast()) {
                    it.second
                })
                .map { it.first }
        }
    }

}