package com.kylecorry.trail_sense.test_utils

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import dagger.hilt.android.testing.CustomTestApplication

open class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

}

@CustomTestApplication(MyApplication::class)
interface MyTestApplication