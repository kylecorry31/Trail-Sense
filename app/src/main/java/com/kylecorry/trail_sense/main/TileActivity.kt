package com.kylecorry.trail_sense.main

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

class TileActivity : AndromedaActivity() {

    override fun onResume() {
        super.onResume()
        val tileId = intent.getIntExtra(EXTRA_TILE_ID, 0)
        inBackground(
            state = BackgroundMinimumState.Created,
            cancelWhenBelowState = false
        ) {
            try {
                when (tileId) {
                    TILE_ID_BACKTRACK -> {
                        BacktrackSubsystem.getInstance(this@TileActivity).enable(true)
                    }

                    TILE_ID_WEATHER -> {
                        WeatherSubsystem.getInstance(this@TileActivity).enableMonitor()
                    }

                    TILE_ID_PEDOMETER -> {
                        PedometerSubsystem.getInstance(this@TileActivity).enable()
                    }
                }
            } finally {
                onMain {
                    finishAndRemoveTask()
                }
            }
        }
    }

    companion object {
        const val EXTRA_TILE_ID = "tile_id"

        const val TILE_ID_BACKTRACK = 1
        const val TILE_ID_WEATHER = 2
        const val TILE_ID_PEDOMETER = 3

        fun pendingIntent(context: Context, id: Int): PendingIntent {
            val intent = Intent(context, TileActivity::class.java)
            intent.putExtra(EXTRA_TILE_ID, id)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return PendingIntent.getActivity(
                context,
                348058223,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

    }

}