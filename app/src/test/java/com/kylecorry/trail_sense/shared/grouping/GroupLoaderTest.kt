package com.kylecorry.trail_sense.shared.grouping

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class GroupLoaderTest {

    @ParameterizedTest
    @MethodSource("provideLoad")
    fun load(root: Long?, depth: Int?, expected: List<Groupable>) = runBlocking {
        // Arrange
        val groups = mapOf(
            0L to group(0),
            1L to group(1),
            2L to group(2),
            3L to group(3),
            null to null
        )

        val children = mapOf(
            null to listOf(item(0), group(0), group(1)),
            0L to listOf(item(1), group(2)),
            1L to listOf(item(2)),
            2L to listOf(group(3)),
            3L to emptyList(),
        )

        val loader = GroupLoader(groups::get, children::getValue)

        // Act
        val actual = loader.load(root, depth)

        // Assert
        assertEquals(expected, actual)
    }

    companion object {

        private fun item(id: Long): Groupable {
            return MockGroup(id, false)
        }

        private fun group(id: Long): Groupable {
            return MockGroup(id, true)
        }

        @JvmStatic
        fun provideLoad(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, 0, emptyList<Groupable>()),
                Arguments.of(null, 1, listOf(item(0), group(0), group(1))),
                Arguments.of(
                    null,
                    2,
                    listOf(item(0), group(0), group(1), item(1), group(2), item(2))
                ),
                Arguments.of(
                    null,
                    null,
                    listOf(item(0), group(0), group(1), item(1), group(2), group(3), item(2))
                ),
                Arguments.of(
                    1L,
                    null,
                    listOf(group(1), item(2))
                ),
                Arguments.of(
                    1L,
                    0,
                    listOf(group(1))
                ),
                Arguments.of(
                    1L,
                    1,
                    listOf(group(1), item(2))
                ),
                Arguments.of(
                    1L,
                    3,
                    listOf(group(1), item(2))
                ),
            )
        }
    }

}