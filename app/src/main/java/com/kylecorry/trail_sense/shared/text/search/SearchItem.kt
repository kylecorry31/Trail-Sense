package com.kylecorry.trail_sense.shared.text.search

class SearchItem(
    val id: String,
    val title: String,
    val keywords: Set<String> = emptySet(),
    val parent: SearchItem? = null,
    val scoreMultiplier: Float = 1f
)