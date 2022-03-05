package com.kylecorry.trail_sense.shared.io

import android.net.Uri
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FragmentUriPicker(private val fragment: AndromedaFragment) :
    UriPicker {
    override suspend fun open(types: List<String>): Uri? {
        return suspendCoroutine { cont ->
            fragment.pickFile(types, fragment.getString(R.string.pick_file)) {
                cont.resume(it)
            }
        }
    }

    override suspend fun create(filename: String, type: String): Uri? {
        return suspendCoroutine { cont ->
            fragment.createFile(filename, listOf(type), fragment.getString(R.string.pick_file)) {
                cont.resume(it)
            }
        }
    }
}