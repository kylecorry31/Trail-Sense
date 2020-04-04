package com.kylecorry.trail_sense.weather.domain.classifier

class StandardPressureClassifier :
    IPressureClassifier {
    override fun classify(pressure: Float): PressureClassification {
        return when {
            pressure >= 1022.689 -> PressureClassification.High
            pressure <= 1009.144 -> PressureClassification.Low
            else -> PressureClassification.Normal
        }
    }
}