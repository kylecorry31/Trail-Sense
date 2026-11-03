package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CellTowerIndexTest {

    // Created by cell_tower_index.py, it includes cells in separate tiles, adjacent cells, and the edges of the world
    private val data = "09130505040505050504050504050504040403030406000101cc0a0103df050103d0060306a2020103b4060103402838c03aaab00af5ecab28d02fa0200aaac0"
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private val cells = listOf(
        1 to 1,
        7185 to 42395,
        11520 to 46079,
        16882 to 13855,
        16882 to 13856,
        16885 to 13864,
        18113 to 23024,
        23039 to 23040
    )

    private fun open(): CellTowerIndex = runBlocking {
        CellTowerIndex.open(object : CellTowerIndex.ByteSource {
            override suspend fun read(offset: Int, size: Int): ByteArray {
                return data.copyOfRange(offset, minOf(offset + size, data.size))
            }
        }, 128)
    }

    private fun unpack(cells: List<Long>): List<Pair<Int, Int>> {
        return cells.map { (it shr 32).toInt() to it.toInt() }.sortedWith(compareBy({ it.first }, { it.second }))
    }

    @Test
    fun getsAllCells() = runBlocking {
        val index = open()
        assertEquals(cells, unpack(index.getCells(0, 23040, 0, 46080)))
    }

    @Test
    fun getsCellsInRange() = runBlocking {
        val index = open()
        assertEquals(
            listOf(16882 to 13855, 16882 to 13856),
            unpack(index.getCells(16880, 16883, 13800, 13900))
        )
        assertEquals(emptyList<Pair<Int, Int>>(), unpack(index.getCells(100, 200, 100, 200)))
    }
}
