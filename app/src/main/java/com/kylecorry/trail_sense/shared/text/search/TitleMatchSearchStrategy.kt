package com.kylecorry.trail_sense.shared.text.search

import com.kylecorry.trail_sense.shared.text.TextUtils

class TitleMatchSearchStrategy(private val matchStrategy: TextMatchStrategy = TextMatchStrategy.Contains) :
    SearchStrategy {
    override fun getSearchScore(
        query: String,
        item: SearchItem
    ): Float {
        return if (TextUtils.isMatch(item.title, query, matchStrategy, true)) {
            1f
        } else {
            0f
        }
    }
}
