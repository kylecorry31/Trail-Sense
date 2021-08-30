package com.kylecorry.trail_sense.shared.database

abstract class Dto<T> {

    protected val finalProperties: MutableMap<String, Any?> = mutableMapOf()

    abstract fun getProperties(): Map<String, SqlType>

    fun set(property: String, value: Any?) {
        finalProperties[property] = value
    }

    abstract fun toObject(): T
}