package com.kylecorry.trail_sense.tools.inclinometer.domain

import kotlin.math.absoluteValue

// From https://avalanche.org/avalanche-tutorial/avalanche-terrain/

class AvalancheRiskClassifier {

    fun classify(inclination: Float): AvalancheRisk {
        return when {
            inclination.absoluteValue < 30 -> AvalancheRisk.NotSteepEnough
            inclination.absoluteValue <= 45 -> AvalancheRisk.MostAvalanches
            else -> AvalancheRisk.SlabsLessCommon
        }
    }

}