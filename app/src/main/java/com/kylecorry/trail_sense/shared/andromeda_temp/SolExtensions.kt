package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.sumOfFloat

fun Vector2.dot(other: Vector2): Float {
    return x * other.x + y * other.y
}

fun Vector2.cross(other: Vector2): Float {
    return x * other.y - y * other.x
}

fun Matrix.dot(vector: Vector2, fill: Float = 1f): Vector2 {
    require(rows() >= 2 && columns() >= 2) { "Matrix must be at least 2x2 to perform the dot product with a vector" }
    val x = this[0, 0] * vector.x + this[0, 1] * vector.y + (2..<columns()).sumOfFloat { this[0, it] * fill }
    val y = this[1, 0] * vector.x + this[1, 1] * vector.y + (2..<columns()).sumOfFloat { this[1, it] * fill }
    return Vector2(x, y)
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
