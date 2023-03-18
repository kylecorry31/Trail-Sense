package com.kylecorry.trail_sense.shared.extensions

import android.content.res.Resources
import android.os.Build

class StringCache(private val resources: Resources) {

    private val cache = mutableMapOf<Int, String>()
    private val lock = Any()

    fun getString(id: Int): String {
        synchronized(lock) {
            if (cache.containsKey(id)) {
                return cache[id]!!
            }
            val string = resources.getString(id)
            cache[id] = string
            return string
        }
    }

    fun getString(id: Int, vararg formatArgs: Any): String {
        val raw = getString(id)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val locale = resources.configuration.locales[0]
            String.format(locale, raw, *formatArgs)
        } else {
            @Suppress("DEPRECATION")
            String.format(resources.configuration.locale, raw, *formatArgs)
        }
    }
}