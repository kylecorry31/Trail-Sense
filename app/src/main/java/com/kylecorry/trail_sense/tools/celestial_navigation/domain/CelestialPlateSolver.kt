package com.kylecorry.trail_sense.tools.celestial_navigation.domain

import android.util.Log
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.stars.DetectedStar
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Coordinate
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

class CelestialPlateSolver(
    private val maximumMagnitude: Float = 3f,
    private val pairToleranceDegrees: Float = 5f,
    private val minimumConsensusRatio: Float = 0.6f,
    private val beamWidth: Int = 2000
) {
    private data class CatalogStar(val star: Star, val position: CelestialObservation)
    private data class State(val assignments: List<Int?>, val cost: Float) {
        val matchCount = assignments.count { it != null }
    }

    fun solve(
        observations: List<CelestialObservation>,
        time: ZonedDateTime,
        approximateLocation: Coordinate
    ): List<DetectedStar> {
        Log.d(TAG, "Starting plate solve with ${observations.size} observations")
        if (observations.size < 4) {
            Log.d(TAG, "Not enough observations to solve")
            return emptyList()
        }

        val catalog = STAR_CATALOG.asSequence()
            .filter { it.magnitude <= maximumMagnitude }
            .map { CatalogStar(it, Astronomy.getStarPosition(it, time, approximateLocation, true)) }
            .filter { it.position.altitude >= -15f }
            .toList()
        Log.d(TAG, "Searching ${catalog.size} catalog stars")
        if (catalog.size < observations.size) {
            Log.d(TAG, "Catalog is smaller than observation set")
            return emptyList()
        }

        val observedDistances = distanceMatrix(observations)
        val catalogDistances = distanceMatrix(catalog.map { it.position })
        var states = listOf(State(emptyList(), 0f))
        for (observationIndex in observations.indices) {
            states = states.flatMap { state ->
                val skipped = State(state.assignments + null, state.cost)
                val matched = catalog.indices.mapNotNull { catalogIndex ->
                    if (state.assignments.any { it == catalogIndex }) {
                        return@mapNotNull null
                    }
                    var additionalCost = 0f
                    var comparisons = 0
                    var inliers = 0
                    state.assignments.forEachIndexed { previousObservation, previousCatalog ->
                        if (previousCatalog == null) {
                            return@forEachIndexed
                        }
                        val residual = abs(
                            observedDistances[observationIndex][previousObservation] -
                                    catalogDistances[catalogIndex][previousCatalog]
                        )
                        comparisons++
                        if (residual <= pairToleranceDegrees) {
                            inliers++
                        }
                        val robustResidual = residual.coerceAtMost(pairToleranceDegrees * 2)
                        additionalCost += robustResidual * robustResidual
                    }
                    val requiredInliers = max(
                        1,
                        ceil(comparisons * minimumConsensusRatio).toInt()
                    )
                    if (comparisons > 0 && inliers < requiredInliers) {
                        return@mapNotNull null
                    }
                    State(state.assignments + catalogIndex, state.cost + additionalCost)
                }
                matched + skipped
            }.sortedWith(compareByDescending<State> { it.matchCount }.thenBy { normalizedCost(it) })
                .take(beamWidth)
            Log.d(
                TAG,
                "Observation ${observationIndex + 1}/${observations.size}: " +
                        "${states.size} candidates, best matches=${states.firstOrNull()?.matchCount ?: 0}"
            )
            if (states.isEmpty()) {
                Log.d(TAG, "No candidate patterns remain")
                return emptyList()
            }
        }

        val minimumMatches = max(4, ceil(observations.size * minimumConsensusRatio).toInt())
        val best = states.filter { it.matchCount >= minimumMatches }.minWithOrNull(
            compareByDescending<State> { it.matchCount }.thenBy { state ->
                val matched = getMatchedObservations(observations, state, catalog)
                normalizedCost(state) + orientationCost(
                    matched.map { it.first },
                    matched.map { it.second.position }
                )
            }
        ) ?: run {
            Log.d(TAG, "No pattern reached $minimumMatches required consensus matches")
            return emptyList()
        }
        val pairCount = best.matchCount * (best.matchCount - 1) / 2
        val rmsError = sqrt(best.cost / pairCount.coerceAtLeast(1))
        val confidence = exp(-rmsError / pairToleranceDegrees).coerceIn(0f, 1f)
        Log.d(
            TAG,
            "Selected ${best.matchCount}/${observations.size} observations, " +
                    "rmsError=$rmsError°, confidence=$confidence"
        )
        return best.assignments.mapIndexedNotNull { index, catalogIndex ->
            catalogIndex ?: return@mapIndexedNotNull null
            Log.d(TAG, "Observation ${index + 1} matched ${catalog[catalogIndex].star.name}")
            DetectedStar(catalog[catalogIndex].star, observations[index], confidence)
        }
    }

    private fun normalizedCost(state: State): Float {
        val pairCount = state.matchCount * (state.matchCount - 1) / 2
        return state.cost / pairCount.coerceAtLeast(1)
    }

    private fun getMatchedObservations(
        observations: List<CelestialObservation>,
        state: State,
        catalog: List<CatalogStar>
    ): List<Pair<CelestialObservation, CatalogStar>> {
        return state.assignments.mapIndexedNotNull { index, catalogIndex ->
            catalogIndex?.let { observations[index] to catalog[it] }
        }
    }

    private fun orientationCost(
        observations: List<CelestialObservation>,
        catalog: List<CelestialObservation>
    ): Float {
        val headingOffsets = observations.zip(catalog).map { (observation, star) ->
            deltaAngle(star.azimuth.value, observation.azimuth.value)
        }
        val meanOffset = headingOffsets.average().toFloat()
        return observations.zip(catalog).sumOf { (observation, star) ->
            val altitudeError = observation.altitude - star.altitude
            val headingError = deltaAngle(
                star.azimuth.value + meanOffset,
                observation.azimuth.value
            )
            (altitudeError * altitudeError + headingError * headingError).toDouble()
        }.toFloat()
    }

    private fun distanceMatrix(points: List<CelestialObservation>): List<FloatArray> {
        return points.indices.map { first ->
            FloatArray(points.size) { second ->
                if (first == second) {
                    0f
                } else {
                    Trigonometry.getAngularDistance(
                        points[first].azimuth.value,
                        points[first].altitude,
                        points[second].azimuth.value,
                        points[second].altitude
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "CelestialPlateSolver"
    }
}
