package com.moviles.eventfinder.data.local

import androidx.room.*
import com.moviles.eventfinder.models.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Int?): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Query("UPDATE events SET isFromCache = :isFromCache")
    suspend fun updateCacheStatus(isFromCache: Boolean)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Int?)

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()

    @Query("SELECT MAX(lastSyncTimestamp) FROM events")
    suspend fun getLastSyncTimestamp(): Long?
}