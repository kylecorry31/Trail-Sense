package com.kylecorry.trail_sense.shared.extensions

import android.content.res.Resources
import android.os.Build
import java.util.*

class StringCache(private val resources: Resources) {

    private var lastLocale = getLocale()
    private val cache = mutableMapOf<Int, String>()
    private val lock = Any()

    fun getString(id: Int): String {
        synchronized(lock) {
            val currentLocale = getLocale()
            if (lastLocale != currentLocale) {
                cache.clear()
                lastLocale = currentLocale
            }

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
        return String.format(lastLocale, raw, *formatArgs)
    }

    private fun getLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
    }
}