package com.kylecorry.trail_sense.shared.grouping.count

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

internal class GroupCounterTest {

    @ParameterizedTest
    @MethodSource("provideCount")
    fun count(children: List<Groupable>, expected: Int) = runBlocking {
        // Arrange
        val loader = mock<IGroupLoader<Groupable>>()
        val counter = GroupCounter(loader)
        whenever(loader.getChildren(1, null)).thenReturn(children)

        // Act
        val actual = counter.count(1)

        // Assert
        assertEquals(expected, actual)
    }


    companion object {

        private fun item(): Groupable {
            val item = mock<Groupable>()
            whenever(item.isGroup).thenReturn(false)
            return item
        }

        private fun group(): Groupable {
            val group = mock<Groupable>()
            whenever(group.isGroup).thenReturn(true)
            return group
        }

        @JvmStatic
        fun provideCount(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(listOf<Groupable>(), 0),
                Arguments.of(listOf(item()), 1),
                Arguments.of(listOf(item(), item()), 2),
                Arguments.of(listOf(item(), item(), group()), 2),
                Arguments.of(listOf(group()), 0),
            )
        }
    }

}