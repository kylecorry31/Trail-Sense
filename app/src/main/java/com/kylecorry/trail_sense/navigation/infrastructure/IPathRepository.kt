package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.navigation.domain.Path

interface IPathRepository {

    fun getAll(): List<Path>

    fun get(name: String): Path?

    fun create(path: Path)

    fun update(path: Path)

    fun delete(path: Path)

}