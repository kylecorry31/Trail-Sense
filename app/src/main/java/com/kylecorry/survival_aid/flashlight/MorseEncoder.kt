package com.kylecorry.survival_aid.flashlight

/**
 * An object for encoding morse code as light
 */
object MorseEncoder {

    /**
     * A morse code symbol
     * @param length the duration of the symbol relative to a dot
     */
    enum class MorseSymbol(val length: Int, val state: Boolean) {
        DOT(1, true),
        DASH(3, true),
        SPACE(3, false),
        WORD_SEPARATOR(7, false)
    }

    private fun morseToBool(symbol: MorseSymbol): Array<Boolean> {
        return Array(symbol.length) { symbol.state }
    }

    /**
     * Encodes morse code symbols as booleans representing the light state
     * @param symbols the morse symbols
     * @return the boolean state representing the transmitted code
     */
    fun encode(symbols: List<MorseSymbol>): List<Boolean> {
        val output = mutableListOf<Boolean>()
        for (symbol in symbols){
            output.addAll(morseToBool(symbol))
            output.add(false)
        }
        return output
    }

}