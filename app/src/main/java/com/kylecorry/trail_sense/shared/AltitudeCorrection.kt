package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.compression.CompressionUtils
import kotlin.math.roundToInt

/*
 *  The geoids.csv is from https://github.com/vectorstofinal/geoid_heights licensed under MIT
 */
object AltitudeCorrection {

    private val table = mutableMapOf<Pair<Int, Int>, Short>()
    private val lock = Object()

    fun getOffset(location: Coordinate?, context: Context?): Float {
        if (location == null || context == null) {
            return 0f
        }

        val loc = Pair(location.latitude.roundToInt(), location.longitude.roundToInt())


        if (table.containsKey(loc)) {
            return table[loc]?.toFloat() ?: 0f
        }

        synchronized(lock) {
            val offset = loadOffset(context, loc)
            if (offset != null) {
                table[loc] = offset
            }
        }

        return table[loc]?.toFloat() ?: 0f
    }


    private fun loadOffset(context: Context, key: Pair<Int, Int>): Short? {
        val input = context.resources.openRawResource(R.raw.geoids)
        val line = ((90 + key.first) * 361 + (180 + key.second))
        return CompressionUtils.getShort(input, line)
    }

}