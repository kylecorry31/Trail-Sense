package com.kylecorry.trail_sense.shared.sensors.temperature

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.kylecorry.trail_sense.shared.sensors.AbstractSensor

class BatteryTemperatureSensor(private val context: Context): AbstractSensor(), IThermometer {

    override val temperature: Float
        get() = _temp
    override val hasValidReading: Boolean
        get() = _hasReading

    private var _temp = Float.NaN
    private var _hasReading = false

    private val receiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            println(temp)
            _temp = temp.toFloat() / 10f
            _hasReading = true
            notifyListeners()
        }
    }

    override fun startImpl() {
        context.registerReceiver(receiver, IntentFilter("android.intent.action.BATTERY_CHANGED"))
    }

    override fun stopImpl() {
        context.unregisterReceiver(receiver)
    }
}