package com.kylecorry.trail_sense.shared.text

object TextUtils {

    fun getSections(markdown: String): List<TextSection> {
        val sections = mutableListOf<TextSection>()
        val lines = markdown.split("\n")
        var currentContent = ""
        var currentTitle: String? = null
        var currentLevel: Int? = null
        for (line in lines) {
            if (line.startsWith("#")) {
                if (currentTitle != null || currentContent.isNotBlank()) {
                    sections.add(TextSection(currentTitle, currentLevel, currentContent.trim()))
                }
                currentLevel = line.count { it == '#' }
                currentTitle = line.substringAfter("#".repeat(currentLevel)).trim()
                currentContent = ""
            } else {
                currentContent += line + "\n"
            }
        }

        if (currentTitle != null || currentContent.isNotBlank()) {
            sections.add(TextSection(currentTitle, currentLevel, currentContent.trim()))
        }

        return sections
    }

    /**
     * Groups sections by their level. Nested sections are grouped together.
     */
    fun groupSections(sections: List<TextSection>, splitLevel: Int?): List<List<TextSection>> {
        val grouped = mutableListOf<MutableList<TextSection>>()
        var currentGroup = mutableListOf<TextSection>()
        for (section in sections) {
            val first = currentGroup.firstOrNull()
            val isLowerLevel = if (splitLevel != null) {
                section.level == null || splitLevel < section.level
            } else if (first?.level == null) {
                false
            } else {
                section.level == null || first.level < section.level
            }


            if (currentGroup.isEmpty() || isLowerLevel) {
                currentGroup.add(section)
            } else {
                grouped.add(currentGroup)
                currentGroup = mutableListOf(section)
            }
        }

        if (currentGroup.isNotEmpty()) {
            grouped.add(currentGroup)
        }

        return grouped
    }

    data class TextSection(val title: String?, val level: Int?, val content: String) {
        fun toMarkdown(): String {
            if (title == null || level == null) {
                return content
            }

            return "#".repeat(level) + " $title\n$content"
        }
    }
}