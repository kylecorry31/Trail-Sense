package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.geometry.Geometry

fun <T> Geometry.getConnectedLines(segments: List<Pair<T, T>>): List<List<T>> {
    val startMap = mutableMapOf<T, MutableList<Int>>()
    val endMap = mutableMapOf<T, MutableList<Int>>()

    segments.forEachIndexed { idx, s ->
        startMap.getOrPut(s.first) { mutableListOf() }.add(idx)
        endMap.getOrPut(s.second) { mutableListOf() }.add(idx)
    }

    val visited = BooleanArray(segments.size)
    val lines = mutableListOf<List<T>>()

    for (i in segments.indices) {
        if (visited[i]) continue

        val seg = segments[i]
        val line = mutableListOf<T>()
        line.add(seg.first)
        line.add(seg.second)
        visited[i] = true

        var head = seg.first
        var tail = seg.second

        // Grow the line forward
        while (true) {
            var nextIdx = startMap[tail]?.firstOrNull { !visited[it] }
            val nextSeg = if (nextIdx != null) {
                visited[nextIdx] = true
                segments[nextIdx]
            } else {
                nextIdx = endMap[tail]?.firstOrNull { !visited[it] } ?: break
                visited[nextIdx] = true
                segments[nextIdx].second to segments[nextIdx].first // Reverse the segment
            }

            line.add(nextSeg.second)
            tail = nextSeg.second
        }

        // Grow the line backward
        while (true) {
            var prevIdx = endMap[head]?.firstOrNull { !visited[it] }
            val prevSeg = if (prevIdx != null) {
                visited[prevIdx] = true
                segments[prevIdx]
            } else {
                prevIdx = startMap[head]?.firstOrNull { !visited[it] } ?: break
                visited[prevIdx] = true
                segments[prevIdx].second to segments[prevIdx].first // Reverse the segment
            }
            line.add(0, prevSeg.first)
            head = prevSeg.first
        }

        lines.add(line)
    }

    return lines
}
