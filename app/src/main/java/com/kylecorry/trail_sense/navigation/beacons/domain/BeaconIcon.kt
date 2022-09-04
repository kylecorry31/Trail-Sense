package com.kylecorry.trail_sense.navigation.beacons.domain

import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.database.Identifiable

enum class BeaconIcon(override val id: Long, @DrawableRes val icon: Int) : Identifiable {
    Mountain(1, R.drawable.ic_altitude),
    Car(2, R.drawable.ic_car),
    Boat(3, R.drawable.ic_boat),
    Bike(4, R.drawable.ic_bike),
    Tent(5, R.drawable.ic_category_shelter),
    WaterRefill(6, R.drawable.ic_category_water),
    Restroom(7, R.drawable.ic_restrooms),
    Phone(8, R.drawable.ic_phone),
    SkiLift(9, R.drawable.ic_ski_lift),
    House(10, R.drawable.ic_house),
    Fishing(11, R.drawable.ic_fishing),
    Food(12, R.drawable.ic_category_food),
    Alert(13, R.drawable.ic_alert_simple),
    Information(14, R.drawable.ic_help_simple),
    Map(15, R.drawable.maps),
    Trash(16, R.drawable.ic_delete),
    BodyOfWater(17, R.drawable.ic_tide_table),
    River(18, R.drawable.ic_river),
    Beach(19, R.drawable.ic_beach),
    Waterfall(20, R.drawable.ic_waterfall),
    Tree(21, R.drawable.tree),
    CellSignal(22, R.drawable.signal_cellular_3),
    WiFi(23, R.drawable.ic_wifi),
    FirstAid(24, R.drawable.ic_category_medical),
    View(25, R.drawable.ic_binoculars),
    Trail(26, R.drawable.ic_trail),
    Road(27, R.drawable.ic_road),
    TrailFork(28, R.drawable.ic_trail_fork),
    Fire(29, R.drawable.ic_category_fire),
    Climbing(30, R.drawable.ic_climbing),

    //    Boulder(31, R.drawable.ic_boulder),
    Power(32, R.drawable.ic_torch_on),

    //    Historic(33, R.drawable.ic_historic),
    Hunting(34, R.drawable.ic_deer),
    Sign(35, R.drawable.ic_sign),
    Picnic(36, R.drawable.ic_picnic),
    Field(37, R.drawable.ic_grass),
    Plant(38, R.drawable.ic_category_natural),
    Building(39, R.drawable.ic_building)
}