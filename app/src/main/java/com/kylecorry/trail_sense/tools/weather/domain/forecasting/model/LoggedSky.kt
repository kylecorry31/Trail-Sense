package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.Instant

internal enum class SkyCover {
    Clear,
    PartlyCloudy,
    MostlyCloudy
}

/**
 * @param cover the cover of the latest log
 * @param clouds every cloud logged
 */
internal data class SkyLog(val cover: SkyCover, val clouds: Set<CloudGenus>) {

    companion object {
        private val LAYERED_CLOUDS = setOf(
            CloudGenus.Stratus,
            CloudGenus.Nimbostratus,
            CloudGenus.Altostratus,
            CloudGenus.Stratocumulus,
            CloudGenus.Cirrostratus
        )

        /**
         * @param clouds logged clouds, where a null genus is a clear sky and clouds logged together share a time
         * @return the logs at least [newest] and less than [oldest] before [now], or null when there are none
         */
        fun fromLoggedClouds(
            clouds: List<Reading<CloudGenus?>>,
            now: Instant,
            newest: Duration,
            oldest: Duration
        ): SkyLog? {
            val logs = clouds.filter {
                val age = Duration.between(it.time, now)
                age >= newest && age < oldest
            }
            val latestTime = logs.maxOfOrNull { it.time } ?: return null
            val latest = logs.filter { it.time == latestTime }.mapNotNull { it.value }
            val cover = when {
                latest.isEmpty() -> SkyCover.Clear
                latest.any { it in LAYERED_CLOUDS } -> SkyCover.MostlyCloudy
                else -> SkyCover.PartlyCloudy
            }
            return SkyLog(cover, logs.mapNotNull { it.value }.toSet())
        }
    }
}

/**
 * @param recent the logs from the past hour
 * @param earlier the logs from 1 to 4 hours ago
 */
internal data class LoggedSky(val recent: SkyLog?, val earlier: SkyLog?) {

    val isLogged = recent != null || earlier != null

    companion object {
        private val RECENT = Duration.ofHours(1)
        private val EARLIER = Duration.ofHours(4)

        fun fromLoggedClouds(clouds: List<Reading<CloudGenus?>>, now: Instant): LoggedSky {
            return LoggedSky(
                SkyLog.fromLoggedClouds(clouds, now, Duration.ZERO, RECENT),
                SkyLog.fromLoggedClouds(clouds, now, RECENT, EARLIER)
            )
        }
    }
}
