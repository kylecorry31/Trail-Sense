package com.kylecorry.trail_sense.experimental.inclinometer.domain

// From https://avalanche.org/avalanche-tutorial/avalanche-terrain/

class AvalancheRiskClassifier {

    fun classify(inclination: Float): AvalancheRisk {
        return when {
            inclination < 30 -> AvalancheRisk.NotSteepEnough
            inclination <= 45 -> AvalancheRisk.MostAvalanches
            else -> AvalancheRisk.SlabsLessCommon
        }
    }

}