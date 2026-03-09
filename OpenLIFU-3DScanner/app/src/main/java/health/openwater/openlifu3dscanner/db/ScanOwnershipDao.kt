package health.openwater.openlifu3dscanner.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScanOwnershipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanOwnershipEntity)

    @Query("SELECT collectionName FROM scan_ownership WHERE userId = :userId")
    suspend fun getCollectionNamesForUser(userId: String): List<String>

    @Query("SELECT collectionName FROM scan_ownership")
    suspend fun getAllOwnedCollectionNames(): List<String>
}
