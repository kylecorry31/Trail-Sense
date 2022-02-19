package com.kylecorry.trail_sense.navigation.beacons.infrastructure.loading

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class BeaconLoaderTest {

    private lateinit var loader: BeaconLoader
    private lateinit var service: IBeaconService
    private lateinit var prefs: IBeaconPreferences

    @BeforeEach
    fun setup() {
        service = mock()
        prefs = mock()
        loader = BeaconLoader(service, prefs)
    }

    @Test
    fun loadsBeacons() = runBlocking {
        val expected = listOf(
            Beacon(1, "Test", Coordinate.zero),
            BeaconGroup(1, "Test")
        )

        whenever(service.getBeacons(1))
            .thenReturn(expected)

        val beacons = loader.load(null, 1)

        assertEquals(expected, beacons)
    }

    @Test
    fun loadsCellSignal() = runBlocking {
        val expected = listOf(
            Beacon(1, "Test", Coordinate.zero),
            BeaconGroup(1, "Test")
        )

        val signal = Beacon(2, "Signal", Coordinate.zero)

        whenever(service.getBeacons(null))
            .thenReturn(expected)

        whenever(service.getTemporaryBeacon(BeaconOwner.CellSignal))
            .thenReturn(signal)

        whenever(prefs.showLastSignalBeacon)
            .thenReturn(true)

        val beacons = loader.load(null, null)

        assertEquals(expected + signal, beacons)
    }

    @Test
    fun doesNotLoadCellSignalWhenFilteredByGroup() = runBlocking {
        val expected = listOf(
            Beacon(1, "Test", Coordinate.zero),
            BeaconGroup(1, "Test")
        )

        val signal = Beacon(2, "Signal", Coordinate.zero)

        whenever(service.getBeacons(1))
            .thenReturn(expected)

        whenever(service.getTemporaryBeacon(BeaconOwner.CellSignal))
            .thenReturn(signal)

        whenever(prefs.showLastSignalBeacon)
            .thenReturn(true)

        val beacons = loader.load(null, 1)

        assertEquals(expected, beacons)
    }

    @Test
    fun doesNotLoadCellSignalWhenPreferenceOff() = runBlocking {
        val expected = listOf(
            Beacon(1, "Test", Coordinate.zero),
            BeaconGroup(1, "Test")
        )

        val signal = Beacon(2, "Signal", Coordinate.zero)

        whenever(service.getBeacons(null))
            .thenReturn(expected)

        whenever(service.getTemporaryBeacon(BeaconOwner.CellSignal))
            .thenReturn(signal)

        whenever(prefs.showLastSignalBeacon)
            .thenReturn(false)

        val beacons = loader.load(null, null)

        assertEquals(expected, beacons)
    }

    @Test
    fun searchesBeacons() = runBlocking {
        val expected = listOf(
            Beacon(1, "Test", Coordinate.zero),
            BeaconGroup(1, "Test")
        )

        whenever(service.search("Test", 1))
            .thenReturn(expected)

        val beacons = loader.load("Test", 1)

        assertEquals(expected, beacons)
    }

}