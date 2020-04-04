package com.kylecorry.trail_sense.weather.domain.classifier

interface IPressureClassifier {

    fun classify(pressure: Float): PressureClassification

}