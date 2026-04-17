package com.kylecorry.trail_sense.shared.navigation

import android.os.Bundle
import androidx.navigation.NavController

class NavControllerAppNavigation(private val controller: NavController) : IAppNavigation {
    override fun navigate(actionId: Int, params: Bundle) {
        controller.navigate(actionId, params)
    }
}