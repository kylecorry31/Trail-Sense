package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import com.kylecorry.andromeda.background.services.AndromedaService
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.AlarmAlerter
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.tools.waterpurification.WaterBoilTimerToolRegistration

class WaterPurificationTimerService : AndromedaService() {

    private var timer: CountDownTimer? = null
    private var done = false

    private var seconds = DEFAULT_SECONDS

    private val cancelAction by lazy {
        Notify.action(
            getString(android.R.string.cancel),
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
        super.onStartCommand(intent, flags, startId)
        startTimer(seconds)
        return START_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        if (!done) {
            Notify.cancel(this, NOTIFICATION_ID)
        } else {
            val prefs = AppServiceRegistry.get<UserPreferences>()
            val useAlarm = prefs.waterBoilTimer.useAlarm
            val notification = Notify.alert(
                this@WaterPurificationTimerService,
                CHANNEL_ID,
                getString(R.string.water_boil_timer_done_title),
                getString(R.string.water_boil_timer_done_content),
                R.drawable.ic_tool_boil_done,
                group = NOTIFICATION_GROUP_WATER,
                intent = openIntent,
                mute = useAlarm
            )
            AppServiceRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, notification)

            val alarm = AlarmAlerter(
                this,
                useAlarm,
                WaterBoilTimerToolRegistration.NOTIFICATION_CHANNEL_WATER_BOIL_TIMER
            )
            alarm.alert()
        }
        stopService(false)
        super.onDestroy()

    }

    override fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, getNotification(seconds.toInt()))
    }

    private fun startTimer(seconds: Long) {
        timer = object : CountDownTimer(seconds * ONE_SECOND, ONE_SECOND) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / ONE_SECOND.toFloat()).safeRoundToInt()
                Notify.update(
                    this@WaterPurificationTimerService,
                    NOTIFICATION_ID,
                    getNotification(secondsLeft)
                )
            }

            override fun onFinish() {
                done = true
                stopService(false)
            }

        }.start()
    }

    private fun getNotification(secondsLeft: Int): Notification {
        return Notify.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.water_boil_timer_title),
            resources.getQuantityString(
                R.plurals.water_boil_timer_content,
                secondsLeft,
                secondsLeft
            ),
            R.drawable.ic_tool_boil,
            group = NOTIFICATION_GROUP_WATER,
            actions = listOf(cancelAction),
            intent = openIntent,
            showForegroundImmediate = true
        )
    }

    companion object {

        const val CHANNEL_ID = "Water_Boil_Timer"
        const val NOTIFICATION_ID = 57293759
        private const val ONE_SECOND = 1000L
        private const val KEY_SECONDS = "seconds"
        private const val DEFAULT_SECONDS = 60L
        private const val NOTIFICATION_GROUP_WATER = "trail_sense_water"

        fun intent(context: Context, seconds: Long = DEFAULT_SECONDS): Intent {
            val i = Intent(context.applicationContext, WaterPurificationTimerService::class.java)
            i.putExtra(KEY_SECONDS, seconds)
            return i
        }

        fun start(context: Context, seconds: Long) {
            Intents.startService(context.applicationContext, intent(context, seconds), true)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }
    }
}