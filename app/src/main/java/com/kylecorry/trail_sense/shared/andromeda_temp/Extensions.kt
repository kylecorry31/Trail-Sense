package com.kylecorry.trail_sense.shared.andromeda_temp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.subscriptions.ISubscription
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.sol.math.SolMath.isCloseTo
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue

fun CoordinateBounds.grow(percent: Float): CoordinateBounds {
    val x = this.width() * percent
    val y = this.height() * percent
    return CoordinateBounds.Companion.from(
        listOf(
            northWest.plus(x, Bearing.Companion.from(CompassDirection.West))
                .plus(y, Bearing.Companion.from(CompassDirection.North)),
            northEast.plus(x, Bearing.Companion.from(CompassDirection.East))
                .plus(y, Bearing.Companion.from(CompassDirection.North)),
            southWest.plus(x, Bearing.Companion.from(CompassDirection.West))
                .plus(y, Bearing.Companion.from(CompassDirection.South)),
            southEast.plus(x, Bearing.Companion.from(CompassDirection.East))
                .plus(y, Bearing.Companion.from(CompassDirection.South)),
        )
    )
}

fun CoordinateBounds.heightDegrees(): Double {
    return (north - south).absoluteValue
}

fun CoordinateBounds.widthDegrees(): Double {
    if (isCloseTo(west, CoordinateBounds.world.west, 0.0001) && isCloseTo(east, CoordinateBounds.world.east, 0.0001)) {
        return 360.0
    }

    return (if (east >= west) {
        east - west
    } else {
        (180 - west) + (east + 180)
    }).absoluteValue
}

fun CoordinateBounds.intersects2(other: CoordinateBounds): Boolean {
    if (south > other.north || other.south > north) {
        return false
    }

    val union = CoordinateBounds.from(
        listOf(
            northEast, northWest, southEast, southWest,
            other.northEast, other.northWest, other.southEast, other.southWest
        )
    )

    return union.widthDegrees() <= (widthDegrees() + other.widthDegrees())
}

fun Fragment.observe(
    subscription: ISubscription,
    state: BackgroundMinimumState = BackgroundMinimumState.Any,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    listener: suspend () -> Unit
) {
    observeFlow(subscription.flow(), state, collectOn, observeOn) { listener() }
}

fun <T> Fragment.observe(
    subscription: com.kylecorry.andromeda.core.subscriptions.generic.ISubscription<T>,
    state: BackgroundMinimumState = BackgroundMinimumState.Any,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    listener: suspend (T) -> Unit
) {
    observeFlow(subscription.flow(), state, collectOn, observeOn, listener)
}

fun Package.hasPermission(
    context: Context,
    packageName: String,
    permission: String
): Boolean {
    return tryOrDefault(false) {
        val pm = context.packageManager
        pm.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED
    }
}

fun Package.getSelfSignatureSha256Fingerprints(context: Context): List<String> {
    return getSignatureSha256Fingerprints(context, context.packageName)
}

@Suppress("DEPRECATION")
fun Package.getSignatureSha256Fingerprints(
    context: Context,
    packageName: String
): List<String> {
    val info = tryOrDefault(null) {
        Package.getPackageInfo(
            context,
            packageName,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        )
    } ?: return emptyList()
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = info.signingInfo ?: return emptyList()
        if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
    } else {
        info.signatures ?: return emptyList()
    }
    val digest = MessageDigest.getInstance("SHA-256")
    val signatureHashes = mutableListOf<String>()
    for (sig in signatures) {
        val digest = digest.digest(sig.toByteArray())
        val hash = digest.joinToString(":") { String.format("%02X", it) }
        signatureHashes.add(hash)
    }
    return signatureHashes.distinct()
}