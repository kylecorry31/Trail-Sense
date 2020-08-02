package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.shared.DatabaseConnection

class BeaconRepo(context: Context) {

    private val conn: DatabaseConnection

    init {
        conn = DatabaseConnection(context, "survive", 2, { conn ->
            conn.transaction {
                conn.execute("create table if not exists beacons (_id integer primary key autoincrement, name, lat, lng, visible)")
            }
        }, { conn, oldVersion, _ ->
            conn.transaction {
                when (oldVersion) {
                    1 -> conn.execute("ALTER TABLE beacons ADD COLUMN visible DEFAULT 1")
                }
            }
        })
    }

    fun get(): Collection<Beacon> {
        conn.open()
        val beacons = conn.queryAll({ BeaconDto() }, "select * from beacons")
        conn.close()
        return beacons
    }

    fun delete(beacon: Beacon) {
        conn.open()
        conn.transaction {
            conn.execute("delete from beacons where _id = ?", arrayOf(beacon.id.toString()))
        }
        conn.close()
    }

    fun add(beacon: Beacon) {
        conn.open()
        conn.transaction {
            if (beacon.id == 0) {
                // Create a new beacon
                conn.execute(
                    "insert into beacons (name, lat, lng, visible) values (?, ?, ?, ?)",
                    arrayOf(
                        beacon.name,
                        beacon.coordinate.latitude.toString(),
                        beacon.coordinate.longitude.toString(),
                        if (beacon.visible) "1" else "0"
                    )
                )
            } else {
                // Update an existing beacon
                conn.execute(
                    "update beacons set name = ?, lat = ?, lng = ?, visible = ? where _id = ?",
                    arrayOf(
                        beacon.name,
                        beacon.coordinate.latitude.toString(),
                        beacon.coordinate.longitude.toString(),
                        if (beacon.visible) "1" else "0",
                        beacon.id.toString()
                    )
                )
            }
        }
        conn.close()
    }

}