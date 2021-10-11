package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.trail_sense.settings.infrastructure.ICompassStylePreferences
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

internal class CompassStyleChooserTest {

    @ParameterizedTest
    @MethodSource("provideStyle")
    fun getStyle(
        useLinear: Boolean,
        useRadar: Boolean,
        orientation: DeviceOrientation.Orientation,
        expected: CompassStyle
    ) {
        val prefs = mock<ICompassStylePreferences>()
        whenever(prefs.useLinearCompass).thenReturn(useLinear)
        whenever(prefs.useRadarCompass).thenReturn(useRadar)
        val chooser = CompassStyleChooser(prefs)
        chooser.getStyle(orientation)
    }

    companion object {

        @JvmStatic
        fun provideStyle(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(true, false, DeviceOrientation.Orientation.Portrait, CompassStyle.Linear),
                Arguments.of(true, false, DeviceOrientation.Orientation.PortraitInverse, CompassStyle.Round),
                Arguments.of(true, false, DeviceOrientation.Orientation.Landscape, CompassStyle.Round),
                Arguments.of(true, false, DeviceOrientation.Orientation.LandscapeInverse, CompassStyle.Round),
                Arguments.of(true, false, DeviceOrientation.Orientation.Flat, CompassStyle.Round),
                Arguments.of(true, false, DeviceOrientation.Orientation.FlatInverse, CompassStyle.Round),
                Arguments.of(true, true, DeviceOrientation.Orientation.Portrait, CompassStyle.Linear),
                Arguments.of(true, true, DeviceOrientation.Orientation.PortraitInverse, CompassStyle.Radar),
                Arguments.of(true, true, DeviceOrientation.Orientation.Landscape, CompassStyle.Radar),
                Arguments.of(true, true, DeviceOrientation.Orientation.LandscapeInverse, CompassStyle.Radar),
                Arguments.of(true, true, DeviceOrientation.Orientation.Flat, CompassStyle.Radar),
                Arguments.of(true, true, DeviceOrientation.Orientation.FlatInverse, CompassStyle.Radar),
                Arguments.of(false, false, DeviceOrientation.Orientation.Portrait, CompassStyle.Round),
                Arguments.of(false, false, DeviceOrientation.Orientation.PortraitInverse, CompassStyle.Round),
                Arguments.of(false, false, DeviceOrientation.Orientation.Landscape, CompassStyle.Round),
                Arguments.of(false, false, DeviceOrientation.Orientation.LandscapeInverse, CompassStyle.Round),
                Arguments.of(false, false, DeviceOrientation.Orientation.Flat, CompassStyle.Round),
                Arguments.of(false, false, DeviceOrientation.Orientation.FlatInverse, CompassStyle.Round),
                Arguments.of(false, true, DeviceOrientation.Orientation.Portrait, CompassStyle.Radar),
                Arguments.of(false, true, DeviceOrientation.Orientation.PortraitInverse, CompassStyle.Radar),
                Arguments.of(false, true, DeviceOrientation.Orientation.Landscape, CompassStyle.Radar),
                Arguments.of(false, true, DeviceOrientation.Orientation.LandscapeInverse, CompassStyle.Radar),
                Arguments.of(false, true, DeviceOrientation.Orientation.Flat, CompassStyle.Radar),
                Arguments.of(false, true, DeviceOrientation.Orientation.FlatInverse, CompassStyle.Radar),
            )
        }
    }

}