package com.riad.bizaccount.ui.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riad.bizaccount.data.local.entity.PaymentMethod
import com.riad.bizaccount.data.local.entity.TransactionType
import com.riad.bizaccount.data.repository.TransactionRepository
import com.riad.bizaccount.data.settings.SettingsDataStore
import com.riad.bizaccount.util.CurrencyFormatter
import com.riad.bizaccount.util.DateUtils
import com.riad.bizaccount.util.ExcelExporter
import com.riad.bizaccount.util.ExportRow
import com.riad.bizaccount.util.ExportSummary
import com.riad.bizaccount.util.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ReportPeriod { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

data class ReportUiState(
    val period: ReportPeriod = ReportPeriod.MONTHLY,
    val startDate: LocalDate = DateUtils.startOfMonth(),
    val endDate: LocalDate = DateUtils.endOfMonth(),
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val transactionCount: Int = 0
) {
    val netMinor get() = incomeMinor - expenseMinor
    val isProfit get() = netMinor >= 0
    val averageMinor get() = if (transactionCount == 0) 0L else (incomeMinor + expenseMinor) / transactionCount
}

sealed class ExportEvent {
    data class Success(val file: File) : ExportEvent()
    object Failure : ExportEvent()
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransactionRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.MONTHLY)
    private val _range = MutableStateFlow(DateUtils.startOfMonth() to DateUtils.endOfMonth())

    private val _exportEvents = MutableSharedFlow<ExportEvent>()
    val exportEvents = _exportEvents.asSharedFlow()

    val uiState: StateFlow<ReportUiState> = _range.flatMapLatest { (start, end) ->
        repository.summary(start.toEpochDay(), end.toEpochDay()).map { s ->
            ReportUiState(
                period = _period.value,
                startDate = start,
                endDate = end,
                incomeMinor = s.totalIncomeMinor,
                expenseMinor = s.totalExpenseMinor,
                transactionCount = s.transactionCount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    fun selectPeriod(period: ReportPeriod, customStart: LocalDate? = null, customEnd: LocalDate? = null) {
        _period.value = period
        val today = DateUtils.today()
        _range.value = when (period) {
            ReportPeriod.DAILY -> today to today
            ReportPeriod.WEEKLY -> DateUtils.startOfWeek(today) to DateUtils.endOfWeek(today)
            ReportPeriod.MONTHLY -> DateUtils.startOfMonth(today) to DateUtils.endOfMonth(today)
            ReportPeriod.YEARLY -> DateUtils.startOfYear(today) to DateUtils.endOfYear(today)
            ReportPeriod.CUSTOM -> (customStart ?: today) to (customEnd ?: today)
        }
    }

    fun exportPdf() = viewModelScope.launch { doExport(isPdf = true) }

    fun exportExcel() = viewModelScope.launch { doExport(isPdf = false) }

    private suspend fun doExport(isPdf: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val state = uiState.value
                val settings = settingsDataStore.settingsFlow.first()
                val rows = repository.inRange(state.startDate.toEpochDay(), state.endDate.toEpochDay()).first()

                val periodLabel = "${DateUtils.formatBangla(state.startDate)} \u2014 ${DateUtils.formatBangla(state.endDate)}"
                val exportSummary = ExportSummary(
                    businessName = settings.businessName,
                    periodLabel = periodLabel,
                    totalIncome = CurrencyFormatter.format(state.incomeMinor, settings.currencySymbol),
                    totalExpense = CurrencyFormatter.format(state.expenseMinor, settings.currencySymbol),
                    netResult = CurrencyFormatter.format(kotlin.math.abs(state.netMinor), settings.currencySymbol),
                    isProfit = state.isProfit,
                    transactionCount = state.transactionCount
                )
                val exportRows = rows.map {
                    ExportRow(
                        date = DateUtils.formatBangla(LocalDate.ofEpochDay(it.dateEpochDay)),
                        type = if (it.type == TransactionType.INCOME) "আয়" else "ব্যয়",
                        category = it.categoryName,
                        description = it.description,
                        paymentMethod = paymentMethodLabel(it.paymentMethod),
                        amount = CurrencyFormatter.format(it.amountMinor, settings.currencySymbol)
                    )
                }

                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val exportDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

                val file = if (isPdf) {
                    PdfExporter.export(File(exportDir, "report_$stamp.pdf"), exportSummary, exportRows)
                } else {
                    val header = listOf("তারিখ", "ধরন", "ক্যাটাগরি", "বিবরণ", "পদ্ধতি", "পরিমাণ")
                    val body = exportRows.map { listOf(it.date, it.type, it.category, it.description, it.paymentMethod, it.amount) }
                    ExcelExporter.export(File(exportDir, "report_$stamp.xlsx"), "রিপোর্ট", header, body)
                }
                _exportEvents.emit(ExportEvent.Success(file))
            } catch (e: Exception) {
                _exportEvents.emit(ExportEvent.Failure)
            }
        }
    }

    private fun paymentMethodLabel(pm: PaymentMethod) = when (pm) {
        PaymentMethod.CASH -> "নগদ"
        PaymentMethod.BANK -> "ব্যাংক"
        PaymentMethod.MOBILE_BANKING -> "মোবাইল ব্যাংকিং"
    }
}
