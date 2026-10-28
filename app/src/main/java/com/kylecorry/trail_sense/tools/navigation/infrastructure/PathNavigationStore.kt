package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSession

class PathNavigationStore {
    private val prefs = getAppService<SessionStateSubsystem>().preferences

    fun hasSavedRoute(): Boolean = prefs.getString(SESSION_KEY) != null

    fun save(session: PathNavigationSession) {
        prefs.putString(SESSION_KEY, JsonConvert.toJson(session))
    }

    fun load(): PathNavigationSession? {
        val saved = prefs.getString(SESSION_KEY) ?: return null
        return requireNotNull(JsonConvert.fromJson<PathNavigationSession>(saved))
    }

    fun clear() {
        prefs.remove(SESSION_KEY)
    }

    private companion object {
        const val SESSION_KEY = "path_navigation_route"
    }
}
