package com.kylecorry.survival_aid.navigator

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The beacon database
 */
class BeaconDB(ctx: Context) {

    private val db = BeaconDBHelper(ctx).writableDatabase

    /**
     * The beacons in the DB
     */
    val beacons: List<Beacon>
        get(){
            val cursor = query(null, null)
            val locations: MutableList<Beacon> = mutableListOf()
            cursor.use { cursor ->
                if (cursor.count == 0){
                    return locations
                }
                cursor.moveToFirst()
                while(!cursor.isAfterLast){
                    locations.add(cursor.getBeacon())
                    cursor.moveToNext()
                }
            }
            return locations
        }

    /**
     * Create a location
     */
    fun create(location: Beacon){
        // Select where all fields are equal
        val values = getContentValues(location)
        db.insert(Beacon.DB_BEACON_TABLE, null, values)
    }

    /**
     * Delete a location
     */
    fun delete(location: Beacon){
        db.delete(Beacon.DB_BEACON_TABLE,
            Beacon.DB_NAME + " = ?",
            arrayOf(location.name))
    }

    private fun getContentValues(location: Beacon): ContentValues {
        val values = ContentValues()
        values.put(Beacon.DB_NAME, location.name)
        values.put(Beacon.DB_LAT, location.coordinate.latitude)
        values.put(Beacon.DB_LNG, location.coordinate.longitude)
        return values
    }

    private fun query(where: String?, whereArgs: Array<String>?): BeaconCursor{
        val cursor = db.query(
            Beacon.DB_BEACON_TABLE,
            null,
            where,
            whereArgs,
            null,
            null,
            null
        )

        return BeaconCursor(cursor)
    }

}

private class BeaconDBHelper(ctx: Context): SQLiteOpenHelper(ctx, "survive", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db ?: return
        db.execSQL("create table " + Beacon.DB_BEACON_TABLE + "(" +
                " _id integer primary key autoincrement, " +
                Beacon.DB_NAME + ", " +
                Beacon.DB_LAT + ", " +
                Beacon.DB_LNG +
                ")"
        );
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db ?: return
        db.execSQL("drop table " + Beacon.DB_BEACON_TABLE)
        this.onCreate(db)
    }
}

private class BeaconCursor(cursor: Cursor): CursorWrapper(cursor) {

    /**
     * Retrieve the beacon at the cursor location
     */
    fun getBeacon(): Beacon {
        var name = ""
        var lat = 0f
        var lng = 0f
        try {
            name = getString(getColumnIndex(Beacon.DB_NAME))
            lat = getFloat(getColumnIndex(Beacon.DB_LAT))
            lng = getFloat(getColumnIndex(Beacon.DB_LNG))
        } catch (e: Exception){}
        return Beacon(name, Coordinate(lat, lng))
    }
}