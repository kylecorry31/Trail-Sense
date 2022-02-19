package com.kylecorry.trail_sense.shared

import androidx.annotation.IdRes

interface IAppNavigation {
    fun navigate(@IdRes actionId: Int, params: List<Pair<String, Any?>> = emptyList())
}