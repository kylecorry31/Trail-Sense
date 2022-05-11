package com.kylecorry.trail_sense.shared.grouping.count

interface IGroupCounter {
    suspend fun count(groupId: Long?): Int
}