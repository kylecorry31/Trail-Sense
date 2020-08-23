package com.kylecorry.trail_sense.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object IntentUtils {

    fun localIntent(context: Context, action: String): Intent {
        val i = Intent(action)
        i.`package` = context.packageName
        i.addCategory(Intent.CATEGORY_DEFAULT)
        return i
    }

    fun pendingIntentExists(context: Context, requestCode: Int, intent: Intent): Boolean {
        return PendingIntent.getBroadcast(
            context, requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE
        ) != null
    }

}