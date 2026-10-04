package com.jjas.labpomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.jjas.labpomodoro.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory ORDER BY atomicNumber")
    fun observeAll(): Flow<List<InventoryEntity>>

    /** Cuántos elementos distintos se han conseguido (cantidad > 0). */
    @Query("SELECT COUNT(*) FROM inventory WHERE quantity > 0")
    fun observeDiscoveredCount(): Flow<Int>

    /** Suma [amount] unidades y registra la fecha del primer hallazgo. Devuelve las filas afectadas. */
    @Query(
        """
        UPDATE inventory
        SET quantity = quantity + :amount,
            firstObtainedAtMillis = COALESCE(firstObtainedAtMillis, :nowMillis)
        WHERE atomicNumber = :atomicNumber
        """
    )
    suspend fun add(atomicNumber: Int, amount: Int, nowMillis: Long): Int
}
