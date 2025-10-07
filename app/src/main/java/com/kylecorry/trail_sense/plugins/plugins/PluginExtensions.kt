package com.kylecorry.trail_sense.plugins.plugins

import com.kylecorry.andromeda.json.JsonConvert

fun Any.toJson(): String {
    return JsonConvert.toJson(this)
}

fun Any.toJsonBytes(): ByteArray {
    return JsonConvert.toJson(this).toByteArray()
}

inline fun <reified T> ByteArray.fromJson(): T? {
    val json = this.toString(Charsets.UTF_8)
    return JsonConvert.fromJson<T>(json)
}