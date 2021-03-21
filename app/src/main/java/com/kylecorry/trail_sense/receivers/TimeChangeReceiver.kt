package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

class TimeChangeReceiver : BroadcastReceiver() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onReceive(context: Context?, intent: Intent?) {
        val validIntentActions = listOf(
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )
        if (validIntentActions.contains(intent?.action) && context != null) {
            Log.d("TimeChangeReceiver", "Time Change Receiver Called - ${intent?.action}")
            TrailSenseServiceUtils.restartServices(context)
//            val pressureRepo = PressureRepo.getInstance(context)
//
//            serviceScope.launch {
//                withContext(Dispatchers.IO){
//                    pressureRepo.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)))
//                }
//            }
        }
    }
}