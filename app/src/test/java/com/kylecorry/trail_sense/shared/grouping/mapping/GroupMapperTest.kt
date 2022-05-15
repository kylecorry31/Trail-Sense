package com.kylecorry.trail_sense.shared.grouping.mapping

import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class GroupMapperTest {

    @Test
    fun mapGroup() = runBlocking {
        // Assert

        val loader = mock<IGroupLoader<Groupable>>()
        val mapper = MockMapper(loader)

        val children = listOf(
            item(2),
            group(3),
            item(4),
            group(5)
        )

        whenever(loader.getChildren(1, null)).thenReturn(children)

        // Act
        val value = mapper.map(group(1))

        // Assert
        assertEquals(8L, value)
    }

    @Test
    fun mapItem() = runBlocking {
        // Assert
        val loader = mock<IGroupLoader<Groupable>>()
        val mapper = MockMapper(loader)

        // Act
        val value = mapper.map(item(1))

        // Assert
        assertEquals(2L, value)
    }

    private fun item(id: Long): Groupable {
        val item = mock<Groupable>()
        whenever(item.id).thenReturn(id)
        whenever(item.isGroup).thenReturn(false)
        return item
    }

    private fun group(id: Long): Groupable {
        val group = mock<Groupable>()
        whenever(group.id).thenReturn(id)
        whenever(group.isGroup).thenReturn(true)
        return group
    }

    class MockMapper(override val loader: IGroupLoader<Groupable>) :
        GroupMapper<Groupable, Long, Long?>() {
        override suspend fun getValue(item: Groupable): Long {
            return item.id
        }

        override suspend fun aggregate(values: List<Long>): Long? {
            return values.maxOrNull()?.let { it * 2 }
        }
    }

}