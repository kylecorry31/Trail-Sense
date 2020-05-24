package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.shared.Coordinate

/**
 * The beacon database
 */
class BeaconDB(ctx: Context) {

    private val db = BeaconDBHelper(
        ctx
    ).writableDatabase

    /**
     * The beacons in the DB
     */
    val beacons: List<Beacon>
        get() {
            val cursor = query(
                null, null,
                BEACON_NAME
            )
            val locations: MutableList<Beacon> = mutableListOf()
            cursor.use { c ->
                if (c.count == 0) {
                    return locations
                }
                c.moveToFirst()
                while (!c.isAfterLast) {
                    locations.add(c.getBeacon())
                    c.moveToNext()
                }
            }
            return locations
        }

    /**
     * Create a location
     */
    fun create(location: Beacon) {

        var beaconToCreate = location
        var uniqueNumber = 1
        while (get(beaconToCreate.name) != null) {
            beaconToCreate = appendNumberToName(location, uniqueNumber)
            uniqueNumber++
        }

        val values = getContentValues(beaconToCreate)
        db.insert(BEACON_TABLE, null, values)
    }

    fun update(location: Beacon) {
        val values = getContentValues(location)
        db.update(
            BEACON_TABLE,
            values,
            "$BEACON_NAME = ?",
            arrayOf(location.name)
        )
    }

    /**
     * Delete a location
     */
    fun delete(location: Beacon) {
        db.delete(
            BEACON_TABLE,
            "$BEACON_NAME = ?",
            arrayOf(location.name)
        )
    }

    /**
     * Gets the beacon with the given name
     * @param name the name of the beacon
     * @return the beacon with the name, or null if it doesn't exist
     */
    fun get(name: String): Beacon? {
        val cursor = query("$BEACON_NAME = ?", arrayOf(name))
        if (cursor.count != 0) {
            cursor.moveToFirst()
            return cursor.getBeacon()
        }
        return null
    }

    private fun appendNumberToName(beacon: Beacon, number: Int): Beacon {
        val name = "${beacon.name} ($number)"
        return Beacon(
            name,
            beacon.coordinate,
            beacon.visible
        )
    }

    private fun getContentValues(location: Beacon): ContentValues {
        val values = ContentValues()
        values.put(BEACON_NAME, location.name)
        values.put(BEACON_LAT, location.coordinate.latitude)
        values.put(BEACON_LNG, location.coordinate.longitude)
        values.put(BEACON_VISIBLE, location.visible)
        return values
    }

    private fun query(
        where: String?,
        whereArgs: Array<String>?,
        orderBy: String? = null
    ): BeaconCursor {
        val cursor = db.query(
            BEACON_TABLE,
            null,
            where,
            whereArgs,
            null,
            null,
            orderBy
        )

        return BeaconCursor(
            cursor
        )
    }

    companion object {
        const val BEACON_TABLE = "beacons"
        const val BEACON_NAME = "name"
        const val BEACON_LAT = "lat"
        const val BEACON_LNG = "lng"
        const val BEACON_VISIBLE = "visible"
    }


}

private class BeaconDBHelper(ctx: Context) : SQLiteOpenHelper(ctx, "survive", null, 2) {
    override fun onCreate(db: SQLiteDatabase?) {
        db ?: return
        db.execSQL(
            "create table if not exists " + BeaconDB.BEACON_TABLE + "(" +
                    " _id integer primary key autoincrement, " +
                    BeaconDB.BEACON_NAME + ", " +
                    BeaconDB.BEACON_LAT + ", " +
                    BeaconDB.BEACON_LNG + ", " +
                    BeaconDB.BEACON_VISIBLE +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db ?: return
        when (oldVersion) {
            1 -> {
                val sql =
                    "ALTER TABLE ${BeaconDB.BEACON_TABLE} ADD COLUMN ${BeaconDB.BEACON_VISIBLE} DEFAULT 1"
                db.execSQL(sql)
            }
        }
        this.onCreate(db)
    }
}

private class BeaconCursor(cursor: Cursor) : CursorWrapper(cursor) {

    /**
     * Retrieve the beacon at the cursor location
     */
    fun getBeacon(): Beacon {
        var name = ""
        var lat = 0.0
        var lng = 0.0
        var visible = false
        try {
            name = getString(getColumnIndex(BeaconDB.BEACON_NAME))
            lat = getDouble(getColumnIndex(BeaconDB.BEACON_LAT))
            lng = getDouble(getColumnIndex(BeaconDB.BEACON_LNG))
            visible = getInt(getColumnIndex(BeaconDB.BEACON_VISIBLE)) == 1
        } catch (e: Exception) {
        }
        return Beacon(
            name,
            Coordinate(lat, lng),
            visible
        )
    }
}