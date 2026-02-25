package com.kylecorry.trail_sense.tools.beacons.domain

import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.data.Identifiable

// Last ID: 64
enum class BeaconIcon(
    override val id: Long,
    @DrawableRes val icon: Int,
    val isUserSelectable: Boolean = true
) : Identifiable {
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
    Beach(19, R.drawable.ic_beach),
    TideHigh(48, R.drawable.ic_tide_high, isUserSelectable = false),
    TideLow(49, R.drawable.ic_tide_low, isUserSelectable = false),
    TideHalf(50, R.drawable.ic_tide_half, isUserSelectable = false),
    Tree(21, R.drawable.tree),
    Field(37, R.drawable.ic_grass),
    Plant(38, R.drawable.ic_category_natural),
    Track(62, R.drawable.paw),

    // Amenities
    Food(12, R.drawable.ic_category_food),
    Picnic(36, R.drawable.ic_picnic),
    WaterRefill(6, R.drawable.ic_category_water),
    Restroom(7, R.drawable.ic_restrooms),
    Phone(8, R.drawable.ic_phone),
    Trash(16, R.drawable.ic_delete),
    CellSignal(22, R.drawable.signal_cellular_3),
    CellTower(47, R.drawable.cell_tower),
    WiFi(23, R.drawable.ic_wifi),
    FirstAid(24, R.drawable.ic_category_medical),
    Power(32, R.drawable.ic_torch_on),
    Music(46, R.drawable.ic_music),

    // Buildings
    Tent(5, R.drawable.ic_category_shelter),
    House(10, R.drawable.ic_house),
    Building(39, R.drawable.ic_building),
    Cabin(31, R.drawable.ic_cabin),
    Barn(40, R.drawable.ic_barn),
    Historic(33, R.drawable.ic_ruins),
    Cemetery(41, R.drawable.ic_grave),
    FireTower(42, R.drawable.ic_fire_tower),
    Bridge(43, R.drawable.ic_bridge),
    Lighthouse(44, R.drawable.ic_lighthouse),
    VisitorCenter(45, R.drawable.ic_visitor_center),

    // Animals and Field Guide Tags
    Animal(51, R.drawable.paw, isUserSelectable = false),
    Bird(52, R.drawable.bird, isUserSelectable = false),
    Mammal(53, R.drawable.ic_deer, isUserSelectable = false),
    Reptile(54, R.drawable.lizard, isUserSelectable = false),
    Amphibian(55, R.drawable.frog, isUserSelectable = false),
    Fish(56, R.drawable.fish, isUserSelectable = false),
    Invertebrate(57, R.drawable.ant, isUserSelectable = false),
    Fungus(58, R.drawable.mushroom, isUserSelectable = false),
    Rock(59, R.drawable.gem, isUserSelectable = false),
    Weather(60, R.drawable.cloud, isUserSelectable = false),

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
    Arrow(61, R.drawable.ic_arrow, isUserSelectable = false),
    TrailCam(63, R.drawable.trailcam),
    Trap(64, R.drawable.trap)
}