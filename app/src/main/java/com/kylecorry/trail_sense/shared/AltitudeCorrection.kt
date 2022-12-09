package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.andromeda.compression.CompressionUtils
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import kotlin.math.roundToInt

/*
 *  The geoids.csv is from https://github.com/vectorstofinal/geoid_heights licensed under MIT
 */
object AltitudeCorrection {

    private val table = mutableMapOf<Pair<Int, Int>, Byte>()
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


    private fun loadOffset(context: Context, key: Pair<Int, Int>): Byte? {
        val input = context.resources.openRawResource(R.raw.geoids)
        val line = ((90 + key.first) * 361 + (180 + key.second))
        return CompressionUtils.getBytes(input, line, 1)?.get(0)
    }

}