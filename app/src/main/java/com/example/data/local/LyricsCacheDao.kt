package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsCacheDao {

    @Query("SELECT * FROM lyrics_cache ORDER BY fetchedAtEpochMs DESC")
    fun observeAll(): Flow<List<LyricsCacheEntity>>

    @Query("SELECT * FROM lyrics_cache WHERE trackKey = :trackKey LIMIT 1")
    suspend fun findByKey(trackKey: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE trackKey = :trackKey")
    suspend fun deleteByKey(trackKey: String)

    @Query("DELETE FROM lyrics_cache WHERE expiresAtEpochMs <= :nowEpochMs")
    suspend fun deleteExpired(nowEpochMs: Long)

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearAll()
}
