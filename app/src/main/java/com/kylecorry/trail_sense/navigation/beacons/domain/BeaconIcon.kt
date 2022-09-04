package com.kylecorry.trail_sense.navigation.beacons.domain

import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.database.Identifiable

// Last ID: 45
enum class BeaconIcon(override val id: Long, @DrawableRes val icon: Int) : Identifiable {
    // Activities
    Trail(26, R.drawable.ic_trail),
    Climbing(30, R.drawable.ic_climbing),
    SkiLift(9, R.drawable.ic_ski_lift),
    Fishing(11, R.drawable.ic_fishing),
    View(25, R.drawable.ic_binoculars),
    TrailFork(28, R.drawable.ic_trail_fork),
    Fire(29, R.drawable.ic_category_fire),
    Hunting(34, R.drawable.ic_deer),

    // Natural features
    Mountain(1, R.drawable.ic_altitude),
    BodyOfWater(17, R.drawable.ic_tide_table),
    River(18, R.drawable.ic_river),
    Waterfall(20, R.drawable.ic_waterfall),
    Beach(19, R.drawable.ic_beach), // TODO: Get a better icon
    Tree(21, R.drawable.tree),
    Field(37, R.drawable.ic_grass),
    Plant(38, R.drawable.ic_category_natural),
//    Boulder(31, R.drawable.ic_boulder),

    // Amenities
    Food(12, R.drawable.ic_category_food),
    Picnic(36, R.drawable.ic_picnic),
    WaterRefill(6, R.drawable.ic_category_water),
    Restroom(7, R.drawable.ic_restrooms),
    Phone(8, R.drawable.ic_phone),
    Trash(16, R.drawable.ic_delete),
    CellSignal(22, R.drawable.signal_cellular_3),
    WiFi(23, R.drawable.ic_wifi),
    FirstAid(24, R.drawable.ic_category_medical),
    Power(32, R.drawable.ic_torch_on),

    // Buildings
    Tent(5, R.drawable.ic_category_shelter),
    House(10, R.drawable.ic_house),
    Building(39, R.drawable.ic_building),
    Barn(40, R.drawable.ic_barn),
    // Historic(33, R.drawable.ic_historic),
    Cemetery(41, R.drawable.ic_grave),
    FireTower(42, R.drawable.ic_fire_tower),
    Bridge(43, R.drawable.ic_bridge),
    Lighthouse(44, R.drawable.ic_lighthouse),
    VisitorCenter(45, R.drawable.ic_visitor_center),


    // Vehicles
    Road(27, R.drawable.ic_road),
    Car(2, R.drawable.ic_car),
    Boat(3, R.drawable.ic_boat),
    Bike(4, R.drawable.ic_bike),


    // Other
    Alert(13, R.drawable.ic_alert_simple),
    Information(14, R.drawable.ic_help_simple),
    Map(15, R.drawable.maps),
    Sign(35, R.drawable.ic_sign),
}