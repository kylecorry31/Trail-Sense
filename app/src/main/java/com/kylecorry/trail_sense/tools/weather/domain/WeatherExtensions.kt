package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.Instant

fun List<Reading<CloudGenus?>>.getLastCloud(
    maxTime: Duration,
    now: Instant = Instant.now()
): Reading<CloudGenus?>? {
    val lastCloud = lastOrNull() ?: return null
    if (Duration.between(lastCloud.time, now).abs() > maxTime) {
        return null
    }
    return lastCloud
}
