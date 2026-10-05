package com.jjas.labpomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.jjas.labpomodoro.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory ORDER BY atomicNumber")
    fun observeAll(): Flow<List<InventoryEntity>>

    /** Cuántos elementos distintos se han conseguido alguna vez (gastarlos no los borra de la tabla). */
    @Query("SELECT COUNT(*) FROM inventory WHERE firstObtainedAtMillis IS NOT NULL")
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

    /** Gasta [amount] unidades solo si alcanzan. Devuelve 0 si no había suficientes. */
    @Query("UPDATE inventory SET quantity = quantity - :amount WHERE atomicNumber = :atomicNumber AND quantity >= :amount")
    suspend fun consume(atomicNumber: Int, amount: Int): Int

    /** Los que se han conseguido alguna vez (aunque ya se hayan gastado en el sintetizador). */
    @Query("SELECT atomicNumber FROM inventory WHERE firstObtainedAtMillis IS NOT NULL")
    suspend fun discoveredAtomicNumbers(): List<Int>
}
