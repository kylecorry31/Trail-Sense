package com.kylecorry.trail_sense.shared.events

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class EventDataTest {

    @Test
    fun storesAndRetrievesStrings() {
        val data = EventData()

        data.putString("key", "value")

        assertEquals("value", data.getString("key"))
    }

    @Test
    fun storesAndRetrievesNullStrings() {
        val data = EventData()

        data.putString("key", null)

        assertNull(data.getString("key"))
    }

    @Test
    fun storesAndRetrievesInts() {
        val data = EventData()

        data.putInt("key", 12)

        assertEquals(12, data.getInt("key"))
    }

    @Test
    fun storesAndRetrievesLongs() {
        val data = EventData()

        data.putLong("key", 12L)

        assertEquals(12L, data.getLong("key"))
    }

    @Test
    fun storesAndRetrievesFloats() {
        val data = EventData()

        data.putFloat("key", 12.5f)

        assertEquals(12.5f, data.getFloat("key"))
    }

    @Test
    fun returnsNullWhenKeyIsMissing() {
        val data = EventData()

        assertNull(data.getString("key"))
        assertNull(data.getInt("key"))
        assertNull(data.getLong("key"))
        assertNull(data.getFloat("key"))
    }

    @Test
    fun returnsNullWhenTypeDoesNotMatch() {
        val data = EventData()

        data.putLong("key", 12L)

        assertNull(data.getString("key"))
        assertNull(data.getInt("key"))
        assertNull(data.getFloat("key"))
    }

    @Test
    fun replacesExistingValue() {
        val data = EventData()

        data.putInt("key", 12)
        data.putLong("key", 15L)

        assertNull(data.getInt("key"))
        assertEquals(15L, data.getLong("key"))
    }

    @Test
    fun iteratesOverValues() {
        val data = EventData().apply {
            putString("string", "value")
            putInt("int", 12)
            putLong("long", 15L)
            putFloat("float", 12.5f)
        }

        val values = mutableMapOf<String, Any?>()
        data.forEach { key, value ->
            values[key] = value
        }

        assertEquals(
            mapOf(
                "string" to "value",
                "int" to 12,
                "long" to 15L,
                "float" to 12.5f
            ),
            values
        )
    }
}
