package com.kylecorry.trail_sense.shared.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun <T> onMain(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

suspend fun <T> onDefault(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Default, block)

suspend fun <T> onIO(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)

enum class BackgroundMinimumState {
    Resumed,
    Started,
    Created,
    Any
}

fun Fragment.inBackground(
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    block: suspend CoroutineScope.() -> Unit
) {
    when (state) {
        BackgroundMinimumState.Resumed -> lifecycleScope.launchWhenResumed(block)
        BackgroundMinimumState.Started -> lifecycleScope.launchWhenStarted(block)
        BackgroundMinimumState.Created -> lifecycleScope.launchWhenCreated(block)
        BackgroundMinimumState.Any -> lifecycleScope.launch { block() }
    }
}


fun AppCompatActivity.inBackground(
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    block: suspend CoroutineScope.() -> Unit
) {
    when (state) {
        BackgroundMinimumState.Resumed -> lifecycleScope.launchWhenResumed(block)
        BackgroundMinimumState.Started -> lifecycleScope.launchWhenStarted(block)
        BackgroundMinimumState.Created -> lifecycleScope.launchWhenCreated(block)
        BackgroundMinimumState.Any -> lifecycleScope.launch { block() }
    }
}