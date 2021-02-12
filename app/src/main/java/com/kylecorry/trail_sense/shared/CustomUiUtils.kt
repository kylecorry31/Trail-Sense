package com.kylecorry.trail_sense.shared

import android.content.Context
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.units.Quality
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

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
        return when (quality){
            Quality.Poor, Quality.Unknown -> UiUtils.color(context, R.color.red)
            Quality.Moderate -> UiUtils.color(context, R.color.yellow)
            Quality.Good -> UiUtils.color(context, R.color.green)
        }
    }

}