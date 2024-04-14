package com.kylecorry.trail_sense.shared.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextUtilsTest {

    @Test
    fun getSections() {
        val text = """
            Start
            # Section 1
            This is section 1
            ## Section 1.1
            This is section 1.1
            ## Section 1.2
            This is section 1.2
            # Section 2
            This is section 2
        """.trimIndent()
        val sections = TextUtils.getSections(text)

        assertEquals(
            listOf(
                TextUtils.TextSection(null, null, "Start"),
                TextUtils.TextSection("Section 1", 1, "This is section 1"),
                TextUtils.TextSection("Section 1.1", 2, "This is section 1.1"),
                TextUtils.TextSection("Section 1.2", 2, "This is section 1.2"),
                TextUtils.TextSection("Section 2", 1, "This is section 2")
            ),
            sections
        )
    }

    @Test
    fun groupSectionsLevel1() {
        val sections = listOf(
            TextUtils.TextSection(null, null, "Start"),
            TextUtils.TextSection("Section 1", 1, "This is section 1"),
            TextUtils.TextSection("Section 1.1", 2, "This is section 1.1"),
            TextUtils.TextSection(null, null, "Value"),
            TextUtils.TextSection("Section 1.2", 2, "This is section 1.2"),
            TextUtils.TextSection("Section 2", 1, "This is section 2")
        )

        val grouped = TextUtils.groupSections(sections, 1)

        assertEquals(
            listOf(
                listOf(
                    TextUtils.TextSection(null, null, "Start"),
                ),
                listOf(
                    TextUtils.TextSection("Section 1", 1, "This is section 1"),
                    TextUtils.TextSection("Section 1.1", 2, "This is section 1.1"),
                    TextUtils.TextSection(null, null, "Value"),
                    TextUtils.TextSection("Section 1.2", 2, "This is section 1.2"),
                ),
                listOf(
                    TextUtils.TextSection("Section 2", 1, "This is section 2")
                )
            ),
            grouped
        )

    }

    @Test
    fun groupSectionsLevel2() {
        val sections = listOf(
            TextUtils.TextSection(null, null, "Start"),
            TextUtils.TextSection("Section 1", 1, "This is section 1"),
            TextUtils.TextSection("Section 1.1", 2, "This is section 1.1"),
            TextUtils.TextSection(null, null, "Value"),
            TextUtils.TextSection("Section 1.2", 2, "This is section 1.2"),
            TextUtils.TextSection("Section 2", 1, "This is section 2")
        )

        val grouped = TextUtils.groupSections(sections, 2)

        assertEquals(
            listOf(
                listOf(
                    TextUtils.TextSection(null, null, "Start"),
                ),
                listOf(
                    TextUtils.TextSection("Section 1", 1, "This is section 1")
                ),
                listOf(
                    TextUtils.TextSection("Section 1.1", 2, "This is section 1.1"),
                    TextUtils.TextSection(null, null, "Value")
                ),
                listOf(
                    TextUtils.TextSection("Section 1.2", 2, "This is section 1.2"),
                ),
                listOf(
                    TextUtils.TextSection("Section 2", 1, "This is section 2")
                )
            ),
            grouped
        )

    }

    @Test
    fun groupSectionsAnyLevel() {
        val sections = listOf(
            TextUtils.TextSection(null, null, "Start"),
            TextUtils.TextSection("Section 1", 1, "This is section 1"),
            TextUtils.TextSection("Section 1.1", 2, "This is section 1.1"),
            TextUtils.TextSection(null, null, "Value"),
            TextUtils.TextSection("Section 1.2", 2, "This is section 1.2"),
            TextUtils.TextSection("Section 2", 1, "This is section 2")
        )

        val grouped = TextUtils.groupSections(sections, null)

        assertEquals(
            listOf(
                listOf(
                    TextUtils.TextSection(null, null, "Start"),
                ),
                listOf(
                    TextUtils.TextSection("Section 1", 1, "This is section 1"),
                    TextUtils.TextSection("Section 1.1", 2, "This is section 1.1"),
                    TextUtils.TextSection(null, null, "Value"),
                    TextUtils.TextSection("Section 1.2", 2, "This is section 1.2"),
                ),
                listOf(
                    TextUtils.TextSection("Section 2", 1, "This is section 2")
                )
            ),
            grouped
        )

    }

    @Test
    fun toMarkdown() {
        assertEquals("Test", TextUtils.TextSection(null, null, "Test").toMarkdown())
        assertEquals("# Test\nText", TextUtils.TextSection("Test", 1, "Text").toMarkdown())
        assertEquals("## Test\nText", TextUtils.TextSection("Test", 2, "Text").toMarkdown())
        assertEquals(
            "### Test\nText\nOther",
            TextUtils.TextSection("Test", 3, "Text\nOther").toMarkdown()
        )
    }

}