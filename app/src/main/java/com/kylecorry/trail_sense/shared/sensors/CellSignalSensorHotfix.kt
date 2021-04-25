package com.kylecorry.trail_sense.shared.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.*
import androidx.core.content.getSystemService
import androidx.core.math.MathUtils
import com.kylecorry.trailsensecore.domain.network.CellNetwork
import com.kylecorry.trailsensecore.domain.network.CellSignal
import com.kylecorry.trailsensecore.domain.units.Quality
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.network.ICellSignalSensor
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.lang.Exception
import java.time.Duration
import java.time.Instant

class CellSignalSensorHotfix(private val context: Context) : AbstractSensor(), ICellSignalSensor {

    private val telephony by lazy { context.getSystemService<TelephonyManager>() }

    override val hasValidReading: Boolean
        get() = hasReading

    override val signals: List<CellSignal>
        get() = _signals

    private var _signals = listOf<CellSignal>()
    private var oldSignals = listOf<RawCellSignal>()
    private var hasReading = false

    @SuppressLint("MissingPermission")
    private val intervalometer = Intervalometer {
        if (PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {
                updateCellInfo(telephony?.allCellInfo ?: listOf())
            } catch (e: Exception){}
        }
    }

    @Suppress("DEPRECATION")
    private fun updateCellInfo(cells: List<CellInfo>) {
        synchronized(this) {
            hasReading = true
            val newSignals = cells.filter { it.isRegistered }.mapNotNull {
                when {
                    it is CellInfoWcdma -> {
                        RawCellSignal(
                            it.cellIdentity.cid.toString(),
                            Instant.ofEpochMilli(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.timestampMillis else (it.timeStamp / 1000000)),
                            it.cellSignalStrength.dbm,
                            it.cellSignalStrength.level,
                            CellNetwork.Wcdma
                        )
                    }
                    it is CellInfoGsm -> {
                        RawCellSignal(
                            it.cellIdentity.cid.toString(),
                            Instant.ofEpochMilli(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.timestampMillis else (it.timeStamp / 1000000)),
                            it.cellSignalStrength.dbm,
                            it.cellSignalStrength.level,
                            CellNetwork.Gsm
                        )
                    }
                    it is CellInfoLte -> {
                        RawCellSignal(
                            it.cellIdentity.ci.toString(),
                            Instant.ofEpochMilli(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.timestampMillis else (it.timeStamp / 1000000)),
                            it.cellSignalStrength.dbm,
                            it.cellSignalStrength.level,
                            CellNetwork.Lte
                        )
                    }
                    it is CellInfoCdma -> {
                        RawCellSignal(
                            it.cellIdentity.basestationId.toString(),
                            Instant.ofEpochMilli(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.timestampMillis else (it.timeStamp / 1000000)),
                            it.cellSignalStrength.dbm,
                            it.cellSignalStrength.level,
                            CellNetwork.Cdma
                        )
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it is CellInfoTdscdma -> {
                        RawCellSignal(
                            it.cellIdentity.cid.toString(),
                            Instant.ofEpochMilli(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.timestampMillis else (it.timeStamp / 1000000)),
                            it.cellSignalStrength.dbm,
                            it.cellSignalStrength.level,
                            CellNetwork.Tdscdma
                        )
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it is CellInfoNr -> {
                        RawCellSignal(
                            it.cellIdentity.operatorAlphaLong.toString(),
                            Instant.ofEpochMilli(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.timestampMillis else (it.timeStamp / 1000000)),
                            it.cellSignalStrength.dbm,
                            it.cellSignalStrength.level,
                            CellNetwork.Nr
                        )
                    }
                    else -> null
                }
            }

            val latestSignals = newSignals.map {
                val old = oldSignals.find { signal -> it.id == signal.id }
                if (old == null) {
                    it
                } else {
                    if (old.time > it.time) old else it
                }
            }

            oldSignals = latestSignals

            _signals = latestSignals.map {
                CellSignal(it.id, it.percent, it.dbm, it.quality, it.network)
            }

            notifyListeners()
        }
    }

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!PermissionUtils.isLocationEnabled(context)) {
            _signals = listOf()
            notifyListeners()
            return
        }
        try {
            updateCellInfo(telephony?.allCellInfo ?: listOf())
        } catch (e: Exception){}
        intervalometer.interval(Duration.ofSeconds(5), Duration.ofSeconds(5))
    }

    override fun stopImpl() {
        intervalometer.stop()
    }

    data class RawCellSignal(
        val id: String,
        val time: Instant,
        val dbm: Int,
        val level: Int,
        val network: CellNetwork
    ) {
        val percent: Float
            get() {
                return MathUtils.clamp(
                    100f * (dbm - network.minDbm) / (network.maxDbm - network.minDbm).toFloat(),
                    0f,
                    100f
                )
            }

        val quality: Quality
            get() = when (level) {
                3, 4 -> Quality.Good
                2 -> Quality.Moderate
                0 -> Quality.Unknown
                else -> Quality.Poor
            }
    }
}