package com.kylecorry.trail_sense.shared.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> onMain(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

suspend fun <T> onDefault(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Default, block)

suspend fun <T> onIO(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)