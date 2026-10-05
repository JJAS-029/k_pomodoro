package com.jjas.labpomodoro.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LabDatabase::class.java)

    @Test
    fun v1_a_v2_conserva_historial_e_inventario() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO inventory (atomicNumber, quantity, firstObtainedAtMillis) VALUES (26, 3, 1000)")
            db.execSQL(
                "INSERT INTO sessions (type, startedAtMillis, endedAtMillis, plannedSeconds, actualSeconds, completed, epochDay, hourOfDay) " +
                    "VALUES ('WORK', 0, 1500000, 1500, 1500, 1, 20000, 9)"
            )
        }
        // Valida contra el esquema 2 exportado
        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT quantity FROM inventory WHERE atomicNumber = 26").use {
                it.moveToFirst()
                assertEquals(3, it.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM sessions").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM discoveries").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
