package com.kylecorry.trail_sense.shared.canvas

class LineInterpolator {

    /**
     * Increase the resolution of a line
     * @param line The line to increase the resolution of (in the form [x1, y1, x2, y2, ...], where each 4 elements are a line segment).
     * @param lineOutput The output list to write the line to
     * @param minSpacing The minimum spacing between points to add
     * @param z The z values of the line. Each z value corresponds to a point in the line.
     * @param zOutput The output list to write the z values to
     */
    fun increaseResolution(
        line: List<Float>,
        lineOutput: MutableList<Float>,
        minSpacing: Float,
        z: List<Float>? = null,
        zOutput: MutableList<Float>? = null
    ) {
        val squareMinSpacing = minSpacing * minSpacing
        for (i in 0 until line.size - 4 step 4) {
            val x1 = line[i]
            val y1 = line[i + 1]
            val x2 = line[i + 2]
            val y2 = line[i + 3]
            val dx = x2 - x1
            val dy = y2 - y1
            val squareDistance = dx * dx + dy * dy
            val segments = (squareDistance / squareMinSpacing).toInt()
            val xStep = dx / segments
            val yStep = dy / segments
            val zStep = if (z != null) (z[i / 4 + 1] - z[i / 4]) / segments else 0f
            for (j in 0..segments) {
                lineOutput.add(x1 + j * xStep)
                lineOutput.add(y1 + j * yStep)
                if (z != null) {
                    zOutput?.add(z[i / 4] + j * zStep)
                }
            }
        }
    }

}