package com.kylecorry.trail_sense.shared.text

class LevenshteinDistance {

    fun editDistance(s1: String, s2: String): Int {
        if (s1 == s2) {
            return 0
        }

        if (s1.length < s2.length) {
            return editDistance(s2, s1)
        }

        var previousRow = IntArray(s2.length + 1) { it }
        for (i in s1.indices) {
            val currentRow = IntArray(s2.length + 1) { i + 1 }
            for (j in s2.indices) {
                val insertions = previousRow[j + 1] + 1
                val deletions = currentRow[j] + 1
                val substitutions = previousRow[j] + if (s1[i] != s2[j]) 1 else 0
                currentRow[j + 1] = minOf(insertions, deletions, substitutions)
            }
            previousRow = currentRow
        }

        return previousRow.last()
    }

    fun percentSimilarity(s1: String, s2: String): Float {
        return 1 - editDistance(s1, s2).toFloat() / maxOf(s1.length, s2.length)
    }

}