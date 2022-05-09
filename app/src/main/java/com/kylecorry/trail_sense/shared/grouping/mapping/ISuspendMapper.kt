package com.kylecorry.trail_sense.shared.grouping.mapping

interface ISuspendMapper<T, Value> {
    suspend fun map(item: T): Value
}