package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.Range

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