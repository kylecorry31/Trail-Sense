package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.core.system.createFile
import com.kylecorry.andromeda.core.system.pickFile
import com.kylecorry.trail_sense.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IntentUriPicker(private val resolver: IntentResultRetriever, private val context: Context) :
    UriPicker {
    override suspend fun open(types: List<String>): Uri? {
        return suspendCoroutine { cont ->
            resolver.pickFile(types, context.getString(R.string.pick_file)) {
                cont.resume(it)
            }
        }
    }

    override suspend fun create(filename: String, type: String): Uri? {
        return suspendCoroutine { cont ->
            resolver.createFile(filename, listOf(type), context.getString(R.string.pick_file)) {
                cont.resume(it)
            }
        }
    }
}