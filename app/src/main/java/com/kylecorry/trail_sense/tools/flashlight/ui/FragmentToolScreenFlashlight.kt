package com.kylecorry.trail_sense.tools.flashlight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.math.interpolation.Interpolation.map
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useBooleanPreference
import com.kylecorry.trail_sense.shared.extensions.compose.useEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useIntPreference
import com.kylecorry.trail_sense.shared.extensions.compose.useLifecycleEffect
import com.kylecorry.trail_sense.shared.extensions.compose.useMemo
import com.kylecorry.trail_sense.shared.extensions.compose.useNavController
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlin.math.roundToInt

class FragmentToolScreenFlashlight : TrailSenseComposeFragment() {

    private var volumeButtonPressHandler: (isVolumeUp: Boolean) -> Unit = {}

    @Composable
    override fun FragmentContent() {
        val flashlight = useMemo { ScreenTorch(requireActivity().window) }
        val preferences = useService<PreferencesSubsystem>()
        val navController = useNavController()

        val (brightness, setBrightness) = useIntPreference(stringResource(R.string.pref_screen_torch_brightness))
        val (isRed, setIsRed) = useBooleanPreference("cache_red_light")

        useEffect(flashlight, preferences) {
            volumeButtonPressHandler = { isVolumeUp: Boolean ->
                val currentBrightness =
                    preferences.preferences.getInt(getString(R.string.pref_screen_torch_brightness)) ?: 100
                if (isVolumeUp) {
                    setBrightness((currentBrightness + 10).coerceAtMost(100))
                } else {
                    setBrightness((currentBrightness - 10).coerceAtLeast(0))
                }
            }
        }

        val actualBrightness = brightness ?: 100
        val actualIsRed = isRed == true

        useEffect(actualBrightness, resumedCount) {
            flashlight.on(map(actualBrightness / 100f, 0f, 1f, 0.1f, 1f))
        }

        useLifecycleEffect(Lifecycle.Event.ON_PAUSE, flashlight) {
            flashlight.off()
        }

        ScreenFlashlightContent(
            isRed = actualIsRed,
            brightness = actualBrightness,
            onIsRedChange = setIsRed,
            onBrightnessChange = setBrightness,
            onTurnOff = { navController.navigateUp() },
            modifier = Modifier.fillMaxSize()
        )
    }

    fun handleVolumeButtonPress(isVolumeUp: Boolean) {
        volumeButtonPressHandler(isVolumeUp)
    }
}

@Composable
private fun ScreenFlashlightContent(
    isRed: Boolean,
    brightness: Int,
    onIsRedChange: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isRed) Color.Red else Color.White
    val switcherColor = if (isRed) Color.White else Color.Red

    Box(
        modifier = modifier
            .background(backgroundColor)
            .testTag("screen_flashlight")
            .keepScreenOn()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(32.dp)
                .background(switcherColor)
                .testTag("red_white_switcher")
                .clickable { onIsRedChange(!isRed) }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Slider(
                value = brightness.toFloat(),
                onValueChange = {
                    onBrightnessChange(it.roundToInt().coerceIn(0, 100))
                },
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("brightness_seek")
            )

            ElevatedButton(
                onClick = onTurnOff,
                modifier = Modifier.testTag("off_btn")
            ) {
                Text(text = stringResource(R.string.turn_off))
            }
        }
    }
}

@Preview
@Composable
private fun ScreenFlashlightPreview() {
    MaterialTheme {
        ScreenFlashlightContent(
            isRed = false,
            brightness = 50,
            onIsRedChange = {},
            onBrightnessChange = {},
            onTurnOff = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun ScreenFlashlightRedPreview() {
    MaterialTheme {
        ScreenFlashlightContent(
            isRed = true,
            brightness = 50,
            onIsRedChange = {},
            onBrightnessChange = {},
            onTurnOff = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
