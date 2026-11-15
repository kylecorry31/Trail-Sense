package com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

internal class FakeNavigationBearingDao : NavigationBearingDao {
    private val entities = MutableStateFlow<List<NavigationBearingEntity>>(emptyList())
    private var nextId = 1L

    fun all(): List<NavigationBearingEntity> = entities.value

    override fun getActiveBearingFlow() =
        flow {
            entities.collect { list -> emit(list.firstOrNull { it.isActive }) }
        }

    override suspend fun clearActiveBearing() {
        entities.value = entities.value.map { it.copy(isActive = false).also { c -> c.id = it.id } }
    }

    override suspend fun upsert(bearing: NavigationBearingEntity): Long {
        val id = if (bearing.id == 0L) nextId++ else bearing.id
        val saved = bearing.copy().also { it.id = id }
        entities.value = entities.value.filter { it.id != id } + saved
        return id
    }

    override suspend fun deleteOlderThan(minEpochMillis: Long) {
        entities.value = entities.value.filter {
            it.isActive || it.startTime == null || it.startTime >= minEpochMillis
        }
    }
}
