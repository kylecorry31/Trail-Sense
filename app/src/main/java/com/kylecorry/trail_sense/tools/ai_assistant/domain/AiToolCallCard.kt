package com.kylecorry.trail_sense.tools.ai_assistant.domain

import org.json.JSONArray
import org.json.JSONObject

data class AiToolCallCard(
    val toolId: Long,
    val toolName: String,
    val skillId: String,
    val status: AiToolRunStatus,
    val summary: String,
    val details: String? = null,
    val openedNavAction: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("tool_id", toolId)
            .put("tool_name", toolName)
            .put("skill_id", skillId)
            .put("status", status.id)
            .put("summary", summary)
            .put("details", details)
            .put("opened_nav_action", openedNavAction)
            .put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): AiToolCallCard {
            val navAction = if (json.isNull("opened_nav_action")) {
                null
            } else {
                json.optInt("opened_nav_action")
            }

            return AiToolCallCard(
                toolId = json.optLong("tool_id"),
                toolName = json.optString("tool_name"),
                skillId = json.optString("skill_id"),
                status = AiToolRunStatus.from(json.optString("status")),
                summary = json.optString("summary"),
                details = if (json.isNull("details")) null else json.optString("details")
                    .takeIf { it.isNotBlank() },
                openedNavAction = navAction,
                timestamp = json.optLong("timestamp")
            )
        }

        fun toJson(cards: List<AiToolCallCard>): String {
            val array = JSONArray()
            cards.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(json: String?): List<AiToolCallCard> {
            if (json.isNullOrBlank()) {
                return emptyList()
            }

            return try {
                val array = JSONArray(json)
                (0 until array.length()).mapNotNull {
                    array.optJSONObject(it)?.let(::fromJson)
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
