package com.kylecorry.trail_sense.shared

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class IsEveningSpecificationTest {

    @Test
    fun detectsWhenItIsEvening(){
        val spec = IsEveningSpecification()
        assertTrue(spec.isSatisfiedBy(LocalTime.of(18, 0)))
        assertTrue(spec.isSatisfiedBy(LocalTime.of(20, 0)))
        assertTrue(spec.isSatisfiedBy(LocalTime.of(21, 0)))
    }

    @Test
    fun detectsWhenItIsNotEvening(){
        val spec = IsEveningSpecification()
        assertFalse(spec.isSatisfiedBy(LocalTime.of(17, 59)))
        assertFalse(spec.isSatisfiedBy(LocalTime.of(21, 1)))
        assertFalse(spec.isSatisfiedBy(LocalTime.of(6, 0)))
    }

}