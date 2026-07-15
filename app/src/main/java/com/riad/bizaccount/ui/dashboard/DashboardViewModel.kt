package com.riad.bizaccount.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riad.bizaccount.data.local.dao.TransactionWithCategory
import com.riad.bizaccount.data.repository.TransactionRepository
import com.riad.bizaccount.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PeriodStats(val incomeMinor: Long = 0, val expenseMinor: Long = 0) {
    val profitMinor: Long get() = incomeMinor - expenseMinor
    val isProfit: Boolean get() = profitMinor >= 0
}

data class DashboardUiState(
    val today: PeriodStats = PeriodStats(),
    val month: PeriodStats = PeriodStats(),
    val year: PeriodStats = PeriodStats(),
    val totalTransactionCount: Int = 0,
    val recent: List<TransactionWithCategory> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val today = DateUtils.today()

    val uiState = combine(
        repository.summary(today.toEpochDay(), today.toEpochDay()),
        repository.summary(DateUtils.startOfMonth(today).toEpochDay(), DateUtils.endOfMonth(today).toEpochDay()),
        repository.summary(DateUtils.startOfYear(today).toEpochDay(), DateUtils.endOfYear(today).toEpochDay()),
        repository.recent(limit = 8)
    ) { todaySummary, monthSummary, yearSummary, recent ->
        DashboardUiState(
            today = PeriodStats(todaySummary.totalIncomeMinor, todaySummary.totalExpenseMinor),
            month = PeriodStats(monthSummary.totalIncomeMinor, monthSummary.totalExpenseMinor),
            year = PeriodStats(yearSummary.totalIncomeMinor, yearSummary.totalExpenseMinor),
            totalTransactionCount = yearSummary.transactionCount,
            recent = recent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
