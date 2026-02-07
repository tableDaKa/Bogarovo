package com.bogarovo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentAmount: Double,
    val unit: String,
    val lowLimit: Double,
    val batchSize: Double
)
