package com.kylecorry.trail_sense.tools.inclinometer.domain

import kotlin.math.absoluteValue

class AvalancheRiskClassifier {

    fun classify(inclination: Float): AvalancheRisk {

        val absAngle = inclination.absoluteValue

        return when {
            absAngle < 20 -> {
                AvalancheRisk.Low
            }
            absAngle in 30.0..50.0 -> {
                AvalancheRisk.High
            }
            else -> {
                AvalancheRisk.Moderate
            }
        }
    }

}