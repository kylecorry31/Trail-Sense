package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import android.os.Bundle

interface Action {
    suspend fun onReceive(context: Context, data: Bundle)
}
