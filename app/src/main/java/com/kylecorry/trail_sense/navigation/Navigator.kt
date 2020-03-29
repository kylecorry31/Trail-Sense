package com.kylecorry.trail_sense.navigation

import com.kylecorry.trail_sense.models.Beacon
import com.kylecorry.trail_sense.models.Coordinate
import java.util.*

/**
 * A class to handle navigation to a beacon
 */
class Navigator: Observable() {

    /**
     * The destination beacon
     */
    var destination: Beacon? = null
        set(value) {
            field = value
            setChanged()
            notifyObservers()
        }

    /**
     * Determines if the navigator has a destination
     */
    val hasDestination
        get() = destination != null

    /**
     * Get the bearing to the destination
     * @param currentLocation the current location of the device
     * @return the bearing in degrees
     */
    fun getBearing(currentLocation: Coordinate): Float {
        val dest = destination
        dest ?: return 0f
        return LocationMath.getBearing(currentLocation, dest.coordinate)
    }

    /**
     * Get the distance to the destination
     * @param currentLocation the current location of the device
     * @return the distance in meters
     */
    fun getDistance(currentLocation: Coordinate): Float {
        val dest = destination
        dest ?: return 0f
        return LocationMath.getDistance(currentLocation, dest.coordinate)
    }

    /**
     * Get the name of the destination
     * @return the destination beacon name
     */
    fun getDestinationName(): String {
        val dest = destination
        dest ?: return ""
        return dest.name
    }

}