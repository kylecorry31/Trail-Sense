package com.kylecorry.trail_sense.shared.text.search

interface SearchStrategy {

    fun getSearchScore(query: String, item: SearchItem): Float

}