package com.kylecorry.trail_sense.shared.canvas

import kotlin.math.sqrt

class LineInterpolator {

    /**
     * Increase the resolution of a line
     * @param line The line to increase the resolution of (in the form [x1, y1, x2, y2, ...], where each 4 elements are a line segment).
     * @param lineOutput The output list to write the line to
     * @param minSpacing The minimum spacing between points to add. The actual spacing may be larger than this value to ensure the spacing is even.
     * @param z The z values of the line. There are two z values per segment, one for each endpoint.
     * @param zOutput The output list to write the z values to
     */
    fun increaseResolution(
        line: List<Float>,
        lineOutput: MutableList<Float>,
        minSpacing: Float,
        z: List<Float>? = null,
        zOutput: MutableList<Float>? = null
    ) {
        // Not enough points to interpolate
        val hasValidZ = z == null || z.size == line.size / 2
        if (line.size % 4 != 0 || !hasValidZ) {
            lineOutput.addAll(line)
            zOutput?.addAll(z ?: emptyList())
            return
        }

        val squareMinSpacing = minSpacing * minSpacing


        for (i in line.indices step 4) {
            val x1 = line[i]
            val y1 = line[i + 1]
            val x2 = line[i + 2]
            val y2 = line[i + 3]
            val zIndex = 2 * (i / 4)
            val z1 = z?.get(zIndex)
            val z2 = z?.get(zIndex + 1)
            val dx = x2 - x1
            val dy = y2 - y1
            val dz = if (z1 != null && z2 != null) {
                z2 - z1
            } else {
                null
            }
            val squareDistance = dx * dx + dy * dy
            val segments = if (squareDistance < squareMinSpacing) {
                0
            } else {
                (sqrt(squareDistance) / minSpacing).toInt()
            }

            // Line is too short to interpolate, so keep it as is
            if (segments < 1) {
                lineOutput.add(x1)
                lineOutput.add(y1)
                lineOutput.add(x2)
                lineOutput.add(y2)
                z1?.let { zOutput?.add(it) }
                z2?.let { zOutput?.add(it) }
                continue
            }

            val xStep = dx / segments
            val yStep = dy / segments
            val zStep = (dz ?: 0f) / segments
            for (j in 0 until segments) {
                lineOutput.add(x1 + j * xStep)
                lineOutput.add(y1 + j * yStep)
                lineOutput.add(x1 + (j + 1) * xStep)
                lineOutput.add(y1 + (j + 1) * yStep)
                if (z1 != null) {
                    zOutput?.add(z1 + j * zStep)
                    zOutput?.add(z1 + (j + 1) * zStep)
                }
            }
        }

    }

}
