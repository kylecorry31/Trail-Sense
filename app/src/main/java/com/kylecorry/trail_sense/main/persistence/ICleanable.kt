package com.kylecorry.trail_sense.main.persistence

interface ICleanable {
    suspend fun clean()
}