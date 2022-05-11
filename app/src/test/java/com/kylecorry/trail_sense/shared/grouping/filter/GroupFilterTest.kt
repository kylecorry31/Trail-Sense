package com.kylecorry.trail_sense.shared.grouping.filter

import com.kylecorry.trail_sense.shared.grouping.Groupable
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

internal class GroupFilterTest {

    @ParameterizedTest
    @MethodSource("provideFilter")
    fun filter(items: List<Groupable>, includeGroups: Boolean, expected: List<Groupable>) =
        runBlocking {
            // Arrange
            val loader = mock<IGroupLoader<Groupable>>()
            val filter = GroupFilter(loader)
            whenever(loader.getChildren(1, 2)).thenReturn(items)

            // Act
            val result = filter.filter(1, includeGroups, 2) { it.id % 2 == 1L }

            // Assert
            assertEquals(expected.map { it.id }, result.map { it.id })
            assertEquals(expected.map { it.isGroup }, result.map { it.isGroup })
        }

    companion object {

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

        @JvmStatic
        fun provideFilter(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(listOf<Groupable>(), true, listOf<Groupable>()),
                Arguments.of(listOf(item(1)), true, listOf(item(1))),
                Arguments.of(listOf(item(1), item(2)), true, listOf(item(1))),
                Arguments.of(
                    listOf(item(1), item(2), group(3), group(4)),
                    true,
                    listOf(item(1), group(3))
                ),
                Arguments.of(listOf(group(1)), true, listOf(group(1))),
                Arguments.of(listOf<Groupable>(), false, listOf<Groupable>()),
                Arguments.of(listOf(item(1)), false, listOf(item(1))),
                Arguments.of(listOf(item(1), item(2)), false, listOf(item(1))),
                Arguments.of(listOf(item(1), item(2), group(3), group(4)), false, listOf(item(1))),
                Arguments.of(listOf(group(1)), false, listOf<Groupable>())
            )
        }
    }
}