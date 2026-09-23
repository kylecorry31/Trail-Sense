package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.location.ISatelliteStatusSensor

interface ISatelliteGPS : IGPS, ISatelliteStatusSensor
