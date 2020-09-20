package com.kylecorry.trail_sense.shared

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import java.util.stream.Collectors
import kotlin.math.roundToInt

/*
 *  The geoids.csv is from https://github.com/vectorstofinal/geoid_heights licensed under MIT
 */
object AltitudeCorrection {

    private val table = mutableMapOf<Pair<Int, Int>, Float>()

    fun getOffset(location: Coordinate, context: Context): Float {
        if (table.isEmpty()){
            loadTable(context)
        }

        val loc = Pair(location.latitude.roundToInt(), location.longitude.roundToInt())
        return table[loc] ?: 0f
    }

    private fun loadTable(context: Context){
        val input = context.resources.openRawResource(R.raw.geoids)
        val lines = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            input.bufferedReader().lines().map { it.split(",") }.collect(Collectors.toList())
        } else {
            with(input.bufferedReader()){
                val lines = mutableListOf<String>()
                while(this.ready()){
                    lines.add(this.readLine())
                }
                lines.map { it.split(",") }
            }
        }
        table.clear()
        for (line in lines){
            table[Pair(line[0].toInt(), line[1].toInt())] = line[2].toFloat()
        }
        input.close()
    }


}