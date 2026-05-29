package com.kylecorry.trail_sense.shared.andromeda_temp

fun <T> List<T>.combinations(size: Int): List<List<T>> {
    return when {
        size == 0 -> listOf(emptyList())
        this.size < size -> emptyList()
        else -> flatMapIndexed { index, item ->
            drop(index + 1)
                .combinations(size - 1)
                .map { listOf(item) + it }
        }
    }
}
