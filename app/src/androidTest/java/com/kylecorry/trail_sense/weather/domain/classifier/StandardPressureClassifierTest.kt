package com.kylecorry.trail_sense.weather.domain.classifier

import org.junit.Assert.assertEquals
import org.junit.Test

class StandardPressureClassifierTest {

    @Test
    fun classifyHighPressure() {
        val classifier = StandardPressureClassifier()
        assertEquals(PressureClassification.High, classifier.classify(1030f))
        assertEquals(PressureClassification.High, classifier.classify(1022.689f))
    }

    @Test
    fun classifyLowPressure() {
        val classifier = StandardPressureClassifier()
        assertEquals(PressureClassification.Low, classifier.classify(1000f))
        assertEquals(PressureClassification.Low, classifier.classify(1009.144f))
    }

    @Test
    fun classifyNormalPressure() {
        val classifier = StandardPressureClassifier()
        assertEquals(PressureClassification.Normal, classifier.classify(1009.145f))
        assertEquals(PressureClassification.Normal, classifier.classify(1022.688f))
        assertEquals(PressureClassification.Normal, classifier.classify(1013f))
    }
}