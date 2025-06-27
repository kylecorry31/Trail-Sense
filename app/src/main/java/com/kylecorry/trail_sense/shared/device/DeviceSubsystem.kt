package com.kylecorry.trail_sense.shared.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

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

    fun getAvailableBitmapMemoryBytes(): Long {
        // Android O and higher store bitmaps in native heap
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityService = context.getSystemService<ActivityManager>()
            activityService?.getMemoryInfo(memoryInfo)
            memoryInfo.availMem
        } else {
            val runtime = Runtime.getRuntime()
            runtime.freeMemory()
        }
    }

}