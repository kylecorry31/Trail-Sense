package com.kylecorry.trail_sense.shared.preferences

import android.content.Context
import com.kylecorry.andromeda.preferences.CachedPreferences
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.preferences.SharedPreferences

class SessionStateSubsystem private constructor(context: Context) {
    val preferences: IPreferences = CachedPreferences(SharedPreferences(context, SESSION_STATE))

    companion object {
        private const val SESSION_STATE = "session_state"
        private var instance: SessionStateSubsystem? = null
        fun getInstance(context: Context): SessionStateSubsystem {
            if (instance == null) {
                instance = SessionStateSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}
