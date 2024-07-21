package com.kylecorry.trail_sense.test_utils

import androidx.annotation.StringRes


fun interface TextMatcher {
    fun matches(text: String): Boolean

    companion object {
        fun contains(text: String): TextMatcher {
            return TextMatcher { it.contains(text) }
        }

        fun equals(text: String): TextMatcher {
            return TextMatcher { it == text }
        }

        fun matches(regex: Regex): TextMatcher {
            return TextMatcher { it.matches(regex) }
        }

        fun equals(@StringRes resId: Int): TextMatcher {
            return TextMatcher { it == TestUtils.getString(resId) }
        }

        fun any(): TextMatcher {
            return TextMatcher { true }
        }
    }
}