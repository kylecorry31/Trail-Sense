package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trail_sense.shared.views.QuickActionNone

class QuickActionFactory {

    fun create(type: QuickActionType, button: ImageButton, fragment: AndromedaFragment): QuickActionButton {
        return when (type){
            QuickActionType.None -> QuickActionNone(button, fragment)
            QuickActionType.Paths -> QuickActionPaths(button, fragment)
            QuickActionType.Flashlight -> QuickActionFlashlight(button, fragment)
            QuickActionType.Clouds -> QuickActionClouds(button, fragment)
            QuickActionType.Temperature -> QuickActionTemperatureEstimation(button, fragment)
            QuickActionType.Ruler -> QuickActionRuler(button, fragment)
            QuickActionType.Maps -> QuickActionPhotoMaps(button, fragment)
            QuickActionType.Whistle -> QuickActionWhistle(button, fragment)
            QuickActionType.WhiteNoise -> QuickActionWhiteNoise(button, fragment)
            QuickActionType.LowPowerMode -> LowPowerQuickAction(button, fragment)
            QuickActionType.Thunder -> QuickActionThunder(button, fragment)
            QuickActionType.Climate -> QuickActionClimate(button, fragment)
            QuickActionType.SunsetAlert -> QuickActionSunsetAlert(button, fragment)
            QuickActionType.NightMode -> QuickActionNightMode(button, fragment)
            QuickActionType.Backtrack -> QuickActionBacktrack(button, fragment)
            QuickActionType.WeatherMonitor -> QuickActionWeatherMonitor(button, fragment)
            QuickActionType.Pedometer -> QuickActionPedometer(button, fragment)
            QuickActionType.ScreenFlashlight -> QuickActionScreenFlashlight(button, fragment)
        }
    }

}