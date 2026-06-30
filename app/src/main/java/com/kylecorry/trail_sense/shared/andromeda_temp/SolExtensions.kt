package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2

fun Vector2.dot(other: Vector2): Float {
    return x * other.x + y * other.y
}

fun Vector2.cross(other: Vector2): Float {
    return x * other.y - y * other.x
}

fun <T : Comparable<T>> List<Range<T>>.mergeIntersecting(): List<Range<T>> {
    val newRanges = mutableListOf<Range<T>>()
    for (range in this) {
        if (newRanges.isEmpty()) {
            newRanges.add(range)
        } else {
            val previous = newRanges.last()
            if (previous.contains(range.start)) {
                newRanges.removeAt(newRanges.lastIndex)
                newRanges.add(Range(previous.start, range.end))
            } else {
                newRanges.add(range)
            }
        }
    }
    return newRanges
}
