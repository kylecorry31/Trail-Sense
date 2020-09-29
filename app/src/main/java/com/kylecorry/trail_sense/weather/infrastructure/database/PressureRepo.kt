package com.kylecorry.trail_sense.weather.infrastructure.database

import android.content.Context
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.persistence.DatabaseConnection
import java.time.Instant

class PressureRepo(private val context: Context) {

    private val conn: DatabaseConnection

    init {
        conn = DatabaseConnection(context, "weather", 1, { conn ->
            conn.transaction {
                createTables(conn)
            }
        }, { conn, oldVersion, newVersion ->
            conn.transaction {
                // Do nothing yet
//                for (i in oldVersion..newVersion) {
//                    when (i + 1) {
//
//                    }
//                }
                createTables(conn)
            }
        })
    }

    fun get(id: Int): PressureAltitudeReading? {
        conn.open()
        val pressure = conn.query(
            { PressureReadingDto() },
            "select * from pressures where _id = ?",
            arrayOf(id.toString())
        )
        conn.close()
        return pressure
    }

    fun get(): Collection<PressureAltitudeReading> {
        conn.open()
        val pressures = conn.queryAll({ PressureReadingDto() }, "select * from pressures")
        conn.close()
        return pressures
    }

    fun deleteOlderThan(time: Instant) {
        conn.open()
        conn.transaction {
            conn.execute(
                "delete from pressures where time < ?",
                arrayOf(time.toEpochMilli().toString())
            )
        }
        conn.close()
    }

    fun add(
        reading: PressureAltitudeReading,
        connection: DatabaseConnection = conn,
        shouldOpen: Boolean = true
    ) {
        if (shouldOpen) {
            connection.open()
        }

        conn.transaction {
            conn.execute(
                "insert into pressures (time, pressure, altitude, altitude_accuracy, temperature) values (?, ?, ?, ?, ?)",
                arrayOf(
                    reading.time.toEpochMilli().toString(),
                    reading.pressure.toString(),
                    reading.altitude.toString(),
                    null,
                    reading.temperature.toString()
                )
            )
        }

        if (shouldOpen) {
            conn.close()
        }
    }

    private fun createTables(conn: DatabaseConnection) {
        val history = PressureHistoryRepository.get(context)
        conn.execute("CREATE TABLE IF NOT EXISTS pressures (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time INTEGER NOT NULL, pressure REAL NOT NULL, altitude REAL NOT NULL, altitude_accuracy REAL NULL, temperature REAL NOT NULL)")
        if (history.isNotEmpty()) {
            history.forEach {
                add(it, conn, false)
            }
        }
//        try {
        PressureHistoryRepository.clear(context)
//        } catch (e: Exception) {
//            // Ignore this
//        }
    }

}