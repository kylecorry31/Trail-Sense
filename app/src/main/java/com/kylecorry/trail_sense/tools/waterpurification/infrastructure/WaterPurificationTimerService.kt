package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import kotlin.math.roundToInt

class WaterPurificationTimerService: Service() {

    private var timer: CountDownTimer? = null
    private var done = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.extras?.getLong(KEY_SECONDS, DEFAULT_SECONDS) ?: DEFAULT_SECONDS
        startForeground(NOTIFICATION_ID, getNotification(seconds.toInt()))
        startTimer(seconds)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        if (!done) {
            NotificationUtils.cancel(this, NOTIFICATION_ID)
        }
        stopForeground(false)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun startTimer(seconds: Long){
        timer = object: CountDownTimer(seconds * ONE_SECOND, ONE_SECOND){
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / ONE_SECOND.toFloat()).roundToInt()
                NotificationUtils.send(applicationContext, NOTIFICATION_ID, getNotification(secondsLeft))
            }

            override fun onFinish() {
                val notification = NotificationUtils.alert(
                    applicationContext,
                    CHANNEL_ID,
                    getString(R.string.water_boil_timer_done_title),
                    getString(R.string.water_boil_timer_done_content),
                    R.drawable.ic_tool_boil_done,
                    group = NotificationChannels.GROUP_WATER
                )
                NotificationUtils.send(applicationContext, NOTIFICATION_ID, notification)
                done = true
                stopForeground(false)
            }

        }.start()
    }

    private fun getNotification(secondsLeft: Int): Notification {
        val cancelAction = NotificationUtils.action(
            getString(R.string.dialog_cancel),
            WaterPurificationCancelReceiver.pendingIntent(applicationContext),
            R.drawable.ic_cancel
        )

        return NotificationUtils.persistent(
            applicationContext,
            CHANNEL_ID,
            getString(R.string.water_boil_timer_title),
            resources.getQuantityString(R.plurals.water_boil_timer_content, secondsLeft, secondsLeft),
            R.drawable.ic_tool_boil,
            group = NotificationChannels.GROUP_WATER,
            actions = listOf(cancelAction)
        )
    }

    companion object {

        const val CHANNEL_ID = "Water_Boil_Timer"
        private const val NOTIFICATION_ID = 57293759
        private const val ONE_SECOND = 1000L
        private const val KEY_SECONDS = "seconds"
        private const val DEFAULT_SECONDS = 60L

        fun intent(context: Context, seconds: Long = DEFAULT_SECONDS): Intent {
            val i = Intent(context, WaterPurificationTimerService::class.java)
            i.putExtra(KEY_SECONDS, seconds)
            return i
        }

        fun start(context: Context, seconds: Long) {
            IntentUtils.startService(context, intent(context, seconds), true)
        }

        fun stop(context: Context){
            context.stopService(intent(context))
        }
    }

}