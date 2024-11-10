package com.kylecorry.trail_sense.shared.extensions

import android.content.Context
import android.graphics.drawable.Icon
import android.widget.RemoteViews

fun RemoteViews.setImageViewResourceAsIcon(context: Context, viewId: Int, resourceId: Int) {
    setImageViewIcon(viewId, Icon.createWithResource(context, resourceId))
}