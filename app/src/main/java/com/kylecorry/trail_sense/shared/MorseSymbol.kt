package com.kylecorry.trail_sense.shared

enum class MorseSymbol(val durationMultiplier: Int) {
    Dot(1), Dash(3), Space(1), LetterSpace(3), WordSpace(7)
}

val SOS = listOf(
    MorseSymbol.Dot, MorseSymbol.Space, MorseSymbol.Dot, MorseSymbol.Space, MorseSymbol.Dot,
    MorseSymbol.Space,
    MorseSymbol.Dash, MorseSymbol.Space, MorseSymbol.Dash, MorseSymbol.Space, MorseSymbol.Dash,
    MorseSymbol.Space,
    MorseSymbol.Dot, MorseSymbol.Space, MorseSymbol.Dot, MorseSymbol.Space, MorseSymbol.Dot,
    MorseSymbol.WordSpace
)