package com.jjas.labpomodoro.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.jjas.labpomodoro.data.local.LabDatabase
import com.jjas.labpomodoro.data.local.dao.InventoryDao
import com.jjas.labpomodoro.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LabDatabase =
        Room.databaseBuilder(context, LabDatabase::class.java, LabDatabase.NAME)
            .addCallback(LabDatabase.SeedInventory)
            .build()

    @Provides
    fun provideSessionDao(db: LabDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideInventoryDao(db: LabDatabase): InventoryDao = db.inventoryDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    /** Reloj inyectable: en los tests se reemplaza por uno fijo. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
