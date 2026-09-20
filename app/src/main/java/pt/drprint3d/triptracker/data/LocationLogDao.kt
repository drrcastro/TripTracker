package pt.drprint3d.triptracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LocationLog): Long

    @Query("SELECT * FROM location_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<LocationLog>>

    @Query("SELECT * FROM location_logs ORDER BY timestamp ASC")
    suspend fun getAllLogsAsc(): List<LocationLog>

    @Query("SELECT COUNT(*) FROM location_logs")
    fun getLogCountFlow(): Flow<Int>

    @Query("DELETE FROM location_logs")
    suspend fun clearAll()
}
