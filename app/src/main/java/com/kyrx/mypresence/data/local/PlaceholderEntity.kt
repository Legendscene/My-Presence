package com.kyrx.mypresence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "placeholder")
data class PlaceholderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
