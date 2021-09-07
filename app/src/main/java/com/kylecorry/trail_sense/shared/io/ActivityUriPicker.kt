package com.kylecorry.trail_sense.shared.io

import android.net.Uri
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.trail_sense.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ActivityUriPicker(private val activity: AndromedaActivity) :
    UriPicker {
    override suspend fun open(types: List<String>): Uri? {
        return suspendCoroutine { cont ->
            activity.pickFile(types, activity.getString(R.string.pick_file)) {
                cont.resume(it)
            }
        }
    }

    override suspend fun create(filename: String, type: String): Uri? {
        return suspendCoroutine { cont ->
            activity.createFile(filename, type) {
                cont.resume(it)
            }
        }
    }
}