package com.kylecorry.trail_sense.shared.sensors2

abstract class AbstractSensor: ISensor {

    private val listeners = mutableSetOf<SensorListener>()
    private var started = false

    override fun start(listener: SensorListener){
        listeners.add(listener)
        if (started) return
        startImpl()
        started = true
    }

    override fun stop(listener: SensorListener?){
        if (listener != null) {
            listeners.remove(listener)
        } else {
            listeners.clear()
        }
        if (listeners.isNotEmpty()) return
        if (!started) return
        stopImpl()
        started = false
    }

    protected abstract fun startImpl()
    protected abstract fun stopImpl()

    protected fun notifyListeners(){
        for (listener in listeners){
            listener.invoke()
        }
    }

}