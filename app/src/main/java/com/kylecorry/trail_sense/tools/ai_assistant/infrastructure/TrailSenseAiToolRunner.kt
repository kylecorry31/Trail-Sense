package com.kylecorry.trail_sense.tools.ai_assistant.infrastructure

import android.content.Context
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiContext
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunResult
import com.kylecorry.trail_sense.tools.ai_assistant.domain.AiToolRunStatus
import com.kylecorry.trail_sense.tools.ai_assistant.domain.CloudAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.domain.NavigationAiContextProvider
import com.kylecorry.trail_sense.tools.ai_assistant.domain.WeatherAiContextProvider
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.climate.infrastructure.ClimateSubsystem
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.turn_back.services.TurnBackToolService
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackFragment
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.Instant

class TrailSenseAiToolRunner(
    context: Context
) : AiTrailSenseToolRunner {

    private val context = context.applicationContext

    override fun getToolName(toolId: Long): String {
        return Tools.getTool(context, toolId)?.name ?: "Tool $toolId"
    }

    override fun getOpenToolAction(toolId: Long): Int? {
        return Tools.getTool(context, toolId)?.navAction
    }

    override suspend fun run(toolId: Long, argumentsJson: String): AiToolRunResult {
        return try {
            if (!Tools.isToolAvailable(context, toolId)) {
                return unavailable(toolId, "${getToolName(toolId)} is unavailable on this device.")
            }

            when (toolId) {
                Tools.WEATHER -> weather()
                Tools.CLOUDS -> clouds()
                Tools.NAVIGATION -> navigation()
                Tools.LIGHTNING_STRIKE_DISTANCE -> lightning()
                Tools.TURN_BACK -> turnBack()
                Tools.BEACONS -> beacons()
                Tools.PATHS -> paths()
                Tools.OFFLINE_MAPS -> offlineMaps()
                Tools.CLIMATE -> climate()
                Tools.PACKING_LISTS -> packingLists()
                Tools.ASTRONOMY -> astronomy()
                Tools.CLINOMETER -> unavailable(
                    toolId,
                    "A live slope-angle measurement is required. Open Clinometer and align the phone with the slope."
                )
                Tools.TEMPERATURE_ESTIMATION -> temperatureEstimation(argumentsJson)
                Tools.MAP -> map()
                else -> unavailable(toolId, "${getToolName(toolId)} has no read-only AI collector yet.")
            }
        } catch (e: Exception) {
            failed(toolId, e.message ?: "Failed to read ${getToolName(toolId)}.")
        }
    }

    private suspend fun weather(): AiToolRunResult {
        val weatherSubsystem = WeatherSubsystem.getInstance(context)
        val aiContext = WeatherAiContextProvider { weatherSubsystem.getWeather() }.getAiContext()
        return fromContext(Tools.WEATHER, aiContext)
    }

    private suspend fun clouds(): AiToolRunResult {
        val aiContext = CloudAiContextProvider(
            CloudRepo.getInstance(context),
            CloudDetailsService(context),
            capturedImage = null
        ).getAiContext()
        return fromContext(Tools.CLOUDS, aiContext)
    }

    private suspend fun navigation(): AiToolRunResult {
        val aiContext = NavigationAiContextProvider(
            Navigator.getInstance(context),
            NavigationService(),
            LocationSubsystem.getInstance(context).location
        ).getAiContext()
        return fromContext(Tools.NAVIGATION, aiContext)
    }

    private suspend fun lightning(): AiToolRunResult {
        val latest = LightningRepo.getInstance(context).getLast()
            ?: return unavailable(
                Tools.LIGHTNING_STRIKE_DISTANCE,
                "No lightning strike distance readings have been recorded recently."
            )

        val distanceMeters = latest.value.distance.meters().value
        return succeeded(
            Tools.LIGHTNING_STRIKE_DISTANCE,
            "Latest lightning estimate is ${distanceMeters.toInt()} m away.",
            mapOf(
                "distance_m" to distanceMeters,
                "time" to latest.time.toString()
            )
        )
    }

    private fun turnBack(): AiToolRunResult {
        val service = TurnBackToolService(context)
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        val turnBackTime = prefs.getInstant(TurnBackFragment.PREF_TURN_BACK_TIME)
        val returnTime = prefs.getInstant(TurnBackFragment.PREF_TURN_BACK_RETURN_TIME)
        val remaining = turnBackTime?.let { Duration.between(Instant.now(), it) }

        return succeeded(
            Tools.TURN_BACK,
            if (service.isEnabled()) {
                "Turn Back is set${remaining?.let { ", with ${formatDuration(it)} until turn-back time" } ?: ""}."
            } else {
                "Turn Back is not set."
            },
            mapOf(
                "is_enabled" to service.isEnabled(),
                "is_running" to service.isRunning(),
                "turn_back_time" to turnBackTime?.toString(),
                "return_time" to returnTime?.toString()
            )
        )
    }

    private suspend fun beacons(): AiToolRunResult {
        val beacons = BeaconService(context).getBeacons(
            groupId = null,
            includeGroups = false,
            maxDepth = null,
            includeRoot = false
        )
        val names = beacons.take(5).joinToString(", ") { it.name }

        return succeeded(
            Tools.BEACONS,
            if (beacons.isEmpty()) "No saved beacons were found." else "Found ${beacons.size} saved beacons.",
            mapOf(
                "beacon_count" to beacons.size,
                "sample_beacons" to names.takeIf { it.isNotBlank() }
            )
        )
    }

    private suspend fun paths(): AiToolRunResult {
        val paths = PathRepo.getInstance(context).getAll()
        val totalDistance = paths.sumOf { it.metadata.distance.meters().value.toDouble() }
        val names = paths.take(5).joinToString(", ") { it.name ?: "Unnamed path" }

        return succeeded(
            Tools.PATHS,
            if (paths.isEmpty()) "No saved paths were found." else "Found ${paths.size} saved paths.",
            mapOf(
                "path_count" to paths.size,
                "total_distance_m" to totalDistance,
                "sample_paths" to names.takeIf { it.isNotBlank() }
            )
        )
    }

    private suspend fun offlineMaps(): AiToolRunResult {
        val db = AppDatabase.getInstance(context)
        val photoMaps = db.photoMapDao().getAll()
        val vectorMaps = db.vectorMapDao().getAllSync()

        return succeeded(
            Tools.OFFLINE_MAPS,
            "Found ${photoMaps.size} photo maps and ${vectorMaps.size} trail maps.",
            mapOf(
                "photo_map_count" to photoMaps.size,
                "trail_map_count" to vectorMaps.size,
                "sample_photo_maps" to photoMaps.take(3).joinToString(", ") { it.name }
                    .takeIf { it.isNotBlank() },
                "sample_trail_maps" to vectorMaps.take(3).joinToString(", ") { it.name }
                    .takeIf { it.isNotBlank() }
            )
        )
    }

    private suspend fun climate(): AiToolRunResult {
        val location = LocationSubsystem.getInstance(context).location
        val range = ClimateSubsystem.getInstance(context).getDailyTemperatureRange(
            location,
            LocalDate.now()
        )

        return succeeded(
            Tools.CLIMATE,
            "Typical daily range is ${range.start.celsius().value}°C to ${range.end.celsius().value}°C.",
            mapOf(
                "low_c" to range.start.celsius().value,
                "high_c" to range.end.celsius().value,
                "latitude" to location.latitude,
                "longitude" to location.longitude
            )
        )
    }

    private suspend fun packingLists(): AiToolRunResult {
        val repo = PackRepo.getInstance(context)
        val packs = repo.getPacksAsync()
        val itemCounts = packs.associate { pack -> pack.name to repo.getItemsFromPackAsync(pack.id).size }
        val packedCounts = packs.associate { pack ->
            pack.name to repo.getItemsFromPackAsync(pack.id).count { it.isFullyPacked }
        }

        return succeeded(
            Tools.PACKING_LISTS,
            if (packs.isEmpty()) "No packing lists were found." else "Found ${packs.size} packing lists.",
            mapOf(
                "pack_count" to packs.size,
                "item_counts" to itemCounts.entries.joinToString("; ") { "${it.key}: ${it.value}" },
                "fully_packed_counts" to packedCounts.entries.joinToString("; ") { "${it.key}: ${it.value}" }
            )
        )
    }

    private fun astronomy(): AiToolRunResult {
        val location = LocationSubsystem.getInstance(context).location
        val service = AstronomyService()
        val sunTimes = service.getTodaySunTimes(location, SunTimesMode.Actual)
        val daylight = service.getLengthOfDay(location, SunTimesMode.Actual, LocalDate.now())
        val nextSunrise = service.getNextSunrise(location, SunTimesMode.Actual)
        val nextSunset = service.getNextSunset(location, SunTimesMode.Actual)

        return succeeded(
            Tools.ASTRONOMY,
            "Daylight lasts about ${formatDuration(daylight)} today.",
            mapOf(
                "sunrise" to sunTimes.rise?.toString(),
                "sunset" to sunTimes.set?.toString(),
                "daylight_minutes" to daylight.toMinutes(),
                "next_sunrise" to nextSunrise?.toString(),
                "next_sunset" to nextSunset?.toString()
            )
        )
    }

    private suspend fun temperatureEstimation(argumentsJson: String): AiToolRunResult {
        val args = parseArguments(argumentsJson)
        val targetElevation = args?.optNumber("target_elevation_m")?.toFloat()
            ?: return unavailable(
                Tools.TEMPERATURE_ESTIMATION,
                "Target elevation is required before Temperature Estimation can calculate a result."
            )
        val location = LocationSubsystem.getInstance(context)
        val weather = WeatherSubsystem.getInstance(context).getWeather()
        val observation = weather.observation
            ?: return unavailable(
                Tools.TEMPERATURE_ESTIMATION,
                "Current weather observation is required before Temperature Estimation can calculate a result."
            )

        val estimated = Meteorology.getTemperatureAtElevation(
            observation.temperature,
            location.elevation,
            Distance.meters(targetElevation)
        )

        return succeeded(
            Tools.TEMPERATURE_ESTIMATION,
            "Estimated temperature at ${targetElevation.toInt()} m is ${estimated.celsius().value}°C.",
            mapOf(
                "base_temperature_c" to observation.temperature.celsius().value,
                "base_elevation_m" to location.elevation.meters().value,
                "target_elevation_m" to targetElevation,
                "estimated_temperature_c" to estimated.celsius().value
            )
        )
    }

    private fun map(): AiToolRunResult {
        val location = LocationSubsystem.getInstance(context)
        return succeeded(
            Tools.MAP,
            "Current map context is ${location.location.latitude}, ${location.location.longitude}.",
            mapOf(
                "latitude" to location.location.latitude,
                "longitude" to location.location.longitude,
                "elevation_m" to location.elevation.meters().value,
                "location_age_seconds" to location.locationAge.seconds,
                "elevation_age_seconds" to location.elevationAge.seconds
            )
        )
    }

    private fun fromContext(toolId: Long, aiContext: AiContext): AiToolRunResult {
        return succeeded(
            toolId,
            summarize(aiContext.summary),
            aiContext.sensorData
        )
    }

    private fun succeeded(
        toolId: Long,
        summary: String,
        data: Map<String, Any?>
    ): AiToolRunResult {
        return AiToolRunResult(
            toolId = toolId,
            toolName = getToolName(toolId),
            status = AiToolRunStatus.Succeeded,
            sensorData = data,
            summary = summary,
            openedNavAction = getOpenToolAction(toolId)
        )
    }

    private fun unavailable(toolId: Long, summary: String): AiToolRunResult {
        return AiToolRunResult(
            toolId = toolId,
            toolName = getToolName(toolId),
            status = AiToolRunStatus.Unavailable,
            summary = summary,
            error = summary,
            openedNavAction = getOpenToolAction(toolId)
        )
    }

    private fun failed(toolId: Long, error: String): AiToolRunResult {
        return AiToolRunResult(
            toolId = toolId,
            toolName = getToolName(toolId),
            status = AiToolRunStatus.Failed,
            summary = "Failed to read ${getToolName(toolId)}.",
            error = error,
            openedNavAction = getOpenToolAction(toolId)
        )
    }

    private fun parseArguments(argumentsJson: String): JSONObject? {
        return try {
            JSONObject(argumentsJson.takeIf { it.isNotBlank() } ?: "{}")
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.optNumber(name: String): Number? {
        if (!has(name) || isNull(name)) {
            return null
        }
        return opt(name) as? Number
    }

    private fun summarize(summary: String): String {
        val lines = summary.lines().map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
        val withoutTitle = lines.drop(1).ifEmpty { lines }
        return withoutTitle.joinToString(" ").take(260)
    }

    private fun formatDuration(duration: Duration): String {
        val normalized = if (duration.isNegative) Duration.ZERO else duration
        val hours = normalized.toHours()
        val minutes = normalized.minusHours(hours).toMinutes()
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${normalized.seconds}s"
        }
    }
}
