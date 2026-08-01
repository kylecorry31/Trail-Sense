package com.kylecorry.trail_sense.shared.andromeda_temp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ResultTest {

    @Test
    fun identifiesOk() {
        val result: Result<Int, String> = Result.Ok(1)

        assertTrue(result.isOk)
        assertFalse(result.isErr)
    }

    @Test
    fun identifiesErr() {
        val result: Result<Int, String> = Result.Err("error")

        assertFalse(result.isOk)
        assertTrue(result.isErr)
    }

    @Test
    fun mapsOkValue() {
        val result: Result<Int, String> = Result.Ok(2)

        assertEquals(Result.Ok(4), result.map { it * 2 })
    }

    @Test
    fun mapPreservesErrWithoutTransforming() {
        val result: Result<Int, String> = Result.Err("error")

        val mapped = result.map { error("Transform should not be called") }

        assertEquals(Result.Err("error"), mapped)
    }

    @Test
    fun mapErrorPreservesOkWithoutTransforming() {
        val result: Result<Int, String> = Result.Ok(2)

        val mapped = result.mapError { error("Transform should not be called") }

        assertEquals(Result.Ok(2), mapped)
    }

    @Test
    fun mapsErrValue() {
        val result: Result<Int, String> = Result.Err("error")

        assertEquals(Result.Err(5), result.mapError { it.length })
    }

    @Test
    fun andThenTransformsOk() {
        val result: Result<Int, String> = Result.Ok(2)

        assertEquals(Result.Ok(4), result.andThen { Result.Ok(it * 2) })
    }

    @Test
    fun andThenPreservesErrWithoutTransforming() {
        val result: Result<Int, String> = Result.Err("error")

        val transformed: Result<Int, String> = result.andThen {
            error("Transform should not be called")
        }

        assertEquals(Result.Err("error"), transformed)
    }

    @Test
    fun foldsOk() {
        val result: Result<Int, String> = Result.Ok(2)

        val folded = result.fold(onOk = { it * 2 }, onErr = { error("onErr should not be called") })

        assertEquals(4, folded)
    }

    @Test
    fun foldsErr() {
        val result: Result<Int, String> = Result.Err("error")

        val folded = result.fold(onOk = { error("onOk should not be called") }, onErr = { it.length })

        assertEquals(5, folded)
    }

    @Test
    fun getsOkOrNull() {
        val result: Result<Int, String> = Result.Ok(2)

        assertEquals(2, result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun getsErrOrNull() {
        val result: Result<Int, String> = Result.Err("error")

        assertNull(result.getOrNull())
        assertEquals("error", result.errorOrNull())
    }

    @Test
    fun unwrapsOk() {
        val result: Result<Int, String> = Result.Ok(2)

        assertEquals(2, result.unwrap())
    }

    @Test
    fun unwrapFailsForErr() {
        val result: Result<Int, String> = Result.Err("error")

        val exception = assertThrows(IllegalStateException::class.java) { result.unwrap() }

        assertEquals("Called unwrap() on Err(error)", exception.message)
    }

    @Test
    fun unwrapsErr() {
        val result: Result<Int, String> = Result.Err("error")

        assertEquals("error", result.unwrapError())
    }

    @Test
    fun unwrapErrorFailsForOk() {
        val result: Result<Int, String> = Result.Ok(2)

        val exception = assertThrows(IllegalStateException::class.java) { result.unwrapError() }

        assertEquals("Called unwrap() on Ok(2)", exception.message)
    }

    @Test
    fun unwrapOrReturnsOk() {
        val result: Result<Int, String> = Result.Ok(2)

        assertEquals(2, result.unwrapOr(3))
    }

    @Test
    fun unwrapOrReturnsDefaultForErr() {
        val result: Result<Int, String> = Result.Err("error")

        assertEquals(3, result.unwrapOr(3))
    }

    @Test
    fun unwrapOrElseReturnsOkWithoutCreatingDefault() {
        val result: Result<Int, String> = Result.Ok(2)

        val value = result.unwrapOrElse { error("Default should not be called") }

        assertEquals(2, value)
    }

    @Test
    fun unwrapOrElseCreatesDefaultForErr() {
        val result: Result<Int, String> = Result.Err("error")

        assertEquals(5, result.unwrapOrElse { it.length })
    }
}
