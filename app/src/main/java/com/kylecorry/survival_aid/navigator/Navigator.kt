package com.kylecorry.survival_aid.navigator

import com.kylecorry.survival_aid.navigator.beacons.Beacon
import com.kylecorry.survival_aid.navigator.gps.Coordinate
import com.kylecorry.survival_aid.navigator.gps.LocationMath
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
     * Determines if the device has arrived at the beacon
     * @param currentLocation the current location of the device
     * @param arriveDistance the distance in meters from the beacon to consider the device to have arrived
     * @return true if the device is at the beacon, false otherwise
     */
    fun hasArrived(currentLocation: Coordinate, arriveDistance: Float): Boolean {
        val distance = getDistance(currentLocation)
        return distance <= arriveDistance
    }

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