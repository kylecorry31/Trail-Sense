package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R

object ToolKeywordLoader {

    fun load(context: Context): Map<Long, Set<String>> {
        val text = context.resources.openRawResource(R.raw.tool_keywords)
            .bufferedReader()
            .use { it.readText() }

        val map = mutableMapOf<Long, Set<String>>()

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Format: ID (Tool Name): keyword1, keyword2, keyword3
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex == -1) continue

            val prefix = trimmed.substring(0, colonIndex).trim()
            val id = prefix.substringBefore('(').trim().toLongOrNull() ?: continue

            val keywords = trimmed.substring(colonIndex + 1)
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            map[id] = keywords
        }

        return map
    }
}
