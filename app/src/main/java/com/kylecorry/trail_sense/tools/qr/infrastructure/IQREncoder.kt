package com.kylecorry.trail_sense.tools.qr.infrastructure

interface IQREncoder<T> {

    fun encode(value: T): String

    fun decode(qr: String): T?

}