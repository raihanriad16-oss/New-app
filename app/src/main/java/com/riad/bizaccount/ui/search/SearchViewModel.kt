package com.riad.bizaccount.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riad.bizaccount.data.local.entity.PaymentMethod
import com.riad.bizaccount.data.local.entity.TransactionType
import com.riad.bizaccount.data.repository.CategoryRepository
import com.riad.bizaccount.data.repository.SortOption
import com.riad.bizaccount.data.repository.TransactionFilters
import com.riad.bizaccount.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(TransactionFilters())
    val filters: StateFlow<TransactionFilters> = _filters.asStateFlow()

    val allCategories = categoryRepository.all()

    val results = _filters
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { f -> transactionRepository.search(f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _filters.value = _filters.value.copy(query = q) }
    fun onTypeFilter(type: TransactionType?) { _filters.value = _filters.value.copy(type = type) }
    fun onCategoryFilter(id: Long?) { _filters.value = _filters.value.copy(categoryId = id) }
    fun onPaymentFilter(pm: PaymentMethod?) { _filters.value = _filters.value.copy(paymentMethod = pm) }
    fun onDateRange(startEpochDay: Long?, endEpochDay: Long?) {
        _filters.value = _filters.value.copy(startEpochDay = startEpochDay, endEpochDay = endEpochDay)
    }
    fun onSort(sort: SortOption) { _filters.value = _filters.value.copy(sort = sort) }
    fun clearFilters() { _filters.value = TransactionFilters(query = _filters.value.query) }
}
