package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.mapsforge

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import org.mapsforge.map.datastore.Way
import java.util.PriorityQueue

object ConnectedWayClusterFinder {

    /**
     * Groups ways into clusters where any two ways in the same cluster have intersecting bounds.
     * Uses a latitude sweep to discard ways that cannot intersect before checking longitude, and
     * union-find to handle transitive connectivity.
     */
    fun findClusters(ways: List<Way>): List<List<Way>> {
        if (ways.size <= 1) {
            return listOf(ways)
        }

        val bounds = Array(ways.size) { i -> computeBounds(ways[i]) }
        val parents = IntArray(ways.size) { it }
        val sizes = IntArray(ways.size) { 1 }
        val waysBySouth = ways.indices.sortedBy { bounds[it].south }
        val activeWays = mutableSetOf<Int>()
        val activeWaysByNorth = PriorityQueue(compareBy<Int> { bounds[it].north })

        for (index in waysBySouth) {
            while (activeWaysByNorth.peek()?.let { bounds[it].north < bounds[index].south } == true) {
                activeWays.remove(activeWaysByNorth.remove())
            }

            for (activeIndex in activeWays) {
                if (bounds[index].intersects(bounds[activeIndex])) {
                    union(parents, sizes, index, activeIndex)
                }
            }

            activeWays.add(index)
            activeWaysByNorth.add(index)
        }

        return ways.indices
            .groupBy { find(parents, it) }
            .values
            .map { component -> component.map { ways[it] } }
            .toList()
    }

    private fun computeBounds(way: Way): CoordinateBounds {
        return CoordinateBounds.from(way.latLongs.flatMap { ring ->
            ring.map { Coordinate(it.latitude, it.longitude) }
        })
    }

    private fun find(parents: IntArray, index: Int): Int {
        if (parents[index] != index) {
            parents[index] = find(parents, parents[index])
        }
        return parents[index]
    }

    private fun union(parents: IntArray, sizes: IntArray, first: Int, second: Int) {
        var firstRoot = find(parents, first)
        var secondRoot = find(parents, second)
        if (firstRoot != secondRoot) {
            if (sizes[firstRoot] < sizes[secondRoot]) {
                val temporary = firstRoot
                firstRoot = secondRoot
                secondRoot = temporary
            }
            parents[secondRoot] = firstRoot
            sizes[firstRoot] += sizes[secondRoot]
        }
    }
}
