package com.kylecorry.trail_sense.shared.views

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences

private val TrailSenseLightColors = lightColorScheme(
    primary = Color(0xFFFF6D00),
    onPrimary = Color(0xFF351000),
    primaryContainer = Color(0x80FF6D00),
    onPrimaryContainer = Color(0xFF351000),
    secondaryContainer = Color(0x80FF6D00),
    tertiaryContainer = Color(0x80FF6D00),
    onSecondaryContainer = Color(0xFF351000),
    onTertiaryContainer = Color(0xFF351000)
)

private val TrailSenseDarkColors = darkColorScheme(
    primary = Color(0xFFFF6D00),
    onPrimary = Color(0xFF351000),
    primaryContainer = Color(0x80FF6D00),
    onPrimaryContainer = Color(0xFF351000),
    secondaryContainer = Color(0x80FF6D00),
    tertiaryContainer = Color(0x80FF6D00),
    onSecondaryContainer = Color(0xFF351000),
    onTertiaryContainer = Color(0xFF351000)
)

@Composable
fun TrailSenseTheme(
    content: @Composable () -> Unit
) {
    val useDynamicColors = remember { getAppService<UserPreferences>().useDynamicColors }
    val useDarkTheme = isSystemInDarkTheme()
    val colorScheme = trailSenseColorScheme(useDarkTheme, useDynamicColors)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
private fun trailSenseColorScheme(
    useDarkTheme: Boolean,
    useDynamicColors: Boolean
): ColorScheme {
    val context = LocalContext.current
    return when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> TrailSenseDarkColors
        else -> TrailSenseLightColors
    }
}
