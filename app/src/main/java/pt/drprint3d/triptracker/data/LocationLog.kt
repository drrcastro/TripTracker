package pt.drprint3d.triptracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_logs")
data class LocationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val mode: String, // "SINGLE_SMS", "LOOP_SMS", "JUST_LOG"
    val recipientPhone: String? = null,
    val recipientName: String? = null,
    val messageText: String? = null,
    val smsSent: Boolean = false
)
