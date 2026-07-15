package com.riad.bizaccount.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import com.riad.bizaccount.data.repository.CategoryInUseException
import com.riad.bizaccount.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CategoryUiEvent {
    data class DeleteBlocked(val count: Int) : CategoryUiEvent()
    object Saved : CategoryUiEvent()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    val incomeCategories = repository.byType(TransactionType.INCOME)
    val expenseCategories = repository.byType(TransactionType.EXPENSE)

    private val _events = MutableSharedFlow<CategoryUiEvent>()
    val events = _events.asSharedFlow()

    fun addCategory(name: String, type: TransactionType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.add(CategoryEntity(name = name.trim(), type = type))
            _events.emit(CategoryUiEvent.Saved)
        }
    }

    fun renameCategory(category: CategoryEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.update(category.copy(name = newName.trim()))
            _events.emit(CategoryUiEvent.Saved)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (e: CategoryInUseException) {
                _events.emit(CategoryUiEvent.DeleteBlocked(e.transactionCount))
            }
        }
    }
}
