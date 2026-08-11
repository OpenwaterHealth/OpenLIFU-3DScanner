package health.openwater.openlifu3dscanner.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScanOwnershipEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanOwnershipDao(): ScanOwnershipDao
}
