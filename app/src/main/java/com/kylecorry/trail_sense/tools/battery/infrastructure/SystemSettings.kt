package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import android.content.res.Configuration
import android.location.LocationManager
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import com.kylecorry.sol.math.SolMath
import kotlin.math.roundToInt


object SystemSettings {

    private const val ON = 1
    private const val OFF = 0

    fun isAirplaneModeEnabled(context: Context): Boolean {
        return getBooleanSystemSetting(context, Settings.Global.AIRPLANE_MODE_ON)
    }

    fun getAirplaneModeRadios(context: Context): List<String> {
        return Settings.Global.getString(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_RADIOS
        )?.split(',') ?: emptyList()
    }

    fun isWifiEnabled(context: Context): Boolean {
        if (!getBooleanSystemSetting(context, Settings.Global.WIFI_ON)) {
            return false
        }
        if (!isAirplaneModeEnabled(context)) {
            return true
        }
        return !getAirplaneModeRadios(context).contains(Settings.Global.RADIO_WIFI)
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        if (!getBooleanSystemSetting(context, Settings.Global.BLUETOOTH_ON)) {
            return false
        }
        if (!isAirplaneModeEnabled(context)) {
            return true
        }
        return !getAirplaneModeRadios(context).contains(Settings.Global.RADIO_BLUETOOTH)
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (adapter?.isEnabled != true) {
            return false
        }
        if (!isAirplaneModeEnabled(context)) {
            return true
        }
        return !getAirplaneModeRadios(context).contains(Settings.Global.RADIO_NFC)
    }

    fun isPowerSaverEnabled(context: Context): Boolean {
        val manager = context.getSystemService<PowerManager>()
        return manager?.isPowerSaveMode == true
    }

    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService<LocationManager>()
        return manager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || manager?.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        ) == true
    }

    fun isAutomaticBrightnessEnabled(context: Context): Boolean {
        return getBooleanSystemSetting(context, Settings.System.SCREEN_BRIGHTNESS_MODE, false)
    }

    fun getScreenOffTimeout(context: Context): Int {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            60000
        )
    }

    fun isDarkThemeEnabled(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }

    private fun getBooleanSystemSetting(
        context: Context,
        key: String,
        default: Boolean = false
    ): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            key, if (default) ON else OFF
        ) != OFF
    }

}