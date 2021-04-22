package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.math.toFloatCompat
import kotlin.math.roundToInt

/*
 *  The geoids.csv is from https://github.com/vectorstofinal/geoid_heights licensed under MIT
 */
object AltitudeCorrection {

    private val table = mutableMapOf<Pair<Int, Int>, Float>()
    private val lock = Object()

    fun getOffset(location: Coordinate?, context: Context?): Float {
        if (location == null || context == null) {
            return 0f
        }

        val loc = Pair(location.latitude.roundToInt(), location.longitude.roundToInt())


        if (table.containsKey(loc)) {
            return table[loc] ?: 0f
        }

        synchronized(lock) {
            val offset = loadOffset(context, loc)
            if (offset != null) {
                table[loc] = offset
            }
        }

        return table[loc] ?: 0f
    }


    private fun loadOffset(context: Context, key: Pair<Int, Int>): Float? {
        // TODO: Seek close to the desired line
        val input = context.resources.openRawResource(R.raw.geoids)
        var offset: Float? = null
        val desiredLine = (90 + key.first) * 361 + (180 + key.second)
        var i = 0
        input.bufferedReader().use {
            while (it.ready()) {
                val line = it.readLine()
                if (i != desiredLine){
                    i++
                    continue
                }
                offset = line.trim().toFloatCompat()
                break
            }
        }

        try {
            input.close()
        } catch (e: Exception) {
        }

        return offset
    }


}