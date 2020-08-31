package com.kylecorry.trail_sense.shared.database

import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.*
import androidx.core.database.sqlite.transaction
import java.lang.Exception

/**
 * A database connection
 * @param context The context
 * @param dbName The name of the database to connect to
 * @param version The version of the database
 * @param onCreate The code to run when the database is first created - DO NOT OPEN OR CLOSE THE CONNECTION HERE
 * @param onUpgrade The code to run when the database version changes - DO NOT OPEN OR CLOSE THE CONNECTION HERE
 */
class DatabaseConnection(
    context: Context,
    dbName: String,
    version: Int,
    onCreate: (connection: DatabaseConnection) -> Unit,
    onUpgrade: (connection: DatabaseConnection, oldVersion: Int, newVersion: Int) -> Unit
) {
    private val helper: SQLiteOpenHelper

    private var database: SQLiteDatabase? = null

    init {
        val inst = this
        helper = object : SQLiteOpenHelper(context, dbName, null, version) {
            override fun onCreate(db: SQLiteDatabase?) {
                database = db
                onCreate(inst)
            }

            override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                database = db
                onUpgrade(inst, oldVersion, newVersion)
            }
        }
    }

    fun transaction(fn: () -> Unit){
        guardClosedDb()
        database?.transaction {
            fn()
        }
    }

    fun execute(sql: String, args: Array<String?>? = null) {
        guardClosedDb()
        if (args == null){
            database?.execSQL(sql)
        } else {
            database?.execSQL(sql, args)
        }
    }

    fun <T> query(dtoFactory: () -> Dto<T>, sql: String, args: Array<String?>? = null): T? {
        guardClosedDb()
        val cursor = database?.rawQuery(sql, args)
        var dto: Dto<T>? = null
        cursor?.apply {
            if (count != 0) {
                moveToFirst()
                dto = MyCursor(dtoFactory, cursor).getDto()
            }
        }
        cursor?.close()
        return dto?.toObject()
    }

    fun <T> queryAll(dtoFactory: () -> Dto<T>, sql: String, args: Array<String?>? = null): Collection<T> {
        guardClosedDb()
        val cursor = database?.rawQuery(sql, args)
        val list = mutableListOf<T>()
        cursor?.apply {
            if (count != 0) {
                moveToFirst()
                val myCursor = MyCursor(dtoFactory, this)
                while (!isAfterLast) {
                    list.add(myCursor.getDto().toObject())
                    moveToNext()
                }
            }
        }
        cursor?.close()
        return list
    }

    fun open() {
        if (database != null){
            return
        }
        database?.close()
        database = helper.writableDatabase
    }

    fun close() {
        database?.close()
        database = null
    }

    private fun guardClosedDb() {
        database
            ?: throw Exception("Database is closed, please call DatabaseConnection.open() before using.")
    }

    private class MyCursor<T>(private val dtoFactory: () -> Dto<T>, cursor: Cursor) : CursorWrapper(cursor) {

        fun getDto(): Dto<T> {
            val dto = dtoFactory.invoke()
            val properties = dto.getProperties()
            for (column in columnNames) {
                val columnIdx = getColumnIndex(column)
                if (properties.containsKey(column)) {
                    dto.set(
                        column, when (properties[column]) {
                            SqlType.Short -> getShort(columnIdx)
                            SqlType.Int -> getInt(columnIdx)
                            SqlType.Long -> getLong(columnIdx)
                            SqlType.Float -> getFloat(columnIdx)
                            SqlType.Double -> getDouble(columnIdx)
                            SqlType.String -> getString(columnIdx)
                            SqlType.Boolean -> getInt(columnIdx) == 1
                            SqlType.NullableShort -> getShortOrNull(columnIdx)
                            SqlType.NullableInt -> getIntOrNull(columnIdx)
                            SqlType.NullableLong -> getLongOrNull(columnIdx)
                            SqlType.NullableFloat -> getFloatOrNull(columnIdx)
                            SqlType.NullableDouble -> getDoubleOrNull(columnIdx)
                            SqlType.NullableString -> getStringOrNull(columnIdx)
                            SqlType.NullableBoolean -> {
                                val value = getIntOrNull(columnIdx)
                                if (value == null) null else value == 1
                            }
                            else -> null
                        }
                    )
                }
            }

            return dto
        }
    }
}