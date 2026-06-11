package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.system.UriAccess
import com.kylecorry.andromeda.core.system.createFile
import com.kylecorry.andromeda.core.system.pickFile
import com.kylecorry.trail_sense.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IntentUriPicker(private val resolver: IntentResultRetriever, private val context: Context) :
    UriPicker {
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
                cont.resume(it)
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
}
