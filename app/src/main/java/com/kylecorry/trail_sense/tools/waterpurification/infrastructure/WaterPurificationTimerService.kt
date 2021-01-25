package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import kotlin.math.roundToInt

class WaterPurificationTimerService: Service() {

    private var timer: CountDownTimer? = null
    private var done = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.extras?.getLong(KEY_SECONDS, DEFAULT_SECONDS) ?: DEFAULT_SECONDS
        val builder = getNotificationBuilder()
            .setContentText(resources.getQuantityString(R.plurals.water_boil_timer_content, seconds.toInt(), seconds))
        startForeground(NOTIFICATION_ID, builder.build())
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
        val builder = getNotificationBuilder()

        timer = object: CountDownTimer(seconds * ONE_SECOND, ONE_SECOND){
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / ONE_SECOND.toFloat()).roundToInt()
                builder.setContentText(resources.getQuantityString(R.plurals.water_boil_timer_content, secondsLeft, secondsLeft))
                NotificationUtils.send(applicationContext, NOTIFICATION_ID, builder.build())
            }

            override fun onFinish() {
                val doneBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_tool_boil_done)
                    .setContentTitle(getString(R.string.water_boil_timer_done_title))
                    .setContentText(getString(R.string.water_boil_timer_done_content))
                    .setOnlyAlertOnce(false)
                NotificationUtils.send(applicationContext, NOTIFICATION_ID, doneBuilder.build())
                done = true
                stopForeground(false)
            }

        }.start()
    }

    private fun getNotificationBuilder(): NotificationCompat.Builder {


        val stopPendingIntent = WaterPurificationCancelReceiver.pendingIntent(this)

        val cancelAction = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(this, R.drawable.ic_cancel),
            getString(R.string.dialog_cancel),
            stopPendingIntent
        ).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tool_boil)
            .setContentTitle(getString(R.string.water_boil_timer_title))
            .setOnlyAlertOnce(true)
            .setNotificationSilent()
            .addAction(cancelAction)
            .setOngoing(true)
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