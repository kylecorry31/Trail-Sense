package com.kylecorry.trail_sense.plugins.plugins

import android.content.Context

interface PluginRegistration {

    fun getPlugin(context: Context): Plugin

}