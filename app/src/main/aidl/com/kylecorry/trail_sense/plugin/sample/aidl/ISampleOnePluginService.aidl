package com.kylecorry.trail_sense.plugin.sample.aidl;

interface ISampleOnePluginService {
    /**
     * Get the weather for a given latitude and longitude
     * @param latitude The latitude in decimal degrees
     * @param longitude The longitude in decimal degrees
     * @return the weather for the given location
     */
    String getWeather(double latitude, double longitude);
}