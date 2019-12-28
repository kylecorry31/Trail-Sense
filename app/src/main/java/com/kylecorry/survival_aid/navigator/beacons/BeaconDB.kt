package com.kylecorry.survival_aid.navigator.beacons

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kylecorry.survival_aid.navigator.gps.Coordinate

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
            val cursor = query(null, null, Beacon.DB_NAME)
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

        var beaconToCreate = location
        var uniqueNumber = 1
        while(get(beaconToCreate.name) != null){
            beaconToCreate = appendNumberToName(location, uniqueNumber)
            uniqueNumber++
        }

        val values = getContentValues(beaconToCreate)
        db.insert(Beacon.DB_BEACON_TABLE, null, values)
    }

    /**
     * Delete a location
     */
    fun delete(location: Beacon){
        db.delete(
            Beacon.DB_BEACON_TABLE,
            "${Beacon.DB_NAME} = ?",
            arrayOf(location.name))
    }

    /**
     * Gets the beacon with the given name
     * @param name the name of the beacon
     * @return the beacon with the name, or null if it doesn't exist
     */
    fun get(name: String): Beacon? {
        val cursor = query("${Beacon.DB_NAME} = ?", arrayOf(name))
        if (cursor.count != 0){
            cursor.moveToFirst()
            return cursor.getBeacon()
        }
        return null
    }

    private fun appendNumberToName(beacon: Beacon, number: Int): Beacon {
        val name = "${beacon.name} ($number)"
        return Beacon(name, beacon.coordinate)
    }

    private fun getContentValues(location: Beacon): ContentValues {
        val values = ContentValues()
        values.put(Beacon.DB_NAME, location.name)
        values.put(Beacon.DB_LAT, location.coordinate.latitude)
        values.put(Beacon.DB_LNG, location.coordinate.longitude)
        return values
    }

    private fun query(where: String?, whereArgs: Array<String>?, orderBy: String? = null): BeaconCursor {
        val cursor = db.query(
            Beacon.DB_BEACON_TABLE,
            null,
            where,
            whereArgs,
            null,
            null,
            orderBy
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
        )
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
        var lat = 0.0
        var lng = 0.0
        try {
            name = getString(getColumnIndex(Beacon.DB_NAME))
            lat = getDouble(getColumnIndex(Beacon.DB_LAT))
            lng = getDouble(getColumnIndex(Beacon.DB_LNG))
        } catch (e: Exception){}
        return Beacon(
            name,
            Coordinate(lat, lng)
        )
    }
}