package com.kylecorry.trail_sense.tools.ai_assistant.domain

import android.graphics.Bitmap
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.clouds.domain.CloudService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo

class CloudAiContextProvider(
    private val cloudRepo: CloudRepo,
    private val cloudDetailsService: CloudDetailsService,
    private val capturedImage: Bitmap?
) : AiContextProvider {

    override val toolId: String = "clouds"

    override suspend fun getAiContext(): AiContext {
        val observations = cloudRepo.getAll()
        val cloudService = CloudService()

        val sensorData = mutableMapOf<String, Any>()

        val summary = buildString {
            append("Cloud Tool Data:\n")
            if (observations.isNotEmpty()) {
                val latest = observations.last()
                val genusName = cloudDetailsService.getCloudName(latest.value.genus)
                sensorData["latest_cloud_type"] = genusName
                append("- Latest observation: $genusName\n")

                val forecast = cloudDetailsService.getCloudForecast(latest.value.genus)
                sensorData["cloud_forecast"] = forecast
                append("- Forecast: $forecast\n")

                val probability = cloudService.getPrecipitationProbability(latest.value.genus)
                sensorData["precipitation_probability"] = probability.name
                append("- Precipitation probability: ${probability.name}\n")

                val precipTypes = cloudService.getPrecipitation(latest.value.genus)
                if (precipTypes.isNotEmpty()) {
                    val types = precipTypes.joinToString(", ") { it.name }
                    sensorData["precipitation_types"] = types
                    append("- Precipitation types: $types\n")
                }
            } else {
                append("- No cloud observations recorded\n")
            }
            sensorData["observation_count"] = observations.size
            append("- Observations in last 48h: ${observations.size}\n")
            if (capturedImage != null) {
                append("- A cloud photo is attached for analysis\n")
            }
        }

        return AiContext(
            toolId = toolId,
            toolName = "Clouds",
            sensorData = sensorData,
            image = capturedImage,
            summary = summary
        )
    }

    override fun getSuggestedQuestions(): List<String> {
        return listOf(
            "What type of cloud is this?",
            "What weather does this cloud indicate?",
            "Should I expect rain?"
        )
    }
}
