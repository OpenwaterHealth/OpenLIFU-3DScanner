package health.openwater.openlifu3dscanner.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import health.openwater.openlifu3dscanner.db.AppDatabase
import health.openwater.openlifu3dscanner.db.ScanOwnershipDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "openlifu_db").build()

    @Provides
    @Singleton
    fun provideScanOwnershipDao(db: AppDatabase): ScanOwnershipDao = db.scanOwnershipDao()
}
