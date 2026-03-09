package health.openwater.openlifu3dscanner.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_ownership")
data class ScanOwnershipEntity(
    @PrimaryKey val collectionName: String,
    val userId: String
)
