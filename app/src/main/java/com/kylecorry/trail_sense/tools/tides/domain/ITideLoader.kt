package com.kylecorry.trail_sense.tools.tides.domain

interface ITideLoader {
    suspend fun getReferenceTide(): TideEntity?
}