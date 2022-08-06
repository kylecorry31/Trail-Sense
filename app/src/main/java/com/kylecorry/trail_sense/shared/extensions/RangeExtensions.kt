package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.math.Range

fun <T : Comparable<T>> List<T>.range(): Range<T>? {
    val start = minOrNull() ?: return null
    val end = maxOrNull() ?: return null
    return Range(start, end)
}