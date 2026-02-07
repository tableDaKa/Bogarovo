package com.bogarovo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: String,
    val stockItemId: Long?,
    val completed: Boolean = false
)
