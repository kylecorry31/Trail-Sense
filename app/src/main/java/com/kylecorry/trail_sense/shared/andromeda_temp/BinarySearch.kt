package com.kylecorry.trail_sense.shared.andromeda_temp

object BinarySearch {

    /**
     * The first index in 0 until [count] where [condition] is true, or [count] if there is none. The condition must be
     * false for every index before that one and true for every index after it (ex. comparing a sorted list to a value).
     */
    inline fun firstIndex(count: Int, condition: (Int) -> Boolean): Int {
        var low = 0
        var high = count
        while (low < high) {
            val middle = (low + high) ushr 1
            if (condition(middle)) {
                high = middle
            } else {
                low = middle + 1
            }
        }
        return low
    }

    /**
     * The index of the first value at or above [value] in [sorted], which must be in ascending order, or its size if
     * there is none.
     */
    fun firstIndexAtOrAbove(sorted: DoubleArray, value: Double): Int {
        return firstIndex(sorted.size) { sorted[it] >= value }
    }
}
