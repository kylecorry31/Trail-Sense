package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tag
import org.mapsforge.map.datastore.Way

internal class ConnectedWayClusterFinderTest {

    private fun way(vararg coords: Pair<Double, Double>): Way {
        val latLongs = arrayOf(coords.map { LatLong(it.first, it.second) }.toTypedArray())
        return Way(0, listOf(Tag("name", "test")), latLongs, null)
    }

    @Test
    fun emptyListReturnsSingleEmptyCluster() {
        val clusters = ConnectedWayClusterFinder.findClusters(emptyList())
        assertEquals(1, clusters.size)
        assertEquals(0, clusters[0].size)
    }

    @Test
    fun singleWayReturnsSingleCluster() {
        val w = way(0.0 to 0.0, 0.0 to 1.0, 1.0 to 1.0, 1.0 to 0.0)
        val clusters = ConnectedWayClusterFinder.findClusters(listOf(w))
        assertEquals(1, clusters.size)
        assertEquals(listOf(w), clusters[0])
    }

    @Test
    fun nonOverlappingWaysFormSeparateClusters() {
        val w1 = way(0.0 to 0.0, 0.0 to 1.0, 1.0 to 1.0, 1.0 to 0.0)
        val w2 = way(10.0 to 10.0, 10.0 to 11.0, 11.0 to 11.0, 11.0 to 10.0)
        val clusters = ConnectedWayClusterFinder.findClusters(listOf(w1, w2))
        assertEquals(2, clusters.size)
        val clusterContents = clusters.map { it.toSet() }.toSet()
        assertEquals(setOf(setOf(w1), setOf(w2)), clusterContents)
    }

    @Test
    fun overlappingWaysFormOneCluster() {
        val w1 = way(0.0 to 0.0, 0.0 to 2.0, 2.0 to 2.0, 2.0 to 0.0)
        val w2 = way(1.0 to 1.0, 1.0 to 3.0, 3.0 to 3.0, 3.0 to 1.0)
        val clusters = ConnectedWayClusterFinder.findClusters(listOf(w1, w2))
        assertEquals(1, clusters.size)
        assertEquals(setOf(w1, w2), clusters[0].toSet())
    }

    @Test
    fun transitivelyConnectedWaysFormOneCluster() {
        // w1 overlaps w2, w2 overlaps w3, but w1 does not overlap w3
        val w1 = way(0.0 to 0.0, 0.0 to 2.0, 2.0 to 2.0, 2.0 to 0.0)
        val w2 = way(1.0 to 1.0, 1.0 to 4.0, 4.0 to 4.0, 4.0 to 1.0)
        val w3 = way(3.0 to 3.0, 3.0 to 6.0, 6.0 to 6.0, 6.0 to 3.0)
        val clusters = ConnectedWayClusterFinder.findClusters(listOf(w1, w2, w3))
        assertEquals(1, clusters.size)
        assertEquals(setOf(w1, w2, w3), clusters[0].toSet())
    }

    @Test
    fun mixedOverlapProducesTwoClusters() {
        val w1 = way(0.0 to 0.0, 0.0 to 2.0, 2.0 to 2.0, 2.0 to 0.0)
        val w2 = way(1.0 to 1.0, 1.0 to 3.0, 3.0 to 3.0, 3.0 to 1.0)
        val w3 = way(10.0 to 10.0, 10.0 to 12.0, 12.0 to 12.0, 12.0 to 10.0)
        val clusters = ConnectedWayClusterFinder.findClusters(listOf(w1, w2, w3))
        assertEquals(2, clusters.size)
        val clusterContents = clusters.map { it.toSet() }.toSet()
        assertEquals(setOf(setOf(w1, w2), setOf(w3)), clusterContents)
    }

    @Test
    fun touchingBoundsAreConsideredIntersecting() {
        // w1 ends at lat=2, w2 starts at lat=2 — bounds touch
        val w1 = way(0.0 to 0.0, 0.0 to 1.0, 2.0 to 1.0, 2.0 to 0.0)
        val w2 = way(2.0 to 0.0, 2.0 to 1.0, 4.0 to 1.0, 4.0 to 0.0)
        val clusters = ConnectedWayClusterFinder.findClusters(listOf(w1, w2))
        assertEquals(1, clusters.size)
        assertEquals(setOf(w1, w2), clusters[0].toSet())
    }
}
