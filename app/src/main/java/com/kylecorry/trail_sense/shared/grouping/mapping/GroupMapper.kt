package com.kylecorry.trail_sense.shared.grouping.mapping

import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader

abstract class GroupMapper<T : Groupable, Value, Aggregation> : ISuspendMapper<T, Aggregation> {

    protected abstract val loader: IGroupLoader<T>
    protected abstract suspend fun getValue(item: T): Value
    protected abstract suspend fun aggregate(values: List<Value>): Aggregation


    override suspend fun map(item: T): Aggregation {
        if (!item.isGroup) {
            return aggregate(listOf(getValue(item)))
        }

        val children = loader.getChildren(item.id, null).filterNot { it.isGroup }
        return aggregate(children.map { getValue(it) })
    }

}