package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import android.view.View
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.pickers.Pickers
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object CoroutineAlerts {

    suspend fun dialog(
        context: Context,
        title: CharSequence,
        content: CharSequence? = null,
        contentView: View? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel),
        allowLinks: Boolean = false
    ) = suspendCoroutine<Boolean> { cont ->
        Alerts.dialog(
            context,
            title,
            content,
            contentView,
            okText,
            cancelText,
            allowLinks
        ) {
            cont.resume(it)
        }
    }

    suspend fun text(
        context: Context,
        title: CharSequence,
        description: CharSequence? = null,
        default: String? = null,
        hint: CharSequence? = null,
        okText: CharSequence? = context.getString(android.R.string.ok),
        cancelText: CharSequence? = context.getString(android.R.string.cancel)
    ) = suspendCoroutine<String?> { cont ->
        Pickers.text(
            context,
            title,
            description,
            default,
            hint,
            okText,
            cancelText
        ) {
            cont.resume(it)
        }
    }

}