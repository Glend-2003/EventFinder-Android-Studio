package com.moviles.eventfinder.models
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: Int? = null,
    val name: String,
    val date: String,
    val location: String,
    val description: String,
    val image: String?,
    val isFromCache: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
