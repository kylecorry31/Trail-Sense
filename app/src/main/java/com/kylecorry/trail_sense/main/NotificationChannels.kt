package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object NotificationChannels {

    // Legacy (intended for deletion)
    private const val CHANNEL_BACKGROUND_UPDATES = "background_updates"
    private const val CHANNEL_BACKGROUND_LAUNCHER = "background_launcher"

    fun createChannels(context: Context) {
        val tools = Tools.getTools(context)
        val channelGroups = tools.flatMap { it.notificationChannelGroups }
        val channels = tools.flatMap { it.notificationChannels }

        channelGroups.forEach {
            Notify.createChannelGroup(context, it.id, it.name, it.description)
        }

        channels.forEach {
            Notify.createChannel(
                context,
                it.id,
                it.name,
                it.description,
                it.importance,
                muteSound = it.muteSound,
                showBadge = it.showBadge,
                isAlarm = it.isAlarm,
                groupId = it.groupId
            )
        }

        // CHANNEL CLEANUP SECTION
        Notify.deleteChannel(context, CHANNEL_BACKGROUND_UPDATES)
        Notify.deleteChannel(context, CHANNEL_BACKGROUND_LAUNCHER)
    }

}
