package com.kylecorry.trail_sense.shared.canvas

class LineInterpolator {

    /**
     * Increase the resolution of a line
     * @param line The line to increase the resolution of (in the form [x1, y1, x2, y2, ...], where each 4 elements are a line segment).
     * @param lineOutput The output list to write the line to
     * @param minSpacing The minimum spacing between points to add. The actual spacing may be larger than this value to ensure the spacing is even.
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
        // Not enough points to interpolate
        if (line.size % 4 != 0){
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
            val dx = x2 - x1
            val dy = y2 - y1
            val squareDistance = dx * dx + dy * dy
            val segments = (squareDistance / squareMinSpacing).toInt()

            // Line is too short to interpolate, so keep it as is
            if (segments < 1){
                lineOutput.add(x1)
                lineOutput.add(y1)
                lineOutput.add(x2)
                lineOutput.add(y2)
                if (z != null) {
                    zOutput?.add(z[i / 4])
                    zOutput?.add(z[i / 4 + 1])
                }
                continue
            }

            val xStep = dx / segments
            val yStep = dy / segments
            val zStep = if (z != null) (z[i / 4 + 1] - z[i / 4]) / segments else 0f
            for (j in 0 until segments) {
                lineOutput.add(x1 + j * xStep)
                lineOutput.add(y1 + j * yStep)
                lineOutput.add(x1 + (j + 1) * xStep)
                lineOutput.add(y1 + (j + 1) * yStep)
                if (z != null) {
                    zOutput?.add(z[i / 4] + j * zStep)
                    zOutput?.add(z[i / 4] + (j + 1) * zStep)
                }
            }
        }

    }

}