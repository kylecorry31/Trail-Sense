package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import com.kylecorry.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trailsensecore.infrastructure.services.ForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import kotlin.math.roundToInt

class WaterPurificationTimerService : ForegroundService() {

    private var timer: CountDownTimer? = null
    private var done = false
    private val notify by lazy { Notify(this) }

    private var seconds = DEFAULT_SECONDS

    private val cancelAction by lazy {
        notify.action(
            getString(R.string.dialog_cancel),
            WaterPurificationCancelReceiver.pendingIntent(applicationContext),
            R.drawable.ic_cancel
        )
    }

    private val openIntent by lazy {
        NavigationUtils.pendingIntent(
            this@WaterPurificationTimerService,
            R.id.waterPurificationFragment
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        seconds = intent?.extras?.getLong(KEY_SECONDS, DEFAULT_SECONDS) ?: DEFAULT_SECONDS
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        timer?.cancel()
        if (!done) {
            notify.cancel(NOTIFICATION_ID)
        }
        stopService(false)
        super.onDestroy()

    }

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        startTimer(seconds)
        return START_NOT_STICKY
    }

    override val foregroundNotificationId: Int
        get() = NOTIFICATION_ID

    override fun getForegroundNotification(): Notification {
        return getNotification(seconds.toInt())
    }


    private fun startTimer(seconds: Long) {
        timer = object : CountDownTimer(seconds * ONE_SECOND, ONE_SECOND) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / ONE_SECOND.toFloat()).roundToInt()
                notify.send(
                    NOTIFICATION_ID,
                    getNotification(secondsLeft)
                )
            }

            override fun onFinish() {
                val notification = notify.alert(
                    CHANNEL_ID,
                    getString(R.string.water_boil_timer_done_title),
                    getString(R.string.water_boil_timer_done_content),
                    R.drawable.ic_tool_boil_done,
                    group = NotificationChannels.GROUP_WATER,
                    intent = openIntent
                )
                notify.send(NOTIFICATION_ID, notification)
                done = true
                stopForeground(false)
            }

        }.start()
    }

    private fun getNotification(secondsLeft: Int): Notification {
        return notify.persistent(
            CHANNEL_ID,
            getString(R.string.water_boil_timer_title),
            resources.getQuantityString(
                R.plurals.water_boil_timer_content,
                secondsLeft,
                secondsLeft
            ),
            R.drawable.ic_tool_boil,
            group = NotificationChannels.GROUP_WATER,
            actions = listOf(cancelAction),
            intent = openIntent
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

        fun stop(context: Context) {
            context.stopService(intent(context))
        }
    }
}