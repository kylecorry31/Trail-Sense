package com.kylecorry.trail_sense.navigation.infrastructure.database

import android.content.Context
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.infrastructure.persistence.DatabaseConnection

class OldBeaconRepo private constructor(context: Context) {

    private val conn: DatabaseConnection

    init {
        conn = DatabaseConnection(context, "survive", 5, { conn ->
            conn.transaction {
                createTables(conn)
            }
        }, { conn, oldVersion, newVersion ->
            conn.transaction {
                for (i in oldVersion..newVersion) {
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
                        5 -> {
                            conn.execute("ALTER TABLE beacons ADD COLUMN temporary INTEGER NOT NULL DEFAULT 0")
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

    fun getGroups(): Collection<BeaconGroup> {
        conn.open()
        val groups = conn.queryAll({ BeaconGroupDto() }, "select * from beacon_groups")
        conn.close()
        return groups
    }


    private fun createTables(conn: DatabaseConnection) {
        conn.execute("CREATE TABLE IF NOT EXISTS beacons (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, visible INTEGER NOT NULL, comment TEXT NULL, beacon_group_id INTEGER NULL, elevation REAL NULL, temporary INTEGER NOT NULL)")
        conn.execute("CREATE TABLE IF NOT EXISTS beacon_groups (beacon_group_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, group_name TEXT NOT NULL)")
    }


    companion object {
        private var instance: OldBeaconRepo? = null

        @Synchronized fun getInstance(context: Context): OldBeaconRepo {
            if (instance == null) {
                instance = OldBeaconRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}