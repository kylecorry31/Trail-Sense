package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.ui.setOnProgressChangeListener
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

class DateTimeSliderSheet(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val formatter = getAppService<FormatService>()
    private val hooks = Hooks(
        stateThrottleMs = 300,
        stateTriggerOnStart = false,
        stateOnChange = {
            onTimeChanged?.invoke(currentTime.toInstant())
        }
    )
    private var trigger by hooks.state("")

    var onTimeChanged: ((Instant?) -> Unit)? = null

    private val datePicker: DatePickerView
    private val timeText: TextView
    private val timeSlider: SeekBar

    private var currentTime: ZonedDateTime = ZonedDateTime.now()

    init {
        inflate(context, R.layout.view_date_time_slider_sheet, this)
        datePicker = findViewById(R.id.date_picker)
        timeText = findViewById(R.id.time_text)
        timeSlider = findViewById(R.id.time_slider)

        datePicker.setOnDateChangeListener {
            currentTime = currentTime.with(it)
            updateUI()
            notifyChanged()
        }

        timeSlider.setOnProgressChangeListener { progress, fromUser ->
            if (fromUser) {
                val seconds = (progress * 60L).coerceAtMost(LocalTime.MAX.toSecondOfDay().toLong())
                val time = LocalTime.ofSecondOfDay(seconds)
                currentTime = currentTime.with(time)
                updateTimeText()
                notifyChanged()
            }
        }

        timeText.setOnClickListener {
            val prefs = UserPreferences(context)
            CustomUiUtils.pickTime(context, prefs.use24HourTime, currentTime.toLocalTime()) {
                if (it != null) {
                    currentTime = currentTime.with(it)
                    updateUI()
                    notifyChanged()
                }
            }
        }
    }

    fun setTime(instant: Instant?) {
        currentTime = instant?.toZonedDateTime() ?: ZonedDateTime.now()
        updateUI()
    }

    private fun updateUI() {
        datePicker.date = currentTime.toLocalDate()
        updateTimeText()
        timeSlider.progress = currentTime.hour * 60 + currentTime.minute
    }

    private fun updateTimeText() {
        timeText.text = formatter.formatTime(currentTime.toLocalTime(), includeSeconds = false)
    }

    private fun notifyChanged() {
        trigger = UUID.randomUUID().toString()
    }

    fun show() {
        hooks.startStateUpdates()
        isVisible = true
    }

    fun hide() {
        hooks.stopStateUpdates()
        isVisible = false
    }
}
