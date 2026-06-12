package com.kylecorry.trail_sense.shared.events

import android.os.Bundle

interface IBundleEventBus {
    fun broadcast(toolBroadcastId: String, data: Bundle? = null)
    fun subscribe(toolBroadcastId: String, callback: suspend (Bundle) -> Unit)
    fun unsubscribe(toolBroadcastId: String, callback: suspend (Bundle) -> Unit)
}
