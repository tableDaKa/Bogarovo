package com.bogarovo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bogarovo.data.AppDatabase
import com.bogarovo.data.StockItem
import com.bogarovo.data.TaskItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BogarovoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val stockDao = database.stockDao()
    private val taskDao = database.taskDao()

    val stockItems: StateFlow<List<StockItem>> = stockDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskItem>> = taskDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addStockItem(
        name: String,
        currentAmount: Double,
        unit: String,
        lowLimit: Double,
        batchSize: Double
    ) {
        viewModelScope.launch {
            stockDao.insert(
                StockItem(
                    name = name,
                    currentAmount = currentAmount,
                    unit = unit,
                    lowLimit = lowLimit,
                    batchSize = batchSize
                )
            )
        }
    }

    fun updateStockAmount(item: StockItem, newAmount: Double) {
        viewModelScope.launch {
            stockDao.update(item.copy(currentAmount = newAmount.coerceAtLeast(0.0)))
        }
    }

    fun bulkDeduct() {
        viewModelScope.launch {
            stockItems.value.forEach { item ->
                val updated = (item.currentAmount - item.batchSize).coerceAtLeast(0.0)
                stockDao.update(item.copy(currentAmount = updated))
            }
        }
    }

    fun addTask(title: String, dueDate: String, stockItemId: Long?) {
        viewModelScope.launch {
            taskDao.insert(
                TaskItem(
                    title = title,
                    dueDate = dueDate,
                    stockItemId = stockItemId,
                    completed = false
                )
            )
        }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            taskDao.update(updated)
            if (!task.completed && updated.completed && updated.stockItemId != null) {
                val stockItem = stockDao.getById(updated.stockItemId)
                if (stockItem != null) {
                    val newAmount = (stockItem.currentAmount - stockItem.batchSize).coerceAtLeast(0.0)
                    stockDao.update(stockItem.copy(currentAmount = newAmount))
                }
            }
        }
    }
}
