package com.kylecorry.trail_sense.tools.ai_assistant.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class NavigationAiContextProvider(
    private val navigator: Navigator,
    private val navigationService: NavigationService,
    private val location: Coordinate?
) : AiContextProvider {

    override val toolId: String = "navigation"

    override suspend fun getAiContext(): AiContext {
        val destination = navigator.getDestination()
        val sensorData = mutableMapOf<String, Any>()

        val summary = buildString {
            append("Navigation Tool Data:\n")
            if (location != null) {
                sensorData["latitude"] = location.latitude
                sensorData["longitude"] = location.longitude
                append("- Current location: ${location.latitude}, ${location.longitude}\n")
            }
            if (destination != null && location != null) {
                val vector = navigationService.navigate(location, destination.coordinate, 0f, true)
                sensorData["destination_name"] = destination.name ?: "Unnamed"
                sensorData["distance_m"] = vector.distance
                sensorData["bearing_degrees"] = vector.direction.value
                append("- Destination: ${destination.name ?: "Unnamed"}\n")
                append("- Distance: ${formatDistance(vector.distance)}\n")
                append("- Bearing: ${vector.direction.value.toInt()}°\n")
                destination.elevation?.let {
                    sensorData["destination_elevation_m"] = it
                    append("- Destination elevation: ${it}m\n")
                }
            } else if (!navigator.isNavigating()) {
                append("- Status: Not navigating to any destination\n")
            }
        }

        return AiContext(
            toolId = toolId,
            toolName = "Navigation",
            sensorData = sensorData,
            image = null,
            summary = summary
        )
    }

    override fun getSuggestedQuestions(): List<String> {
        return listOf(
            "How do I get to my destination?",
            "Am I on the right track?",
            "How long until I arrive?"
        )
    }

    private fun formatDistance(meters: Float): String {
        return if (meters >= 1000) {
            "${"%.1f".format(meters / 1000)}km"
        } else {
            "${meters.toInt()}m"
        }
    }
}
