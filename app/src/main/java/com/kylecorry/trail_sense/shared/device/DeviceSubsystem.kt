package com.kylecorry.trail_sense.shared.device

import android.app.ActivityManager
import android.content.Context

class DeviceSubsystem(private val context: Context) {

    fun isLowMemory(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        if (activityManager != null) {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return memoryInfo.lowMemory
        } else {
            return true
        }
    }

    fun getAvailableMemoryBytes(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        if (activityManager != null) {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return memoryInfo.availMem
        } else {
            return 0
        }
    }

}