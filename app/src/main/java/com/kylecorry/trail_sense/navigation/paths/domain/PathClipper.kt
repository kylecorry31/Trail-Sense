package com.kylecorry.trail_sense.navigation.paths.domain

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

class PathClipper {

    suspend fun clip(path: List<Coordinate>, bounds: CoordinateBounds): List<List<Coordinate>> =
        onDefault {
            val clipped = mutableListOf<MutableList<Coordinate>>()
            for (i in 1 until path.size) {
                val a = path[i - 1]
                val b = path[i]
                val aIn = bounds.contains(a)
                val bIn = bounds.contains(b)
                if (aIn && bIn) {
                    append(clipped, a, b)
                }

                if (!aIn && !bIn) {
                    // Both points have latitudes below the bounds, so skip
                    if (a.latitude < bounds.south && b.latitude < bounds.south) {
                        continue
                    }

                    // Both points have latitudes above the bounds, so skip
                    if (a.latitude > bounds.north && b.latitude > bounds.north) {
                        continue
                    }

                    // TODO: Create bounds around the world instead of checking longitude directly

                    // Both points have longitudes below the bounds, so skip
                    if (a.longitude < bounds.west && b.longitude < bounds.west) {
                        continue
                    }

                    // Both points have longitudes above the bounds, so skip
                    if (a.longitude > bounds.east && b.longitude > bounds.east) {
                        continue
                    }

                    // There might be an intersection across a corner
                    val intersectionAB = intersection(a, b, bounds)
                    if (intersectionAB != null) {
                        val intersectionBA = intersection(b, a, bounds)
                        if (intersectionBA != null) {
                            append(clipped, intersectionAB, intersectionBA)
                        }
                    }
                } else if (aIn && !bIn) {
                    val intersection = intersection(a, b, bounds)
                    if (intersection != null) {
                        append(clipped, a, intersection)
                    }
                } else if (!aIn && bIn) {
                    val intersection = intersection(a, b, bounds)
                    if (intersection != null) {
                        append(clipped, intersection, b)
                    }
                }
            }
            clipped
        }

    private fun intersection(a: Coordinate, b: Coordinate, bounds: CoordinateBounds): Coordinate? {
        // TODO: Figure out which side of the bounds the intersection will occur on
        // TODO: Calculate the point of intersection using Ed William’s aviation formulary (https://www.movable-type.co.uk/scripts/latlong.html)
        return null
    }

    private fun append(paths: MutableList<MutableList<Coordinate>>, a: Coordinate, b: Coordinate) {
        val lastPath = paths.lastOrNull()
        val pathToAppend = if (lastPath?.lastOrNull() != a) {
            // Start a new path because the last path doesn't end with a
            mutableListOf(a)
        } else {
            lastPath
        }

        pathToAppend.add(b)
        if (lastPath != pathToAppend) {
            paths.add(pathToAppend)
        }
    }

}