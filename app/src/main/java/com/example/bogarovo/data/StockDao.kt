package com.example.bogarovo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun observeAll(): Flow<List<StockItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StockItem)

    @Update
    suspend fun update(item: StockItem)

    @Query("SELECT * FROM stock_items WHERE id = :id")
    suspend fun getById(id: Long): StockItem?
}
