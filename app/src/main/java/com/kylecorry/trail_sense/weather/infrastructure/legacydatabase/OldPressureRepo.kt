package com.kylecorry.trail_sense.weather.infrastructure.legacydatabase

import android.content.Context
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.persistence.DatabaseConnection

class OldPressureRepo private constructor(private val context: Context) {

    private val conn: DatabaseConnection

    init {
        conn = DatabaseConnection(context, "weather", 1, { conn ->
            conn.transaction {
                createTables(conn)
            }
        }, { conn, oldVersion, newVersion ->
            conn.transaction {
                createTables(conn)
            }
        })
    }


    fun get(): Collection<PressureAltitudeReading> {
        conn.open()
        val pressures = conn.queryAll({ PressureReadingDto() }, "select * from pressures")
        conn.close()
        return pressures
    }

    fun deleteAll() {
        try {
            conn.open()
            conn.transaction {
                conn.execute("delete from pressures")
            }
            conn.close()
        } catch (e: Exception){
            // Don't do anything - it isn't the end of the world
        }
    }

    private fun createTables(conn: DatabaseConnection) {
        conn.execute("CREATE TABLE IF NOT EXISTS pressures (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time INTEGER NOT NULL, pressure REAL NOT NULL, altitude REAL NOT NULL, altitude_accuracy REAL NULL, temperature REAL NOT NULL)")
    }


    companion object {
        private var instance: OldPressureRepo? = null

        @Synchronized fun getInstance(context: Context): OldPressureRepo {
            if (instance == null){
                instance = OldPressureRepo(context.applicationContext)
            }
            return instance!!
        }

    }

}