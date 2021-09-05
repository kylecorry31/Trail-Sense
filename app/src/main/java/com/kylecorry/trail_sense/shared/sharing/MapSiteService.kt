package com.kylecorry.trail_sense.shared.sharing

import com.kylecorry.sol.units.Coordinate

internal class MapSiteService {

    fun getUrl(coordinate: Coordinate, site: MapSite): String {
        return when(site){
            MapSite.Google -> "https://www.google.com/maps/@${coordinate.latitude},${coordinate.longitude},16z"
            MapSite.OSM -> "https://www.openstreetmap.org/#map=16/${coordinate.latitude}/${coordinate.longitude}"
            MapSite.Bing -> "https://www.bing.com/maps?lvl=16&cp=${coordinate.latitude}~${coordinate.longitude}"
            MapSite.Apple -> "http://maps.apple.com/?z=16&ll=${coordinate.latitude},${coordinate.longitude}"
            MapSite.Caltopo -> "https://caltopo.com/map.html#ll=${coordinate.latitude},${coordinate.longitude}&z=16"
        }
    }

}