package com.kylecorry.trail_sense.shared

import com.kylecorry.trail_sense.shared.Slugify.slugify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SlugifyTest {

    @Test
    fun slugify() {
        assertEquals("test", "test".slugify())
        assertEquals("test-test2", "test test2".slugify())
        assertEquals("test-test2", "te?st test2".slugify())
        assertEquals("test-test2", "test-test2".slugify())
        assertEquals("test", "TEST".slugify())
        assertEquals("test-test2", "  test    test2  ".slugify())
        assertEquals("", "?".slugify())
        assertEquals("", "".slugify())
        assertEquals("aeiou-test", "áéíóů test".slugify())
    }
}