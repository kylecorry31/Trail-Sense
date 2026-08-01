package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.system.UriAccess
import com.kylecorry.andromeda.core.system.createFile
import com.kylecorry.andromeda.core.system.pickFile
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IntentUriPicker(private val resolver: IntentResultRetriever, private val context: Context) :
    UriPicker {

    private val files = getAppService<FileSubsystem>()
    private val prefs = getAppService<PreferencesSubsystem>().preferences

    override suspend fun open(types: List<String>, requirePersistentAccess: Boolean): Uri? {
        return suspendCancellableCoroutine { cont ->
            resolver.pickFile(
                types,
                context.getString(R.string.pick_file),
                access = UriAccess(
                    requirePersistentAccess = requirePersistentAccess,
                    requireReadAccess = true
                )
            ) {
                val readResult = it?.let { uri -> files.canRead(uri) }
                if (readResult != null) {
                    prefs.putBoolean(KEY_HAS_EXTERNAL_STORAGE_DENIAL, !readResult.canRead)
                    if (readResult.deniedByPackage != null) {
                        prefs.putString(KEY_EXTERNAL_STORAGE_DENIED_BY, readResult.deniedByPackage)
                    } else {
                        prefs.remove(KEY_EXTERNAL_STORAGE_DENIED_BY)
                    }
                }

                if (readResult?.canRead == false) {
                    Log.e("IntentUriPicker", "Read permission was not granted")
                    cont.resume(null)
                } else {
                    cont.resume(it)
                }
            }
        }
    }

    override suspend fun create(filename: String, type: String): Uri? {
        return suspendCancellableCoroutine { cont ->
            resolver.createFile(filename, listOf(type), context.getString(R.string.pick_file)) {
                cont.resume(it)
            }
        }
    }

    companion object {
        const val KEY_HAS_EXTERNAL_STORAGE_DENIAL = "cache_has_external_storage_denial"
        const val KEY_EXTERNAL_STORAGE_DENIED_BY = "cache_external_storage_denied_by"
    }
}
