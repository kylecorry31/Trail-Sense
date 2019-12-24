package com.kylecorry.survival_aid

import android.hardware.Sensor
import android.os.Build

object Constants {

    var TEMP_SENSOR = Sensor.TYPE_AMBIENT_TEMPERATURE
    var TEMP_OFFSET = 0

    init {
        println(Build.MANUFACTURER + " " + Build.MODEL)
        if (Build.MANUFACTURER.toLowerCase() == "google" && Build.MODEL.toLowerCase() == "pixel 3a"){
            TEMP_SENSOR = 33172002
            TEMP_OFFSET = -10
        }
    }

}