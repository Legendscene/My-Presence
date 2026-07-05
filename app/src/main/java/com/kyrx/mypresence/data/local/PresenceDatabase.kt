package com.kyrx.mypresence.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PresenceDatabase : RoomDatabase()
