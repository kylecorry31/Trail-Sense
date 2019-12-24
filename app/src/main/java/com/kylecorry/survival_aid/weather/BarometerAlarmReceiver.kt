package com.kylecorry.survival_aid.weather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*

class BarometerAlarmReceiver: BroadcastReceiver(), Observer {

    private lateinit var barometer: Barometer

    override fun update(o: Observable?, arg: Any?) {
        PressureHistory.addReading(barometer.pressure)
        println(barometer.pressure)

        if (WeatherUtils.isStormIncoming(PressureHistory.readings)){
            println("STORM INCOMING!")
        }
        barometer.stop()
        barometer.deleteObserver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null){
            barometer = Barometer(context)
            barometer.addObserver(this)
            barometer.start()
        }
    }
}