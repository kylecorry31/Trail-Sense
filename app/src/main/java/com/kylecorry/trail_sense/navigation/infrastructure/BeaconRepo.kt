package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.shared.DatabaseConnection

class BeaconRepo(context: Context) {

    private val conn: DatabaseConnection

    init {
        conn = DatabaseConnection(context, "survive", 4, { conn ->
            conn.transaction {
                createTables(conn)
            }
        }, { conn, oldVersion, newVersion ->
            conn.transaction {
                for (i in oldVersion..newVersion){
                    when (i + 1) {
                        2 -> {
                            conn.execute("ALTER TABLE beacons ADD COLUMN visible INTEGER NOT NULL DEFAULT 1")
                        }
                        3 -> {
                            conn.execute("ALTER TABLE beacons ADD COLUMN comment TEXT NULL DEFAULT NULL")
                            conn.execute("ALTER TABLE beacons ADD COLUMN beacon_group_id INTEGER NULL DEFAULT NULL")
                        }
                        4 -> {
                            conn.execute("ALTER TABLE beacons ADD COLUMN elevation REAL NULL DEFAULT NULL")
                        }
                    }
                }
                createTables(conn)
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
                    "insert into beacons (name, lat, lng, visible, comment, beacon_group_id, elevation) values (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(
                        beacon.name,
                        beacon.coordinate.latitude.toString(),
                        beacon.coordinate.longitude.toString(),
                        if (beacon.visible) "1" else "0",
                        beacon.comment,
                        beacon.beaconGroupId?.toString(),
                        beacon.elevation?.toString()
                    )
                )
            } else {
                // Update an existing beacon
                conn.execute(
                    "update beacons set name = ?, lat = ?, lng = ?, visible = ?, comment = ?, beacon_group_id = ? , elevation = ? where _id = ?",
                    arrayOf(
                        beacon.name,
                        beacon.coordinate.latitude.toString(),
                        beacon.coordinate.longitude.toString(),
                        if (beacon.visible) "1" else "0",
                        beacon.id.toString(),
                        beacon.comment,
                        beacon.beaconGroupId?.toString(),
                        beacon.elevation?.toString()
                    )
                )
            }
        }
        conn.close()
    }

    private fun createTables(conn: DatabaseConnection) {
        conn.execute("CREATE TABLE IF NOT EXISTS beacons (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, visible INTEGER NOT NULL, comment TEXT NULL, beacon_group_id INTEGER NULL, elevation REAL NULL)")
        conn.execute("CREATE TABLE IF NOT EXISTS beacon_groups (beacon_group_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, group_name TEXT NOT NULL)")
    }

}