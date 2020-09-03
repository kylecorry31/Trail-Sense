package com.kylecorry.trail_sense.tools.inclinometer.domain

import org.junit.Test

import org.junit.Assert.*

class AvalancheRiskClassifierTest {

    @Test
    fun classify() {
        val classifier = AvalancheRiskClassifier()

        // Low
        assertEquals(AvalancheRisk.Low, classifier.classify(0f))
        assertEquals(AvalancheRisk.Low, classifier.classify(19f))

        // Moderate
        assertEquals(AvalancheRisk.Moderate, classifier.classify(20f))
        assertEquals(AvalancheRisk.Moderate, classifier.classify(29f))
        assertEquals(AvalancheRisk.Moderate, classifier.classify(51f))
        assertEquals(AvalancheRisk.Moderate, classifier.classify(90f))

        // High
        assertEquals(AvalancheRisk.High, classifier.classify(30f))
        assertEquals(AvalancheRisk.High, classifier.classify(45f))
        assertEquals(AvalancheRisk.High, classifier.classify(50f))
    }
}