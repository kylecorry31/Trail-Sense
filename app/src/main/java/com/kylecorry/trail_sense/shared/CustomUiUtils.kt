package com.kylecorry.trail_sense.shared

import android.app.TimePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.ImageButton
import android.widget.TimePicker
import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.units.Quality
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import java.time.LocalTime

object CustomUiUtils {

    fun setButtonState(button: ImageButton, state: Boolean) {
        UiUtils.setButtonState(
            button,
            state,
            UiUtils.color(button.context, R.color.colorPrimary),
            UiUtils.color(button.context, R.color.colorSecondary)
        )
    }

    fun setButtonState(
        button: Button,
        isOn: Boolean
    ) {
        if (isOn) {
            button.setTextColor(UiUtils.color(button.context, R.color.colorSecondary))
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.color(button.context, R.color.colorPrimary))
        } else {
            button.setTextColor(UiUtils.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.androidBackgroundColorSecondary(button.context))
        }
    }

    @ColorInt
    fun getQualityColor(context: Context, quality: Quality): Int {
        return when (quality) {
            Quality.Poor, Quality.Unknown -> UiUtils.color(context, R.color.red)
            Quality.Moderate -> UiUtils.color(context, R.color.yellow)
            Quality.Good -> UiUtils.color(context, R.color.green)
        }
    }

    fun pickTime(
        context: Context,
        use24Hours: Boolean,
        default: LocalTime = LocalTime.now(),
        onTimePick: (time: LocalTime?) -> Unit
    ) {
        val timePickerDialog = TimePickerDialog(
            context,
            { timePicker: TimePicker, hour: Int, minute: Int ->
                val time = LocalTime.of(hour, minute)
                onTimePick.invoke(time)
            },
            default.hour,
            default.minute,
            use24Hours
        )
        timePickerDialog.setOnCancelListener {
            onTimePick.invoke(null)
        }
        timePickerDialog.show()
    }

}