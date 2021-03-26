package com.kylecorry.trail_sense.shared.services

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CoroutineForegroundService: ForegroundService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            doWork()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    abstract suspend fun doWork()
}