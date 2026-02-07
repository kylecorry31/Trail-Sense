---
name: trail-sense-database-persistence
description: Add new Room database persistence to Trail-Sense Android app. Use when the user asks to create, add, or implement database persistence for a model, including Entity, DAO, Repository, and AppDatabase migration. Covers entity-to-model mapping, index configuration, and standard CRUD operations.
---

# Trail-Sense Database Persistence

Add Room database persistence for a domain model following Trail-Sense patterns.

## File Locations

```
app/src/main/java/com/kylecorry/trail_sense/tools/{toolName}/
├── domain/
│   └── {Model}.kt                    # Domain model (if not already exists)
└── infrastructure/persistence/
    ├── {Model}Entity.kt              # Room entity
    ├── {Model}Dao.kt                 # DAO interface
    └── {Model}Repo.kt                # Repository
```

Also update:
- `app/src/main/java/com/kylecorry/trail_sense/main/persistence/AppDatabase.kt`
- `app/src/main/java/com/kylecorry/trail_sense/main/persistence/Converters.kt` (if new types needed)
- `{ToolName}ToolRegistration.kt` - register repo singleton

## Workflow

1. Create Entity with mapping functions
2. Create DAO interface
3. Add DAO to AppDatabase and create migration
4. Create Repository
5. Register repo singleton in ToolRegistration

## 1. Entity

```kotlin
package com.kylecorry.trail_sense.tools.{toolname}.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kylecorry.trail_sense.tools.{toolname}.domain.{Model}

@Entity(
    tableName = "{table_name}",  // plural, lowercase, snake_case
    indices = [
        // Add indices for foreign keys and frequently queried columns
        // Index(value = ["parent_id"]),
        // Index(value = ["time"])
    ]
)
data class {Model}Entity(
    @ColumnInfo(name = "column_name") val property: Type,
    // ... other properties
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun to{Model}(): {Model} {
        return {Model}(
            id = id,
            // Map entity properties to domain model
        )
    }

    companion object {
        fun from(model: {Model}): {Model}Entity {
            return {Model}Entity(
                // Map domain model properties to entity
            ).also {
                it.id = model.id
            }
        }
    }
}
```

### Index Guidelines

Add indices for:
- Foreign key columns (e.g., `parent_id`, `group_id`)
- Time-based columns if queried by time range
- Columns used in WHERE clauses frequently
- Composite indices for multi-column filters: `Index(value = ["col1", "col2"])`

### Type Mapping

- `Instant` -> stored as `Long` (epoch millis), converter exists
- `Duration` -> stored as `Long` (millis), converter exists
- `Coordinate` -> split into `latitude: Double`, `longitude: Double`
- `Distance` -> store as `Float` in meters
- Enums with `id` property -> use existing converters or add to `Converters.kt`
- `AppColor` -> converter exists
- Lists/collections -> join to comma-separated string, parse in mapping

## 2. DAO

```kotlin
package com.kylecorry.trail_sense.tools.{toolname}.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface {Model}Dao {
    @Query("SELECT * FROM {table_name}")
    fun getAll(): Flow<List<{Model}Entity>>

    @Query("SELECT * FROM {table_name}")
    suspend fun getAllSync(): List<{Model}Entity>

    @Query("SELECT * FROM {table_name} WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): {Model}Entity?

    @Upsert
    suspend fun upsert(entity: {Model}Entity): Long

    @Delete
    suspend fun delete(entity: {Model}Entity)
}
```

### Optional DAO Methods

```kotlin
// Filter by parent/group
@Query("SELECT * FROM {table_name} WHERE parent_id IS :parentId")
suspend fun getAllInGroup(parentId: Long?): List<{Model}Entity>

// Time-based cleanup
@Query("DELETE FROM {table_name} WHERE time < :minEpochMillis")
suspend fun deleteOlderThan(minEpochMillis: Long)

// Get latest
@Query("SELECT * FROM {table_name} ORDER BY _id DESC LIMIT 1")
suspend fun getLast(): {Model}Entity?
```

## 3. AppDatabase Updates

### Add Entity to Database

In `AppDatabase.kt`, add entity to `@Database` annotation:

```kotlin
@Database(
    entities = [..., {Model}Entity::class],
    version = {NEXT_VERSION},  // Increment from current
    exportSchema = false
)
```

### Add DAO Accessor

```kotlin
abstract fun {model}Dao(): {Model}Dao
```

### Add Migration

Inside `buildDatabase()`, add migration before the `return Room.databaseBuilder`:

```kotlin
val MIGRATION_{PREV}_{NEXT} = object : Migration({PREV}, {NEXT}) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `{table_name}` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `column_name` TEXT NOT NULL,
                `nullable_column` TEXT DEFAULT NULL
                -- Match column types: TEXT, INTEGER, REAL
                -- NOT NULL for non-nullable, DEFAULT NULL for nullable
            )
        """.trimIndent())

        // Add indices if defined in entity
        // db.execSQL("CREATE INDEX IF NOT EXISTS index_{table_name}_{column} ON {table_name}({column})")
    }
}
```

### Register Migration

Add to `.addMigrations()`:

```kotlin
.addMigrations(
    ...,
    MIGRATION_{PREV}_{NEXT}
)
```

## 4. Repository

```kotlin
package com.kylecorry.trail_sense.tools.{toolname}.infrastructure.persistence

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.{toolname}.domain.{Model}
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class {Model}Repo private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).{model}Dao()

    fun getAll(): Flow<List<{Model}>> = dao.getAll()
        .map { it.map { entity -> entity.to{Model}() } }
        .flowOn(Dispatchers.IO)

    suspend fun getAllSync(): List<{Model}> = onIO {
        dao.getAllSync().map { it.to{Model}() }
    }

    suspend fun get(id: Long): {Model}? = onIO {
        dao.get(id)?.to{Model}()
    }

    suspend fun add(model: {Model}): Long = onIO {
        dao.upsert({Model}Entity.from(model))
    }

    suspend fun delete(model: {Model}) = onIO {
        dao.delete({Model}Entity.from(model))
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: {Model}Repo? = null

        @Synchronized
        fun getInstance(context: Context): {Model}Repo {
            if (instance == null) {
                instance = {Model}Repo(context.applicationContext)
            }
            return instance!!
        }
    }
}
```

## 5. Register Singleton in ToolRegistration

In `{ToolName}ToolRegistration.kt`, add the repo to `singletons`:

```kotlin
object {ToolName}ToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            // ... other properties
            singletons = listOf(
                {Model}Repo::getInstance
            ),
            // ...
        )
    }
}
```

## SQL Type Reference

| Kotlin Type | SQLite Type | Notes |
|-------------|-------------|-------|
| `Long`, `Int` | `INTEGER` | |
| `Double`, `Float` | `REAL` | |
| `String` | `TEXT` | |
| `Boolean` | `INTEGER` | 0/1 |
| `Instant` | `INTEGER` | epoch millis |
| Enums | `INTEGER` | via id property |
| Nullable | Add `DEFAULT NULL` | |
