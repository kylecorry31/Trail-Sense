package com.kylecorry.trail_sense.tools.beacons.domain

import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.data.Identifiable

// Last ID: 47
enum class BeaconIcon(override val id: Long, @DrawableRes val icon: Int) : Identifiable {
    // Activities
    Fishing(1, R.drawable.ic_fishing),
    Fire(2, R.drawable.ic_category_fire),

    // Natural features
    Mountain(3, R.drawable.ic_altitude),
    BodyOfWater(4, R.drawable.ic_tide_table),
    River(5, R.drawable.ic_river),
    Waterfall(6, R.drawable.ic_waterfall),
    Beach(7, R.drawable.ic_beach),
    Tree(8, R.drawable.tree),
    Field(9, R.drawable.ic_grass),
    Plant(10, R.drawable.ic_category_natural),

    // Amenities
    Food(11, R.drawable.ic_category_food),
    Picnic(12, R.drawable.ic_picnic),
    WaterRefill(13, R.drawable.ic_category_water),
    Coffee(14, R.drawable.ic_coffee),
    Pub(15, R.drawable.ic_glass_mug_variant),
    Restroom(16, R.drawable.ic_restrooms),
    Shower(17, R.drawable.ic_shower_head),
    Toilet(18, R.drawable.ic_toilet),
    Phone(19, R.drawable.ic_phone),
    Trash(20, R.drawable.ic_delete),
    CellSignal(21, R.drawable.signal_cellular_3),
    WiFi(22, R.drawable.ic_wifi),
    FirstAid(23, R.drawable.ic_category_medical),
    Power(24, R.drawable.ic_torch_on),
    Music(25, R.drawable.ic_music),

    // Buildings
    Tent(26, R.drawable.ic_category_shelter),
    House(27, R.drawable.ic_house),
    Building(28, R.drawable.ic_building),
    Resturant(29, R.drawable.ic_silverware_fork_knife),
    Shop(30, R.drawable.ic_store),
    FireTower(31, R.drawable.ic_fire_tower),
    Bridge(32, R.drawable.ic_bridge),
    Lighthouse(33, R.drawable.ic_lighthouse),
    VisitorCenter(34, R.drawable.ic_visitor_center),


    // Vehicles
    Road(35, R.drawable.ic_road),
    Car(36, R.drawable.ic_car),
    Boat(37, R.drawable.ic_boat),
    Bike(38, R.drawable.ic_bike),
    SailBoat(39, R.drawable.ic_sail_boat),


    // Other
    Alert(40, R.drawable.ic_alert_simple),
    Information(41, R.drawable.ic_help_simple),
    Map(42, R.drawable.maps),
    Sign(43, R.drawable.ic_sign),
    Lifebuoy(44, R.drawable.ic_lifebuoy),
    Anchor(45, R.drawable.ic_anchor),
    CompassRose(46, R.drawable.ic_compass_rose),
    Radio(47, R.drawable.ic_radio_handheld),

}