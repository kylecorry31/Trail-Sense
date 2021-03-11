package com.kylecorry.trail_sense.shared

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class IsMorningSpecificationTest {

    @Test
    fun detectsWhenItIsMorning(){
        val spec = IsMorningSpecification()
        assertTrue(spec.isSatisfiedBy(LocalTime.of(6, 0)))
        assertTrue(spec.isSatisfiedBy(LocalTime.of(8, 0)))
        assertTrue(spec.isSatisfiedBy(LocalTime.of(9, 0)))
    }

    @Test
    fun detectsWhenItIsNotMorning(){
        val spec = IsMorningSpecification()
        assertFalse(spec.isSatisfiedBy(LocalTime.of(5, 59)))
        assertFalse(spec.isSatisfiedBy(LocalTime.of(9, 1)))
        assertFalse(spec.isSatisfiedBy(LocalTime.of(18, 0)))
    }

}