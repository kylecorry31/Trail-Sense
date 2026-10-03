package com.kylecorry.trail_sense.tools.map

import android.graphics.Point
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ToolMapTest : ToolTestBase(Tools.MAP, location) {

    @Test
    fun verifyBasicFunctionality() {
        // Disclaimer
        clickOk()

        canZoom()
        canLock()
        canLongPressMap()
        verifyMapMenuOptions()
        verifySensorStatusBadges()
    }

    @Test
    fun verifyTimeSlider() {
        // Disclaimer
        clickOk()

        // Open the layers sheet
        click(R.id.menu_btn)
        click("Layers")

        // Enable Night layer (time dependent)
        scrollUntil { click("Additional layers") }
        click("Night")
        clickOk()

        // Close layers sheet
        click(toolbarButton(R.id.title, Side.Right))

        // Open time sheet
        click(R.id.time_btn)

        // Verify elements exist
        hasText(Regex("\\d{1,2}:\\d{2} [AP]M"))

        // Reset/Close
        click(R.id.time_btn)
    }

    @Test
    fun verifyMapLayers() {
        // Disclaimer
        clickOk()

        // Open the layers sheet
        click(R.id.menu_btn)
        click("Layers")

        // Verify the default layers are present
        scrollUntil { hasText("Location") }
        scrollUntil { hasText("Beacons") }
        scrollUntil { hasText("Paths") }
        scrollUntil { hasText("Tides") }
        scrollUntil { hasText("Navigation") }
        scrollUntil { hasText("Contours") }
        scrollUntil { hasText("Photo maps") }
        scrollUntil { hasText("Hillshade") }
        scrollUntil { hasText("Elevation") }
        scrollUntil { hasText("Trail maps") }
        scrollUntil { hasText("Basemap") }

        // Add an additional layer (Slope layer)
        scrollUntil { hasText("Additional layers") }
        click("Additional layers")
        click("Slope")
        clickOk()

        // Verify the new layer appears
        hasText("Slope")

        // Expand the layer
        click("Slope")

        // Move the layer down
        click(R.id.layer_move_down)

        // Move the layer up
        click(R.id.layer_move_up)

        // Toggle visible (and then re-enable)
        click("Visible")
        click("Visible")
        scrollUntil { hasText("Opacity") }

        // Copy layer to other maps
        scrollUntil { hasText("Copy settings to other maps") }
        click("Copy settings to other maps")
        // Verify Navigation is checked, uncheck Photo maps
        isChecked("Navigation")
        isChecked("Photo maps")
        click("Photo maps")
        isNotChecked("Photo maps")
        clickOk()

        // Verify the "High resolution" toggle is disabled, then turn it on
        scrollUntil { hasText("High resolution") }
        click("High resolution")

        // Remove layer
        scrollUntil { hasText("Remove layer") }
        click("Remove layer")
        clickOk()

        // Re-add all layers
        scrollUntil { hasText("Additional layers") }
        click("Additional layers")
        click("Aspect")
        click("Cell towers")
        click("Lunar eclipse")
        click("Night")
        scrollUntil { click("Ruggedness") }
        scrollUntil { click("Sightings") }
        scrollUntil { hasText("Slope") }
        scrollUntil { click("Solar eclipse") }
        clickOk()

        // Close sheet
        click(toolbarButton(R.id.title, Side.Right))
    }

    private fun verifySensorStatusBadges() {
        click(R.id.sensor_status_badges)
        hasText(string(R.string.accuracy_info_title))
        clickOk()
    }

    private fun canLock() {
        click(R.id.lock_btn)
        click(R.id.lock_btn)
        click(R.id.lock_btn)
    }

    private fun canZoom() {
        click(R.id.zoom_in_btn)
        click(R.id.zoom_out_btn)
    }

    private fun canLongPressMap() {
        longClick(R.id.map)
        hasText(Regex("-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°"))
        hasText(Regex("Elevation: -?\\d+(\\.\\d+)?\\s*(ft|m)"))
        // Verify the distance, direction, and elevation-difference data points are shown.
        hasText(R.id.location_data_distance, Regex("-?\\d+(\\.\\d+)?\\s*(mi|ft|km|m)"))
        hasText(R.id.location_data_direction, Regex("-?\\d+°\\s+\\w+"))
        hasText(R.id.location_data_elevation_diff, Regex("[+-]?\\d+(\\.\\d+)?\\s*(ft|m)"))
        hasText("Beacon")
        hasText("Navigate")
        hasText("Distance")

        click("Beacon")
        hasText("Create beacon")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        back()
        click("Leave")
        longClick(R.id.map)

        click("Navigate")
        hasText(Regex(".*-?\\d+\\.\\d+°,\\s+-?\\d+\\.\\d+°.*"))
        click(toolbarButton(R.id.navigation_sheet_title, Side.Right))
        click("Yes")

        longClick(R.id.map)
        click("Distance")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))
    }

    private fun verifyMapMenuOptions() {
        click(R.id.menu_btn)
        click("Measure")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        click(R.id.menu_btn)
        click("Create path")
        hasText("Distance")
        hasText(Regex("\\d+(\\.\\d+)? (mi|ft)"))
        hasText("Create path")
        click(toolbarButton(R.id.map_distance_title, Side.Right))

        click(R.id.menu_btn)
        click("Layers")
        click(toolbarButton(R.id.title, Side.Right))
    }

    // The map is drawn on a canvas, so these tests find where something should be on the screen using
    // the map's projection and then interact with that point.

    private fun openWithCamp() {
        assumeFalse(isStagingBuild)
        relaunchTool { TestData.addBeacon("Camp", campLocation, elevation = 100f) }
    }

    @Test
    fun mapStartsCenteredOnTheCurrentLocation() {
        assumeFalse(isStagingBuild)
        clickOk()

        longClick(R.id.map)

        val shown = parseCoordinate(waitFor { textOf(R.id.toolbar_title) })
        assertEquals(location.latitude, shown.latitude, 0.0003)
        assertEquals(location.longitude, shown.longitude, 0.0003)
    }

    @Test
    fun zoomingInHalvesTheDistanceBetweenPoints() {
        assumeFalse(isStagingBuild)
        clickOk()

        val before = longitudeSpanBetweenTwoPressedPoints()
        click(R.id.zoom_in_btn)
        val after = longitudeSpanBetweenTwoPressedPoints()

        // Each zoom in doubles the scale
        assertEquals(before / 2, after, before * 0.1)
    }

    @Test
    fun tappingABeaconShowsItAndCanNavigateToIt() {
        openWithCamp()
        clickOk()

        tapAt(campLocation) {
            hasText(R.id.toolbar_title, "Camp", waitForTime = 1000)
        }
        hasText("42.031500°,  -71.970000°")

        click("Navigate", exact = true)

        hasText(R.id.navigation_sheet_title, "Camp")
        // 0.0015 degrees of latitude is 167 m, due north
        hasText(R.id.navigation_distance, Regex("54[78] ft"))
        hasText(R.id.navigation_distance, "0° N")
    }

    @Test
    fun longPressingReportsTheLocationDistanceAndDirection() {
        assumeFalse(isStagingBuild)
        clickOk()
        // About 223 m north and 82 m east of the current location
        val target = Coordinate(42.032, -71.969)

        longPressAt(target)

        // Shown at the pixel that was pressed, which is within a pixel (2 m) of the target
        val shown = waitFor {
            parseCoordinate(view(R.id.toolbar_title).uiObject.text)
        }
        assertEquals(target.latitude, shown.latitude, 0.0001)
        assertEquals(target.longitude, shown.longitude, 0.0001)

        val expectedFeet = distanceMeters(location, shown) * 3.28084
        val expectedBearing = bearingDegrees(location, shown)
        val distance = Regex("(\\d+) ft")
            .find(textOf(R.id.location_data_distance))!!.groupValues[1].toInt()
        val direction = Regex("(\\d+)°")
            .find(textOf(R.id.location_data_direction))!!.groupValues[1].toInt()
        assertEquals(expectedFeet, distance.toDouble(), expectedFeet * 0.02)
        assertEquals(expectedBearing, direction.toDouble(), 1.5)
        hasText(Regex("Elevation: \\d+ ft"))
    }

    @Test
    fun hidingTheBeaconLayerHidesBeacons() {
        openWithCamp()
        clickOk()
        tapAt(campLocation) { hasText(R.id.toolbar_title, "Camp", waitForTime = 1000) }
        TestUtils.back()

        toggleLayerVisibility("Beacons")

        // Nothing is at that location any more
        val point = getScreenPoint(campLocation)
        TestUtils.device.click(point.x, point.y)
        not { hasText("Camp") }

        toggleLayerVisibility("Beacons")

        tapAt(campLocation) { hasText(R.id.toolbar_title, "Camp", waitForTime = 1000) }
    }

    private fun toggleLayerVisibility(layer: String) {
        click(R.id.menu_btn)
        click("Layers")
        scrollUntil { click(layer) }
        click("Visible")
        click(toolbarButton(R.id.title, Side.Right))
        not { hasText("Layers", exact = true) }
    }

    private fun textOf(
        id: Int,
        timeout: Long = AutomationLibrary.DEFAULT_WAIT_FOR_TIMEOUT
    ): String {
        return waitFor(timeout) {
            TestUtils.getMatchingChild(view(id).uiObject) { it.text != null }!!.text
        }
    }

    /**
     * Long presses at 25% and 75% of the width of the map and returns the difference in longitude.
     */
    private fun longitudeSpanBetweenTwoPressedPoints(): Double {
        val bounds = waitFor { view(R.id.map).uiObject.visibleBounds }
        val y = bounds.centerY()
        val longitudes = listOf(0.25f, 0.75f).map {
            val x = bounds.left + (bounds.width() * it).toInt()
            // The press is ignored while the previous sheet is still closing, so repeat it
            val pressed = waitFor(20000) {
                TestUtils.device.swipe(x, y, x, y, 200)
                parseCoordinate(textOf(R.id.toolbar_title, timeout = 3000))
            }
            TestUtils.back()
            isNotVisible(R.id.location_data_distance)
            pressed.longitude
        }
        return longitudes[1] - longitudes[0]
    }

    private fun getScreenPoint(coordinate: Coordinate): Point {
        var point = Point()
        scenario.onActivity {
            val map = it.findViewById<MapView>(R.id.map)
            val pixel = map.toPixel(coordinate)
            val origin = IntArray(2)
            map.getLocationOnScreen(origin)
            point = Point(origin[0] + pixel.x.toInt(), origin[1] + pixel.y.toInt())
        }
        return point
    }

    /**
     * Taps the coordinate until [verify] passes, since the layers load in the background.
     */
    private fun tapAt(coordinate: Coordinate, verify: () -> Unit) {
        waitFor(10000) {
            val point = getScreenPoint(coordinate)
            TestUtils.device.click(point.x, point.y)
            verify()
        }
    }

    private fun longPressAt(coordinate: Coordinate) {
        val point = getScreenPoint(coordinate)
        // Holding a swipe in place for a second is a long press
        TestUtils.device.swipe(point.x, point.y, point.x, point.y, 200)
    }

    private fun parseCoordinate(text: String): Coordinate {
        val (latitude, longitude) = Regex("(-?\\d+\\.\\d+)°,\\s+(-?\\d+\\.\\d+)°")
            .find(text)!!.destructured
        return Coordinate(latitude.toDouble(), longitude.toDouble())
    }

    private fun distanceMeters(from: Coordinate, to: Coordinate): Double {
        val radius = 6371008.8
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * radius * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun bearingDegrees(from: Coordinate, to: Coordinate): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    companion object {
        private val location = Coordinate(42.03, -71.97)
        // About 167 m north of the current location
        private val campLocation = Coordinate(42.0315, -71.97)
    }
}
