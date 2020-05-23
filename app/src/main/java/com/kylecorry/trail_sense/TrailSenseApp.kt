package com.kylecorry.trail_sense

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class TrailSenseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}