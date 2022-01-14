package com.kylecorry.trail_sense.shared.domain

enum class Probability {
    Never,
    Low,
    Moderate,
    High,
    Always
}

fun probability(chance: Float): Probability {
    return when {
        chance < 0.05f -> {
            Probability.Never
        }
        chance < 0.25f -> {
            Probability.Low
        }
        chance < 0.75f -> {
            Probability.Moderate
        }
        chance < 0.95f -> {
            Probability.High
        }
        else -> {
            Probability.Always
        }
    }
}