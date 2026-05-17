package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import org.mapsforge.map.datastore.Way

object ConnectedWayClusterFinder {

    /**
     * Groups ways into clusters where any two ways in the same cluster have intersecting bounds.
     * Uses union-find to handle transitive connectivity.
     */
    fun findClusters(ways: List<Way>): List<List<Way>> {
        if (ways.size <= 1) {
            return listOf(ways)
        }

        val parents = IntArray(ways.size) { it }
        val bounds = Array(ways.size) { i -> computeBounds(ways[i]) }

        for (i in ways.indices) {
            for (j in i + 1..<ways.size) {
                if (bounds[i].intersects(bounds[j])) {
                    union(parents, i, j)
                }
            }
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

    private fun union(parents: IntArray, first: Int, second: Int) {
        val firstRoot = find(parents, first)
        val secondRoot = find(parents, second)
        if (firstRoot != secondRoot) {
            parents[secondRoot] = firstRoot
        }
    }
}
