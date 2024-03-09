package com.kylecorry.trail_sense.shared.debugging

import com.kylecorry.trail_sense.shared.safeRoundToInt

object Perf {

    private val startTimes = mutableMapOf<String, Long>()
    private val endTimes = mutableMapOf<String, Long>()

    fun start(tag: String) {
        val startTime = System.nanoTime()
        startTimes[tag] = startTime
    }

    fun end(tag: String) {
        val endTime = System.nanoTime()
        endTimes[tag] = endTime
    }

    inline fun trace(tag: String, block: () -> Unit) {
        start(tag)
        block()
        end(tag)
    }

    fun clear() {
        startTimes.clear()
        endTimes.clear()
    }

    fun print() {
        val parents = startTimes.map { getParent(it.key) }.filterNotNull().distinct()
        val times = startTimes.keys.map { it to getTime(it) }.sortedByDescending { it.second }
        for (parent in parents) {
            val totalParentTime = getTime(parent)?.let { it / 1_000_000.0 }
            println("$parent ($totalParentTime ms)")
            println("-----")
            for (entry in times) {
                val tag = entry.first
                if (getParent(tag) == parent) {
                    val ms = entry.second?.let { it / 1_000_000.0 }
                    val percent = totalParentTime?.let { ms?.div(it) }?.times(100)?.safeRoundToInt()
                    println("${tag}: $ms ms ($percent %)")
                }
            }
            println()
        }

        // Display all times
        for (tag in times.map { it.first }) {
            val ms = getTime(tag)?.let { it / 1_000_000.0 }
            println("${tag}: $ms ms")
        }
    }

    private fun getParent(tag: String): String? {
        val myStartTime = startTimes[tag] ?: return null

        // Get the list of entries at the time the tag started
        return startTimes.entries
            .filter { it.value <= myStartTime }
            .filter { (endTimes[it.key] ?: Long.MAX_VALUE) >= myStartTime }
            .sortedBy { it.value }
            .lastOrNull { it.key != tag }?.key
    }

    private fun getTime(tag: String): Long? {
        val start = startTimes[tag] ?: return null
        val end = endTimes[tag] ?: return null
        return end - start
    }


}