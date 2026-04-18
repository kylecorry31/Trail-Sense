package com.kylecorry.trail_sense.shared.navigation

import android.os.Bundle
import androidx.annotation.IdRes

interface IAppNavigation {
    fun navigate(@IdRes actionId: Int, params: Bundle = Bundle())
}
