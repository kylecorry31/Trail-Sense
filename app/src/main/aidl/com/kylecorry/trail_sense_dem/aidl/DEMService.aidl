// DEMService.aidl
package com.kylecorry.trail_sense_dem.aidl;

import com.kylecorry.trail_sense_dem.aidl.ElevationResult;

interface DEMService {
    /**
     * Get elevation for a given latitude and longitude
     * @param latitude The latitude in decimal degrees
     * @param longitude The longitude in decimal degrees
     * @return ElevationResult containing elevation in meters or error information
     */
    ElevationResult getElevation(double latitude, double longitude);
}