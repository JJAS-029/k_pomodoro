package com.jjas.labpomodoro.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.jjas.labpomodoro.data.local.LabDatabase
import com.jjas.labpomodoro.data.local.dao.DiscoveryDao
import com.jjas.labpomodoro.data.local.dao.InventoryDao
import com.jjas.labpomodoro.data.local.dao.PlaceDao
import com.jjas.labpomodoro.data.local.dao.TagDao
import com.jjas.labpomodoro.data.repository.PrefsPrizeStore
import com.jjas.labpomodoro.data.repository.PrizeStore
import com.jjas.labpomodoro.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
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
    fun provideDiscoveryDao(db: LabDatabase): DiscoveryDao = db.discoveryDao()

    @Provides
    fun providePlaceDao(db: LabDatabase): PlaceDao = db.placeDao()

    @Provides
    fun provideTagDao(db: LabDatabase): TagDao = db.tagDao()

    @Provides
    fun providePrizeStore(store: PrefsPrizeStore): PrizeStore = store

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    /**
     * Reloj inyectable: en los tests se reemplaza por uno fijo. Lee la zona horaria en cada uso
     * (Clock.systemDefaultZone la fija al crearse): si el usuario viaja o cambia la zona con la app
     * abierta, "hoy" sigue siendo su día.
     */
    @Provides
    fun provideClock(): Clock = SystemZoneClock
}

private object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
}
