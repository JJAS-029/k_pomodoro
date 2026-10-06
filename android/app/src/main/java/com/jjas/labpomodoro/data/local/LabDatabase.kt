package com.jjas.labpomodoro.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jjas.labpomodoro.data.local.dao.DiscoveryDao
import com.jjas.labpomodoro.data.local.dao.InventoryDao
import com.jjas.labpomodoro.data.local.dao.PlaceDao
import com.jjas.labpomodoro.data.local.dao.SessionDao
import com.jjas.labpomodoro.data.local.entity.DiscoveryEntity
import com.jjas.labpomodoro.data.local.entity.InventoryEntity
import com.jjas.labpomodoro.data.local.entity.PlaceEntity
import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.PeriodicTable

@Database(
    entities = [SessionEntity::class, InventoryEntity::class, DiscoveryEntity::class, PlaceEntity::class],
    version = 3,
    exportSchema = true,
    // v2 (Fase 4): tabla de hallazgos. v3: lugares
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class LabDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun discoveryDao(): DiscoveryDao
    abstract fun placeDao(): PlaceDao

    companion object {
        const val NAME = "lab_pomodoro.db"
    }

    /** Precarga una fila por elemento (cantidad 0) al crear la base de datos. */
    object SeedInventory : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.beginTransaction()
            try {
                for (z in 1..PeriodicTable.SIZE) {
                    db.execSQL("INSERT INTO inventory (atomicNumber, quantity) VALUES ($z, 0)")
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
